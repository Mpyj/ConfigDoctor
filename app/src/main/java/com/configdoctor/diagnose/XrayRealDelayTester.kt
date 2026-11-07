package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

object XrayRealDelayTester {

    private const val TEST_URL = "http://www.gstatic.com/generate_204"

    suspend fun test(
        context: Context,
        config: VlessConfig,
        timeoutMs: Long = 10000L
    ): RealDelayResult = withContext(Dispatchers.IO) {
        try {
            val xrayJson = buildXrayConfigJson(config)
                ?: return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "خطا در ساخت کانفیگ Xray (JSON نامعتبر)",
                    ip = null, country = null
                )

            // ۱. گرفتن Real Delay
            val delay = Libv2ray.measureOutboundDelay(xrayJson, TEST_URL)

            if (delay <= 0 || delay >= timeoutMs) {
                return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "تایم‌اوت یا کانفیگ نامعتبر",
                    ip = null, country = null
                )
            }

            // ۲. گرفتن IP و کشور از طریق SOCKS5
            // صبر می‌کنیم تا تونل Xray کامل بالا بیاد (نیم ثانیه کافیه)
            Thread.sleep(500)
            val (ip, country) = fetchIpAndCountryViaSocks5()

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

    private fun fetchIpAndCountryViaSocks5(): Pair<String?, String?> {
        return try {
            val exitIp = resolveIpViaSocks5(10808, "api.ipify.org")
            if (exitIp == null) return null to null

            val country = resolveCountryViaSocks5(10808, exitIp)
            exitIp to country
        } catch (e: Exception) {
            null to null
        }
    }

    private fun resolveIpViaSocks5(socksPort: Int, targetHost: String): String? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", socksPort), 5000)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()
                val inp = socket.getInputStream()

                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()
                val auth = ByteArray(2)
                inp.read(auth)
                if (auth.size < 2 || auth[1] != 0x00.toByte()) return null

                val hostBytes = targetHost.toByteArray()
                val request = byteArrayOf(0x05, 0x01, 0x00, 0x03, hostBytes.size.toByte()) + hostBytes + byteArrayOf(0x00, 0x50)
                out.write(request)
                out.flush()

                val resp = ByteArray(10)
                inp.read(resp)
                if (resp.size < 2 || resp[1] != 0x00.toByte()) return null

                out.write("GET / HTTP/1.1\r\nHost: $targetHost\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()

                val reader = BufferedReader(InputStreamReader(inp))
                val response = reader.readText()
                val ipRegex = Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")
                ipRegex.find(response)?.value
            }
        } catch (e: Exception) { null }
    }

    private fun resolveCountryViaSocks5(socksPort: Int, ip: String): String? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", socksPort), 5000)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()
                val inp = socket.getInputStream()

                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()
                val auth = ByteArray(2)
                inp.read(auth)
                if (auth.size < 2 || auth[1] != 0x00.toByte()) return null

                val targetHost = "ip-api.com"
                val hostBytes = targetHost.toByteArray()
                val request = byteArrayOf(0x05, 0x01, 0x00, 0x03, hostBytes.size.toByte()) + hostBytes + byteArrayOf(0x00, 0x50)
                out.write(request)
                out.flush()

                val resp = ByteArray(10)
                inp.read(resp)
                if (resp.size < 2 || resp[1] != 0x00.toByte()) return null

                val path = "/json/$ip?fields=countryCode"
                out.write("GET $path HTTP/1.1\r\nHost: $targetHost\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()

                val reader = BufferedReader(InputStreamReader(inp))
                val response = reader.readText()
                val jsonStart = response.indexOf('{')
                if (jsonStart >= 0) {
                    val json = JSONObject(response.substring(jsonStart))
                    json.optString("countryCode", null)
                } else null
            }
        } catch (e: Exception) { null }
    }

    private fun buildXrayConfigJson(config: VlessConfig): String? {
        return try {
            val root = JSONObject()

            // Inbounds
            val inbounds = JSONArray()
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

            // Outbounds
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