@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.js.ExperimentalWasmJsInterop
import io.github.rajumark.hoverfly.hideout.Hideout

// Website live demo: docs/demo/worker.js calls load() once, then run() per input; run() returns JSON.

private fun q(s: String) = buildString {
    append('"')
    for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\")
        else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
    }
    append('"')
}

private var instance: Hideout? = null

private fun model(): Hideout = instance ?: Hideout().also { instance = it }

/** Loads the bundled model and warms it up. */
@JsExport
fun load() {
    model()
}

/** The personal information found, in text order: [{type, start, end, score}] (UTF-16 offsets, as in JS). */
@JsExport
fun run(input: String, option: String): String =
    model().find(input).joinToString(",", "[", "]") {
        "{\"type\":${q(it.type.name)},\"start\":${it.start},\"end\":${it.end},\"score\":${it.score}}"
    }

fun main() {}
