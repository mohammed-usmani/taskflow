package com.taskflow.common

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * @RestControllerAdvice = applies to every controller. Whenever a controller (or the
 * service it calls) throws, Spring looks here for the most specific @ExceptionHandler
 * matching the exception type, and returns what that handler returns.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    // Our own domain errors: 404, 409, 401 ...
    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException, req: HttpServletRequest) =
        respond(ex.status, ex.message ?: ex.status.reasonPhrase, req)

    // @Valid failed on a request body -> 400 with one entry per invalid field
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, req: HttpServletRequest) =
        respond(
            HttpStatus.BAD_REQUEST, "Validation failed", req,
            ex.bindingResult.fieldErrors.map { FieldError(it.field, it.defaultMessage ?: "is invalid") },
        )

    // Body isn't valid JSON, or a value has the wrong type (e.g. "priority": "URGENT")
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        respond(HttpStatus.BAD_REQUEST, "Malformed request body", req)

    // Path/query param of the wrong type, e.g. /api/projects/abc
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        respond(HttpStatus.BAD_REQUEST, "Invalid value for '${ex.name}'", req)

    // A DB constraint fired (e.g. two simultaneous registrations with the same email
    // both passed the "exists?" check; the unique index stops the second one).
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(ex: DataIntegrityViolationException, req: HttpServletRequest) =
        respond(HttpStatus.CONFLICT, "Request conflicts with existing data", req)

    // Unknown URL / wrong HTTP method: without these, the catch-all below would turn them into 500s
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException, req: HttpServletRequest) =
        respond(HttpStatus.NOT_FOUND, "No endpoint ${req.method} ${req.requestURI}", req)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethod(ex: HttpRequestMethodNotSupportedException, req: HttpServletRequest) =
        respond(HttpStatus.METHOD_NOT_ALLOWED, "Method ${req.method} not allowed here", req)

    // Anything unexpected: log the real cause, never leak it to the client.
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("Unhandled error on {} {}", req.method, req.requestURI, ex)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", req)
    }

    private fun respond(
        status: HttpStatus,
        message: String,
        req: HttpServletRequest,
        fieldErrors: List<FieldError> = emptyList(),
    ) = ResponseEntity.status(status).body(
        ErrorResponse(status.value(), status.reasonPhrase, message, req.requestURI, fieldErrors = fieldErrors),
    )
}
