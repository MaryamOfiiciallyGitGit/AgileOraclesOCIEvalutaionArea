package com.agileoracles.leave_portal_app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class CategorizationWorker {

    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "1527");
    private static final String DB_URL = "jdbc:oracle:thin:@//" + DB_HOST + ":" + DB_PORT + "/FREEPDB1";
    private static final String DB_USERNAME = System.getenv().getOrDefault("DB_USERNAME", "eta_maryam");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "firstpeerpass");
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    private static final List<String> VALID_CATEGORIES = List.of(
            "Sick Leave", "Annual Leave", "Emergency Leave",
            "Maternity Leave", "Unpaid Leave", "Other"
    );

    public static void main(String[] args) throws Exception {

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {

            System.out.println("Connected to database. Looking for uncategorized records...");

            String selectSql = "SELECT id, reason_for_leave FROM leave_requests WHERE leave_category IS NULL";

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 ResultSet rs = selectStmt.executeQuery()) {

                while (rs.next()) {
                    long id = rs.getLong("id");
                    String reason = rs.getString("reason_for_leave");

                    System.out.println("Processing record #" + id + "...");

                    String category = categorizeWithGemini(reason);

                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE leave_requests SET leave_category = ? WHERE id = ?")) {
                        updateStmt.setString(1, category);
                        updateStmt.setLong(2, id);
                        updateStmt.executeUpdate();
                    }

                    System.out.println("Record #" + id + " categorized as: " + category);
                }
            }

            System.out.println("Done.");
        }
    }

    private static String categorizeWithGemini(String documentText) throws Exception {

        String prompt = "You are a leave request classifier. Read the following leave "
                + "request text and classify it into EXACTLY ONE of these categories: "
                + "Sick Leave, Annual Leave, Emergency Leave, Maternity Leave, Unpaid Leave, Other. "
                + "Respond with ONLY the category name, nothing else.\n\nText:\n" + documentText;

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + GEMINI_API_KEY;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());
        String rawCategory = root.at("/candidates/0/content/parts/0/text").asText("").trim();

        return VALID_CATEGORIES.contains(rawCategory) ? rawCategory : "Other";
    }
}