package com.xiaojingyu.app

import com.xiaojingyu.app.girlfriend.SecureGirlfriendStorage
import com.xiaojingyu.app.model.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** 一条聊天记录（加密存储用）。 */
@Serializable
data class StoredMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val reasoning: String? = null,
    val imagePath: String? = null
)

/** 聊天记录存储：data/chats.json（加密 JSON）。 */
class ChatStore(private val dataDir: File) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val chatFile: File get() = File(dataDir, "chats.json")

    @Volatile
    private var messages: List<StoredMessage> = load()

    fun all(): List<StoredMessage> = synchronized(this) { messages }

    @Synchronized
    fun add(content: String, isUser: Boolean, reasoning: String? = null, imagePath: String? = null) {
        messages = messages + StoredMessage(content, isUser, System.currentTimeMillis(), reasoning, imagePath)
        save()
    }

    @Synchronized
    fun clear() {
        messages = emptyList()
        save()
    }

    fun asChatMessages(): List<ChatMessage> = synchronized(this) {
        messages.map {
            ChatMessage(content = it.content, isUser = it.isUser, timestamp = java.time.Instant.ofEpochMilli(it.timestamp), reasoning = it.reasoning)
        }
    }

    private fun save() {
        val text = json.encodeToString(ListSerializer(StoredMessage.serializer()), messages)
        SecureGirlfriendStorage.writeEncrypted(chatFile, text)
    }

    private fun load(): List<StoredMessage> {
        return try {
            val text = SecureGirlfriendStorage.readEncrypted(chatFile)
            if (text != null) json.decodeFromString(ListSerializer(StoredMessage.serializer()), text)
            else emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
