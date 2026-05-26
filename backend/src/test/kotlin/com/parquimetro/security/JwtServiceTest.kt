package com.parquimetro.security

import io.jsonwebtoken.security.SignatureException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class JwtServiceTest {

    private val secret = Base64.getEncoder().encodeToString(ByteArray(64) { it.toByte() })
    private val service = JwtService(secret, expirationMs = 60_000)

    @Test
    fun `generate produz token com subject e roles`() {
        val token = service.generate("admin", listOf("ROLE_ADMIN"))
        val claims = service.validate(token)
        assertEquals("admin", claims.subject)
        @Suppress("UNCHECKED_CAST")
        val roles = claims["roles"] as List<String>
        assertTrue(roles.contains("ROLE_ADMIN"))
    }

    @Test
    fun `validate lanca excecao para token adulterado`() {
        val token = service.generate("admin", listOf("ROLE_ADMIN"))
        val partes = token.split(".")
        val tokenAdulterado = "${partes[0]}.${partes[1]}XXXXX.${partes[2]}"
        assertThrows<Exception> { service.validate(tokenAdulterado) }
    }

    @Test
    fun `validate lanca excecao para token expirado`() {
        val expirado = JwtService(secret, expirationMs = -1000)
        val token = expirado.generate("admin", listOf())
        assertThrows<Exception> { service.validate(token) }
    }
}
