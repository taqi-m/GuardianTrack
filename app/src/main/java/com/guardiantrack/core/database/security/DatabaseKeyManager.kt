package com.guardiantrack.core.database.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "guardian_secure_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getOrCreatePassphrase(): ByteArray {
        val existing = securePrefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoded = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
        securePrefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
        return randomBytes
    }

    fun getOrCreateDeviceId(): String {
        val existing = securePrefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val newId = "guardian-" + java.util.UUID.randomUUID().toString()
        securePrefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    companion object {
        private const val KEY_PASSPHRASE = "db_passphrase_key"
        private const val KEY_DEVICE_ID = "device_identity_id"
    }
}
