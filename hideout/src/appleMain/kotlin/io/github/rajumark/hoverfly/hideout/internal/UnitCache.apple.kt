package io.github.rajumark.hoverfly.hideout.internal

import platform.Foundation.NSLock

internal actual class UnitCache actual constructor() {
    private val lru = Lru()
    private val lock = NSLock()

    actual fun get(key: String): IntArray? {
        lock.lock()
        try { return lru.get(key) } finally { lock.unlock() }
    }

    actual fun put(key: String, value: IntArray) {
        lock.lock()
        try { lru.put(key, value) } finally { lock.unlock() }
    }
}
