package com.xiaojingyu.app.model

/**
 * 桌面版回复清洗：
 * - 剥离 JSON 包装（{text, avatar, voice} → 只取 text）
 * - 剥离思考/分析残留、角色卡文件名前缀、场景构建
 */
object StructuredReplyParserDesktop {

    fun sanitize(fullText: String): String {
        if (fullText.isBlank()) return fullText
        // 手动提取 text 字段（不用正则/JSON解析，避免转义和特殊字符问题）
        val key = "\"text\""
        val idx = fullText.indexOf(key)
        if (idx < 0) return cleanPrefix(fullText)
        var pos = idx + key.length
        // 跳过冒号和空白
        while (pos < fullText.length && (fullText[pos] == ' ' || fullText[pos] == ':')) pos++
        if (pos >= fullText.length || fullText[pos] != '\"') return cleanPrefix(fullText)
        pos++ // 跳过开引号
        val sb = StringBuilder()
        while (pos < fullText.length) {
            if (fullText[pos] == '\\' && pos + 1 < fullText.length) {
                pos++
                sb.append(fullText[pos])
            } else if (fullText[pos] == '\"') {
                break // 闭引号
            } else {
                sb.append(fullText[pos])
            }
            pos++
        }
        val text = sb.toString().trim()
        return if (text.isBlank()) cleanPrefix(fullText) else cleanPrefix(text)
    }

    private fun cleanPrefix(text: String): String {
        var result = text.trim()
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
}
