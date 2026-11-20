package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object MtuTester {
    suspend fun test(address: String, port: Int): Int? = withContext(Dispatchers.IO) {
        // تست سایزهای رایج MTU از بزرگ به کوچک
        val testSizes = listOf(1500, 1450, 1400, 1350, 1300, 1280, 1200)
        
        for (mtu in testSizes) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(address, port), 2000)
                    // ارسال پکت با سایز مشخص (منهای هدرهای IP و TCP)
                    socket.sendBufferSize = mtu - 40 
                    socket.getOutputStream().write(ByteArray(mtu - 40))
                    socket.getOutputStream().flush()
                    return@withContext mtu // اگر پکت رد شد، این بهترین MTU است
                }
            } catch (e: Exception) {
                continue // اگر ارور داد، یعنی سایز بزرگه، برو سایز بعدی
            }
        }
        return@withContext null
    }
}