package com.configdoctor.parser

import android.net.Uri
import com.configdoctor.model.VlessConfig

object VlessParser {

    fun parse(link: String): Result<VlessConfig> {
        val trimmed = link.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("لینک باید با vless:// شروع بشه"))
        }

        return try {
            val uri = Uri.parse(trimmed)
            val uuid = uri.userInfo ?: return Result.failure(IllegalArgumentException("UUID پیدا نشد"))
            
            // اگه host خالی بود، احتمالاً آدرس به شکل غیراستاندارد نوشته شده
            var address = uri.host
            if (address.isNullOrBlank()) {
                // تلاش برای استخراج دستی از بخش authority
                val authority = uri.authority ?: ""
                val parts = authority.split("@").lastOrNull()?.split(":")
                if (parts != null && parts.isNotEmpty()) {
                    address = parts[0]
                }
            }
            
            if (address.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("آدرس سرور پیدا نشد"))
            }

            // پورت: اگه -1 بود، 443 بذار
            var port = uri.port
            if (port <= 0) {
                port = 443
            }

            val params = mutableMapOf<String, String>()
            uri.queryParameterNames.forEach { name ->
                uri.getQueryParameter(name)?.let { params[name] = it }
            }

            val config = VlessConfig(
                rawLink = trimmed,
                uuid = uuid,
                address = address,
                port = port,
                sni = params["sni"] ?: params["peer"],
                host = params["host"],
                path = params["path"],
                networkType = params["type"] ?: "tcp",
                security = params["security"] ?: "none",
                fingerprint = params["fp"],
                flow = params["flow"],
                publicKey = params["pbk"],
                shortId = params["sid"]
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildLink(config: VlessConfig): String {
        val sb = StringBuilder()
        sb.append("vless://")
        sb.append(config.uuid)
        sb.append("@")
        sb.append(config.address)
        sb.append(":")
        sb.append(if (config.port > 0) config.port else 443)
        sb.append("?")

        val params = mutableListOf<String>()
        params.add("security=${config.security}")
        params.add("type=${config.networkType}")
        
        if (!config.sni.isNullOrBlank()) params.add("sni=${config.sni}")
        if (!config.host.isNullOrBlank()) params.add("host=${config.host}")
        if (!config.path.isNullOrBlank()) params.add("path=${config.path}")
        if (!config.fingerprint.isNullOrBlank()) params.add("fp=${config.fingerprint}")
        if (!config.flow.isNullOrBlank()) params.add("flow=${config.flow}")
        if (!config.publicKey.isNullOrBlank()) params.add("pbk=${config.publicKey}")
        if (!config.shortId.isNullOrBlank()) params.add("sid=${config.shortId}")

        sb.append(params.joinToString("&"))
        sb.append("#Config_Doctor_Healed")
        
        return sb.toString()
    }
}