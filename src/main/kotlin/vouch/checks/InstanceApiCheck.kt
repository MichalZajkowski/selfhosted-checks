package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import vouch.Check
import vouch.CheckResult
import vouch.HttpClientFactory
import java.net.URL

class InstanceApiCheck(private val client: HttpClient) : Check {
    override val name: String = "instance_api_v2"

    private val requiredFields = listOf("domain", "title", "version")

    override suspend fun run(target: URL): CheckResult {
        val url = "${target.toString().trimEnd('/')}/api/v2/instance"
        return try {
            val response = client.get(url)
            if (response.status.value != 200) {
                return CheckResult.Fail("expected 200 from /api/v2/instance, got ${response.status.value}")
            }
            val body = response.bodyAsText()
            val parsed = HttpClientFactory.json.parseToJsonElement(body)
            if (parsed !is JsonObject) {
                return CheckResult.Fail("response is not a JSON object")
            }
            val missing = requiredFields.filter { it !in parsed }
            if (missing.isNotEmpty()) {
                return CheckResult.Fail("missing required fields: ${missing.joinToString(", ")}")
            }
            val domain = parsed["domain"]?.jsonPrimitive?.content ?: "?"
            val version = parsed["version"]?.jsonPrimitive?.content ?: "?"
            CheckResult.Pass("domain=$domain version=$version")
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
