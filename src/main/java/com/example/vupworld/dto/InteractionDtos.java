package com.example.vupworld.dto;

import java.util.List;

public final class InteractionDtos {
    private InteractionDtos() {
    }

    public record ChooseInteractionRequest(String choiceType, @jakarta.validation.constraints.NotBlank String idempotencyKey) {
    }

    public record InteractionEventDTO(
            Long id,
            String eventKey,
            String description,
            List<InteractionChoiceDTO> choices
    ) {
    }

    public record InteractionChoiceDTO(
            String choiceId,
            String choiceType,
            String label,
            String costPreview,
            String effectPreview,
            String riskPreview,
            boolean enabled,
            String disabledReason
    ) {
        public InteractionChoiceDTO(String choiceType, String label, String riskPreview) {
            this(choiceType, choiceType, label, null, null, riskPreview, true, null);
        }

        public InteractionChoiceDTO(String choiceType, String label, String costPreview, String effectPreview, String riskPreview) {
            this(choiceType, choiceType, label, costPreview, effectPreview, riskPreview, true, null);
        }
    }

    public record PendingInteractionDTO(
            boolean pending,
            Long id,
            String eventKey,
            String title,
            String description,
            List<PendingInteractionChoiceDTO> choices
    ) {
    }

    public record PendingInteractionChoiceDTO(
            String choiceId,
            String choiceType,
            String label,
            String costPreview,
            String riskPreview,
            String effectPreview,
            boolean enabled,
            String disabledReason
    ) {
        public PendingInteractionChoiceDTO(String choiceType, String label, String riskPreview, String effectPreview, boolean enabled, String disabledReason) {
            this(choiceType, choiceType, label, null, riskPreview, effectPreview, enabled, disabledReason);
        }
    }
}
