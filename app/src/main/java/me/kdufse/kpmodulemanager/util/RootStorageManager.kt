package me.kdufse.kpmodulemanager.util

import android.content.Context
import android.util.Base64
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RootStorageManager(private val context: Context) {

    companion object {
        private const val SUPERKEY_FILE_PATH = "/system/etc/superkey.prop"
        private const val SALT_FILE_PATH = "/data/system/kpm_salt.dat"
        private const val KEY_ALGORITHM = "AES"
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        
        // 主密钥（应该从安全的地方派生，这里简化处理）
        private val MASTER_KEY by lazy {
            // 从设备特定信息派生密钥
            deriveMasterKey()
        }
    }

    /**
     * 检查是否具有Root权限
     */
    fun hasRootAccess(): Boolean {
        return Shell.isAppGrantedRoot() == true
    }

    /**
     * 使用Root权限存储SuperKey到系统分区
     */
    fun storeSuperKeyWithRoot(superKey: String): Boolean {
        if (!hasRootAccess()) {
            return false
        }

        return try {
            // 1. 加密SuperKey
            val encryptedData = encryptSuperKey(superKey)
            
            // 2. 创建临时文件
            val tempFile = SuFile.createTempFile("superkey", ".tmp", SuFile("/data/local/tmp"))
            tempFile.writeText(encryptedData)
            
            // 3. 挂载系统分区为可写
            val mountResult = Shell.cmd(
                "mount -o remount,rw /system",
                "mkdir -p /system/etc"
            ).exec()
            
            if (!mountResult.isSuccess) {
                tempFile.delete()
                return false
            }
            
            // 4. 复制文件到系统分区
            val copyResult = Shell.cmd("cp ${tempFile.absolutePath} $SUPERKEY_FILE_PATH").exec()
            
            // 5. 设置正确的权限（仅Root可读）
            Shell.cmd(
                "chmod 600 $SUPERKEY_FILE_PATH",
                "chown root:root $SUPERKEY_FILE_PATH"
            ).exec()
            
            // 6. 重新挂载为只读
            Shell.cmd("mount -o remount,ro /system").exec()
            
            // 7. 清理临时文件
            tempFile.delete()
            
            copyResult.isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 使用Root权限读取SuperKey
     */
    fun retrieveSuperKeyWithRoot(): String? {
        if (!hasRootAccess()) {
            return null
        }

        return try {
            // 检查文件是否存在
            val superKeyFile = SuFile(SUPERKEY_FILE_PATH)
            if (!superKeyFile.exists()) {
                return null
            }
            
            // 读取加密数据
            val encryptedData = superKeyFile.readText()
            
            // 解密数据
            decryptSuperKey(encryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查SuperKey是否存在
     */
    fun hasStoredSuperKey(): Boolean {
        if (!hasRootAccess()) {
            return false
        }
        
        return try {
            SuFile(SUPERKEY_FILE_PATH).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除存储的SuperKey
     */
    fun deleteStoredSuperKey(): Boolean {
        if (!hasRootAccess()) {
            return false
        }
        
        return try {
            Shell.cmd(
                "mount -o remount,rw /system",
                "rm -f $SUPERKEY_FILE_PATH",
                "mount -o remount,ro /system"
            ).exec().isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 加密SuperKey
     */
    private fun encryptSuperKey(superKey: String): String {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val keySpec = SecretKeySpec(MASTER_KEY, KEY_ALGORITHM)
        
        // 生成随机IV
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        
        val encrypted = cipher.doFinal(superKey.toByteArray(Charsets.UTF_8))
        
        // 组合IV + 加密数据
        val result = iv + encrypted
        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /**
     * 解密SuperKey
     */
    private fun decryptSuperKey(encryptedData: String): String? {
        return try {
            val data = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // 提取IV（前12字节）和加密数据
            val iv = data.copyOfRange(0, 12)
            val encrypted = data.copyOfRange(12, data.size)
            
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val keySpec = SecretKeySpec(MASTER_KEY, KEY_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            val decrypted = cipher.doFinal(encrypted)
            
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从设备信息派生主密钥
     */
    private fun deriveMasterKey(): ByteArray {
        // 使用设备特定的信息来派生密钥
        val deviceId = getDeviceSpecificId()
        val salt = getOrCreateSalt()
        
        // 简单的密钥派生（实际应该使用PBKDF2等安全方法）
        return (deviceId + salt).toByteArray(Charsets.UTF_8)
            .copyOf(32) // AES-256需要32字节密钥
    }

    /**
     * 获取设备特定ID
     */
    private fun getDeviceSpecificId(): String {
        return try {
            // 使用Build信息创建设备特定ID
            Build.BOARD + Build.BRAND + Build.DEVICE + Build.HARDWARE + Build.MODEL
        } catch (e: Exception) {
            "kpm_default_device_id"
        }
    }

    /**
     * 获取或创建盐值
     */
    private fun getOrCreateSalt(): String {
        return try {
            val saltFile = SuFile(SALT_FILE_PATH)
            if (saltFile.exists()) {
                saltFile.readText()
            } else {
                // 生成随机盐值
                val salt = generateRandomSalt()
                if (hasRootAccess()) {
                    saltFile.writeText(salt)
                    Shell.cmd(
                        "chmod 600 $SALT_FILE_PATH",
                        "chown root:root $SALT_FILE_PATH"
                    ).exec()
                }
                salt
            }
        } catch (e: Exception) {
            generateRandomSalt()
        }
    }

    /**
     * 生成随机盐值
     */
    private fun generateRandomSalt(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}