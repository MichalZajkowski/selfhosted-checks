package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import vouch.Check
import vouch.CheckResult
import java.net.URL

class RateLimitHeadersCheck(private val client: HttpClient) : Check {
    override val name: String = "rate_limit_headers"

    override suspend fun run(target: URL): CheckResult {
        val url = "${target.toString().trimEnd('/')}/api/v1/instance"
        return try {
            val response = client.get(url)
            val limit = response.headers["X-RateLimit-Limit"]
            val remaining = response.headers["X-RateLimit-Remaining"]
            when {
                limit != null && remaining != null ->
                    CheckResult.Pass("limit=$limit remaining=$remaining")
                limit != null || remaining != null ->
                    CheckResult.Fail("only partial rate-limit headers exposed (limit=$limit, remaining=$remaining)")
                else ->
                    CheckResult.Fail("no X-RateLimit-* headers exposed on /api/v1/instance")
            }
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
