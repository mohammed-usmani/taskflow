package com.taskflow.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs once for every request, before Spring's authorization check.
 *
 *   "Authorization: Bearer <jwt>"  --valid-->  SecurityContext holds userId  --> controller
 *   missing / invalid / expired    -------->  context stays empty --> authorization rule
 *                                               rejects protected URLs with 401
 *
 * It never sends a response itself: it only *identifies* the caller. Deciding
 * what the caller may access stays in SecurityConfig's rules.
 *
 * Not a @Component on purpose: Spring Boot auto-registers every Filter bean with the
 * servlet container, so it would run twice (once there, once in the security chain).
 * SecurityConfig creates it and adds it to the chain only.
 */
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val userId = jwtService.parseUserId(header.removePrefix("Bearer ").trim())
            if (userId != null) {
                // principal = the user id (Long). Controllers read it with
                // @AuthenticationPrincipal userId: Long. No roles in this app -> empty list.
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        chain.doFilter(request, response) // always continue; the rules decide
    }
}
