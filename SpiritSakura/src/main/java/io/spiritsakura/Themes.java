package io.spiritsakura;

import java.util.LinkedHashMap;
import java.util.Map;

/** Available colorways. The id matches packs/&lt;id&gt;.zip inside the plugin jar. */
public final class Themes {
    public static final String DEFAULT = "crimson";
    private static final Map<String, String> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("crimson", "#e0353a");
        COLORS.put("ember", "#ff8a1f");
        COLORS.put("emerald", "#2fd06c");
        COLORS.put("abyss", "#1fd0e0");
        COLORS.put("royal", "#4f8bff");
        COLORS.put("frost", "#9fdcf5");
        COLORS.put("obsidian", "#8a8a99");
        COLORS.put("ivory", "#eee6cc");
        COLORS.put("venom", "#8be01f");
        COLORS.put("solar", "#ffd21a");
    }

    private Themes() {}

    public static Map<String, String> all() {
        return COLORS;
    }

    public static boolean exists(String id) {
        return COLORS.containsKey(id);
    }

    public static String color(String id) {
        return COLORS.getOrDefault(id, COLORS.get(DEFAULT));
    }
}
