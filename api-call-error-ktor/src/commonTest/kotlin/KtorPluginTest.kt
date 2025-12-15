package com.ahparhizgar.apicallerror

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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorPluginTest {
    var handler: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData)? = null
    val mockEngine = MockEngine {
        handler!!.invoke(this, it)
    }

    val client = HttpClient(mockEngine) {
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
    fun `status 400 should be converted to NotFound`() = runTest {
        handler = {
            respond("", HttpStatusCode.NotFound)
        }
        assertFailsWith<NotFound> {
            client.get("/")
        }
    }

    @Test
    fun `SocketTimeOutException should be converted to NetworkError`() = runTest {
        handler = {
            throw SocketTimeoutException("Socket timed out")
        }
        assertFailsWith<NetworkError> {
            client.get("/")
        }
    }

    @Test
    fun `valid json response should now throw`() = runTest {
        handler = {
            respond(
                content = """{"id": 1, "name": "Test"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        client.get("/").body<TestData>()
    }

    @Test
    fun `malformed json structure should throw Invalid DataError`() = runTest {
        handler = {
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
    fun `json response without content type should throw InvalidDataError`() = runTest {
        handler = {
            respond(
                content = """{"id": 1, "name": "Test"}""",
                status = HttpStatusCode.OK,
            )
        }
        assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
    }

    @Test
    fun `json response with missing field should throw InvalidDataError`() = runTest {
        handler = {
            respond(
                content = """{"id": 1}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        val e = assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
        assertTrue(
            actual = e.cause?.message?.contains("Field 'name' is required") ?: false,
            message = "error message of cause should contain Field name is required"
        )
    }
}

@Serializable
data class TestData(val id: Int, val name: String)
