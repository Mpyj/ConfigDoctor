package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

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
    val summary: String,
    val packetLoss: PacketLossResult? = null
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

    // ─────────────────────────────
    // ۱) تشخیص وضعیت کانفیگ (تک)
    // ─────────────────────────────
    suspend fun diagnose(context: Context, config: VlessConfig): DiagnosisResult = withContext(Dispatchers.IO) {
        val (serverType, resolvedIp) = ServerTypeDetector.detect(config.address)
        val tcp = TcpTester.test(config.address, config.port)

        if (!tcp.ok) {
            return@withContext DiagnosisResult(
                config = config, serverType = serverType, resolvedIp = resolvedIp,
                tcpOk = false, tcpLatencyMs = null, realDelayMs = null, outputIp = null,
                outputCountry = null, tlsOk = null, tlsError = null,
                overallStatus = if (resolvedIp == null) OverallStatus.DNS_FAILED else OverallStatus.IP_DEAD,
                summary = "❌ سرور پاسخ نمی‌دهد.\nآی‌پی سرور احتمالاً سوخته یا مسیر اینترنت شما مشکل دارد.\nخطا: ${tcp.error}",
                packetLoss = null
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

        // تست پکت‌لاس فقط برای کانفیگ‌های سالم
        val packetLoss = if (realDelay.ok) {
            PacketLossTester.test(
                address = config.address,
                port = config.port,
                count = 5,
                intervalMs = 150,
                timeoutMs = 1500
            )
        } else null

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
                    if (packetLoss != null && packetLoss.ok) {
                        append("پکت‌لاس: ${"%.0f".format(packetLoss.lossPercent)}% (${packetLoss.received}/${packetLoss.sent})\n")
                    }
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
            tlsOk = tlsOk, tlsError = tlsError, overallStatus = status, summary = summary,
            packetLoss = packetLoss
        )
    }

    // ─────────────────────────────
    // ۲) ترمیم کانفیگ (چهار مرحله‌ای با موازی‌سازی)
    // ─────────────────────────────
    suspend fun healConfig(
        context: Context,
        config: VlessConfig,
        mode: HealMode = HealMode.DEEP,
        onProgress: (String) -> Unit = {}
    ): VlessConfig? = withContext(Dispatchers.IO) {
        val baseConfig = if (config.port <= 0) config.copy(port = 443) else config

        // ── مرحله ۱: تولید IPها و تست TCP (موازی با محدودیت ۱۰) ──
        onProgress("تولید IPهای جدید...")
        val randomIps = CloudflareCleanIps.generateRandomIps(100)

        val ipv6Candidates = randomIps.filter { it.contains(":") }
        val ipv4Candidates = randomIps.filter { it.contains(".") }
        val allCandidates = (ipv6Candidates + ipv4Candidates).distinct()

        onProgress("مرحله ۱: غربالگری ${allCandidates.size} IP (موازی)...")
        val tcpCounter = AtomicInteger(0)
        val tcpSemaphore = Semaphore(10)

        val tcpPassed = coroutineScope {
            allCandidates.map { ip ->
                async(Dispatchers.IO) {
                    tcpSemaphore.withPermit {
                        val done = tcpCounter.incrementAndGet()
                        if (done % 10 == 0) {
                            onProgress("مرحله ۱: تست $done از ${allCandidates.size}")
                        }
                        if (TcpTester.test(ip, baseConfig.port, timeoutMs = 1000).ok) ip else null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (tcpPassed.isEmpty()) {
            onProgress("هیچ IP سالمی در مرحله ۱ پیدا نشد.")
            return@withContext null
        }

        // حالت QUICK: اگه اولین IP سالم کافیه، همون رو برگردون
        if (mode == HealMode.QUICK) {
            onProgress("حالت سریع: اولین IP سالم پیدا شد.")
            return@withContext baseConfig.copy(address = tcpPassed.first())
        }

        // ── مرحله ۲: تست Real Delay (موازی با محدودیت ۵) ──
        val toTestRealDelay = tcpPassed.take(20)
        onProgress("مرحله ۲: تست Real Delay روی ${toTestRealDelay.size} IP (موازی)...")
        val rdCounter = AtomicInteger(0)
        val rdSemaphore = Semaphore(5)

        val realDelayPassed = coroutineScope {
            toTestRealDelay.map { ip ->
                async(Dispatchers.IO) {
                    rdSemaphore.withPermit {
                        val done = rdCounter.incrementAndGet()
                        onProgress("مرحله ۲: تست $done از ${toTestRealDelay.size} ($ip)")
                        val newConfig = baseConfig.copy(address = ip)
                        val result = XrayRealDelayTester.test(context, newConfig, timeoutMs = 5000)
                        if (result.ok && result.delayMs != null) {
                            HealCandidate(newConfig, result.delayMs)
                        } else null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (realDelayPassed.isEmpty()) {
            onProgress("هیچ تونل سالمی در مرحله ۲ پیدا نشد.")
            return@withContext null
        }

        // ── مرحله ۳: تست پایداری روی ۳ کاندیدای برتر (موازی) ──
        val topCandidates = realDelayPassed.sortedBy { it.realDelayMs }.take(3)
        onProgress("مرحله ۳: تست پایداری روی ${topCandidates.size} کاندیدای برتر (موازی)...")

        data class StabilityResult(val config: VlessConfig, val avgDelay: Long?)

        val stabilityResults = coroutineScope {
            topCandidates.mapIndexed { index, candidate ->
                async(Dispatchers.IO) {
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
                        StabilityResult(candidate.config, delays.average().toLong())
                    } else {
                        StabilityResult(candidate.config, null)
                    }
                }
            }.awaitAll()
        }

        val stableCandidates = stabilityResults.filter { it.avgDelay != null }
        if (stableCandidates.isEmpty()) {
            onProgress("هیچ کاندیدای پایداری پیدا نشد. استفاده از بهترین گزینه‌ی قبلی...")
            return@withContext realDelayPassed.minByOrNull { it.realDelayMs }?.config
        }

        val bestStableConfig = stableCandidates.minByOrNull { it.avgDelay!! }?.config
        val bestAvgDelay = stableCandidates.minByOrNull { it.avgDelay!! }?.avgDelay ?: Long.MAX_VALUE

        // ── مرحله ۴: تست پکت‌لاس روی ۳ کاندیدای برتر (موازی) ──
        onProgress("مرحله ۴: تست پکت‌لاس روی ${topCandidates.size} کاندیدای برتر (موازی)...")

        val packetLossPassed = coroutineScope {
            topCandidates.mapIndexed { index, candidate ->
                async(Dispatchers.IO) {
                    val pl = PacketLossTester.test(
                        address = candidate.config.address,
                        port = candidate.config.port,
                        count = 10,
                        intervalMs = 150,
                        timeoutMs = 2000
                    )
                    onProgress(
                        "مرحله ۴: ${candidate.config.address} → پکت‌لاس ${"%.0f".format(pl.lossPercent)}% " +
                                "(دریافت ${pl.received}/${pl.sent}" +
                                (if (pl.avgLatencyMs != null) "، میانگین ${pl.avgLatencyMs}ms" else "") + ")"
                    )
                    candidate.config to pl
                }
            }.awaitAll()
        }

        val bestByPacketLoss = packetLossPassed
            .filter { it.second.lossPercent <= 30f }
            .minWithOrNull(
                compareBy<Pair<VlessConfig, PacketLossResult>> { it.second.lossPercent }
                    .thenBy { it.second.avgLatencyMs ?: Long.MAX_VALUE }
            )

        if (bestByPacketLoss != null) {
            onProgress(
                "بهترین کانفیگ نهایی: ${bestByPacketLoss.first.address} " +
                        "(پکت‌لاس ${"%.0f".format(bestByPacketLoss.second.lossPercent)}%، " +
                        "میانگین ${bestByPacketLoss.second.avgLatencyMs}ms)"
            )
            return@withContext bestByPacketLoss.first
        }

        onProgress("هیچ کاندیدایی پکت‌لاس قابل قبول نداشت. استفاده از بهترین پایدار قبلی (میانگین ${bestAvgDelay}ms)...")
        return@withContext bestStableConfig
    }

    // ─────────────────────────────
    // ۳) تست گروهی (موازی با محدودیت ۵)
    // ─────────────────────────────
    suspend fun diagnoseBatch(
        context: Context,
        links: List<String>,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): List<Pair<String, DiagnosisResult?>> = withContext(Dispatchers.IO) {
        val total = links.size
        if (total == 0) return@withContext emptyList()

        val completed = AtomicInteger(0)
        val semaphore = Semaphore(5)  // حداکثر ۵ تست همزمان

        coroutineScope {
            links.mapIndexed { index, link ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val parsed = com.configdoctor.parser.ConfigParserFactory.parse(link)
                        val ipText = parsed.getOrNull()?.address ?: "ناشناس"
                        onProgress("در حال تست کانفیگ $index از $total (آی‌پی: $ipText)", index.toFloat() / total)

                        val result = parsed.fold(
                            onSuccess = { config ->
                                try {
                                    diagnose(context, config)
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            onFailure = { null }
                        )

                        val done = completed.incrementAndGet()
                        onProgress("تست شد: $done از $total", done.toFloat() / total)

                        link to result
                    }
                }
            }.awaitAll()
        }.also {
            onProgress("تست کامل شد", 1f)
        }
    }

    // ─────────────────────────────
    // ۴) ترمیم گروهی (موازی با محدودیت ۳)
    // ─────────────────────────────
    suspend fun healBatch(
        context: Context,
        configs: List<VlessConfig>,
        mode: HealMode = HealMode.QUICK,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): List<Pair<VlessConfig, VlessConfig?>> = withContext(Dispatchers.IO) {
        val total = configs.size
        if (total == 0) return@withContext emptyList()

        val completed = AtomicInteger(0)
        val semaphore = Semaphore(3)  // حداکثر ۳ ترمیم همزمان (heal سنگینه)

        coroutineScope {
            configs.mapIndexed { index, config ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        onProgress("در حال ترمیم کانفیگ ${index + 1} از $total (${config.address})", index.toFloat() / total)

                        val healed = healConfig(context, config, mode) { progressMsg ->
                            onProgress("ترمیم ${index + 1} از $total: $progressMsg", index.toFloat() / total)
                        }

                        val done = completed.incrementAndGet()
                        onProgress("ترمیم شد: $done از $total", done.toFloat() / total)

                        config to healed
                    }
                }
            }.awaitAll()
        }.also {
            onProgress("ترمیم کامل شد", 1f)
        }
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