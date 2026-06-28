package com.example.vupworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.List;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.dto.InteractionDtos.InteractionEventDTO;

public final class ActionDtos {
    private ActionDtos() {
    }

    public record ActionOptionDTO(
            String actionType,
            String name,
            int staminaCost,
            String requirement,
            String effectPreview,
            String costPreview,
            String riskPreview,
            String pressureState,
            String pressureLockGroupLabel,
            String pressureHint,
            int pressureCooldownLeft,
            int pressureWoundLeft,
            boolean pressureWounded,
            String pressureReplacementActionType,
            String pressureReplacementActionLabel,
            int pressureReplacementStaminaCost,
            int pressureReplacementInspirationCost,
            int pressureReplacementReputationCost,
            String pressureReplacementHint,
            String routeBiasType,
            String routeBiasLabel,
            String riskLevel,
            String planArchetype,
            String recommendedReason,
            String tempoHint,
            String routeFocusHint,
            String comboKey,
            String comboLabel,
            String comboHint,
            boolean enabled,
            String disabledReason,
            int actionPointCost
    ) {
    }

    public record SubmitActionRequest(
            @NotBlank(message = "行动类型不能为空") String actionType,
            String planType,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record SubmitScheduleRequest(
            String planKey,
            List<ScheduleSlotRequest> slots,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record ScheduleSlotRequest(
            String slotKey,
            @NotBlank(message = "行动类型不能为空") String actionType,
            @NotBlank(message = "强度不能为空") String intensity
    ) {
    }

    public record StreamPlanOptionDTO(String planType, String label) {
    }

    public record ActionResultDTO(
            String actionType,
            String selectedTitle,
            String summary,
            int fanChange,
            int trueFanChange,
            int funFanChange,
            int unicornFanChange,
            int ddFanChange,
            int staminaChange,
            int attributeChange,
            int routeScoreChange,
            List<DebtCreatedDTO> debtCreated,
            Map<String, Object> evidenceRef,
            FatigueInfoDTO fatigueInfo
    ) {
        public ActionResultDTO(
                String actionType,
                String selectedTitle,
                String summary,
                int fanChange,
                int trueFanChange,
                int funFanChange,
                int unicornFanChange,
                int ddFanChange,
                int staminaChange,
                int attributeChange,
                int routeScoreChange,
                Map<String, Object> evidenceRef
        ) {
            this(
                    actionType,
                    selectedTitle,
                    summary,
                    fanChange,
                    trueFanChange,
                    funFanChange,
                    unicornFanChange,
                    ddFanChange,
                    staminaChange,
                    attributeChange,
                    routeScoreChange,
                    List.of(),
                    evidenceRef,
                    null
            );
        }

        public ActionResultDTO(
                String actionType,
                String summary,
                int fanChange,
                int trueFanChange,
                int funFanChange,
                int unicornFanChange,
                int ddFanChange,
                int staminaChange,
                int attributeChange,
                int routeScoreChange,
                Map<String, Object> evidenceRef
        ) {
            this(
                    actionType,
                    null,
                    summary,
                    fanChange,
                    trueFanChange,
                    funFanChange,
                    unicornFanChange,
                    ddFanChange,
                    staminaChange,
                    attributeChange,
                    routeScoreChange,
                    List.of(),
                    evidenceRef,
                    null
            );
        }

        public ActionResultDTO withDebtCreated(List<DebtCreatedDTO> debtCreated) {
            return new ActionResultDTO(
                    actionType,
                    selectedTitle,
                    summary,
                    fanChange,
                    trueFanChange,
                    funFanChange,
                    unicornFanChange,
                    ddFanChange,
                    staminaChange,
                    attributeChange,
                    routeScoreChange,
                    debtCreated == null ? List.of() : List.copyOf(debtCreated),
                    evidenceRef,
                    fatigueInfo
            );
        }

        public ActionResultDTO withFatigueInfo(FatigueInfoDTO fatigueInfo) {
            return new ActionResultDTO(
                    actionType,
                    selectedTitle,
                    summary,
                    fanChange,
                    trueFanChange,
                    funFanChange,
                    unicornFanChange,
                    ddFanChange,
                    staminaChange,
                    attributeChange,
                    routeScoreChange,
                    debtCreated,
                    evidenceRef,
                    fatigueInfo
            );
        }
    }

    public record FatigueInfoDTO(
            int consecutiveDays,
            int fatiguePercent,
            String fatigueHint
    ) {
    }

    public record DebtCreatedDTO(
            String debtType,
            int severity,
            int remainingDays
    ) {
    }

    public record DayResultDTO(
            String phase,
            ActionResultDTO actionResult,
            InteractionEventDTO interactionEvent,
            PendingEventDTO formalEvent,
            Long reportId,
            boolean endingReady,
            Long selectedPlanId,
            List<TitleOptionDTO> titleCandidates
    ) {
        public DayResultDTO(
                String phase,
                ActionResultDTO actionResult,
                InteractionEventDTO interactionEvent,
                Long reportId,
                boolean endingReady,
                Long selectedPlanId,
                List<TitleOptionDTO> titleCandidates
        ) {
            this(phase, actionResult, interactionEvent, null, reportId, endingReady, selectedPlanId, titleCandidates);
        }
    }

    public record TitleOptionDTO(
            Long id,
            String titleText,
            String style,
            String effectPreview,
            String debtRiskPreview,
            String reportTone
    ) {
    }

    public record RerollTitleRequest(
            @NotNull(message = "企划ID不能为空") Long planId,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record TitleRerollResultDTO(
            String phase,
            int titleRerollCount,
            int inspiration,
            List<TitleOptionDTO> titleCandidates
    ) {
    }

    public record ChooseTitleRequest(
            @NotNull(message = "标题ID不能为空") Long titleTemplateId,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record CancelActionRequest(
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record OffStreamOptionDTO(
            String type,
            String name,
            String effectPreview,
            String benefitPreview,
            String costPreview,
            String bestFor,
            String riskPreview,
            String recommendedReason,
            String routeBias,
            boolean recommended
    ) {
    }

    public record SubmitOffStreamRequest(
            @NotBlank(message = "下播行动类型不能为空") String offStreamType
    ) {
    }

    public record SendGiftRequest(
            String giftId,
            int qty,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record GiftAccumulateDTO(
            int giftCount,
            int giftCoinValue,
            int coinBalance
    ) {
    }

    public record SendDanmakuRequest(
            String text,
            String mood,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {
    }

    public record DanmakuAccumulateDTO(
            int count,
            int heat,
            int reputationPenalty
    ) {
    }
}
