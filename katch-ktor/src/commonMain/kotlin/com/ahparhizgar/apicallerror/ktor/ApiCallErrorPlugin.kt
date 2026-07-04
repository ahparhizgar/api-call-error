package com.ahparhizgar.apicallerror.ktor

import com.ahparhizgar.apicallerror.ClientError
import com.ahparhizgar.apicallerror.InvalidDataError
import com.ahparhizgar.apicallerror.NetworkError
import com.ahparhizgar.apicallerror.ServerError
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.serialization.ContentConvertException
import io.ktor.util.AttributeKey
import io.ktor.util.reflect.instanceOf
import kotlinx.io.IOException

class ApiCallErrorPlugin private constructor(private val config: Config) {
    class Config {
        internal var payloadExtractor: (HttpResponse) -> ClientErrorExtras? = { null }

        /**
         * Called when 4xx or 5xx responses are received to extract additional
         * information from the response.
         * [block] receives a [HttpResponse] and should return a [ClientErrorExtras] object.
         * In case of 5xx responses, only the payload is used.
         */
        fun extractPayload(block: (HttpResponse) -> ClientErrorExtras?) {
            payloadExtractor = block
        }
    }

    companion object Plugin : HttpClientPlugin<Config, ApiCallErrorPlugin> {
        override val key = AttributeKey<ApiCallErrorPlugin>("ApiCallErrorPlugin")

        override fun prepare(block: Config.() -> Unit): ApiCallErrorPlugin {
            val config = Config().apply(block)
            return ApiCallErrorPlugin(config)
        }

        override fun install(plugin: ApiCallErrorPlugin, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.State) {
                val call = try {
                    proceed()
                } catch (e: IOException) {
                    throw NetworkError(message = "A network failure occurred", cause = e)
                }
                call as HttpClientCall
                when (val code = call.response.status.value) {
                    in 400..499 -> {
                        val extras = plugin.config.payloadExtractor(call.response)
                        throw ClientError(
                            message = "Client Error ($code)",
                            code = code,
                            key = extras?.errorKey,
                            userMessage = extras?.userMessage,
                            payload = extras?.payload,
                        )
                    }

                    in 500..599 -> {
                        val extras = plugin.config.payloadExtractor(call.response)
                        throw ServerError(
                            message = "Server Error ($code)",
                            code = code,
                            payload = extras?.payload,
                        )
                    }
                }
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Parse) {
                try {
                    proceedWith(subject).also {
                        if (!it.response.instanceOf(it.expectedType.type)) {
                            throw InvalidDataError(message = "No suitable deserializer found for type ${it.expectedType}")
                        }
                    }
                } catch (e: ContentConvertException) {
                    throw InvalidDataError(
                        message = "Failed to convert JSON response",
                        cause = e.cause
                    )
                }
            }
        }
    }
}

class ClientErrorExtras(val userMessage: String?, val errorKey: String?, val payload: Any?)