package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

object CloudflareCleanIps {

    private val DEFAULT_IPV4 = listOf(
        "104.16.0.0", "104.16.1.0", "104.16.2.0", "104.16.3.0",
        "104.17.0.0", "104.18.0.0", "104.19.0.0", "104.20.0.0",
        "104.21.0.0", "104.22.0.0", "104.23.0.0", "104.24.0.0",
        "172.64.0.0", "172.65.0.0", "172.66.0.0", "172.67.0.0",
        "188.114.96.0", "188.114.97.0", "188.114.98.0", "188.114.99.0",
        "162.159.192.0", "162.159.193.0", "162.159.195.0", "162.159.200.0"
    )

    private val DEFAULT_IPV6 = listOf(
        "2606:4700::", "2606:4700:1::", "2606:4700:2::", "2606:4700:3::",
        "2606:4700:10::", "2606:4700:20::", "2606:4700:30::", "2606:4700:40::",
        "2606:4700:50::", "2606:4700:60::", "2606:4700:70::", "2606:4700:80::"
    )

    suspend fun generateRandomIpsFromStorage(context: Context, count: Int = 100): List<String> {
        val savedIps = SettingsManager.getCloudflareIps(context).first()
        val cachedIps = SettingsManager.getCachedWorkingIps(context).first()
        
        val baseList = if (!savedIps.isNullOrBlank()) {
            savedIps.lines().filter { it.isNotBlank() }
        } else {
            DEFAULT_IPV4 + DEFAULT_IPV6
        }
        
        // IPهای کش شده رو جدا می‌کنیم که اول تست بشن
        val cachedList = cachedIps?.lines()?.filter { it.isNotBlank() } ?: emptyList()
        
        // ترکیب IPهای کش شده با بقیه
        val finalList = mutableListOf<String>()
        finalList.addAll(cachedList) // اول کش
        
        // بقیه رو رندوم اضافه می‌کنیم تا سقف count پر بشه
        val randomNeeded = count - cachedList.size
        if (randomNeeded > 0) {
            finalList.addAll(generateFromBase(baseList, randomNeeded))
        }
        
        return finalList.distinct()
    }

    fun generateRandomIps(count: Int = 100): List<String> {
        return generateFromBase(DEFAULT_IPV4 + DEFAULT_IPV6, count)
    }

    private fun generateFromBase(baseList: List<String>, count: Int): List<String> {
        if (baseList.isEmpty()) return emptyList()
        val result = mutableSetOf<String>()
        var attempts = 0
        while (result.size < count && attempts < count * 10) {
            val base = baseList.random()
            val parts = base.split(".")
            val newIp = if (parts.size == 4) {
                "${parts[0]}.${parts[1]}.${parts[2]}.${Random.nextInt(1, 255)}"
            } else {
                base
            }
            result.add(newIp)
            attempts++
        }
        return result.toList()
    }

    suspend fun fetchFromSource(urlString: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ConfigDoctor")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP $responseCode"))
            }

            val content = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val ips = content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") && isValidIp(it) }

            if (ips.isEmpty()) {
                Result.failure(Exception("Empty list"))
            } else {
                Result.success(ips)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidIp(ip: String): Boolean {
        if (ip.contains(":")) return true
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
    }
}