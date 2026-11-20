package com.configdoctor.diagnose

import android.content.Context
import com.configdoctor.model.VlessConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object SniTester {
    // لیست SNIهای معروف که معمولاً توسط DPI بلاک نمی‌شن
    private val candidateSnies = listOf(
        "cdn.cloudflare.com",
        "www.microsoft.com",
        "www.bing.com",
        "www.apple.com",
        "www.amazon.com",
        "www.samsung.com",
        "cdn.jsdelivr.net",
        "www.speedtest.net"
    )

    suspend fun findBestSni(context: Context, config: VlessConfig): Pair<String, Long>? = withContext(Dispatchers.IO) {
        // اگه کانفیگ اصلاً SNI نداره، یکی از اینا رو تست کن
        val currentSni = config.sni ?: config.host ?: config.address
        val snisToTest = (candidateSnies + currentSni).distinct()

        return@withContext coroutineScope {
            snisToTest.map { sni ->
                async(Dispatchers.IO) {
                    val newConfig = config.copy(sni = sni)
                    val result = XrayRealDelayTester.test(context, newConfig, timeoutMs = 5000)
                    if (result.ok && result.delayMs != null) {
                        sni to result.delayMs
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull().minByOrNull { it.second }
        }
    }
}