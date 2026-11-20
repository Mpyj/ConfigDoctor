package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DnsResult(
    val name: String,
    val ip: String,
    val latencyMs: Long,
    val addressToEnter: String // ← جدید: آدرسی که کاربر باید تو Private DNS وارد کنه
)

object DnsTester {
    private val dnsServers = mapOf(
        "Shecan" to Pair("178.22.122.100", "free.shecan.ir"),
        "Radar" to Pair("10.202.10.10", "dns.radar.game"),
        "Cloudflare" to Pair("1.1.1.1", "1dot1dot1dot1.cloudflare-dns.com"),
        "Google" to Pair("8.8.8.8", "dns.google"),
        "Quad9" to Pair("9.9.9.9", "dns.quad9.net")
    )

    suspend fun testAllDns(): List<DnsResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DnsResult>()
        for ((name, pair) in dnsServers) {
            val (ip, host) = pair
            val latency = pingDns(ip)
            if (latency > 0) {
                results.add(DnsResult(name, ip, latency, host))
            }
        }
        return@withContext results.sortedBy { it.latencyMs }
    }

    private fun pingDns(dnsIp: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            
            val query = byteArrayOf(
                0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x06, 0x67, 0x6F, 0x6F, 0x67, 0x6C, 0x65, 0x03, 0x63, 0x6F, 0x6D, 0x00,
                0x00, 0x01, 0x00, 0x01
            )
            
            val address = InetAddress.getByName(dnsIp)
            val packet = DatagramPacket(query, query.size, address, 53)
            socket.send(packet)
            
            val response = ByteArray(512)
            val receivePacket = DatagramPacket(response, response.size)
            socket.receive(receivePacket)
            socket.close()
            
            val end = System.currentTimeMillis()
            end - start
        } catch (e: Exception) {
            0L
        }
    }
}