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

private data class HealCandidate(
    val config: VlessConfig,
    val realDelayMs: Long
)

object Diagnostician {

    enum class HealMode {
        QUICK,
        DEEP
    }

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
                    if (!realDelay.ip.isNullOrBlank()) append("IP خروجی: ${realDelay.ip}\n")
                    if (!realDelay.country.isNullOrBlank()) append("کشور خروجی: ${formatCountry(realDelay.country)}\n")
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
                    if (!realDelay.ip.isNullOrBlank()) append("\nIP خروجی: ${realDelay.ip}")
                    if (!realDelay.country.isNullOrBlank()) append("\nکشور خروجی: ${formatCountry(realDelay.country)}")
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

    suspend fun healConfig(
        context: Context, 
        config: VlessConfig,
        mode: HealMode = HealMode.DEEP,
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
            if (index % 10 == 0) {
                onProgress("مرحله ۱: تست ${index + 1} از ${allCandidates.size}")
            }
            if (TcpTester.test(ip, baseConfig.port, timeoutMs = 1000).ok) {
                tcpPassed.add(ip)
                if (mode == HealMode.QUICK) {
                    onProgress("حالت سریع: اولین IP سالم پیدا شد.")
                    return@withContext baseConfig.copy(address = ip)
                }
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

        val topCandidates = realDelayPassed.sortedBy { it.realDelayMs }.take(3)
        onProgress("مرحله ۳: تست پایداری روی ${topCandidates.size} کاندیدای برتر...")
        
        var bestStableConfig: VlessConfig? = null
        var bestAvgDelay = Long.MAX_VALUE
        
        for ((index, candidate) in topCandidates.withIndex()) {
            onProgress("مرحله ۳: تست پایداری ${index + 1} از ${topCandidates.size} (${candidate.config.address})")
            val delays = mutableListOf<Long>()
            var successCount = 0
            
            repeat(3) { 
                val result = XrayRealDelayTester.test(context, candidate.config, timeoutMs = 5000)
                if (result.ok && result.delayMs != null) {
                    delays.add(result.delayMs)
                    successCount++
                }
            }
            
            if (successCount >= 2) {
                val avgDelay = delays.average().toLong()
                if (avgDelay < bestAvgDelay) {
                    bestAvgDelay = avgDelay
                    bestStableConfig = candidate.config
                }
            }
        }
        
        if (bestStableConfig == null) {
            onProgress("هیچ کاندیدای پایداری پیدا نشد. استفاده از بهترین گزینه‌ی قبلی...")
            return@withContext realDelayPassed.minByOrNull { it.realDelayMs }?.config
        }
        
        onProgress("بهترین کانفیگ پایدار انتخاب شد (میانگین ${bestAvgDelay}ms).")
        return@withContext bestStableConfig
    }

    // --- توابع جدید برای تست و ترمیم گروهی ---

    suspend fun diagnoseBatch(
        context: Context,
        links: List<String>,
        onProgress: (String) -> Unit = {}
    ): List<Pair<String, DiagnosisResult?>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, DiagnosisResult?>>()
        for ((index, link) in links.withIndex()) {
            onProgress("تست ${index + 1} از ${links.size}...")
            val parsed = com.configdoctor.parser.ConfigParserFactory.parse(link)
            parsed.fold(
                onSuccess = { config ->
                    try {
                        val diag = diagnose(context, config)
                        results.add(link to diag)
                    } catch (e: Exception) {
                        results.add(link to null)
                    }
                },
                onFailure = {
                    results.add(link to null)
                }
            )
        }
        return@withContext results
    }

    suspend fun healBatch(
        context: Context,
        configs: List<VlessConfig>,
        onProgress: (String) -> Unit = {}
    ): List<VlessConfig?> = withContext(Dispatchers.IO) {
        val healed = mutableListOf<VlessConfig?>()
        for ((index, config) in configs.withIndex()) {
            onProgress("ترمیم ${index + 1} از ${configs.size}...")
            healed.add(healConfig(context, config, HealMode.QUICK, onProgress))
        }
        return@withContext healed
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DATACENTER -> "دیتاسنتر / VPS"
        ServerType.DIRECT -> "سرور مستقیم"
        ServerType.UNKNOWN -> "نامشخص"
    }
}

// توابع کمکی برای نمایش کشور و پرچم
fun formatCountry(code: String?): String {
    if (code.isNullOrBlank()) return "-"
    val name = when (code.uppercase()) {
        "US" -> "آمریکا"
        "DE" -> "آلمان"
        "NL" -> "هلند"
        "FR" -> "فرانسه"
        "GB" -> "انگلستان"
        "TR" -> "ترکیه"
        "AE" -> "امارات"
        "CA" -> "کانادا"
        "FI" -> "فنلاند"
        "SE" -> "سوئد"
        "RU" -> "روسیه"
        "IR" -> "ایران"
        "SG" -> "سنگاپور"
        "JP" -> "ژاپن"
        "IN" -> "هند"
        "PL" -> "لهستان"
        "AT" -> "اتریش"
        "CH" -> "سوئیس"
        "IT" -> "ایتالیا"
        "ES" -> "اسپانیا"
        else -> code.uppercase()
    }
    val flag = getFlagEmoji(code)
    return "$flag $name ($code)".trim()
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}