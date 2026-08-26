package com.isai.trader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
                AppScreen()
            }
        }
    }
}

private val PROVIDERS = listOf("openai", "anthropic", "google", "deepseek", "openrouter", "groq", "ollama")

private fun defaultModels(provider: String): Pair<String, String> = when (provider) {
    "openai" -> Pair("gpt-5.5", "gpt-5.4-mini")
    "anthropic" -> Pair("claude-sonnet-5", "claude-haiku-4")
    "google" -> Pair("gemini-2.5-pro", "gemini-2.5-flash")
    "deepseek" -> Pair("deepseek-chat", "deepseek-chat")
    "openrouter" -> Pair("openrouter/auto", "openrouter/auto")
    "groq" -> Pair("llama-3.3-70b-versatile", "llama-3.3-70b-versatile")
    else -> Pair("", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    var ticker by remember { mutableStateOf("NVDA") }
    var date by remember { mutableStateOf("2026-08-25") }
    var provider by remember { mutableStateOf("openai") }
    var providerExpanded by remember { mutableStateOf(false) }
    var deepModel by remember { mutableStateOf("gpt-5.5") }
    var quickModel by remember { mutableStateOf("gpt-5.4-mini") }
    var apiKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Enter a ticker and your API key.") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("Isai - TradingAgents") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = ticker,
                onValueChange = { ticker = it },
                label = { Text("Ticker (e.g. NVDA, RELIANCE.NS, BTC-USD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Analysis date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it }
            ) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("LLM provider") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    PROVIDERS.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = {
                                provider = p
                                val (d, q) = defaultModels(p)
                                deepModel = d
                                quickModel = q
                                providerExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = deepModel,
                onValueChange = { deepModel = it },
                label = { Text("Deep think model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quickModel,
                onValueChange = { quickModel = it },
                label = { Text("Quick think model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key (stays on device)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    busy = true
                    status = "Running agent team... this can take several minutes."
                    result = ""
                    scope.launch {
                        val output = withContext(Dispatchers.IO) {
                            try {
                                val py = Python.getInstance()
                                py.getModule("trading_bridge")
                                    .callAttr(
                                        "analyze",
                                        ticker.trim().uppercase(),
                                        date.trim(),
                                        provider,
                                        deepModel.trim(),
                                        quickModel.trim(),
                                        apiKey.trim(),
                                        context.filesDir.absolutePath
                                    )
                                    .toString()
                            } catch (e: Exception) {
                                "Python call failed:\n${e.message}"
                            }
                        }
                        result = output
                        status = if (output.contains("\"error\"")) "Finished with errors." else "Done."
                        busy = false
                    }
                },
                enabled = !busy && ticker.isNotBlank() && date.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Analyzing..." else "Analyze")
            }
            if (busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                }
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
            if (result.isNotEmpty()) {
                Text(
                    result,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
            }
        }
    }
}
