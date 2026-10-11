package com.nclicker.game;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local persistence for the Java game model. */
public final class PlateStorage {
    private static final String KEY = "collection_json";
    private static final int MAX_ITEMS = 250;

    private PlateStorage() {
    }

    public static List<Plate> load(SharedPreferences preferences) {
        String raw = preferences.getString(KEY, "[]");
        if (raw == null) raw = "[]";
        try {
            JSONArray array = new JSONArray(raw);
            List<Plate> result = new ArrayList<>(Math.min(array.length(), MAX_ITEMS));
            for (int index = 0; index < array.length() && result.size() < MAX_ITEMS; index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) continue;
                PlateRarity rarity = PlateRarity.fromName(item.optString("rarity", "COMMON"));
                result.add(new Plate(
                        item.optString("id", "saved-" + index),
                        item.optString("country", "Неизвестно"),
                        item.optString("countryCode", "??"),
                        item.optString("flag", "🏁"),
                        item.optString("region", "Без региона"),
                        item.optString("number", "NC 000"),
                        item.optString("type", "Гражданский"),
                        rarity,
                        item.optLong("reward", rarity.getReward())
                ));
            }
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public static void save(SharedPreferences preferences, List<Plate> plates) {
        JSONArray array = new JSONArray();
        if (plates != null) {
            int count = Math.min(plates.size(), MAX_ITEMS);
            for (int i = 0; i < count; i++) {
                Plate plate = plates.get(i);
                array.put(new JSONObject()
                        .put("id", plate.getId())
                        .put("country", plate.getCountry())
                        .put("countryCode", plate.getCountryCode())
                        .put("flag", plate.getFlag())
                        .put("region", plate.getRegion())
                        .put("number", plate.getNumber())
                        .put("type", plate.getType())
                        .put("rarity", plate.getRarity().name())
                        .put("reward", plate.getReward()));
            }
        }
        preferences.edit().putString(KEY, array.toString()).apply();
    }
}
