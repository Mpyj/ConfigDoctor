package com.configdoctor.parser

import android.net.Uri
import com.configdoctor.model.VlessConfig

object VlessParser {

    fun parse(link: String): Result<VlessConfig> {
        // ... (کد قبلی پارس بدون تغییر) ...
        val trimmed = link.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("لینک باید با vless:// شروع بشه"))
        }

        return try {
            val uri = Uri.parse(trimmed)
            val uuid = uri.userInfo ?: return Result.failure(IllegalArgumentException("UUID پیدا نشد"))
            val address = uri.host ?: return Result.failure(IllegalArgumentException("آدرس سرور پیدا نشد"))
            val port = if (uri.port > 0) uri.port else return Result.failure(IllegalArgumentException("پورت معتبر نیست"))

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

    // تابع جدید برای ساخت لینک از روی کانفیگ (برای ترمیم)
    fun buildLink(config: VlessConfig): String {
        val builder = Uri.Builder()
            .scheme("vless")
            .authority("${config.uuid}@${config.address}:${config.port}")

        if (config.security.isNotBlank() && config.security != "none") builder.appendQueryParameter("security", config.security)
        if (config.networkType.isNotBlank()) builder.appendQueryParameter("type", config.networkType)
        if (!config.sni.isNullOrBlank()) builder.appendQueryParameter("sni", config.sni)
        if (!config.host.isNullOrBlank()) builder.appendQueryParameter("host", config.host)
        if (!config.path.isNullOrBlank()) builder.appendQueryParameter("path", config.path)
        if (!config.fingerprint.isNullOrBlank()) builder.appendQueryParameter("fp", config.fingerprint)
        if (!config.flow.isNullOrBlank()) builder.appendQueryParameter("flow", config.flow)
        if (!config.publicKey.isNullOrBlank()) builder.appendQueryParameter("pbk", config.publicKey)
        if (!config.shortId.isNullOrBlank()) builder.appendQueryParameter("sid", config.shortId)

        // یه اسم برای کانفیگ ترمیم شده
        builder.fragment("Config_Doctor_Healed")
        
        return builder.build().toString()
    }
}