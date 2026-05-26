package com.parquimetro.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HmacUtilsTest {

    private val secret = "test-secret-key"
    private val payload = """{"deviceId":"ESP32-VAG-001","status":"OCUPADA","battery":87}"""

    @Test
    fun `compute retorna hash hex de 64 chars`() {
        val hash = HmacUtils.compute(payload, secret)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `verify retorna true para assinatura correta`() {
        val hash = HmacUtils.compute(payload, secret)
        assertTrue(HmacUtils.verify(payload, secret, hash))
    }

    @Test
    fun `verify retorna false para payload adulterado`() {
        val hash = HmacUtils.compute(payload, secret)
        val adulterado = payload.replace("OCUPADA", "LIVRE")
        assertFalse(HmacUtils.verify(adulterado, secret, hash))
    }

    @Test
    fun `verify retorna false para secret errado`() {
        val hash = HmacUtils.compute(payload, secret)
        assertFalse(HmacUtils.verify(payload, "wrong-secret", hash))
    }

    @Test
    fun `verify e case-insensitive no hash`() {
        val hash = HmacUtils.compute(payload, secret).uppercase()
        assertTrue(HmacUtils.verify(payload, secret, hash))
    }
}
