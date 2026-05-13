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

class InstanceApiCheckTest {

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
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
    fun `pass when 200 and required fields present`() = runTest {
        val body = """{"domain":"example.social","title":"Example","version":"4.3.0"}"""
        val result = InstanceApiCheck(clientReturning(HttpStatusCode.OK, body))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("example.social"), result.message)
        assertTrue(result.message.contains("4.3.0"), result.message)
    }

    @Test
    fun `fail when status is not 200`() = runTest {
        val result = InstanceApiCheck(clientReturning(HttpStatusCode.NotFound, ""))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("404"), result.message)
    }

    @Test
    fun `fail when required field missing`() = runTest {
        val body = """{"domain":"example.social","title":"Example"}"""
        val result = InstanceApiCheck(clientReturning(HttpStatusCode.OK, body))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("version"), result.message)
    }

    @Test
    fun `fail when response is not a JSON object`() = runTest {
        val result = InstanceApiCheck(clientReturning(HttpStatusCode.OK, """[1,2,3]"""))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
    }
}
