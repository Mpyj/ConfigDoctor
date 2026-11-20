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
    private val commonPorts = listOf(443, 8443, 2053, 2087, 2096, 8080)

    suspend fun findBestPort(address: String, originalPort: Int): Int = withContext(Dispatchers.IO) {
        val portsToTest = (commonPorts + originalPort).distinct()

        val results = coroutineScope {
            portsToTest.map { port ->
                async(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    val result = TcpTester.test(address, port, timeoutMs = 3000)
                    val latency = if (result.ok) System.currentTimeMillis() - start else null
                    PortTestResult(port, latency, result.ok)
                }
            }.awaitAll()
        }

        val openPorts = results.filter { it.isOpen && it.latencyMs != null }
        if (openPorts.isEmpty()) return@withContext originalPort

        // انتخاب پورت با کمترین تأخیر
        return@withContext openPorts.minByOrNull { it.latencyMs!! }?.port ?: originalPort
    }
}