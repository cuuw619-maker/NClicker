package com.nclicker.engine;

import java.util.Locale;
import java.util.regex.Pattern;

/** Java helper shared by the Kotlin Compose UI. */
public final class PlateFormat {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private PlateFormat() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return WHITESPACE.matcher(value.trim()).replaceAll(" ").toUpperCase(Locale.ROOT);
    }

    public static String countryLine(String country, String region) {
        String safeCountry = country == null ? "" : country.trim();
        String safeRegion = region == null ? "" : region.trim();
        if (safeRegion.isEmpty()) return safeCountry;
        if (safeCountry.isEmpty()) return safeRegion;
        return safeCountry + " · " + safeRegion;
    }
}
