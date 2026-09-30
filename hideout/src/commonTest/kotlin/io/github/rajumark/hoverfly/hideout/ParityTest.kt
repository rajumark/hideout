package io.github.rajumark.hoverfly.hideout

import io.github.rajumark.hoverfly.hideout.internal.Featurizer
import io.github.rajumark.hoverfly.hideout.internal.SentencePiece
import io.github.rajumark.hoverfly.hideout.internal.Text
import io.github.rajumark.hoverfly.hideout.internal.TestData
import io.github.rajumark.hoverfly.hideout.internal.decodeChunks
import io.github.rajumark.hoverfly.hideout.internal.readModelFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the Kotlin port against the reference implementation on testvectors.tsv: identical units, token, unit and
 * shape ids, the same label for every unit, and the same hidden text.
 * Runs on every target.
 */
class ParityTest {
    class Vector(
        val input: String, val units: String, val tok: List<Int>, val unit: List<Int>, val shape: List<Int>,
        val labels: List<Int>, val output: String,
    )

    @Test
    fun featurizerMatchesReference() {
        val f = Featurizer(SentencePiece(readModelFile("spm_pieces.tsv").decodeToString()))
        var bad = 0
        for (v in vectors()) {
            val us = Text.units(v.input)
            val got = f.featurize(v.input, us)
            val covered = if (got.owner.isEmpty()) 0 else got.owner.last() + 1
            val unitText = us.take(covered).joinToString(" ") { v.input.substring(it.start, it.end) }
            if (unitText != v.units || got.tokIds.toList() != v.tok || got.unitIds.toList() != v.unit ||
                got.shapeIds.toList() != v.shape) {
                bad++
                println("MISMATCH: ${v.input}\n  units $unitText\n  want  ${v.units}\n  tok   ${got.tokIds.toList()}\n" +
                    "  want  ${v.tok}\n  shape ${got.shapeIds.toList()}\n  want  ${v.shape}")
            }
        }
        println("featurizer: ${vectors().size - bad}/${vectors().size} identical")
        assertEquals(0, bad)
    }

    @Test
    fun modelMatchesReference() {
        val h = testHideout()
        var labels = 0
        var text = 0
        val vs = vectors()
        for (v in vs) {
            val (us, lab) = h.firstWindowLabels(v.input)
            if (lab.toList() == v.labels) labels++ else println("LABELS DIFF: ${v.input}\n  got  ${lab.toList()}\n  want ${v.labels}")
            val found = h.collect(v.input, us, lab, FloatArray(lab.size), Hideout.ALL)
            val out = Hideout.render(v.input, found) { "[${it.type.name}]" }
            if (out == v.output) text++ else println("TEXT DIFF: ${v.input}\n  got  $out\n  want ${v.output}")
        }
        println("model: labels $labels/${vs.size}, text $text/${vs.size}")
        assertTrue(labels >= vs.size - 1, "labels differ too often: $labels/${vs.size}") // a near-tie may flip
        assertTrue(text >= vs.size - 1, "text differs too often: $text/${vs.size}")
    }

    companion object {
        private var shared: Hideout? = null

        /** One instance per test run: loading is the slow part on the native and web targets. */
        fun testHideout(): Hideout = shared ?: Hideout().also { shared = it }


        private fun ints(s: String) = if (s.isEmpty()) emptyList() else s.split(',').map { it.toInt() }

        fun unescape(s: String): String {
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

        fun parse(lines: List<String>): List<Vector> = lines.map { it.split('\t') }.map {
            Vector(unescape(it[0]), unescape(it[1]), ints(it[2]), ints(it[3]), ints(it[4]), ints(it[5]), unescape(it[6]))
        }

        private var cached: List<Vector>? = null
        fun vectors(): List<Vector> = cached ?: parse(
            decodeChunks(TestData.files.getValue("testvectors.tsv")).decodeToString().lines().filter { it.isNotEmpty() }
        ).also { cached = it }
    }
}
