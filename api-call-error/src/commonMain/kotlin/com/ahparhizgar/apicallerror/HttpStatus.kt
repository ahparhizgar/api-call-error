package com.ahparhizgar.apicallerror

internal enum class HttpStatus(val code: Int) {
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    RATE_LIMIT(429),
    BAD_REQUEST(400),
    ;

    companion object Companion {
        val codes: List<Int> = entries.map { it.code }
    }
}