package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL
import java.net.HttpURLConnection

enum class ServerType {
    CLOUDFLARE,
    DATACENTER,   // VPS / دیتاسنتر
    DIRECT,       // سرور شخصی / Residential
    UNKNOWN
}

object ServerTypeDetector {

    private val cloudflareRanges = listOf(
        "103.21.244.", "103.22.200.", "103.31.4.",
        "104.16.", "104.24.", "108.162.192.",
        "131.0.72.", "141.101.64.", "162.158.",
        "172.64.", "173.245.48.", "188.114.96.",
        "190.93.240.", "197.234.240.", "198.41.128."
    )

    suspend fun detect(address: String): Pair<ServerType, String?> = withContext(Dispatchers.IO) {
        val ip = resolveToIp(address) ?: return@withContext ServerType.UNKNOWN to null

        // ۱. اول چک کن کلادفلر هست یا نه
        if (isCloudflareIp(ip)) {
            return@withContext ServerType.CLOUDFLARE to ip
        }

        // ۲. از API بپرس دیتاسنتر هست یا نه
        val isDatacenter = checkDatacenter(ip)
        val type = if (isDatacenter) ServerType.DATACENTER else ServerType.DIRECT
        return@withContext type to ip
    }

    private fun resolveToIp(address: String): String? {
        return try {
            InetAddress.getByName(address).hostAddress
        } catch (e: Exception) {
            null
        }
    }

    private fun isCloudflareIp(ip: String): Boolean {
        return cloudflareRanges.any { ip.startsWith(it) }
    }

    private fun checkDatacenter(ip: String): Boolean {
        return try {
            val url = URL("https://api.ipapi.is/?q=$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) return false

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)

            // چک فیلدهای مختلف
            val isDatacenter = json.optBoolean("is_datacenter", false)
            val isHosting = json.optBoolean("is_hosting", false)
            val asn = json.optString("asn", "").lowercase()
            val company = json.optString("company", "").lowercase()

            val hostingKeywords = listOf(
                "digitalocean", "hetzner", "ovh", "vultr", "linode",
                "amazon", "aws", "google cloud", "azure", "microsoft",
                "contabo", "oracle", "cloud", "hosting", "server"
            )

            isDatacenter || isHosting ||
                hostingKeywords.any { asn.contains(it) || company.contains(it) }
        } catch (e: Exception) {
            false
        }
    }
}