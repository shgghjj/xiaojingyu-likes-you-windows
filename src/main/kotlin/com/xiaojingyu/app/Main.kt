package com.xiaojingyu.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaojingyu.app.girlfriend.GirlfriendMemoryStore
import com.xiaojingyu.app.girlfriend.JailbreakLibrary
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.coroutines.launch

@Composable
fun App() {
    val configStore = remember { ConfigStore() }
    val chatStore = remember { ChatStore(configStore.dataDir) }
    val appState = remember { AppState(configStore, chatStore) }

    val config by appState.config.collectAsState()
    val lang = config.language
    var showLicense by remember { mutableStateOf(!configStore.get().licenseAccepted) }
    var showSettings by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121217)) {
            if (showLicense) {
                LicenseNoticeScreen(lang = lang, onAccept = {
                    configStore.update { it.copy(licenseAccepted = true) }
                    showLicense = false
                })
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    LeftPanel(appState, lang = lang, onOpenSettings = { showSettings = true })
                    Box(modifier = Modifier.weight(1f)) { ChatPanel(appState, lang = lang) }
                    RightPanel(appState, lang = lang)
                }
            }
            // 设置面板：DialogWindow 自动在最上层
            if (showSettings) {
                SettingsPanel(appState, configStore, onDismiss = { showSettings = false })
            }
        }
    }
}

@Composable
private fun LeftPanel(appState: AppState, lang: String, onOpenSettings: () -> Unit) {
    val boredom by appState.boredom.collectAsState()

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Color(0xFF16161C))
            .padding(16.dp)
    ) {
        Text(I18n.get("app_title", lang), color = Color(0xFF6EC6F0), fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color(0xFF1A1A22), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) { Text("🐳", fontSize = 48.sp) }
        Spacer(Modifier.height(12.dp))
        Text(appState.name, color = Color(0xFFE8E8EC), fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(I18n.get("app_subtitle", lang), color = Color(0xFF6E6E7A), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))

        // 无聊值
        Text(I18n.get("boredom_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { boredom / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (boredom >= 60) Color(0xFFF0A050) else Color(0xFF6EC6F0),
            trackColor = Color(0xFF20202A)
        )
        Spacer(Modifier.height(4.dp))
        val boredomLabel = when {
            boredom >= 80 -> I18n.get("boredom_super", lang)
            boredom >= 60 -> I18n.get("boredom_bit", lang)
            boredom >= 30 -> I18n.get("boredom_slightly", lang)
            else -> I18n.get("boredom_ok", lang)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$boredom / 100", color = Color(0xFF8E8E9A), fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Text(boredomLabel, color = if (boredom >= 60) Color(0xFFF0A050) else Color(0xFF6E6E7A), fontSize = 10.sp)
        }

        Spacer(Modifier.height(6.dp))
        // 测试自主行动按钮
        OutlinedButton(
            onClick = { appState.testAutonomousAction() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) { Text("🧪 测试自主", color = Color(0xFFF0A050), fontSize = 11.sp) }

        Spacer(Modifier.weight(1f))

        // 清空对话
        var showClearConfirm by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showClearConfirm = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) { Text(I18n.get("clear_chat", lang), color = Color(0xFFE54860), fontSize = 12.sp) }
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(I18n.get("clear_chat_title", lang), color = Color(0xFFE8E8EC)) },
                text = { Text(I18n.get("clear_chat_body", lang), color = Color(0xFF9A9AA4)) },
                confirmButton = {
                    TextButton(onClick = { appState.clearChat(); showClearConfirm = false }) {
                        Text(I18n.get("clear_chat_ok", lang), color = Color(0xFFE54860))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text(I18n.get("cancel", lang), color = Color(0xFF8E8E9A)) }
                },
                containerColor = Color(0xFF14141A)
            )
        }

        // Live2D 模型管理
        Text(I18n.get("live2d_model", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        var selectedModel by remember { mutableStateOf(Live2DModelManager.currentModel) }
        val models = remember { Live2DModelManager.listModels() }
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedModel ?: I18n.get("live2d_select", lang),
                    color = Color(0xFF6EC6F0),
                    fontSize = 12.sp
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                models.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name, fontSize = 12.sp) },
                        onClick = {
                            selectedModel = name
                            Live2DModelManager.currentModel = name
                            expanded = false
                        }
                    )
                }
                if (models.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(I18n.get("live2d_none", lang), color = Color(0xFF6E6E7A), fontSize = 12.sp) },
                        onClick = { expanded = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row {
            // 导入按钮
            OutlinedButton(
                onClick = {
                    val dialog = java.awt.FileDialog(java.awt.Frame(), I18n.get("import_dialog_title", lang), java.awt.FileDialog.LOAD)
                    dialog.file = "*.zip"
                    dialog.isVisible = true
                    if (dialog.file != null) {
                        val zipPath = java.io.File(dialog.directory, dialog.file).absolutePath
                        if (Live2DModelManager.importModel(zipPath) != null) {
                            selectedModel = Live2DModelManager.listModels().lastOrNull()
                            Live2DModelManager.currentModel = selectedModel
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(I18n.get("live2d_import", lang), color = Color(0xFF6EC6F0), fontSize = 12.sp) }
            Spacer(Modifier.width(4.dp))
            // 打开舞台
            OutlinedButton(
                onClick = { Live2DStageServer.openInBrowser() },
                modifier = Modifier.weight(1f)
            ) { Text(I18n.get("live2d_open", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp) }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18n.get("settings", lang), color = Color(0xFF9A9AA4)) }
    }
}

@Composable
private fun ChatPanel(appState: AppState, lang: String) {
    val messages by appState.messages.collectAsState()
    val streaming by appState.streaming.collectAsState()
    val generating by appState.generating.collectAsState()
    val error by appState.error.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streaming) {
        if (messages.isNotEmpty() || streaming.isNotEmpty()) {
            listState.scrollToItem(Int.MAX_VALUE)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 聊天记录
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (streaming.isNotEmpty()) {
                item {
                    StreamingBubble(streaming)
                }
            }
            if (generating && streaming.isEmpty()) {
                item { Text(I18n.get("typing", lang), color = Color(0xFF6E6E7A), fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
            }
        }

        error?.let {
            Text(it, color = Color(0xFFE54860), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        // 输入区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF1A1A22), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(I18n.get("send_placeholder", lang), color = Color(0xFF6E6E7A)) },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE8E8EC), fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE8E8EC),
                    unfocusedTextColor = Color(0xFFE8E8EC),
                    cursorColor = Color(0xFF6EC6F0),
                    focusedBorderColor = Color(0xFF6EC6F0),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = {
                    if (input.isNotBlank() && !generating) {
                        appState.sendMessage(input)
                        input = ""
                    }
                })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank() && !generating) {
                        appState.sendMessage(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank() && !generating,
                modifier = Modifier.background(Color(0xFF6EC6F0), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = I18n.get("send", lang), tint = Color(0xFF121217))
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: StoredMessage) {
    val isUser = msg.isUser
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) Color(0xFF323840) else Color(0xFF22222E),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Text(
                text = msg.content,
                color = if (isUser) Color(0xFFE0E0E8) else Color(0xFFE8E8EC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun StreamingBubble(content: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = Color(0xFF22222E)
        ) {
            Text(
                text = content,
                color = Color(0xFFE8E8EC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

fun main() = application {
    // 单实例保护
    val lockFile = java.io.File(System.getProperty("java.io.tmpdir"), "xiaojingyu_app.lock")
    try {
        val raf = java.io.RandomAccessFile(lockFile, "rw")
        val lock = raf.channel.tryLock()
        if (lock == null) {
            // 已有实例在运行
            raf.close()
            exitApplication()
            return@application
        }
        // 注册退出清理
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { lock.release(); raf.close(); lockFile.delete() } })
    } catch (_: Exception) { /* 锁失败不阻止启动 */ }

    SystemTrayManager.onOpenWindow = { }
    SystemTrayManager.onExit = { exitApplication() }
    SystemTrayManager.install()

    // 自动加入 Windows Defender 白名单
    runCatching {
        val jarPath = MainKt::class.java.protectionDomain.codeSource.location.path
        SecurityWhitelist.install(jarPath)
    }

    val windowIcon = remember { loadAppIcon() }
    val appLang = runCatching { ConfigStore().get().language }.getOrDefault("zh")

    Window(
        onCloseRequest = { exitApplication() },
        title = I18n.get("app_title", appLang),
        icon = windowIcon,
        state = rememberWindowState(size = DpSize(1300.dp, 850.dp))
    ) {
        App()
    }
    SystemTrayManager.remove()
}

/** 从 resources 加载 ICO 图标（转 PNG 供 Compose Window 使用） */
private fun loadAppIcon(): androidx.compose.ui.graphics.painter.Painter? {
    return try {
        val bytes = Live2DStageServer::class.java.classLoader
            ?.getResourceAsStream("xiaojingyu_icon.ico")?.use { it.readBytes() } ?: return null
        // 从 ICO 提取 PNG（现代 ICO 用 PNG 压缩）
        val pngBytes = extractPngFromIco(bytes) ?: return null
        val bitmap = org.jetbrains.skia.Image.makeFromEncoded(pngBytes)
            .toComposeImageBitmap()
        androidx.compose.ui.graphics.painter.BitmapPainter(bitmap)
    } catch (_: Exception) { null }
}

/** 从 ICO 二进制提取内嵌 PNG（现代 ICO 用 PNG 压缩） */
private fun extractPngFromIco(ico: ByteArray): ByteArray? {
    return try {
        val count = ((ico[4].toInt() and 0xFF) shl 8) or (ico[5].toInt() and 0xFF)
        if (count <= 0) return null
        // 目录项 16 字节，第 12 字节是 PNG 数据偏移（4字节 LE）
        val offset = ((ico[12].toInt() and 0xFF)) or
            ((ico[13].toInt() and 0xFF) shl 8) or
            ((ico[14].toInt() and 0xFF) shl 16) or
            ((ico[15].toInt() and 0xFF) shl 24)
        val pngSize = ico.size - offset
        if (pngSize <= 0) return null
        ico.copyOfRange(offset, ico.size)
    } catch (_: Exception) { null }
}

@Composable
private fun RightPanel(appState: AppState, lang: String) {
    var tab by remember { mutableStateOf(0) } // 0=账本 1=沙盒
    val entries = remember { mutableStateListOf<ActionEntry>().apply { addAll(appState.actionLedger.all()) } }
    var restoredMsg by remember { mutableStateOf<String?>(null) }
    var sandboxFiles by remember { mutableStateOf<List<java.io.File>>(appState.sandbox.listFiles(appState.sandboxRoot)) }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF16161C))
            .padding(12.dp)
    ) {
        Row {
            TabButton(I18n.get("right_ledger", lang), tab == 0) { tab = 0 }
            Spacer(Modifier.width(8.dp))
            TabButton(I18n.get("right_sandbox", lang), tab == 1) { tab = 1; sandboxFiles = appState.sandbox.listFiles(appState.sandboxRoot) }
        }
        Spacer(Modifier.height(8.dp))

        if (tab == 0) {
            Button(
                onClick = {
                    val restored = appState.restoreAllActions()
                    restoredMsg = I18n.get("right_restored", lang).replace("{}", restored.toString())
                    entries.clear(); entries.addAll(appState.actionLedger.all())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF323840))
            ) { Text(I18n.get("right_restore", lang), color = Color(0xFF6EC6F0)) }
            restoredMsg?.let {
                Text(it, color = Color(0xFFF0A050), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(entries.reversed()) { entry ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (entry.restored) Color(0xFF20202A) else Color(0xFF1A1A22),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                "${entry.timestamp} [${entry.type}]",
                                color = if (entry.restored) Color(0xFF6E6E7A) else Color(0xFFF0A050),
                                fontSize = 11.sp
                            )
                            Text(entry.description, color = Color(0xFFE8E8EC), fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = { sandboxFiles = appState.sandbox.listFiles(appState.sandboxRoot) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF323840))
            ) { Text(I18n.get("right_sandbox_refresh", lang), color = Color(0xFF6EC6F0)) }
            Spacer(Modifier.height(8.dp))
            Text(I18n.get("right_sandbox_path", lang), color = Color(0xFF6E6E7A), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sandboxFiles) { f ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A1A22),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                            appState.showFileContent(f)
                        }
                    ) {
                        Text(
                            if (f.name.startsWith(".白音藏起来的_")) "🔒 ${f.name.removePrefix(".白音藏起来的_")} (${I18n.get("right_hidden", lang)})"
                            else "📄 ${f.name}",
                            color = Color(0xFFE8E8EC),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                        if (sandboxFiles.isEmpty()) {
                    item { Text(I18n.get("right_sandbox_empty", lang), color = Color(0xFF8E8E9A), fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun LicenseNoticeScreen(lang: String, onAccept: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121217)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(I18n.get("license_title", lang), style = MaterialTheme.typography.headlineSmall, color = Color(0xFF6EC6F0), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text(
                I18n.get("license_body", lang),
                color = Color(0xFFB0B0BA),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6EC6F0), contentColor = Color(0xFF121217))
            ) { Text(I18n.get("license_accept", lang), modifier = Modifier.padding(vertical = 12.dp)) }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF202030) else Color.Transparent,
            contentColor = if (selected) Color(0xFF6EC6F0) else Color(0xFF6E6E7A)
        ),
        modifier = Modifier.height(32.dp)
    ) { Text(label, fontSize = 12.sp) }
}
