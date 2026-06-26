package com.example.vupworld.dto;

import java.util.List;

public class DailyHighlightDtos {
    public record DailyHighlightDTO(
            Long id,
            Long vupId,
            int day,
            String title,
            String description,
            String icon,
            String highlightType,
            String createdAt
    ) {}

    public record HighlightListDTO(
            List<DailyHighlightDTO> highlights,
            int totalCount,
            String lastHighlightTitle
    ) {}
}
