package com.xiaojingyu.app

import java.io.File
import java.util.zip.ZipFile

/**
 * Live2D 模型管理器。
 * - 导入：用户选 ZIP → 解压到 live2d 模型目录
 * - 列出现有模型
 * - 切换当前模型
 */
object Live2DModelManager {

    private val live2dDir: File by lazy {
        File(System.getProperty("user.home"), ".xiaojingyu/live2d").apply { mkdirs() }
    }

    val modelsDir: File by lazy { File(live2dDir, "models").apply { mkdirs() } }

    /** 导入一个 ZIP 模型包。返回模型名，失败返回 null。 */
    fun importModel(zipPath: String): String? {
        val zipFile = ZipFile(zipPath)
        val name = File(zipPath).nameWithoutExtension.take(60)
        val targetDir = File(modelsDir, name)
        targetDir.mkdirs()
        try {
            zipFile.use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory) {
                        File(targetDir, entry.name).mkdirs()
                    } else {
                        val dest = File(targetDir, entry.name)
                        dest.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
            // 找到 .model3.json 确认有效
            val hasModel = targetDir.walkTopDown().any { it.name.endsWith(".model3.json") }
            if (!hasModel) { targetDir.deleteRecursively(); return null }
            return name
        } catch (_: Exception) {
            try { targetDir.deleteRecursively() } catch (_: Exception) {}
            return null
        }
    }

    /** 列出所有可用模型名 */
    fun listModels(): List<String> {
        return modelsDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.map { it.name }
            ?.sorted() ?: emptyList()
    }

    /** 获取模型的 model3.json 路径（相对于 live2d 根目录的 URL 路径） */
    fun modelJsonPath(modelName: String): String? {
        val dir = File(modelsDir, modelName)
        val json = dir.walkTopDown().find { it.name.endsWith(".model3.json") }
        return json?.absolutePath?.replace(live2dDir.absolutePath, "")?.replace("\\", "/")?.trimStart('/')
    }

    /** 当前选中的模型名（持久化到配置） */
    var currentModel: String?
        get() = configFile.let { if (it.exists()) it.readText().trim().ifBlank { null } else null }
        set(value) {
            if (value != null) configFile.writeText(value)
            else configFile.delete()
        }

    private val configFile: File get() = File(live2dDir, "selected_model.txt")
}
