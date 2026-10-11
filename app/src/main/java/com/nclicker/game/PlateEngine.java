package com.nclicker.game;

import org.json.JSONObject;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java gameplay facade. Rust performs rarity rolls, plate formatting,
 * country/region selection, category selection and reward balancing.
 *
 * The Java fallback is intentionally kept for Android Studio previews and
 * devices where the optional native binary has not been built.
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

    private static native String nativeGenerate(long seed, int luckLevel, int bonusLevel);

    public static boolean isNativeEngineAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static Plate preview() {
        return new Plate(
                "preview", "Россия", "RU", "🇷🇺", "Москва · 77",
                "А777ВС 77", "Гражданский", PlateRarity.COMMON, PlateRarity.COMMON.getReward()
        );
    }

    public static Plate roll(int luckLevel, int bonusLevel) {
        int luck = Math.max(0, Math.min(luckLevel, 60));
        int bonus = Math.max(0, Math.min(bonusLevel, 60));

        if (NATIVE_AVAILABLE) {
            try {
                long seed = SEQUENCE.addAndGet(0x9E3779B97F4A7C15L) ^ System.nanoTime();
                String payload = nativeGenerate(seed, luck, bonus);
                Plate plate = parseNativePlate(payload);
                if (plate != null) return plate;
            } catch (RuntimeException | UnsatisfiedLinkError ignored) {
                // Keep the game playable if the optional native library cannot run.
            }
        }
        return fallbackRoll(luck, bonus);
    }

    private static Plate parseNativePlate(String payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(payload);
            PlateRarity rarity = PlateRarity.fromName(json.optString("rarity", "COMMON"));
            return new Plate(
                    json.optString("id", "native-" + System.nanoTime()),
                    json.optString("country", "Неизвестно"),
                    json.optString("countryCode", "??"),
                    json.optString("flag", "🏁"),
                    json.optString("region", "Без региона"),
                    json.optString("number", "NC 000"),
                    json.optString("type", "Гражданский"),
                    rarity,
                    json.optLong("reward", rarity.getReward())
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Plate fallbackRoll(int luckLevel, int bonusLevel) {
        Random random = new Random(SEQUENCE.addAndGet(0x9E3779B97F4A7C15L) ^ System.nanoTime());
        String[][] countries = {
                {"Россия", "RU", "🇷🇺", "Москва · 77", "Санкт-Петербург · 78", "Татарстан · 16", "Краснодар · 23"},
                {"Украина", "UA", "🇺🇦", "Киев · AA", "Львов · BC", "Одесса · BH", "Днепр · AE"},
                {"Германия", "DE", "🇩🇪", "Берлин · B", "Гамбург · HH", "Мюнхен · M"},
                {"Япония", "JP", "🇯🇵", "Токио · 品川", "Осака · なにわ", "Йокогама · 横浜"},
                {"США", "US", "🇺🇸", "Калифорния · CA", "Нью-Йорк · NY", "Техас · TX"},
                {"Великобритания", "GB", "🇬🇧", "Лондон · LD", "Манчестер · MN", "Уэльс · CY"},
                {"Франция", "FR", "🇫🇷", "Париж · 75", "Лион · 69", "Марсель · 13"},
                {"ОАЭ", "AE", "🇦🇪", "Дубай · D", "Абу-Даби · AU", "Шарджа · SH"},
                {"Польша", "PL", "🇵🇱", "Варшава · WA", "Краков · KR", "Гданьск · GD"},
                {"Италия", "IT", "🇮🇹", "Рим · RM", "Милан · MI", "Турин · TO"}
        };

        long[] weights = weightedRarityWeights(luckLevel);
        long total = 0;
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
        String suffix = region.substring(region.lastIndexOf('·') + 1).trim();
        String number = fallbackNumber(random, country[1], suffix);
        long reward = (long) Math.min(Long.MAX_VALUE / 2.0,
                rarity.getReward() * (1.0 + Math.min(bonusLevel, 60) * 0.35));
        long id = SEQUENCE.incrementAndGet();

        return new Plate(
                country[1] + "-" + Long.toUnsignedString(id, 36),
                country[0], country[1], country[2], region, number, type, rarity, Math.max(1L, reward)
        );
    }

    private static long[] weightedRarityWeights(int luck) {
        double level = Math.min(luck, 60);
        return new long[] {
                Math.max(4_000L, (long) (60_000.0 / (1.0 + level * 0.075))),
                24_000L,
                (long) (12_000.0 * (1.0 + level * 0.035)),
                (long) (3_000.0 * (1.0 + level * 0.065)),
                (long) (800.0 * (1.0 + level * 0.09)),
                (long) (170.0 * (1.0 + level * 0.12)),
                (long) (30.0 * (1.0 + level * 0.16))
        };
    }

    private static String fallbackNumber(Random random, String code, String suffix) {
        String letters = "АВЕКМНОРСТУХ";
        switch (code) {
            case "RU":
                return "" + letters.charAt(random.nextInt(letters.length())) + digits(random, 3)
                        + letters.charAt(random.nextInt(letters.length()))
                        + letters.charAt(random.nextInt(letters.length())) + " " + suffix;
            case "UA": return suffix + " " + digits(random, 4) + " " + letters.charAt(random.nextInt(letters.length()));
            case "DE": return suffix + " · NC " + digits(random, 4);
            case "JP": return suffix + " 503 ね " + digits(random, 2) + "-" + digits(random, 2);
            case "US": return suffix + " · " + digits(random, 1) + "NCX" + digits(random, 3);
            case "GB": return "NC" + digits(random, 2) + " NCL";
            case "FR": return "NC-" + digits(random, 3) + "-ZZ";
            case "AE": return suffix + " · " + digits(random, 5);
            case "PL": return suffix + " " + digits(random, 3) + " NC";
            default: return "NC " + digits(random, 3) + " ZZ";
        }
    }

    private static String digits(Random random, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) value.append(random.nextInt(10));
        return value.toString();
    }
}
