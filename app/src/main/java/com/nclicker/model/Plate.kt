package com.nclicker.model

import android.content.SharedPreferences
import com.nclicker.game.PlateEngine
import com.nclicker.game.PlateStorage as JavaPlateStorage

/**
 * Kotlin compatibility facade only. The model, economy, local storage,
 * and native-generation bridge live in Java; game generation runs in Rust.
 */
typealias Plate = com.nclicker.game.Plate
typealias PlateRarity = com.nclicker.game.PlateRarity

object PlateCatalog {
    fun preview(): Plate = PlateEngine.preview()

    fun roll(luckLevel: Int, bonusLevel: Int): Plate =
        PlateEngine.roll(luckLevel, bonusLevel)
}

object PlateStorage {
    fun load(preferences: SharedPreferences): List<Plate> =
        JavaPlateStorage.load(preferences)

    fun save(preferences: SharedPreferences, plates: List<Plate>) {
        JavaPlateStorage.save(preferences, plates)
    }
}
