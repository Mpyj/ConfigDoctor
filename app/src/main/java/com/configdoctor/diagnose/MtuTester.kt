package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object MtuTester {
    suspend fun test(address: String, port: Int): Int? = withContext(Dispatchers.IO) {
        // تست MTU با استفاده از Ping (فقط تو اندروید کار می‌کنه)
        val sizes = listOf(1500, 1450, 1400, 1350, 1300, 1280, 1200)
        
        for (size in sizes) {
            try {
                // -M do یعنی fragment نکن، -s سایز پکت، -c 1 یعنی یه بار
                val process = ProcessBuilder("ping", "-M", "do", "-s", "${size - 28}", "-c", "1", address)
                    .redirectErrorStream(true)
                    .start()
                
                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                process.waitFor()
                
                if (output.contains("1 received") || output.contains("1 packets received")) {
                    return@withContext size
                }
            } catch (e: Exception) {
                // ادامه بده
            }
        }
        return@withContext null
    }
}