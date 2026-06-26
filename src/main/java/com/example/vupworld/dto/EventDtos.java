package com.example.vupworld.dto;

public final class EventDtos {
    private EventDtos() {
    }

    public record EventDTO(
            String id,
            String title,
            String description,
            String effect,
            String eventType,
            String icon
    ) {
    }

    public record PendingEventDTO(
            boolean pending,
            Long debtId,
            String debtType,
            String title,
            String description,
            int severity,
            int dueDay,
            String sourceAction,
            String sourceTitle,
            java.util.List<EventChoiceDTO> choices
    ) {
    }

    public record EventChoiceDTO(
            String choiceId,
            String choiceType,
            String label,
            String costPreview,
            String riskPreview,
            String effectPreview,
            boolean enabled,
            String disabledReason
    ) {
        public EventChoiceDTO(
                String choiceType,
                String label,
                String costPreview,
                String riskPreview,
                String effectPreview,
                boolean enabled,
                String disabledReason
        ) {
            this(choiceType, choiceType, label, costPreview, riskPreview, effectPreview, enabled, disabledReason);
        }
    }

    public record ChooseEventRequest(
            Long eventId,
            String choiceId,
            String choiceType,
            @jakarta.validation.constraints.NotBlank String idempotencyKey
    ) {
        public ChooseEventRequest(String choiceType, String idempotencyKey) {
            this(null, null, choiceType, idempotencyKey);
        }

        public String selectedChoiceType() {
            return hasText(choiceId) ? choiceId : choiceType;
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
