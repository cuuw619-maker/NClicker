package com.nclicker.game;

/** UI metadata for each tier. Probabilities and rewards are calculated by Rust. */
public enum PlateRarity {
    COMMON("Обычный", 2L, 0xFF9BA9BD, "Стандартный выпуск"),
    UNCOMMON("Необычный", 8L, 0xFF75DCC5, "Интересная комбинация"),
    RARE("Редкий", 25L, 0xFF82B8FF, "Редкая серия"),
    EPIC("Эпический", 90L, 0xFFC29AFF, "Коллекционная находка"),
    LEGENDARY("Легендарный", 400L, 0xFFFFC976, "Особый выпуск"),
    MYTHIC("Мифический", 2500L, 0xFFFF86BB, "Почти невозможная находка"),
    SECRET("Секретный", 15000L, 0xFFFFF0A6, "Секрет коллекции");

    private final String title;
    private final long reward;
    private final int colorHex;
    private final String subtitle;

    PlateRarity(String title, long reward, int colorHex, String subtitle) {
        this.title = title;
        this.reward = reward;
        this.colorHex = colorHex;
        this.subtitle = subtitle;
    }

    public String getTitle() { return title; }
    public long getReward() { return reward; }
    public int getColorHex() { return colorHex; }
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
