package vouch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JUnitXmlTest {

    @Test
    fun `empty suite renders valid skeleton`() {
        val xml = JUnitXml.render("suite", emptyList(), 0.0)
        assertTrue(xml.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertTrue(xml.contains("""<testsuite name="suite" tests="0" failures="0" errors="0" time="0.000">"""))
        assertTrue(xml.trimEnd().endsWith("</testsuites>"))
    }

    @Test
    fun `pass with message embeds system-out`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(JUnitXml.CaseReport("ok", CheckResult.Pass("all good"), 0.1)),
            0.1,
        )
        assertTrue(xml.contains("<system-out>all good</system-out>"), "pass message should be in system-out: $xml")
    }

    @Test
    fun `pass without message is self-closed`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(JUnitXml.CaseReport("silent", CheckResult.Pass(""), 0.1)),
            0.1,
        )
        assertTrue(xml.contains("""<testcase classname="suite" name="silent" time="0.100"/>"""), xml)
    }

    @Test
    fun `failure produces failure element and increments counter`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(JUnitXml.CaseReport("bad", CheckResult.Fail("boom"), 0.5)),
            0.5,
        )
        assertTrue(xml.contains("""failures="1""""), xml)
        assertTrue(xml.contains("""<failure message="boom" type="CheckFailure">boom</failure>"""), xml)
    }

    @Test
    fun `error produces error element with throwable class`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(JUnitXml.CaseReport("crash", CheckResult.Error("oops", IllegalStateException("bad state")), 0.5)),
            0.5,
        )
        assertTrue(xml.contains("""errors="1""""), xml)
        assertTrue(xml.contains("""type="java.lang.IllegalStateException""""), xml)
    }

    @Test
    fun `xml special characters are escaped in messages`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(JUnitXml.CaseReport("xss", CheckResult.Fail("<script>&\"'</script>"), 0.0)),
            0.0,
        )
        assertTrue(xml.contains("&lt;script&gt;&amp;&quot;&apos;&lt;/script&gt;"), xml)
        assertTrue(!xml.contains("<script>"), "raw < should not appear inside payload")
    }

    @Test
    fun `mixed results produce correct counters`() {
        val xml = JUnitXml.render(
            "suite",
            listOf(
                JUnitXml.CaseReport("a", CheckResult.Pass("ok"), 0.1),
                JUnitXml.CaseReport("b", CheckResult.Fail("nope"), 0.2),
                JUnitXml.CaseReport("c", CheckResult.Error("kaboom"), 0.3),
                JUnitXml.CaseReport("d", CheckResult.Pass(""), 0.05),
            ),
            0.65,
        )
        assertTrue(xml.contains("""tests="4""""), xml)
        assertTrue(xml.contains("""failures="1""""), xml)
        assertTrue(xml.contains("""errors="1""""), xml)
        assertEquals(1, Regex("""<failure """).findAll(xml).count())
        assertEquals(1, Regex("""<error """).findAll(xml).count())
    }
}
