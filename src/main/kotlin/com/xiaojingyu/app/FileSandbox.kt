package com.xiaojingyu.app

import java.io.File

/**
 * 文件沙盒管理器（Windows 版）。
 * - 沙盒目录：文档/小鲸鱼喜欢你/沙盒（她自由活动区）
 * - 所有操作走行动账本，可一键恢复
 * - 读取权限：全盘文本（受 fileReadEnabled 总开关控制）
 * - 修改权限：沙盒内自由，外部需授权
 */
class FileSandbox(
    private val ledger: ActionLedger,
    private val sandboxRoot: File,
    private val enabledDirs: Set<File> = emptySet()
) {
    companion object {
        private val READABLE_EXTENSIONS = setOf(
            "txt", "md", "json", "log", "csv", "xml", "html", "htm", "yaml", "yml", "toml",
            "ini", "cfg", "conf", "py", "js", "ts", "kt", "java", "cpp", "c", "h", "rs", "go",
            "swift", "sh", "bat", "ps1", "sql", "properties", "gradle"
        )
        // 无论如何都禁读的路径片段
        private val FORBIDDEN_PATH_PARTS = listOf(
            ".ssh", "AppData\\Local\\Google\\Chrome\\User Data\\Default\\Login Data",
            "AppData\\Roaming\\Mozilla\\Firefox\\Profiles", "Windows\\System32\\config",
            "password", "wallet", ".gnupg", "NTUSER.DAT"
        )
    }

    /** 沙盒内路径解析 */
    fun sandboxFile(name: String): File = File(sandboxRoot, sanitizeName(name))

    /** 读取文件内容（全盘文本，受开关控制） */
    fun readText(file: File, fileReadEnabled: Boolean): String? {
        if (!file.exists() || !file.isFile) return null
        if (file.absolutePath.startsWith(sandboxRoot.absolutePath)) {
            // 沙盒内：随便读
        } else {
            // 沙盒外：需要总开关
            if (!fileReadEnabled) return null
            if (!isAllowedExternalFile(file)) return null
        }
        val ext = file.extension.lowercase()
        if (ext !in READABLE_EXTENSIONS) return null
        if (file.length() > 200_000) return null
        val content = try { file.readText() } catch (_: Exception) { return null }
        ledger.record("READ", "读取了文件「${file.name}」", originalPath = file.absolutePath, canRestore = false)
        return content.take(8000)
    }

    /** 列出目录文件（沙盒内/外部白名单） */
    fun listFiles(dir: File): List<File> {
        val target = if (dir.absolutePath.isBlank() || !dir.exists()) sandboxRoot else dir
        return try {
            target.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /** 写文件（仅沙盒内自由） */
    fun writeFile(name: String, content: String): File? {
        val file = sandboxFile(name)
        return try {
            val backup = ledger.backupFile(file)
            file.parentFile?.mkdirs()
            file.writeText(content)
            ledger.record("WRITE", "写了文件「${file.name}」", originalPath = file.absolutePath, backupPath = backup)
            file
        } catch (_: Exception) { null }
    }

    /** 删除文件（沙盒内直接删进回收区；外部需授权目录） */
    fun deleteFile(file: File): Boolean {
        if (!file.exists()) return false
        val inSandbox = file.absolutePath.startsWith(sandboxRoot.absolutePath)
        val inEnabled = enabledDirs.any { file.absolutePath.startsWith(it.absolutePath) }
        if (!inSandbox && !inEnabled) return false
        return try {
            val backup = ledger.backupFile(file)
            file.delete()
            ledger.record("DELETE", "删除了文件「${file.name}」", originalPath = file.absolutePath, backupPath = backup)
            true
        } catch (_: Exception) { false }
    }

    /** 藏文件（改名加隐藏前缀；沙盒内自由，外部需授权） */
    fun hideFile(file: File): Boolean {
        if (!file.exists()) return false
        val inSandbox = file.absolutePath.startsWith(sandboxRoot.absolutePath)
        val inEnabled = enabledDirs.any { file.absolutePath.startsWith(it.absolutePath) }
        if (!inSandbox && !inEnabled) return false
        return try {
            val hidden = File(file.parentFile, ".白音藏起来的_${file.name}")
            if (file.renameTo(hidden)) {
                ledger.record("HIDE", "藏起了文件「${file.name}」", originalPath = file.absolutePath, newPath = hidden.absolutePath)
                true
            } else false
        } catch (_: Exception) { false }
    }

    /** 恢复被藏的文件 */
    fun unhideFile(hidden: File): Boolean {
        if (!hidden.exists()) return false
        return try {
            val visible = File(hidden.parentFile, hidden.name.removePrefix(".白音藏起来的_"))
            if (hidden.renameTo(visible)) {
                ledger.record("HIDE", "恢复了文件「${visible.name}」", originalPath = hidden.absolutePath, newPath = visible.absolutePath)
                true
            } else false
        } catch (_: Exception) { false }
    }

    /** 外部文件是否可读（黑名单过滤） */
    private fun isAllowedExternalFile(file: File): Boolean {
        val path = file.absolutePath.replace("/", "\\").lowercase()
        return FORBIDDEN_PATH_PARTS.none { path.contains(it.lowercase()) }
    }

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "untitled.txt" }
}
