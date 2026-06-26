package com.example.vupworld.service.ending;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.VupStatus;
import com.example.vupworld.dto.RebirthDtos.RestartRequest;
import com.example.vupworld.dto.RebirthDtos.RestartResultDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.UserAccountMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.BiFunction;

@Service
public class RebirthService {

    private static final Logger log = LoggerFactory.getLogger(RebirthService.class);

    private static final String RESTART_PATH = "/api/rebirth/restart";
    private static final int FAN_BIAS_PERCENT = 5;

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final UserAccountMapper userAccountMapper;
    private final JsonService jsonService;
    private final IdempotencyRunner idempotencyRunner;
    private final RequestHashService requestHashService;
    private final EndingAtlasService endingAtlasService;

    public RebirthService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            UserAccountMapper userAccountMapper,
            JsonService jsonService,
            IdempotencyRunner idempotencyRunner,
            RequestHashService requestHashService,
            EndingAtlasService endingAtlasService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.userAccountMapper = userAccountMapper;
        this.jsonService = jsonService;
        this.idempotencyRunner = idempotencyRunner;
        this.requestHashService = requestHashService;
        this.endingAtlasService = endingAtlasService;
    }

    @Transactional
    public RestartResultDTO restart(Long userId, RestartRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup previous = vupService.requireActiveVup(userId);

        BiFunction<String, RestartResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(previous.getId());
            record.setApiPath(RESTART_PATH);
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, RESTART_PATH, idempotencyKey, requestHash,
                () -> doRestart(userId, previous, request),
                RestartResultDTO.class, buildRecord);
    }

    private RestartResultDTO doRestart(Long userId, Vup previous, RestartRequest request) {
        log.info("Processing restart for userId={}, previousVupId={}", userId, previous.getId());

        DaySession endingSession = daySessionMapper.findByVupIdAndDay(previous.getId(), previous.getDayCount());
        if (endingSession == null) {
            throw new GameException("SYSTEM_ERROR", "当前天数状态丢失，毕业照找不到底片。");
        }
        if (!DayPhase.ENDING_READY.name().equals(endingSession.getPhase())) {
            throw new GameException("RESTART_NOT_ALLOWED", "还没打完30天，别急着开复活赛。");
        }
        if (!Boolean.TRUE.equals(request.confirmRestart())) {
            throw new GameException("RESTART_CONFIRM_REQUIRED", "重开会结束本轮存档，请先确认。");
        }

        EndingAtlasService.RestartTarget restartTarget = restartTargetFor(userId, request.restartBiasType());
        String fanBiasType = dominantFanBiasType(previous);
        vupMapper.updateStatus(previous.getId(), VupStatus.COMPLETED.name());
        Vup next = vupService.createRestartedRun(
                previous,
                endingSession.getEndingReviewId(),
                fanBiasType,
                restartTarget.routeBiasType(),
                restartTarget.targetType()
        );
        userAccountMapper.incrementRestartCount(userId);

        log.info("Restart completed: newVupId={}, fanBias={}, routeBias={}, target={}",
                next.getId(), fanBiasType, restartTarget.routeBiasType(), restartTarget.targetType());

        DaySession nextSession = daySessionMapper.findByVupIdAndDay(next.getId(), next.getDayCount());
        return new RestartResultDTO(
                next.getId(),
                nextSession.getDay(),
                nextSession.getPhase(),
                Map.of(
                        "targetType", restartTarget.targetType(),
                        "routeBiasType", restartTarget.routeBiasType(),
                        "biasType", fanBiasType,
                        "fanBiasPercent", FAN_BIAS_PERCENT,
                        "openingAdvice", restartTarget.openingAdvice(),
                        "legacyTag", restartTarget.legacyTag(),
                        "runObjective", restartTarget.runObjective(),
                        "boundary", restartTarget.boundary()
                )
        );
    }

    private EndingAtlasService.RestartTarget restartTargetFor(Long userId, String rawTargetType) {
        String targetType = rawTargetType == null || rawTargetType.isBlank()
                ? endingAtlasService.nextRestartTargetType(userId)
                : rawTargetType.trim();
        return endingAtlasService.restartTarget(targetType);
    }

    private String dominantFanBiasType(Vup vup) {
        String biasType = "TRUE_FANS";
        int max = vup.getTrueFans();
        if (vup.getFunFans() > max) {
            biasType = "FUN_FANS";
            max = vup.getFunFans();
        }
        if (vup.getUnicornFans() > max) {
            biasType = "UNICORN_FANS";
            max = vup.getUnicornFans();
        }
        if (vup.getDdFans() > max) {
            biasType = "DD_FANS";
        }
        return biasType;
    }

}
