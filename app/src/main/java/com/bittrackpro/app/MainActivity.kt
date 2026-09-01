package com.bittrackpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BitTrackApp() }
    }
}

private data class MarketRow(val symbol: String, val price: String, val change: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitTrackApp() {
    var tab by remember { mutableIntStateOf(0) }
    val bg = Color(0xFFF7F8FA)
    val accent = Color(0xFFF7931A)
    MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = bg, surface = Color.White)) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("BitTrack Pro", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)) },
            bottomBar = {
                NavigationBar {
                    listOf("Piyasa", "Portföy", "Alarmlar", "Premium").forEachIndexed { i, label ->
                        NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(listOf("₿", "◫", "⏰", "★")[i], fontSize = 18.sp) }, label = { Text(label) })
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(bg)) {
                when (tab) { 0 -> MarketScreen(); 1 -> PortfolioScreen(); 2 -> AlertsScreen(); else -> PremiumScreen() }
            }
        }
    }
}

@Composable private fun MarketScreen() {
    val rows = listOf(MarketRow("BTC / EUR", "€58.420,00", "+2,8%"), MarketRow("BTC / USD", "$63,250.00", "+2,6%"))
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp)) { Text("Bitcoin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Canlı fiyat takibi", color = Color.Gray); Spacer(Modifier.height(16.dp)); Text("€58.420,00", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold); Text("Bugün +2,8%", color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold) } } }
        items(rows) { row -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(row.symbol, fontWeight = FontWeight.Bold); Text(row.price) }; Text(row.change, color = Color(0xFF15803D), fontWeight = FontWeight.Bold) } } }
        item { Text("İlk sürümde fiyatlar örnek veridir. Yayın sürümünde gerçek piyasa veri API'si bağlanacaktır.", color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable private fun PortfolioScreen() { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Portföy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Toplam değer", color = Color.Gray); Text("€0,00", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); Text("Alış fiyatını ve BTC miktarını ekleyerek kâr/zarar takibi yapılacak.") } }; Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Portföye BTC ekle") } } }

@Composable private fun AlertsScreen() { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Fiyat alarmları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("BTC hedef fiyatına ulaştığında bildirim gönder."); Spacer(Modifier.height(12.dp)); OutlinedTextField(value = "", onValueChange = {}, label = { Text("Hedef fiyat (€)") }, modifier = Modifier.fillMaxWidth()) } }; Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Alarm oluştur") } } }

@Composable private fun PremiumScreen() { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("BitTrack Pro Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("Gelişmiş alarmlar, sınırsız portföy, reklamsız kullanım ve premium analizler.", color = Color.Gray); PlanCard("Aylık Premium", "Google Play üzerinden"); PlanCard("Yıllık Premium", "Google Play üzerinden"); Text("Gerçek fiyatlar Google Play Console'daki abonelik ürünlerinden okunacaktır.", color = Color.Gray, fontSize = 12.sp) } }

@Composable private fun PlanCard(title: String, subtitle: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 13.sp) }; Button(onClick = {}) { Text("Abone ol") } } } }
