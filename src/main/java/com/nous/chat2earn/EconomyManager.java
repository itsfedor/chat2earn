package com.nous.chat2earn;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class EconomyManager {
    private final Chat2Earn plugin;
    private Economy economy;
    private final File dataFile;
    private YamlConfiguration data;

    public EconomyManager(Chat2Earn plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadEconomy();
        loadData();
    }

    private void loadEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create playerdata.yml");
            }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isEconomyReady() {
        return economy != null;
    }

    public String getEconomyName() {
        return economy != null ? economy.getName() : "none";
    }

    /**
     * Deposit dollars to player and update Chat2Earn total.
     * Must be called from main thread.
     */
    public boolean deposit(OfflinePlayer player, double dollars) {
        if (economy == null) return false;

        economy.depositPlayer(player, dollars);

        UUID uuid = player.getUniqueId();
        double current = data.getDouble(uuid.toString() + ".total-earned", 0.0);
        data.set(uuid.toString() + ".total-earned", current + dollars);
        saveData();
        return true;
    }

    /**
     * Get total dollars earned through Chat2Earn.
     */
    public double getTotalEarned(UUID uuid) {
        return data.getDouble(uuid.toString() + ".total-earned", 0.0);
    }

    /**
     * Get the next milestone for this player (e.g., $5, $10, $15...).
     */
    public double getNextMilestone(UUID uuid, double milestoneDollars) {
        double total = getTotalEarned(uuid);
        return Math.floor(total / milestoneDollars) * milestoneDollars + milestoneDollars;
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml");
        }
    }
}
