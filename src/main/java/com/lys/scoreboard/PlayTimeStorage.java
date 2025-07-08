package com.lys.scoreboard;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayTimeStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 存储总时间（秒）
    private static final Map<UUID, Long> playTimeMap = new HashMap<>();
    private static Path storagePath;

    public static void load(MinecraftServer server) {
        storagePath = server.getRunDirectory().toPath()
                .resolve("shitmod_playtime.json");

        if (Files.exists(storagePath)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(storagePath)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    long seconds = entry.getValue().getAsLong();
                    playTimeMap.put(uuid, seconds);
                }
            } catch (IOException | JsonParseException e) {
                System.err.println("Failed to load play time data: " + e.getMessage());
            }
        }
    }

    public static void save(MinecraftServer server) {
        if (storagePath == null) {
            storagePath = server.getRunDirectory().toPath()
                    .resolve("shitmod_playtime.json");
        }

        try {
            JsonObject json = new JsonObject();
            for (Map.Entry<UUID, Long> entry : playTimeMap.entrySet()) {
                json.addProperty(entry.getKey().toString(), entry.getValue());
            }
            Files.writeString(storagePath, GSON.toJson(json));
        } catch (IOException e) {
            System.err.println("Failed to save play time data: " + e.getMessage());
        }
    }

    // 获取总时间（秒）
    public static long getPlayTimeSeconds(UUID uuid) {
        return playTimeMap.getOrDefault(uuid, 0L);
    }

    // 添加时间（秒）
    public static void addPlayTimeSeconds(UUID uuid, long seconds) {
        long current = getPlayTimeSeconds(uuid);
        playTimeMap.put(uuid, current + seconds);
    }

    // 重置玩家时间
    public static void resetPlayTime(UUID uuid) {
        playTimeMap.put(uuid, 0L);
    }
}