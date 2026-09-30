package io.github.rajumark.hoverfly.hideout.internal

import io.github.rajumark.hoverfly.hideout.Hideout
import java.text.Normalizer

internal actual fun nfkc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)

// The model ships as Java resources in the jar/AAR (src/modelData), so no Context or copy is needed.
internal actual fun readModelFile(name: String): ByteArray =
    Hideout::class.java.getResourceAsStream("/io/github/rajumark/hoverfly/hideout/model/$name")?.use { it.readBytes() }
        ?: error("Hideout model file $name is missing from the library jar")
