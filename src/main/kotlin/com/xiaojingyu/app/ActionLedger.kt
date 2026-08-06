package com.xiaojingyu.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 一次行动记录。 */
@Serializable
data class ActionEntry(
    val id: String,
    val timestamp: String,
    val type: String,          // READ / WRITE / CREATE / DELETE / RENAME / HIDE / COMMAND / GIT / UNDO
    val description: String,   // 她干了什么（人话）
    val originalPath: String? = null,   // 原路径
    val newPath: String? = null,        // 新路径（重命名/移动后）
    val backupPath: String? = null,     // 快照备份位置（可恢复用）
    val canRestore: Boolean = true,     // 是否可恢复
    val restored: Boolean = false       // 是否已恢复
)

/**
 * 行动账本（Windows 版核心安全特性）。
 * - 所有她干的实事如实记录
 * - 模型无法修改或删除账本（应用层保护）
 * - 支持一键恢复（还原备份）
 */
class ActionLedger(private val dataDir: File) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val ledgerFile: File get() = File(dataDir, "action_ledger.json")
    private val backupDir: File get() = File(dataDir, "backups").apply { mkdirs() }

    @Volatile
    private var entries: MutableList<ActionEntry> = load()

    @Synchronized
    fun all(): List<ActionEntry> = entries.toList()

    @Synchronized
    fun record(
        type: String,
        description: String,
        originalPath: String? = null,
        newPath: String? = null,
        backupPath: String? = null,
        canRestore: Boolean = true
    ) {
        entries.add(
            ActionEntry(
                id = "${System.currentTimeMillis()}-${entries.size}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                type = type,
                description = description,
                originalPath = originalPath,
                newPath = newPath,
                backupPath = backupPath,
                canRestore = canRestore
            )
        )
        save()
    }

    /** 备份一个文件到回收区（改文件前调用） */
    @Synchronized
    fun backupFile(file: File): String? {
        if (!file.exists()) return null
        return try {
            val target = File(backupDir, "backup_${System.currentTimeMillis()}_${file.name}")
            file.copyTo(target, overwrite = true)
            target.absolutePath
        } catch (_: Exception) { null }
    }

    /** 一键恢复所有可恢复的操作 */
    @Synchronized
    fun restoreAll(): Int {
        var restored = 0
        val updated = entries.mapIndexed { index, entry ->
            if (!entry.canRestore || entry.restored) return@mapIndexed entry
            val ok = when {
                // 有备份：恢复原文件
                entry.backupPath != null && entry.originalPath != null -> {
                    val backup = File(entry.backupPath)
                    val original = File(entry.originalPath)
                    if (backup.exists()) {
                        try {
                            original.parentFile?.mkdirs()
                            backup.copyTo(original, overwrite = true)
                            true
                        } catch (_: Exception) { false }
                    } else false
                }
                // 新建的文件：删除（撤销创建）
                entry.type == "CREATE" && entry.originalPath != null -> {
                    val f = File(entry.originalPath)
                    if (f.exists()) {
                        try { f.delete() } catch (_: Exception) { false }
                    } else true
                }
                // 改名/移动：改回原位置（跨盘 fallback 到备份还原）
                entry.type == "RENAME" && entry.originalPath != null && entry.newPath != null -> {
                    val target = File(entry.newPath)
                    val original = File(entry.originalPath)
                    if (target.exists()) {
                        try {
                            original.parentFile?.mkdirs()
                            if (original.exists()) original.delete()
                            val renamed = target.renameTo(original)
                            if (!renamed && entry.backupPath != null) {
                                // 跨盘改名失败，用备份还原 → 删改过名的文件
                                val backup = File(entry.backupPath)
                                if (backup.exists()) {
                                    backup.copyTo(original, overwrite = true)
                                    target.delete()
                                    true
                                } else false
                            } else renamed
                        } catch (_: Exception) { false }
                    } else if (!original.exists()) false else true
                }
                // 隐藏/改名：改回原名字
                entry.type == "HIDE" && entry.originalPath != null && entry.newPath != null -> {
                    val hidden = File(entry.newPath)
                    val original = File(entry.originalPath)
                    if (hidden.exists()) {
                        try { hidden.renameTo(original) } catch (_: Exception) { false }
                    } else if (!original.exists()) false else true
                }
                else -> false
            }
            if (ok) {
                restored++
                entry.copy(restored = true)
            } else entry
        }
        entries = updated.toMutableList()
        save()
        return restored
    }

    @Synchronized
    fun clear() {
        entries.clear()
        save()
    }

    private fun save() {
        try {
            ledgerFile.writeText(json.encodeToString(ListSerializer(ActionEntry.serializer()), entries))
        } catch (_: Exception) {}
    }

    private fun load(): MutableList<ActionEntry> {
        return try {
            if (ledgerFile.exists()) {
                json.decodeFromString(ListSerializer(ActionEntry.serializer()), ledgerFile.readText()).toMutableList()
            } else mutableListOf()
        } catch (_: Exception) { mutableListOf() }
    }
}
