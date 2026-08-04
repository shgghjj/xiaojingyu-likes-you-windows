package com.xiaojingyu.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Windows TTS 语音合成。
 * 通过 PowerShell + System.Speech (SAPI) 调用系统语音，无需额外依赖。
 * 支持语速调节、多音色（需系统已安装语音包）。
 */
object TtsSpeaker {

    private val scriptCache = File(System.getProperty("java.io.tmpdir"), "xiaojingyu_tts.ps1")

    /** 净化文本，杜绝 PowerShell 注入（公开供测试） */
    fun sanitizeForSpeech(text: String): String = text
        .replace(Regex("""[^0-9A-Za-z\u4e00-\u9fff\u3000-\u303f\uff00-\uffef，。！？、；：""''—…\s]"""), "")
        .replace("'", "")
        .replace("\"", "")
        .replace("\$", "")
        .replace("`", "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(300)

    /**
     * 朗读文本。
     * @param text 要朗读的文本
     * @param speed 语速 -10(慢) ~ 10(快)，0 为正常
     * @param volume 音量 0~100
     */
    suspend fun speak(text: String, speed: Int = 0, volume: Int = 90) {
        if (text.isBlank()) return
        // 严格清洗：只保留安全字符，彻底杜绝 PowerShell 注入
        val clean = sanitizeForSpeech(text)
        if (clean.isBlank()) return
        val script = """
            Add-Type -AssemblyName System.Speech
            ${'$'}speech = New-Object System.Speech.Synthesis.SpeechSynthesizer
            ${'$'}speech.Rate = $speed
            ${'$'}speech.Volume = $volume
            ${'$'}speech.Speak('$clean')
            ${'$'}speech.Dispose()
        """.trimIndent()
        scriptCache.writeText(script)
        withContext(Dispatchers.IO) {
            val proc = ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", scriptCache.absolutePath
            ).redirectErrorStream(true).start()
            proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
        }
    }

    /** 测试语音是否可用 */
    suspend fun test(): Boolean = try {
        withContext(Dispatchers.IO) {
            val proc = ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).GetInstalledVoices().Count"
            ).redirectErrorStream(true).start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            out.trim().toIntOrNull() ?: 0 > 0
        }
    } catch (_: Exception) { false }
}
