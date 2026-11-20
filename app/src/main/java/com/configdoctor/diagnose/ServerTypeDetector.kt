package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

enum class ServerType {
    CLOUDFLARE, DATACENTER, DIRECT, UNKNOWN
}

object ServerTypeDetector {
    // رنج‌های دقیق IPv4 کلادفلر (به صورت Prefix)
    private val cloudflareIpv4Ranges = listOf(
        "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
        "104.16.0.0/13", "104.24.0.0/14", "108.162.192.0/18",
        "131.0.72.0/22", "141.101.64.0/18", "162.158.0.0/15",
        "172.64.0.0/13", "173.245.48.0/20", "188.114.96.0/20",
        "190.93.240.0/20", "197.234.240.0/22", "198.41.128.0/17"
    )

    // رنج‌های دقیق IPv6 کلادفلر
    private val cloudflareIpv6Ranges = listOf(
        "2400:cb00::/32", "2405:8100::/32", "2405:b500::/32",
        "2606:4700::/32", "2803:f800::/32", "2a06:98c0::/29", "2c0f:f248::/32"
    )

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        try {
            val resolvedIp = InetAddress.getByName(address).hostAddress ?: return@withContext Pair(ServerType.UNKNOWN, null)
            
            if (isCloudflareIp(resolvedIp)) {
                return@withContext Pair(ServerType.CLOUDFLARE, resolvedIp)
            }
            
            return@withContext Pair(ServerType.DIRECT, resolvedIp)
        } catch (e: Exception) {
            return@withContext Pair(ServerType.UNKNOWN, null)
        }
    }

    private fun isCloudflareIp(ip: String): Boolean {
        if (ip.contains(":")) {
            // چک کردن IPv6
            return cloudflareIpv6Ranges.any { ip.startsWith(it.substringBefore("::")) }
        }
        
        // چک کردن IPv4
        val ipBytes = ipToBytes(ip) ?: return false
        
        return cloudflareIpv4Ranges.any { cidr ->
            val parts = cidr.split("/")
            val networkBytes = ipToBytes(parts[0]) ?: return@any false
            val prefixLength = parts[1].toInt()
            
            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8
            
            for (i in 0 until fullBytes) {
                if (ipBytes[i] != networkBytes[i]) return@any false
            }
            
            if (remainingBits > 0) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                // 🔴 این خط اصلاح شد (toInt اضافه شد)
                if ((ipBytes[fullBytes].toInt() and mask) != (networkBytes[fullBytes].toInt() and mask)) return@any false
            }
            true
        }
    }

    private fun ipToBytes(ip: String): ByteArray? {
        val parts = ip.split(".")
        if (parts.size != 4) return null
        val bytes = ByteArray(4)
        for (i in 0..3) {
            val num = parts[i].toIntOrNull() ?: return null
            if (num !in 0..255) return null
            bytes[i] = num.toByte()
        }
        return bytes
    }
}