package com.ahparhizgar.apicallerror

abstract class ApiCallError : Throwable() {
    /**
     * Custom payload to store additional information about the error.
     * Can be used to provide additional context or data related to the error.
     * It's always null if the error is created by the library itself.
     * @see [ClientError.userMessage] field in for server-provided user-friendly messages.
     * @see [ClientError.key] field for server-provided error identification key.
     * @see [HttpError.code] field for HTTP status code of the error response.
     */
    abstract val payload: Any?
}

open class InvalidDataError(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : ApiCallError()

public fun invalidData(
    message: String? = null,
    cause: Throwable? = null,
    payload: Any? = null,
): Nothing = throw InvalidDataError(
    message = message,
    cause = cause,
    payload = payload,
)

public fun requireData(
    condition: Boolean,
    payload: Any? = null,
    message: () -> String,
) {
    if (!condition) {
        invalidData(
            message = message(),
            payload = payload,
        )
    }
}

open class NetworkError(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : ApiCallError()

abstract class HttpError : ApiCallError() {
    /**
     * Http status code of the error response
     */
    abstract val code: Int
}

class ServerError(
    override val code: Int,
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null,
) : HttpError()

abstract class ClientError : ApiCallError() {
    abstract val code: Int
    abstract val userMessage: String?

    // TODO add documentation on how to parse the payload
    /**
     * A key to identify the error type returned by the server.
     * Can be used for localization or specific error handling.
     * May be null if the server does not provide a specific key or the response is not parsed.
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

class Other(
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

//public fun ClientError(
//    code: Int,
//    key: String?,
//    message: String? = null,
//    cause: Throwable? = null,
//): ClientError = when (code) {
//    HttpStatusCode.UNAUTHORIZED -> Unauthorized(key, message, cause)
//    HttpStatusCode.FORBIDDEN -> Forbidden(key, message, cause)
//    HttpStatusCode.NOT_FOUND -> NotFound(key, message, cause)
//    HttpStatusCode.RATE_LIMIT -> RateLimitReached(key, message, cause)
//    else -> Other(code, key, message, cause)
//}

