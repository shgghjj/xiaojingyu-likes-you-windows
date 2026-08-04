package com.xiaojingyu.app.model

/**
 * 结构化聊天消息（chat-completions API 用）。
 */
data class PromptMessage(
    val role: String,    // "system", "user", or "assistant"
    val content: String,
    val imageDataUrl: String? = null
)
