package vouch

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import vouch.checks.HttpReachabilityCheck
import vouch.checks.InstanceApiCheck
import vouch.checks.NodeInfoCheck
import vouch.checks.PeersCheck
import vouch.checks.RateLimitHeadersCheck
import vouch.checks.WebfingerCheck
import java.io.File
import java.net.URI
import java.net.URL
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

private const val SUITE_NAME = "selfhosted-checks.mastodon"

fun main(args: Array<String>): Unit = runBlocking {
    if (args.isEmpty() || args[0] in listOf("-h", "--help")) {
        System.err.println(
            """
            selfhosted-checks — Mastodon instance health checker

            Usage: selfhosted-checks <instance-url> [--out <path>]

            Arguments:
              <instance-url>   Root URL of the Mastodon instance (e.g. https://mastodon.social)

            Options:
              --out <path>     Write JUnit XML to <path> instead of stdout

            Exit codes:
              0  all checks passed
              1  one or more checks failed or errored
              2  invalid arguments
            """.trimIndent()
        )
        exitProcess(if (args.isEmpty()) 2 else 0)
    }

    val rawUrl = args[0]
    val outIndex = args.indexOf("--out")
    val outFile: File? = if (outIndex >= 0 && outIndex + 1 < args.size) File(args[outIndex + 1]) else null

    val target: URL = try {
        URI(rawUrl).toURL()
    } catch (e: Exception) {
        System.err.println("Invalid URL: $rawUrl (${e.message})")
        exitProcess(2)
    }

    val client: HttpClient = HttpClientFactory.create()
    val checks: List<Check> = listOf(
        HttpReachabilityCheck(),
        InstanceApiCheck(client),
        WebfingerCheck(client),
        NodeInfoCheck(client),
        PeersCheck(client),
        RateLimitHeadersCheck(client),
    )

    val suiteStart = System.nanoTime()
    val reports = mutableListOf<JUnitXml.CaseReport>()
    for (check in checks) {
        val t0 = System.nanoTime()
        val result = try {
            check.run(target)
        } catch (e: Exception) {
            CheckResult.Error("uncaught exception: ${e.message}", e)
        }
        val elapsed = (System.nanoTime() - t0).nanoseconds.toDouble(DurationUnit.SECONDS)
        reports += JUnitXml.CaseReport(check.name, result, elapsed)
        val tag = when (result) {
            is CheckResult.Pass -> "PASS"
            is CheckResult.Fail -> "FAIL"
            is CheckResult.Error -> "ERROR"
        }
        val msg = when (result) {
            is CheckResult.Pass -> result.message
            is CheckResult.Fail -> result.message
            is CheckResult.Error -> result.message
        }
        System.err.println("[$tag] ${check.name} (${"%.2f".format(elapsed)}s) $msg")
    }
    client.close()

    val totalSeconds = (System.nanoTime() - suiteStart).nanoseconds.toDouble(DurationUnit.SECONDS)
    val xml = JUnitXml.render(SUITE_NAME, reports, totalSeconds)

    if (outFile != null) {
        outFile.parentFile?.mkdirs()
        outFile.writeText(xml)
        System.err.println("wrote ${outFile.absolutePath}")
    } else {
        print(xml)
    }

    val ok = reports.all { it.result is CheckResult.Pass }
    exitProcess(if (ok) 0 else 1)
}
