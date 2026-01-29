package com.abhinav.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizService {

    private String getApiKey() {
        // 1. Try Environment Variable (Railway/Cloud)
        String envKey = System.getenv("gemini_api_key");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }

        // 2. Fallback to local file (Local Config)
        try {
            String key = Files.readString(Path.of("gemini_api_key.txt")).trim();
            if (key.contains("PASTE_YOUR_GEMINI_API_KEY_HERE") || key.isEmpty()) {
                System.err.println("API Key file contains placeholder or is empty.");
                return null;
            }
            return key;
        } catch (IOException e) {
            System.err.println("Could not read gemini_api_key.txt or find env var 'gemini_api_key': " + e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> generateQuiz(List<String> topics, int count) {
        List<Map<String, Object>> questions = new ArrayList<>();

        if (topics == null || topics.isEmpty()) {
            return questions;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("topic", "Setup");
            err.put("question", "API Key missing. Please update gemini_api_key.txt in project root.");
            err.put("options", List.of("Ok"));
            err.put("answer", "Ok");
            questions.add(err);
            return questions;
        }

        // Use 'latest' alias which is present in user's model list
        final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + apiKey;

        try {
            // constructing the prompt
            String topicList = String.join(", ", topics);
            String promptText = "Generate " + count
                    + " multiple-choice questions total, distributed relevantly among these topics: [" + topicList
                    + "]. " +
                    "Return a strictly valid JSON array of objects. " +
                    "Each object must have these keys: 'topic' (string), 'question' (string), 'options' (array of 4 strings), 'answer' (string, must match exactly one option). "
                    +
                    "Do not include markdown formatting (like ```json), just raw JSON.";

            // generic JSON structure for Gemini
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payload = new HashMap<>();

            // "contents": [{ "parts": [{"text": "..."}] }]
            Map<String, Object> part = new HashMap<>();
            part.put("text", promptText);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            payload.put("contents", List.of(content));

            String requestBody = mapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Fallback for 404/400
            if (response.statusCode() != 200) {
                System.out.println(
                        "Gemini Flash Latest failed (" + response.statusCode() + "), trying gemini-pro-latest...");
                String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-latest:generateContent?key="
                        + apiKey;

                HttpRequest fallbackRequest = HttpRequest.newBuilder()
                        .uri(URI.create(fallbackUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                response = client.send(fallbackRequest, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() == 200) {
                // Parse response
                JsonNode root = mapper.readTree(response.body());
                // Response structure: candidates[0].content.parts[0].text
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    String rawJson = textNode.asText();

                    // Cleanup markdown if present (e.g. ```json ... ```)
                    rawJson = rawJson.replaceAll("```json", "").replaceAll("```", "").trim();

                    questions = mapper.readValue(rawJson, new TypeReference<List<Map<String, Object>>>() {
                    });
                }
            } else {
                System.err.println("Gemini API Error: " + response.statusCode() + " " + response.body());
                // Fallback to mock on error or notify
                Map<String, Object> err = new HashMap<>();
                err.put("topic", "Error");
                err.put("question",
                        "Failed to generate quiz via AI. Status: " + response.statusCode() + ". Check console/logs.");
                err.put("options", List.of("Retry"));
                err.put("answer", "Retry");
                questions.add(err);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            Map<String, Object> err = new HashMap<>();
            err.put("topic", "System");
            err.put("question", "Error calling AI: " + e.getMessage());
            err.put("options", List.of("Ok"));
            err.put("answer", "Ok");
            questions.add(err);
        }

        return questions;
    }
}
