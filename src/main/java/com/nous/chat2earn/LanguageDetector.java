package com.nous.chat2earn;

public class LanguageDetector {

    /**
     * Returns true if the message passes the cyrillic check
     * (cyrillic characters are &lt;= maxCyrillicPercent of all letters).
     */
    public static boolean isPassing(String message, int maxCyrillicPercent) {
        int totalLetters = 0;
        int cyrillicLetters = 0;

        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                totalLetters++;
                if (isCyrillic(c)) {
                    cyrillicLetters++;
                }
            }
        }

        if (totalLetters == 0) return false;

        int cyrillicPercent = (cyrillicLetters * 100) / totalLetters;
        return cyrillicPercent <= maxCyrillicPercent;
    }

    private static boolean isCyrillic(char c) {
        return (c >= 0x0400 && c <= 0x04FF) || // Cyrillic block
               (c >= 0x0500 && c <= 0x052F);    // Cyrillic supplement
    }
}
