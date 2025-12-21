package com.ahparhizgar.apicallerror

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Base class for all errors produced or surfaced by the *Api Call Error* library.
 * Custom errors in calling backed should also extend this class to provide structured error
 * handling.
 *
 * Hierarchy:
 * ```
 *      ApiCallError
 *      ├─ InvalidDataError
 *      ├─ NetworkError
 *      └─ HttpError
 *          ├─ ServerError
 *          └─ ClientError
 *              ├─ BadRequest
 *              ├─ Unauthorized
 *              ├─ Forbidden
 *              ├─ NotFound
 *              ├─ RateLimitReached
 *              └─ Other
 * ```
 */
abstract class ApiCallError : Exception() {
    /**
     * Custom payload to store additional information about the error.
     *
     * The payload can be parsed from server responses when available
     * and may contain structured data (maps/objects) that consumers can use
     * for display to the user or for error handling.
     *
     * It is always null for errors created by the library itself.
     *
     * @see ClientError.userMessage for server-provided user-facing message.
     * @see ClientError.key for a server-provided error key/identifier.
     * @see HttpError.code for HTTP status code when applicable.
     */
    abstract val payload: Any?
}

/**
 * Represents errors due to invalid or missing fields in the response.
 * Thrown by the library when structure of response data is invalid. i.e.,
 * required fields are missing or have incorrect types.
 * Or by the user when data integrity checks fail.
 * @see invalidData
 * @see requireData
 */
open class InvalidDataError(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : ApiCallError()

/**
 * Throw an [InvalidDataError] with optional message, cause and payload.
 *
 * This helper is convenient when you need to abort execution due to
 * validation or missing required data.
 *
 * @throws InvalidDataError always
 */
public fun invalidData(
    message: String? = null,
    cause: Throwable? = null,
    payload: Any? = null,
): Nothing = throw InvalidDataError(
    message = message,
    cause = cause,
    payload = payload,
)

/**
 * Require a condition and throw [InvalidDataError] when it is false.
 *
 * Use this like a lightweight assertion for response or data integrity checks.
 *
 * @param condition the boolean condition that must be true
 * @param payload optional payload to attach to the thrown error
 * @param message a lambda producing the error message to avoid allocating when condition is true
 */
@OptIn(ExperimentalContracts::class)
public fun requireData(
    condition: Boolean,
    payload: Any? = null,
    message: () -> String,
) {
    contract {
        returns() implies condition
    }
    if (!condition) {
        invalidData(
            message = message(),
            payload = payload,
        )
    }
}

/**
 * Represents network-related errors such as timeouts, connectivity issues, etc.
 */
open class NetworkError(
    override val message: String? = null,
    /**
     * Actual platform-specific exception that caused this network error.
     */
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : ApiCallError()

/**
 * Represents HTTP-related errors that include a numeric status [code].
 */
abstract class HttpError : ApiCallError() {
    /**
     * HTTP status code returned by the server (e.g. 400, 401, 500).
     */
    abstract val code: Int
}

/**
 * Represents HTTP 5xx server errors.
 *
 * The [payload] can contain the raw parsed response body (map/object)
 */
class ServerError(
    override val code: Int,
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : HttpError()

/**
 * Represents HTTP 4xx client errors with optional structured metadata.
 *
 * The [payload] can contain the parsed response body.
 */
abstract class ClientError : ApiCallError() {
    /** HTTP status code for this client error (4xx). */
    abstract val code: Int

    /** Optional localized/user-friendly message coming from the server. */
    abstract val userMessage: String?

    /**
     * A key to identify the error type returned by the server.
     *
     * Can be used for localization or specific error handling. May be null if the
     * server does not provide a specific key or the response body wasn't parsed.
     */
    abstract val key: String?
}

class BadRequest(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    override val code: Int = HttpStatus.BAD_REQUEST.code
}

class Unauthorized(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    override val code: Int = HttpStatus.UNAUTHORIZED.code
}

class Forbidden(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    override val code: Int = HttpStatus.FORBIDDEN.code
}

class NotFound(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    override val code: Int = HttpStatus.NOT_FOUND.code
}

class RateLimitReached(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    override val code: Int = HttpStatus.RATE_LIMIT.code
}

class OtherClientError(
    override val code: Int,
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val userMessage: String? = null,
    override val payload: Any? = null,
    override val key: String? = null,
) : ClientError() {
    init {
        require(code !in HttpStatus.codes) { "Use specific ClientError subclass for code $code" }
    }
}

public fun ClientError(
    code: Int,
    key: String? = null,
    message: String? = null,
    userMessage: String? = null,
    cause: Throwable? = null,
    payload: Any? = null,
): ClientError = when (HttpStatus.ofCode(code)) {
    HttpStatus.BAD_REQUEST -> BadRequest(
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )

    HttpStatus.UNAUTHORIZED -> Unauthorized(
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )

    HttpStatus.FORBIDDEN -> Forbidden(
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )

    HttpStatus.NOT_FOUND -> NotFound(
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )

    HttpStatus.RATE_LIMIT -> RateLimitReached(
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )

    else -> OtherClientError(
        code = code,
        message = message,
        cause = cause,
        userMessage = userMessage,
        payload = payload,
        key = key
    )
}
