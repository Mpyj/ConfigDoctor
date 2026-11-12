package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL

enum class ServerType {
    CLOUDFLARE,
    DATACENTER,
    DIRECT,
    UNKNOWN
}

object ServerTypeDetector {

    private var cloudflareRanges: List<String> = listOf(
        "103.21.244.", "103.22.200.", "103.31.4.",
        "104.16.", "104.17.", "104.18.", "104.19.", "104.20.", "104.21.", "104.22.", "104.23.", "104.24.", "104.25.", "104.26.", "104.27.",
        "108.162.192.", "131.0.72.", "141.101.64.", "162.158.",
        "172.64.", "172.65.", "172.66.", "172.67.", "172.68.", "172.69.", "172.70.", "172.71.",
        "173.245.48.", "188.114.96.", "190.93.240.", "197.234.240.", "198.41.128.",
        "2400:cb00:", "2606:4700:", "2803:f800:", "2405:b500:", "2405:8100:", "2a06:98c0:", "2a06:98c1:", "2c0f:f248:"
    )

    private val datacenterRanges = listOf(
        "5.9.", "88.99.", "116.202.", "116.203.", "128.140.",
        "135.181.", "138.201.", "142.132.", "144.76.", "148.251.",
        "157.90.", "159.69.", "162.55.", "167.235.", "168.119.",
        "176.9.", "178.63.", "188.40.", "195.201.", "213.239."
        // ...
    )

    // یک بار در ابتدا رنج‌های کلادفلر رو دانلود کن
    private var isInitialized = false

    private suspend fun initCloudflareRanges() {
        if (isInitialized) return
        try {
            val url = URL("https://api.cloudflare.com/client/v4/ips")
            val conn = url.openConnection()
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val body = conn.getInputStream().bufferedReader().readText()
            val json = JSONObject(body)
            val result = json.getJSONObject("result")
            val ipv4 = result.getJSONArray("ipv4_cidrs")
            val ipv6 = result.getJSONArray("ipv6_cidrs")
            val newRanges = mutableListOf<String>()
            for (i in 0 until ipv4.length()) {
                newRanges.add(ipv4.getString(i).substringBefore("/"))
            }
            for (i in 0 until ipv6.length()) {
                newRanges.add(ipv6.getString(i).substringBefore("/"))
            }
            if (newRanges.isNotEmpty()) {
                cloudflareRanges = newRanges
            }
            isInitialized = true
        } catch (e: Exception) {
            // اگه نتونست، از لیست پیش‌فرض استفاده می‌کنه
        }
    }

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        initCloudflareRanges()

        val ip = resolveToIp(address) ?: return@withContext ServerType.UNKNOWN to null

        if (isCloudflareIp(ip)) {
            return@withContext ServerType.CLOUDFLARE to ip
        }

        if (isDatacenterIp(ip)) {
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

    private fun isCloudflareIp(ip: String): Boolean {
        return cloudflareRanges.any { ip.startsWith(it, ignoreCase = true) }
    }

    private fun isDatacenterIp(ip: String): Boolean {
        return datacenterRanges.any { ip.startsWith(it) }
    }
}