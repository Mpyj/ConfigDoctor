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

// یک کلاس داخلی برای نگه‌داری کاندیدای ترمیم و تاخیرش
private data class HealCandidate(
    val config: VlessConfig,
    val realDelayMs: Long
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
     * ترمیم کانفیگ: اسکن سه‌مرحله‌ای با تولید IPهای تصادفی از رنج‌های رسمی کلادفلر
     * @param onProgress یک callback برای گزارش وضعیت به UI
     */
    suspend fun healConfig(
        context: Context, 
        config: VlessConfig,
        onProgress: (String) -> Unit = {}
    ): VlessConfig? = withContext(Dispatchers.IO) {
        val baseConfig = if (config.port <= 0) config.copy(port = 443) else config

        onProgress("در حال تولید IPهای جدید...")
        val randomIps = CloudflareCleanIps.generateRandomIps(100)
        
        val ipv6Candidates = randomIps.filter { it.contains(":") }
        val ipv4Candidates = randomIps.filter { it.contains(".") }
        val allCandidates = (ipv6Candidates + ipv4Candidates).distinct()

        onProgress("مرحله ۱: غربالگری ${allCandidates.size} IP...")
        val tcpPassed = mutableListOf<String>()
        for ((index, ip) in allCandidates.withIndex()) {
            // هر ۱۰ تا یه بار پیشرفت رو آپدیت کن (برای اینکه UI کند نشه)
            if (index % 10 == 0) {
                onProgress("مرحله ۱: تست ${index + 1} از ${allCandidates.size}")
            }
            if (TcpTester.test(ip, baseConfig.port, timeoutMs = 1000).ok) {
                tcpPassed.add(ip)
            }
        }
        
        if (tcpPassed.isEmpty()) {
            onProgress("هیچ IP سالمی در مرحله ۱ پیدا نشد.")
            return@withContext null
        }

        val toTestRealDelay = tcpPassed.take(20)
        onProgress("مرحله ۲: تست Real Delay روی ${toTestRealDelay.size} IP...")
        val realDelayPassed = mutableListOf<HealCandidate>()
        
        for ((index, ip) in toTestRealDelay.withIndex()) {
            onProgress("مرحله ۲: تست ${index + 1} از ${toTestRealDelay.size} ($ip)")
            val newConfig = baseConfig.copy(address = ip)
            val result = XrayRealDelayTester.test(context, newConfig, timeoutMs = 5000)
            if (result.ok && result.delayMs != null) {
                realDelayPassed.add(HealCandidate(newConfig, result.delayMs))
            }
        }
        
        if (realDelayPassed.isEmpty()) {
            onProgress("هیچ تونل سالمی در مرحله ۲ پیدا نشد.")
            return@withContext null
        }

        onProgress("انتخاب بهترین کانفیگ...")
        val best = realDelayPassed.minByOrNull { it.realDelayMs }
        return@withContext best?.config
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DATACENTER -> "دیتاسنتر / VPS"
        ServerType.DIRECT -> "سرور مستقیم"
        ServerType.UNKNOWN -> "نامشخص"
    }
}