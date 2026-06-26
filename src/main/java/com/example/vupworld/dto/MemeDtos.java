package com.example.vupworld.dto;

import java.util.List;

public final class MemeDtos {
    private MemeDtos() {
    }

    public record MemeLifecycleDTO(
            String headline,
            List<MemeLifecycleItemDTO> items,
            String nextHint
    ) {
    }

    public record MemeLifecycleItemDTO(
            String memeSubtype,
            String label,
            String stage,
            String stageLabel,
            int usesInWindow,
            Integer firstSeenDay,
            Integer lastSeenDay,
            String nextHint
    ) {
    }
}
