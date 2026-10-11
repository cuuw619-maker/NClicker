package com.nclicker.game;

import org.json.JSONObject;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java gameplay facade. Rust generates rarity rolls, country/region, formats
 * and base market value. Java owns the persisted plate object and sale offer.
 */
public final class PlateEngine {
    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime());
    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("plate_engine");
            loaded = true;
        } catch (UnsatisfiedLinkError | SecurityException error) {
            loaded = false;
        }
        NATIVE_AVAILABLE = loaded;
    }

    private PlateEngine() {
    }

    private static native String nativeGenerate(long seed, int luckLevel, int brokerLevel);

    public static boolean isNativeEngineAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static Plate preview() {
        return new Plate(
                "preview", "Россия", "RU", "🇷🇺", "Москва · 77",
                "А777ВС 77", "Гражданский", PlateRarity.COMMON, 100L
        );
    }

    public static Plate roll(int luckLevel, int brokerLevel) {
        int luck = Math.max(0, Math.min(luckLevel, GameEconomy.MAX_LUCK_LEVEL));
        int broker = Math.max(0, Math.min(brokerLevel, GameEconomy.MAX_BROKER_LEVEL));
        if (NATIVE_AVAILABLE) {
            try {
                long seed = SEQUENCE.addAndGet(0x9E3779B97F4A7C15L) ^ System.nanoTime();
                Plate plate = parseNativePlate(nativeGenerate(seed, luck, broker));
                if (plate != null) return plate;
            } catch (RuntimeException | UnsatisfiedLinkError ignored) {
                // Fall back so preview/debug runs remain playable.
            }
        }
        return fallbackRoll(luck);
    }

    public static long salePrice(Plate plate, int brokerLevel) {
        return GameEconomy.salePrice(plate, brokerLevel);
    }

    private static Plate parseNativePlate(String payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(payload);
            PlateRarity rarity = PlateRarity.fromName(json.optString("rarity", "COMMON"));
            long marketValue = json.optLong("marketValue", rarity.getBaseValue());
            return new Plate(
                    json.optString("id", "native-" + System.nanoTime()),
                    json.optString("country", "Неизвестно"),
                    json.optString("countryCode", "??"),
                    json.optString("flag", "🏁"),
                    json.optString("region", "Без региона"),
                    json.optString("number", "NC 000"),
                    json.optString("type", "Гражданский"),
                    rarity,
                    Math.max(1L, marketValue)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Plate fallbackRoll(int luckLevel) {
        Random random = new Random(SEQUENCE.addAndGet(0x9E3779B97F4A7C15L) ^ System.nanoTime());
        String[][] countries = {
                {"Россия", "RU", "🇷🇺", "Москва · 77", "Санкт-Петербург · 78", "Татарстан · 16", "Краснодар · 23", "Свердловская область · 66"},
                {"Украина", "UA", "🇺🇦", "Киев · AA", "Львов · BC", "Одесса · BH", "Харьков · AX", "Днепр · AE"},
                {"Германия", "DE", "🇩🇪", "Берлин · B", "Гамбург · HH", "Мюнхен · M", "Кёльн · K"},
                {"Япония", "JP", "🇯🇵", "Токио · 品川", "Осака · なにわ", "Йокогама · 横浜"},
                {"США", "US", "🇺🇸", "Калифорния · CA", "Нью-Йорк · NY", "Техас · TX", "Флорида · FL"},
                {"Великобритания", "GB", "🇬🇧", "Лондон · LD", "Манчестер · MN", "Уэльс · CY"},
                {"Франция", "FR", "🇫🇷", "Париж · 75", "Лион · 69", "Марсель · 13"},
                {"ОАЭ", "AE", "🇦🇪", "Дубай · D", "Абу-Даби · AU", "Шарджа · SH"},
                {"Польша", "PL", "🇵🇱", "Варшава · WA", "Краков · KR", "Гданьск · GD"},
                {"Италия", "IT", "🇮🇹", "Рим · RM", "Милан · MI", "Турин · TO"}
        };

        long[] weights = rarityWeights(luckLevel);
        long total = 0L;
        for (long weight : weights) total += weight;
        long ticket = Math.floorMod(random.nextLong(), total);
        int rarityIndex = 0;
        for (; rarityIndex < weights.length - 1; rarityIndex++) {
            if (ticket < weights[rarityIndex]) break;
            ticket -= weights[rarityIndex];
        }
        PlateRarity rarity = PlateRarity.values()[rarityIndex];

        int typeRoll = random.nextInt(10_000);
        String type;
        if (rarity.ordinal() >= PlateRarity.LEGENDARY.ordinal() && typeRoll < 1_200) type = "Чёрная коллекционная серия";
        else if (typeRoll < 180) type = "ФСБ · игровая спецсерия";
        else if (typeRoll < 700) type = "Полиция · игровой выпуск";
        else if (typeRoll < 1_300) type = "Чёрная серия";
        else if (typeRoll < 2_400) type = "Такси";
        else if (typeRoll < 3_200) type = "Дипломатический";
        else if (typeRoll < 4_200) type = "Ретро";
        else type = "Гражданский";

        String[] country = countries[type.startsWith("ФСБ") ? 0 : random.nextInt(countries.length)];
        String region = country[3 + random.nextInt(country.length - 3)];
        String code = region.substring(region.lastIndexOf('·') + 1).trim();
        String number = fallbackNumber(random, country[1], code);
        long baseValue = rarity.getBaseValue();
        double categoryMultiplier = type.contains("Чёрная") ? 1.35
                : type.startsWith("ФСБ") ? 1.25
                : type.startsWith("Полиция") ? 1.20
                : type.equals("Такси") ? 0.90
                : type.equals("Дипломатический") ? 1.60
                : type.equals("Ретро") ? 1.40 : 1.0;
        long marketValue = Math.max(50L, Math.round(baseValue * categoryMultiplier));
        long id = SEQUENCE.incrementAndGet();

        return new Plate(
                country[1] + "-" + Long.toUnsignedString(id, 36),
                country[0], country[1], country[2], region, number, type, rarity, marketValue
        );
    }

    private static long[] rarityWeights(int luck) {
        double level = Math.min(luck, GameEconomy.MAX_LUCK_LEVEL);
        return new long[] {
                Math.max(4_000L, (long) (60_000.0 / (1.0 + level * 0.10))),
                24_000L,
                (long) (12_000.0 * (1.0 + level * 0.06)),
                (long) (3_000.0 * (1.0 + level * 0.10)),
                (long) (800.0 * (1.0 + level * 0.13)),
                (long) (170.0 * (1.0 + level * 0.16)),
                (long) (30.0 * (1.0 + level * 0.20))
        };
    }

    private static String fallbackNumber(Random random, String countryCode, String regionCode) {
        switch (countryCode) {
            case "RU":
                return ruLetter(random) + digits(random, 3) + ruLetter(random) + ruLetter(random) + " " + regionCode;
            case "UA":
                return regionCode + " " + digits(random, 4) + " " + latinLetters(random, 2);
            case "DE":
                return regionCode + " " + latinLetters(random, 2) + " " + digits(random, 4);
            case "JP":
                return "503 ね " + digits(random, 2) + "-" + digits(random, 2);
            case "US":
                if ("NY".equals(regionCode) || "TX".equals(regionCode) || "FL".equals(regionCode)) {
                    return latinLetters(random, 3) + "-" + digits(random, 4);
                }
                return digits(random, 1) + latinLetters(random, 3) + digits(random, 3);
            case "GB":
                return latinLetters(random, 2) + digits(random, 2) + " " + latinLetters(random, 3);
            case "FR":
                return latinLetters(random, 2) + "-" + digits(random, 3) + "-" + latinLetters(random, 2);
            case "AE":
                return digits(random, 5);
            case "PL":
                return regionCode + " " + digits(random, 3) + latinLetters(random, 2);
            case "IT":
                return latinLetters(random, 2) + " " + digits(random, 3) + " " + latinLetters(random, 2);
            default:
                return "NC " + digits(random, 3) + " ZZ";
        }
    }

    private static String ruLetter(Random random) {
        String letters = "АВЕКМНОРСТУХ";
        return String.valueOf(letters.charAt(random.nextInt(letters.length())));
    }

    private static String latinLetters(Random random, int count) {
        String letters = "ABCDEFGHJKLMNPRSTUVWXYZ";
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) value.append(letters.charAt(random.nextInt(letters.length())));
        return value.toString();
    }

    private static String digits(Random random, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) value.append(random.nextInt(10));
        return value.toString();
    }
}
