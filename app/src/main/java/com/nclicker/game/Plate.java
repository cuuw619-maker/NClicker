package com.nclicker.game;

/** Immutable in-game vehicle registration item. */
public final class Plate {
    private final String id;
    private final String country;
    private final String countryCode;
    private final String flag;
    private final String region;
    private final String number;
    private final String type;
    private final PlateRarity rarity;
    private final long marketValue;

    public Plate(
            String id,
            String country,
            String countryCode,
            String flag,
            String region,
            String number,
            String type,
            PlateRarity rarity,
            long marketValue
    ) {
        this.id = safe(id, "plate");
        this.country = safe(country, "Unknown");
        this.countryCode = safe(countryCode, "??");
        this.flag = safe(flag, "🏁");
        this.region = safe(region, "Unknown region");
        this.number = safe(number, "NC 000");
        this.type = safe(type, "Civilian");
        this.rarity = rarity == null ? PlateRarity.COMMON : rarity;
        this.marketValue = Math.max(1L, marketValue);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public String getId() { return id; }
    public String getCountry() { return country; }
    public String getCountryCode() { return countryCode; }
    public String getFlag() { return flag; }
    public String getRegion() { return region; }
    public String getNumber() { return number; }
    public String getType() { return type; }
    public PlateRarity getRarity() { return rarity; }
    public long getMarketValue() { return marketValue; }

    /** Compatibility alias for old save data and UI references. */
    @Deprecated
    public long getReward() { return marketValue; }
}
