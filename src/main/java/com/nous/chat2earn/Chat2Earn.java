package com.nous.chat2earn;

import org.bukkit.plugin.java.JavaPlugin;

public final class Chat2Earn extends JavaPlugin {

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private MotivationManager motivationManager;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);

        String apiKey = configManager.getGroqApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            getLogger().warning("Groq API key is not set! Edit plugins/Chat2Earn/config.yml");
        } else {
            getLogger().info("Groq API key loaded (" + apiKey.substring(0, Math.min(8, apiKey.length())) + "...)");
        }

        this.economyManager = new EconomyManager(this);

        if (!economyManager.isEconomyReady()) {
            getLogger().severe("Vault economy not found! Is VaultUnlocked installed and enabled?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Economy provider: " + economyManager.getEconomyName());

        this.motivationManager = new MotivationManager(configManager.getMotivationMessages());
        getLogger().info("Loaded " + configManager.getMotivationMessages().size() + " motivation variants");

        this.chatListener = new ChatListener(this, configManager, economyManager, motivationManager);
        getServer().getPluginManager().registerEvents(chatListener, this);

        getLogger().info("Chat2Earn v1.0.0 enabled! Model: " + configManager.getGroqModel() +
            " | $" + String.format("%.2f", configManager.getDollarsPerPoint()) + "/point" +
            " | $" + String.format("%.2f", configManager.getMilestoneDollars()) + " milestone" +
            " | min " + configManager.getMinWords() + " words" +
            " | max " + configManager.getMaxCyrillicPercent() + "% cyrillic");
    }

    @Override
    public void onDisable() {
        getLogger().info("Chat2Earn disabled!");
    }
}
