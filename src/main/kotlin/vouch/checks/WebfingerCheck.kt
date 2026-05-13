package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import vouch.Check
import vouch.CheckResult
import vouch.HttpClientFactory
import java.net.URL

class WebfingerCheck(private val client: HttpClient) : Check {
    override val name: String = "webfinger"

    override suspend fun run(target: URL): CheckResult {
        val host = target.host
        val probeAccount = "acct:nonexistent-probe-${System.currentTimeMillis()}@$host"
        val url = "${target.toString().trimEnd('/')}/.well-known/webfinger?resource=$probeAccount"
        return try {
            val response = client.get(url)
            val code = response.status.value
            // 404 is a valid response for a non-existent account; both 200 and 404
            // confirm the endpoint is functioning. 4xx/5xx other than 404 indicate failure.
            when {
                code == 200 -> {
                    val body = response.bodyAsText()
                    val parsed = runCatching { HttpClientFactory.json.parseToJsonElement(body) }.getOrNull()
                    if (parsed is JsonObject && "subject" in parsed) {
                        CheckResult.Pass("200 OK with valid JRD")
                    } else {
                        CheckResult.Fail("200 response missing JRD subject field")
                    }
                }
                code == 404 || code == 410 -> {
                    CheckResult.Pass("endpoint live ($code for unknown account)")
                }
                else -> CheckResult.Fail("unexpected status $code from webfinger endpoint")
            }
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
