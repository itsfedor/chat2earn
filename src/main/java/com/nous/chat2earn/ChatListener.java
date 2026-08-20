package com.nous.chat2earn;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {
    private final Chat2Earn plugin;
    private final ConfigManager config;
    private final AIScorer scorer;
    private final EconomyManager economy;
    private final MotivationManager motivation;

    // Anti-spam: per-player recent messages + cooldown timestamps
    private final Map<UUID, Deque<String>> recentMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastScoredTime = new ConcurrentHashMap<>();
    private static final int MAX_RECENT = 5;
    private static final long COOLDOWN_MS = 2000; // 2 seconds
    private static final double WORD_OVERLAP_THRESHOLD = 0.70; // 70%

    public ChatListener(Chat2Earn plugin, ConfigManager config, EconomyManager economy, MotivationManager motivation) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
        this.motivation = motivation;
        this.scorer = new AIScorer(config.getGroqApiKey(), config.getGroqModel(), config.getGroqTimeoutSeconds());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = extractPlainText(event.message());

        // Quick sync filters
        if (message.startsWith("/")) return;
        String[] words = message.trim().split("\\s+");
        if (words.length < config.getMinWords()) return;
        if (!LanguageDetector.isPassing(message, config.getMaxCyrillicPercent())) return;

        // Anti-spam: cooldown
        long now = System.currentTimeMillis();
        Long last = lastScoredTime.get(player.getUniqueId());
        if (last != null && (now - last) < COOLDOWN_MS) return;

        // Anti-spam: duplicate/similarity check
        if (isSpam(player.getUniqueId(), message, words)) {
            plugin.getLogger().info("[" + player.getName() + "] SPAM blocked: \"" + message + "\"");
            return;
        }

        // Record this message
        Deque<String> history = recentMessages.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        history.addLast(message);
        if (history.size() > MAX_RECENT) history.removeFirst();

        plugin.getLogger().info("[" + player.getName() + "] Chat: \"" + message + "\" — passed filters, scoring...");

        // Run async for AI call
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int score = scorer.score(message);

            if (score <= 0) {
                if (score < 0) plugin.getLogger().warning("AI call failed for " + player.getName());
                return;
            }

            final double dollars = score * config.getDollarsPerPoint();
            plugin.getLogger().info(String.format("[%s] Scored %d → $%.2f",
                player.getName(), score, dollars));

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                lastScoredTime.put(player.getUniqueId(), System.currentTimeMillis());

                double totalBefore = economy.getTotalEarned(player.getUniqueId());

                if (!economy.deposit(player, dollars)) {
                    plugin.getLogger().warning("Economy deposit failed for " + player.getName());
                    return;
                }

                // Report to EnglishProgression
                try {
                    Class<?> ep = Class.forName("com.nous.progression.EnglishProgression");
                    ep.getMethod("addEarnings", org.bukkit.entity.Player.class, double.class).invoke(null, player, dollars);
                } catch (Exception ignored) {}

                double totalAfter = economy.getTotalEarned(player.getUniqueId());
                double milestone = config.getMilestoneDollars();
                int milestonesBefore = (int)(totalBefore / milestone);
                int milestonesAfter = (int)(totalAfter / milestone);

                plugin.getLogger().info(String.format(
                    "[%s] total: $%.2f → $%.2f | milestones: %d → %d",
                    player.getName(), totalBefore, totalAfter, milestonesBefore, milestonesAfter
                ));

                if (milestonesAfter > milestonesBefore) {
                    double earnedThisBlock = totalAfter - (milestonesBefore * milestone);
                    Component msg = motivation.getRandomMessage(earnedThisBlock);
                    player.sendMessage(msg);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    plugin.getLogger().info(String.format(
                        "[%s] MILESTONE! +$%.2f", player.getName(), earnedThisBlock
                    ));
                }
            });
        });
    }

    /**
     * Check if message is spam: exact duplicate or high word overlap with recent messages.
     */
    private boolean isSpam(UUID uuid, String message, String[] words) {
        Deque<String> history = recentMessages.get(uuid);
        if (history == null || history.isEmpty()) return false;

        String normalized = message.toLowerCase().trim();

        for (String prev : history) {
            String prevNorm = prev.toLowerCase().trim();

            // Exact duplicate
            if (normalized.equals(prevNorm)) return true;

            // High word overlap with previous message
            Set<String> prevWords = new HashSet<>(Arrays.asList(prevNorm.split("\\s+")));
            Set<String> currWordsSet = new HashSet<>(Arrays.asList(normalized.split("\\s+")));

            if (prevWords.isEmpty() || currWordsSet.isEmpty()) continue;

            Set<String> intersection = new HashSet<>(prevWords);
            intersection.retainAll(currWordsSet);

            int unionSize = prevWords.size() + currWordsSet.size() - intersection.size();
            if (unionSize == 0) continue;

            double overlap = (double) intersection.size() / unionSize;
            if (overlap > WORD_OVERLAP_THRESHOLD) return true;
        }

        return false;
    }

    private String extractPlainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }
}
