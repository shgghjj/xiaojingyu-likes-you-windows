package com.xiaojingyu.app

import java.io.File

/**
 * Git 操作封装（编码 Agent 用）。
 * 所有操作走命令执行器 + 行动账本。
 */
class GitOperator(private val executor: CommandExecutor, private val ledger: ActionLedger) {

    fun status(repo: File): String = executor.execute("git status", repo, 30).output

    fun log(repo: File, count: Int = 10): String =
        executor.execute("git log --oneline -$count", repo, 30).output

    fun branch(repo: File): String = executor.execute("git branch -a", repo, 30).output

    fun diff(repo: File): String = executor.execute("git diff --stat", repo, 30).output

    fun addAll(repo: File): String = executor.execute("git add -A", repo, 60).output

    fun commit(repo: File, message: String): String =
        executor.execute("git commit -m \"${message.replace("\"", "'")}\"", repo, 60).output

    fun push(repo: File): String = executor.execute("git push", repo, 120).output

    fun pull(repo: File): String = executor.execute("git pull", repo, 120).output

    fun createBranch(repo: File, name: String): String =
        executor.execute("git checkout -b $name", repo, 60).output

    fun checkout(repo: File, branch: String): String =
        executor.execute("git checkout $branch", repo, 60).output

    /** 运行中的代码仓库目录列表（从 ConfigStore 授权目录里筛 .git） */
    fun findRepos(dirs: Set<File>): List<File> {
        val repos = mutableListOf<File>()
        for (dir in dirs) {
            if (!dir.exists()) continue
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory && File(f, ".git").exists()) repos.add(f)
            }
            if (File(dir, ".git").exists()) repos.add(dir)
        }
        return repos.distinctBy { it.absolutePath }
    }
}
