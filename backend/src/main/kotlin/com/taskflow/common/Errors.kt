package com.taskflow.common

import org.springframework.http.HttpStatus
import java.time.Instant

/**
 * The ONE error shape every endpoint returns.
 *
 *   { "status": 400, "error": "Bad Request", "message": "Validation failed",
 *     "path": "/api/auth/register", "timestamp": "...",
 *     "fieldErrors": [ { "field": "email", "message": "must be a well-formed email address" } ] }
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: List<FieldError> = emptyList(),
)

data class FieldError(val field: String, val message: String)

/**
 * Domain exceptions. Services throw these; they know nothing about HTTP responses.
 * GlobalExceptionHandler turns each into an ErrorResponse with the right status.
 * `sealed` = every subclass lives in this file, so the handler covers them all.
 */
sealed class ApiException(val status: HttpStatus, message: String) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(HttpStatus.BAD_REQUEST, message)
class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)
class ConflictException(message: String) : ApiException(HttpStatus.CONFLICT, message)
class UnauthorizedException(message: String) : ApiException(HttpStatus.UNAUTHORIZED, message)
