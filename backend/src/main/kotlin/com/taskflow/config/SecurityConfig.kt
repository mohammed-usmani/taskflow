package com.taskflow.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.taskflow.auth.JwtAuthFilter
import com.taskflow.auth.JwtService
import com.taskflow.common.ErrorResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtService: JwtService,          // Spring injects beans into @Bean method params too
        objectMapper: ObjectMapper,
    ): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())       // uses the corsConfigurationSource bean below
            .csrf { it.disable() }                 // CSRF protects cookie sessions; we use Bearer tokens
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll()
                    // Spring forwards unhandled errors to /error; if that were protected,
                    // a 400/500 for an anonymous caller would turn into a confusing 401.
                    .requestMatchers("/error").permitAll()
                    // API docs and the health check (used by Docker) are public; no data in them.
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                // no/invalid token -> 401 in the same JSON shape as every other error
                it.authenticationEntryPoint(jsonUnauthorized(objectMapper))
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            // Our filter runs before Spring's username/password filter, so the user is
            // known by the time the authorization rules above are checked.
            .addFilterBefore(JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun jsonUnauthorized(objectMapper: ObjectMapper) = AuthenticationEntryPoint { request, response, _ ->
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            ErrorResponse(401, "Unauthorized", "Missing, invalid or expired token", request.requestURI),
        )
    }

    // The React dev server (localhost:5173) is a different origin than the API (:8080),
    // so the browser blocks its calls unless the API allows that origin explicitly.
    @Bean
    fun corsConfigurationSource(
        @Value("\${app.cors.allowed-origins}") allowedOrigins: List<String>,
    ): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", config) }
    }
}
