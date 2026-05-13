# selfhosted-checks

Minimal Kotlin CLI that runs a fixed battery of health/conformance checks
against a Mastodon instance and emits JUnit XML.

V0.1 — Mastodon only. Multi-service support, plugins, dashboards, and
persistence are explicitly out of scope (planned for V1.0).

## Checks

| Name                          | What it verifies                                                  |
|-------------------------------|-------------------------------------------------------------------|
| `http_reachability_and_tls`   | Root URL is reachable over HTTPS with a valid TLS handshake       |
| `instance_api_v2`             | `GET /api/v2/instance` returns 200 + JSON with `domain/title/version` |
| `webfinger`                   | `/.well-known/webfinger?resource=acct:…` responds (200 JRD or 404) |
| `nodeinfo_2_0`                | NodeInfo discovery + `/nodeinfo/2.0` schema conformance           |
| `federation_peers`            | `GET /api/v1/instance/peers` returns a JSON array                 |
| `rate_limit_headers`          | `X-RateLimit-Limit` / `X-RateLimit-Remaining` are exposed         |

## Usage

```sh
selfhosted-checks <instance-url> [--out <path>]
```

- `<instance-url>` — full root URL (e.g. `https://mastodon.social`). Must be HTTPS.
- `--out <path>` — write JUnit XML to a file instead of stdout.

Exit codes: `0` all passed, `1` one or more failed/errored, `2` bad arguments.

Per-check progress lines are written to stderr; JUnit XML goes to stdout (unless `--out` is set).

## Run with Docker

Build:

```sh
docker build -t vouch:dev .
```

Run:

```sh
docker run --rm vouch:dev https://mastodon.social
```

Capture the XML report locally:

```sh
docker run --rm vouch:dev https://mastodon.social > results.xml
```

Or with `--out` and a bind mount:

```sh
docker run --rm -v "$PWD:/out" vouch:dev https://mastodon.social --out /out/results.xml
```

## Build & run locally (no Docker)

Requires JDK 17+.

```sh
./gradlew shadowJar
java -jar build/libs/selfhosted-checks.jar https://mastodon.social
```

Or via the Gradle `application` plugin:

```sh
./gradlew run --args="https://mastodon.social --out results.xml"
```

## Example output

Stderr (human-readable progress):

```
[PASS] http_reachability_and_tls (0.42s) reachable, status 200, TLS OK
[PASS] instance_api_v2 (0.61s) domain=mastodon.social version=4.3.1
[PASS] webfinger (0.50s) endpoint live (404 for unknown account)
[PASS] nodeinfo_2_0 (0.71s) nodeinfo 2.0 OK, software=mastodon
[PASS] federation_peers (0.80s) peers array returned, size=18432
[FAIL] rate_limit_headers (0.36s) no X-RateLimit-* headers exposed on /api/v1/instance
```

Stdout (JUnit XML) — see [`example-output.xml`](example-output.xml).

## Project layout

```
src/main/kotlin/vouch/
├── Main.kt                 # CLI entry point + runner
├── Check.kt                # Check interface + CheckResult sealed type
├── HttpClientFactory.kt    # Shared Ktor CIO client config
├── JUnitXml.kt             # JUnit XML serializer
└── checks/
    ├── HttpReachabilityCheck.kt
    ├── InstanceApiCheck.kt
    ├── WebfingerCheck.kt
    ├── NodeInfoCheck.kt
    ├── PeersCheck.kt
    └── RateLimitHeadersCheck.kt
```

## Adding a check

Implement `vouch.Check` (one class per file, in `vouch.checks`), then add it to the
`checks` list in [`Main.kt`](src/main/kotlin/vouch/Main.kt). The interface is:

```kotlin
interface Check {
    val name: String
    suspend fun run(target: URL): CheckResult
}
```

`CheckResult` is `Pass(message)`, `Fail(message)`, or `Error(message, throwable?)`.

## Stack

- Kotlin 2.1, JVM 17
- Ktor 3.0 client (CIO engine)
- kotlinx.serialization for JSON
- Gradle Kotlin DSL + Shadow plugin for the fat JAR
- Multi-stage Docker build, Alpine + Temurin JRE 17
