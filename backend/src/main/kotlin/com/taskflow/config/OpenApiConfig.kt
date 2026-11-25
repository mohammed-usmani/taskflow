package com.taskflow.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI at /swagger-ui.html. springdoc reads the controllers and DTOs itself;
 * this only adds the "Authorize" button: log in, paste the token, and every call
 * from the UI sends "Authorization: Bearer <token>".
 */
@Configuration
@OpenAPIDefinition(
    info = Info(title = "TaskFlow API", version = "1.0"),
    security = [SecurityRequirement(name = "bearerAuth")],
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig
