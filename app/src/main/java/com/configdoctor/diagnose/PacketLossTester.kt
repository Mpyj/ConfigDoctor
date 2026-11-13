package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class PacketLossResult(
    val ok: Boolean,
    val sent: Int,
    val received: Int,
    val lost: Int,
    val lossPercent: Float,
    val avgLatencyMs: Long?,
    val minLatencyMs: Long?,
    val maxLatencyMs: Long?,
    val jitterMs: Long?,
    val error: String? = null
)

object PacketLossTester {

    suspend fun test(
        address: String,
        port: Int,
        count: Int = 10,
        intervalMs: Long = 150,
        timeoutMs: Int = 2000
    ): PacketLossResult = withContext(Dispatchers.IO) {

        val latencies = mutableListOf<Long>()
        var failures = 0

        repeat(count) { i ->
            val result = TcpTester.test(address, port, timeoutMs)
            if (result.ok && result.latencyMs != null) {
                latencies.add(result.latencyMs)
            } else {
                failures++
            }
            if (i < count - 1) delay(intervalMs)
        }

        val received = latencies.size
        val lost = failures
        val lossPercent = (lost.toFloat() / count.toFloat()) * 100f

        val avg = if (latencies.isNotEmpty()) latencies.average().toLong() else null
        val min = latencies.minOrNull()
        val max = latencies.maxOrNull()
        val jitter = if (min != null && max != null) max - min else null

        PacketLossResult(
            ok = received > 0,
            sent = count,
            received = received,
            lost = lost,
            lossPercent = lossPercent,
            avgLatencyMs = avg,
            minLatencyMs = min,
            maxLatencyMs = max,
            jitterMs = jitter,
            error = if (received == 0) "هیچ پاسخی دریافت نشد" else null
        )
    }
}