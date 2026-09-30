package io.github.rajumark.hoverfly.hideout.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.hideout.Hideout
import io.github.rajumark.hoverfly.hideout.Pii
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.TimeSource

private val examples = listOf(
    "Call me on 98765 43210, my UPI is raju@okaxis",
    "Mera naam Pooja Gupta hai, aadhar 4521 8736 1292",
    "Account no 50100234567812, IFSC HDFC0001234",
    "Deliver to Flat 302, Sai Residency, Baner Road, Pune 411045",
    "मेरा नंबर 98765 43210 है",
    "Order #40512378 will arrive by Friday, costs Rs 24,999",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val hidden: String, val found: List<Pii>, val micros: Long)

/** The whole demo: a message in, personal info found and hidden. [platform] is shown so screenshots say where they ran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(platform: String) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Loading reads the model: do it once, off the main thread.
            val hideout by produceState<Hideout?>(null) { value = withContext(Dispatchers.Default) { Hideout() } }
            var input by remember { mutableStateOf(examples[0]) }

            val result by produceState<Result?>(null, hideout, input) {
                val h = hideout ?: return@produceState
                value = withContext(Dispatchers.Default) {
                    val t0 = TimeSource.Monotonic.markNow()
                    val found = h.find(input)
                    val us = t0.elapsedNow().inWholeMicroseconds
                    Result(h.hide(input), found, us)
                }
            }

            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Hideout", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Kotlin Multiplatform · $platform · io.github.rajumark:hideout:$HIDEOUT_VERSION",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    minLines = 3,
                )
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Hidden", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val r = result
                    if (hideout == null || r == null) {
                        Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        Text(r.hidden.ifEmpty { "—" }, style = MaterialTheme.typography.titleLarge)
                        if (r.found.isEmpty()) {
                            Text("No personal info found", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        r.found.forEach { p ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    p.type.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                                Text(p.text, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f))
                                Text("${(p.score * 100).roundToInt() / 100.0}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("${r.micros} µs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Try", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    examples.forEach { SuggestionChip(onClick = { input = it }, label = { Text(it, maxLines = 1) }) }
                }
                Text(
                    "Runs on this device. No network, no permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

const val HIDEOUT_VERSION = "2.0.0"
