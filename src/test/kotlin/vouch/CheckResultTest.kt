package vouch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CheckResultTest {

    @Test
    fun `pass carries message`() {
        val r: CheckResult = CheckResult.Pass("ok")
        assertTrue(r is CheckResult.Pass)
        assertEquals("ok", r.message)
    }

    @Test
    fun `fail and error are distinguishable from pass and each other`() {
        val pass: CheckResult = CheckResult.Pass()
        val fail: CheckResult = CheckResult.Fail("bad")
        val err: CheckResult = CheckResult.Error("worse")

        assertTrue(pass is CheckResult.Pass)
        assertTrue(fail is CheckResult.Fail)
        assertTrue(err is CheckResult.Error)

        assertNotEquals<CheckResult>(pass, fail)
        assertNotEquals<CheckResult>(fail, err)
    }

    @Test
    fun `error preserves throwable`() {
        val cause = RuntimeException("io")
        val r = CheckResult.Error("network failed", cause)
        assertEquals(cause, r.throwable)
    }

    @Test
    fun `default pass message is empty`() {
        assertEquals("", CheckResult.Pass().message)
    }
}
