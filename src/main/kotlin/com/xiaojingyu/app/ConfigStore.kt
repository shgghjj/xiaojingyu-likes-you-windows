package com.xiaojingyu.app

import com.xiaojingyu.app.girlfriend.SecureGirlfriendStorage
import com.xiaojingyu.app.model.ApiConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** 桌面版应用配置：数据目录、API 配置、设置。JSON 加密存储。 */
@Serializable
data class AppConfig(
    val apiKey: String = "",
    val chatCompletionSource: String = "deepseek",
    val customUrl: String = "",
    val currentModel: String = "deepseek-v4-flash",
    val girlfriendName: String = "白音",
    val petName: String = "老大",
    val proactiveEnabled: Boolean = false,
    val boredomThreshold: Int = 60,
    val autoActionEnabled: Boolean = false,
    val fullAutonomyEnabled: Boolean = false,
    val fileReadEnabled: Boolean = false,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash-exp",
    val quietHoursStart: Int = 23,  // 安静时段 23:00
    val quietHoursEnd: Int = 7,     // 到 7:00
    val dailyProactiveLimit: Int = 10,
    val ttsEnabled: Boolean = true,  // 语音朗读开关
    val authorizedDirs: List<String> = emptyList(),  // 用户授权的额外工作目录
    val licenseAccepted: Boolean = false  // 首次启动许可是否已接受
) {
    fun toApiConfiguration(): ApiConfiguration = ApiConfiguration(
        mainApi = "openai",
        textGenType = "koboldcpp",
        apiServer = "http://127.0.0.1:5001",
        chatCompletionSource = chatCompletionSource,
        customUrl = customUrl.ifBlank { null },
        apiKey = apiKey,
        currentModel = currentModel,
        availableModels = emptyList()
    )
}

/** 配置持久化：保存在用户目录 .xiaojingyu/config.json（加密）。 */
class ConfigStore(private val baseDir: File = File(System.getProperty("user.home"), ".xiaojingyu")) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val configFile: File get() = File(baseDir, "config.json")

    init { baseDir.mkdirs() }

    @Volatile
    private var cached: AppConfig = load()

    fun get(): AppConfig = cached

    @Synchronized
    fun update(transform: (AppConfig) -> AppConfig) {
        cached = transform(cached)
        save()
    }

    private fun save() {
        val text = json.encodeToString(AppConfig.serializer(), cached)
        SecureGirlfriendStorage.writeEncrypted(configFile, text)
    }

    private fun load(): AppConfig {
        return try {
            val text = SecureGirlfriendStorage.readEncrypted(configFile)
            if (text != null) json.decodeFromString(AppConfig.serializer(), text)
            else AppConfig()
        } catch (_: Exception) {
            AppConfig()
        }
    }

    /** 数据目录（聊天/记忆/沙盒） */
    val dataDir: File get() = File(baseDir, "data").apply { mkdirs() }
}
