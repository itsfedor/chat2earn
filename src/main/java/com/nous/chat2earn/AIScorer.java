package com.nous.chat2earn;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIScorer {
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT =
            "You are an English writing evaluator. Analyze the chat message and score it on 4 criteria. " +
            "Each criterion is worth 1 point (total max 4 points).\n\n" +
            "Criteria:\n" +
            "1. Correctness (1 point): Message makes logical sense in English, no major meaning errors.\n" +
            "2. Length (1 point): Message is substantial (not just 'yes' or 'ok'; has meaningful content).\n" +
            "3. Grammar (1 point): No major grammatical errors (ignore minor typos if meaning is clear).\n" +
            "4. Punctuation (1 point): Proper use of periods, commas, capitalization.\n\n" +
            "Respond with ONLY a JSON object, nothing else: " +
            "{\"correctness\": 0 or 1, \"length\": 0 or 1, \"grammar\": 0 or 1, \"punctuation\": 0 or 1}";

    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public AIScorer(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * Score an English chat message via Groq API.
     * @return total score 0-4, or -1 on error
     */
    public int score(String message) {
        try {
            String requestBody = buildRequestBody(message);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return -1;
            }

            return parseScore(response.body());

        } catch (Exception e) {
            return -1;
        }
    }

    private String buildRequestBody(String message) {
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "Message to evaluate: \"" + message + "\"");

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", GSON.toJsonTree(new JsonObject[]{systemMsg, userMsg}));
        body.addProperty("temperature", 0.0);
        body.addProperty("max_tokens", 50);

        return GSON.toJson(body);
    }

    private int parseScore(String responseBody) {
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            String content = root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();

            // Extract JSON from response (strip markdown if present)
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            }

            JsonObject scores = GSON.fromJson(content, JsonObject.class);
            int total = 0;
            total += getIntSafe(scores, "correctness");
            total += getIntSafe(scores, "length");
            total += getIntSafe(scores, "grammar");
            total += getIntSafe(scores, "punctuation");

            return Math.min(total, 4);
        } catch (Exception e) {
            return -1;
        }
    }

    private int getIntSafe(JsonObject obj, String key) {
        if (obj.has(key)) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
