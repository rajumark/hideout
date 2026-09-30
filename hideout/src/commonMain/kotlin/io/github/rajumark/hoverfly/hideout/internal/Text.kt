package io.github.rajumark.hoverfly.hideout.internal

/**
 * Cutting text into units and describing their shape. Must behave exactly like the reference implementation
 * (hideout/text.py and hideout/features.py); ParityTest checks the units and ids on every test vector.
 *
 * Strings are walked by code point; offsets returned to callers are UTF-16 indices into the original string.
 * Whitespace follows Python's str.isspace(). Character classes come from [UnicodeTables] (generated from the JDK),
 * so every platform classifies text exactly like the 1.x java.lang.Character calls.
 */
internal object Text {

    /** A unit: [start, end) in UTF-16 chars, plus whether whitespace (or the start of the text) comes before it. */
    class Unit(val start: Int, val end: Int, val spaceBefore: Boolean)

    fun isPySpace(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.SPACE, cp)

    private fun isLetterOrMark(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.LETTER_OR_MARK, cp)

    private fun isMark(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.MARK, cp)

    private fun isDigit(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.DIGIT, cp)

    private fun isUpper(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.UPPER, cp)

    /** ' ' whitespace, 'd' decimal digit, 'a' letter or combining mark, 's' anything else. */
    private fun kind(cp: Int): Char = when {
        isPySpace(cp) -> ' '
        isDigit(cp) -> 'd'
        isLetterOrMark(cp) -> 'a'
        else -> 's'
    }

    private fun latin(cp: Int): Boolean = cp < 0x250

    private fun joiner(cp: Int): Boolean = cp == 0x200C || cp == 0x200D

    /**
     * Runs of letters (with their combining marks; zero-width joiners stay inside), runs of digits, single symbols.
     * A letter run also breaks where Latin meets another script ("Tadaలో" -> "Tada", "లో").
     */
    fun units(text: String): List<Unit> {
        val out = ArrayList<Unit>()
        var i = 0
        val n = text.length
        var prevSpace = true
        while (i < n) {
            val cp = text.cpAt(i)
            val k = kind(cp)
            if (k == ' ') {
                i += cpLen(cp)
                prevSpace = true
                continue
            }
            var j = i + cpLen(cp)
            if (k == 'd') {
                while (j < n) {
                    val c = text.cpAt(j)
                    if (kind(c) != 'd') break
                    j += cpLen(c)
                }
            } else if (k == 'a') {
                val lat = latin(cp)
                while (j < n) {
                    val c = text.cpAt(j)
                    val ok = joiner(c) || (kind(c) == 'a' && (latin(c) == lat || (isMark(c) && !lat)))
                    if (!ok) break
                    j += cpLen(c)
                }
            }
            out.add(Unit(i, j, prevSpace))
            prevSpace = false
            i = j
        }
        return out
    }

    fun norm(u: String): String = nfkc(u).lowercase()

    private fun script(cp: Int): String = when {
        cp < 0x250 -> "latin"
        cp in 0x0900..0x097F -> "deva"
        cp in 0x0980..0x0DFF -> "indic"
        cp in 0x0600..0x06FF || cp in 0x0750..0x077F -> "arabic"
        cp in 0x0400..0x04FF -> "cyrl"
        cp in 0x3040..0x30FF || cp in 0x3400..0x9FFF || cp in 0xAC00..0xD7AF -> "cjk"
        else -> "other"
    }

    private fun lenBucket(n: Int): String = when {
        n == 1 -> "1"; n == 2 -> "2"; n == 3 -> "3"; n <= 5 -> "4-5"; n <= 8 -> "6-8"; else -> "9+"
    }

    private fun cpString(cp: Int): String = cpToString(cp)

    /** What a unit looks like: "d5 _" (5 digits after a space), "A:latin:4-5 ." (all caps, no space before), "@ .". */
    fun shape(u: String, spaceBefore: Boolean): String {
        val sp = if (spaceBefore) "_" else "."
        val c = u.cpAt(0)
        val nCp = u.cpCount()
        if (isDigit(c)) return "d${minOf(nCp, 20)} $sp"
        if (isLetterOrMark(c)) {
            val cased = ArrayList<Int>()
            var i = 0
            while (i < u.length) {
                val cp = u.cpAt(i)
                val s = cpString(cp)
                if (s.lowercase() != s.uppercase()) cased.add(cp)
                i += cpLen(cp)
            }
            val case = when {
                cased.isEmpty() -> "-"
                cased.all { isUpper(it) } -> if (cased.size > 1) "A" else "Aa"
                isUpper(cased[0]) -> "Aa"
                cased.any { isUpper(it) } -> "aA"
                else -> "a"
            }
            return "$case:${script(c)}:${lenBucket(nCp)} $sp"
        }
        return "${if (c < 0x3000) cpString(c) else "sym"} $sp"
    }

    fun isLetterUnit(u: String): Boolean = isLetterOrMark(u.cpAt(0))
}
