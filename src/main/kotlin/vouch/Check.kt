package vouch

import java.net.URL

sealed class CheckResult {
    data class Pass(val message: String = "") : CheckResult()
    data class Fail(val message: String) : CheckResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CheckResult()
}

interface Check {
    val name: String
    suspend fun run(target: URL): CheckResult
}
