package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.data.SettingsManager
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

enum class OverallStatus {
    HEALTHY, IP_DEAD, SNI_FILTERED, TLS_ERROR, DNS_FAILED, UNKNOWN
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
    val packetLoss: PacketLossResult? = null,
    val mtu: Int? = null
)

private data class HealCandidate(
    val config: VlessConfig,
    val realDelayMs: Long
)

object Diagnostician {

    enum class HealMode { QUICK, DEEP }

    suspend fun diagnose(context: Context, config: VlessConfig): DiagnosisResult = withContext(Dispatchers.IO) {
        try {
            val (serverType, resolvedIp) = ServerTypeDetector.detect(config.address)
            val tcp = TcpTester.test(config.address, config.port)

            if (!tcp.ok) {
                return@withContext DiagnosisResult(
                    config = config, serverType = serverType, resolvedIp = resolvedIp,
                    tcpOk = false, tcpLatencyMs = null, realDelayMs = null, outputIp = null,
                    outputCountry = null, tlsOk = null, tlsError = null,
                    overallStatus = if (resolvedIp == null) OverallStatus.DNS_FAILED else OverallStatus.IP_DEAD,
                    summary = "❌ سرور پاسخ نمی‌دهد.\nخطا: ${tcp.error}",
                    packetLoss = null, mtu = null
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

            val packetLoss = if (realDelay.ok) {
                PacketLossTester.test(config.address, config.port, count = 5, intervalMs = 150, timeoutMs = 1500)
            } else null
            
            // تست MTU فقط برای سرورهای شخصی (غیر کلودفلر)
            val mtu = if (serverType != ServerType.CLOUDFLARE) {
                MtuTester.test(config.address, config.port)
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
                            append("پکت‌لاس: ${"%.0f".format(packetLoss.lossPercent)}%\n")
                        }
                        if (mtu != null) append("MTU بهینه: $mtu\n")
                        if (!realDelay.ip.isNullOrBlank()) append("IP خروجی: ${realDelay.ip}\n")
                        if (!realDelay.country.isNullOrBlank()) append("کشور خروجی: ${formatCountry(realDelay.country)}\n")
                    }
                }
                tlsOk == false -> {
                    status = OverallStatus.SNI_FILTERED
                    summary = "⚠️ TCP وصل شد ولی TLS شکست خورد.\nخطای TLS: $tlsError"
                }
                else -> {
                    status = OverallStatus.UNKNOWN
                    summary = "⚠️ وضعیت نامشخص.\nخطا: ${realDelay.error}"
                }
            }

            return@withContext DiagnosisResult(
                config = config, serverType = serverType, resolvedIp = resolvedIp,
                tcpOk = true, tcpLatencyMs = tcp.latencyMs, realDelayMs = realDelay.delayMs,
                outputIp = realDelay.ip, outputCountry = realDelay.country,
                tlsOk = tlsOk, tlsError = tlsError, overallStatus = status, summary = summary,
                packetLoss = packetLoss, mtu = mtu
            )
        } catch (e: Exception) {
            return@withContext DiagnosisResult(
                config = config, serverType = ServerType.UNKNOWN, resolvedIp = null,
                tcpOk = false, tcpLatencyMs = null, realDelayMs = null, outputIp = null,
                outputCountry = null, tlsOk = null, tlsError = null,
                overallStatus = OverallStatus.UNKNOWN,
                summary = "خطای سیستمی: ${e.message}",
                packetLoss = null, mtu = null
            )
        }
    }

    suspend fun healConfig(
        context: Context,
        config: VlessConfig,
        mode: HealMode = HealMode.DEEP,
        onProgress: (String) -> Unit = {}
    ): VlessConfig? = withContext(Dispatchers.IO) {
        try {
            val baseConfig = if (config.port <= 0) config.copy(port = 443) else config

            onProgress("تشخیص نوع سرور...")
            val (serverType, _) = ServerTypeDetector.detect(baseConfig.address)
            
            if (serverType == ServerType.CLOUDFLARE) {
                onProgress("کلودفلر تشخیص داده شد. شروع ترمیم با تغییر IP...")
                return@withContext healCloudflare(context, baseConfig, mode, onProgress)
            } else {
                onProgress("سرور شخصی تشخیص داده شد. تغییر IP فایده‌ای ندارد.")
                // ۱. پیدا کردن بهترین پورت
                onProgress("اسکن پورت‌های جایگزین...")
                val bestPort = PortScanner.findBestPort(baseConfig.address, baseConfig.port)
                if (bestPort != baseConfig.port) {
                    onProgress("پورت بهتر پیدا شد: $bestPort (قبلی: ${baseConfig.port})")
                } else {
                    onProgress("پورت فعلی بهترین گزینه است.")
                }
                
                // ۲. پیدا کردن MTU
                onProgress("تست MTU...")
                val mtu = MtuTester.test(baseConfig.address, bestPort)
                if (mtu != null) onProgress("MTU بهینه: $mtu")
                
                return@withContext baseConfig.copy(port = bestPort)
            }
        } catch (e: Exception) {
            onProgress("خطا در حین ترمیم: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun healCloudflare(
        context: Context,
        baseConfig: VlessConfig,
        mode: HealMode,
        onProgress: (String) -> Unit
    ): VlessConfig? = coroutineScope {
        onProgress("تولید IPهای جدید...")
        val randomIps = CloudflareCleanIps.generateRandomIpsFromStorage(context, 100)
        val allCandidates = randomIps.distinct()

        onProgress("مرحله ۱: غربالگری ${allCandidates.size} IP (موازی)...")
        val tcpCounter = AtomicInteger(0)
        val tcpSemaphore = Semaphore(10)

        val tcpPassed = allCandidates.map { ip ->
            async(Dispatchers.IO) {
                tcpSemaphore.withPermit {
                    val done = tcpCounter.incrementAndGet()
                    if (done % 10 == 0) onProgress("مرحله ۱: تست $done از ${allCandidates.size}")
                    if (TcpTester.test(ip, baseConfig.port, timeoutMs = 1000).ok) ip else null
                }
            }
        }.awaitAll().filterNotNull()

        if (tcpPassed.isEmpty()) {
            onProgress("هیچ IP سالمی پیدا نشد.")
            return@coroutineScope null
        }
        if (mode == HealMode.QUICK) {
            onProgress("حالت سریع: اولین IP سالم پیدا شد.")
            return@coroutineScope baseConfig.copy(address = tcpPassed.first())
        }

        val toTestRealDelay = tcpPassed.take(20)
        onProgress("مرحله ۲: تست Real Delay روی ${toTestRealDelay.size} IP...")
        val rdCounter = AtomicInteger(0)
        val rdSemaphore = Semaphore(5)

        val realDelayPassed = toTestRealDelay.map { ip ->
            async(Dispatchers.IO) {
                rdSemaphore.withPermit {
                    val done = rdCounter.incrementAndGet()
                    onProgress("مرحله ۲: تست $done از ${toTestRealDelay.size} ($ip)")
                    val newConfig = baseConfig.copy(address = ip)
                    val result = withTimeoutOrNull(7000) { XrayRealDelayTester.test(context, newConfig, timeoutMs = 5000) }
                    if (result != null && result.ok && result.delayMs != null) HealCandidate(newConfig, result.delayMs) else null
                }
            }
        }.awaitAll().filterNotNull()

        if (realDelayPassed.isEmpty()) {
            onProgress("هیچ تونل سالمی پیدا نشد.")
            return@coroutineScope null
        }

        val topCandidates = realDelayPassed.sortedBy { it.realDelayMs }.take(3)
        onProgress("مرحله ۳: تست پایداری...")
        
        data class StabilityResult(val config: VlessConfig, val avgDelay: Long?)
        val stabilityResults = topCandidates.mapIndexed { index, candidate ->
            async(Dispatchers.IO) {
                onProgress("مرحله ۳: پایداری ${index + 1} از ${topCandidates.size}")
                val delays = mutableListOf<Long>()
                var successCount = 0
                repeat(3) {
                    val result = withTimeoutOrNull(7000) { XrayRealDelayTester.test(context, candidate.config, timeoutMs = 5000) }
                    if (result != null && result.ok && result.delayMs != null) { delays.add(result.delayMs); successCount++ }
                }
                if (successCount >= 2) StabilityResult(candidate.config, delays.average().toLong()) else StabilityResult(candidate.config, null)
            }
        }.awaitAll()

        val stableCandidates = stabilityResults.filter { it.avgDelay != null }
        if (stableCandidates.isEmpty()) {
            onProgress("هیچ کاندیدای پایداری پیدا نشد.")
            return@coroutineScope realDelayPassed.minByOrNull { it.realDelayMs }?.config
        }

        val bestStableConfig = stableCandidates.minByOrNull { it.avgDelay!! }?.config
        val bestAvgDelay = stableCandidates.minByOrNull { it.avgDelay!! }?.avgDelay ?: Long.MAX_VALUE

        onProgress("مرحله ۴: تست پکت‌لاس...")
        val packetLossPassed = topCandidates.mapIndexed { index, candidate ->
            async(Dispatchers.IO) {
                val pl = PacketLossTester.test(candidate.config.address, candidate.config.port, count = 10, intervalMs = 150, timeoutMs = 2000)
                onProgress("مرحله ۴: ${candidate.config.address} → ${"%.0f".format(pl.lossPercent)}%")
                candidate.config to pl
            }
        }.awaitAll()

        val bestByPacketLoss = packetLossPassed.filter { it.second.lossPercent <= 30f }
            .minWithOrNull(compareBy<Pair<VlessConfig, PacketLossResult>> { it.second.lossPercent }.thenBy { it.second.avgLatencyMs ?: Long.MAX_VALUE })

        val finalConfig = bestByPacketLoss?.first ?: bestStableConfig
        
        finalConfig?.let { SettingsManager.addCachedWorkingIp(context, it.address) }
        
        if (bestByPacketLoss != null) {
            onProgress("بهترین کانفیگ: ${bestByPacketLoss.first.address} (پکت‌لاس ${"%.0f".format(bestByPacketLoss.second.lossPercent)}%)")
        } else {
            onProgress("استفاده از پایدارترین کانفیگ (میانگین ${bestAvgDelay}ms)")
        }

        return@coroutineScope finalConfig
    }

    suspend fun diagnoseBatch(
        context: Context,
        links: List<String>,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): List<Pair<String, DiagnosisResult?>> = withContext(Dispatchers.IO) {
        val total = links.size
        if (total == 0) return@withContext emptyList()
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(5)
        coroutineScope {
            links.mapIndexed { index, link ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val parsed = com.configdoctor.parser.ConfigParserFactory.parse(link)
                        val ipText = parsed.getOrNull()?.address ?: "ناشناس"
                        onProgress("در حال تست کانفیگ $index از $total (آی‌پی: $ipText)", index.toFloat() / total)
                        val result = parsed.fold(onSuccess = { config -> try { diagnose(context, config) } catch (e: Exception) { null } }, onFailure = { null })
                        val done = completed.incrementAndGet()
                        onProgress("تست شد: $done از $total", done.toFloat() / total)
                        link to result
                    }
                }
            }.awaitAll()
        }.also { onProgress("تست کامل شد", 1f) }
    }

    suspend fun healBatch(
        context: Context,
        configs: List<VlessConfig>,
        mode: HealMode = HealMode.QUICK,
        onProgress: (String, Float) -> Unit = { _, _ -> }
    ): List<Pair<VlessConfig, VlessConfig?>> = withContext(Dispatchers.IO) {
        val total = configs.size
        if (total == 0) return@withContext emptyList()
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(3)
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
        }.also { onProgress("ترمیم کامل شد", 1f) }
    }

    private fun serverTypeLabel(type: ServerType): String = when (type) {
        ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
        ServerType.DATACENTER -> "دیتاسنتر / VPS"
        ServerType.DIRECT -> "سرور مستقیم"
        ServerType.UNKNOWN -> "نامشخص"
    }
}

fun formatCountry(code: String?): String {
    if (code.isNullOrBlank()) return "-"
    val name = when (code.uppercase()) {
        "US" -> "آمریکا"; "DE" -> "آلمان"; "NL" -> "هلند"; "FR" -> "فرانسه"; "GB" -> "انگلستان"
        "TR" -> "ترکیه"; "AE" -> "امارات"; "CA" -> "کانادا"; "FI" -> "فنلاند"; "SE" -> "سوئد"
        "RU" -> "روسیه"; "IR" -> "ایران"; "SG" -> "سنگاپور"; "JP" -> "ژاپن"; "IN" -> "هند"
        "PL" -> "لهستان"; "AT" -> "اتریش"; "CH" -> "سوئیس"; "IT" -> "ایتالیا"; "ES" -> "اسپانیا"
        else -> code.uppercase()
    }
    return "${getFlagEmoji(code)} $name ($code)".trim()
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}