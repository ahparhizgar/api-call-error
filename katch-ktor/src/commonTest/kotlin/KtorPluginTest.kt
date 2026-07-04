package com.ahparhizgar.apicallerror

import com.ahparhizgar.apicallerror.ktor.ApiCallErrorPlugin
import com.ahparhizgar.apicallerror.ktor.ClientErrorExtras
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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorPluginTest {
    var handler: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData)? = null
    var payloadExtractor: (HttpResponse) -> ClientErrorExtras? = { null }
    val mockEngine = MockEngine {
        handler!!.invoke(this, it)
    }

    val client = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json)
        }
        install(ApiCallErrorPlugin) {
            extractPayload {
                this@KtorPluginTest.payloadExtractor(it)
            }
        }
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
    fun `valid json response should run`() = runTest {
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
        val e = assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
        assertContains(e.cause?.message.orEmpty(), "Expected end of the object")
    }

    @Test
    fun `json response without content type should throw InvalidDataError`() = runTest {
        handler = {
            respond(
                content = """{"id": 1, "name": "Test"}""",
                status = HttpStatusCode.OK,
            )
        }
        val e = assertFailsWith<InvalidDataError> {
            client.get("/").body<TestData>()
        }
        assertContains(e.message.orEmpty(), "No suitable deserializer found")
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
        assertContains(e.cause?.message.orEmpty(), "Field 'name' is required")
    }

    @Test
    fun `status 400 with payload extractor should populate ClientErrorExtras`() = runTest {
        payloadExtractor = { response ->
            ClientErrorExtras(
                errorKey = "INVALID_REQUEST",
                userMessage = "The request was invalid",
                payload = mapOf("detail" to "Missing required parameter 'id'")
            )
        }
        handler = {
            respond("", HttpStatusCode.BadRequest)
        }

        val e = assertFailsWith<ClientError> {
            client.get("/")
        }

        assertTrue(
            message = "error key should be populated",
            actual = e.key == "INVALID_REQUEST",
        )
        assertTrue(
            message = "user message should be populated",
            actual = e.userMessage == "The request was invalid",
        )
        assertTrue(
            message = "payload should be populated",
            actual = (e.payload as? Map<*, *>)?.get("detail") == "Missing required parameter 'id'",
        )
    }
}

@Serializable
data class TestData(val id: Int, val name: String)
