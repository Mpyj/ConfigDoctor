package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

enum class OverallStatus {
    HEALTHY,
    IP_DEAD,
    SNI_FILTERED,
    TLS_ERROR,
    DNS_FAILED,
    UNKNOWN
}

data class DiagnosisResult(
    val config: VlessConfig,
    val serverType: ServerType,
    val resolvedIp: String?,
    val tcpOk: Boolean,
    val tcpLatencyMs: Long?,
    val realDelayMs: Long?,
    val outputIp: String?,
    val outputCountry: String?,
    val tlsOk: Boolean?,
    val tlsError: String?,
    val overallStatus: OverallStatus,
    val summary: String
)

object Diagnostician {

    suspend fun diagnose(context: Context, config: VlessConfig): DiagnosisResult = withContext(Dispatchers.IO) {
        val (serverType, resolvedIp) = ServerTypeDetector.detect(config.address)
        val tcp = TcpTester.test(config.address, config.port)

        if (!tcp.ok) {
            return@withContext DiagnosisResult(
                config = config, serverType = serverType, resolvedIp = resolvedIp,
                tcpOk = false, tcpLatencyMs = null, realDelayMs = null, outputIp = null,
                outputCountry = null, tlsOk = null, tlsError = null,
                overallStatus = if (resolvedIp == null) OverallStatus.DNS_FAILED else OverallStatus.IP_DEAD,
                summary = "❌ سرور پاسخ نمی‌دهد.\nآی‌پی سرور احتمالاً سوخته یا مسیر اینترنت شما مشکل دارد.\nخطا: ${tcp.error}"
            )
        }

        var tlsOk: Boolean? = null
        var tlsError: String? = null

        if (config.security.equals("tls", ignoreCase = true) || config.security.equals("reality", ignoreCase = true)) {
            val sniForTest = config.sni ?: config.host
            val tls = TlsTester.test(config.address, config.port, sniForTest)
            tlsOk = tls.ok
            tlsError = tls.error
        }

        val realDelay = XrayRealDelayTester.test(context, config)

        val status: OverallStatus
        val summary: String

        when {
            realDelay.ok -> {
                status = OverallStatus.HEALTHY
                summary = buildString {
                    append("✅ سرور سالم است.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    if (realDelay.delayMs != null) append("Real Delay: ${realDelay.delayMs}ms\n")
                    if (tcp.latencyMs != null) append("TCP RTT: ${tcp.latencyMs}ms\n")
                    if (tlsOk == true) append("TLS: موفق\n")
                }
            }
            tlsOk == false -> {
                status = OverallStatus.SNI_FILTERED
                summary = buildString {
                    append("⚠️ TCP وصل شد ولی TLS شکست خورد.\n")
                    append("احتمالاً SNI فیلتر شده یا گواهی مشکل دارد.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    append("خطای TLS: $tlsError")
                }
            }
            else -> {
                status = OverallStatus.UNKNOWN
                summary = buildString {
                    append("⚠️ وضعیت نامشخص.\n")
                    append("TCP وصل شد ولی Real Delay برقرار نشد (تونل خراب است).\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    if (realDelay.error != null) append("خطا: ${realDelay.error}")
                }
            }
        }

        return@withContext DiagnosisResult(
            config = config, serverType = serverType, resolvedIp = resolvedIp,
            tcpOk = true, tcpLatencyMs = tcp.latencyMs, realDelayMs = realDelay.delayMs,
            outputIp = realDelay.ip, outputCountry = realDelay.country,
            tlsOk = tlsOk, tlsError = tlsError, overallStatus = status, summary = summary
        )
    }

    /**
     * ترمیم کانفیگ: تست هوشمند همه‌ی مسیرها (IPv4/IPv6/دامنه)
     */
    suspend fun healConfig(context: Context, config: VlessConfig): VlessConfig? = withContext(Dispatchers.IO) {
        
        // ۱. اول ببین آدرس فعلی IPv4 هست یا IPv6 یا دامنه
        val currentIps = try {
            InetAddress.getAllByName(config.address).map { it.hostAddress }
        } catch (e: Exception) { emptyList() }

        val hasIpv4 = currentIps.any { it.contains(".") }
        val hasIpv6 = currentIps.any { it.contains(":") }

        // ۲. لیست کاندیداها برای تست (اولویت‌بندی شده)
        val candidates = mutableListOf<String>()

        // اگه IPv4 داره ولی وصل نمی‌شه، اول IPv6ش رو امتحان کن
        if (hasIpv4) {
            currentIps.filter { it.contains(":") }.forEach { candidates.add(it) }
        }
        
        // اگه IPv6 داره ولی وصل نمی‌شه، اول IPv4ش رو امتحان کن
        if (hasIpv6) {
            currentIps.filter { it.contains(".") }.forEach { candidates.add(it) }
        }

        // ۳. در نهایت، IPهای تمیز کلادفلر (هم IPv4 هم IPv6)
        CloudflareCleanIps.ips.forEach { candidates.add(it) }

        // ۴. تست کاندیداها به ترتیب
        for (candidateIp in candidates.distinct()) {
            val newConfig = config.copy(address = candidateIp)
            
            // تست TCP (سریع)
            val tcpTest = TcpTester.test(candidateIp, config.port, timeoutMs = 3000)
            if (tcpTest.ok) {
                // تست کامل Real Delay (برای اطمینان از سالم بودن تونل)
                val realDelayTest = XrayRealDelayTester.test(context, newConfig, timeoutMs = 5000)
                if (realDelayTest.ok) {
                    return@withContext newConfig // یه مسیر سالم پیدا شد!
                }
            }
        }
        
        return@withContext null // هیچ مسیر سالمی پیدا نشد
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DATACENTER -> "دیتاسنتر / VPS"
        ServerType.DIRECT -> "سرور مستقیم"
        ServerType.UNKNOWN -> "نامشخص"
    }
}