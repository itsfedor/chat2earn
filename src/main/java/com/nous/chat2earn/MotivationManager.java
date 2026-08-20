package com.nous.chat2earn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Random;

public class MotivationManager {
    private final List<String> messages;
    private final Random random = new Random();
    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public MotivationManager(List<String> messages) {
        this.messages = messages;
    }

    /**
     * Get a random motivational Component with color codes parsed.
     * @param earned dollars earned in this milestone block
     */
    public Component getRandomMessage(double earned) {
        if (messages == null || messages.isEmpty()) {
            return SERIALIZER.deserialize(
                String.format("&6You earned &a+$%.2f &6for your English!", earned)
            );
        }

        String template = messages.get(random.nextInt(messages.size()));
        String formatted = String.format(template, earned);
        return SERIALIZER.deserialize(formatted);
    }
}
