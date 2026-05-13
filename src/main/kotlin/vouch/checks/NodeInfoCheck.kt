package vouch.checks

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import vouch.Check
import vouch.CheckResult
import vouch.HttpClientFactory
import java.net.URL

class NodeInfoCheck(private val client: HttpClient) : Check {
    override val name: String = "nodeinfo_2_0"

    override suspend fun run(target: URL): CheckResult {
        val base = target.toString().trimEnd('/')
        return try {
            val discoveryResp = client.get("$base/.well-known/nodeinfo")
            val nodeinfoUrl: String = if (discoveryResp.status.value == 200) {
                val disc = HttpClientFactory.json.parseToJsonElement(discoveryResp.bodyAsText())
                val links = (disc as? JsonObject)?.get("links") as? JsonArray
                val link20 = links?.mapNotNull { it as? JsonObject }
                    ?.firstOrNull { it["rel"]?.jsonPrimitive?.content?.contains("schema/2.0") == true }
                link20?.get("href")?.jsonPrimitive?.content ?: "$base/nodeinfo/2.0"
            } else {
                "$base/nodeinfo/2.0"
            }

            val resp = client.get(nodeinfoUrl)
            if (resp.status.value != 200) {
                return CheckResult.Fail("nodeinfo 2.0 returned ${resp.status.value}")
            }
            val parsed = HttpClientFactory.json.parseToJsonElement(resp.bodyAsText()) as? JsonObject
                ?: return CheckResult.Fail("nodeinfo response not a JSON object")
            val version = parsed["version"]?.jsonPrimitive?.content
            val software = (parsed["software"] as? JsonObject)?.get("name")?.jsonPrimitive?.content
            if (version != "2.0") {
                return CheckResult.Fail("nodeinfo version is '$version', expected '2.0'")
            }
            if (software.isNullOrBlank()) {
                return CheckResult.Fail("nodeinfo missing software.name")
            }
            CheckResult.Pass("nodeinfo 2.0 OK, software=$software")
        } catch (e: Exception) {
            CheckResult.Error("request failed: ${e.message}", e)
        }
    }
}
