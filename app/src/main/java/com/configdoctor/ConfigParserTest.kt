package com.configdoctor

import com.configdoctor.parser.ConfigParserFactory
import org.junit.Assert.*
import org.junit.Test

class ConfigParserTest {

    @Test
    fun testValidVlessConfig() {
        val link = "vless://uuid-1234-5678@104.16.0.1:443?security=tls&sni=example.com&type=ws&path=%2Ftest#MyConfig"
        val result = ConfigParserFactory.parse(link)
        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertNotNull(config)
        assertEquals("vless", config?.protocol)
        assertEquals("104.16.0.1", config?.address)
        assertEquals(443, config?.port)
        assertEquals("example.com", config?.sni)
    }

    @Test
    fun testInvalidVlessConfig() {
        val link = "vless://invalid-link-format"
        val result = ConfigParserFactory.parse(link)
        // پارسر باید به جای کرش، خطا برگردونه
        assertTrue(result.isFailure)
    }

    @Test
    fun testEmptyString() {
        val result = ConfigParserFactory.parse("")
        assertTrue(result.isFailure)
    }
}