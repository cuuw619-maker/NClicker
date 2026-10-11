package com.nclicker.game;

/**
 * Shared game rarity catalogue. Probability and reward calculations are owned
 * by the Rust gameplay engine; this enum describes how the Android client
 * displays each resulting tier.
 */
public enum PlateRarity {
    COMMON("Обычный", 2L, 0xFF9BA9BDL, "Стандартный выпуск"),
    UNCOMMON("Необычный", 8L, 0xFF75DCC5L, "Интересная комбинация"),
    RARE("Редкий", 25L, 0xFF82B8FFL, "Редкая серия"),
    EPIC("Эпический", 90L, 0xFFC29AFFL, "Коллекционная находка"),
    LEGENDARY("Легендарный", 400L, 0xFFFFC976L, "Особый выпуск"),
    MYTHIC("Мифический", 2500L, 0xFFFF86BBL, "Почти невозможная находка"),
    SECRET("Секретный", 15000L, 0xFFFFF0A6L, "Секрет коллекции");

    private final String title;
    private final long reward;
    private final long colorHex;
    private final String subtitle;

    PlateRarity(String title, long reward, long colorHex, String subtitle) {
        this.title = title;
        this.reward = reward;
        this.colorHex = colorHex;
        this.subtitle = subtitle;
    }

    public String getTitle() { return title; }
    public long getReward() { return reward; }
    public long getColorHex() { return colorHex; }
    public String getSubtitle() { return subtitle; }

    public static PlateRarity fromName(String value) {
        if (value == null) return COMMON;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return COMMON;
        }
    }
}
