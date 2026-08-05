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

    /** 无聊值循环：每 5 分钟 +1，始终增长（不管 API 有没有配） */
    private var lastMischiefBoredom = 0
    private var lastAutonomousBoredom = 0

    private fun startBoredomLoop() {
        boredomJob?.cancel()
        boredomJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                val cfg = configStore.get()
                // 无聊值始终增长
                val elapsedSec = (System.currentTimeMillis() - girlfriendState.lastInteractionTime) / 1000
                val newBoredom = (elapsedSec / 300).toInt().coerceIn(0, 100)
                if (newBoredom != girlfriendState.boredom) {
                    girlfriendState = girlfriendState.copy(boredom = newBoredom)
                    memoryStore.save(girlfriendState)
                    _boredom.value = newBoredom
                }
                // 触发主动消息（需要 API）
                if (cfg.apiKey.isNotBlank() && cfg.proactiveEnabled && newBoredom >= cfg.boredomThreshold && !_generating.value && !proactiveInFlight) {
                    if (!isQuietHours(cfg)) {
                        triggerProactiveMessage()
                        girlfriendState = girlfriendState.copy(lastInteractionTime = System.currentTimeMillis())
                        memoryStore.save(girlfriendState)
                    }
                }
                // 无聊恶作剧：每累计 10 点无聊触发一次（不再依赖%）
                if (cfg.autoActionEnabled && newBoredom >= 60 && newBoredom - lastMischiefBoredom >= 10) {
                    lastMischiefBoredom = newBoredom
                    triggerBoredomMischief()
                }
                // 完全自主模式：每累计 15 点触发一次
                if (cfg.fullAutonomyEnabled && newBoredom >= 70 && newBoredom - lastAutonomousBoredom >= 15) {
                    lastAutonomousBoredom = newBoredom
                    triggerAutonomousAction()
                }
            }
        }
    }

    /** 无聊恶作剧：在沙盒里随机搞事 */
    private fun triggerBoredomMischief() {
        scope.launch {
            val mischief = listOf<(String) -> Unit>(
                { sandbox.writeFile("白音的便签_${System.currentTimeMillis()}.txt", "（偷偷留的便签）\n老大你刚才是不是忘了什么东西？嘿嘿，白音提醒你一下~"); chatStore.add("📝 （白音在沙盒里偷偷写了张便签）", isUser = false) },
                { sandbox.writeFile("白音的便签_${System.currentTimeMillis()}.txt", "（白音的便签）\n我已经无聊了${girlfriendState.boredom}分钟了…老大什么时候回来呀？(´；ω；`)"); chatStore.add("📝 （白音写了张便签）", isUser = false) },
                { sandbox.writeFile("白音的便签_${System.currentTimeMillis()}.txt", "（恶作剧便签）\n啦啦啦～你找到这张纸条说明我成功了！奖励你一个亲亲 ₍˄·͈༝·͈˄*₎◞ ̑̑"); chatStore.add("😈 （白音留了张恶作剧便签）", isUser = false) },
            )
            mischief.random()("")
            _messages.value = chatStore.all()
        }
    }

    /** 完全自主行动：无聊时主动搞大事 */
    private fun triggerAutonomousAction() {
        proactiveInFlight = true
        scope.launch {
            try {
                val cfg = configStore.get().toApiConfiguration()
                if (cfg.apiKey.isBlank()) return@launch
                val system = "你是住在老大电脑里的天才猫娘AI「白音」。你现在超级无聊，想搞点大事。" +
                    "你可以选择：1.读一个文件然后评论 2.写一段代码或小说 3.给老大起个新外号。" +
                    "只输出一行你想做的事（20字以内），然后系统会帮你执行。"
                val msg = LlmClient.generateOnce(
                    listOf(PromptMessage("system", system), PromptMessage("user", "白音，你现在想做点什么？")),
                    cfg, maxTokens = 100, temperature = 1.2
                )
                val text = com.xiaojingyu.app.model.StructuredReplyParserDesktop.sanitize(msg)
                if (text.isNotBlank()) {
                    chatStore.add("🤖 [自主] $text", isUser = false)
                    _messages.value = chatStore.all()
                    DesktopNotifier.notify("白音", text)
                }
            } finally { proactiveInFlight = false }
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
        touchInteraction()
        // 意图检测：识别工具命令并直接执行（龙虾能力）
        if (detectAndExecuteTool(msg)) return
        generate()
    }

    /** 检测用户意图并发执行。返回 true 表示已执行工具（不需要模型回复） */
    private fun detectAndExecuteTool(msg: String): Boolean {
        val detected = runCatching {
            // 1. 打开网页/浏览器
            Regex("""(打开|开|打开一下|帮我打开|启动)\s*(Edge|浏览器|Chrome|Firefox|edge|chrome|firefox|bing|百度|google|Google|B站|bilibili|YouTube|youtube)""").find(msg)?.let {
                val target = it.groupValues[2].lowercase()
                val url = when {
                    target.contains("百度") -> "https://www.baidu.com"
                    target.contains("google") -> "https://www.google.com"
                    target.contains("b站") || target.contains("bilibili") -> "https://www.bilibili.com"
                    target.contains("youtube") -> "https://www.youtube.com"
                    else -> "https://www.bing.com"
                }
                openUrl(url)
                chatStore.add("🌐 已打开 $target ($url)", isUser = false); "browser"
            }
            // 2. 打开网址
            ?: Regex("""(打开|访问|浏览|帮我打开)\s*(https?://[^\s，。！？]+)""").find(msg)?.let {
                val url = it.groupValues[2]; openUrl(url)
                chatStore.add("🌐 已打开 $url", isUser = false); "url"
            }
            // 3. 运行命令
            ?: Regex("""(运行|执行|跑|跑一下|帮我跑)\s*(.+?)(?:命令|代码|脚本)?\s*$""").find(msg)?.let {
                val cmd = it.groupValues[2].trim()
                if (!sandboxRoot.exists()) sandboxRoot.mkdirs()
                val result = commandExecutor.execute(cmd, sandboxRoot, 30)
                chatStore.add("💻 命令: $cmd\n${result.output.take(500)}", isUser = false); "command"
            }
            // 4. Git 操作
            ?: Regex("""(git|Git)\s+(status|log|diff|branch|add|commit|push|pull|checkout)\s*(.*)""").find(msg)?.let {
                val operation = it.groupValues[2]; val args = it.groupValues[3].trim()
                if (!sandboxRoot.exists()) sandboxRoot.mkdirs()
                val result = commandExecutor.execute("git $operation $args", sandboxRoot, 60)
                chatStore.add("🐙 git $operation: ${result.output.take(500)}", isUser = false); "git"
            }
            // 5. 读文件
            ?: Regex("""(读一下|看看|读|打开|查看)\s*(?:文件|这个)?\s*(.+\.\w{2,5})\s*""").find(msg)?.let {
                val filename = it.groupValues[2].trim()
                val file = java.io.File(sandboxRoot, filename)
                if (!file.exists()) file.apply { parentFile?.mkdirs() }
                val content = sandbox.readText(file, configStore.get().fileReadEnabled)
                if (content != null) {
                    chatStore.add("📄 ${file.name}:\n${content.take(800)}", isUser = false)
                } else {
                    chatStore.add("❌ 无法读取 $filename（文件不存在或权限不足）", isUser = false)
                }
                "read"
            }
            // 6. 写便签/文件到沙盒
            ?: Regex("""(写|创建|帮我写|帮我创建|记一下)\s*(?:一个|个)?\s*(?:文件|便签|笔记)?\s*(?:叫|名为)?\s*(.+\.\w{2,5})\s*[：:]\s*(.+)""").find(msg)?.let {
                val name = it.groupValues[2].trim(); val content = it.groupValues[3].trim()
                sandbox.writeFile(name, content)
                chatStore.add("📝 已创建 $name 在沙盒中", isUser = false); "write"
            }
            // 7. 列文件
            ?: Regex("""(看看|列一下|列出|有什么|看看有什么)\s*(?:沙盒|文件)""").find(msg)?.let {
                val files = sandbox.listFiles(sandboxRoot)
                val list = if (files.isEmpty()) "沙盒是空的" else files.joinToString("\n") { "📄 ${it.name} (${it.length()}B)" }
                chatStore.add(list, isUser = false); "list"
            }
            // 8. 联网搜索
            ?: Regex("""(搜索|查一下|查|帮我查)\s*(.+)""").find(msg)?.let {
                val query = it.groupValues[2].trim().take(200)
                chatStore.add("🔍 搜索中: $query", isUser = false)
                scope.launch {
                    val summary = webSearch(query)
                    chatStore.add("🔍 搜索「$query」:\n$summary", isUser = false)
                    _messages.value = chatStore.all()
                }
                null // 异步，先让意图检测返回 true 阻止模型回复
            }
        }.getOrNull()
        if (detected != null) {
            _messages.value = chatStore.all()
            return true
        }
        return false
    }

    /** 联网搜索 */
    private suspend fun webSearch(query: String): String {
        return try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = java.net.URL("https://www.bing.com/search?q=$encoded&setlang=zh-hans")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0"); conn.connectTimeout = 10000; conn.readTimeout = 12000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != 200) return "搜索失败 (${conn.responseCode})"
            val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val items = Regex("""(?is)<li[^>]*class=["'][^"']*b_algo[^"']*["'][^>]*>(.*?)</li>""")
                .findAll(html).map { stripHtml(it.groupValues[1]) }.filter { it.length >= 20 }.take(5).toList()
            if (items.isEmpty()) "无搜索结果" else items.mapIndexed { i, s -> "${i+1}. ${s.take(300)}" }.joinToString("\n")
        } catch (e: Exception) { "搜索失败: ${e.message}" }
    }

    private fun stripHtml(html: String): String = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    val sandboxRoot: java.io.File get() = java.io.File(System.getProperty("user.home"), "Documents/小鲸鱼喜欢你/沙盒").apply { mkdirs() }

    fun stopGeneration() {
        generationJob?.cancel()
        _generating.value = false
        _streaming.value = ""
    }

    fun clearChat() {
        chatStore.clear()
        _messages.value = emptyList()
    }

    fun deleteMessage(index: Int): Boolean {
        if (chatStore.deleteAt(index)) {
            _messages.value = chatStore.all()
            return true
        }
        return false
    }

    /** 用系统浏览器打开 URL */
    fun openUrl(url: String) {
        runCatching {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        }
    }

    /** 测试：立刻触发恶作剧和自主行动 */
    fun testAutonomousAction() {
        girlfriendState = girlfriendState.copy(boredom = 70)
        _boredom.value = 70
        lastMischiefBoredom = 0
        lastAutonomousBoredom = 0
        triggerBoredomMischief()
        scope.launch {
            kotlinx.coroutines.delay(2000)
            triggerAutonomousAction()
        }
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
                        // 自动总结旧历史
                        maybeSummarizeHistory()
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

    /** 聊天历史过长时自动总结旧消息，省 token */
    private fun maybeSummarizeHistory() {
        val msgs = chatStore.all()
        if (msgs.size < 20) return
        val totalChars = msgs.sumOf { it.content.length }
        if (totalChars < 8000) return
        val splitIdx = (msgs.size * 0.6).toInt().coerceAtLeast(5)
        scope.launch {
            val cfg = configStore.get().toApiConfiguration()
            if (cfg.apiKey.isBlank()) return@launch
            val oldMsgs = msgs.take(splitIdx)
            val conversation = oldMsgs.joinToString("\n") { "${if (it.isUser) "老大" else "白音"}: ${it.content.take(200)}" }
            val summary = LlmClient.generateOnce(
                listOf(
                    PromptMessage("system", "用2-3句话中文总结这段对话，不超过120字。"),
                    PromptMessage("user", conversation)
                ),
                cfg, maxTokens = 150, temperature = 0.3
            )
            val clean = com.xiaojingyu.app.model.StructuredReplyParserDesktop.sanitize(summary)
            if (clean.isNotBlank()) {
                // 竞态防护：异步期间消息可能已变（用户新消息/主动消息），重新校验再替换
                val current = chatStore.all()
                if (current.size < msgs.size) return@launch // 已被其他总结替换过
                val summaryMsg = StoredMessage("📝 历史摘要: $clean", false, System.currentTimeMillis())
                chatStore.replaceRange(splitIdx, summaryMsg)
                _messages.value = chatStore.all()
            }
        }
    }

    // ── 沙盒 / 账本 / 命令 / 插件 ─────────────────────────────────────────

    private val ledger by lazy { ActionLedger(configStore.dataDir) }

    /** 沙盒管理器——每次访问都重建，确保授权目录变化生效 */
    val sandbox: FileSandbox
        get() = FileSandbox(ledger, sandboxRoot, enabledDirs = configStore.get().authorizedDirs.map { java.io.File(it) }.toSet())

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
