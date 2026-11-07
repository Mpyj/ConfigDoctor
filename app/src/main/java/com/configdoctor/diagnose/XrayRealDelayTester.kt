package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

object XrayRealDelayTester {

    private const val TEST_URL = "http://www.gstatic.com/generate_204"

    // گرفتن Real Delay، IP خروجی و کشور
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

            // ۱. گرفتن Real Delay
            val delay = Libv2ray.measureOutboundDelay(xrayJson, TEST_URL)

            if (delay <= 0 || delay >= timeoutMs) {
                return@withContext RealDelayResult(
                    ok = false, delayMs = null, error = "تایم‌اوت یا کانفیگ نامعتبر",
                    ip = null, country = null
                )
            }

            // ۲. گرفتن IP و کشور از طریق SOCKS5
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

    /**
     * گرفتن IP و کشور از داخل تونل Xray (پورت SOCKS5 لوکال)
     * از api.ipify.org برای گرفتن IP و از ip-api.com برای گرفتن کشور استفاده می‌کنیم.
     */
    private fun fetchIpAndCountryViaSocks5(): Pair<String?, String?> {
        return try {
            // ۱. گرفتن IP خروجی از api.ipify.org
            val exitIp = resolveExitIpViaSocks5(10808, "api.ipify.org")
                ?: return null to null

            // ۲. گرفتن کشور از ip-api.com
            val country = resolveCountryViaSocks5(10808, exitIp)

            exitIp to country
        } catch (e: Exception) {
            null to null
        }
    }

    /**
     * اتصال از طریق SOCKS5 به یه دامنه و برگردوندن IP خروجی
     * (بر اساس پیاده‌سازی استاندارد SOCKS5)
     */
    private fun resolveExitIpViaSocks5(socksPort: Int, targetHost: String): String? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", socksPort), 5000)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()
                val inp = socket.getInputStream()

                // SOCKS5 greeting
                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()
                val auth = ByteArray(2)
                inp.read(auth)
                if (auth[1] != 0x00.toByte()) return null

                // CONNECT request
                val hostBytes = targetHost.toByteArray()
                val request = byteArrayOf(
                    0x05, 0x01, 0x00, 0x03,
                    hostBytes.size.toByte()
                ) + hostBytes + byteArrayOf(0x00, 0x50) // port 80
                out.write(request)
                out.flush()

                // Read response (10 bytes for IPv4)
                val resp = ByteArray(10)
                inp.read(resp)
                if (resp[1] != 0x00.toByte()) return null

                // Send HTTP request
                out.write("GET / HTTP/1.1\r\nHost: $targetHost\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()

                val reader = BufferedReader(InputStreamReader(inp))
                val response = reader.readText()
                // IP معمولاً آخرین خط یا خطی که فقط شامل IP هست
                response.lines().lastOrNull { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * گرفتن کشور یه IP از طریق ip-api.com
     */
    private fun resolveCountryViaSocks5(socksPort: Int, ip: String): String? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", socksPort), 5000)
                socket.soTimeout = 5000

                val out = socket.getOutputStream()
                val inp = socket.getInputStream()

                // SOCKS5 greeting
                out.write(byteArrayOf(0x05, 0x01, 0x00))
                out.flush()
                val auth = ByteArray(2)
                inp.read(auth)
                if (auth[1] != 0x00.toByte()) return null

                // CONNECT request to ip-api.com
                val targetHost = "ip-api.com"
                val hostBytes = targetHost.toByteArray()
                val request = byteArrayOf(
                    0x05, 0x01, 0x00, 0x03,
                    hostBytes.size.toByte()
                ) + hostBytes + byteArrayOf(0x00, 0x50) // port 80
                out.write(request)
                out.flush()

                val resp = ByteArray(10)
                inp.read(resp)
                if (resp[1] != 0x00.toByte()) return null

                // HTTP GET to ip-api
                val path = "/json/$ip?fields=countryCode"
                out.write("GET $path HTTP/1.1\r\nHost: $targetHost\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()

                val reader = BufferedReader(InputStreamReader(inp))
                val response = reader.readText()
                // پارس JSON ساده
                val jsonStart = response.indexOf('{')
                if (jsonStart >= 0) {
                    val jsonStr = response.substring(jsonStart)
                    val json = JSONObject(jsonStr)
                    json.optString("countryCode", null)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildXrayConfigJson(config: VlessConfig): String? {
        // همون کد قبلی، بدون تغییر
        // ... (کد ساخت JSON رو از پیام قبلی کپی کن)
        return null // موقت
    }

    data class RealDelayResult(
        val ok: Boolean,
        val delayMs: Long?,
        val error: String?,
        val ip: String?,
        val country: String?
    )
}