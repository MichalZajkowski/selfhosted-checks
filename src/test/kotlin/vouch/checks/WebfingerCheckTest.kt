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

class WebfingerCheckTest {

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
    fun `pass when 200 with valid JRD subject`() = runTest {
        val body = """{"subject":"acct:[email protected]","links":[]}"""
        val result = WebfingerCheck(clientReturning(HttpStatusCode.OK, body))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("JRD"), result.message)
    }

    @Test
    fun `fail when 200 but JRD missing subject`() = runTest {
        val body = """{"links":[]}"""
        val result = WebfingerCheck(clientReturning(HttpStatusCode.OK, body))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("subject"), result.message)
    }

    @Test
    fun `pass when 404 for unknown account`() = runTest {
        val result = WebfingerCheck(clientReturning(HttpStatusCode.NotFound))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("404"), result.message)
    }

    @Test
    fun `pass when 410 for unknown account`() = runTest {
        val result = WebfingerCheck(clientReturning(HttpStatusCode.Gone))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
    }

    @Test
    fun `fail on unexpected status`() = runTest {
        val result = WebfingerCheck(clientReturning(HttpStatusCode.InternalServerError))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("500"), result.message)
    }
}
