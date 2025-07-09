package com.lys.scoreboard;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardDataStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Map<String, Integer>> scoreData = new HashMap<>();
    private static Path storagePath;

    public static void load(MinecraftServer server) {
        storagePath = server.getRunDirectory().toPath().resolve("shitmod_scoredata.json");

        if (Files.exists(storagePath)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(storagePath)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> playerEntry : json.entrySet()) {
                    UUID uuid = UUID.fromString(playerEntry.getKey());
                    Map<String, Integer> scores = new HashMap<>();

                    JsonObject playerScores = playerEntry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> scoreEntry : playerScores.entrySet()) {
                        scores.put(scoreEntry.getKey(), scoreEntry.getValue().getAsInt());
                    }

                    scoreData.put(uuid, scores);
                }
            } catch (IOException | JsonParseException e) {
                System.err.println("Failed to load score data: " + e.getMessage());
            }
        }
    }

    public static void save(MinecraftServer server) {
        if (storagePath == null) {
            storagePath = server.getRunDirectory().toPath().resolve("shitmod_scoredata.json");
        }

        try {
            JsonObject json = new JsonObject();
            for (Map.Entry<UUID, Map<String, Integer>> playerEntry : scoreData.entrySet()) {
                JsonObject playerScores = new JsonObject();
                for (Map.Entry<String, Integer> scoreEntry : playerEntry.getValue().entrySet()) {
                    playerScores.addProperty(scoreEntry.getKey(), scoreEntry.getValue());
                }
                json.add(playerEntry.getKey().toString(), playerScores);
            }
            Files.writeString(storagePath, GSON.toJson(json));
        } catch (IOException e) {
            System.err.println("Failed to save score data: " + e.getMessage());
        }
    }

    public static int getPlayerScore(UUID uuid, String boardType) {
        Map<String, Integer> playerScores = scoreData.get(uuid);
        if (playerScores == null) return 0;
        return playerScores.getOrDefault(boardType, 0);
    }

    public static void setPlayerScore(UUID uuid, String boardType, int value) {
        Map<String, Integer> playerScores = scoreData.computeIfAbsent(uuid, k -> new HashMap<>());
        playerScores.put(boardType, value);
    }

    public static void resetPlayerScore(UUID uuid, String boardType) {
        Map<String, Integer> playerScores = scoreData.get(uuid);
        if (playerScores != null) {
            playerScores.remove(boardType);
        }
    }
}