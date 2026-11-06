package com.configdoctor.diagnose

import java.net.InetSocketAddress
import java.net.Socket

data class TcpResult(
    val ok: Boolean,
    val latencyMs: Long?,
    val error: String?
)

object TcpTester {

    fun test(address: String, port: Int, timeoutMs: Int = 5000): TcpResult {
        val start = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(address, port), timeoutMs)
                val latency = System.currentTimeMillis() - start
                TcpResult(ok = true, latencyMs = latency, error = null)
            }
        } catch (e: Exception) {
            TcpResult(ok = false, latencyMs = null, error = e.message ?: e.javaClass.simpleName)
        }
    }
}