package com.xiaojingyu.app

import java.awt.AWTException
import java.awt.Image
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.PopupMenu
import java.awt.MenuItem
import java.awt.event.ActionListener

/**
 * 系统托盘（Windows 桌面体验）。
 * - 最小化到托盘常驻
 * - 右键菜单：打开/停止主动消息/退出
 */
object SystemTrayManager {

    private var trayIcon: TrayIcon? = null

    var onOpenWindow: (() -> Unit)? = null
    var onToggleProactive: (() -> Unit)? = null
    var onExit: (() -> Unit)? = null

    fun install(): Boolean {
        if (!SystemTray.isSupported()) return false
        try {
            val tray = SystemTray.getSystemTray()
            val image = createTrayImage()
            val popup = PopupMenu()

            val openItem = MenuItem("打开小鲸鱼").apply {
                addActionListener(ActionListener { onOpenWindow?.invoke() })
            }
            val proactiveItem = MenuItem("切换主动消息").apply {
                addActionListener(ActionListener { onToggleProactive?.invoke() })
            }
            val exitItem = MenuItem("退出").apply {
                addActionListener(ActionListener { onExit?.invoke() })
            }
            popup.add(openItem)
            popup.add(proactiveItem)
            popup.addSeparator()
            popup.add(exitItem)

            val icon = TrayIcon(image, "小鲸鱼喜欢你").apply {
                isImageAutoSize = true
                popupMenu = popup
                addActionListener(ActionListener { onOpenWindow?.invoke() })
            }
            tray.add(icon)
            trayIcon = icon
            return true
        } catch (_: AWTException) { return false }
    }

    fun remove() {
        try {
            trayIcon?.let { SystemTray.getSystemTray().remove(it) }
            trayIcon = null
        } catch (_: Exception) {}
    }

    private fun createTrayImage(): Image {
        // 用文字画一个简单的鲸鱼图标（无外部资源）
        val size = 32
        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(0x6EC6F0)
        g.fillOval(4, 8, 24, 16)
        g.fillOval(2, 14, 6, 8)
        g.fillOval(24, 14, 6, 8)
        g.color = java.awt.Color(0x0A0A0E)
        g.fillOval(10, 12, 3, 4)
        g.fillOval(19, 12, 3, 4)
        g.dispose()
        return image
    }
}
