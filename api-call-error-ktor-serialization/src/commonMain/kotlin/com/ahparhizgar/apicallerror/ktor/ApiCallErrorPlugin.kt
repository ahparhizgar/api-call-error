package com.ahparhizgar.apicallerror.ktor

import com.ahparhizgar.apicallerror.ApiCallError
import com.ahparhizgar.apicallerror.ClientError
import com.ahparhizgar.apicallerror.InvalidDataError
import com.ahparhizgar.apicallerror.NetworkError
import com.ahparhizgar.apicallerror.NotFound
import com.ahparhizgar.apicallerror.ServerError
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.content.NullBody
import io.ktor.serialization.JsonConvertException
import io.ktor.util.AttributeKey
import io.ktor.util.reflect.instanceOf
import kotlinx.io.IOException

class ApiCallErrorPlugin private constructor() {
    class Config {
        // you can add configuration options here if needed
    }

    companion object Plugin : HttpClientPlugin<Config, ApiCallErrorPlugin> {
        override val key = AttributeKey<ApiCallErrorPlugin>("ApiCallErrorPlugin")

        override fun prepare(block: Config.() -> Unit): ApiCallErrorPlugin {
            val config = Config().apply(block)
            return ApiCallErrorPlugin()
        }

        override fun install(plugin: ApiCallErrorPlugin, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.State) {
                val originalCall = try {
                    proceed()
                } catch (e: IOException) {
                    throw NetworkError("A network error occurred: ${e.message}")
                }
                originalCall as HttpClientCall
                when (val code = originalCall.response.status.value) {
                    in 400..499 -> throw NotFound("HTTP Error: ${originalCall.response.status.value}")
                    in 500..599 -> throw ServerError(
                        code = code,
                        message = "Server Error: ${originalCall.response.status.value}"
                    )

                    else -> originalCall
                }
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Parse) {
                try {
                    proceedWith(subject).also {
                        if (!it.response.instanceOf(it.expectedType.type)) {
                            throw InvalidDataError(
                                "No suitable deserializer found for type ${it.expectedType}. " +
                                        "Or you may Installed api-call-error plugin after content negotiation."
                            )
                        }
                    }
                } catch (e: Exception) {
                    throw InvalidDataError("Failed to deserialize JSON response: ${e.message}")
                }
            }
        }
    }
}