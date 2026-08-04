package com.xiaojingyu.app

import com.xiaojingyu.app.model.ApiConfiguration
import com.xiaojingyu.app.model.PromptMessage
import com.xiaojingyu.app.model.StreamEvent
import com.xiaojingyu.app.model.effectiveBaseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 桌面版 LLM 客户端。
 * 封装 OpenAI 兼容 chat completions 流式调用（DeepSeek/OpenAI/Ollama 等）。
 */
object LlmClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 流式生成。返回 Token/ThinkingToken/Complete/Error 事件流。 */
    fun generateStream(
        messages: List<PromptMessage>,
        config: ApiConfiguration,
        maxTokens: Int = 1024,
        temperature: Double? = null,
        showThoughts: Boolean = false
    ): Flow<StreamEvent> = flow {
        val baseUrl = config.effectiveBaseUrl.trimEnd('/')
        val body = buildString {
            append("{\"model\":\"${config.currentModel}\",\"messages\":[")
            append(messages.joinToString(",") { m ->
                """{"role":"${m.role}","content":"${m.content.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
            })
            append("],\"stream\":true,\"max_tokens\":$maxTokens")
            if (temperature != null) append(",\"temperature\":$temperature")
            append("}")
        }
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .also { if (config.apiKey.isNotBlank()) it.addHeader("Authorization", "Bearer ${config.apiKey}") }
            .build()

        val accumulated = StringBuilder()
        val thinking = StringBuilder()
        try {
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    emit(StreamEvent.Error("API ${response.code}: ${err.take(200)}"))
                    return@use
                }
                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val chunk = json.parseToJsonElement(data).jsonObject
                            val delta = chunk["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                                ?.get("delta")?.jsonObject ?: continue
                            val reasoning = delta["reasoning_content"]?.jsonPrimitive?.contentOrNull
                            if (reasoning != null && reasoning.isNotEmpty() && showThoughts) {
                                thinking.append(reasoning)
                                emit(StreamEvent.ThinkingToken(reasoning, thinking.toString()))
                            }
                            val token = delta["content"]?.jsonPrimitive?.contentOrNull ?: continue
                            if (token.isEmpty()) continue
                            accumulated.append(token)
                            emit(StreamEvent.Token(token, accumulated.toString()))
                        } catch (_: Exception) { /* skip malformed */ }
                    }
                }
            }
            emit(StreamEvent.Complete(accumulated.toString(), thinking.toString()))
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "网络错误"))
        }
    }.flowOn(Dispatchers.IO)

    /** 非流式一次调用，返回完整文本（记忆整理/主动消息检查用）。 */
    suspend fun generateOnce(
        messages: List<PromptMessage>,
        config: ApiConfiguration,
        maxTokens: Int = 512,
        temperature: Double = 0.4
    ): String {
        val baseUrl = config.effectiveBaseUrl.trimEnd('/')
        val body = buildString {
            append("{\"model\":\"${config.currentModel}\",\"messages\":[")
            append(messages.joinToString(",") { m ->
                """{"role":"${m.role}","content":"${m.content.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
            })
            append("],\"stream\":false,\"max_tokens\":$maxTokens,\"temperature\":$temperature}")
        }
        val req = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .also { if (config.apiKey.isNotBlank()) it.addHeader("Authorization", "Bearer ${config.apiKey}") }
            .build()
        return try {
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@use ""
                val obj = json.parseToJsonElement(response.body?.string() ?: "").jsonObject
                obj["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
            }
        } catch (_: Exception) { "" }
    }
}
