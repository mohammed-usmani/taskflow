package com.taskflow.auth

import com.taskflow.common.ConflictException
import com.taskflow.common.UnauthorizedException
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        val email = normalise(req.email)
        if (users.existsByEmailIgnoreCase(email)) {
            throw ConflictException("Email is already registered")
        }
        // The unique index on lower(email) still guards the race where two requests
        // pass this check at once; GlobalExceptionHandler maps that to 409 as well.
        val user = users.save(User(req.name.trim(), email, passwordEncoder.encode(req.password)))
        return AuthResponse(jwtService.generateToken(user.id!!), UserResponse.from(user))
    }

    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val user = users.findByEmailIgnoreCase(normalise(req.email))
        // Same message whether the email is unknown or the password is wrong, so the
        // endpoint can't be used to discover which emails have accounts.
        if (user == null || !passwordEncoder.matches(req.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }
        return AuthResponse(jwtService.generateToken(user.id!!), UserResponse.from(user))
    }

    private fun normalise(email: String) = email.trim().lowercase()
}
