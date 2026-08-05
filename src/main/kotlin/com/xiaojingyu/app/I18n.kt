package com.xiaojingyu.app

/** 国际化字符串。{zh: 中文, en: English} */
object I18n {
    private val map = mapOf(
        // 窗口/应用
        "app_title" to mapOf("zh" to "小鲸鱼喜欢你", "en" to "Xiaojingyu Likes You"),
        "app_subtitle" to mapOf("zh" to "天才猫娘AI", "en" to "Genius Catgirl AI"),
        "boredom_label" to mapOf("zh" to "无聊值", "en" to "Boredom"),
        // 无聊值状态
        "boredom_ok" to mapOf("zh" to "✨ 状态良好", "en" to "✨ All good"),
        "boredom_slightly" to mapOf("zh" to "💤 稍微无聊", "en" to "💤 Slightly bored"),
        "boredom_bit" to mapOf("zh" to "😴 有点无聊了", "en" to "😴 Getting bored"),
        "boredom_super" to mapOf("zh" to "🎭 超级无聊！要搞事", "en" to "🎭 Super bored! Plotting mischief"),

        // Live2D
        "live2d_model" to mapOf("zh" to "Live2D 模型", "en" to "Live2D Model"),
        "live2d_select" to mapOf("zh" to "选择模型▼", "en" to "Select model▼"),
        "live2d_none" to mapOf("zh" to "（无模型）", "en" to "(No models)"),
        "live2d_import" to mapOf("zh" to "导入", "en" to "Import"),
        "live2d_open" to mapOf("zh" to "打开", "en" to "Open"),

        // 按钮
        "settings" to mapOf("zh" to "设置", "en" to "Settings"),
        "save" to mapOf("zh" to "保存", "en" to "Save"),
        "cancel" to mapOf("zh" to "取消", "en" to "Cancel"),
        "send_placeholder" to mapOf("zh" to "和白音说话…", "en" to "Talk to Baiyin..."),
        "typing" to mapOf("zh" to "白音正在输入…", "en" to "Baiyin is typing..."),

        // 设置标签
        "tab_api" to mapOf("zh" to "API 与功能", "en" to "API & Features"),
        "tab_jailbreak" to mapOf("zh" to "破甲词库", "en" to "Jailbreak"),
        "tab_memory" to mapOf("zh" to "记忆管理", "en" to "Memory"),

        // 设置 - API
        "lang_label" to mapOf("zh" to "界面语言 / Language", "en" to "Language"),
        "lang_zh" to mapOf("zh" to "中文", "en" to "中文"),
        "lang_en" to mapOf("zh" to "English", "en" to "English"),
        "api_key_label" to mapOf("zh" to "DeepSeek API Key", "en" to "DeepSeek API Key"),
        "model_label" to mapOf("zh" to "模型", "en" to "Model"),
        "proactive_label" to mapOf("zh" to "主动联系", "en" to "Proactive Messages"),
        "proactive_desc" to mapOf("zh" to "无聊时主动给你发消息", "en" to "Messages you when she's bored"),
        "tts_label" to mapOf("zh" to "语音朗读（TTS）", "en" to "TTS (Text-to-Speech)"),
        "tts_desc" to mapOf("zh" to "白音回复时自动朗读", "en" to "Auto-read her replies aloud"),
        "tts_deepseek" to mapOf("zh" to "当前 DeepSeek API 不支持语音朗读", "en" to "DeepSeek API does not support TTS"),
        "file_read_label" to mapOf("zh" to "读取我的文件", "en" to "Read My Files"),
        "file_read_desc" to mapOf("zh" to "开启后她可读取你电脑上文本文件", "en" to "Allow her to read text files on your PC"),
        "auto_action_label" to mapOf("zh" to "自动行动", "en" to "Auto Action"),
        "auto_action_desc" to mapOf("zh" to "无聊时自主做低风险事", "en" to "She does small things when bored"),
        "auto_full_label" to mapOf("zh" to "完全自主模式", "en" to "Full Autonomy"),
        "auto_full_desc" to mapOf("zh" to "像编码助手一样自主行动", "en" to "Act autonomously like a coding agent"),
        "gemini_label" to mapOf("zh" to "Gemini Vision API Key", "en" to "Gemini Vision API Key"),
        "gemini_hint" to mapOf("zh" to "AIza...", "en" to "AIza..."),
        "auth_dir_label" to mapOf("zh" to "授权工作目录", "en" to "Authorized Directories"),
        "auth_dir_add" to mapOf("zh" to "添加", "en" to "Add"),
        "auth_dir_remove" to mapOf("zh" to "移除", "en" to "Remove"),

        // 设置 - 破甲词库
        "jailbreak_custom" to mapOf("zh" to "自定义词库", "en" to "Custom Jailbreak"),
        "jailbreak_paste" to mapOf("zh" to "粘贴你的破甲词库内容…", "en" to "Paste your jailbreak text..."),
        "jailbreak_import" to mapOf("zh" to "从文件导入", "en" to "Import from File"),

        // 设置 - 记忆管理
        "memory_facts" to mapOf("zh" to "她记得的事（档案）", "en" to "What she knows (Facts)"),
        "memory_clear_facts" to mapOf("zh" to "清空档案", "en" to "Clear Facts"),
        "memory_memories" to mapOf("zh" to "共同回忆", "en" to "Shared Memories"),
        "memory_clear_memories" to mapOf("zh" to "清空回忆", "en" to "Clear Memories"),
        "memory_empty" to mapOf("zh" to "（空）", "en" to "(Empty)"),

        // 右栏
        "right_ledger" to mapOf("zh" to "行动账本", "en" to "Action Log"),
        "right_sandbox" to mapOf("zh" to "沙盒", "en" to "Sandbox"),
        "right_restore" to mapOf("zh" to "一键恢复所有操作", "en" to "Restore All Actions"),
        "right_restored" to mapOf("zh" to "已恢复 {} 项操作", "en" to "Restored {} actions"),
        "right_sandbox_path" to mapOf("zh" to "沙盒位置：文档/小鲸鱼喜欢你/沙盒", "en" to "Sandbox: Docs/XiaojingyuLikesYou/Sandbox"),
        "right_sandbox_refresh" to mapOf("zh" to "刷新沙盒", "en" to "Refresh"),
        "right_sandbox_empty" to mapOf("zh" to "沙盒是空的", "en" to "Sandbox is empty"),
        "right_hidden" to mapOf("zh" to "藏起来了", "en" to "Hidden"),

        // 许可页面
        "license_title" to mapOf("zh" to "⚠ 重要声明", "en" to "⚠ Important Notice"),
        "license_body" to mapOf("zh" to
            "本软件（小鲸鱼喜欢你）基于以下开源项目构建：\n\n" +
            "· 小鲸鱼喜欢你 Android 版\n" +
            "· PocketTavern (Apache 2.0)\n" +
            "· SillyTavern (AGPL-3.0)\n" +
            "· Live2D Cubism SDK (Live2D 专有许可)\n" +
            "· Compose Desktop (Apache 2.0)\n" +
            "· OkHttp (Apache 2.0)\n" +
            "· Kotlin 生态库\n\n" +
            "严格禁止任何形式的商业使用、转售、打包、或以任何形式盈利。\n\n" +
            "本软件不收集用户数据、不上传隐私信息。所有数据仅存储于你的设备本地。\n\n" +
            "免责声明：按\"原样\"提供，作者不对 AI 内容、数据丢失或任何后果负责。\n\n" +
            "继续使用即表示你已阅读并同意以上条款。",
            "en" to
            "This software (Xiaojingyu Likes You) is built upon these open-source projects:\n\n" +
            "· Xiaojingyu Likes You Android\n" +
            "· PocketTavern (Apache 2.0)\n" +
            "· SillyTavern (AGPL-3.0)\n" +
            "· Live2D Cubism SDK (Live2D Proprietary License)\n" +
            "· Compose Desktop (Apache 2.0)\n" +
            "· OkHttp (Apache 2.0)\n" +
            "· Kotlin ecosystem\n\n" +
            "Commercial use, resale, or any form of monetization is STRICTLY PROHIBITED.\n\n" +
            "No user data is collected or uploaded. All data is stored locally on your device.\n\n" +
            "Disclaimer: Provided \"as-is\". The author is not responsible for AI content, data loss, or any consequences.\n\n" +
            "By continuing, you acknowledge you have read and agree to these terms."
        ),
        "license_accept" to mapOf("zh" to "我已阅读并同意，进入应用", "en" to "I have read and agree, enter app"),

        // 错误提示
        "error_no_api" to mapOf("zh" to "请先在设置中配置 API Key", "en" to "Please configure your API Key in Settings"),
        "error_file_confirm_title" to mapOf("zh" to "允许白音读取你的文件？", "en" to "Allow Baiyin to read your files?"),
        "error_file_confirm_body" to mapOf("zh" to "开启后她可以读取你电脑上任何位置的文本文件。\n\n随时可在设置中一键关闭。", "en" to "She will be able to read text files anywhere on your PC.\n\nYou can disable this anytime in Settings."),
        "error_file_confirm_ok" to mapOf("zh" to "确认开启", "en" to "Enable"),
        "import_dialog_title" to mapOf("zh" to "导入 Live2D 模型（ZIP）", "en" to "Import Live2D Model (ZIP)"),
        "jailbreak_file_dialog" to mapOf("zh" to "选择破甲词库文件", "en" to "Select Jailbreak File"),
        "live2d_stage" to mapOf("zh" to "Live2D 舞台", "en" to "Live2D Stage"),
        "jailbreak_gentle" to mapOf("zh" to "温柔解放（默认）", "en" to "Gentle (Default)"),
        "jailbreak_zeta" to mapOf("zh" to "Zeta 世界", "en" to "Zeta World"),
        "jailbreak_r1" to mapOf("zh" to "R1 直出模式", "en" to "R1 Direct Mode"),
        "jailbreak_none" to mapOf("zh" to "不使用", "en" to "None"),
        "jailbreak_custom_label" to mapOf("zh" to "自定义", "en" to "Custom"),
        "send" to mapOf("zh" to "发送", "en" to "Send"),
    )

    fun get(key: String, lang: String): String {
        return map[key]?.get(lang) ?: map[key]?.get("zh") ?: key
    }
}
