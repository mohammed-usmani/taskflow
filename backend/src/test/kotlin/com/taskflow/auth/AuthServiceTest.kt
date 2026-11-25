package com.taskflow.auth

import com.taskflow.common.ConflictException
import com.taskflow.common.UnauthorizedException
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals

/**
 * Unit tests: no Spring, no database. MockK fakes the dependencies so each test
 * checks only AuthService's own rules.
 */
class AuthServiceTest {
    private val users = mockk<UserRepository>()
    private val encoder = mockk<PasswordEncoder>()
    private val jwt = mockk<JwtService>()
    private val service = AuthService(users, encoder, jwt)

    // An entity's id is set by the database; in a unit test we set it via reflection.
    private fun savedUser(email: String, hash: String) = User("Ali", email, hash).also {
        User::class.java.getDeclaredField("id").apply { isAccessible = true }.set(it, 7L)
    }

    @Test
    fun `register normalises email, hashes password and returns a token`() {
        val saved = slot<User>()
        every { users.existsByEmailIgnoreCase("ali@example.com") } returns false
        every { encoder.encode("password123") } returns "HASH"
        every { users.save(capture(saved)) } answers { savedUser(saved.captured.email, saved.captured.passwordHash) }
        every { jwt.generateToken(7L) } returns "TOKEN"

        val res = service.register(RegisterRequest(" Ali ", "  Ali@Example.COM ", "password123"))

        assertEquals("ali@example.com", saved.captured.email)
        assertEquals("HASH", saved.captured.passwordHash)   // never the raw password
        assertEquals("Ali", saved.captured.name)
        assertEquals("TOKEN", res.token)
    }

    @Test
    fun `register rejects an existing email without saving`() {
        every { users.existsByEmailIgnoreCase("ali@example.com") } returns true

        assertThrows<ConflictException> { service.register(RegisterRequest("Ali", "ali@example.com", "password123")) }
        verify(exactly = 0) { users.save(any()) }
    }

    @Test
    fun `login fails the same way for unknown email and wrong password`() {
        every { users.findByEmailIgnoreCase("nobody@example.com") } returns null
        every { users.findByEmailIgnoreCase("ali@example.com") } returns savedUser("ali@example.com", "HASH")
        every { encoder.matches("wrong", "HASH") } returns false

        val unknown = assertThrows<UnauthorizedException> { service.login(LoginRequest("nobody@example.com", "x")) }
        val wrongPw = assertThrows<UnauthorizedException> { service.login(LoginRequest("ali@example.com", "wrong")) }
        assertEquals(unknown.message, wrongPw.message)
    }
}
