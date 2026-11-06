package com.configdoctor.diagnose

import java.net.InetAddress

enum class ServerType {
    CLOUDFLARE,
    DIRECT,
    UNKNOWN
}

object ServerTypeDetector {

    // رنج‌های IPv4 کلودفلر (نمونه‌های اصلی)
    private val cloudflareRanges = listOf(
        "103.21.244." to 22,
        "103.22.200." to 22,
        "103.31.4." to 22,
        "104.16." to 13,
        "104.24." to 14,
        "108.162.192." to 18,
        "131.0.72." to 22,
        "141.101.64." to 18,
        "162.158." to 15,
        "172.64." to 13,
        "173.245.48." to 20,
        "188.114.96." to 20,
        "190.93.240." to 20,
        "197.234.240." to 22,
        "198.41.128." to 17
    )

    fun detect(address: String): Pair<ServerType, String?> {
        val ip = resolveToIp(address) ?: return ServerType.UNKNOWN to null
        val type = if (isCloudflareIp(ip)) ServerType.CLOUDFLARE else ServerType.DIRECT
        return type to ip
    }

    private fun resolveToIp(address: String): String? {
        return try {
            InetAddress.getByName(address).hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun isCloudflareIp(ip: String): Boolean {
        return cloudflareRanges.any { (prefix, _) -> ip.startsWith(prefix) }
    }
}