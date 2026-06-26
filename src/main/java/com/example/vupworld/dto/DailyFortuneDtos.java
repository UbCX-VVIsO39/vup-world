package com.example.vupworld.dto;

public class DailyFortuneDtos {
    public record DailyFortuneDTO(
            String fortune,
            String advice,
            String luckyAction,
            String icon,
            String mood
    ) {}
}
