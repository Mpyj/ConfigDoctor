package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val tlsOk: Boolean?,
    val tlsError: String?,
    val overallStatus: OverallStatus,
    val summary: String
)

object Diagnostician {

    suspend fun diagnose(context: Context, config: VlessConfig): DiagnosisResult = withContext(Dispatchers.IO) {
        
        // ۱. تشخیص نوع سرور
        val (serverType, resolvedIp) = ServerTypeDetector.detect(config.address)

        // ۲. تست TCP (فقط برای اینکه ببینیم پورت بازه یا نه)
        val tcp = TcpTester.test(config.address, config.port)

        if (!tcp.ok) {
            return@withContext DiagnosisResult(
                config = config,
                serverType = serverType,
                resolvedIp = resolvedIp,
                tcpOk = false,
                tcpLatencyMs = null,
                realDelayMs = null,
                tlsOk = null,
                tlsError = null,
                overallStatus = if (resolvedIp == null) OverallStatus.DNS_FAILED else OverallStatus.IP_DEAD,
                summary = "❌ سرور پاسخ نمی‌دهد.\nآی‌پی سرور احتمالاً سوخته یا پورت بسته است.\nخطا: ${tcp.error}"
            )
        }

        // ۳. تست TLS
        var tlsOk: Boolean? = null
        var tlsError: String? = null

        if (config.security.equals("tls", ignoreCase = true) ||
            config.security.equals("reality", ignoreCase = true)
        ) {
            val sniForTest = config.sni ?: config.host
            val tls = TlsTester.test(config.address, config.port, sniForTest)
            tlsOk = tls.ok
            tlsError = tls.error
        }

        // ۴. Real Delay (فعلاً placeholder)
        val realDelay = XrayRealDelayTester.test(context, config)

        // ۵. جمع‌بندی
        val status: OverallStatus
        val summary: String

        when {
            realDelay.ok -> {
                status = OverallStatus.HEALTHY
                summary = buildString {
                    append("✅ سرور سالم است.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    append("Real Delay: ${realDelay.delayMs}ms\n")
                    if (tcpLatencyMs != null) append("TCP RTT: ${tcp.latencyMs}ms\n")
                    if (tlsOk == true) append("TLS: موفق")
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
                    append("TCP وصل شد ولی Real Delay برقرار نشد.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    if (realDelay.error != null) append("خطا: ${realDelay.error}")
                }
            }
        }

        return@withContext DiagnosisResult(
            config = config,
            serverType = serverType,
            resolvedIp = resolvedIp,
            tcpOk = true,
            tcpLatencyMs = tcp.latencyMs,
            realDelayMs = realDelay.delayMs,
            tlsOk = tlsOk,
            tlsError = tlsError,
            overallStatus = status,
            summary = summary
        )
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DATACENTER -> "دیتاسنتر / VPS"
        ServerType.DIRECT -> "سرور مستقیم (Residential/Personal)"
        ServerType.UNKNOWN -> "نامشخص"
    }
}