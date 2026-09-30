package io.github.rajumark.hoverfly.hideout

import io.github.rajumark.hoverfly.hideout.internal.Text
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.TimeSource
import kotlin.test.assertFailsWith

class HideoutTest {
    private val hideout = ParityTest.testHideout()

    @Test
    fun hidesPhoneAndUpi() {
        val out = hideout.hide("Call me on 98765 43210, my UPI is raju@okaxis")
        println(out)
        assertEquals("Call me on [PHONE], my UPI is [UPI]", out)
    }

    @Test
    fun findGivesExactOffsets() {
        val text = "Mera number 9876543210 hai"
        val found = hideout.find(text)
        assertEquals(1, found.size)
        val p = found[0]
        assertEquals(PiiType.PHONE, p.type)
        assertEquals("9876543210", text.substring(p.start, p.end))
        assertEquals(p.text, text.substring(p.start, p.end))
        assertTrue(p.score in 0.5f..1f)
    }

    @Test
    fun leavesOrdinaryNumbersAlone() {
        for (t in listOf("Meet me at 5:30 near the metro station", "The phone costs Rs 24,999 on Flipkart",
            "Order #40512378 will arrive by Friday")) {
            assertEquals(t, hideout.hide(t))
        }
    }

    @Test
    fun typeFilter() {
        val text = "Call Rahul on 98765 43210"
        val out = hideout.hide(text, types = setOf(PiiType.PHONE))
        assertEquals("Call Rahul on [PHONE]", out)
    }

    @Test
    fun customReplacement() {
        val out = hideout.hide("OTP is 482913", replacement = { "*".repeat(it.text.length) })
        assertEquals("OTP is ******", out)
    }

    @Test
    fun blankAndNoPii() {
        assertEquals("", hideout.hide(""))
        assertEquals("   ", hideout.hide("   "))
        assertTrue(hideout.find("ok bye take care").isEmpty())
        assertFalse(hideout.contains("thanks, see you on Monday"))
    }

    @Test
    fun surrogatePairsKeepOffsets() {
        val text = "😂😂 call 9876543210 😂"
        val p = hideout.find(text).single()
        assertEquals("9876543210", p.text)
    }

    @Test
    fun longTextIsCoveredEntirely() {
        val text = (1..40).joinToString(" ") { "this is line $it of a long note" } + " call me on 9876543210"
        assertTrue(Text.units(text).size > 200)
        val found = hideout.find(text)
        assertEquals(listOf("9876543210"), found.map { it.text })
    }

    @Test
    fun closedInstanceThrows() {
        val h = Hideout()
        h.close()
        assertFailsWith<IllegalStateException> { h.find("hello") }
    }

    @Test
    fun latency() {
        val texts = listOf("Call me on 98765 43210, my UPI is raju@okaxis", "Mera naam Pooja Gupta hai aur main Jaipur se hoon",
            "Account no 50100234567812, IFSC HDFC0001234", "मेरा नंबर 98765 43210 है", "thanks so much see you on monday")
        repeat(300) { hideout.find(texts[it % texts.size]) }
        val n = 2000
        val t0 = TimeSource.Monotonic.markNow()
        repeat(n) { hideout.find(texts[it % texts.size]) }
        val ms = t0.elapsedNow().inWholeNanoseconds / 1e6 / n
        println("latency: ${fmt(ms, 3)} ms per message")
        assertTrue(ms < 200, "too slow: $ms ms") // generous: Kotlin/Native test binaries are unoptimized debug builds
        val l0 = TimeSource.Monotonic.markNow()
        Hideout().close()
        println("load: ${l0.elapsedNow().inWholeMilliseconds} ms")
    }
}
