package io.github.rajumark.hoverfly.hideout.internal

/**
 * Units -> model inputs. Each unit is normalised (NFKC + lowercase) and encoded with SentencePiece on its own;
 * every token also carries its unit's hashed id and shape id. Must produce exactly the reference ids (ParityTest).
 */
internal class Featurizer(private val sp: SentencePiece) {

    /** tok / unit / shape ids per token, and the index (into the given units) of the unit each token belongs to. */
    class Features(val tokIds: IntArray, val unitIds: IntArray, val shapeIds: IntArray, val owner: IntArray)

    private val cache = object : LinkedHashMap<String, IntArray>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, IntArray>?) = size > 4096
    }

    fun pieces(unit: String): IntArray {
        val n = Text.norm(unit)
        synchronized(cache) { cache[n]?.let { return it } }
        val ids = sp.encode(n)
        synchronized(cache) { cache[n] = ids }
        return ids
    }

    fun featurize(text: String, units: List<Text.Unit>, maxTokens: Int = MAX_TOKENS): Features {
        val tok = ArrayList<Int>()
        val uid = ArrayList<Int>()
        val sid = ArrayList<Int>()
        val own = ArrayList<Int>()
        outer@ for ((k, un) in units.withIndex()) {
            val u = text.substring(un.start, un.end)
            val shp = Text.shape(u, un.spaceBefore)
            val uh = unitHash(u, shp)
            val sh = shapeHash(shp)
            for (t in pieces(u)) {
                if (tok.size >= maxTokens) break@outer
                tok.add(t); uid.add(uh); sid.add(sh); own.add(k)
            }
        }
        return Features(tok.toIntArray(), uid.toIntArray(), sid.toIntArray(), own.toIntArray())
    }

    /** Number of tokens of one unit (at least 1), for cutting long text into windows. */
    fun tokenCount(unit: String): Int = pieces(unit).size

    companion object {
        const val MAX_TOKENS = 128
        const val N_BUCKETS = 1 shl 15
        const val N_SHAPES = 1 shl 11

        fun fnv1a(s: String): Long {
            var h = 0x811C9DC5L
            for (b in s.toByteArray(Charsets.UTF_8)) {
                h = h xor (b.toLong() and 0xFF)
                h = (h * 0x01000193L) and 0xFFFFFFFFL
            }
            return h
        }

        fun unitHash(u: String, shape: String): Int {
            val key = if (Text.isLetterUnit(u)) "u:" + Text.norm(u) else "s:$shape"
            return (fnv1a(key) % (N_BUCKETS - 1) + 1).toInt()
        }

        fun shapeHash(shape: String): Int = (fnv1a("s:$shape") % (N_SHAPES - 1) + 1).toInt()
    }
}
