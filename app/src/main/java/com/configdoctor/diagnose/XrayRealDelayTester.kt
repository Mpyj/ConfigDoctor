package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

object XrayRealDelayTester {

    private const val TEST_URL = "http://www.gstatic.com/generate_204"
    private const val CLOUDFLARE_TRACE_URL = "https://www.cloudflare.com/cdn-cgi/trace"
    private const val IPIFY_URL = "https://api.ipify.org?format=json"
    private const val HTTP_PORT = 10809

    suspend fun test(
        context: Context,
        config: VlessConfig,
        timeoutMs: Long = 10000L
    ): RealDelayResult = withContext(Dispatchers.IO) {
        try {
            val xrayJson = buildXrayConfigJson(config)
                ?: return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "خطا در ساخت کانفیگ Xray",
                    ip = null, country = null
                )

            // ۱. اندازه‌گیری Real Delay
            val delay = Libv2ray.measureOutboundDelay(xrayJson, TEST_URL)

            if (delay <= 0 || delay >= timeoutMs) {
                return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "تایم‌اوت یا کانفیگ نامعتبر",
                    ip = null, country = null
                )
            }

            // ۲. گرفتن IP و کشور از طریق تونل موقت
            val (ip, country) = fetchIpAndCountry(config)

            RealDelayResult(
                ok = true,
                delayMs = delay,
                error = null,
                ip = ip,
                country = country
            )
        } catch (e: Exception) {
            RealDelayResult(
                ok = false, delayMs = null, error = e.message ?: "خطای ناشناخته",
                ip = null, country = null
            )
        }
    }

    private fun fetchIpAndCountry(config: VlessConfig): Pair<String?, String?> {
        val xrayJson = buildXrayConfigJson(config) ?: return Pair(null, null)
        
        val callback = object : CoreCallbackHandler {
            override fun startup(): Long = 0L
            override fun shutdown(): Long = 0L
            override fun onEmitStatus(code: Long, msg: String?): Long = 0L
        }
        
        val controller = CoreController(callback)
        
        return try {
            controller.startLoop(xrayJson, 0)
            Thread.sleep(1000)
            
            var result = fetchFromCloudflareTrace()
            
            if (result.first == null) {
                result = fetchFromIpify()
            }
            
            controller.stopLoop()
            result
        } catch (e: Exception) {
            try { controller.stopLoop() } catch (_: Exception) {}
            Pair(null, null)
        }
    }

    private fun fetchFromCloudflareTrace(): Pair<String?, String?> {
        return try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", HTTP_PORT))
            val url = URL(CLOUDFLARE_TRACE_URL)
            val conn = url.openConnection(proxy) as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) {
                return Pair(null, null)
            }

            val body = conn.inputStream.bufferedReader().readText()
            var ip: String? = null
            var country: String? = null
            body.lines().forEach { line ->
                if (line.startsWith("ip=")) ip = line.removePrefix("ip=").trim()
                if (line.startsWith("loc=")) country = line.removePrefix("loc=").trim()
            }
            Pair(ip, country)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun fetchFromIpify(): Pair<String?, String?> {
        return try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", HTTP_PORT))
            val url = URL(IPIFY_URL)
            val conn = url.openConnection(proxy) as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode != 200) {
                return Pair(null, null)
            }

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val ip = json.optString("ip", null)
            
            val country = if (ip != null) fetchCountryFromIpApi(ip) else null
            Pair(ip, country)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun fetchCountryFromIpApi(ip: String): String? {
        return try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", HTTP_PORT))
            val url = URL("http://ip-api.com/json/$ip?fields=countryCode")
            val conn = url.openConnection(proxy) as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            JSONObject(body).optString("countryCode", null)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildXrayConfigJson(config: VlessConfig): String? {
        return try {
            val root = JSONObject()

            val inbounds = JSONArray()

            val socksInbound = JSONObject()
            socksInbound.put("tag", "socks")
            socksInbound.put("port", 10808)
            socksInbound.put("listen", "127.0.0.1")
            socksInbound.put("protocol", "socks")
            val socksSettings = JSONObject()
            socksSettings.put("auth", "noauth")
            socksSettings.put("udp", false)
            socksInbound.put("settings", socksSettings)
            inbounds.put(socksInbound)

            val httpInbound = JSONObject()
            httpInbound.put("tag", "http")
            httpInbound.put("port", HTTP_PORT)
            httpInbound.put("listen", "127.0.0.1")
            httpInbound.put("protocol", "http")
            val httpSettings = JSONObject()
            httpSettings.put("allowTransparent", false)
            httpInbound.put("settings", httpSettings)
            inbounds.put(httpInbound)

            root.put("inbounds", inbounds)

            val outbounds = JSONArray()
            val outbound = JSONObject()
            outbound.put("tag", "proxy")
            outbound.put("protocol", "vless")

            val settings = JSONObject()
            val vnext = JSONArray()
            val server = JSONObject()
            server.put("address", config.address)
            server.put("port", config.port)

            val users = JSONArray()
            val user = JSONObject()
            user.put("id", config.uuid)
            user.put("encryption", "none")
            if (!config.flow.isNullOrBlank()) user.put("flow", config.flow)
            users.put(user)
            server.put("users", users)
            vnext.put(server)
            settings.put("vnext", vnext)
            outbound.put("settings", settings)

            val streamSettings = JSONObject()
            streamSettings.put("network", config.networkType.ifBlank { "tcp" })
            streamSettings.put("security", config.security.ifBlank { "none" })

            if (config.security.equals("tls", ignoreCase = true)) {
                val tlsSettings = JSONObject()
                tlsSettings.put("serverName", config.sni ?: config.host ?: config.address)
                tlsSettings.put("allowInsecure", false)
                if (!config.fingerprint.isNullOrBlank()) tlsSettings.put("fingerprint", config.fingerprint)
                streamSettings.put("tlsSettings", tlsSettings)
            }

            if (config.security.equals("reality", ignoreCase = true)) {
                val realitySettings = JSONObject()
                realitySettings.put("serverName", config.sni ?: config.address)
                realitySettings.put("fingerprint", config.fingerprint ?: "chrome")
                if (!config.publicKey.isNullOrBlank()) realitySettings.put("publicKey", config.publicKey)
                if (!config.shortId.isNullOrBlank()) realitySettings.put("shortId", config.shortId)
                streamSettings.put("realitySettings", realitySettings)
            }

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
        val error: String?,
        val ip: String?,
        val country: String?
    )
}