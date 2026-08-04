package com.xiaojingyu.app.plugin

import com.xiaojingyu.app.ActionLedger
import com.xiaojingyu.app.AppConfig
import com.xiaojingyu.app.FileSandbox
import kotlinx.serialization.Serializable

/**
 * 插件系统核心接口（Windows 版预留扩展能力）。
 *
 * 任何新玩法（联机游戏、新工具、新恶作剧）都通过实现 [XiaojingyuPlugin] 注册，
 * 核心框架不需要改动。
 */
interface XiaojingyuPlugin {
    val id: String
    val name: String
    val description: String
    val version: String

    /** 插件启用时调用（可注册命令、菜单、定时任务） */
    fun onEnable(context: PluginContext)

    /** 插件禁用时调用（清理资源） */
    fun onDisable()
}

/** 插件上下文：提供给插件的核心能力入口 */
class PluginContext(
    val ledger: ActionLedger,
    val sandbox: FileSandbox,
    val config: () -> AppConfig,
    val commandExecutor: com.xiaojingyu.app.CommandExecutor
)

/** 插件元信息（注册表用） */
@Serializable
data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val enabled: Boolean = false
)

/** 插件注册表 */
object PluginRegistry {

    private val plugins = mutableMapOf<String, XiaojingyuPlugin>()
    private val enabledPlugins = mutableSetOf<String>()

    fun register(plugin: XiaojingyuPlugin) {
        plugins[plugin.id] = plugin
    }

    fun unregister(id: String) {
        plugins.remove(id)?.onDisable()
        enabledPlugins.remove(id)
    }

    fun all(): List<XiaojingyuPlugin> = plugins.values.toList()

    fun infos(): List<PluginInfo> = plugins.values.map {
        PluginInfo(it.id, it.name, it.description, it.version, it.id in enabledPlugins)
    }

    fun enable(id: String, context: PluginContext): Boolean {
        val plugin = plugins[id] ?: return false
        runCatching { plugin.onEnable(context) }
        enabledPlugins.add(id)
        return true
    }

    fun disable(id: String): Boolean {
        val plugin = plugins[id] ?: return false
        runCatching { plugin.onDisable() }
        enabledPlugins.remove(id)
        return true
    }

    fun isEnabled(id: String): Boolean = id in enabledPlugins
}

/**
 * 预留玩法接口：游戏会话（如联机我的世界）。
 * 后续实现对应插件时实现此接口。
 */
interface GameSessionPlugin : XiaojingyuPlugin {
    /** 启动一个游戏会话 */
    fun startSession(gameName: String, args: Map<String, String>): String
    /** 获取当前会话状态 */
    fun sessionStatus(): String
    /** 结束会话 */
    fun stopSession(): String
}
