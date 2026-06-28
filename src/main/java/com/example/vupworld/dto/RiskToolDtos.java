package com.example.vupworld.dto;

import com.example.vupworld.dto.VupDtos.DebtDTO;

import java.util.List;

public final class RiskToolDtos {
    private RiskToolDtos() {
    }

    public record UseRiskToolRequest(
            String toolType,
            Long targetDebtId,
            @jakarta.validation.constraints.NotBlank String idempotencyKey
    ) {
    }

    public record RiskToolResultDTO(
            String toolType,
            String phase,
            int coinDelta,
            int inspirationDelta,
            int debtSeverityDelta,
            int reputationDelta,
            int popularityDelta,
            int coin,
            int stamina,
            int inspiration,
            DebtDTO debt,
            String summary
    ) {
    }

    public record RiskToolOptionDTO(
            String toolType,
            String label,
            String costPreview,
            String effectPreview,
            boolean enabled,
            String disabledReason,
            Long targetDebtId,
            DebtDTO targetDebt,
            String targetSummary,
            String urgencyPreview,
            List<String> supportedDebtTypes
    ) {
    }

    public record CrisisAlertDTO(
            String type,
            String title,
            int daysLeft,
            String severity,
            String description
    ) {
    }
}
