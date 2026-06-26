package com.example.vupworld.dto;

public class RandomEventDtos {
    public record RandomEventDTO(
            String id,
            String title,
            String description,
            String effect,
            String icon,
            String eventType
    ) {}
}
