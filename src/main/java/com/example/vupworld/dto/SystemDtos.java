package com.example.vupworld.dto;

import java.util.List;

public final class SystemDtos {
    private SystemDtos() {
    }

    public record ConfigCheckDTO(
            String status,
            String checkedAt,
            List<ConfigCategoryDTO> categories,
            List<String> fatalErrors,
            List<String> warnings,
            List<DefenseEvidenceDTO> defenseEvidence,
            DevToolsDTO devTools,
            boolean canPlayP0
    ) {
    }

    public record ConfigCategoryDTO(
            String name,
            String status,
            String detail,
            Integer expectedCount,
            Integer actualCount,
            Integer required,
            Integer actual
    ) {
        public ConfigCategoryDTO(String name, String status, String detail) {
            this(name, status, detail, null, null, null, null);
        }

        public ConfigCategoryDTO(String name, String status, String detail, Integer expectedCount, Integer actualCount) {
            this(name, status, detail, expectedCount, actualCount, expectedCount, actualCount);
        }
    }

    public record DevToolsDTO(
            boolean enabled,
            String profile,
            List<String> availableStrategies
    ) {
    }

    public record DefenseEvidenceDTO(
            String key,
            String label,
            String detail,
            String proof
    ) {
    }
}
