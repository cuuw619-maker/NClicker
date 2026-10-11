package com.nclicker.game;

/** Display metadata and base market prices for plate rarity tiers. */
public enum PlateRarity {
    COMMON("Обычный", 100L, 0xFF9BA9BD, "Стандартный выпуск"),
    UNCOMMON("Необычный", 150L, 0xFF75DCC5, "Интересная комбинация"),
    RARE("Редкий", 300L, 0xFF82B8FF, "Редкая серия"),
    EPIC("Эпический", 750L, 0xFFC29AFF, "Коллекционная находка"),
    LEGENDARY("Легендарный", 2_000L, 0xFFFFC976, "Особый выпуск"),
    MYTHIC("Мифический", 7_500L, 0xFFFF86BB, "Почти невозможная находка"),
    SECRET("Секретный", 25_000L, 0xFFFFF0A6, "Секрет коллекции");

    private final String title;
    private final long baseValue;
    private final int colorHex;
    private final String subtitle;

    PlateRarity(String title, long baseValue, int colorHex, String subtitle) {
        this.title = title;
        this.baseValue = baseValue;
        this.colorHex = colorHex;
        this.subtitle = subtitle;
    }

    public String getTitle() { return title; }
    public long getBaseValue() { return baseValue; }
    public int getColorHex() { return colorHex; }
    public String getSubtitle() { return subtitle; }

    /** Kept as a compatibility alias; this is a sale value, not a reward. */
    @Deprecated
    public long getReward() { return baseValue; }

    public static PlateRarity fromName(String value) {
        if (value == null) return COMMON;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return COMMON;
        }
    }
}
