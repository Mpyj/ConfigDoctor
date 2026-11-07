package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

enum class ServerType {
    CLOUDFLARE,
    DATACENTER,
    DIRECT,
    UNKNOWN
}

object ServerTypeDetector {

    // رنج‌های IPv4 و IPv6 کلودفلر
    private val cloudflareRanges = listOf(
        "103.21.244.", "103.22.200.", "103.31.4.",
        "104.16.", "104.17.", "104.18.", "104.19.", "104.20.", "104.21.", "104.22.", "104.23.", "104.24.", "104.25.", "104.26.", "104.27.",
        "108.162.192.", "131.0.72.", "141.101.64.", "162.158.",
        "172.64.", "172.65.", "172.66.", "172.67.", "172.68.", "172.69.", "172.70.", "172.71.",
        "173.245.48.", "188.114.96.", "190.93.240.", "197.234.240.", "198.41.128.",
        // IPv6
        "2400:cb00:", "2606:4700:", "2803:f800:", "2405:b500:", "2405:8100:", 
        "2a06:98c0:", "2a06:98c1:", // این رنج اضافه شد تا IPهای IPv6 کلودفلر شناسایی بشن
        "2c0f:f248:"
    )

    // رنج‌های IPv4 دیتاسنترهای معروف (نمونه)
    private val datacenterRanges = listOf(
        "5.9.", "88.99.", "116.202.", "116.203.", "128.140.",
        "135.181.", "138.201.", "142.132.", "144.76.", "148.251.",
        "157.90.", "159.69.", "162.55.", "167.235.", "168.119.",
        "176.9.", "178.63.", "188.40.", "195.201.", "213.239.",
        // بقیه رنج‌ها...
    )

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        val ip = resolveToIp(address) ?: return@withContext ServerType.UNKNOWN to null

        // ۱. اول چک کن کلادفلر هست یا نه
        if (isCloudflareIp(ip)) {
            return@withContext ServerType.CLOUDFLARE to ip
        }

        // ۲. چک کن دیتاسنتر هست یا نه
        if (isDatacenterIp(ip)) {
            return@withContext ServerType.DATACENTER to ip
        }

        // ۳. بقیه = مستقیم
        return@withContext ServerType.DIRECT to ip
    }

    private fun resolveToIp(address: String): String? {
        return try {
            // اگه آدرس دامنه باشه، IP رو resolve میکنه
            InetAddress.getByName(address).hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun isCloudflareIp(ip: String): Boolean {
        return cloudflareRanges.any { ip.startsWith(it, ignoreCase = true) }
    }

    private fun isDatacenterIp(ip: String): Boolean {
        return datacenterRanges.any { ip.startsWith(it) }
    }
}