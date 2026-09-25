package io.github.rajumark.hoverfly.hideout.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.hideout.Hideout
import io.github.rajumark.hoverfly.hideout.Pii
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { HideoutTheme { HideoutScreen() } }
    }
}

private val EXAMPLES = listOf(
    "Call me on 98765 43210, my UPI is raju@okaxis",
    "Mera naam Pooja Gupta hai, aadhar 4521 8736 1292",
    "Account no 50100234567812, IFSC HDFC0001234",
    "Deliver to Flat 302, Sai Residency, Baner Road, Pune 411045",
    "मेरा नंबर 98765 43210 है",
    "Order #40512378 will arrive by Friday, costs Rs 24,999",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val hidden: String, val found: List<Pii>, val micros: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HideoutScreen() {
    val context = LocalContext.current.applicationContext

    // Loading reads the model: do it once, off the main thread.
    val hideout by produceState<Hideout?>(null) {
        value = withContext(Dispatchers.Default) { Hideout(context) }
        awaitDispose { value?.close() }
    }
    var input by remember { mutableStateOf(EXAMPLES[0]) }

    val result by produceState<Result?>(null, hideout, input) {
        val h = hideout ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val found = h.find(input)
            val us = (System.nanoTime() - t0) / 1000
            val hidden = h.hide(input)
            Result(hidden, found, us)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Hideout") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message") },
                minLines = 3,
                trailingIcon = {
                    if (input.isNotEmpty()) IconButton(onClick = { input = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                },
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
                            Text("%.2f".format(p.score), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "${r.micros} µs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Try", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXAMPLES.forEach { SuggestionChip(onClick = { input = it }, label = { Text(it, maxLines = 1) }) }
            }
            Text(
                "Runs on this device. No network, no permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HideoutTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
