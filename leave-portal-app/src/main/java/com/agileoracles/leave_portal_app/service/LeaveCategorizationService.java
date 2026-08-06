package com.agileoracles.leave_portal_app.service;

import com.agileoracles.leave_portal_app.dto.CategorizationResult;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveCategorizationService {

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("Sick Leave", List.of("doctor", "hospital", "fever", "surgery"));
        CATEGORY_KEYWORDS.put("Annual Leave", List.of("vacation", "holiday", "family"));
        CATEGORY_KEYWORDS.put("Emergency Leave", List.of("emergency", "urgent"));
        CATEGORY_KEYWORDS.put("Maternity Leave", List.of("maternity", "childbirth"));
        CATEGORY_KEYWORDS.put("Unpaid Leave", List.of("unpaid"));
    }

    public CategorizationResult categorize(String content) {
        if (content == null || content.isBlank()) {
            return new CategorizationResult("Other", "No readable content found in the file");
        }

        String lowerContent = content.toLowerCase();

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> matchedKeywords = entry.getValue().stream()
                    .filter(lowerContent::contains)
                    .toList();

            if (!matchedKeywords.isEmpty()) {
                String reason = "Matched keywords: " + String.join(", ", matchedKeywords);
                return new CategorizationResult(category, reason);
            }
        }

        return new CategorizationResult("Other", "No matching keywords found");
    }
}