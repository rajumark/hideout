package io.github.rajumark.hoverfly.hideout.sample

import io.github.rajumark.hoverfly.hideout.Hideout
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/** "Kotlin/JS" or "Kotlin/Wasm". */
expect val runtime: String

private val examples = listOf(
    "Call me on 98765 43210, my UPI is raju@okaxis",
    "Mera naam Pooja Gupta hai, aadhar 4521 8736 1292",
    "Account no 50100234567812, IFSC HDFC0001234",
    "Deliver to Flat 302, Sai Residency, Baner Road, Pune 411045",
    "मेरा नंबर 98765 43210 है",
    "Order #40512378 will arrive by Friday, costs Rs 24,999",
)

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

fun main() {
    fun el(id: String) = document.getElementById(id) as HTMLElement
    val input = document.getElementById("text") as HTMLTextAreaElement
    el("platform").textContent = "Kotlin Multiplatform · $runtime · io.github.rajumark:hideout:2.0.0"

    val t0 = TimeSource.Monotonic.markNow()
    val hideout = Hideout()
    el("load").textContent = "Model loaded in ${t0.elapsedNow().inWholeMilliseconds} ms"

    fun render() {
        val mark = TimeSource.Monotonic.markNow()
        val found = hideout.find(input.value)
        val micros = mark.elapsedNow().inWholeMicroseconds
        el("out").textContent = hideout.hide(input.value).ifEmpty { "—" }
        el("found").innerHTML = if (found.isEmpty()) "<p class=\"muted\">No personal info found</p>" else found.joinToString("") {
            "<div class=\"pii\"><span class=\"tag\">${it.type.name}</span><code>${esc(it.text)}</code><span class=\"score\">${(it.score * 100).roundToInt()}%</span></div>"
        }
        el("timing").textContent = "$micros µs"
    }

    el("examples").innerHTML = examples.joinToString("") { "<button class=\"chip\">${esc(it)}</button>" }
    val list = el("examples").querySelectorAll("button")
    for (i in 0 until list.length) {
        val b = list.item(i) as HTMLElement
        b.onclick = { input.value = b.textContent ?: ""; render(); null }
    }
    input.oninput = { render(); null }
    input.value = examples[0]
    render()
}
