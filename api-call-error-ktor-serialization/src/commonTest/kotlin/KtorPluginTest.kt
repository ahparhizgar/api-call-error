package io.github.kotlin.fibonacci

import com.ahparhizgar.apicallerror.ApiCallError
import com.ahparhizgar.apicallerror.ktor.ApiCallErrorPlugin
import com.ahparhizgar.apicallerror.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorPluginTest {
    val mockEngine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/200" -> respond("", HttpStatusCode.OK)
            "/400" -> respond("", HttpStatusCode.NotFound)
            "/network" -> throw Exception("Network error")
            "/badjson" -> respond("This is not JSON", HttpStatusCode.OK, headers = headersOf("Content-Type" to listOf("application/json")))
            else -> error(request.url.encodedPath)
        }
    }

    val client = HttpClient(mockEngine) {
        install(ApiCallErrorPlugin)
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                }
            )
        }
    }

    @Test
    fun `test setup works ok`() = runTest {
        client.get("/200").apply {
            assertEquals(HttpStatusCode.OK, this.status)
        }
    }

    @Test
    fun `test 400 error is thrown`() = runTest {
        assertFailsWith<ApiCallError> {
            client.get("/400")
        }
    }

    @Test
    fun `test newtok error`() = runTest {
        assertFailsWith<NetworkError> {
            client.get("/network")
        }
    }

    @Test
    fun `test content negotiation`() = runTest {
        assertFailsWith<ApiCallError> {
            client.get("/badjson").body<TestData>()
        }
    }
}

@Serializable
data class TestData(val id: Int, val name: String)