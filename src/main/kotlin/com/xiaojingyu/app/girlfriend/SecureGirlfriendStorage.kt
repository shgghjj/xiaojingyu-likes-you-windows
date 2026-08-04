package com.xiaojingyu.app.girlfriend

import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 小女友本地数据的 AES-256-GCM 加密层（桌面版）。
 * 密钥以随机生成的 AES-256 密钥文件形式保存于数据目录（权限 600），
 * 不随明文暴露。数据格式：<12字节IV + 密文 + GCM tag>，落盘前 Base64 编码。
 */
object SecureGirlfriendStorage {

    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12
    private const val KEY_FILE_NAME = ".xiaojingyu_secret_key"

    @Volatile
    private var cachedKey: SecretKey? = null

    fun readEncrypted(file: File): String? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val decoded = Base64.getDecoder().decode(file.readBytes())
            val iv = decoded.copyOfRange(0, IV_LENGTH)
            val ciphertext = decoded.copyOfRange(IV_LENGTH, decoded.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(file.parentFile), GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun writeEncrypted(file: File, plaintext: String): Boolean {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(file.parentFile))
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val out = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, out, 0, iv.size)
            System.arraycopy(ciphertext, 0, out, iv.size, ciphertext.size)
            file.writeBytes(Base64.getEncoder().encode(out))
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 密钥按数据目录缓存，密钥文件存放在目录下 */
    private fun getOrCreateKey(dataDir: File?): SecretKey {
        cachedKey?.let { return it }
        val dir = dataDir ?: File(System.getProperty("user.home"), ".xiaojingyu")
        dir.mkdirs()
        val keyFile = File(dir, KEY_FILE_NAME)
        if (keyFile.exists()) {
            val bytes = keyFile.readBytes()
            val spec = javax.crypto.spec.SecretKeySpec(bytes, "AES")
            cachedKey = spec
            return spec
        }
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256, SecureRandom())
        val key = generator.generateKey()
        keyFile.writeBytes(key.encoded)
        try { keyFile.setReadable(false, false); keyFile.setReadable(true, true); keyFile.setWritable(false, false) } catch (_: Exception) {}
        cachedKey = key
        return key
    }
}
