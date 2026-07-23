package com.velorise.emc;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.velorise.emc.common.GameplayConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Loads, normalizes and synchronizes the server-authoritative gameplay balance file. */
public final class GameplayConfigManager {
    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("velorise_emc").resolve("gameplay.json");
    private static final int MAX_SYNC_JSON_LENGTH = 262_144;
    private static final DateTimeFormatter BROKEN_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private GameplayConfigManager() {}

    public static synchronized ReloadResult loadServer() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.notExists(CONFIG_PATH)) {
                GameplayConfig.resetToDefaults();
                writeNormalized(GameplayConfig.current());
                return new ReloadResult(true, "Created gameplay.json with default server settings.");
            }

            GameplayConfig.Values loaded;
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, GameplayConfig.Values.class);
            }
            GameplayConfig.install(loaded);
            writeNormalized(GameplayConfig.current());
            return new ReloadResult(true, "Reloaded and normalized gameplay.json.");
        } catch (JsonParseException | IllegalStateException ex) {
            return recoverBrokenFile("Invalid JSON: " + safeMessage(ex));
        } catch (IOException ex) {
            return new ReloadResult(false, "Could not read gameplay.json: " + safeMessage(ex));
        } catch (RuntimeException ex) {
            return recoverBrokenFile("Invalid gameplay values: " + safeMessage(ex));
        }
    }

    private static ReloadResult recoverBrokenFile(String reason) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                String name = "gameplay.broken-" + LocalDateTime.now().format(BROKEN_SUFFIX) + ".json";
                Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING);
            }
            GameplayConfig.resetToDefaults();
            writeNormalized(GameplayConfig.current());
            return new ReloadResult(false, reason + "; the broken file was backed up and defaults were restored.");
        } catch (IOException recoveryError) {
            GameplayConfig.resetToDefaults();
            return new ReloadResult(false, reason + "; recovery also failed: " + safeMessage(recoveryError));
        }
    }

    public static synchronized String serverJson() {
        String json = GSON.toJson(GameplayConfig.current());
        if (json.length() > MAX_SYNC_JSON_LENGTH) {
            throw new IllegalStateException("Normalized gameplay config is too large to synchronize");
        }
        return json;
    }

    public static synchronized boolean applyClientJson(String json) {
        if (json == null || json.isBlank() || json.length() > MAX_SYNC_JSON_LENGTH) return false;
        try {
            GameplayConfig.install(GSON.fromJson(json, GameplayConfig.Values.class));
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static void writeNormalized(GameplayConfig.Values values) throws IOException {
        Path temp = CONFIG_PATH.resolveSibling("gameplay.json.tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(values, writer);
        }
        try {
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    public record ReloadResult(boolean success, String message) {}
}
