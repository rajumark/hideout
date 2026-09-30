@file:JvmName("HideoutAndroid")

package io.github.rajumark.hoverfly.hideout

import android.content.Context

/** Kept so 1.x code (`Hideout(context)`) still compiles; the model no longer needs a [Context]. */
@Deprecated("The model is bundled without assets now; use Hideout().", ReplaceWith("Hideout()"))
@Suppress("UNUSED_PARAMETER", "FunctionName")
public fun Hideout(context: Context): Hideout = Hideout()
