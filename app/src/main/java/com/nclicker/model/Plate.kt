package com.nclicker.model

import android.content.SharedPreferences
import com.nclicker.engine.NativeEngine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class PlateRarity(
    val title: String,
    val reward: Long,
    val colorHex: Long,
    val subtitle: String
) {
    COMMON("Обычный", 2L, 0xFF9BA9BD, "Стандартный выпуск"),
    UNCOMMON("Необычный", 8L, 0xFF75DCC5, "Интересная комбинация"),
    RARE("Редкий", 25L, 0xFF82B8FF, "Редкая серия"),
    EPIC("Эпический", 90L, 0xFFC29AFF, "Коллекционная находка"),
    LEGENDARY("Легендарный", 400L, 0xFFFFC976, "Особый выпуск"),
    MYTHIC("Мифический", 2500L, 0xFFFF86BB, "Почти невозможная находка"),
    SECRET("Секретный", 15000L, 0xFFFFF0A6, "Секрет коллекции")
}

data class Plate(
    val id: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val region: String,
    val number: String,
    val type: String,
    val rarity: PlateRarity,
    val reward: Long
)

private data class CountryPattern(
    val name: String,
    val code: String,
    val flag: String,
    val regions: List<String>
)

object PlateCatalog {
    private val countries = listOf(
        CountryPattern("Россия", "RU", "🇷🇺", listOf("Москва · 77", "Санкт-Петербург · 78", "Татарстан · 16", "Краснодар · 23", "Свердловская область · 66")),
        CountryPattern("Украина", "UA", "🇺🇦", listOf("Киев · AA", "Львов · BC", "Одесса · BH", "Харьков · AX", "Днепр · AE")),
        CountryPattern("Германия", "DE", "🇩🇪", listOf("Берлин · B", "Гамбург · HH", "Мюнхен · M", "Кёльн · K")),
        CountryPattern("Япония", "JP", "🇯🇵", listOf("Токио · 品川", "Осака · なにわ", "Йокогама · 横浜")),
        CountryPattern("США", "US", "🇺🇸", listOf("Калифорния · CA", "Нью-Йорк · NY", "Техас · TX", "Флорида · FL")),
        CountryPattern("Великобритания", "GB", "🇬🇧", listOf("Лондон · LD", "Манчестер · MN", "Уэльс · CY")),
        CountryPattern("Франция", "FR", "🇫🇷", listOf("Париж · 75", "Лион · 69", "Марсель · 13")),
        CountryPattern("ОАЭ", "AE", "🇦🇪", listOf("Дубай · D", "Абу-Даби · AU", "Шарджа · SH")),
        CountryPattern("Польша", "PL", "🇵🇱", listOf("Варшава · WA", "Краков · KR", "Гданьск · GD")),
        CountryPattern("Италия", "IT", "🇮🇹", listOf("Рим · RM", "Милан · MI", "Турин · TO"))
    )

    fun preview(): Plate = Plate(
        id = "preview",
        country = "Россия",
        countryCode = "RU",
        flag = "🇷🇺",
        region = "Москва · 77",
        number = "А777ВС 77",
        type = "Гражданский",
        rarity = PlateRarity.COMMON,
        reward = PlateRarity.COMMON.reward
    )

    fun roll(luckLevel: Int, bonusLevel: Int): Plate {
        val ticket = (NativeEngine.nextTicket(100_000) - luckLevel * 1_450).coerceAtLeast(0)
        val rarity = when {
            ticket < 60_000 -> PlateRarity.COMMON
            ticket < 84_000 -> PlateRarity.UNCOMMON
            ticket < 96_000 -> PlateRarity.RARE
            ticket < 99_000 -> PlateRarity.EPIC
            ticket < 99_800 -> PlateRarity.LEGENDARY
            ticket < 99_970 -> PlateRarity.MYTHIC
            else -> PlateRarity.SECRET
        }

        val kindRoll = Random.nextInt(100)
        val type = when {
            rarity.ordinal >= PlateRarity.LEGENDARY.ordinal && kindRoll < 28 -> "Чёрная коллекционная серия"
            kindRoll < 3 -> "ФСБ · игровая спецсерия"
            kindRoll < 9 -> "Полиция · игровой выпуск"
            kindRoll < 16 -> "Чёрная серия"
            kindRoll < 28 -> "Такси"
            kindRoll < 35 -> "Дипломатический"
            kindRoll < 43 -> "Ретро"
            else -> "Гражданский"
        }

        val country = if (type.startsWith("ФСБ")) countries.first { it.code == "RU" } else countries.random()
        val region = country.regions.random()
        val number = makeNumber(country.code, region)
        val reward = (rarity.reward * (1.0 + bonusLevel * 0.35)).toLong().coerceAtLeast(1L)
        val id = "${country.code}-${System.currentTimeMillis()}-${Random.nextInt(100_000, 999_999)}"

        return Plate(
            id = id,
            country = country.name,
            countryCode = country.code,
            flag = country.flag,
            region = region,
            number = number,
            type = type,
            rarity = rarity,
            reward = reward
        )
    }

    private fun makeNumber(countryCode: String, region: String): String {
        val digits = { count: Int -> (1..count).joinToString("") { Random.nextInt(10).toString() } }
        val letters = "АВЕКМНОРСТУХ"
        val ruLetters = { count: Int -> (1..count).joinToString("") { letters.random().toString() } }
        val suffix = region.substringAfterLast("·").trim()

        return when (countryCode) {
            "RU" -> "${ruLetters(1)}${digits(3)}${ruLetters(2)} $suffix"
            "UA" -> "$suffix ${digits(4)} ${ruLetters(2)}"
            "DE" -> "$suffix · NC ${digits(4)}"
            "JP" -> "$suffix 503 ね ${digits(2)}-${digits(2)}"
            "US" -> "$suffix · ${digits(1)}NCX${digits(3)}"
            "GB" -> "NC${digits(2)} NCL"
            "FR" -> "${ruLetters(2)}-${digits(3)}-${ruLetters(2)}"
            "AE" -> "$suffix · ${digits(5)}"
            "PL" -> "$suffix ${digits(3)} NC"
            else -> "NC ${digits(3)} ZZ"
        }
    }
}

object PlateStorage {
    private const val KEY = "collection_json"

    fun load(preferences: SharedPreferences): List<Plate> {
        val raw = preferences.getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<Plate>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rarity = runCatching {
                    PlateRarity.valueOf(item.optString("rarity", PlateRarity.COMMON.name))
                }.getOrDefault(PlateRarity.COMMON)
                result.add(
                    Plate(
                        id = item.optString("id", "saved-$index"),
                        country = item.optString("country", "Неизвестно"),
                        countryCode = item.optString("countryCode", "??"),
                        flag = item.optString("flag", "🏁"),
                        region = item.optString("region", "Без региона"),
                        number = item.optString("number", "NC 000"),
                        type = item.optString("type", "Гражданский"),
                        rarity = rarity,
                        reward = item.optLong("reward", rarity.reward)
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(preferences: SharedPreferences, plates: List<Plate>) {
        val array = JSONArray()
        plates.forEach { plate ->
            array.put(
                JSONObject()
                    .put("id", plate.id)
                    .put("country", plate.country)
                    .put("countryCode", plate.countryCode)
                    .put("flag", plate.flag)
                    .put("region", plate.region)
                    .put("number", plate.number)
                    .put("type", plate.type)
                    .put("rarity", plate.rarity.name)
                    .put("reward", plate.reward)
            )
        }
        preferences.edit().putString(KEY, array.toString()).apply()
    }
}
