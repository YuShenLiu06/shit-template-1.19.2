package com.lys.scoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("shitmod_scoreboard.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ScoreboardConfig instance;

    public boolean globalEnabled = true;
    public Map<String, Boolean> playerVisibility = new HashMap<>();

    public static ScoreboardConfig getConfig() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static ScoreboardConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return GSON.fromJson(Files.newBufferedReader(CONFIG_PATH), ScoreboardConfig.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        instance = new ScoreboardConfig();
        return instance;
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerVisibility(UUID uuid, boolean visible) {
        playerVisibility.put(uuid.toString(), visible);
        save();
    }

    public boolean getPlayerVisibility(UUID uuid) {
        return playerVisibility.getOrDefault(uuid.toString(), true);
    }
}