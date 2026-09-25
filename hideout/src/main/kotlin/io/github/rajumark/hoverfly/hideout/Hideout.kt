package io.github.rajumark.hoverfly.hideout

import android.content.Context
import io.github.rajumark.hoverfly.hideout.internal.Featurizer
import io.github.rajumark.hoverfly.hideout.internal.Network
import io.github.rajumark.hoverfly.hideout.internal.SentencePiece
import io.github.rajumark.hoverfly.hideout.internal.Text
import java.io.Closeable
import java.io.InputStream

/**
 * On-device personal information detector: finds phone numbers, UPI IDs, Aadhaar, PAN, card and bank numbers,
 * emails, names, addresses and more in a text, and hides them.
 *
 * ```
 * Hideout(context).use { hideout ->
 *     hideout.hide("Call me on 98765 43210, my UPI is raju@okaxis")
 *     // "Call me on [PHONE], my UPI is [UPI]"
 *     hideout.find("Mera naam Pooja hai")
 *     // [Pii(type=NAME, start=10, end=15, text=Pooja, score=0.98)]
 * }
 * ```
 *
 * Built for Indian apps first (English, Hinglish, Hindi and other Indian languages), plus European languages.
 *
 * Everything runs locally: the model ships inside the library, there is no network, no permission and no
 * dependency. Creating an instance reads the model (tens of ms), so create it off the main thread and keep it
 * around; [find] and [hide] take a few milliseconds for a chat message and are safe to call from several threads.
 */
public class Hideout internal constructor(open: (String) -> InputStream) : Closeable {

    /** Loads the model bundled in the library's assets. */
    public constructor(context: Context) : this({ name -> context.assets.open("$ASSET_DIR/$name") })

    private var network: Network? = open("hideout.bin").use { Network(it) }
    private val featurizer = Featurizer(open("spm_pieces.tsv").use { SentencePiece(it) })

    init {
        // The first calls run interpreted; pay that here (off the UI thread) instead of on the first message.
        repeat(WARM_UP) { find("warm up $it: call Rahul on 98765 43210 or mail rahul@gmail.com") }
    }

    /**
     * Finds the personal information in [text].
     *
     * @param types the kinds to look for; everything by default.
     * @param threshold how sure the model must be, 0..1. Lower it to hide more (fewer leaks, more false alarms),
     *   raise it to hide less. The default 0.5 suits chat messages.
     * @return the found pieces, in text order, not overlapping.
     */
    @JvmOverloads
    public fun find(text: String, types: Set<PiiType> = ALL, threshold: Float = DEFAULT_THRESHOLD): List<Pii> {
        val units = Text.units(text)
        if (units.isEmpty()) return emptyList()
        val labels = IntArray(units.size)
        val pii = FloatArray(units.size)
        unitLabels(text, units, threshold, labels, pii)
        return collect(text, units, labels, pii, types)
    }

    /**
     * Returns [text] with every piece of personal information replaced, by default with its type in brackets:
     * "Call me on [PHONE]". Use [replacement] for another style, for example `{ "*".repeat(it.text.length) }`.
     */
    @JvmOverloads
    public fun hide(
        text: String,
        types: Set<PiiType> = ALL,
        threshold: Float = DEFAULT_THRESHOLD,
        replacement: (Pii) -> String = { "[${it.type.name}]" },
    ): String {
        return render(text, find(text, types, threshold), replacement)
    }

    /** True if [text] contains personal information of the given [types]. */
    @JvmOverloads
    public fun contains(text: String, types: Set<PiiType> = ALL, threshold: Float = DEFAULT_THRESHOLD): Boolean =
        find(text, types, threshold).isNotEmpty()

    /** Releases the model (about 30 MB of heap). The instance cannot be used afterwards. */
    override fun close() {
        network = null
    }

    /**
     * Per-unit BIO label (0 = O, 1 + 2*type = B, 2 + 2*type = I) and P(personal info). A unit is PII when
     * P(not O) >= threshold; it then takes its most likely PII label. Long text is cut into overlapping windows;
     * each unit takes the prediction of the window where it has the most context on both sides.
     */
    internal fun unitLabels(text: String, units: List<Text.Unit>, threshold: Float, labels: IntArray, pii: FloatArray,
                            oneWindow: Boolean = false) {
        val net = checkNotNull(network) { "Hideout is closed" }
        val nl = net.nLabels
        val best = IntArray(units.size) { -1 }
        val ws = if (oneWindow) listOf(0 to units.size) else windows(text, units, net.maxTokens)
        for ((s, e) in ws) {
            val f = featurizer.featurize(text, units.subList(s, e), net.maxTokens)
            val p = net.probs(f.tokIds, f.unitIds, f.shapeIds)
            val n = e - s
            val owner = f.owner
            for (i in owner.indices) {
                if (i > 0 && owner[i - 1] == owner[i]) continue
                val wk = owner[i]
                val g = s + wk
                val centre = minOf(wk + 1, n - wk)
                if (centre <= best[g]) continue
                best[g] = centre
                val o = i * nl
                pii[g] = 1f - p[o]
                if (pii[g] >= threshold) {
                    var b = 1
                    for (c in 2 until nl) if (p[o + c] > p[o + b]) b = c
                    labels[g] = b
                } else {
                    labels[g] = 0
                }
            }
        }
    }

    /**
     * BIO labels per unit -> found pieces (an I- with no B- before it starts a span, like the reference). One address
     * predicted as pieces split at a comma is joined back: ADDRESS spans whose gap is only spaces and separators.
     */
    internal fun collect(text: String, units: List<Text.Unit>, labels: IntArray, pii: FloatArray,
                         types: Set<PiiType>): List<Pii> {
        val raw = ArrayList<IntArray>()  // [first unit, end unit, type]
        var k = 0
        val n = labels.size
        while (k < n) {
            if (labels[k] == 0) { k++; continue }
            val type = (labels[k] - 1) / 2
            var e = k + 1
            // I- of the same type continues the span; B- or another type starts a new one
            while (e < n && labels[e] != 0 && (labels[e] - 1) % 2 == 1 && (labels[e] - 1) / 2 == type) e++
            val prev = raw.lastOrNull()
            if (prev != null && type == ADDRESS && prev[2] == ADDRESS &&
                text.substring(units[prev[1] - 1].end, units[k].start).all { it in JOIN }) {
                prev[1] = e
            } else {
                raw.add(intArrayOf(k, e, type))
            }
            k = e
        }
        val out = ArrayList<Pii>(raw.size)
        for ((a, e, type) in raw) {
            val t = PiiType.entries[type]
            if (t !in types) continue
            var sc = 0f
            for (i in a until e) sc += pii[i]
            val s = units[a].start
            val en = units[e - 1].end
            out.add(Pii(t, s, en, text.substring(s, en), sc / (e - a)))
        }
        return out
    }

    /** Labels of the units covered by the first window only (how the reference test vectors are made). */
    internal fun firstWindowLabels(text: String, threshold: Float = DEFAULT_THRESHOLD): Pair<List<Text.Unit>, IntArray> {
        val net = checkNotNull(network) { "Hideout is closed" }
        val units = Text.units(text)
        val f = featurizer.featurize(text, units, net.maxTokens)
        val covered = if (f.owner.isEmpty()) 0 else f.owner.last() + 1
        val labels = IntArray(covered)
        val pii = FloatArray(covered)
        unitLabels(text, units.subList(0, covered), threshold, labels, pii, oneWindow = true)
        return units.subList(0, covered) to labels
    }

    private fun windows(text: String, units: List<Text.Unit>, maxTokens: Int): List<Pair<Int, Int>> {
        val nTok = IntArray(units.size) { featurizer.tokenCount(text.substring(units[it].start, units[it].end)) }
        val out = ArrayList<Pair<Int, Int>>()
        var s = 0
        while (s < units.size) {
            var e = s
            var t = 0
            while (e < units.size && t + nTok[e] <= maxTokens) { t += nTok[e]; e++ }
            e = maxOf(e, s + 1)
            out.add(s to e)
            if (e >= units.size) break
            s = maxOf(e - OVERLAP, s + 1)
        }
        return out
    }

    public companion object {
        internal fun render(text: String, found: List<Pii>, replacement: (Pii) -> String): String {
            if (found.isEmpty()) return text
            val sb = StringBuilder(text.length)
            var last = 0
            for (p in found) {
                sb.append(text, last, p.start).append(replacement(p))
                last = p.end
            }
            return sb.append(text, last, text.length).toString()
        }

        /** Every [PiiType]. */
        @JvmField
        public val ALL: Set<PiiType> = PiiType.entries.toSet()

        public const val DEFAULT_THRESHOLD: Float = 0.5f

        internal const val ASSET_DIR = "hideout"
        internal const val WARM_UP = 20
        internal const val OVERLAP = 16
        private val ADDRESS = PiiType.ADDRESS.ordinal
        private const val JOIN = " \t,-–/|"
    }
}
