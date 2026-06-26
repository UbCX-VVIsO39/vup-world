package com.example.vupworld.dto;

import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;

import java.util.List;

public final class DayDtos {
    private DayDtos() {
    }

    public record DaySessionDTO(
            int day,
            String phase,
            String selectedAction,
            Long selectedPlanId,
            Long selectedTitleTemplateId,
            String titleCandidatesJson,
            List<TitleOptionDTO> titleCandidates,
            int titleRerollCount,
            boolean streamPlanCancelled,
            Long pendingInteractionEventId,
            PendingInteractionDTO pendingInteractionEvent,
            Long pendingFormalEventId,
            PendingEventDTO pendingFormalEvent,
            Long reportId,
            Long endingReviewId,
            List<String> riskHints,
            List<DisabledActionDTO> disabledActions,
            String availableHint
    ) {
    }

    public record DisabledActionDTO(
            String actionType,
            String disabledReason
    ) {
    }

    public record NextDayRequest(@jakarta.validation.constraints.NotBlank String idempotencyKey) {
    }
}
