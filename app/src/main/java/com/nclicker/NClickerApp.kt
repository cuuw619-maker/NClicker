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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

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
        context.getSharedPreferences("nclicker-game", Context.MODE_PRIVATE)
    }

    var balance by remember { mutableLongStateOf(prefs.getLong("balance", 0L)) }
    var totalRolls by remember { mutableIntStateOf(prefs.getInt("rolls", 0)) }
    var luckLevel by remember { mutableIntStateOf(prefs.getInt("luck", 0)) }
    var bonusLevel by remember { mutableIntStateOf(prefs.getInt("bonus", 0)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var collection by remember { mutableStateOf(PlateStorage.load(prefs).take(250)) }
    var currentPlate by remember { mutableStateOf(collection.firstOrNull() ?: PlateCatalog.preview()) }
    var rollKey by remember { mutableIntStateOf(0) }
    var lastReward by remember { mutableLongStateOf(0L) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showReveal by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var selectedPlate by remember { mutableStateOf<Plate?>(null) }

    LaunchedEffect(balance, totalRolls, luckLevel, bonusLevel, soundEnabled, collection) {
        prefs.edit()
            .putLong("balance", balance)
            .putInt("rolls", totalRolls)
            .putInt("luck", luckLevel)
            .putInt("bonus", bonusLevel)
            .putBoolean("sound", soundEnabled)
            .apply()
        PlateStorage.save(prefs, collection)
    }

    DisposableEffect(Unit) {
        onDispose { SoundEffects.release() }
    }

    fun rollPlate() {
        val plate = PlateCatalog.roll(luckLevel, bonusLevel)
        currentPlate = plate
        lastReward = plate.reward
        balance += plate.reward
        totalRolls += 1
        collection = (listOf(plate) + collection).take(250)
        rollKey += 1
        showReveal = plate.rarity.ordinal >= PlateRarity.EPIC.ordinal
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundEnabled) SoundEffects.play(plate.rarity)
    }

    fun upgradeLuck() {
        val cost = upgradeCost(150L, luckLevel)
        if (balance < cost) return
        balance -= cost
        luckLevel += 1
        Toast.makeText(context, "Шанс редких номеров увеличен", Toast.LENGTH_SHORT).show()
    }

    fun upgradeBonus() {
        val cost = upgradeCost(100L, bonusLevel)
        if (balance < cost) return
        balance -= cost
        bonusLevel += 1
        Toast.makeText(context, "Награда за находки увеличена", Toast.LENGTH_SHORT).show()
    }

    fun resetProgress() {
        balance = 0L
        totalRolls = 0
        luckLevel = 0
        bonusLevel = 0
        collection = emptyList()
        currentPlate = PlateCatalog.preview()
        lastReward = 0L
        rollKey = 0
        showReveal = false
        prefs.edit().clear().apply()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Page)
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeScreen(
                    balance = balance,
                    totalRolls = totalRolls,
                    collectionCount = collection.size,
                    currentPlate = currentPlate,
                    rollKey = rollKey,
                    lastReward = lastReward,
                    luckLevel = luckLevel,
                    onRoll = ::rollPlate
                )
                MainTab.COLLECTION -> CollectionScreen(
                    collection = collection,
                    onOpen = { selectedPlate = it }
                )
                MainTab.UPGRADES -> UpgradesScreen(
                    balance = balance,
                    luckLevel = luckLevel,
                    bonusLevel = bonusLevel,
                    onUpgradeLuck = ::upgradeLuck,
                    onUpgradeBonus = ::upgradeBonus
                )
                MainTab.SETTINGS -> SettingsScreen(
                    soundEnabled = soundEnabled,
                    onSoundChange = { soundEnabled = it },
                    totalRolls = totalRolls,
                    collectionCount = collection.size,
                    onReset = { showResetConfirmation = true }
                )
            }
        }
    }

    if (showReveal) {
        val plate = currentPlate
        AlertDialog(
            onDismissRequest = { showReveal = false },
            containerColor = Color(0xFF171D2A),
            icon = { Icon(Icons.Filled.Stars, contentDescription = null, tint = rarityColor(plate.rarity), modifier = Modifier.size(34.dp)) },
            title = {
                Text(
                    when (plate.rarity) {
                        PlateRarity.EPIC -> "Эпическая находка!"
                        PlateRarity.LEGENDARY -> "Легендарный номер!"
                        PlateRarity.MYTHIC -> "Мифический дроп!"
                        PlateRarity.SECRET -> "СЕКРЕТНЫЙ НОМЕР"
                        else -> "Новая находка"
                    },
                    textAlign = TextAlign.Center,
                    color = rarityColor(plate.rarity),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${plate.flag}  ${plate.country} · ${plate.region}", color = TextMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    PlateVisual(plate = plate)
                    Spacer(Modifier.height(14.dp))
                    Text(plate.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("+${formatNumber(plate.reward)} NC", color = Mint, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showReveal = false }) {
                    Text("Забрать номер")
                }
            }
        )
    }

    if (selectedPlate != null) {
        val plate = selectedPlate!!
        AlertDialog(
            onDismissRequest = { selectedPlate = null },
            containerColor = Color(0xFF171D2A),
            title = { Text("Детали номера") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlateVisual(plate = plate)
                    RarityBadge(plate.rarity)
                    Text(plate.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(PlateFormat.countryLine(plate.country, plate.region), color = TextMuted)
                    Text("Награда: ${formatNumber(plate.reward)} NC", color = Mint)
                    Text("Комбинации в игре сгенерированы случайно и не обозначают конкретное реальное транспортное средство.", color = TextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPlate = null }) { Text("Закрыть") }
            }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = Color(0xFF171D2A),
            title = { Text("Сбросить прогресс?") },
            text = { Text("Баланс, улучшения и собранные номера будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = ::resetProgress) { Text("Сбросить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    balance: Long,
    totalRolls: Int,
    collectionCount: Int,
    currentPlate: Plate,
    rollKey: Int,
    lastReward: Long,
    luckLevel: Int,
    onRoll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("НОМЕРНАЯ КОЛЛЕКЦИЯ", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                Text("NClicker", color = MaterialTheme.colorScheme.onBackground, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
                Text("Каждый клик — новая история", color = TextMuted, fontSize = 14.sp)
            }
            Surface(
                modifier = Modifier.size(52.dp),
                color = Color(0xFF202B40),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFF394967))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Баланс",
                value = "${formatNumber(balance)} NC",
                icon = Icons.Filled.Paid,
                accent = Mint
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Открыто номеров",
                value = formatNumber(totalRolls.toLong()),
                icon = Icons.Filled.CollectionsBookmark,
                accent = MaterialTheme.colorScheme.primary
            )
        }

        PlateShowcase(plate = currentPlate, rollKey = rollKey, totalRolls = totalRolls)

        Button(
            onClick = onRoll,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color(0xFF172448))
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Получить номер", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Последняя награда", color = TextMuted, fontSize = 13.sp)
                Text(if (lastReward > 0L) "+${formatNumber(lastReward)} NC" else "Нажми, чтобы начать", color = if (lastReward > 0L) Mint else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Surface(color = PanelRaised, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("${collectionCount}/250", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Mint)
                    Spacer(Modifier.width(9.dp))
                    Text("Шанс редких серий", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Ур. ${luckLevel}", color = Mint, fontWeight = FontWeight.Bold)
                }
                val progress = (balance.toFloat() / upgradeCost(150L, luckLevel).toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = Mint,
                    trackColor = Color(0xFF2A3546)
                )
                Text("Улучшай удачу во вкладке «Улучшения», чтобы чаще находить редкие номера.", color = TextMuted, fontSize = 12.sp)
            }
        }

        Text(
            "Все номера и спецсерии сгенерированы для игры. Это не база реальных регистраций.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun PlateShowcase(plate: Plate, rollKey: Int, totalRolls: Int) {
    val rarityLevel = plate.rarity.ordinal
    val scaleTarget = if (rollKey % 2 == 0) 1f else 1.018f + rarityLevel * 0.008f
    val animationStiffness = when {
        rarityLevel >= PlateRarity.MYTHIC.ordinal -> 85f
        rarityLevel >= PlateRarity.LEGENDARY.ordinal -> 145f
        rarityLevel >= PlateRarity.EPIC.ordinal -> 230f
        rarityLevel >= PlateRarity.RARE.ordinal -> 340f
        else -> 520f
    }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = if (rarityLevel >= PlateRarity.LEGENDARY.ordinal) 0.24f else 0.58f,
            stiffness = animationStiffness
        ),
        label = "plate-scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (rollKey % 2 == 0) 0f else 0.25f + rarityLevel * 0.62f,
        animationSpec = spring(
            dampingRatio = if (rarityLevel >= PlateRarity.MYTHIC.ordinal) 0.22f else 0.42f,
            stiffness = animationStiffness
        ),
        label = "plate-rotation"
    )
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        },
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, rarityColor(plate.rarity).copy(alpha = 0.75f)),
        colors = CardDefaults.cardColors(containerColor = Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = if (plate.rarity.ordinal >= PlateRarity.LEGENDARY.ordinal) 9.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF20283A), Color(0xFF151A26), Color(0xFF10141D))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RarityBadge(plate.rarity)
                Spacer(Modifier.weight(1f))
                Text("+${formatNumber(plate.reward)} NC", color = Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plate.flag, fontSize = 25.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plate.country, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(plate.region, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("#${(totalRolls + if (rollKey == 0) 1 else 0).toString().padStart(4, '0')}", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            PlateVisual(plate = plate)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = rarityColor(plate.rarity), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(plate.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(plate.rarity.subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlateVisual(plate: Plate, modifier: Modifier = Modifier) {
    val numberSize = when {
        plate.number.length >= 16 -> 14.sp
        plate.number.length >= 13 -> 16.sp
        else -> 19.sp
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F3EE),
        border = BorderStroke(1.dp, Color(0xFFB8C0C6)),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(7.dp).height(72.dp).background(
                    when (plate.countryCode) {
                        "UA" -> Color(0xFF5C9DE8)
                        "RU" -> Color(0xFF7E91B2)
                        "JP" -> Color(0xFFBC6B76)
                        else -> Color(0xFF496EB7)
                    }
                )
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    PlateFormat.countryLine(plate.country, plate.region),
                    color = Color(0xFF4B5563),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    PlateFormat.normalize(plate.number),
                    color = Color(0xFF171B20),
                    fontFamily = FontFamily.Monospace,
                    fontSize = numberSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.width(41.dp).padding(end = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(plate.countryCode, color = Color(0xFF303B4E), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Text(plate.flag, fontSize = 15.sp)
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
private fun CollectionScreen(collection: List<Plate>, onOpen: (Plate) -> Unit) {
    val filters = listOf("Все", "Такси", "Полиция", "ФСБ", "Чёрная", "Дипломатический", "Ретро")
    var filter by rememberSaveable { mutableStateOf("Все") }
    val filtered = remember(collection, filter) {
        if (filter == "Все") collection else collection.filter { it.type.contains(filter, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 16.dp)) {
        Text("ТВОЙ ГАРАЖ", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text("Коллекция", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("${collection.size} из 250 последних находок", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = null, tint = TextMuted, modifier = Modifier.size(42.dp))
                    Text(if (collection.isEmpty()) "Коллекция пока пуста" else "В этой серии пока нет номеров", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Возвращайся на главную и получи первый номер.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { plate ->
                    CollectionPlateRow(plate = plate, onClick = { onOpen(plate) })
                }
            }
        }
    }
}

@Composable
private fun CollectionPlateRow(plate: Plate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plate.flag, fontSize = 18.sp)
                Spacer(Modifier.width(7.dp))
                Text(plate.country, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(color = Color(0xFFF2F3EE), shape = RoundedCornerShape(8.dp)) {
                Text(
                    PlateFormat.normalize(plate.number),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    color = Color(0xFF171B20),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Text(plate.type, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RarityBadge(plate.rarity)
            Text("+${formatNumber(plate.reward)} NC", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(plate.region, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UpgradesScreen(
    balance: Long,
    luckLevel: Int,
    bonusLevel: Int,
    onUpgradeLuck: () -> Unit,
    onUpgradeBonus: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("РАЗВИТИЕ ГАРАЖА", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text("Улучшения", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Surface(color = Panel, shape = RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Paid, contentDescription = null, tint = Mint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Твой баланс", color = TextMuted, fontSize = 12.sp)
                    Text("${formatNumber(balance)} NC", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
        UpgradeCard(
            icon = Icons.Filled.Stars,
            title = "Интуиция коллекционера",
            description = "Повышает шанс выпадения редких и легендарных комбинаций. Каждый уровень смещает бросок в сторону редкости.",
            level = luckLevel,
            cost = upgradeCost(150L, luckLevel),
            canBuy = balance >= upgradeCost(150L, luckLevel),
            actionLabel = "Улучшить шанс",
            onClick = onUpgradeLuck
        )
        UpgradeCard(
            icon = Icons.Filled.TrendingUp,
            title = "Торговая площадка",
            description = "Увеличивает награду за каждый найденный номер на 35% за уровень.",
            level = bonusLevel,
            cost = upgradeCost(100L, bonusLevel),
            canBuy = balance >= upgradeCost(100L, bonusLevel),
            actionLabel = "Увеличить награду",
            onClick = onUpgradeBonus
        )
        Text("Стоимость растёт с каждым уровнем. Улучшения и баланс сохраняются на устройстве.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun UpgradeCard(
    icon: ImageVector,
    title: String,
    description: String,
    level: Int,
    cost: Long,
    canBuy: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color(0xFF283246))
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF26334B), shape = RoundedCornerShape(15.dp), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Уровень $level", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(description, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
            Button(
                onClick = onClick,
                enabled = canBuy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("$actionLabel · ${formatNumber(cost)} NC", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    totalRolls: Int,
    collectionCount: Int,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ПЕРСОНАЛИЗАЦИЯ", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text("Настройки", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Звуки находок", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Звук меняется в зависимости от редкости.", color = TextMuted, fontSize = 12.sp)
                }
                Switch(checked = soundEnabled, onCheckedChange = onSoundChange)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Mint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Об игре", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("NClicker — коллекционный кликер с номерными знаками разных стран, уровнями редкости, улучшениями и звуковыми эффектами.", color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                Text("Номера, регионы и специальные серии генерируются для игры. Они вымышленные и не являются проверкой реальных регистраций, документов или принадлежности к ведомствам.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Text("Кликов: ${formatNumber(totalRolls.toLong())}     •     В коллекции: $collectionCount", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF442A34), contentColor = Color(0xFFFFC2CC))
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Сбросить весь прогресс", fontWeight = FontWeight.Bold)
        }
        Text("Версия 1.0.0 · Material 3 · Kotlin + Java + Rust", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun upgradeCost(base: Long, level: Int): Long =
    (base.toDouble() * 1.72.pow(level.coerceAtMost(20))).toLong().coerceAtLeast(base)

private fun rarityColor(rarity: PlateRarity): Color = Color(rarity.colorHex)

private fun formatNumber(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

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
