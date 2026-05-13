# Contributing

Issues and pull requests are welcome. This project is licensed under
[AGPL-3.0-or-later](LICENSE).

## Before submitting a PR

```sh
./gradlew test        # all tests must pass
./gradlew shadowJar   # fat JAR must build
```

Or equivalently via container (no local JDK required):

```sh
podman build -t vouch:dev .
podman run --rm vouch:dev https://mastodon.social
```

## Adding a check

1. Create `src/main/kotlin/vouch/checks/YourCheck.kt` implementing `vouch.Check`.
2. Inject `HttpClient` via constructor so tests can use Ktor's `MockEngine`.
3. Add a matching test in `src/test/kotlin/vouch/checks/YourCheckTest.kt`.
4. Register the check in the `checks` list in `Main.kt`.

## Scope (V0.1)

This release covers Mastodon only. Nextcloud and Matrix modules are planned
for V1.0. Please open an issue before starting work on a new service module
so we can coordinate design.
