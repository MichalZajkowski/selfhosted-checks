package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import vouch.CheckResult
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertTrue

class RateLimitHeadersCheckTest {

    private fun clientWith(headers: Headers): HttpClient {
        val engine = MockEngine {
            respond(content = "", status = HttpStatusCode.OK, headers = headers)
        }
        return HttpClient(engine) { expectSuccess = false }
    }

    @Test
    fun `pass and report values when both headers present`() = runTest {
        val headers = Headers.build {
            append("X-RateLimit-Limit", "300")
            append("X-RateLimit-Remaining", "299")
        }
        val result = RateLimitHeadersCheck(clientWith(headers))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("limit=300"), result.message)
        assertTrue(result.message.contains("remaining=299"), result.message)
    }

    @Test
    fun `non-fatal pass when only one header present`() = runTest {
        val headers = Headers.build { append("X-RateLimit-Limit", "300") }
        val result = RateLimitHeadersCheck(clientWith(headers))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected non-fatal Pass, got $result")
        assertTrue(result.message.contains("non-fatal"), result.message)
    }

    @Test
    fun `non-fatal pass when no rate-limit headers present`() = runTest {
        val result = RateLimitHeadersCheck(clientWith(Headers.Empty))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected non-fatal Pass, got $result")
        assertTrue(result.message.contains("non-fatal"), result.message)
    }
}
