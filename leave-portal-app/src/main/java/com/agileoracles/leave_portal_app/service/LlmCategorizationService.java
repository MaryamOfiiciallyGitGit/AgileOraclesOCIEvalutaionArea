package com.agileoracles.leave_portal_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class LlmCategorizationService {

    private static final List<String> VALID_CATEGORIES = List.of(
            "Sick Leave", "Annual Leave", "Emergency Leave",
            "Maternity Leave", "Unpaid Leave", "Other"
    );

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String categorize(String documentText) throws Exception {

        String prompt = "You are a leave request classifier. Read the following leave "
                + "request text and classify it into EXACTLY ONE of these categories: "
                + "Sick Leave, Annual Leave, Emergency Leave, Maternity Leave, Unpaid Leave, Other. "
                + "Respond with ONLY the category name, nothing else.\n\nText:\n" + documentText;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + apiKey;

        String responseJson = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(requestBody))
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(responseJson);
        String rawCategory = root.at("/candidates/0/content/parts/0/text").asText("").trim();

        return VALID_CATEGORIES.contains(rawCategory) ? rawCategory : "Other";
    }
}