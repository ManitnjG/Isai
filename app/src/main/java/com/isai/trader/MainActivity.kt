package com.isai.trader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Bg = Color(0xFF0B0F17)
private val Surface = Color(0xFF121826)
private val SurfaceHi = Color(0xFF1C2436)
private val Outline = Color(0xFF2B3648)
private val Green = Color(0xFF10B981)
private val Red = Color(0xFFEF4444)
private val Amber = Color(0xFFF59E0B)
private val TextMain = Color(0xFFE5EAF2)
private val TextDim = Color(0xFF8B95A7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Green,
                    onPrimary = Color.White,
                    secondary = Amber,
                    background = Bg,
                    onBackground = TextMain,
                    surface = Surface,
                    onSurface = TextMain,
                    surfaceVariant = SurfaceHi,
                    onSurfaceVariant = TextDim,
                    outline = Outline,
                    error = Red
                )
            ) {
                AppScreen()
            }
        }
    }
}

private val PROVIDERS = listOf("openai", "anthropic", "deepseek", "openrouter", "groq", "ollama")
private val QUICK_TICKERS = listOf("NVDA", "AAPL", "TSLA", "RELIANCE.NS", "BTC-USD")

private fun defaultModels(provider: String): Pair<String, String> = when (provider) {
    "openai" -> Pair("gpt-5.5", "gpt-5.4-mini")
    "anthropic" -> Pair("claude-sonnet-5", "claude-haiku-4")
    "deepseek" -> Pair("deepseek-chat", "deepseek-chat")
    "openrouter" -> Pair("openrouter/auto", "openrouter/auto")
    "groq" -> Pair("llama-3.3-70b-versatile", "llama-3.3-70b-versatile")
    else -> Pair("", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    var ticker by remember { mutableStateOf("NVDA") }
    var date by remember { mutableStateOf("2026-08-26") }
    var provider by remember { mutableStateOf("openai") }
    var providerExpanded by remember { mutableStateOf(false) }
    var deepModel by remember { mutableStateOf("gpt-5.5") }
    var quickModel by remember { mutableStateOf("gpt-5.4-mini") }
    var apiKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready.") }
    var busy by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf("") }
    var showFull by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(busy) {
        elapsed = 0
        while (busy) {
            delay(1000)
            elapsed++
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceHi),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Isai Trader", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("TradingAgents · on-device", fontSize = 11.sp, color = TextDim)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetupCard(
                ticker, { ticker = it }, date, { date = it },
                provider, { provider = it }, providerExpanded, { providerExpanded = it },
                deepModel, { deepModel = it }, quickModel, { quickModel = it },
                apiKey, { apiKey = it },
                onProviderChange = { p ->
                    provider = p
                    val (d, q) = defaultModels(p)
                    deepModel = d
                    quickModel = q
                }
            )

            Button(
                onClick = {
                    busy = true
                    showFull = false
                    status = "Agent team running..."
                    result = ""
                    scope.launch {
                        val output = withContext(Dispatchers.IO) {
                            try {
                                Python.getInstance().getModule("trading_bridge")
                                    .callAttr(
                                        "analyze",
                                        ticker.trim().uppercase(),
                                        date.trim(),
                                        provider,
                                        deepModel.trim(),
                                        quickModel.trim(),
                                        apiKey.trim(),
                                        context.filesDir.absolutePath
                                    ).toString()
                            } catch (e: Exception) {
                                JSONObject().put("error", e.message ?: "unknown").toString()
                            }
                        }
                        result = output
                        status = if (output.contains("\"error\"")) "Finished with errors." else "Done in ${elapsed}s."
                        busy = false
                    }
                },
                enabled = !busy && ticker.isNotBlank() && date.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White)
            ) {
                Text(if (busy) "Analyzing..." else "Run Analysis", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            if (busy) {
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Green))
                            Spacer(Modifier.width(8.dp))
                            Text("${ticker.uppercase()} · $provider · ${elapsed}s", fontSize = 13.sp, color = TextDim)
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), trackColor = SurfaceHi)
                        Text(
                            "Analysts → bull/bear debate → trader → risk team → portfolio manager",
                            fontSize = 12.sp, color = TextDim
                        )
                    }
                }
            } else {
                Text(status, fontSize = 13.sp, color = TextDim)
            }

            if (result.isNotEmpty()) {
                DecisionCard(result, showFull) { showFull = !showFull }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupCard(
    ticker: String, onTicker: (String) -> Unit,
    date: String, onDate: (String) -> Unit,
    provider: String, onProvider: (String) -> Unit,
    providerExpanded: Boolean, onExpanded: (Boolean) -> Unit,
    deepModel: String, onDeep: (String) -> Unit,
    quickModel: String, onQuick: (String) -> Unit,
    apiKey: String, onKey: (String) -> Unit,
    onProviderChange: (String) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Analysis Setup", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDim)

            OutlinedTextField(
                value = ticker, onValueChange = onTicker,
                label = { Text("Ticker") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_TICKERS.forEach { t ->
                    FilterChip(
                        selected = ticker.equals(t, true),
                        onClick = { onTicker(t) },
                        label = { Text(t, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            OutlinedTextField(
                value = date, onValueChange = onDate,
                label = { Text("Analysis date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
            ExposedDropdownMenuBox(expanded = providerExpanded, onExpandedChange = onExpanded) {
                OutlinedTextField(
                    value = provider, onValueChange = {}, readOnly = true,
                    label = { Text("LLM provider") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { onExpanded(false) }) {
                    PROVIDERS.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = { onProvider(p); onProviderChange(p) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = deepModel, onValueChange = onDeep,
                    label = { Text("Deep model") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = quickModel, onValueChange = onQuick,
                    label = { Text("Quick model") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors()
                )
            }
            OutlinedTextField(
                value = apiKey, onValueChange = onKey,
                label = { Text("API key (stays on device)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green,
    unfocusedBorderColor = Outline,
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    cursorColor = Green,
    focusedLabelColor = Green,
    unfocusedLabelColor = TextDim
)

@Composable
private fun DecisionCard(result: String, showFull: Boolean, onToggle: () -> Unit) {
    var action = ""
    var confidence = ""
    var error = ""
    var pretty = result
    try {
        val obj = JSONObject(result)
        if (obj.has("error")) error = obj.getString("error")
        action = (obj.optString("action", obj.optString("final_signal", ""))).uppercase()
        confidence = obj.optString("confidence", "")
        pretty = JSONObject(result).toString(2)
    } catch (_: Exception) { }

    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (error.isNotEmpty()) {
                Text("Error", color = Red, fontWeight = FontWeight.Bold)
                Text(error.takeLast(1200), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextDim)
            } else if (action.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val c = when {
                        action.contains("BUY") || action.contains("LONG") -> Green
                        action.contains("SELL") || action.contains("SHORT") -> Red
                        else -> Amber
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(c).padding(horizontal = 18.dp, vertical = 10.dp)
                    ) { Text(action, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                    if (confidence.isNotEmpty()) {
                        Text("confidence: $confidence", fontSize = 13.sp, color = TextDim)
                    }
                }
                TextButton(onClick = onToggle) {
                    Text(if (showFull) "Hide full report" else "View full report", color = Green)
                }
                if (showFull) {
                    Text(pretty, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = TextDim)
                }
            } else {
                Text(pretty.take(1500), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = TextDim)
            }
        }
    }
}
