package com.configdoctor.parser

import android.net.Uri
import com.configdoctor.model.VlessConfig

object TrojanParser : ConfigParser {

    override fun canParse(link: String): Boolean = link.startsWith("trojan://", ignoreCase = true)

    override fun parse(link: String): Result<VlessConfig> {
        return try {
            val uri = Uri.parse(link)
            val password = uri.userInfo ?: return Result.failure(IllegalArgumentException("پسورد پیدا نشد"))
            val address = uri.host ?: return Result.failure(IllegalArgumentException("آدرس پیدا نشد"))
            val port = if (uri.port > 0) uri.port else 443

            val params = mutableMapOf<String, String>()
            uri.queryParameterNames.forEach { name ->
                uri.getQueryParameter(name)?.let { params[name] = it }
            }

            val config = VlessConfig(
                rawLink = link,
                protocol = "trojan",
                uuid = password, // توی Trojan، پسورد جای UUID رو می‌گیره
                address = address,
                port = port,
                sni = params["sni"] ?: params["peer"],
                host = params["host"],
                path = params["path"],
                networkType = params["type"] ?: "tcp",
                security = params["security"] ?: "tls", // Trojan معمولاً TLS داره
                fingerprint = params["fp"],
                flow = null,
                publicKey = null,
                shortId = null
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("خطا در پارس لینک Trojan: ${e.message}"))
        }
    }

    override fun buildLink(config: VlessConfig): String {
        val sb = StringBuilder()
        sb.append("trojan://")
        sb.append(config.uuid) // پسورد
        sb.append("@")
        sb.append(if (config.address.contains(":")) "[${config.address}]" else config.address)
        sb.append(":")
        sb.append(if (config.port > 0) config.port else 443)
        sb.append("?security=${config.security}&type=${config.networkType}")
        if (!config.sni.isNullOrBlank()) sb.append("&sni=${config.sni}")
        if (!config.host.isNullOrBlank()) sb.append("&host=${config.host}")
        if (!config.path.isNullOrBlank()) sb.append("&path=${config.path}")
        sb.append("#Config_Doctor_Healed")
        return sb.toString()
    }
}