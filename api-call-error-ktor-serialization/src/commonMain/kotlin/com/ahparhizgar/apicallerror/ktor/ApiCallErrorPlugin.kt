package com.ahparhizgar.apicallerror.ktor

import com.ahparhizgar.apicallerror.ClientError
import com.ahparhizgar.apicallerror.InvalidDataError
import com.ahparhizgar.apicallerror.NetworkError
import com.ahparhizgar.apicallerror.ServerError
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.serialization.ContentConvertException
import io.ktor.serialization.JsonConvertException
import io.ktor.util.AttributeKey
import io.ktor.util.reflect.instanceOf
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

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
                val call = try {
                    proceed()
                } catch (e: IOException) {
                    throw NetworkError(message = "A network failure occurred", cause = e)
                }
                call as HttpClientCall
                when (val code = call.response.status.value) {
                    in 400..499 -> throw ClientError(message = "Client Error ($code)", code = code)
                    in 500..599 -> throw ServerError(code = code, message = "Server Error ($code)")
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
                    throw InvalidDataError(message = "Failed to convert JSON response", cause = e.cause)
                }
            }
        }
    }
}