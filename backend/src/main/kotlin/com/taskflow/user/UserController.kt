package com.taskflow.user

import com.taskflow.auth.UserResponse
import com.taskflow.common.NotFoundException
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val users: UserRepository) {

    // Who am I? The frontend calls this on load to show the name and to check the token.
    // @AuthenticationPrincipal = the principal JwtAuthFilter put in the SecurityContext.
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: Long): UserResponse =
        users.findById(userId)
            .map(UserResponse::from)
            .orElseThrow { NotFoundException("User not found") }
}
