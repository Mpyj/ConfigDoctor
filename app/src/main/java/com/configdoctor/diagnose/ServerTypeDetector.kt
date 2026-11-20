package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

enum class ServerType {
    CLOUDFLARE, DATACENTER, DIRECT, UNKNOWN
}

object ServerTypeDetector {
    // رنج‌های IPv4 کلادفلر
    private val cloudflareIpv4Ranges = listOf(
        "103.21.244.0", "103.22.200.0", "103.31.4.0",
        "104.16.0.0", "104.17.0.0", "104.18.0.0", "104.19.0.0",
        "104.20.0.0", "104.21.0.0", "104.22.0.0", "104.23.0.0",
        "104.24.0.0", "104.25.0.0", "104.26.0.0", "104.27.0.0",
        "108.162.192.0", "131.0.72.0", "141.101.64.0",
        "162.158.0.0", "172.64.0.0", "172.65.0.0", "172.66.0.0", "172.67.0.0",
        "173.245.48.0", "188.114.96.0", "188.114.97.0", "188.114.98.0", "188.114.99.0",
        "190.93.240.0", "197.234.240.0", "198.41.128.0"
    )

    // رنج‌های IPv6 کلادفلر
    private val cloudflareIpv6Ranges = listOf(
        "2400:cb00", "2405:8100", "2405:b500",
        "2606:4700", "2803:f800", "2a06:98c0", "2c0f:f248"
    )

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        try {
            val resolvedIp = InetAddress.getByName(address).hostAddress ?: return@withContext Pair(ServerType.UNKNOWN, null)
            
            if (isCloudflareIp(resolvedIp)) {
                return@withContext Pair(ServerType.CLOUDFLARE, resolvedIp)
            }
            
            // اگر IPv6 بود ولی کلادفلر نبود، به عنوان دیتاسنتر/مستقیم حساب می‌کنیم
            val type = if (resolvedIp.contains(":")) {
                ServerType.DATACENTER 
            } else {
                ServerType.DIRECT
            }
            
            return@withContext Pair(type, resolvedIp)
        } catch (e: Exception) {
            return@withContext Pair(ServerType.UNKNOWN, null)
        }
    }

    private fun isCloudflareIp(ip: String): Boolean {
        if (ip.contains(":")) {
            // چک کردن IPv6 (بر اساس پیشوند)
            return cloudflareIpv6Ranges.any { ip.startsWith(it) }
        }
        
        // چک کردن IPv4
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        val ipNum = (parts[0].toLong() shl 24) + (parts[1].toLong() shl 16) + (parts[2].toLong() shl 8) + parts[3].toLong()
        
        return cloudflareIpv4Ranges.any { range ->
            val rangeParts = range.split(".")
            val rangeNum = (rangeParts[0].toLong() shl 24) + (rangeParts[1].toLong() shl 16) + (rangeParts[2].toLong() shl 8) + rangeParts[3].toLong()
            // مقایسه ۲۴ بیت اول (کلاس C)
            (ipNum shr 8) == (rangeNum shr 8)
        }
    }
}