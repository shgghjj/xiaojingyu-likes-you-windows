package com.xiaojingyu.app.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 桌面版回复清洗：
 * - 剥离 JSON 包装（{text, avatar, voice} → 只取 text）
 * - 剥离思考/分析残留、角色卡文件名前缀、场景构建
 */
object StructuredReplyParserDesktop {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun sanitize(fullText: String): String {
        if (fullText.isBlank()) return fullText
        val text = extractJsonText(fullText) ?: fullText
        var result = text
            .replace(Regex("```(?:json|JSON)?\\s*"), "")
            .replace("```", "")
            .replace("**", "")
            .replace(Regex("^girlfriend_card\\.png\\s*[:：]\\s*"), "")
            .replace(Regex("^\\w+\\.png\\s*[:：]\\s*"), "")
            .trim()
        // 剥掉开头"好的，让我想想…"等思考腔
        val prefixes = listOf(
            "好的，让我想想", "好的让我想想", "让我想想", "让我想一下",
            "好的，我来", "我来想想", "首先，", "首先", "嗯，", "明白了，", "收到，"
        )
        for (p in prefixes) {
            if (result.startsWith(p)) {
                result = result.removePrefix(p).trimStart('，', '：', ',', '。', ' ')
                if (result.isNotBlank()) break
            }
        }
        return result.ifBlank { text }
    }

    private fun extractJsonText(fullText: String): String? {
        // 从尾部向前找 JSON 对象，取 text 字段
        var searchFrom = fullText.length
        repeat(5) {
            val end = fullText.lastIndexOf('}', searchFrom - 1)
            if (end < 0) return@repeat
            val start = fullText.lastIndexOf('{', end)
            if (start < 0) return@repeat
            searchFrom = start
            try {
                val obj = json.parseToJsonElement(fullText.substring(start, end + 1)).jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                if (text != null) return text.trim().takeIf { it.isNotEmpty() }
            } catch (_: Exception) { /* try next */ }
        }
        return null
    }
}
