package com.ahparhizgar.katch.ktor

import com.ahparhizgar.katch.ClientError
import com.ahparhizgar.katch.InvalidDataError
import com.ahparhizgar.katch.NetworkError
import com.ahparhizgar.katch.ServerError
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

class KatchPlugin private constructor(private val config: Config) {
    class Config {
        internal var payloadExtractor: suspend (HttpResponse) -> ClientErrorExtras? = { null }

        /**
         * Called when 4xx or 5xx responses are received to extract additional
         * information from the response.
         * [block] receives a [HttpResponse] and should return a [ClientErrorExtras] object.
         * In case of 5xx responses, only the payload is used.
         */
        fun extractPayload(block: suspend (HttpResponse) -> ClientErrorExtras?) {
            payloadExtractor = block
        }
    }

    companion object Plugin : HttpClientPlugin<Config, KatchPlugin> {
        override val key = AttributeKey<KatchPlugin>("KatchPlugin")

        override fun prepare(block: Config.() -> Unit): KatchPlugin {
            val config = Config().apply(block)
            return KatchPlugin(config)
        }

        override fun install(plugin: KatchPlugin, scope: HttpClient) {
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
                            message = extras?.debugMessage ?: "Client Error ($code)",
                            code = code,
                            key = extras?.errorKey,
                            userMessage = extras?.userMessage,
                            payload = extras?.payload,
                        )
                    }

                    in 500..599 -> {
                        val extras = plugin.config.payloadExtractor(call.response)
                        throw ServerError(
                            message = extras?.debugMessage ?: "Server Error ($code)",
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

class ClientErrorExtras(
    val userMessage: String? = null,
    val debugMessage: String? = null,
    val errorKey: String? = null,
    val payload: Any? = null
)
