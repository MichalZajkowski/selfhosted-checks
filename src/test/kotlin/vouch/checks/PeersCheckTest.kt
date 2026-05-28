package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import vouch.CheckResult
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertTrue

class PeersCheckTest {

    private fun clientReturning(status: HttpStatusCode, body: String = ""): HttpClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        return HttpClient(engine) { expectSuccess = false }
    }

    @Test
    fun `pass and report size when 200 with a JSON array`() = runTest {
        val result = PeersCheck(clientReturning(HttpStatusCode.OK, """["a.social","b.social","c.social"]"""))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("size=3"), result.message)
    }

    @Test
    fun `soft pass when peers endpoint is restricted (403)`() = runTest {
        val result = PeersCheck(clientReturning(HttpStatusCode.Forbidden))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected non-fatal Pass, got $result")
        assertTrue(result.message.contains("403"), result.message)
    }

    @Test
    fun `soft pass when peers endpoint is 404`() = runTest {
        val result = PeersCheck(clientReturning(HttpStatusCode.NotFound))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected non-fatal Pass, got $result")
        assertTrue(result.message.contains("404"), result.message)
    }

    @Test
    fun `fail when 200 but response is not a JSON array`() = runTest {
        val result = PeersCheck(clientReturning(HttpStatusCode.OK, """{"peers":"nope"}"""))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("array"), result.message)
    }

    @Test
    fun `fail on unexpected server error`() = runTest {
        val result = PeersCheck(clientReturning(HttpStatusCode.InternalServerError))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("500"), result.message)
    }
}
