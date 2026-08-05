package com.xiaojingyu.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.FileDialog
import java.io.File

@Composable
fun SettingsPanel(appState: AppState, configStore: ConfigStore, onDismiss: () -> Unit) {
    val config by appState.config.collectAsState()
    val isDeepSeek = config.chatCompletionSource.isBlank() || config.chatCompletionSource.lowercase() == "deepseek"
        || config.currentModel.lowercase().contains("deepseek")
    var tab by remember { mutableStateOf(0) }

    var apiKey by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.currentModel) }
    var proactive by remember { mutableStateOf(config.proactiveEnabled) }
    var fileRead by remember { mutableStateOf(config.fileReadEnabled) }
    var showFileConfirm by remember { mutableStateOf(false) }
    var geminiKey by remember { mutableStateOf(config.geminiApiKey) }
    var ttsEnabled by remember { mutableStateOf(config.ttsEnabled && !isDeepSeek) }
    var language by remember { mutableStateOf(config.language) }
    var autoAction by remember { mutableStateOf(config.autoActionEnabled) }
    var fullAutonomy by remember { mutableStateOf(config.fullAutonomyEnabled) }

    var jailbreakId by remember { mutableStateOf(appState.currentJailbreakId()) }
    var customJailbreak by remember { mutableStateOf("") }
    var authDirInput by remember { mutableStateOf("") }
    var authDirs by remember { mutableStateOf(config.authorizedDirs) }

    var facts by remember { mutableStateOf(appState.factsSnapshot()) }
    var memories by remember { mutableStateOf(appState.memoriesSnapshot()) }

    // 全屏覆盖层
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14141A),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp, 16.dp, 24.dp, 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("设置", color = Color(0xFFE8E8EC), fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    TextButton(onClick = onDismiss) { Text("✕", color = Color(0xFF8E8E9A), fontSize = 16.sp) }
                }

                // 标签栏
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
                    TabButton(I18n.get("tab_api", config.language), tab == 0) { tab = 0 }
                    Spacer(Modifier.width(6.dp))
                    TabButton(I18n.get("tab_jailbreak", config.language), tab == 1) { tab = 1 }
                    Spacer(Modifier.width(6.dp))
                    TabButton(I18n.get("tab_memory", config.language), tab == 2) { tab = 2 }
                }
                HorizontalDivider(color = Color(0xFF252530))

                // 内容区
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    when (tab) {
                        0 -> {
                            Text("界面语言 / Language", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { language = "zh" }) {
                                    RadioButton(selected = language == "zh", onClick = { language = "zh" })
                                    Text("中文", color = Color(0xFFE8E8EC), fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(20.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { language = "en" }) {
                                    RadioButton(selected = language == "en", onClick = { language = "en" })
                                    Text("English", color = Color(0xFFE8E8EC), fontSize = 14.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 8.dp))

                            Text("DeepSeek API Key", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("sk-...") })
                            Spacer(Modifier.height(10.dp))
                            Text("模型", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(14.dp))

                            SwitchRow("主动联系", "无聊时主动给你发消息", proactive) { proactive = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            if (isDeepSeek) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text("语音朗读（TTS）", color = Color(0xFF6E6E7A), fontSize = 14.sp)
                                        Text("当前 DeepSeek API 不支持语音朗读", color = Color(0xFF5E5E6A), fontSize = 11.sp)
                                    }
                                    Switch(checked = false, enabled = false, onCheckedChange = {})
                                }
                            } else {
                                SwitchRow("语音朗读（TTS）", "白音回复时自动朗读", ttsEnabled) { ttsEnabled = it }
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow("读取我的文件", "开启后她可读取你电脑上文本文件", fileRead) {
                                if (it) showFileConfirm = true else fileRead = false
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow("自动行动", "无聊时自主做低风险事", autoAction) { autoAction = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow("完全自主模式", "像编码助手一样自主行动", fullAutonomy) { fullAutonomy = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))

                            Text("Gemini Vision API Key", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            OutlinedTextField(geminiKey, { geminiKey = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("AIza...") })
                            Spacer(Modifier.height(14.dp))

                            Text("授权工作目录", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(authDirInput, { authDirInput = it }, Modifier.weight(1f), singleLine = true)
                                Spacer(Modifier.width(6.dp))
                                Button(onClick = { if (authDirInput.isNotBlank()) { authDirs = authDirs + authDirInput; authDirInput = "" } }) { Text("添加", fontSize = 12.sp) }
                            }
                            authDirs.forEach { dir ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).background(Color(0xFF1A1A20), MaterialTheme.shapes.small)) {
                                    Text(dir, color = Color(0xFF9A9AA4), fontSize = 12.sp, modifier = Modifier.weight(1f).padding(8.dp))
                                    TextButton(onClick = { authDirs = authDirs - dir }) { Text("移除", fontSize = 11.sp, color = Color(0xFFE54860)) }
                                }
                            }
                        }
                        1 -> {
                            appState.jailbreakPresets().forEach { preset ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { jailbreakId = preset.id }.padding(vertical = 2.dp)) {
                                    RadioButton(selected = jailbreakId == preset.id, onClick = { jailbreakId = preset.id })
                                    Column {
                                        Text(preset.label, color = Color(0xFFE8E8EC), fontSize = 14.sp)
                                        Text(preset.description, color = Color(0xFF8E8E9A), fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("自定义词库", color = Color(0xFFE8E8EC), fontSize = 14.sp)
                            OutlinedTextField(customJailbreak, { customJailbreak = it }, Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("粘贴你的破甲词库内容…") })
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = {
                                    val d = FileDialog(java.awt.Frame(), "选择破甲词库文件", FileDialog.LOAD)
                                    d.file = "*.txt"; d.isVisible = true
                                    if (d.file != null) try { customJailbreak = File(d.directory, d.file).readText().take(5000) } catch (_: Exception) {}
                                }, modifier = Modifier.height(32.dp)) { Text(I18n.get("jailbreak_import", config.language), fontSize = 12.sp) }
                            }
                        }
                        2 -> {
                            Text("她记得的事（档案）", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Button(onClick = { appState.clearFacts(); facts = appState.factsSnapshot() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A303A))) {
                                Text(I18n.get("memory_clear_facts", config.language), color = Color(0xFFE54860), fontSize = 12.sp)
                            }
                            facts.forEach { Text("· $it", color = Color(0xFFE8E8EC), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                            if (facts.isEmpty()) Text(I18n.get("memory_empty", config.language), color = Color(0xFF8E8E9A), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("共同回忆", color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Button(onClick = { appState.clearMemories(); memories = appState.memoriesSnapshot() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A303A))) {
                                Text(I18n.get("memory_clear_memories", config.language), color = Color(0xFFE54860), fontSize = 12.sp)
                            }
                            memories.forEach { Text("· $it", color = Color(0xFFE8E8EC), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                            if (memories.isEmpty()) Text(I18n.get("memory_empty", config.language), color = Color(0xFF8E8E9A), fontSize = 12.sp)
                        }
                    }
                }

                // 底部按钮
                HorizontalDivider(color = Color(0xFF252530))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(I18n.get("cancel", config.language), color = Color(0xFF8E8E9A)) }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = {
                        if (jailbreakId == "custom" && customJailbreak.isNotBlank()) appState.saveCustomJailbreak(customJailbreak)
                        else appState.setJailbreak(jailbreakId)
                        appState.updateConfig {
                            it.copy(
                                apiKey = apiKey.trim(),
                                currentModel = model.trim().ifBlank { it.currentModel },
                                proactiveEnabled = proactive, fileReadEnabled = fileRead,
                                geminiApiKey = geminiKey.trim(),
                                ttsEnabled = if (isDeepSeek) false else ttsEnabled,
                                autoActionEnabled = autoAction, fullAutonomyEnabled = fullAutonomy,
                                authorizedDirs = authDirs,
                                language = language
                            )
                        }
                        onDismiss()
                    }) { Text(I18n.get("save", config.language), color = Color(0xFF6EC6F0)) }
                }
            }
        }
    }
    // 保存逻辑在下面
    // ... existing save code in confirmButton ...

    if (showFileConfirm) {
        AlertDialog(
            onDismissRequest = { showFileConfirm = false },
            title = { Text("允许白音读取你的文件？", color = Color(0xFFE8E8EC)) },
            text = { Text("开启后她可以读取你电脑上任何位置的文本文件。\n\n随时可在设置中一键关闭。", color = Color(0xFF9A9AA4)) },
            confirmButton = { TextButton(onClick = { fileRead = true; showFileConfirm = false }) { Text("确认开启", color = Color(0xFF6EC6F0)) } },
            dismissButton = { TextButton(onClick = { showFileConfirm = false }) { Text(I18n.get("cancel", config.language), color = Color(0xFF8E8E9A)) } },
            containerColor = Color(0xFF14141A)
        )
    }
}

@Composable
private fun SwitchRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color(0xFFE8E8EC), fontSize = 14.sp)
            Text(desc, color = Color(0xFF8E8E9A), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF1A1A24) else Color.Transparent,
            contentColor = if (selected) Color(0xFF6EC6F0) else Color(0xFF8E8E9A)
        ),
        modifier = Modifier.height(30.dp)
    ) { Text(label, fontSize = 12.sp) }
}