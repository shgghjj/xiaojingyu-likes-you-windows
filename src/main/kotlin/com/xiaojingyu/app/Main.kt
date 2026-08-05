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

    // 首次启动许可声明（只出现一次）
    var showLicense by remember {
        mutableStateOf(!configStore.get().licenseAccepted)
    }

    if (showLicense) {
        // 使用与 MaterialTheme 一致的颜色无需额外 MaterialTheme 包裹
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121217)) {
            if (showLicense) {
                LicenseNoticeScreen(onAccept = {
                    configStore.update { it.copy(licenseAccepted = true) }
                    showLicense = false
                })
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // 左栏：白音状态
                    LeftPanel(appState, configStore)
                    // 中间：聊天
                    Box(modifier = Modifier.weight(1f)) {
                        ChatPanel(appState)
                    }
                    // 右栏：行动账本 + 沙盒
                    RightPanel(appState)
                }
            }
        }
    }
}

@Composable
private fun LeftPanel(appState: AppState, configStore: ConfigStore) {
    val boredom by appState.boredom.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Color(0xFF16161C))
            .padding(16.dp)
    ) {
        Text("小鲸鱼喜欢你", color = Color(0xFF6EC6F0), fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color(0xFF1A1A22), RoundedCornerShape(60.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🐳", fontSize = 48.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("白音", color = Color(0xFFE8E8EC), fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("天才猫娘AI", color = Color(0xFF6E6E7A), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))

        // 无聊值
        Text("无聊值", color = Color(0xFF9A9AA4), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { boredom / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (boredom >= 60) Color(0xFFF0A050) else Color(0xFF6EC6F0),
            trackColor = Color(0xFF20202A)
        )
        Spacer(Modifier.height(4.dp))
        Text("$boredom / 100", color = Color(0xFF6E6E7A), fontSize = 11.sp)

        Spacer(Modifier.weight(1f))

        // Live2D 舞台
        OutlinedButton(
            onClick = { Live2DStageServer.openInBrowser() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("Live2D 舞台", color = Color(0xFF6EC6F0)) }

        // 设置按钮
        OutlinedButton(
            onClick = { showSettings = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("设置", color = Color(0xFF9A9AA4)) }
    }

    if (showSettings) {
        SettingsPanel(appState, configStore, onDismiss = { showSettings = false })
    }
}

@Composable
private fun ChatPanel(appState: AppState) {
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
                item { Text("白音正在输入…", color = Color(0xFF6E6E7A), fontSize = 13.sp, modifier = Modifier.padding(8.dp)) }
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
                placeholder = { Text("和白音说话…", color = Color(0xFF6E6E7A)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6EC6F0),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
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
                Icon(Icons.Default.Send, contentDescription = "发送", tint = Color(0xFF121217))
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
    // 托盘
    SystemTrayManager.onOpenWindow = { }
    SystemTrayManager.onExit = { exitApplication() }
    SystemTrayManager.install()

    // 加载窗口图标（从 resources）
    val windowIcon = remember { loadAppIcon() }

    Window(
        onCloseRequest = { exitApplication() },
        title = "小鲸鱼喜欢你",
        icon = windowIcon,
        state = rememberWindowState(size = DpSize(1100.dp, 720.dp))
    ) {
        App()
    }
    // 退出时清理托盘
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
private fun RightPanel(appState: AppState) {
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
            TabButton("行动账本", tab == 0) { tab = 0 }
            Spacer(Modifier.width(8.dp))
            TabButton("沙盒", tab == 1) { tab = 1; sandboxFiles = appState.sandbox.listFiles(appState.sandboxRoot) }
        }
        Spacer(Modifier.height(8.dp))

        if (tab == 0) {
            Button(
                onClick = {
                    val restored = appState.restoreAllActions()
                    restoredMsg = "已恢复 $restored 项操作"
                    entries.clear(); entries.addAll(appState.actionLedger.all())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF323840))
            ) { Text("一键恢复所有操作", color = Color(0xFF6EC6F0)) }
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
            ) { Text("刷新沙盒", color = Color(0xFF6EC6F0)) }
            Spacer(Modifier.height(8.dp))
            Text("沙盒位置：文档/小鲸鱼喜欢你/沙盒", color = Color(0xFF6E6E7A), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(sandboxFiles) { f ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A1A22),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Text(
                            if (f.name.startsWith(".白音藏起来的_")) "🔒 ${f.name.removePrefix(".白音藏起来的_")}（藏起来了）"
                            else "📄 ${f.name}",
                            color = Color(0xFFE8E8EC),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                        if (sandboxFiles.isEmpty()) {
                    item { Text("沙盒是空的", color = Color(0xFF8E8E9A), fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun LicenseNoticeScreen(onAccept: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121217)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⚠ 重要声明", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF6EC6F0), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text(
                buildString {
                    append("本软件（小鲸鱼喜欢你）基于以下开源项目构建：\n\n")
                    append("· 小鲸鱼喜欢你 Android 版\n")
                    append("· PocketTavern (Apache 2.0)\n")
                    append("· SillyTavern (AGPL-3.0)\n")
                    append("· Live2D Cubism SDK (Live2D 专有许可)\n")
                    append("· Compose Desktop (Apache 2.0)\n")
                    append("· OkHttp (Apache 2.0)\n")
                    append("· Kotlin 生态库\n\n")
                    append("严格禁止任何形式的商业使用、\n转售、打包、或以任何形式盈利。\n\n")
                    append("本软件不收集用户数据、\n不上传隐私信息。\n所有数据仅存储于你的设备本地。\n\n")
                    append("免责声明：\n按\"原样\"提供，作者不对 AI 内容、\n数据丢失或任何后果负责。\n\n")
                    append("继续使用即表示你已阅读并同意以上条款。")
                },
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
            ) { Text("我已阅读并同意，进入应用", modifier = Modifier.padding(vertical = 12.dp)) }
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
