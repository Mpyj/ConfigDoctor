package com.configdoctor.diagnose

import kotlin.random.Random

object CloudflareCleanIps {

    // رنج‌های رسمی IPv4 و IPv6 کلادفلر (طبق مستندات خودشون)
    private val cloudflareIpv4Ranges = listOf(
        "104.16.0.0/13",    // حدود ۵۰۰ هزار IP
        "172.64.0.0/13",    // حدود ۵۰۰ هزار IP
        "104.24.0.0/14",
        "162.158.0.0/15",
        "198.41.128.0/17",
        "188.114.96.0/20",
        "190.93.240.0/20"
    )

    private val cloudflareIpv6Ranges = listOf(
        "2606:4700::/32",
        "2a06:98c0::/29"
    )

    /**
     * تولید یک لیست تصادفی از IPهای کلادفلر برای تست.
     * هر بار که این تابع صدا زده می‌شه، IPهای جدیدی تولید می‌شه.
     */
    fun generateRandomIps(count: Int = 100): List<String> {
        val ips = mutableListOf<String>()
        
        // ۷۰٪ IPv4 و ۳۰٪ IPv6 (چون IPv6 برات بهتره، شاید بخوای نسبت رو عوض کنی)
        val ipv4Count = (count * 0.7).toInt()
        val ipv6Count = count - ipv4Count

        repeat(ipv4Count) {
            ips.add(generateRandomIpv4())
        }
        repeat(ipv6Count) {
            ips.add(generateRandomIpv6())
        }
        
        return ips.shuffled() // ترتیب رو به هم می‌زنیم
    }

    private fun generateRandomIpv4(): String {
        // یه رنج تصادفی انتخاب کن
        val range = cloudflareIpv4Ranges.random()
        val (baseIp, prefix) = range.split("/")
        val parts = baseIp.split(".").map { it.toInt() }.toIntArray()
        
        // محاسبه‌ی تعداد بیت‌های Host
        val hostBits = 32 - prefix.toInt()
        val maxHost = (1 shl hostBits) - 1 // مثلاً برای /13 می‌شه 524287
        
        // یه عدد تصادفی برای Host تولید کن
        val randomHost = Random.nextInt(1, maxHost) // 0 رو رد می‌کنیم
        
        // تبدیل به IP
        val baseInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val finalInt = baseInt + randomHost
        
        return "${(finalInt shr 24) and 0xFF}.${(finalInt shr 16) and 0xFF}.${(finalInt shr 8) and 0xFF}.${finalInt and 0xFF}"
    }

    private fun generateRandomIpv6(): String {
        // برای سادگی، از یه رنج ثابت IPv6 یه IP تصادفی می‌سازیم
        val range = cloudflareIpv6Ranges.random()
        // این یه پیاده‌سازی ساده‌ست. برای دقت بیشتر باید بیت‌ها رو دستکاری کنیم.
        // فعلاً از یه الگوی ساده استفاده می‌کنیم.
        val randomPart = Random.nextInt(1, 65535).toString(16)
        return if (range.startsWith("2606:4700")) {
            "2606:4700:${randomPart}::1"
        } else {
            "2a06:98c1:${randomPart}::1"
        }
    }
}