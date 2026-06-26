package com.example.vupworld.service.core;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.operating.OperatingPressureService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.DayPhaseStateMachine;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.DayDtos.NextDayRequest;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiFunction;

@Service
public class DayService {
    private static final String NEXT_DAY_PATH = "/api/day/next";

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final VupStateMapper vupStateMapper;
    private final JsonService jsonService;
    private final IdempotencyRunner idempotencyRunner;
    private final RequestHashService requestHashService;
    private final BalanceConfig balanceConfig;
    private final OperatingPressureService operatingPressureService;

    public DayService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            VupStateMapper vupStateMapper,
            JsonService jsonService,
            IdempotencyRunner idempotencyRunner,
            RequestHashService requestHashService,
            BalanceConfig balanceConfig,
            OperatingPressureService operatingPressureService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.vupStateMapper = vupStateMapper;
        this.jsonService = jsonService;
        this.idempotencyRunner = idempotencyRunner;
        this.requestHashService = requestHashService;
        this.balanceConfig = balanceConfig;
        this.operatingPressureService = operatingPressureService;
    }

    @Transactional
    public DaySessionDTO nextDay(Long userId, NextDayRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        // Capture current session state for idempotency record before business logic
        Vup vup = vupService.requireActiveVup(userId);
        DaySession current = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());

        BiFunction<String, DaySessionDTO, ApiIdempotencyRecord> buildRecord = (hash, dto) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(current.getDay());
            record.setApiPath(NEXT_DAY_PATH);
            record.setPhase(current.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(dto));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, NEXT_DAY_PATH, idempotencyKey, requestHash,
                () -> doNextDay(userId, vup),
                DaySessionDTO.class, buildRecord);
    }

    private DaySessionDTO doNextDay(Long userId, Vup vup) {
        DaySession current = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (current == null) {
            throw new GameException("SYSTEM_ERROR", "当前天数状态丢失。");
        }
        DayPhase currentPhase = dayPhase(current.getPhase());
        int maxDay = balanceConfig.maxDay();
        if (current.getDay() >= maxDay && currentPhase == DayPhase.ENDING_READY) {
            throw new GameException("DAY_LIMIT_REACHED", "最终日之后不能进入下一天。");
        }
        if (currentPhase != DayPhase.REPORT_READY) {
            throw new GameException("PHASE_NOT_ALLOWED", "日报还没生成，不能进入下一天。");
        }
        if (!DayPhaseStateMachine.canStartNextDay(currentPhase, current.getDay(), maxDay)) {
            throw new GameException("DAY_LIMIT_REACHED", "最终日之后不能进入下一天。");
        }

        int nextDay = current.getDay() + 1;
        vup.setDayCount(nextDay);
        vup.setStamina(Math.min(vup.getMaxStamina(), vup.getStamina() + balanceConfig.dailyStaminaRecovery()));
        operatingPressureService.advanceDay(vup, nextDay);
        vupMapper.updateState(vup);

        DaySession next = new DaySession();
        next.setVupId(vup.getId());
        next.setDay(nextDay);
        next.setPhase(DayPhase.READY.name());
        next.setTitleCandidatesJson("[]");
        next.setTitleRerollCount(0);
        next.setStreamPlanCancelled(false);
        next.setRiskToolUsed(false);
        next.setFormalEventSlotStatus("EMPTY");
        next.setFormalEventPriority(0);
        next.setRandomSeed(vup.getRunSeed() + "-day-" + nextDay);
        next.setRngCursor(0);
        next.setLocked(false);
        daySessionMapper.insert(next);

        return vupStateMapper.toDaySessionDto(next);
    }

    private DayPhase dayPhase(String phase) {
        try {
            return DayPhase.valueOf(phase);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
