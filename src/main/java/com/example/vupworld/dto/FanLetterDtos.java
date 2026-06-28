package com.example.vupworld.dto;

public class FanLetterDtos {
    public record FanLetterDTO(
            String id,
            String fanType,
            String fanName,
            String content,
            String mood,
            String icon,
            String npcBinding
    ) {}
}
