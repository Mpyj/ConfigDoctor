package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PortScanner {
    // پورت‌های رایجی که معمولاً بلاک نمی‌شن
    private val commonPorts = listOf(443, 8443, 2053, 2087, 2096, 8080)

    suspend fun findBestPort(address: String, originalPort: Int): Int = withContext(Dispatchers.IO) {
        // اگر پورت اصلی جواب داد، همون رو نگه دار
        if (TcpTester.test(address, originalPort, timeoutMs = 1000).ok) {
            return@withContext originalPort
        }

        // اگر پورت اصلی بسته بود، پورت‌های دیگه رو تست کن
        for (port in commonPorts) {
            if (port == originalPort) continue
            if (TcpTester.test(address, port, timeoutMs = 1000).ok) {
                return@withContext port
            }
        }
        
        return@withContext originalPort // اگر هیچی باز نبود، همون اصلی رو برگردون
    }
}