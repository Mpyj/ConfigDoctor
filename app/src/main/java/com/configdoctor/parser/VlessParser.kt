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
            val address = uri.host ?: return Result.failure(IllegalArgumentException("آدرس سرور پیدا نشد"))
            
            // اگه پورت -1 باشه یعنی توی لینک نبوده، از پارامترها یا پیش‌فرض استفاده کن
            val port = if (uri.port > 0) uri.port else {
                // اگه پورت توی URI نبود، شاید توی query param باشه (نادر)
                val queryPort = uri.getQueryParameter("port")
                if (queryPort != null) queryPort.toIntOrNull() ?: 443 else 443
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

    // ساخت دستی لینک (بدون Uri.Builder برای جلوگیری از باگ‌های اندروید)
    fun buildLink(config: VlessConfig): String {
        val sb = StringBuilder()
        sb.append("vless://")
        sb.append(config.uuid)
        sb.append("@")
        sb.append(config.address)
        sb.append(":")
        sb.append(config.port)
        sb.append("?")

        val params = mutableListOf<String>()
        
        // پارامترهای ضروری
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