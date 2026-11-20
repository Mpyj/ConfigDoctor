package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class PortTestResult(
    val port: Int,
    val latencyMs: Long?,
    val isOpen: Boolean
)

object PortScanner {
    // لیست پورت‌های پیشنهادی که معمولاً باز هستند
    private val commonPorts = listOf(443, 8443, 2053, 2087, 2096, 8080)

    suspend fun findBestPort(address: String, originalPort: Int): Int = withContext(Dispatchers.IO) {
        // تست همه پورت‌ها (به همراه پورت اصلی) به صورت موازی
        val portsToTest = (commonPorts + originalPort).distinct()

        val results = coroutineScope {
            portsToTest.map { port ->
                async(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    val result = TcpTester.test(address, port, timeoutMs = 2000)
                    val latency = if (result.ok) System.currentTimeMillis() - start else null
                    PortTestResult(port, latency, result.ok)
                }
            }.awaitAll()
        }

        // فیلتر پورت‌های باز، سپس مرتب‌سازی بر اساس کمترین تأخیر
        val openPorts = results.filter { it.isOpen && it.latencyMs != null }
        
        // اگر هیچ پورتی باز نبود، همون پورت اصلی رو برگردون
        if (openPorts.isEmpty()) return@withContext originalPort

        // انتخاب پورت با کمترین تأخیر
        val bestPort = openPorts.minByOrNull { it.latencyMs!! }?.port ?: originalPort
        
        return@withContext bestPort
    }
}