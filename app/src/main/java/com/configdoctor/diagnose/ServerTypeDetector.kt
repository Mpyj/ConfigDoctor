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

    // لیست رسمی و کامل رنج‌های IPv4 و IPv6 کلودفلر (منبع: مستندات کلودفلر)
    private val cloudflareCidrs = listOf(
        // IPv4
        "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
        "104.16.0.0/13", "104.24.0.0/14",
        "108.162.192.0/18", "131.0.72.0/22", "141.101.64.0/18",
        "162.158.0.0/15", "172.64.0.0/13", "173.245.48.0/20",
        "188.114.96.0/20", "190.93.240.0/20", "197.234.240.0/22",
        "198.41.128.0/17",
        // IPv6
        "2400:cb00::/32", "2606:4700::/32", "2803:f800::/32",
        "2405:b500::/32", "2405:8100::/32", "2a06:98c0::/29",
        "2c0f:f248::/32"
    )

    // رنج‌های دیتاسنترهای معروف (نمونه)
    private val datacenterCidrs = listOf(
        // Hetzner
        "5.9.0.0/16", "88.99.0.0/16", "116.202.0.0/16", "116.203.0.0/16", "128.140.0.0/16",
        "135.181.0.0/16", "138.201.0.0/16", "142.132.0.0/16", "144.76.0.0/16", "148.251.0.0/16",
        "157.90.0.0/16", "159.69.0.0/16", "162.55.0.0/16", "167.235.0.0/16", "168.119.0.0/16",
        "176.9.0.0/16", "178.63.0.0/16", "188.40.0.0/16", "195.201.0.0/16", "213.239.0.0/16",
        // DigitalOcean
        "64.225.0.0/16", "64.226.0.0/16", "64.227.0.0/16", "68.183.0.0/16", "104.131.0.0/16",
        "104.236.0.0/16", "107.170.0.0/16", "128.199.0.0/16", "134.209.0.0/16", "138.68.0.0/16",
        "138.197.0.0/16", "142.93.0.0/16", "143.110.0.0/16", "143.198.0.0/16", "146.190.0.0/16",
        "157.230.0.0/16", "159.65.0.0/16", "159.89.0.0/16", "161.35.0.0/16", "164.90.0.0/16",
        "165.22.0.0/16", "165.227.0.0/16", "167.71.0.0/16", "167.99.0.0/16", "174.138.0.0/16",
        "178.62.0.0/16", "188.166.0.0/16", "206.189.0.0/16", "207.154.0.0/16", "209.97.0.0/16",
        // OVH
        "5.135.0.0/16", "5.196.0.0/16", "37.59.0.0/16", "37.187.0.0/16", "51.68.0.0/16",
        "51.75.0.0/16", "51.77.0.0/16", "51.79.0.0/16", "51.83.0.0/16", "51.89.0.0/16",
        "54.36.0.0/16", "54.37.0.0/16", "54.38.0.0/16", "91.121.0.0/16", "94.23.0.0/16",
        "137.74.0.0/16", "141.94.0.0/16", "145.239.0.0/16", "147.135.0.0/16", "149.202.0.0/16",
        "151.80.0.0/16", "158.69.0.0/16", "164.132.0.0/16", "167.114.0.0/16", "178.32.0.0/16",
        "178.33.0.0/16", "188.165.0.0/16", "193.70.0.0/16", "213.32.0.0/16", "213.186.0.0/16"
    )

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        val ip = resolveToIp(address) ?: return@withContext ServerType.UNKNOWN to null

        if (isIpInCidrList(ip, cloudflareCidrs)) {
            return@withContext ServerType.CLOUDFLARE to ip
        }

        if (isIpInCidrList(ip, datacenterCidrs)) {
            return@withContext ServerType.DATACENTER to ip
        }

        return@withContext ServerType.DIRECT to ip
    }

    private fun resolveToIp(address: String): String? {
        return try {
            InetAddress.getByName(address).hostAddress
        } catch (e: Exception) {
            null
        }
    }

    /**
     * چک می‌کنه که آیا یه IP توی لیست رنج‌های CIDR هست یا نه.
     * این روش دقیق‌تر از startsWith هست و ماسک شبکه رو در نظر می‌گیره.
     */
    private fun isIpInCidrList(ip: String, cidrList: List<String>): Boolean {
        val ipBytes = InetAddress.getByName(ip).address
        for (cidr in cidrList) {
            if (isIpInCidr(ipBytes, cidr)) {
                return true
            }
        }
        return false
    }

    private fun isIpInCidr(ipBytes: ByteArray, cidr: String): Boolean {
        return try {
            val parts = cidr.split("/")
            val cidrIp = parts[0]
            val prefixLength = parts[1].toInt()
            val cidrBytes = InetAddress.getByName(cidrIp).address

            // طول بایت‌ها باید یکی باشه (یا هر دو IPv4 یا هر دو IPv6)
            if (ipBytes.size != cidrBytes.size) return false

            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8

            // چک کردن بایت‌های کامل
            for (i in 0 until fullBytes) {
                if (ipBytes[i] != cidrBytes[i]) return false
            }

            // چک کردن بیت‌های باقی‌مانده
            if (remainingBits > 0) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((ipBytes[fullBytes].toInt() and mask) != (cidrBytes[fullBytes].toInt() and mask)) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}