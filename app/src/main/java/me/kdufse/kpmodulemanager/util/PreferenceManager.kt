package me.kdufse.kpmodulemanager.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PreferenceManager(private val context: Context) {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "kpm_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_SUPER_KEY = "encrypted_super_key"
        private const val KEY_SUPER_KEY_SALT = "super_key_salt"
        private const val KEY_IS_SUPER_KEY_SET = "is_super_key_set"
    }

    fun setSuperKey(superKey: String) {
        try {
            // 对超级密钥进行加密存储
            val encryptedKey = encryptData(superKey)
            with(sharedPreferences.edit()) {
                putString(KEY_SUPER_KEY, encryptedKey)
                putBoolean(KEY_IS_SUPER_KEY_SET, true)
                apply()
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt super key", e)
        }
    }

    fun getSuperKey(): String? {
        return try {
            val encryptedKey = sharedPreferences.getString(KEY_SUPER_KEY, null)
            encryptedKey?.let { decryptData(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun hasSuperKey(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_SUPER_KEY_SET, false) && getSuperKey() != null
    }

    fun clearSuperKey() {
        with(sharedPreferences.edit()) {
            remove(KEY_SUPER_KEY)
            remove(KEY_IS_SUPER_KEY_SET)
            apply()
        }
    }

    private fun encryptData(data: String): String {
        return try {
            // 使用Android Keystore系统进行加密
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = getOrCreateSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // 将IV和加密数据一起存储
            Base64.encodeToString(iv + encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    private fun decryptData(encryptedData: String): String {
        return try {
            val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = decodedData.copyOfRange(0, 12) // GCM IV长度通常为12字节
            val encryptedBytes = decodedData.copyOfRange(12, decodedData.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = getOrCreateSecretKey()
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        val keyAlias = "kpm_super_key_alias"
        
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setKeySize(256)
                setUserAuthenticationRequired(false)
            }.build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        
        return keyStore.getKey(keyAlias, null) as SecretKey
    }
}