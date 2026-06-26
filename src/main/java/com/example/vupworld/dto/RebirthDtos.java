package com.example.vupworld.dto;

import java.util.Map;

public final class RebirthDtos {
    private RebirthDtos() {
    }

    public record RestartRequest(Boolean confirmRestart, String restartBiasType, @jakarta.validation.constraints.NotBlank String idempotencyKey) {
    }

    public record RestartResultDTO(
            Long vupId,
            int day,
            String phase,
            Map<String, Object> restartHintApplied
    ) {
    }
}
