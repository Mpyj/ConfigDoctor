package com.configdoctor.parser

import android.net.Uri
import android.util.Base64
import com.configdoctor.model.VlessConfig
import org.json.JSONObject

object VmessParser : ConfigParser {

    override fun canParse(link: String): Boolean = link.startsWith("vmess://", ignoreCase = true)

    override fun parse(link: String): Result<VlessConfig> {
        return try {
            val base64Part = link.removePrefix("vmess://").trim()
            val decoded = String(Base64.decode(base64Part, Base64.DEFAULT))
            val json = JSONObject(decoded)

            val config = VlessConfig(
                rawLink = link,
                protocol = "vmess",
                uuid = json.getString("id"),
                address = json.getString("add"),
                port = json.getInt("port"),
                sni = json.optString("sni", json.optString("host", null)),
                host = json.optString("host", null),
                path = json.optString("path", null),
                networkType = json.optString("net", "tcp"),
                security = if (json.optString("tls") == "tls") "tls" else "none",
                fingerprint = null, // VMess معمولاً fingerprint جدا نداره
                flow = null,
                publicKey = null,
                shortId = null
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("خطا در پارس لینک VMess: ${e.message}"))
        }
    }

    override fun buildLink(config: VlessConfig): String {
        // برای سادگی، فقط لینک رو با فیلدهای ضروری می‌سازیم
        val json = JSONObject().apply {
            put("v", "2")
            put("ps", "Config_Doctor_Healed")
            put("add", config.address)
            put("port", config.port.toString())
            put("id", config.uuid)
            put("aid", "0")
            put("net", config.networkType)
            put("type", "none")
            put("host", config.host ?: "")
            put("path", config.path ?: "")
            put("tls", if (config.security == "tls") "tls" else "")
            put("sni", config.sni ?: "")
        }
        val base64 = Base64.encodeToString(json.toString().toByteArray(), Base64.NO_WRAP)
        return "vmess://$base64"
    }
}