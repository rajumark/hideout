package io.github.rajumark.hoverfly.hideout.internal

internal actual class UnitCache actual constructor() {
    private val lru = Lru()

    actual fun get(key: String): IntArray? = synchronized(lru) { lru.get(key) }

    actual fun put(key: String, value: IntArray) = synchronized(lru) { lru.put(key, value) }
}
