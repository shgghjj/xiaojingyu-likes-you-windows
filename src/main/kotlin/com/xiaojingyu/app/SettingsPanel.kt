package com.xiaojingyu.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    val lang = config.language
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

    // 全屏设置层：覆盖整个窗口，标签栏固定顶部，内容可滚动，按钮固定底部
    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        // 设置面板全屏
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF14141A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("设置", color = Color(0xFFE8E8EC), fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    TextButton(onClick = onDismiss) {
                        Text(if (lang == "en") "✕ Close" else "✕ 关闭", color = Color(0xFF8E8E9A), fontSize = 13.sp)
                    }
                }

                // 标签栏（固定顶部）
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
                    TabButton(I18n.get("tab_api", lang), tab == 0) { tab = 0 }
                    Spacer(Modifier.width(8.dp))
                    TabButton(I18n.get("tab_jailbreak", lang), tab == 1) { tab = 1 }
                    Spacer(Modifier.width(8.dp))
                    TabButton(I18n.get("tab_memory", lang), tab == 2) { tab = 2 }
                }
                HorizontalDivider(color = Color(0xFF252530))

                // 内容区（可滚动）
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    when (tab) {
                        0 -> {
                            // 语言
                            Text(I18n.get("lang_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { language = "zh" }) {
                                    RadioButton(selected = language == "zh", onClick = { language = "zh" })
                                    Text(I18n.get("lang_zh", lang), color = Color(0xFFE8E8EC), fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(24.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { language = "en" }) {
                                    RadioButton(selected = language == "en", onClick = { language = "en" })
                                    Text("English", color = Color(0xFFE8E8EC), fontSize = 14.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 8.dp))

                            Text(I18n.get("api_key_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            OutlinedTextField(
                                apiKey, { apiKey = it },
                                Modifier.fillMaxWidth(), singleLine = true,
                                placeholder = { Text("sk-...", color = Color(0xFF6E6E7A)) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE8E8EC)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFE8E8EC),
                                    unfocusedTextColor = Color(0xFFE8E8EC),
                                    cursorColor = Color(0xFF6EC6F0),
                                    focusedBorderColor = Color(0xFF6EC6F0),
                                    unfocusedBorderColor = Color(0xFF252530),
                                    focusedContainerColor = Color(0xFF1A1A22),
                                    unfocusedContainerColor = Color(0xFF1A1A22)
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(I18n.get("model_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            // 模型下拉选择：主流模型全收录，可手输自定义
                            val allModels = listOf(
                                // DeepSeek
                                "deepseek-v4-flash", "deepseek-chat", "deepseek-reasoner", "deepseek-v3", "deepseek-coder", "deepseek-r1",
                                // OpenAI
                                "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo", "o1", "o1-mini", "o3-mini",
                                // Anthropic
                                "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229", "claude-3-sonnet-20240229",
                                // Google
                                "gemini-2.0-flash", "gemini-2.0-flash-exp", "gemini-1.5-pro", "gemini-1.5-flash",
                                // Meta / Llama
                                "llama-3.3-70b-instruct", "llama-3.1-405b-instruct", "llama-3.1-70b-instruct", "llama-3.1-8b-instruct",
                                // Mistral
                                "mistral-large-latest", "mistral-medium-latest", "mistral-small-latest",
                                // Qwen / 通义
                                "qwen2.5-72b-instruct", "qwen2.5-32b-instruct", "qwen2.5-7b-instruct",
                                // 其他
                                "grok-2", "grok-beta", "command-r-plus", "yi-large", "glm-4", "moonshot-v1-32k", "ernie-4.0", "doubao-pro"
                            )
                            var modelExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE8E8EC)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color(0xFFE8E8EC),
                                        unfocusedTextColor = Color(0xFFE8E8EC),
                                        cursorColor = Color(0xFF6EC6F0),
                                        focusedBorderColor = Color(0xFF6EC6F0),
                                        unfocusedBorderColor = Color(0xFF252530),
                                        focusedContainerColor = Color(0xFF1A1A22),
                                        unfocusedContainerColor = Color(0xFF1A1A22)
                                    )
                                )
                                // 透明点击层打开下拉
                                Box(
                                    modifier = Modifier.matchParentSize().clickable { modelExpanded = true }
                                )
                                DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                    allModels.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m, fontSize = 13.sp) },
                                            onClick = { model = m; modelExpanded = false }
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF252530))
                                    DropdownMenuItem(
                                        text = { Text(I18n.get("model_custom", lang), fontSize = 13.sp, color = Color(0xFF6EC6F0)) },
                                        onClick = { modelExpanded = false }
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            SwitchRow(I18n.get("proactive_label", lang), I18n.get("proactive_desc", lang), proactive) { proactive = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            if (isDeepSeek) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text(I18n.get("tts_label", lang), color = Color(0xFF6E6E7A), fontSize = 14.sp)
                                        Text(I18n.get("tts_deepseek", lang), color = Color(0xFF5E5E6A), fontSize = 11.sp)
                                    }
                                    Switch(checked = false, enabled = false, onCheckedChange = {})
                                }
                            } else {
                                SwitchRow(I18n.get("tts_label", lang), I18n.get("tts_desc", lang), ttsEnabled) { ttsEnabled = it }
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow(I18n.get("file_read_label", lang), I18n.get("file_read_desc", lang), fileRead) {
                                if (it) showFileConfirm = true else fileRead = false
                            }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow(I18n.get("auto_action_label", lang), I18n.get("auto_action_desc", lang), autoAction) { autoAction = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))
                            SwitchRow(I18n.get("auto_full_label", lang), I18n.get("auto_full_desc", lang), fullAutonomy) { fullAutonomy = it }
                            HorizontalDivider(color = Color(0xFF252530), modifier = Modifier.padding(vertical = 6.dp))

                            Text(I18n.get("gemini_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text(I18n.get("gemini_hint", lang), color = Color(0xFF6E6E7A)) },
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE8E8EC)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFE8E8EC),
                                    unfocusedTextColor = Color(0xFFE8E8EC),
                                    cursorColor = Color(0xFF6EC6F0),
                                    focusedBorderColor = Color(0xFF6EC6F0),
                                    unfocusedBorderColor = Color(0xFF252530),
                                    focusedContainerColor = Color(0xFF1A1A22),
                                    unfocusedContainerColor = Color(0xFF1A1A22)
                                )
                            )
                            Spacer(Modifier.height(14.dp))

                            Text(I18n.get("auth_dir_label", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(authDirInput, { authDirInput = it }, Modifier.weight(1f), singleLine = true)
                                Spacer(Modifier.width(6.dp))
                                Button(onClick = { if (authDirInput.isNotBlank()) { authDirs = authDirs + authDirInput.trim(); authDirInput = "" } }) { Text(I18n.get("auth_dir_add", lang), fontSize = 12.sp) }
                            }
                            authDirs.forEach { dir ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).background(Color(0xFF1A1A20), MaterialTheme.shapes.small)) {
                                    Text(dir, color = Color(0xFF9A9AA4), fontSize = 12.sp, modifier = Modifier.weight(1f).padding(8.dp))
                                    TextButton(onClick = { authDirs = authDirs - dir }) { Text(I18n.get("auth_dir_remove", lang), fontSize = 11.sp, color = Color(0xFFE54860)) }
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
                            Text(I18n.get("jailbreak_custom", lang), color = Color(0xFFE8E8EC), fontSize = 14.sp)
                            OutlinedTextField(customJailbreak, { customJailbreak = it }, Modifier.fillMaxWidth().height(120.dp), placeholder = { Text(I18n.get("jailbreak_paste", lang)) })
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = {
                                    val d = FileDialog(java.awt.Frame(), I18n.get("jailbreak_file_dialog", lang), FileDialog.LOAD)
                                    d.file = "*.txt"; d.isVisible = true
                                    if (d.file != null) try { customJailbreak = File(d.directory, d.file).readText().take(5000) } catch (_: Exception) {}
                                }, modifier = Modifier.height(32.dp)) { Text(I18n.get("jailbreak_import", lang), fontSize = 12.sp) }
                            }
                        }
                        2 -> {
                            Text(I18n.get("memory_facts", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Button(onClick = { appState.clearFacts(); facts = appState.factsSnapshot() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A303A))) {
                                Text(I18n.get("memory_clear_facts", lang), color = Color(0xFFE54860), fontSize = 12.sp)
                            }
                            facts.forEach { Text("· $it", color = Color(0xFFE8E8EC), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                            if (facts.isEmpty()) Text(I18n.get("memory_empty", lang), color = Color(0xFF8E8E9A), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(I18n.get("memory_memories", lang), color = Color(0xFF9A9AA4), fontSize = 12.sp)
                            Button(onClick = { appState.clearMemories(); memories = appState.memoriesSnapshot() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A303A))) {
                                Text(I18n.get("memory_clear_memories", lang), color = Color(0xFFE54860), fontSize = 12.sp)
                            }
                            memories.forEach { Text("· $it", color = Color(0xFFE8E8EC), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                            if (memories.isEmpty()) Text(I18n.get("memory_empty", lang), color = Color(0xFF8E8E9A), fontSize = 12.sp)
                        }
                    }
                }

                // 底部按钮（固定底部）
                HorizontalDivider(color = Color(0xFF252530))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(I18n.get("cancel", lang), color = Color(0xFF8E8E9A)) }
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
                    }) { Text(I18n.get("save", lang), color = Color(0xFF6EC6F0)) }
                }
            }
        }
    }

    if (showFileConfirm) {
        AlertDialog(
            onDismissRequest = { showFileConfirm = false },
            title = { Text(I18n.get("error_file_confirm_title", lang), color = Color(0xFFE8E8EC)) },
            text = { Text(I18n.get("error_file_confirm_body", lang), color = Color(0xFF9A9AA4)) },
            confirmButton = { TextButton(onClick = { fileRead = true; showFileConfirm = false }) { Text(I18n.get("error_file_confirm_ok", lang), color = Color(0xFF6EC6F0)) } },
            dismissButton = { TextButton(onClick = { showFileConfirm = false }) { Text(I18n.get("cancel", lang), color = Color(0xFF8E8E9A)) } },
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
        modifier = Modifier.height(32.dp)
    ) { Text(label, fontSize = 12.sp) }
}