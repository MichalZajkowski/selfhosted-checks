package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonArray
import vouch.Check
import vouch.CheckResult
import vouch.HttpClientFactory
import java.net.URL

class PeersCheck(private val client: HttpClient) : Check {
    override val name: String = "federation_peers"

    override suspend fun run(target: URL): CheckResult {
        val url = "${target.toString().trimEnd('/')}/api/v1/instance/peers"
        return try {
            val response = client.get(url)
            val code = response.status.value
            if (code == 401 || code == 403 || code == 404) {
                return CheckResult.Pass("endpoint disabled ($code) — instance has restricted peers, treated as non-fatal")
            }
            if (code != 200) {
                return CheckResult.Fail("expected 200 from /api/v1/instance/peers, got $code")
            }
            val parsed = HttpClientFactory.json.parseToJsonElement(response.bodyAsText())
            if (parsed !is JsonArray) {
                return CheckResult.Fail("peers response is not a JSON array")
            }
            CheckResult.Pass("peers array returned, size=${parsed.size}")
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
