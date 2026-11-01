package com.kotlinide.shahid.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_FILENAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    fun saveToken(token: String) {
        val cleanToken = cleanToken(token)
        prefs.edit().putString(KEY_GITHUB_TOKEN, cleanToken).apply()
    }

    fun getToken(): String? {
        val raw = prefs.getString(KEY_GITHUB_TOKEN, null) ?: return null
        val cleanToken = cleanToken(raw)
        return if (cleanToken.isNotBlank()) cleanToken else null
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

    fun clearToken() {
        prefs.edit().remove(KEY_GITHUB_TOKEN).apply()
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val PREFS_FILENAME = "secure_user_credentials"
        private const val PREFS_FILENAME_FALLBACK = "user_credentials_prefs"
        private const val KEY_GITHUB_TOKEN = "enc_gh_pat_token"

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
