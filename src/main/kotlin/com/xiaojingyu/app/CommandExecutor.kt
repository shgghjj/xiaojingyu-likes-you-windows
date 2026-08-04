package com.xiaojingyu.app

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 命令执行器（编码 Agent 核心）。
 * - 每次执行前必须经过用户确认（由 UI 层把关）
 * - 超时自动终止
 * - 捕获 stdout/stderr
 * - 记录到行动账本
 */
class CommandExecutor(private val ledger: ActionLedger) {

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val timedOut: Boolean
    )

    /**
     * 执行命令。
     * @param command 命令（如 "git status"）
     * @param workingDir 工作目录（必须在授权目录内）
     * @param timeoutSeconds 超时（默认60秒）
     */
    fun execute(command: String, workingDir: File, timeoutSeconds: Int = 60): CommandResult {
        val process = ProcessBuilder("cmd", "/c", command)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()
        val timedOut = !process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        if (timedOut) {
            process.destroyForcibly()
        }
        val output = process.inputStream.bufferedReader().readText().trim()
        val exit = if (timedOut) -1 else process.exitValue()
        ledger.record(
            type = "COMMAND",
            description = "执行了命令「$command」",
            originalPath = workingDir.absolutePath,
            canRestore = false
        )
        return CommandResult(exit, output, timedOut)
    }

    /** 安全检查：工作目录必须在授权范围内 */
    fun isAllowedDir(dir: File, enabledDirs: Set<File>): Boolean {
        return enabledDirs.any { dir.absolutePath.startsWith(it.absolutePath) }
    }
}
