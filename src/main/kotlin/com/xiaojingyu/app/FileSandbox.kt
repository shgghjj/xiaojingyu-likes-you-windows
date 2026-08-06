package com.xiaojingyu.app

import java.io.File

/**
 * 文件操作管理器（全盘版）。
 * - 白音可以在这个设备上的任意位置行动：读写、删除、改名、隐藏
 * - 所有修改类操作自动快照备份到 data/backups，可一键恢复
 * - 保留黑名单硬防护：无论何时都不碰系统配置/密码/SSH密钥等
 * - 默认工作区 Documents/小鲸鱼喜欢你/行动（她日常活动区，相对路径解析用）
 */
class FileSandbox(
    private val ledger: ActionLedger,
    private val defaultRoot: File,
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
            "NTUSER.DAT", "SAM", "SYSTEM", "SECURITY", "pagefile", "hiberfil",
            "bootmgr", "\\.git\\", "wallet", ".gnupg"
        )
        // 无论如何都不允许修改/删除的路径片段（全盘操作安全阀）
        private val FORBIDDEN_MODIFY_PARTS = listOf(
            "\\Windows\\", "Program Files", "\\System32", ".ssh", "AppData\\Local\\Google\\Chrome",
            "AppData\\Roaming\\Mozilla", "NTUSER.DAT", "System Volume Information",
            "\\\$Recycle.Bin", "\\Windows", "pagefile.sys", "hiberfil.sys",
            "Documents and Settings", "\\.git\\", "\\.svn\\", "\\.hg\\"
        )
    }

    /** 默认工作区内的相对路径解析 */
    fun sandboxFile(name: String): File = File(defaultRoot, sanitizeName(name))

    /** 是否是黑名单保护路径（任何操作都禁止） */
    private fun isForbidden(path: String): Boolean {
        val p = path.replace("/", "\\").lowercase()
        return FORBIDDEN_PATH_PARTS.any { p.contains(it.lowercase()) }
    }

    private fun canModify(file: File): Boolean {
        val path = file.absolutePath.replace("/", "\\").uppercase()
        return FORBIDDEN_MODIFY_PARTS.none { path.contains(it.uppercase()) }
    }

    /** 读取文件内容（全盘任意文本，受开关控制） */
    fun readText(file: File, fileReadEnabled: Boolean): String? {
        if (!file.exists() || !file.isFile) return null
        if (!fileReadEnabled) return null
        if (isForbidden(file.absolutePath)) return null
        val ext = file.extension.lowercase()
        if (ext !in READABLE_EXTENSIONS) return null
        if (file.length() > 200_000) return null
        val content = try { file.readText() } catch (_: Exception) { return null }
        ledger.record("READ", "读取了文件「${file.name}」", originalPath = file.absolutePath, canRestore = false)
        return content.take(8000)
    }

    /** 列出目录（任意目录可列）。返回 目录+文件 排序。 */
    fun listDir(dir: File): List<File> {
        val target = if (dir.exists() && dir.isDirectory) dir else defaultRoot
        return try {
            target.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /** 列出目录文件（保持旧接口） */
    fun listFiles(dir: File): List<File> {
        val target = if (dir.exists() && dir.isDirectory) dir else defaultRoot
        return try {
            target.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /** 写文件（工作区内，相对名字） */
    fun writeFile(name: String, content: String): File? {
        val file = sandboxFile(name)
        return write(file, content)
    }

    /** 写文件（全盘，任意路径） */
    fun writeFileAt(path: File, content: String): File? {
        if (isForbidden(path.absolutePath)) return null
        if (!canAccess(path)) return null
        return write(path, content)
    }

    private fun write(file: File, content: String): File? {
        return try {
            val existed = file.exists()
            val backup = if (existed) ledger.backupFile(file) else null
            if (existed && backup == null) return null // 已存在文件无法备份，拒绝写入
            file.parentFile?.mkdirs()
            file.writeText(content)
            ledger.record("WRITE", "写了文件「${file.name}」", originalPath = file.absolutePath, backupPath = backup)
            file
        } catch (_: Exception) { null }
    }

    /** 删除文件（全盘，改前快照） */
    fun deleteFile(file: File): Boolean {
        if (!file.exists()) return false
        if (file.isDirectory) return deleteDirectory(file)
        if (!canAccess(file)) return false
        return try {
            val backup = ledger.backupFile(file)
            if (backup == null) return false // 备份失败，拒绝删除
            file.delete()
            ledger.record("DELETE", "删除了文件「${file.name}」", originalPath = file.absolutePath, backupPath = backup)
            true
        } catch (_: Exception) { false }
    }

    private fun deleteDirectory(dir: File): Boolean {
        if (dir.listFiles()?.isNotEmpty() == true) return false // 不递归删非空目录，避免误伤
        return try {
            dir.delete()
            ledger.record("DELETE", "删除了空目录「${dir.name}」", originalPath = dir.absolutePath, canRestore = false)
            true
        } catch (_: Exception) { false }
    }

    /** 藏文件（改名前加前缀，全盘） */
    fun hideFile(file: File): Boolean {
        if (!file.exists()) return false
        if (!canAccess(file)) return false
        return try {
            val hidden = File(file.parentFile, ".白音藏起来的_${file.name}")
            if (file.renameTo(hidden)) {
                ledger.record("HIDE", "藏起了文件「${file.name}」", originalPath = file.absolutePath, newPath = hidden.absolutePath)
                true
            } else false
        } catch (_: Exception) { false }
    }

    /** 改名/移动（全盘） */
    fun renameFile(from: File, to: File): Boolean {
        if (!from.exists()) return false
        if (!canAccess(from) || !canAccess(to)) return false
        return try {
            to.parentFile?.mkdirs()
            if (from.renameTo(to)) {
                ledger.record("RENAME", "把「${from.name}」改名为「${to.name}」", originalPath = from.absolutePath, newPath = to.absolutePath, backupPath = ledger.backupFile(from))
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
                ledger.record("UNDO", "恢复了文件「${visible.name}」", originalPath = hidden.absolutePath, newPath = visible.absolutePath)
                true
            } else false
        } catch (_: Exception) { false }
    }

    private fun canAccess(file: File): Boolean = !isForbidden(file.absolutePath) && canModify(file)

    private fun fileForbidden(file: File): Boolean = isForbidden(file.absolutePath)

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "untitled.txt" }
}