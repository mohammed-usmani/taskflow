package com.taskflow.auth

import com.taskflow.config.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtServiceTest {
    private val secret = "test-secret-test-secret-test-secret-123456"
    private val service = JwtService(JwtProperties(secret = secret, expirationMinutes = 60))

    @Test
    fun `valid token round-trips to the user id`() {
        assertEquals(42L, service.parseUserId(service.generateToken(42L)))
    }

    @Test
    fun `token expires after the configured minutes`() {
        val exp = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.toByteArray())).build()
            .parseSignedClaims(service.generateToken(1L)).payload.expiration.toInstant()
        val minutesLeft = Duration.between(Instant.now(), exp).toMinutes()
        assertTrue(minutesLeft in 58..60, "expected ~60 minutes, got $minutesLeft")
    }

    @Test
    fun `tampered or garbage tokens are rejected`() {
        val token = service.generateToken(42L)
        assertNull(service.parseUserId(token.dropLast(2) + "xx"))   // broken signature
        assertNull(service.parseUserId("not-a-jwt"))
        assertNull(service.parseUserId(""))
    }
}