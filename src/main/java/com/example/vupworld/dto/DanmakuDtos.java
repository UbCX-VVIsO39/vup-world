package com.example.vupworld.dto;

import java.util.List;

public class DanmakuDtos {
    public record DanmakuDTO(
            String persona,
            String text,
            String tone
    ) {}

    public record DanmakuListDTO(
            List<DanmakuDTO> danmakus,
            int count,
            String mood
    ) {}
}
