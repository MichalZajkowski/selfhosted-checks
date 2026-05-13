package vouch

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun create(): HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(UserAgent) { agent = "selfhosted-checks/0.1 (+https://github.com/example/selfhosted-checks)" }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(json) }
        engine {
            requestTimeout = 15_000
        }
    }
}
