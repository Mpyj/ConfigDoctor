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
        // IPv4
        "103.21.244.", "103.22.200.", "103.31.4.",
        "104.16.", "104.17.", "104.18.", "104.19.", "104.20.", "104.21.", "104.22.", "104.23.", "104.24.", "104.25.", "104.26.", "104.27.",
        "108.162.192.", "131.0.72.", "141.101.64.", "162.158.",
        "172.64.", "172.65.", "172.66.", "172.67.", "172.68.", "172.69.", "172.70.", "172.71.",
        "173.245.48.", "188.114.96.", "190.93.240.", "197.234.240.", "198.41.128.",
        // IPv6
        "2400:cb00:", "2606:4700:", "2803:f800:", "2405:b500:", "2405:8100:", "2a06:98c0:", "2c0f:f248:"
    )

    // رنج‌های IPv4 دیتاسنترهای معروف (Hetzner, DigitalOcean, OVH, Vultr, Linode, AWS, Google Cloud, Azure)
    private val datacenterRanges = listOf(
        // Hetzner
        "5.9.", "88.99.", "116.202.", "116.203.", "128.140.",
        "135.181.", "138.201.", "142.132.", "144.76.", "148.251.",
        "157.90.", "159.69.", "162.55.", "167.235.", "168.119.",
        "176.9.", "178.63.", "188.40.", "195.201.", "213.239.",
        // DigitalOcean
        "64.225.", "64.226.", "64.227.", "68.183.", "104.131.",
        "104.236.", "107.170.", "128.199.", "134.209.", "138.68.",
        "138.197.", "142.93.", "143.110.", "143.198.", "146.190.",
        "157.230.", "159.65.", "159.89.", "161.35.", "164.90.",
        "165.22.", "165.227.", "167.71.", "167.99.", "174.138.",
        "178.62.", "188.166.", "206.189.", "207.154.", "209.97.",
        // OVH
        "5.135.", "5.196.", "37.59.", "37.187.", "51.68.",
        "51.75.", "51.77.", "51.79.", "51.83.", "51.89.",
        "54.36.", "54.37.", "54.38.", "91.121.", "94.23.",
        "137.74.", "141.94.", "145.239.", "147.135.", "149.202.",
        "151.80.", "158.69.", "164.132.", "167.114.", "178.32.",
        "178.33.", "188.165.", "193.70.", "213.32.", "213.186.",
        // Vultr
        "45.32.", "45.63.", "45.76.", "45.77.", "64.176.",
        "66.42.", "78.141.", "95.179.", "108.61.", "136.244.",
        "139.180.", "140.82.", "144.202.", "149.28.", "155.138.",
        "158.247.", "167.179.", "173.199.", "207.246.", "216.128.",
        // Linode
        "45.33.", "45.56.", "45.79.", "50.116.", "66.175.",
        "69.164.", "72.14.", "96.126.", "97.107.", "103.3.",
        "106.187.", "139.144.", "143.42.", "172.104.", "173.255.",
        "176.58.", "178.79.", "192.155.", "194.195.", "198.58.",
        // AWS (نمونه)
        "3.", "13.", "15.", "18.", "34.", "35.", "52.", "54.",
        // Google Cloud (نمونه)
        "34.", "35.", "104.196.", "104.197.", "104.198.", "130.211.",
        "146.148.", "162.222.", "173.255.", "199.192.", "199.223.",
        // Azure (نمونه)
        "13.", "20.", "23.", "40.", "51.", "52.", "65.", "104.",
        "137.", "138.", "168.", "191.",
        // Contabo
        "5.189.", "144.91.", "173.212.", "207.180.", "209.145.", "213.136."
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