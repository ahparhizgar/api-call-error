package com.ahparhizgar.apicallerror.ktor

import com.ahparhizgar.apicallerror.ApiCallError
import com.ahparhizgar.apicallerror.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.serialization.JsonConvertException
import io.ktor.util.AttributeKey

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
                } catch (e: Exception) {
                    if (e.message == "Network error") {
                        throw NetworkError("A network error occurred: ${e.message}")
                    }
                    when (e) {
                        is JsonConvertException -> throw ApiCallError("Failed to deserialize JSON response: ${e.message}")
                        else -> throw e
                    }
                }
                originalCall as HttpClientCall
                when (originalCall.response.status.value) {
                    in 400..499 -> throw ApiCallError("HTTP Error: ${originalCall.response.status.value}")
                    in 500..599 -> throw ApiCallError("Server Error: ${originalCall.response.status.value}")
                    else -> originalCall
                }
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Transform) {
                try {
                    proceedWith(subject)
                } catch (e: Exception) {
                    throw ApiCallError("Failed to deserialize JSON response: ${e.message}")
                }
            }
        }
    }
}