package com.example.vupworld.service.core;

import com.example.vupworld.service.infra.DeterministicRngService;

import com.example.vupworld.mapper.DaySessionMapper;
import org.springframework.stereotype.Component;

/**
 * DaySessionMapper 的 helper 实现，避免 DeterministicRngService 直接依赖 Mapper。
 */
@Component
public class DaySessionMapperHelperImpl implements DeterministicRngService.DaySessionMapperHelper {

    private final DaySessionMapper daySessionMapper;

    public DaySessionMapperHelperImpl(DaySessionMapper daySessionMapper) {
        this.daySessionMapper = daySessionMapper;
    }

    @Override
    public void updateRngCursor(Long sessionId, int cursor) {
        daySessionMapper.updateRngCursor(sessionId, cursor);
    }
}
