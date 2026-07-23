package com.velorise.emc;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Client-only mini-HUD preferences shared by both loaders. */
public final class EmcHudPreferences {
    private static final Path FILE = Path.of("config", "velorise_emc", "hud.properties");
    private static final float[] SCALES = {0.75F, 1.00F, 1.25F, 1.50F};
    private static boolean loaded;
    private static boolean showLookedAtBlockEmc;
    private static boolean showStackEmc = true;
    private static boolean showWorldTier = true;
    private static int scaleIndex = 1;
    private static double relativeX;
    private static double relativeY;

    private EmcHudPreferences() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        Properties properties = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream input = Files.newInputStream(FILE)) {
                properties.load(input);
            } catch (Exception ignored) {}
        }
        showLookedAtBlockEmc = Boolean.parseBoolean(properties.getProperty("showLookedAtBlockEmc", "false"));
        showStackEmc = Boolean.parseBoolean(properties.getProperty("showStackEmc", "true"));
        showWorldTier = Boolean.parseBoolean(properties.getProperty("showWorldTier", "true"));
        scaleIndex = clamp(parseInt(properties.getProperty("scale", "1"), 1), 0, SCALES.length - 1);
        relativeX = clamp(parseDouble(properties.getProperty("relativeX", "0"), 0.0D), 0.0D, 1.0D);
        relativeY = clamp(parseDouble(properties.getProperty("relativeY", "0"), 0.0D), 0.0D, 1.0D);
    }

    public static synchronized void save() {
        load();
        try {
            Files.createDirectories(FILE.getParent());
            Properties properties = new Properties();
            properties.setProperty("showLookedAtBlockEmc", Boolean.toString(showLookedAtBlockEmc));
            properties.setProperty("showStackEmc", Boolean.toString(showStackEmc));
            properties.setProperty("showWorldTier", Boolean.toString(showWorldTier));
            properties.setProperty("scale", Integer.toString(scaleIndex));
            properties.setProperty("relativeX", Double.toString(relativeX));
            properties.setProperty("relativeY", Double.toString(relativeY));
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Velorise EMC client HUD settings");
            }
        } catch (Exception ignored) {}
    }

    public static boolean showLookedAtBlockEmc() { load(); return showLookedAtBlockEmc; }
    public static void setShowLookedAtBlockEmc(boolean value) { load(); showLookedAtBlockEmc = value; }
    public static boolean showStackEmc() { load(); return showStackEmc; }
    public static void setShowStackEmc(boolean value) { load(); showStackEmc = value; }
    public static boolean showWorldTier() { load(); return showWorldTier; }
    public static void setShowWorldTier(boolean value) { load(); showWorldTier = value; }
    public static float scale() { load(); return SCALES[scaleIndex]; }
    public static int scalePercent() { return Math.round(scale() * 100.0F); }

    public static void cycleScale() {
        load();
        scaleIndex = (scaleIndex + 1) % SCALES.length;
    }

    public static int pixelX(int screenWidth, int scaledHudWidth) {
        load();
        return (int)Math.round(relativeX * Math.max(0, screenWidth - scaledHudWidth));
    }

    public static int pixelY(int screenHeight, int scaledHudHeight) {
        load();
        return (int)Math.round(relativeY * Math.max(0, screenHeight - scaledHudHeight));
    }

    public static void setPixelPosition(int x, int y, int screenWidth, int screenHeight,
                                        int scaledHudWidth, int scaledHudHeight) {
        load();
        int availableX = Math.max(0, screenWidth - scaledHudWidth);
        int availableY = Math.max(0, screenHeight - scaledHudHeight);
        relativeX = availableX == 0 ? 0.0D : clamp((double)x / availableX, 0.0D, 1.0D);
        relativeY = availableY == 0 ? 0.0D : clamp((double)y / availableY, 0.0D, 1.0D);
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { return fallback; }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
