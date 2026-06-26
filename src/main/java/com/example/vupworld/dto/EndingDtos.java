package com.example.vupworld.dto;

import java.util.List;
import java.util.Map;

public final class EndingDtos {
    private EndingDtos() {
    }

    public record EndingReviewDTO(
            Long id,
            String endingType,
            String finalTitle,
            String subtitle,
            List<EndingTagDTO> endingTags,
            String endingReason,
            Map<String, Object> endingReasonJson,
            String summary,
            Map<String, Object> fanProfile,
            List<Map<String, Object>> keyEvents,
            List<Map<String, Object>> debtRefs,
            Map<String, Object> routeReview,
            Map<String, Object> restartHint
    ) {
    }

    public record EndingTagDTO(
            String tagKey,
            String label,
            String evidence
    ) {
    }

    public record EndingForecastDTO(
            String headline,
            String likelyEndingType,
            String likelyFinalTitle,
            String ifEndedNowType,
            String ifEndedNowTitle,
            String targetReason,
            int confidence,
            List<EndingForecastRequirementDTO> requirements,
            String riskLine,
            String sprintHint,
            String image
    ) {
    }

    public record EndingForecastRequirementDTO(
            String key,
            String label,
            String status,
            String line,
            int currentValue,
            int targetValue,
            int missingValue,
            String targetRoute,
            List<String> suggestedActionTypes,
            int priority,
            boolean blocking
    ) {
    }
}
