package com.example.vupworld.dto;

import java.util.Map;
import java.util.List;

public final class FanTopicDtos {
    private FanTopicDtos() {
    }

    public record ChooseFanTopicRequest(String topicKey, String choiceType, @jakarta.validation.constraints.NotBlank String idempotencyKey) {
    }

    public record FanTopicOptionDTO(
            String topicKey,
            String title,
            String description,
            String triggerPreview,
            String riskPreview,
            List<FanTopicChoiceDTO> choices
    ) {
    }

    public record FanTopicChoiceDTO(
            String choiceType,
            String label,
            String costPreview,
            String effectPreview,
            String riskPreview,
            boolean enabled,
            String disabledReason
    ) {
    }

    public record FanTopicResultDTO(
            String phase,
            String topicKey,
            String choiceType,
            int reputationDelta,
            int watchHeatDelta,
            int inspirationDelta,
            String summary,
            Map<String, Object> evidenceRef
    ) {
    }
}
