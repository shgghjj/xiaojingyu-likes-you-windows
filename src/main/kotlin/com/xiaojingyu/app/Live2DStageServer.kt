package com.xiaojingyu.app

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI

/**
 * Live2D 舞台服务（桌面版）。
 * 用内置 HTTP 服务器托管 live2d/index.html，浏览器打开渲染。
 * 模型文件从 resources 解压到用户目录，避免 jar 内读取问题。
 */
object Live2DStageServer {

    private var server: HttpServer? = null
    private var baseDir: java.io.File? = null

    /** 解压 live2d 资源到用户目录并启动服务，返回舞台 URL */
    fun start(): String {
        stop()
        val home = java.io.File(System.getProperty("user.home"), ".xiaojingyu/live2d")
        home.mkdirs()
        // 解压 resources/live2d 到 home
        extractResources(home)
        baseDir = home

        val port = 38911
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
        httpServer.createContext("/") { exchange -> handle(exchange) }
        httpServer.executor = null
        httpServer.start()
        server = httpServer
        return "http://127.0.0.1:$port/index.html"
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    fun openInBrowser() {
        val url = start()
        runCatching {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    private fun extractResources(home: java.io.File) {
        val classLoader = Live2DStageServer::class.java.classLoader
        val resource = classLoader.getResource("live2d") ?: return
        // 遍历解压所有文件（含子目录）
        val base = java.io.File(resource.toURI())
        if (base.isDirectory) {
            copyDir(base, home)
        }
    }

    private fun copyDir(src: java.io.File, dst: java.io.File) {
        src.listFiles()?.forEach { f ->
            val target = java.io.File(dst, f.name)
            if (f.isDirectory) {
                target.mkdirs()
                copyDir(f, target)
            } else {
                f.copyTo(target, overwrite = true)
            }
        }
    }

    private fun handle(exchange: HttpExchange) {
        try {
            val rawPath = exchange.requestURI.path.let { if (it == "/") "/index.html" else it }
            val dir = baseDir ?: return
            // 路径遍历防护：规范化后必须在 baseDir 内
            val base = dir.canonicalFile
            val file = java.io.File(base, rawPath.trimStart('/')).canonicalFile
            if (!file.absolutePath.startsWith(base.absolutePath + java.io.File.separator) && file != base) {
                exchange.sendResponseHeaders(403, -1)
                exchange.close()
                return
            }
            if (file.exists() && file.isFile) {
                val bytes = file.readBytes()
                val mime = when (file.extension.lowercase()) {
                    "html" -> "text/html; charset=utf-8"
                    "js" -> "application/javascript"
                    "json" -> "application/json"
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "moc3" -> "application/octet-stream"
                    else -> "application/octet-stream"
                }
                exchange.responseHeaders.set("Content-Type", mime)
                exchange.responseHeaders.set("Cache-Control", "no-cache")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } else {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }
        } catch (_: Exception) {
            runCatching { exchange.sendResponseHeaders(500, -1) }
            exchange.close()
        }
    }
}
