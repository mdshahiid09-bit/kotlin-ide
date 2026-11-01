package com.kotlinide.shahid.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.kotlinide.shahid.app.util.CrashReportingHelper
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TokenStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
            ?: throw IllegalStateException("Could not retrieve secret key from Android Keystore")
    }

    @Synchronized
    fun saveToken(token: String) {
        val clean = cleanToken(token)
        if (clean.isBlank()) {
            clearToken()
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))

            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_ENC_IV, ivB64)
                .putString(KEY_ENC_TOKEN, cipherB64)
                .apply()
        } catch (t: Throwable) {
            CrashReportingHelper.logHandledException("TokenStorage", "Failed to encrypt GitHub token", t)
        }
    }

    @Synchronized
    fun getToken(): String? {
        val ivB64 = prefs.getString(KEY_ENC_IV, null) ?: return null
        val cipherB64 = prefs.getString(KEY_ENC_TOKEN, null) ?: return null

        return try {
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            val encryptedBytes = Base64.decode(cipherB64, Base64.DEFAULT)
            val secretKey = getOrCreateSecretKey()

            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decrypted = cipher.doFinal(encryptedBytes)
            val clean = cleanToken(String(decrypted, Charsets.UTF_8))
            if (clean.isNotBlank()) clean else null
        } catch (t: Throwable) {
            CrashReportingHelper.logHandledException(
                "TokenStorage",
                "Failed to decrypt GitHub token (Keystore corrupted or reset)",
                t
            )
            null
        }
    }

    fun getMaskedToken(): String? {
        val token = getToken() ?: return null
        return if (token.length > 8) {
            val prefix = token.take(4)
            val suffix = token.takeLast(4)
            val asterisks = "*".repeat((token.length - 8).coerceIn(6, 16))
            "$prefix$asterisks$suffix"
        } else {
            "********"
        }
    }

    @Synchronized
    fun clearToken() {
        prefs.edit()
            .remove(KEY_ENC_IV)
            .remove(KEY_ENC_TOKEN)
            .apply()
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "kotlin_ide_gh_token_key"
        private const val PREFS_FILENAME = "kotlin_ide_secure_prefs"
        private const val KEY_ENC_IV = "enc_token_iv"
        private const val KEY_ENC_TOKEN = "enc_token_payload"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        fun cleanToken(raw: String): String {
            return raw
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "")
                .replace("\t", "")
                .trim()
        }
    }
}
