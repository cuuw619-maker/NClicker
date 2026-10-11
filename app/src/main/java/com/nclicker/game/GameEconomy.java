package com.nclicker.game;

/** Centralized prices, upgrade caps and storage capacity for the game economy. */
public final class GameEconomy {
    public static final long STARTING_BALANCE = 3_000L;
    public static final int INITIAL_CAPACITY = 30;
    public static final int MAX_CAPACITY = 150;
    public static final int MAX_LUCK_LEVEL = 12;
    public static final int MAX_BROKER_LEVEL = 10;
    public static final int MAX_GARAGE_LEVEL = 6;

    private GameEconomy() {
    }

    public static int inventoryCapacity(int garageLevel) {
        int level = Math.max(0, Math.min(garageLevel, MAX_GARAGE_LEVEL));
        return Math.min(MAX_CAPACITY, INITIAL_CAPACITY + level * 20);
    }

    public static long upgradeCost(String upgrade, int currentLevel) {
        int level = Math.max(0, currentLevel);
        switch (upgrade) {
            case "luck":
                return scaledCost(500L, 1.62, level, MAX_LUCK_LEVEL);
            case "broker":
                return scaledCost(400L, 1.58, level, MAX_BROKER_LEVEL);
            case "garage":
                return scaledCost(600L, 1.75, level, MAX_GARAGE_LEVEL);
            default:
                throw new IllegalArgumentException("Unknown upgrade: " + upgrade);
        }
    }

    private static long scaledCost(long base, double growth, int level, int maxLevel) {
        if (level >= maxLevel) return Long.MAX_VALUE;
        double result = base * Math.pow(growth, level);
        return result >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(result);
    }

    public static long salePrice(Plate plate, int brokerLevel) {
        int level = Math.max(0, Math.min(brokerLevel, MAX_BROKER_LEVEL));
        double multiplier = 1.0 + level * 0.12;
        double result = plate.getMarketValue() * multiplier;
        return result >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, Math.round(result));
    }
}
