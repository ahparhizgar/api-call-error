package io.github.kotlin.fibonacci

import com.ahparhizgar.apicallerror.InvalidDataError
import com.ahparhizgar.apicallerror.NetworkError
import com.ahparhizgar.apicallerror.NotFound
import com.ahparhizgar.apicallerror.ktor.ApiCallErrorPlugin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
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
    var handler: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData)? = null
    val mockEngine2 = MockEngine {
        handler!!.invoke(this, it)
    }

    val client = HttpClient(mockEngine2) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                }
            )
        }
        install(ApiCallErrorPlugin)
    }

    @Test
    fun `test setup works ok`() = runTest {
        handler = { request ->
            respond("", HttpStatusCode.OK)
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test 400 error is thrown`() = runTest {
        handler = { request ->
            respond("", HttpStatusCode.NotFound)
        }
        assertFailsWith<NotFound> {
            client.get("/")
        }
    }

    @Test
    fun `test network error`() = runTest {
        handler = { request ->
            throw SocketTimeoutException("Socket timed out")
        }
        assertFailsWith<NetworkError> {
            client.get("/")
        }
    }

    @Test
    fun `test content negotiation with JsonConvertException`() = runTest {
        handler = { request ->
            respond(
                content = "{",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
    }

    @Test
    fun `has transformation`() = runTest {
        handler = { request ->
            respond(
                content = """{"id": 1, "name": "Test"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        client.get("/").body<TestData>()
    }

    @Test
    fun `no transformation`() = runTest {
        handler = { request ->
            respond(
                content = """{"id": 1, "name": "Test"}""",
                status = HttpStatusCode.OK,
            )
        }
        assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
    }
}

@Serializable
data class TestData(val id: Int, val name: String)