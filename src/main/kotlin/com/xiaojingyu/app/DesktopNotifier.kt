package com.xiaojingyu.app

import java.io.File

/**
 * Windows 桌面通知（Toast）。
 * 通过 PowerShell + BurntToast 或 WinRT 弹系统通知。
 * 使用 WinRT ToastNotification（Windows 10+ 原生支持）。
 */
object DesktopNotifier {

    private val scriptCache = File(System.getProperty("java.io.tmpdir"), "xiaojingyu_toast.ps1")

    /** 弹出系统通知 */
    fun notify(title: String, message: String) {
        try {
            // 净化文本，杜绝 PowerShell 注入
            fun clean(s: String): String = s
                .replace("'", "")
                .replace("\"", "")
                .replace("\$", "")
                .replace("`", "")
                .replace("\r", " ").replace("\n", " ")
                .take(150)
            val t = clean(title)
            val m = clean(message)
            val script = """
                [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                [Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null
                ${'$'}template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
                ${'$'}textNodes = ${'$'}template.GetElementsByTagName("text")
                ${'$'}textNodes.Item(0).AppendChild(${'$'}template.CreateTextNode('$t')) | Out-Null
                ${'$'}textNodes.Item(1).AppendChild(${'$'}template.CreateTextNode('$m')) | Out-Null
                ${'$'}toast = New-Object Windows.UI.Notifications.ToastNotification ${'$'}template
                [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("小鲸鱼喜欢你").Show(${'$'}toast)
            """.trimIndent()
            scriptCache.writeText(script)
            val proc = ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", scriptCache.absolutePath
            ).redirectErrorStream(true).start()
            proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) { /* 通知失败不影响主功能 */ }
    }
}
