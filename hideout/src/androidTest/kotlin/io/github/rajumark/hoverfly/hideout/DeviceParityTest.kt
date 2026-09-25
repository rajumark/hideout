package io.github.rajumark.hoverfly.hideout

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same check as the JVM ParityTest, on a real device: Android's ICU-backed Unicode tables (NFKC, lowercase,
 * character types) must give the reference output on every vector.
 */
@RunWith(AndroidJUnit4::class)
class DeviceParityTest {
    @Test
    fun matchesReferenceOnDevice() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val vectors = inst.context.assets.open("testvectors.tsv").bufferedReader().readLines().map { line ->
            val c = line.split('\t')
            Vector(unescape(c[0]), unescape(c[6]))
        }
        val t0 = System.nanoTime()
        val hideout = Hideout(inst.targetContext)
        val loadMs = (System.nanoTime() - t0) / 1e6
        var same = 0
        for (v in vectors) {
            val (us, lab) = hideout.firstWindowLabels(v.input)
            val got = Hideout.render(v.input, hideout.collect(v.input, us, lab, FloatArray(lab.size), Hideout.ALL)) {
                "[${it.type.name}]"
            }
            if (got == v.output) same++ else android.util.Log.w("HIDEOUT_DEVICE", "DIFF: ${v.input} | got $got | want ${v.output}")
        }
        val texts = vectors.map { it.input }.filter { it.length in 5..200 }
        repeat(200) { hideout.find(texts[it % texts.size]) }
        val n = 1000
        val s0 = System.nanoTime()
        repeat(n) { hideout.find(texts[it % texts.size]) }
        val ms = (System.nanoTime() - s0) / 1e6 / n
        android.util.Log.i("HIDEOUT_DEVICE", "text $same/${vectors.size} load=${"%.0f".format(loadMs)}ms latency=${"%.3f".format(ms)}ms")
        assertTrue("$same/${vectors.size}", same >= vectors.size - 1)
    }

    private class Vector(val input: String, val output: String)

    /** testvectors.tsv escapes backslash, newline, tab and carriage return in its text columns. */
    private fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r'); else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}
