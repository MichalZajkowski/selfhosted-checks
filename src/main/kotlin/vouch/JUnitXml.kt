package vouch

object JUnitXml {
    data class CaseReport(
        val name: String,
        val result: CheckResult,
        val timeSeconds: Double,
    )

    fun render(suiteName: String, cases: List<CaseReport>, totalTimeSeconds: Double): String {
        val tests = cases.size
        val failures = cases.count { it.result is CheckResult.Fail }
        val errors = cases.count { it.result is CheckResult.Error }
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("<testsuites>\n")
        sb.append(
            """  <testsuite name="${escape(suiteName)}" tests="$tests" failures="$failures" errors="$errors" time="${fmt(totalTimeSeconds)}">"""
        ).append('\n')
        for (case in cases) {
            sb.append(
                """    <testcase classname="${escape(suiteName)}" name="${escape(case.name)}" time="${fmt(case.timeSeconds)}""""
            )
            when (val r = case.result) {
                is CheckResult.Pass -> {
                    if (r.message.isBlank()) {
                        sb.append("/>\n")
                    } else {
                        sb.append(">\n")
                        sb.append("      <system-out>${escape(r.message)}</system-out>\n")
                        sb.append("    </testcase>\n")
                    }
                }
                is CheckResult.Fail -> {
                    sb.append(">\n")
                    sb.append("""      <failure message="${escape(r.message)}" type="CheckFailure">${escape(r.message)}</failure>""").append('\n')
                    sb.append("    </testcase>\n")
                }
                is CheckResult.Error -> {
                    sb.append(">\n")
                    val type = r.throwable?.javaClass?.name ?: "CheckError"
                    val detail = r.throwable?.stackTraceToString() ?: r.message
                    sb.append("""      <error message="${escape(r.message)}" type="${escape(type)}">${escape(detail)}</error>""").append('\n')
                    sb.append("    </testcase>\n")
                }
            }
        }
        sb.append("  </testsuite>\n")
        sb.append("</testsuites>\n")
        return sb.toString()
    }

    private fun fmt(seconds: Double): String = String.format(java.util.Locale.ROOT, "%.3f", seconds)

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
