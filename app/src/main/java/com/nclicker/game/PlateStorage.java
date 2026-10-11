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
                long value = item.has("marketValue")
                        ? item.optLong("marketValue", rarity.getBaseValue())
                        : rarity.getBaseValue();
                result.add(new Plate(
                        item.optString("id", "saved-" + index),
                        item.optString("country", "Неизвестно"),
                        item.optString("countryCode", "??"),
                        item.optString("flag", "🏁"),
                        item.optString("region", "Без региона"),
                        item.optString("number", "NC 000"),
                        item.optString("type", "Гражданский"),
                        rarity,
                        value
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
                try {
                    JSONObject item = new JSONObject();
                    item.put("id", plate.getId());
                    item.put("country", plate.getCountry());
                    item.put("countryCode", plate.getCountryCode());
                    item.put("flag", plate.getFlag());
                    item.put("region", plate.getRegion());
                    item.put("number", plate.getNumber());
                    item.put("type", plate.getType());
                    item.put("rarity", plate.getRarity().name());
                    item.put("marketValue", plate.getMarketValue());
                    array.put(item);
                } catch (Exception ignored) {
                    // Keep the rest of the serializable collection.
                }
            }
        }
        preferences.edit().putString(KEY, array.toString()).apply();
    }
}
