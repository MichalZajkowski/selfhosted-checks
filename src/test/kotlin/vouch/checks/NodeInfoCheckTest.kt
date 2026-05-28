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

class NodeInfoCheckTest {

    private val jsonHeaders =
        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val discoveryLinks = """
        {"links":[{"rel":"http://nodeinfo.diaspora.software/ns/schema/2.0","href":"https://example.social/nodeinfo/2.0"}]}
    """.trimIndent()

    /**
     * The check makes up to two requests: NodeInfo discovery
     * (`/.well-known/nodeinfo`) and the NodeInfo document itself. Branch on the
     * path so each receives the right canned response.
     */
    private fun client(
        discoveryStatus: HttpStatusCode,
        discoveryBody: String,
        nodeinfoStatus: HttpStatusCode,
        nodeinfoBody: String,
    ): HttpClient {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.contains("well-known")) {
                respond(discoveryBody, discoveryStatus, jsonHeaders)
            } else {
                respond(nodeinfoBody, nodeinfoStatus, jsonHeaders)
            }
        }
        return HttpClient(engine) { expectSuccess = false }
    }

    @Test
    fun `pass via discovery when nodeinfo 2_0 conforms`() = runTest {
        val nodeinfo = """{"version":"2.0","software":{"name":"mastodon","version":"4.6.0"}}"""
        val result = NodeInfoCheck(client(HttpStatusCode.OK, discoveryLinks, HttpStatusCode.OK, nodeinfo))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("mastodon"), result.message)
    }

    @Test
    fun `pass via fallback path when discovery is missing`() = runTest {
        val nodeinfo = """{"version":"2.0","software":{"name":"mastodon"}}"""
        val result = NodeInfoCheck(client(HttpStatusCode.NotFound, "", HttpStatusCode.OK, nodeinfo))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Pass, "expected Pass, got $result")
        assertTrue(result.message.contains("mastodon"), result.message)
    }

    @Test
    fun `fail when nodeinfo version is not 2_0`() = runTest {
        val nodeinfo = """{"version":"2.1","software":{"name":"mastodon"}}"""
        val result = NodeInfoCheck(client(HttpStatusCode.NotFound, "", HttpStatusCode.OK, nodeinfo))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("2.1"), result.message)
    }

    @Test
    fun `fail when software name is missing`() = runTest {
        val nodeinfo = """{"version":"2.0","software":{}}"""
        val result = NodeInfoCheck(client(HttpStatusCode.NotFound, "", HttpStatusCode.OK, nodeinfo))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("software"), result.message)
    }

    @Test
    fun `fail when nodeinfo endpoint returns non-200`() = runTest {
        val result = NodeInfoCheck(client(HttpStatusCode.NotFound, "", HttpStatusCode.InternalServerError, ""))
            .run(URL("https://example.social"))
        assertTrue(result is CheckResult.Fail, "expected Fail, got $result")
        assertTrue(result.message.contains("500"), result.message)
    }
}
