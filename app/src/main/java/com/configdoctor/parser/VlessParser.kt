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
            
            // ۱. استخراج آدرس (host)
            var address = uri.host
            if (address.isNullOrBlank()) {
                // تلاش برای استخراج دستی از authority (مخصوصاً وقتی براکت IPv6 وجود داره)
                val authority = uri.authority ?: ""
                val withoutUser = authority.substringAfter("@", authority)
                // اگر IPv6 داخل براکت باشه، تا قبل از ] رو برمیداریم
                address = if (withoutUser.contains("[")) {
                    withoutUser.substringAfter("[").substringBefore("]")
                } else {
                    withoutUser.substringBefore(":")
                }
            }
            
            if (address.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("آدرس سرور پیدا نشد"))
            }

            // ۲. استخراج پورت (اگه نبود، 443)
            var port = uri.port
            if (port <= 0) {
                // تلاش برای استخراج دستی پورت از authority
                val authority = uri.authority ?: ""
                val parts = authority.substringAfter("@", authority).split(":")
                if (parts.size > 1) {
                    port = parts.last().toIntOrNull() ?: 443
                } else {
                    port = 443
                }
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

    // ساخت لینک با استفاده از URI استاندارد جاوا (جلوگیری از باگ‌های اندروید)
    fun buildLink(config: VlessConfig): String {
        return try {
            // چک میکنیم اگه IPv6 بود، داخل براکت بذاریم
            val formattedAddress = if (config.address.contains(":")) "[${config.address}]" else config.address

            val sb = StringBuilder()
            sb.append("vless://")
            sb.append(config.uuid)
            sb.append("@")
            sb.append(formattedAddress)
            sb.append(":")
            sb.append(if (config.port > 0) config.port else 443)
            
            // ساخت Query String
            val queryParams = mutableListOf<String>()
            queryParams.add("security=${config.security}")
            queryParams.add("type=${config.networkType}")
            
            if (!config.sni.isNullOrBlank()) queryParams.add("sni=${Uri.encode(config.sni)}")
            if (!config.host.isNullOrBlank()) queryParams.add("host=${Uri.encode(config.host)}")
            if (!config.path.isNullOrBlank()) queryParams.add("path=${Uri.encode(config.path)}")
            if (!config.fingerprint.isNullOrBlank()) queryParams.add("fp=${config.fingerprint}")
            if (!config.flow.isNullOrBlank()) queryParams.add("flow=${config.flow}")
            if (!config.publicKey.isNullOrBlank()) queryParams.add("pbk=${config.publicKey}")
            if (!config.shortId.isNullOrBlank()) queryParams.add("sid=${config.shortId}")

            sb.append("?")
            sb.append(queryParams.joinToString("&"))
            sb.append("#Config_Doctor_Healed")

            sb.toString()
        } catch (e: Exception) {
            // فالبک در صورت خطا
            "vless://${config.uuid}@${config.address}:${if (config.port > 0) config.port else 443}?security=${config.security}&type=${config.networkType}"
        }
    }
}