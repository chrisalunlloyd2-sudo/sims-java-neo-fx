package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 2026-11-08 (roadmap step 61): ONE large-text setting for every Viper pop-up (Chris: BVD, 4 cm focal --
 * narrow short lines, big font). C:/Viper/config/ui_prefs.json, shared with MoeGUI's MoeUpdatesTab:
 *   {"large_text": true, "scale": 1.5, "line_chars": 38}
 * Missing file = large text ON (that is the need). Read at most every 2 s; never throws.
 */
public final class ViperUi {
    static final Path PREFS = Paths.get("C:\\Viper\\config\\ui_prefs.json");
    private static final ObjectMapper M = new ObjectMapper();
    private static long readAt = 0, mtime = -1;
    private static boolean large = true;
    private static double scale = 1.5;
    private static int lineChars = 38;

    private ViperUi() {}

    private static synchronized void load() {
        long now = System.currentTimeMillis();
        if (now - readAt < 2000) return;
        readAt = now;
        try {
            long m = Files.exists(PREFS) ? Files.getLastModifiedTime(PREFS).toMillis() : 0;
            if (m == mtime) return;
            mtime = m;
            if (m == 0) { large = true; scale = 1.5; lineChars = 38; return; }
            JsonNode j = M.readTree(Files.readAllBytes(PREFS));
            apply(j.path("large_text").asBoolean(true), j.path("scale").asDouble(1.5), j.path("line_chars").asInt(38));
        } catch (Exception e) { /* keep last values */ }
    }

    /** Pure setter, clamped, so tests need no file. */
    static synchronized void apply(boolean lg, double sc, int lc) {
        large = lg;
        scale = Math.max(1.0, Math.min(3.0, sc));
        lineChars = Math.max(20, Math.min(120, lc));
    }

    public static boolean large() { load(); return large; }
    public static double scale() { load(); return large ? scale : 1.0; }
    /** Font size in px for a base size. */
    public static int px(double base) { return (int) Math.round(base * scale()); }
    /** Wrap width in px for text of this base size: short lines in large mode, roomy otherwise. */
    public static double wrap(double base) {
        load();
        return large ? lineChars * px(base) * 0.62 : 9999;
    }

    /** Tray toggle. Writes the whole file (3 keys). */
    public static void setLarge(boolean on) {
        load();
        try {
            Files.createDirectories(PREFS.getParent());
            Files.write(PREFS, String.format(java.util.Locale.ROOT, "{\"large_text\": %s, \"scale\": %.2f, \"line_chars\": %d}%n",
                    on, scale, lineChars).getBytes(StandardCharsets.UTF_8));
            readAt = 0;
        } catch (Exception e) { System.out.println("[UI] prefs write failed: " + e); }
    }

    public static String line() { return "Large text: " + (large() ? "ON (click to turn off)" : "off (click to turn on)"); }
}
