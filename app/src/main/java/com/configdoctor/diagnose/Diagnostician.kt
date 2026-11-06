package com.configdoctor.diagnose

import com.configdoctor.model.VlessConfig

enum class OverallStatus {
    HEALTHY,
    IP_DEAD,
    SNI_FILTERED,
    TLS_ERROR,
    DNS_FAILED,
    PARSED_ONLY
}

data class DiagnosisResult(
    val config: VlessConfig,
    val serverType: ServerType,
    val resolvedIp: String?,
    val tcpOk: Boolean,
    val tcpLatencyMs: Long?,
    val tlsOk: Boolean?,
    val tlsError: String?,
    val overallStatus: OverallStatus,
    val summary: String
)

object Diagnostician {

    fun diagnose(config: VlessConfig): DiagnosisResult {
        // ۱. تشخیص نوع سرور
        val (serverType, resolvedIp) = ServerTypeDetector.detect(config.address)

        // ۲. تست TCP
        val tcp = TcpTester.test(config.address, config.port)

        if (!tcp.ok) {
            val summary = buildString {
                append("❌ سرور پاسخ نمی‌دهد.\n")
                append("آی‌پی سرور احتمالاً سوخته یا پورت بسته است.\n")
                append("جزئیات: ${tcp.error}")
            }
            return DiagnosisResult(
                config = config,
                serverType = serverType,
                resolvedIp = resolvedIp,
                tcpOk = false,
                tcpLatencyMs = null,
                tlsOk = null,
                tlsError = null,
                overallStatus = if (resolvedIp == null) OverallStatus.DNS_FAILED else OverallStatus.IP_DEAD,
                summary = summary
            )
        }

        // ۳. تست TLS (اگه امنیت TLS/Reality بود)
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

        // ۴. جمع‌بندی
        val status: OverallStatus
        val summary: String

        when {
            tlsOk == true -> {
                status = OverallStatus.HEALTHY
                summary = buildString {
                    append("✅ سرور سالم است.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    append("پینگ TCP: ${tcp.latencyMs}ms\n")
                    append("TLS Handshake: موفق")
                }
            }
            tlsOk == false -> {
                status = OverallStatus.SNI_FILTERED
                summary = buildString {
                    append("⚠️ TCP وصل شد ولی TLS شکست خورد.\n")
                    append("احتمالاً SNI فیلتر شده یا گواهی مشکل دارد.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    append("پینگ TCP: ${tcp.latencyMs}ms\n")
                    append("خطای TLS: $tlsError")
                }
            }
            else -> {
                status = OverallStatus.HEALTHY
                summary = buildString {
                    append("✅ سرور پاسخ می‌دهد.\n")
                    append("نوع سرور: ${serverTypeLabel(serverType)}\n")
                    append("پینگ TCP: ${tcp.latencyMs}ms\n")
                    append("(تست TLS انجام نشد — امنیت none)")
                }
            }
        }

        return DiagnosisResult(
            config = config,
            serverType = serverType,
            resolvedIp = resolvedIp,
            tcpOk = true,
            tcpLatencyMs = tcp.latencyMs,
            tlsOk = tlsOk,
            tlsError = tlsError,
            overallStatus = status,
            summary = summary
        )
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DIRECT -> "سرور مستقیم"
        ServerType.UNKNOWN -> "نامشخص"
    }
}