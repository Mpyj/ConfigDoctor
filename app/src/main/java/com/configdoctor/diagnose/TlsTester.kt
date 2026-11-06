package com.configdoctor.diagnose

import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

data class TlsResult(
    val ok: Boolean,
    val error: String?
)

object TlsTester {

    fun test(
        address: String,
        port: Int,
        sni: String?,
        timeoutMs: Int = 7000
    ): TlsResult {
        if (sni.isNullOrBlank()) {
            return TlsResult(ok = false, error = "SNI مشخص نشده")
        }
        return try {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, null, null)
            val factory = ctx.socketFactory

            val plainSocket = Socket()
            plainSocket.connect(InetSocketAddress(address, port), timeoutMs)
            plainSocket.soTimeout = timeoutMs

            val sslSocket = factory.createSocket(
                plainSocket, sni, port, true
            ) as SSLSocket
            sslSocket.sslParameters = sslSocket.sslParameters.apply {
                serverNames = listOf(javax.net.ssl.SNIHostName(sni))
            }

            sslSocket.startHandshake()
            val ok = sslSocket.session.isValid
            sslSocket.close()
            TlsResult(ok = ok, error = if (ok) null else "TLS session نامعتبر")
        } catch (e: Exception) {
            TlsResult(ok = false, error = e.message ?: e.javaClass.simpleName)
        }
    }
}