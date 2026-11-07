package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object XrayRealDelayTester {

    // آدرسی که v2rayNG هم استفاده می‌کنه
    private const val TEST_URL = "http://www.gstatic.com/generate_204"

    suspend fun test(
        context: Context,
        config: VlessConfig,
        timeoutMs: Long = 10000L
    ): RealDelayResult = withContext(Dispatchers.IO) {
        try {
            // ۱. تبدیل کانفیگ VLESS به JSON که libv2ray می‌فهمه
            val xrayJson = buildXrayConfigJson(config)
                ?: return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "خطا در ساخت کانفیگ Xray"
                )

            // ۲. صدا زدن متد اصلی (از کتابخانه libv2ray)
            // این متد همون چیزیه که v2rayNG استفاده می‌کنه 
            val delay = Libv2ray.measureOutboundDelay(xrayJson, TEST_URL)

            // ۳. بررسی نتیجه
            if (delay > 0 && delay < timeoutMs) {
                RealDelayResult(ok = true, delayMs = delay, error = null)
            } else {
                RealDelayResult(ok = false, delayMs = null, error = "تایم‌اوت یا کانفیگ نامعتبر")
            }
        } catch (e: Exception) {
            RealDelayResult(ok = false, delayMs = null, error = e.message ?: "خطای ناشناخته")
        }
    }

    /**
     * ساخت JSON کانفیگ Xray از VlessConfig
     * ساختار دقیقاً مثل خروجی v2rayNG هست
     */
    private fun buildXrayConfigJson(config: VlessConfig): String? {
        return try {
            val root = JSONObject()

            // بخش Inbounds (یه پورت لوکال socks برای تست)
            val inbounds = org.json.JSONArray()
            val inbound = JSONObject()
            inbound.put("tag", "socks")
            inbound.put("port", 10808)
            inbound.put("listen", "127.0.0.1")
            inbound.put("protocol", "socks")
            val inboundSettings = JSONObject()
            inboundSettings.put("auth", "noauth")
            inboundSettings.put("udp", false)
            inbound.put("settings", inboundSettings)
            inbounds.put(inbound)
            root.put("inbounds", inbounds)

            // بخش Outbounds (خود کانفیگ VLESS)
            val outbounds = org.json.JSONArray()
            val outbound = JSONObject()
            outbound.put("tag", "proxy")
            outbound.put("protocol", "vless")

            val settings = JSONObject()
            val vnext = org.json.JSONArray()
            val server = JSONObject()
            server.put("address", config.address)
            server.put("port", config.port)

            val users = org.json.JSONArray()
            val user = JSONObject()
            user.put("id", config.uuid)
            user.put("encryption", "none")
            if (!config.flow.isNullOrBlank()) {
                user.put("flow", config.flow)
            }
            users.put(user)
            server.put("users", users)
            vnext.put(server)
            settings.put("vnext", vnext)
            outbound.put("settings", settings)

            // Stream Settings
            val streamSettings = JSONObject()
            streamSettings.put("network", config.networkType)
            streamSettings.put("security", config.security)

            // تنظیمات TLS
            if (config.security.equals("tls", ignoreCase = true)) {
                val tlsSettings = JSONObject()
                tlsSettings.put("serverName", config.sni ?: config.host ?: config.address)
                tlsSettings.put("allowInsecure", false)
                if (!config.fingerprint.isNullOrBlank()) {
                    tlsSettings.put("fingerprint", config.fingerprint)
                }
                streamSettings.put("tlsSettings", tlsSettings)
            }

            // تنظیمات Reality
            if (config.security.equals("reality", ignoreCase = true)) {
                val realitySettings = JSONObject()
                realitySettings.put("serverName", config.sni ?: config.address)
                realitySettings.put("fingerprint", config.fingerprint ?: "chrome")
                if (!config.publicKey.isNullOrBlank()) {
                    realitySettings.put("publicKey", config.publicKey)
                }
                if (!config.shortId.isNullOrBlank()) {
                    realitySettings.put("shortId", config.shortId)
                }
                streamSettings.put("realitySettings", realitySettings)
            }

            // تنظیمات WebSocket
            if (config.networkType.equals("ws", ignoreCase = true)) {
                val wsSettings = JSONObject()
                wsSettings.put("path", config.path ?: "/")
                if (!config.host.isNullOrBlank()) {
                    val headers = JSONObject()
                    headers.put("Host", config.host)
                    wsSettings.put("headers", headers)
                }
                streamSettings.put("wsSettings", wsSettings)
            }

            // تنظیمات gRPC
            if (config.networkType.equals("grpc", ignoreCase = true)) {
                val grpcSettings = JSONObject()
                grpcSettings.put("serviceName", config.path ?: "")
                streamSettings.put("grpcSettings", grpcSettings)
            }

            outbound.put("streamSettings", streamSettings)
            outbounds.put(outbound)
            root.put("outbounds", outbounds)

            root.toString()
        } catch (e: Exception) {
            null
        }
    }

    data class RealDelayResult(
        val ok: Boolean,
        val delayMs: Long?,
        val error: String?
    )
}