package com.example.vupworld.dto;

import java.util.List;

public final class PersonaDtos {
    private PersonaDtos() {
    }

    public record PersonaTagBoardDTO(
            String headline,
            String image,
            int activeCount,
            List<PersonaTagDTO> tags,
            String nextHint
    ) {
    }

    public record PersonaTagDTO(
            String tagKey,
            String label,
            int strength,
            String status,
            String statusLabel,
            String evidence,
            String nextHint,
            String tone
    ) {
    }
}
