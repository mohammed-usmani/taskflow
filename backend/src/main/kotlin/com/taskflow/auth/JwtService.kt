package com.taskflow.auth

import com.taskflow.config.JwtProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService (private val props: JwtProperties) {
    private val key: SecretKey = Keys.hmacShaKeyFor(props.secret.toByteArray())

    fun generateToken(userId: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus
                (Duration.ofMinutes(props.expirationMinutes))
            ))
            .signWith(key)
            .compact()
    }

    fun parseUserId(token: String): Long? =
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
                .toLong()
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}