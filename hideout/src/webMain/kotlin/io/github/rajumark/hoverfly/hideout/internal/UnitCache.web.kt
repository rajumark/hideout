package io.github.rajumark.hoverfly.hideout.internal

// JavaScript and WebAssembly run the library on one thread, so no lock is needed.
internal actual class UnitCache actual constructor() {
    private val lru = Lru()

    actual fun get(key: String): IntArray? = lru.get(key)

    actual fun put(key: String, value: IntArray) = lru.put(key, value)
}
