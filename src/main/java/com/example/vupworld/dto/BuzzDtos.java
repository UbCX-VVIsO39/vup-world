package com.example.vupworld.dto;

public final class BuzzDtos {
    private BuzzDtos() {
    }

    public record BuzzBriefingDTO(
            String headline,
            String heatLabel,
            String image,
            String forumLine,
            String clipperLine,
            String trendLabel,
            String trendDescription,
            String nextMoveHint
    ) {
    }
}
