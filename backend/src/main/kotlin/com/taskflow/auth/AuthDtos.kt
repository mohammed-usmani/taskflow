package com.taskflow.auth

import com.taskflow.user.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/*
 * Request DTOs. Two Kotlin + validation details:
 *  - @field:  puts the annotation on the Java FIELD, where Bean Validation looks.
 *    Without it Kotlin attaches it to the constructor parameter and it is silently ignored.
 *  - Default "" : if a client omits "email" entirely, Jackson fills "" and @NotBlank
 *    reports a clean field error, instead of a non-null constructor failing with a
 *    vague "Malformed request body".
 */
data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String = "",

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255)
    val email: String = "",

    // BCrypt only uses the first 72 bytes of a password, so cap it there.
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    val password: String = "",
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    val email: String = "",
    @field:NotBlank(message = "Password is required")
    val password: String = "",
)

// Response DTOs: never return the User entity (it holds passwordHash).
data class UserResponse(val id: Long, val name: String, val email: String) {
    companion object {
        // Factory method: one place that maps entity -> DTO (a pattern to name in the README).
        fun from(user: User) = UserResponse(user.id!!, user.name, user.email)
    }
}

data class AuthResponse(val token: String, val user: UserResponse)
