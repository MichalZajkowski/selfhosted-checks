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
            // Missing rate-limit headers do not mean the instance is unhealthy:
            // a reverse proxy can legitimately strip them. Treated as non-fatal,
            // consistent with PeersCheck's handling of a disabled peers endpoint.
            // A failed request (exception below) is the only fatal outcome here.
            when {
                limit != null && remaining != null ->
                    CheckResult.Pass("limit=$limit remaining=$remaining")
                limit != null || remaining != null ->
                    CheckResult.Pass("partial rate-limit headers (limit=$limit, remaining=$remaining) — non-fatal")
                else ->
                    CheckResult.Pass("no X-RateLimit-* headers exposed — non-fatal (a reverse proxy may strip them)")
            }
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
