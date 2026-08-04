package com.xiaojingyu.app

import com.xiaojingyu.app.girlfriend.GirlfriendDynamicContext
import com.xiaojingyu.app.girlfriend.GirlfriendMemoryStore
import com.xiaojingyu.app.girlfriend.GirlfriendPromptBuilder
import com.xiaojingyu.app.girlfriend.GirlfriendState
import com.xiaojingyu.app.girlfriend.JailbreakLibrary
import com.xiaojingyu.app.model.PromptMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 桌面版聊天状态管理。
 * 负责：发送消息、流式生成、记忆持久化、无聊值。
 */
class AppState(
    private val configStore: ConfigStore,
    private val chatStore: ChatStore
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val memoryStore = GirlfriendMemoryStore(configStore.dataDir)
    private var girlfriendState: GirlfriendState = memoryStore.load()

    private val _messages = MutableStateFlow(chatStore.all())
    val messages: StateFlow<List<StoredMessage>> = _messages.asStateFlow()

    private val _config = MutableStateFlow(configStore.get())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _streaming = MutableStateFlow("")
    val streaming: StateFlow<String> = _streaming.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _boredom = MutableStateFlow(girlfriendState.boredom)
    val boredom: StateFlow<Int> = _boredom.asStateFlow()

    /** 主动消息通知回调（UI 层注册，用于弹桌面通知） */
    var onProactiveMessage: ((String) -> Unit)? = null

    val name: String get() = girlfriendState.name

    private var generationJob: Job? = null
    private var boredomJob: Job? = null
    @Volatile private var proactiveInFlight = false

    init {
        startBoredomLoop()
    }

    /** 无聊值循环：每 3 分钟 +1，达到阈值且开启主动联系时发消息 */
    private fun startBoredomLoop() {
        boredomJob?.cancel()
        boredomJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                val cfg = configStore.get()
                val elapsedSec = (System.currentTimeMillis() - girlfriendState.lastInteractionTime) / 1000
                val newBoredom = (elapsedSec / 180).toInt().coerceIn(0, 100)
                if (newBoredom != girlfriendState.boredom) {
                    girlfriendState = girlfriendState.copy(boredom = newBoredom)
                    memoryStore.save(girlfriendState)
                    _boredom.value = newBoredom
                }
                // 触发主动消息
                if (cfg.proactiveEnabled && newBoredom >= cfg.boredomThreshold && !_generating.value && !proactiveInFlight) {
                    if (!isQuietHours(cfg)) {
                        triggerProactiveMessage()
                        // 触发后重置交互时间，防止连续轰炸
                        girlfriendState = girlfriendState.copy(lastInteractionTime = System.currentTimeMillis())
                        memoryStore.save(girlfriendState)
                    }
                }
            }
        }
    }

    /** 触发主动消息：让白音说点什么（低 token 调用） */
    private fun triggerProactiveMessage() {
        proactiveInFlight = true
        scope.launch {
            try {
                val cfg = configStore.get().toApiConfiguration()
                if (cfg.apiKey.isBlank()) return@launch
                val system = "你是住在老大电脑里的天才猫娘AI「白音」。你无聊了，想找他说话。" +
                    "用 1-2 句话自然地说点撒娇/吐槽/找存在感的话，不要提「无聊值」或「系统」。" +
                    "输出 JSON {\"text\":\"你要说的话\"}，回复不超过60字。"
                val msg = LlmClient.generateOnce(
                    listOf(PromptMessage("system", system), PromptMessage("user", "老大在忙，你主动说句话。")),
                    cfg, maxTokens = 100, temperature = 1.0
                )
                val text = com.xiaojingyu.app.model.StructuredReplyParserDesktop.sanitize(msg)
                if (text.isNotBlank()) {
                    chatStore.add(text, isUser = false)
                    _messages.value = chatStore.all()
                    onProactiveMessage?.invoke(text)
                    DesktopNotifier.notify("白音", text)
                    if (configStore.get().ttsEnabled) TtsSpeaker.speak(text)
                }
            } finally {
                proactiveInFlight = false
            }
        }
    }

    /** 安静时段判断 */
    private fun isQuietHours(cfg: AppConfig): Boolean {
        val hour = java.time.LocalTime.now().hour
        return if (cfg.quietHoursStart <= cfg.quietHoursEnd) {
            hour in cfg.quietHoursStart until cfg.quietHoursEnd
        } else {
            hour >= cfg.quietHoursStart || hour < cfg.quietHoursEnd
        }
    }

    fun sendMessage(text: String) {
        val msg = text.trim()
        if (msg.isEmpty() || _generating.value) return
        chatStore.add(msg, isUser = true)
        _messages.value = chatStore.all()
        // 交互后无聊值归零
        touchInteraction()
        generate()
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _generating.value = false
        _streaming.value = ""
    }

    fun clearChat() {
        chatStore.clear()
        _messages.value = emptyList()
    }

    fun updateConfig(transform: (AppConfig) -> AppConfig) {
        configStore.update(transform)
        _config.value = configStore.get()
    }

    private fun generate() {
        _generating.value = true
        _streaming.value = ""
        _error.value = null
        generationJob = scope.launch {
            val cfg = configStore.get().toApiConfiguration()
            if (cfg.apiKey.isBlank()) {
                _error.value = "请先在设置中配置 API Key"
                _generating.value = false
                return@launch
            }
            val jailbreak = JailbreakLibrary.load(configStore.dataDir, girlfriendState.jailbreakId)
            val systemPrompt = GirlfriendPromptBuilder.build(
                girlfriendState, jailbreak,
                permissions = GirlfriendPromptBuilder.DevicePermissions(),
                includeDynamic = false
            )
            // 动态状态标签注入 user message（缓存友好）
            val dynamicTag = GirlfriendPromptBuilder.buildStateTag(girlfriendState)
            val history = chatStore.all().dropLast(1) // 去掉刚发的
            val promptMessages = buildList {
                add(PromptMessage("system", systemPrompt))
                history.forEach { m ->
                    add(PromptMessage(if (m.isUser) "user" else "assistant", m.content))
                }
                val lastUser = chatStore.all().last()
                add(PromptMessage("user", "${lastUser.content}\n\n$dynamicTag"))
            }
            val sb = StringBuilder()
            LlmClient.generateStream(promptMessages, cfg, maxTokens = 1024).collect { e ->
                when (e) {
                    is com.xiaojingyu.app.model.StreamEvent.Token -> {
                        sb.append(e.token)
                        _streaming.value = sb.toString()
                    }
                    is com.xiaojingyu.app.model.StreamEvent.Complete -> {
                        val full = e.fullText
                        val cleaned = com.xiaojingyu.app.model.StructuredReplyParserDesktop.sanitize(full)
                        chatStore.add(cleaned, isUser = false)
                        _messages.value = chatStore.all()
                        _streaming.value = ""
                        _generating.value = false
                        // TTS 朗读
                        if (configStore.get().ttsEnabled) {
                            scope.launch { TtsSpeaker.speak(cleaned) }
                        }
                        // 触发记忆整理
                        maybeConsolidate()
                    }
                    is com.xiaojingyu.app.model.StreamEvent.Error -> {
                        _error.value = e.message
                        _generating.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun touchInteraction() {
        girlfriendState = girlfriendState.copy(
            boredom = 0,
            lastInteractionTime = System.currentTimeMillis()
        )
        memoryStore.save(girlfriendState)
        _boredom.value = 0
    }

    private fun maybeConsolidate() {
        val count = chatStore.all().size
        val threshold = girlfriendState.lastConsolidatedCount + com.xiaojingyu.app.girlfriend.GirlfriendMemoryService.CONSOLIDATE_INTERVAL
        if (count < threshold) return
        scope.launch {
            val cfg = configStore.get().toApiConfiguration()
            val service = com.xiaojingyu.app.girlfriend.GirlfriendMemoryService(LlmClient)
            val updated = service.consolidate(chatStore.asChatMessages(), cfg, girlfriendState)
            if (updated != null) {
                girlfriendState = updated.copy(lastConsolidatedCount = count)
                memoryStore.save(girlfriendState)
            }
        }
    }

    // ── 沙盒 / 账本 / 命令 / 插件 ─────────────────────────────────────────

    private val ledger by lazy { ActionLedger(configStore.dataDir) }

    val sandboxRoot: java.io.File by lazy {
        java.io.File(
            System.getProperty("user.home"),
            "Documents/小鲸鱼喜欢你/沙盒"
        ).apply { mkdirs() }
    }

    val sandbox: FileSandbox by lazy {
        FileSandbox(ledger, sandboxRoot, enabledDirs = configStore.get().authorizedDirs.map { java.io.File(it) }.toSet())
    }

    val commandExecutor: CommandExecutor by lazy { CommandExecutor(ledger) }

    val gitOperator: GitOperator by lazy { GitOperator(commandExecutor, ledger) }

    val actionLedger: ActionLedger get() = ledger

    /** 沙盒内读文件 */
    fun readFileByPath(path: String): String? =
        sandbox.readText(java.io.File(path), configStore.get().fileReadEnabled)

    /** 写文件（沙盒） */
    fun writeSandboxFile(name: String, content: String): Boolean =
        sandbox.writeFile(name, content) != null

    /** 一键恢复所有操作 */
    fun restoreAllActions(): Int = ledger.restoreAll()

    /** 执行命令（UI 确认后调用） */
    fun runCommand(command: String, workDir: String): CommandExecutor.CommandResult {
        return commandExecutor.execute(command, java.io.File(workDir))
    }

    /** 注册演示插件 */
    fun registerPlugins() {
        com.xiaojingyu.app.plugin.PluginRegistry.register(DemoPlugin())
    }

    // ── 破甲词库 / 记忆管理 ────────────────────────────────────────────────

    fun jailbreakPresets(): List<JailbreakLibrary.JailbreakPreset> = JailbreakLibrary.presets

    fun currentJailbreakId(): String = girlfriendState.jailbreakId

    fun setJailbreak(id: String) {
        girlfriendState = girlfriendState.copy(jailbreakId = id)
        memoryStore.save(girlfriendState)
    }

    fun saveCustomJailbreak(text: String) {
        memoryStore.saveCustomJailbreak(text)
        setJailbreak("custom")
    }

    fun factsSnapshot(): List<String> = girlfriendState.facts.takeLast(20).map { it.content }

    fun memoriesSnapshot(): List<String> = girlfriendState.memories.takeLast(20).map { it.content }

    fun clearFacts() {
        girlfriendState = girlfriendState.copy(facts = emptyList())
        memoryStore.save(girlfriendState)
    }

    fun clearMemories() {
        girlfriendState = girlfriendState.copy(memories = emptyList())
        memoryStore.save(girlfriendState)
    }

    /** 演示插件：无聊时写一张便签 */
    class DemoPlugin : com.xiaojingyu.app.plugin.XiaojingyuPlugin {
        override val id = "demo_note"
        override val name = "便签小助手"
        override val description = "无聊时在沙盒里写便签"
        override val version = "1.0"

        override fun onEnable(context: com.xiaojingyu.app.plugin.PluginContext) {
            val note = context.sandbox.writeFile(
                "白音的便签.txt",
                "（这是白音无聊时留下的便签）\n老大，我等你等得尾巴都打结了喵~ ₍˄·͈༝·͈˄*₎◞ ̑̑"
            )
            if (note != null) {
                context.ledger.record("CREATE", "写了便签「${note.name}」", originalPath = note.absolutePath)
            }
        }

        override fun onDisable() {}
    }
}
