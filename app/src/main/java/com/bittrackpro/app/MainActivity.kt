package com.bittrackpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BitTrackApp() }
    }
}

private fun euro(v: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(v)
private fun usd(v: Double): String = NumberFormat.getCurrencyInstance(Locale.US).format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitTrackApp() {
    var tab by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    val bg = Color(0xFFF7F8FA)
    val accent = Color(0xFFF7931A)

    MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = bg, surface = Color.White)) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("BitTrack Pro", fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    val labels = listOf("Piyasa", "Al/Sat", "Cüzdan", "Emirler", "Premium")
                    val icons = listOf("₿", "⇄", "◫", "≡", "★")
                    labels.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icons[i], fontSize = 18.sp) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(bg)) {
                when (tab) {
                    0 -> MarketScreen()
                    1 -> TradeScreen(apiKey, apiSecret, connected, { apiKey = it }, { apiSecret = it }, { connected = it })
                    2 -> WalletScreen(apiKey, apiSecret, connected)
                    3 -> OrdersScreen(apiKey, apiSecret, connected)
                    else -> PremiumScreen()
                }
            }
        }
    }
}

@Composable
private fun MarketScreen() {
    var eurPrice by remember { mutableStateOf<Double?>(null) }
    var usdPrice by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                eurPrice = KrakenClient.ticker("XBTEUR")
                usdPrice = KrakenClient.ticker("XBTUSD")
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Piyasa verisi alınamadı"
            }
            delay(15000)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Canlı Bitcoin piyasası", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("BTC / EUR", color = Color.Gray)
                Text(eurPrice?.let(::euro) ?: "Yükleniyor…", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("Kraken canlı spot fiyatı • 15 sn yenileme", color = Color(0xFF15803D), fontSize = 12.sp)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("BTC / USD", color = Color.Gray)
                Text(usdPrice?.let(::usd) ?: "Yükleniyor…", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Bu ekrandaki fiyatlar artık örnek veri değildir; Kraken piyasa API'sinden alınır.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun TradeScreen(
    apiKey: String,
    apiSecret: String,
    connected: Boolean,
    setApiKey: (String) -> Unit,
    setApiSecret: (String) -> Unit,
    setConnected: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var side by remember { mutableStateOf("buy") }
    var orderType by remember { mutableStateOf("market") }
    var volume by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Gerçek emir onayı") },
            text = { Text("${if (side == "buy") "AL" else "SAT"} • $orderType • $volume BTC${if (price.isNotBlank()) " • €$price" else ""}\n\nOnaylarsan emir bağlı Kraken hesabına gönderilecek.") },
            confirmButton = {
                Button(onClick = {
                    confirm = false
                    busy = true
                    scope.launch {
                        try {
                            val r = KrakenClient.addOrder(apiKey, apiSecret, side, orderType, volume, price.ifBlank { null })
                            val tx = r.optJSONObject("result")?.optJSONArray("txid")?.optString(0) ?: "oluşturuldu"
                            status = "Emir gönderildi: $tx"
                        } catch (e: Exception) {
                            status = "Emir hatası: ${e.message}"
                        } finally { busy = false }
                    }
                }) { Text("EMRİ GÖNDER") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bitcoin Al / Sat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!connected) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kraken hesabını bağla", fontWeight = FontWeight.Bold)
                    Text("API anahtarını yalnızca emir ve bakiye izinleriyle oluştur. Para çekme izni verme.", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(apiKey, setApiKey, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(apiSecret, setApiSecret, label = { Text("API Secret") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = apiKey.isNotBlank() && apiSecret.isNotBlank() && !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    KrakenClient.balance(apiKey, apiSecret)
                                    setConnected(true)
                                    status = "Kraken hesabı bağlandı"
                                } catch (e: Exception) {
                                    status = "Bağlantı hatası: ${e.message}"
                                } finally { busy = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Kontrol ediliyor…" else "Hesabı bağla") }
                }
            }
        } else {
            AssistChip(onClick = {}, label = { Text("● Kraken bağlı") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = side == "buy", onClick = { side = "buy" }, label = { Text("AL") })
                FilterChip(selected = side == "sell", onClick = { side = "sell" }, label = { Text("SAT") })
            }
            Text("Emir türü", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("market", "limit").forEach { t ->
                    FilterChip(selected = orderType == t, onClick = { orderType = t }, label = { Text(if (t == "market") "Piyasa" else "Limit") })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("stop-loss", "take-profit").forEach { t ->
                    FilterChip(selected = orderType == t, onClick = { orderType = t }, label = { Text(if (t == "stop-loss") "Stop" else "Kâr Al") })
                }
            }
            OutlinedTextField(volume, { volume = it }, label = { Text("BTC miktarı") }, modifier = Modifier.fillMaxWidth())
            if (orderType != "market") {
                OutlinedTextField(price, { price = it }, label = { Text("Tetik/limit fiyatı (€)") }, modifier = Modifier.fillMaxWidth())
            }
            Button(
                enabled = volume.toDoubleOrNull()?.let { it > 0 } == true && (orderType == "market" || price.toDoubleOrNull()?.let { it > 0 } == true) && !busy,
                onClick = { confirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (side == "buy") "BTC AL" else "BTC SAT") }
            OutlinedButton(onClick = { setConnected(false); setApiKey(""); setApiSecret("") }, modifier = Modifier.fillMaxWidth()) { Text("Bağlantıyı kapat") }
        }
        if (status.isNotBlank()) Text(status, color = if (status.contains("hata", true)) MaterialTheme.colorScheme.error else Color(0xFF15803D))
    }
}

@Composable
private fun WalletScreen(apiKey: String, apiSecret: String, connected: Boolean) {
    val scope = rememberCoroutineScope()
    var btc by remember { mutableStateOf("-") }
    var eurBalance by remember { mutableStateOf("-") }
    var status by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            try {
                val result = KrakenClient.balance(apiKey, apiSecret).getJSONObject("result")
                btc = result.optString("XXBT", result.optString("XBT", "0"))
                eurBalance = result.optString("ZEUR", result.optString("EUR", "0"))
                status = "Bakiye güncellendi"
            } catch (e: Exception) { status = "Hata: ${e.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cüzdan / Bakiye", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!connected) {
            Text("Önce Al/Sat sekmesinden Kraken hesabını bağla.")
        } else {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Bitcoin"); Text("$btc BTC", fontSize = 28.sp, fontWeight = FontWeight.Bold) } }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Euro"); Text("€$eurBalance", fontSize = 28.sp, fontWeight = FontWeight.Bold) } }
            Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Bakiyeyi yenile") }
            if (btc == "-") LaunchedEffect(Unit) { refresh() }
        }
        if (status.isNotBlank()) Text(status, color = Color.Gray)
    }
}

@Composable
private fun OrdersScreen(apiKey: String, apiSecret: String, connected: Boolean) {
    val scope = rememberCoroutineScope()
    var openCount by remember { mutableStateOf("-") }
    var closedCount by remember { mutableStateOf("-") }
    var status by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            try {
                val open = KrakenClient.openOrders(apiKey, apiSecret).getJSONObject("result").getJSONObject("open")
                val closed = KrakenClient.closedOrders(apiKey, apiSecret).getJSONObject("result").getJSONObject("closed")
                openCount = open.length().toString()
                closedCount = closed.length().toString()
                status = "Emir geçmişi güncellendi"
            } catch (e: Exception) { status = "Hata: ${e.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Emirler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!connected) Text("Önce Kraken hesabını bağla.") else {
            Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Açık emirler"); Text(openCount, fontWeight = FontWeight.Bold) } }
            Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Kapanan emirler"); Text(closedCount, fontWeight = FontWeight.Bold) } }
            Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Emirleri yenile") }
            if (openCount == "-") LaunchedEffect(Unit) { refresh() }
        }
        if (status.isNotBlank()) Text(status, color = Color.Gray)
    }
}

@Composable
private fun PremiumScreen() {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BitTrack Pro Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("Canlı piyasa, işlem ekranı, gelişmiş emirler, portföy ve bildirim özellikleri.", color = Color.Gray)
        PlanCard("Aylık Premium", "Google Play")
        PlanCard("Yıllık Premium", "Google Play")
        Text("Aboneliklerin gerçek ücretle çalışması için uygulama Google Play Console'da yayınlanmalı ve ürün kimlikleri tanımlanmalıdır.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun PlanCard(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 13.sp) }
            Button(onClick = {}) { Text("Abone ol") }
        }
    }
}
