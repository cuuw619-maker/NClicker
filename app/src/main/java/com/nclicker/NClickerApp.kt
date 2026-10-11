package com.nclicker

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nclicker.engine.PlateFormat
import com.nclicker.model.Plate
import com.nclicker.model.PlateCatalog
import com.nclicker.model.PlateRarity
import com.nclicker.model.PlateStorage
import com.nclicker.game.GameEconomy
import com.nclicker.game.PlateEngine
import java.text.NumberFormat
import java.util.Locale

private val Page = Color(0xFF0B0E15)
private val Panel = Color(0xFF151B27)
private val PanelRaised = Color(0xFF1C2433)
private val TextMuted = Color(0xFFA7B0C4)
private val Mint = Color(0xFF75E2D0)

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Главная", Icons.Filled.Home),
    COLLECTION("Коллекция", Icons.Filled.CollectionsBookmark),
    UPGRADES("Улучшения", Icons.Filled.TrendingUp),
    SETTINGS("Настройки", Icons.Filled.Settings)
}

@Composable
fun NClickerApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember(context) {
        val stored = context.getSharedPreferences("nclicker-game", Context.MODE_PRIVATE)
        if (stored.getInt("economy_schema", 0) < 2) {
            stored.edit()
                .putInt("economy_schema", 2)
                .putLong("balance", GameEconomy.STARTING_BALANCE)
                .putInt("luck", 0)
                .putInt("bonus", 0)
                .putInt("storage", 0)
                .apply()
        }
        stored
    }

    var balance by remember { mutableLongStateOf(prefs.getLong("balance", GameEconomy.STARTING_BALANCE)) }
    var totalRolls by remember { mutableIntStateOf(prefs.getInt("rolls", 0)) }
    var luckLevel by remember { mutableIntStateOf(prefs.getInt("luck", 0)) }
    var brokerLevel by remember { mutableIntStateOf(prefs.getInt("bonus", 0)) }
    var garageLevel by remember { mutableIntStateOf(prefs.getInt("storage", 0)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var collection by remember { mutableStateOf(PlateStorage.load(prefs).take(250)) }
    var currentPlate by remember { mutableStateOf(collection.firstOrNull() ?: PlateCatalog.preview()) }
    var rollKey by remember { mutableIntStateOf(0) }
    var lastAction by remember { mutableStateOf("Номер попадает в гараж. Деньги начисляются только после продажи.") }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val capacity = GameEconomy.inventoryCapacity(garageLevel)

    LaunchedEffect(balance, totalRolls, luckLevel, brokerLevel, garageLevel, soundEnabled, collection) {
        prefs.edit()
            .putInt("economy_schema", 2)
            .putLong("balance", balance)
            .putInt("rolls", totalRolls)
            .putInt("luck", luckLevel)
            .putInt("bonus", brokerLevel)
            .putInt("storage", garageLevel)
            .putBoolean("sound", soundEnabled)
            .apply()
        PlateStorage.save(prefs, collection)
    }

    DisposableEffect(Unit) {
        onDispose { SoundEffects.release() }
    }

    fun rollPlate() {
        if (collection.size >= capacity) {
            lastAction = "Гараж заполнен. Продай номер или увеличь вместимость."
            return
        }
        val plate = PlateCatalog.roll(luckLevel, brokerLevel)
        currentPlate = plate
        totalRolls += 1
        collection = (listOf(plate) + collection).take(250)
        rollKey += 1
        lastAction = "В гараже новый номер ${plate.number}. Оценка: ${formatCurrency(plate.marketValue)}. Продажа доступна в инвентаре."
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundEnabled) SoundEffects.play(plate.rarity)
    }

    fun sellPlate(plate: Plate) {
        val owned = collection.firstOrNull { it.id == plate.id } ?: return
        val price = PlateEngine.salePrice(owned, brokerLevel)
        collection = collection.filterNot { it.id == owned.id }
        balance = if (Long.MAX_VALUE - balance < price) Long.MAX_VALUE else balance + price
        lastAction = "Продан ${owned.number}: +${formatCurrency(price)}"
        if (soundEnabled) SoundEffects.play(owned.rarity)
    }

    fun upgradeLuck() {
        val cost = GameEconomy.upgradeCost("luck", luckLevel)
        if (luckLevel >= GameEconomy.MAX_LUCK_LEVEL || balance < cost) return
        balance -= cost
        luckLevel += 1
        lastAction = "Поиск улучшен. Вероятность редких серий выросла."
    }

    fun upgradeBroker() {
        val cost = GameEconomy.upgradeCost("broker", brokerLevel)
        if (brokerLevel >= GameEconomy.MAX_BROKER_LEVEL || balance < cost) return
        balance -= cost
        brokerLevel += 1
        lastAction = "Связи с коллекционерами повышают цену продажи на 12% за уровень."
    }

    fun upgradeGarage() {
        val cost = GameEconomy.upgradeCost("garage", garageLevel)
        if (garageLevel >= GameEconomy.MAX_GARAGE_LEVEL || balance < cost) return
        balance -= cost
        garageLevel += 1
        lastAction = "Гараж расширен: теперь доступно ${GameEconomy.inventoryCapacity(garageLevel)} мест."
    }

    fun resetProgress() {
        balance = GameEconomy.STARTING_BALANCE
        totalRolls = 0
        luckLevel = 0
        brokerLevel = 0
        garageLevel = 0
        collection = emptyList()
        currentPlate = PlateCatalog.preview()
        rollKey = 0
        lastAction = "Новый гараж открыт. Стартовый капитал — ${formatCurrency(GameEconomy.STARTING_BALANCE)}."
        prefs.edit().clear().putInt("economy_schema", 2)
            .putLong("balance", GameEconomy.STARTING_BALANCE)
            .putBoolean("sound", soundEnabled).apply()
        showResetConfirmation = false
    }

    Scaffold(
        containerColor = Page,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111621)) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).background(Page)) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    balance = balance, totalRolls = totalRolls, collectionCount = collection.size,
                    capacity = capacity, currentPlate = currentPlate, rollKey = rollKey,
                    lastAction = lastAction, luckLevel = luckLevel, canRoll = collection.size < capacity,
                    onRoll = ::rollPlate
                )
                MainTab.COLLECTION -> CollectionScreen(
                    collection = collection, brokerLevel = brokerLevel, capacity = capacity, onSell = ::sellPlate
                )
                MainTab.UPGRADES -> UpgradesScreen(
                    balance = balance, luckLevel = luckLevel, brokerLevel = brokerLevel, garageLevel = garageLevel,
                    onUpgradeLuck = ::upgradeLuck, onUpgradeBroker = ::upgradeBroker, onUpgradeGarage = ::upgradeGarage
                )
                MainTab.SETTINGS -> SettingsScreen(
                    soundEnabled = soundEnabled, onSoundChange = { soundEnabled = it },
                    totalRolls = totalRolls, collectionCount = collection.size, balance = balance,
                    onReset = { showResetConfirmation = true }
                )
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = Color(0xFF171D2A),
            title = { Text("Сбросить прогресс?") },
            text = { Text("Баланс вернётся к \$3 000, коллекция и улучшения будут сброшены.") },
            confirmButton = {
                TextButton(onClick = ::resetProgress) { Text("Сбросить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun HomeScreen(
    balance: Long, totalRolls: Int, collectionCount: Int, capacity: Int,
    currentPlate: Plate, rollKey: Int, lastAction: String, luckLevel: Int,
    canRoll: Boolean, onRoll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp).padding(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PRIVATE REGISTRATION CLUB", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("NClicker Garage", color = MaterialTheme.colorScheme.onBackground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("Коллекционирование номерных знаков", color = TextMuted, fontSize = 13.sp)
            }
            Surface(modifier = Modifier.size(50.dp), color = Color(0xFF202B40), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF394967))) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(Modifier.weight(1f), "Доступно средств", formatCurrency(balance), Icons.Filled.Paid, Mint)
            StatTile(Modifier.weight(1f), "Собрано номеров", formatNumber(totalRolls.toLong()), Icons.Filled.CollectionsBookmark, MaterialTheme.colorScheme.primary)
        }
        PlateShowcase(plate = currentPlate, rollKey = rollKey, totalRolls = totalRolls)
        Button(
            onClick = onRoll, enabled = canRoll,
            modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF172448),
                disabledContainerColor = Color(0xFF303849), disabledContentColor = TextMuted
            )
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(if (canRoll) "Получить новый номер" else "Гараж заполнен", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text("Каждый клик добавляет номер в гараж. Баланс не меняется, пока ты не продашь его в инвентаре.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Последнее действие", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(lastAction, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Места в гараже", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$collectionCount / $capacity", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = if (capacity > 0) (collectionCount.toFloat() / capacity).coerceIn(0f, 1f) else 1f,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (collectionCount >= capacity) Color(0xFFFFC976) else Mint, trackColor = Color(0xFF2A3546)
                )
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Mint)
                    Spacer(Modifier.width(8.dp))
                    Text("Рыночная оценка", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text("Обычный номер оценивается примерно в \$100. Чем реже серия, тем выше цена коллекционера. Прокачивай поиск, связи с покупателями и вместимость гаража.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                Text("Уровень поиска: $luckLevel", color = Mint, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
        Text("Номера используют узнаваемые форматы разных стран, но сами комбинации случайные и вымышленные.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PlateShowcase(plate: Plate, rollKey: Int, totalRolls: Int) {
    val rarityLevel = plate.rarity.ordinal
    val scaleTarget = if (rollKey == 0 || rollKey % 2 == 0) 1f else 1.012f + rarityLevel * 0.006f
    val springTension = when {
        rarityLevel >= PlateRarity.MYTHIC.ordinal -> 95f
        rarityLevel >= PlateRarity.LEGENDARY.ordinal -> 150f
        rarityLevel >= PlateRarity.EPIC.ordinal -> 240f
        else -> 420f
    }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(dampingRatio = if (rarityLevel >= PlateRarity.LEGENDARY.ordinal) 0.30f else 0.62f, stiffness = springTension),
        label = "plate-scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (rollKey == 0 || rollKey % 2 == 0) 0f else (rarityLevel * 0.28f).coerceAtMost(1.6f),
        animationSpec = spring(dampingRatio = 0.46f, stiffness = springTension),
        label = "plate-rotation"
    )
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, rarityColor(plate.rarity).copy(alpha = 0.75f)),
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rarityLevel >= PlateRarity.LEGENDARY.ordinal) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF20283A), Color(0xFF151A26), Color(0xFF10141D)))).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RarityBadge(plate.rarity)
                Spacer(Modifier.weight(1f))
                Text(formatCurrency(plate.marketValue), color = Mint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plate.flag, fontSize = 23.sp)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plate.country, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(plate.region, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("#${(totalRolls.coerceAtLeast(1)).toString().padStart(4, '0')}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            PlateVisual(plate = plate)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = rarityColor(plate.rarity), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(plate.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(plate.rarity.title, color = rarityColor(plate.rarity), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlateVisual(plate: Plate, modifier: Modifier = Modifier, compact: Boolean = false) {
    val blackSeries = plate.type.contains("Чёрная", ignoreCase = true)
    val taxiSeries = plate.type.equals("Такси", ignoreCase = true)
    val specialSeries = plate.type.contains("Полиция", ignoreCase = true) || plate.type.contains("ФСБ", ignoreCase = true)
    val plateBg = when {
        blackSeries -> Color(0xFF171A1D)
        taxiSeries -> Color(0xFFFFD64A)
        plate.countryCode == "US" -> Color(0xFFF1EBD5)
        plate.countryCode == "JP" -> Color(0xFFF2F0E2)
        else -> Color(0xFFF3F4F1)
    }
    val plateInk = if (blackSeries) Color(0xFFF2F2ED) else Color(0xFF171B20)
    val outline = when {
        specialSeries -> Color(0xFF3F75B8)
        blackSeries -> Color(0xFF555C64)
        taxiSeries -> Color(0xFFAB871E)
        else -> Color(0xFFB7BEC5)
    }
    val plateHeight = if (compact) 58.dp else when (plate.countryCode) {
        "US", "AE" -> 104.dp
        "JP" -> 86.dp
        else -> 74.dp
    }
    Surface(
        modifier = modifier.fillMaxWidth().height(plateHeight),
        shape = RoundedCornerShape(if (compact) 7.dp else 10.dp),
        color = plateBg,
        border = BorderStroke(if (blackSeries) 1.5.dp else 1.dp, outline),
        shadowElevation = if (compact) 1.dp else 4.dp
    ) {
        when (plate.countryCode) {
            "RU" -> {
                val parts = plate.number.trim().split(Regex("\\s+"))
                val main = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else plate.number
                val regionCode = parts.lastOrNull() ?: "00"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        if (!compact) Text("РОССИЯ", color = plateInk.copy(alpha = 0.62f), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text(main, color = plateInk, fontFamily = FontFamily.Monospace, fontSize = if (compact) 18.sp else 25.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, maxLines = 1)
                    }
                    Box(Modifier.width(1.dp).fillMaxSize().background(outline.copy(alpha = 0.8f)))
                    Column(
                        modifier = Modifier.width(if (compact) 45.dp else 54.dp).fillMaxSize().padding(horizontal = 3.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(regionCode, color = plateInk, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 16.sp)
                        if (!compact) {
                            Text("RUS", color = plateInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(0.65f).height(4.dp)) {
                                Box(Modifier.weight(1f).fillMaxSize().background(Color.White))
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFF3768C4)))
                                Box(Modifier.weight(1f).fillMaxSize().background(Color(0xFFD84A4A)))
                            }
                        }
                    }
                }
            }
            "US" -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 6.dp else 11.dp, vertical = if (compact) 4.dp else 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(plate.region.substringBefore("·").trim().uppercase(Locale.US), color = if (blackSeries) plateInk else Color(0xFF275449), fontSize = if (compact) 7.sp else 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, maxLines = 1)
                Text(plate.number, color = plateInk, fontFamily = FontFamily.Monospace, fontSize = if (compact) 17.sp else 25.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!compact) Text("MOTOR VEHICLE  •  USA", color = Color(0xFF52645D), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            "JP" -> {
                val chunks = plate.number.split(Regex("\\s+"))
                val main = chunks.lastOrNull() ?: plate.number
                val classAndKana = chunks.dropLast(1).joinToString(" ")
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${plate.region.substringAfter("·").trim()}  $classAndKana", color = Color(0xFF3C5748), fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(main, color = plateInk, fontFamily = FontFamily.Monospace, fontSize = if (compact) 17.sp else 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, maxLines = 1)
                }
            }
            "AE" -> {
                val code = plate.region.substringAfter("·").trim()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).fillMaxSize().padding(start = 10.dp, top = 4.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            when (code) { "D" -> "DUBAI  •  PRIVATE"; "AU" -> "ABU DHABI  •  PRIVATE"; "SH" -> "SHARJAH  •  PRIVATE"; else -> "UAE  •  PRIVATE" },
                            color = Color(0xFF4B555F), fontSize = if (compact) 7.sp else 9.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
                        )
                        Text(plate.number, color = plateInk, fontFamily = FontFamily.Monospace, fontSize = if (compact) 18.sp else 28.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1)
                    }
                    Column(Modifier.width(if (compact) 38.dp else 48.dp).fillMaxSize().background(Color(0xFFE1E5E6)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(code, color = Color(0xFF28333D), fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 14.sp else 19.sp)
                        if (!compact) Text("UAE", color = Color(0xFF57616A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                val regionId = plate.region.substringAfter("·").trim()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (plate.countryCode in listOf("UA", "DE", "FR", "PL", "IT", "GB")) {
                        Column(
                            modifier = Modifier.width(if (compact) 25.dp else 30.dp).fillMaxSize().background(Color(0xFF244D9B)),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            if (!compact && plate.countryCode != "GB" && plate.countryCode != "UA") Text("★", color = Color(0xFFFFD75C), fontSize = 9.sp)
                            Text(when (plate.countryCode) {
                                "UA" -> "UA"; "DE" -> "D"; "FR" -> "F"; "PL" -> "PL"; "IT" -> "I"; else -> "UK"
                            }, color = Color.White, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.ExtraBold)
                            if (plate.countryCode == "UA" && !compact) Box(Modifier.width(15.dp).height(3.dp).background(Color(0xFFFFD64A)))
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 3.dp else 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        if (plate.countryCode == "DE" && !compact) Text("ZULASSUNGSBEZIRK  $regionId", color = Color(0xFF5E6770), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        else if (plate.countryCode == "FR" && !compact) Text("RÉPUBLIQUE FRANÇAISE", color = Color(0xFF5E6770), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        else if (plate.countryCode == "IT" && !compact) Text("REPUBBLICA ITALIANA", color = Color(0xFF5E6770), fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(plate.number, color = plateInk, fontFamily = FontFamily.Monospace, fontSize = if (compact) 17.sp else 23.sp, fontWeight = FontWeight.Black, letterSpacing = if (compact) 0.sp else 0.7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun RarityBadge(rarity: PlateRarity) {
    val color = rarityColor(rarity)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            rarity.title.uppercase(Locale.getDefault()),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CollectionScreen(collection: List<Plate>, brokerLevel: Int, capacity: Int, onSell: (Plate) -> Unit) {
    val filters = listOf("Все", "Такси", "Полиция", "ФСБ", "Чёрная", "Дипломатический", "Ретро")
    var filter by rememberSaveable { mutableStateOf("Все") }
    val filtered = remember(collection, filter) {
        if (filter == "Все") collection else collection.filter { it.type.contains(filter, ignoreCase = true) }
    }
    val totalOffer = collection.sumOf { PlateEngine.salePrice(it, brokerLevel) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 16.dp)) {
        Text("ЛИЧНАЯ КОЛЛЕКЦИЯ", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Инвентарь", color = MaterialTheme.colorScheme.onBackground, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${collection.size} номеров · гараж $capacity мест", color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("Оценка: ${formatCurrency(totalOffer)}", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { item -> FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) }) }
        }
        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, tint = TextMuted, modifier = Modifier.size(42.dp))
                    Text(if (collection.isEmpty()) "Инвентарь пуст" else "Номеров такой серии пока нет", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Получи новый номер на главной. Деньги придут только после продажи.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { plate ->
                    CollectionPlateRow(plate, PlateEngine.salePrice(plate, brokerLevel)) { onSell(plate) }
                }
            }
        }
    }
}

@Composable
private fun CollectionPlateRow(plate: Plate, offer: Long, onSell: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color(0xFF283246))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlateVisual(plate = plate, compact = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${plate.flag}  ${plate.country}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${plate.region} · ${plate.type}", color = TextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                RarityBadge(plate.rarity)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Оценка рынка", color = TextMuted, fontSize = 10.sp)
                    Text(formatCurrency(offer), color = Mint, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
                Button(onClick = onSell, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)) {
                    Text("Продать")
                }
            }
        }
    }
}

@Composable
private fun UpgradesScreen(
    balance: Long, luckLevel: Int, brokerLevel: Int, garageLevel: Int,
    onUpgradeLuck: () -> Unit, onUpgradeBroker: () -> Unit, onUpgradeGarage: () -> Unit
) {
    val luckCost = GameEconomy.upgradeCost("luck", luckLevel)
    val brokerCost = GameEconomy.upgradeCost("broker", brokerLevel)
    val garageCost = GameEconomy.upgradeCost("garage", garageLevel)
    val capacity = GameEconomy.inventoryCapacity(garageLevel)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("РАЗВИТИЕ КОЛЛЕКЦИИ", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Улучшения", color = MaterialTheme.colorScheme.onBackground, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Paid, contentDescription = null, tint = Mint, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Свободный капитал", color = TextMuted, fontSize = 12.sp)
                    Text(formatCurrency(balance), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                }
            }
        }
        UpgradeCard(
            icon = Icons.Filled.Stars, title = "Поиск редких комбинаций",
            description = "Профессиональный поиск повышает вероятность находить редкие серии. Максимум 12 уровней.",
            level = luckLevel, maxLevel = GameEconomy.MAX_LUCK_LEVEL, cost = luckCost,
            canBuy = balance >= luckCost, actionLabel = "Улучшить поиск", onClick = onUpgradeLuck
        )
        UpgradeCard(
            icon = Icons.Filled.TrendingUp, title = "Связи с коллекционерами",
            description = "Покупатели платят на 12% больше за уровень. Бонус применяется при продаже и старых, и новых номеров.",
            level = brokerLevel, maxLevel = GameEconomy.MAX_BROKER_LEVEL, cost = brokerCost,
            canBuy = balance >= brokerCost, actionLabel = "Развить связи", onClick = onUpgradeBroker
        )
        UpgradeCard(
            icon = Icons.Filled.DirectionsCar, title = "Вместительный гараж",
            description = "Каждый уровень добавляет 20 мест для хранения. Вместимость растёт с 30 до 150 номеров. Сейчас: $capacity мест.",
            level = garageLevel, maxLevel = GameEconomy.MAX_GARAGE_LEVEL, cost = garageCost,
            canBuy = balance >= garageCost, actionLabel = "Расширить гараж", onClick = onUpgradeGarage
        )
        Text("Улучшения покупаются за деньги от продаж. Получение номера не начисляет валюту и не тратит баланс.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun UpgradeCard(
    icon: ImageVector, title: String, description: String,
    level: Int, maxLevel: Int, cost: Long, canBuy: Boolean, actionLabel: String, onClick: () -> Unit
) {
    val maxed = level >= maxLevel
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color(0xFF283246))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF26334B), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(if (maxed) "MAX · $level/$maxLevel" else "Уровень $level / $maxLevel", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(description, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            Button(
                onClick = onClick, enabled = canBuy && !maxed,
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (maxed) "Максимальный уровень" else "$actionLabel · ${formatCurrency(cost)}", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    soundEnabled: Boolean, onSoundChange: (Boolean) -> Unit,
    totalRolls: Int, collectionCount: Int, balance: Long, onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("ПЕРСОНАЛИЗАЦИЯ", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Настройки", color = MaterialTheme.colorScheme.onBackground, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Звуки находок", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Сигналы зависят от редкости номера.", color = TextMuted, fontSize = 12.sp)
                }
                Switch(checked = soundEnabled, onCheckedChange = onSoundChange)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Mint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Экономика и сохранение", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Стартовый капитал — \$3 000. Получение номера не начисляет деньги. Каждый номер хранится в инвентаре и продаётся отдельно по рыночной цене.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Text("Баланс: ${formatCurrency(balance)}", color = Mint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Получено номеров: ${formatNumber(totalRolls.toLong())} · Сейчас в гараже: $collectionCount", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                Text("Форматы номеров похожи на стандарты стран, но комбинации случайные и вымышленные.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Button(
            onClick = onReset, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF442A34), contentColor = Color(0xFFFFC2CC))
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сбросить прогресс", fontWeight = FontWeight.Bold)
        }
        Text("Material 3 · Kotlin UI · Java economy · Rust game engine", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun rarityColor(rarity: PlateRarity): Color = Color(rarity.colorHex)

private fun formatNumber(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value)

private fun formatCurrency(value: Long): String =
    "\$" + NumberFormat.getIntegerInstance(Locale.US).format(value.coerceAtLeast(0L))

private object SoundEffects {
    private var generator: ToneGenerator? = null

    fun play(rarity: PlateRarity) {
        runCatching {
            val tone = generator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 72).also { generator = it }
            val type = when (rarity) {
                PlateRarity.COMMON -> ToneGenerator.TONE_PROP_BEEP
                PlateRarity.UNCOMMON -> ToneGenerator.TONE_PROP_ACK
                PlateRarity.RARE -> ToneGenerator.TONE_PROP_BEEP2
                PlateRarity.EPIC -> ToneGenerator.TONE_PROP_PROMPT
                PlateRarity.LEGENDARY -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                PlateRarity.MYTHIC -> ToneGenerator.TONE_CDMA_ONE_MIN_BEEP
                PlateRarity.SECRET -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
            }
            val duration = when (rarity) {
                PlateRarity.COMMON -> 70
                PlateRarity.UNCOMMON -> 100
                PlateRarity.RARE -> 140
                PlateRarity.EPIC -> 220
                PlateRarity.LEGENDARY -> 350
                PlateRarity.MYTHIC -> 520
                PlateRarity.SECRET -> 700
            }
            tone.startTone(type, duration)
        }
    }

    fun release() {
        runCatching { generator?.release() }
        generator = null
    }
}
