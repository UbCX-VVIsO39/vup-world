package com.example.vupworld.dto;

import java.util.List;

public final class AudienceDtos {
    private AudienceDtos() {
    }

    public record FunAudienceProfileDTO(
            String headline,
            String riskLabel,
            String image,
            List<FunAudienceMetricDTO> metrics,
            String dominantSignal,
            String nextMoveHint
    ) {
    }

    public record FunAudienceMetricDTO(
            String key,
            String label,
            int value,
            String tone,
            String line
    ) {
    }

    public record AudienceExpectationBoardDTO(
            String headline,
            AudienceExpectationDTO lead,
            List<AudienceExpectationDTO> expectations,
            String pivotRiskLabel,
            String pivotRiskLine,
            String nextMoveHint,
            String image
    ) {
    }

    public record AudienceExpectationDTO(
            String routeType,
            String routeLabel,
            int score,
            int share,
            String lockLabel,
            String evidenceLine,
            String tone
    ) {
    }
}
