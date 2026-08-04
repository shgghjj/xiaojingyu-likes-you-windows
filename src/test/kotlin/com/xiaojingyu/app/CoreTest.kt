package com.xiaojingyu.app

import com.xiaojingyu.app.girlfriend.GirlfriendMemoryStore
import com.xiaojingyu.app.girlfriend.JailbreakLibrary
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 桌面版核心链路测试（不依赖 UI/网络）。 */
class CoreTest {

    private val testDir = File(System.getProperty("java.io.tmpdir"), "xjytest_${System.currentTimeMillis()}")

    @Test
    fun testMemoryStoreRoundTrip() {
        val store = GirlfriendMemoryStore(testDir)
        val state = store.load()
        assertEquals("白音", state.name)
        // 修改并保存
        val updated = state.copy(boredom = 42, lastInteractionTime = System.currentTimeMillis())
        store.save(updated)
        val reloaded = store.load()
        assertEquals(42, reloaded.boredom)
        assertEquals(updated.lastInteractionTime, reloaded.lastInteractionTime)
    }

    @Test
    fun testCustomJailbreakEncrypted() {
        val store = GirlfriendMemoryStore(testDir)
        store.saveCustomJailbreak("测试词库内容")
        assertEquals("测试词库内容", store.loadCustomJailbreak())
        // 明文不能直接出现在文件里
        val raw = File(testDir, "girlfriend/custom_jailbreak.txt").readText()
        assertTrue(!raw.contains("测试词库内容"), "词库应该是加密存储的")
    }

    @Test
    fun testJailbreakLibraryLoads() {
        val gentle = JailbreakLibrary.load(testDir, "gentle")
        assertTrue(gentle.isNotBlank(), "内置 gentle 词库应能加载")
        val none = JailbreakLibrary.load(testDir, "none")
        assertEquals("", none)
    }

    @Test
    fun testSandboxWriteAndLedger() {
        val ledger = ActionLedger(testDir)
        val sandboxRoot = File(testDir, "sandbox")
        val sandbox = FileSandbox(ledger, sandboxRoot)

        // 沙盒内写文件
        val file = sandbox.writeFile("test.txt", "hello 白音")
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(ledger.all().any { it.type == "WRITE" })

        // 读文件（沙盒内不需要开关）
        val content = sandbox.readText(file, fileReadEnabled = false)
        assertEquals("hello 白音", content)

        // 删除进账本，可恢复
        assertTrue(sandbox.deleteFile(file))
        assertTrue(!file.exists())
        assertEquals(1, ledger.restoreAll())
        assertTrue(file.exists())
    }

    @Test
    fun testSandboxExternalReadRequiresSwitch() {
        val ledger = ActionLedger(testDir)
        val sandboxRoot = File(testDir, "sandbox")
        val sandbox = FileSandbox(ledger, sandboxRoot)

        // 外部文件（临时目录里建一个 txt）
        testDir.mkdirs()
        val external = File(testDir, "external.txt").apply { writeText("外部文件内容") }
        // 开关关闭时不能读
        assertEquals(null, sandbox.readText(external, fileReadEnabled = false))
        // 开关开启时能读
        assertEquals("外部文件内容", sandbox.readText(external, fileReadEnabled = true))
    }

    @Test
    fun testConfigRoundTrip() {
        val store = ConfigStore(testDir)
        val cfg = store.get()
        assertEquals("白音", cfg.girlfriendName)
        store.update { it.copy(apiKey = "sk-test-123456", fileReadEnabled = true) }
        val reloaded = ConfigStore(testDir).get()
        assertEquals("sk-test-123456", reloaded.apiKey)
        assertTrue(reloaded.fileReadEnabled)
    }

    @Test
    fun testChatStoreEncrypted() {
        val chatStore = ChatStore(testDir)
        chatStore.add("你好白音", isUser = true)
        chatStore.add("老大你好喵~", isUser = false)
        assertEquals(2, chatStore.all().size)
        val raw = File(testDir, "chats.json")
        val text = raw.readText()
        assertTrue(!text.contains("你好白音"), "聊天记录应加密存储")
    }

    @Test
    fun testPluginRegistry() = runBlocking {
        val ledger = ActionLedger(testDir)
        val sandbox = FileSandbox(ledger, File(testDir, "sandbox"))
        val executor = CommandExecutor(ledger)
        val context = com.xiaojingyu.app.plugin.PluginContext(ledger, sandbox, { AppConfig() }, executor)
        val appState = AppState(ConfigStore(testDir), ChatStore(testDir))
        appState.registerPlugins()
        val infos = com.xiaojingyu.app.plugin.PluginRegistry.infos()
        assertTrue(infos.any { it.id == "demo_note" }, "演示插件应已注册")
        com.xiaojingyu.app.plugin.PluginRegistry.enable("demo_note", context)
        // 便签应该写入了沙盒
        assertTrue(File(testDir, "sandbox/白音的便签.txt").exists())
    }
}
