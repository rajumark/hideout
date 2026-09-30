package io.github.rajumark.hoverfly.hideout.internal

/**
 * A small LRU cache of SentencePiece ids per normalised unit (at most [UnitCache.MAX] entries), safe to use from
 * several threads. Platform code, because locking differs: synchronized on the JVM and Android, NSLock on Apple;
 * the web is single-threaded.
 */
internal expect class UnitCache() {
    fun get(key: String): IntArray?

    fun put(key: String, value: IntArray)
}

internal const val UNIT_CACHE_MAX = 4096

/** Access-ordered LRU on a plain map (common code has no access-ordered LinkedHashMap). Not thread-safe by itself. */
internal class Lru {
    private val map = LinkedHashMap<String, IntArray>()

    fun get(key: String): IntArray? {
        val v = map.remove(key) ?: return null
        map[key] = v // move to the end (most recently used)
        return v
    }

    fun put(key: String, value: IntArray) {
        map.remove(key)
        map[key] = value
        if (map.size > UNIT_CACHE_MAX) map.remove(map.keys.first())
    }
}
