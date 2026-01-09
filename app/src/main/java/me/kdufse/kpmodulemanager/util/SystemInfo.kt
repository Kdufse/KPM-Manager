package me.kdufse.kpmodulemanager.util

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader

object SystemInfo {

    fun getKernelVersion(): String {
        return try {
            val kernelVersion = System.getProperty("os.version") ?: "Unknown"
            kernelVersion
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getKernelArchitecture(): String {
        return Build.SUPPORTED_ABIS?.joinToString(", ") ?: Build.CPU_ABI ?: "Unknown"
    }

    fun getLoadedModulesCount(): Int {
        return try {
            val modulesFile = "/proc/modules"
            BufferedReader(FileReader(modulesFile)).use { reader ->
                var count = 0
                while (reader.readLine() != null) count++
                count
            }
        } catch (e: Exception) {
            0
        }
    }

    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun getManagerVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}