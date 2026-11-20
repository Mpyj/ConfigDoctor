package com.configdoctor.diagnose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SpeedResult(
    val downloadMbps: Float?,
    val uploadMbps: Float?,
    val error: String? = null
)

object SpeedTester {
    suspend fun testSpeed(): SpeedResult = withContext(Dispatchers.IO) {
        var downloadMbps: Float? = null
        var uploadMbps: Float? = null
        var errorMsg: String? = null

        try {
            // ─── تست دانلود (۱ مگابایت) ───
            val downloadStart = System.currentTimeMillis()
            val downloadUrl = URL("https://speed.cloudflare.com/__down?bytes=1000000")
            val dlConn = downloadUrl.openConnection() as HttpURLConnection
            dlConn.connectTimeout = 10000
            dlConn.readTimeout = 15000
            
            if (dlConn.responseCode == 200) {
                val input = BufferedInputStream(dlConn.inputStream)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                }
                input.close()
                val timeMs = System.currentTimeMillis() - downloadStart
                if (timeMs > 0) {
                    downloadMbps = (totalBytes * 8f / 1000000f) / (timeMs / 1000f)
                }
            }
            dlConn.disconnect()

            // ─── تست آپلود (۱ مگابایت) ───
            val uploadStart = System.currentTimeMillis()
            val uploadUrl = URL("https://speed.cloudflare.com/__up")
            val ulConn = uploadUrl.openConnection() as HttpURLConnection
            ulConn.requestMethod = "POST"
            ulConn.doOutput = true
            ulConn.connectTimeout = 10000
            ulConn.readTimeout = 15000
            ulConn.setFixedLengthStreamingMode(1000000)
            
            val uploadData = ByteArray(1000000)
            val output: OutputStream = ulConn.outputStream
            output.write(uploadData)
            output.flush()
            output.close()
            
            if (ulConn.responseCode in 200..299) {
                val timeMs = System.currentTimeMillis() - uploadStart
                if (timeMs > 0) {
                    uploadMbps = (1000000L * 8f / 1000000f) / (timeMs / 1000f)
                }
            }
            ulConn.disconnect()
            
        } catch (e: Exception) {
            errorMsg = e.message
        }

        return@withContext SpeedResult(downloadMbps, uploadMbps, errorMsg)
    }
}