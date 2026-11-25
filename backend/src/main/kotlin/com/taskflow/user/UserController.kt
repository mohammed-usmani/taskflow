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

    // Everyone who can be assigned a task (assignee dropdown). Assumption, in the README:
    // there are no teams in the brief, so any registered user is assignable. Only id, name
    // and email are returned, never password hashes.
    // ponytail: unpaged; add ?q= search + paging once there are more than a few hundred users.
    @GetMapping
    fun list(): List<UserResponse> = users.findAllByOrderByNameAsc().map(UserResponse::from)

    // Who am I? The frontend calls this on load to show the name and to check the token.
    // @AuthenticationPrincipal = the principal JwtAuthFilter put in the SecurityContext.
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userId: Long): UserResponse =
        users.findById(userId)
            .map(UserResponse::from)
            .orElseThrow { NotFoundException("User not found") }
}
