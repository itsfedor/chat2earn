package com.nous.chat2earn;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    private final Chat2Earn plugin;
    private FileConfiguration config;

    public ConfigManager(Chat2Earn plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public String getGroqApiKey() {
        return config.getString("groq.api-key", "");
    }

    public String getGroqModel() {
        return config.getString("groq.model", "llama-3.1-8b-instant");
    }

    public int getGroqTimeoutSeconds() {
        return config.getInt("groq.timeout-seconds", 10);
    }

    public int getMaxPointsPerMessage() {
        return config.getInt("scoring.points-per-message-max", 4);
    }

    public double getDollarsPerPoint() {
        return config.getDouble("scoring.dollars-per-point", 0.10);
    }

    public double getMilestoneDollars() {
        return config.getDouble("scoring.milestone-dollars", 5.0);
    }

    public int getMinWords() {
        return config.getInt("filters.min-words", 3);
    }

    public int getMaxCyrillicPercent() {
        return config.getInt("filters.max-cyrillic-percent", 49);
    }

    public List<String> getMotivationMessages() {
        return config.getStringList("motivation");
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
