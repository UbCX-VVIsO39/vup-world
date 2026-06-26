package com.example.vupworld.service.event;

import com.example.vupworld.service.risk.RiskDebtLabels;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.report.ReportService;
import com.example.vupworld.service.risk.RewardCalculator;
import com.example.vupworld.service.risk.DebtService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.ActionDtos.ActionResultDTO;
import com.example.vupworld.dto.ActionDtos.DebtCreatedDTO;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.InteractionDtos.ChooseInteractionRequest;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionChoiceDTO;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class InteractionService {
    private static final String INTERACTION_CHOOSE_PATH = "/api/interaction/choose";

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final BusinessLogMapper businessLogMapper;
    private final ReportService reportService;
    private final IdempotencyRunner idempotencyRunner;
    private final JsonService jsonService;
    private final PendingInteractionPresenter pendingInteractionPresenter;
    private final RequestHashService requestHashService;
    private final DayFlowService dayFlowService;
    private final EventOutcomeResolver eventOutcomeResolver;
    private final RewardCalculator rewardCalculator;
    private final DebtService debtService;

    public InteractionService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            BusinessLogMapper businessLogMapper,
            ReportService reportService,
            IdempotencyRunner idempotencyRunner,
            JsonService jsonService,
            PendingInteractionPresenter pendingInteractionPresenter,
            RequestHashService requestHashService,
            DayFlowService dayFlowService,
            EventOutcomeResolver eventOutcomeResolver,
            RewardCalculator rewardCalculator,
            DebtService debtService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.businessLogMapper = businessLogMapper;
        this.reportService = reportService;
        this.idempotencyRunner = idempotencyRunner;
        this.jsonService = jsonService;
        this.pendingInteractionPresenter = pendingInteractionPresenter;
        this.requestHashService = requestHashService;
        this.dayFlowService = dayFlowService;
        this.eventOutcomeResolver = eventOutcomeResolver;
        this.rewardCalculator = rewardCalculator;
        this.debtService = debtService;
    }

    public PendingInteractionDTO pending(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        return pendingInteractionPresenter.pending(session);
    }

    @Transactional
    public DayResultDTO choose(Long userId, ChooseInteractionRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(INTERACTION_CHOOSE_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, INTERACTION_CHOOSE_PATH, idempotencyKey, requestHash,
                () -> doChoose(vup, session, request, idempotencyKey),
                DayResultDTO.class, buildRecord);
    }

    private DayResultDTO doChoose(Vup vup, DaySession session, ChooseInteractionRequest request, String idempotencyKey) {
        if (!DayPhase.NEED_INTERACTION_CHOICE.name().equals(session.getPhase()) || session.getPendingInteractionEventId() == null) {
            throw new GameException("EVENT_NOT_PENDING", "当前没有待处理直播现场事件。");
        }
        PendingInteractionDTO pendingInteraction = pendingInteractionPresenter.pending(session);
        if (!pendingInteraction.pending()) {
            throw new GameException("EVENT_NOT_PENDING", "待处理直播现场事件不存在。");
        }

        ActionResultDTO interactionResult = resolveInteraction(vup, pendingInteraction, request.choiceType());
        vupMapper.updateState(vup);

        ActionResultDTO reportActionResult = reportActionResultFor(session, interactionResult);
        BusinessLog reportLog = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), session.getDay());
        BusinessLog log = toBusinessLog(vup, session, pendingInteraction, request.choiceType(), interactionResult, idempotencyKey);
        businessLogMapper.insert(log);
        RiskDebt interactionDebt = debtService.createForInteractionChoiceIfNeeded(
                vup,
                session,
                log,
                pendingInteraction.eventKey(),
                request.choiceType()
        );
        if (interactionDebt != null) {
            addInteractionDebtEvidence(interactionResult, interactionDebt, session);
            interactionResult = interactionResult.withDebtCreated(List.of(new DebtCreatedDTO(
                    interactionDebt.getDebtType(),
                    interactionDebt.getSeverity(),
                    Math.max(0, interactionDebt.getDueDay() - session.getDay())
            )));
            log.setDebtIds(jsonService.write(List.of(interactionDebt.getId())));
            businessLogMapper.updateDebtIds(log);
        }
        if (reportLog == null) {
            reportLog = log;
        }

        var report = reportService.createReport(
                vup,
                session,
                reportActionResult,
                reportLog,
                selectedTitleText(session),
                List.of(interactionResult.evidenceRef())
        );
        reportLog.setReportId(report.getId());
        businessLogMapper.attachReport(reportLog);
        log.setReportId(report.getId());
        businessLogMapper.attachReport(log);

        session.setPendingInteractionEventId(null);
        session.setPendingEventResultJson(jsonService.write(interactionResult));
        session.setReportId(report.getId());
        boolean endingReady = dayFlowService.finishSessionAfterReport(vup, session, report, reportLog);
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(session.getPhase(), interactionResult, null, report.getId(), endingReady, null, null);
    }

    private void addInteractionDebtEvidence(ActionResultDTO interactionResult, RiskDebt debt, DaySession session) {
        if (interactionResult.evidenceRef() == null) {
            return;
        }
        interactionResult.evidenceRef().put("interactionDebtId", debt.getId());
        interactionResult.evidenceRef().put("interactionDebtType", debt.getDebtType());
        interactionResult.evidenceRef().put("interactionDebtSeverity", debt.getSeverity());
        interactionResult.evidenceRef().put("interactionDebtDueDay", debt.getDueDay());
        interactionResult.evidenceRef().put("interactionDebtRemainingDays", Math.max(0, debt.getDueDay() - session.getDay()));
    }

    private ActionResultDTO reportActionResultFor(DaySession session, ActionResultDTO fallback) {
        if (session.getPendingActionResultJson() == null || session.getPendingActionResultJson().isBlank()) {
            return fallback;
        }
        return jsonService.read(session.getPendingActionResultJson(), ActionResultDTO.class);
    }

    private ActionResultDTO resolveInteraction(Vup vup, PendingInteractionDTO pendingInteraction, String choiceType) {
        PendingInteractionChoiceDTO choice = pendingInteraction.choices().stream()
                .filter(item -> choiceType.equals(item.choiceType()))
                .findFirst()
                .orElseThrow(() -> new GameException("CHOICE_NOT_AVAILABLE", "当前直播现场没有这个处理选项。"));

        EventOutcomeResolver.EventOutcome outcome = eventOutcomeResolver.resolveInteraction(vup, choiceType, pendingInteraction.eventKey());
        rewardCalculator.applyToVup(vup, outcome.delta());
        dayFlowService.applyRouteScoreChange(vup, outcome.delta().routeType(), outcome.delta().routeScoreChange());

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("type", "live_interaction");
        evidence.put("id", pendingInteraction.id());
        evidence.put("eventKey", pendingInteraction.eventKey());
        evidence.put("choiceType", choiceType);
        evidence.put("popularityChange", outcome.delta().popularityChange());
        evidence.put("watchHeatChange", outcome.delta().watchHeatChange());
        evidence.put("reputationChange", outcome.delta().reputationChange());
        evidence.put("memeChange", outcome.delta().memeChange());
        evidence.put("routeType", outcome.delta().routeType());
        evidence.put("routeScoreChange", outcome.delta().routeScoreChange());
        evidence.put("hasRisk", outcome.hasRisk());

        return new ActionResultDTO(
                "INTERACTION_CHOICE",
                pendingInteraction.description() + " 你选择" + choice.label() + "，" + choice.effectPreview() + "。",
                outcome.delta().fanChange(),
                outcome.delta().trueFanChange(),
                outcome.delta().funFanChange(),
                outcome.delta().unicornFanChange(),
                outcome.delta().ddFanChange(),
                outcome.delta().popularityChange(),
                outcome.delta().watchHeatChange(),
                outcome.delta().routeScoreChange(),
                evidence
        );
    }

    private String selectedTitleText(DaySession session) {
        if (session.getSelectedTitleTemplateId() == null || session.getTitleCandidatesJson() == null) {
            return null;
        }
        return jsonService.readTitleOptions(session.getTitleCandidatesJson()).stream()
                .filter(candidate -> candidate.id().equals(session.getSelectedTitleTemplateId()))
                .map(candidate -> candidate.titleText())
                .findFirst()
                .orElse(null);
    }

    private BusinessLog toBusinessLog(
            Vup vup,
            DaySession session,
            PendingInteractionDTO pendingInteraction,
            String choiceType,
            ActionResultDTO result,
            String idempotencyKey
    ) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.ACTION_RESOLVED.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction(result.actionType());
        log.setInteractionEventId(pendingInteraction.id());
        log.setResult(result.summary());
        log.setRawFanGain(result.fanChange());
        log.setFinalFanGain(result.fanChange());
        log.setFanChange(result.fanChange());
        log.setTrueFanChange(result.trueFanChange());
        log.setFunFanChange(result.funFanChange());
        log.setUnicornFanChange(result.unicornFanChange());
        log.setDdFanChange(result.ddFanChange());
        log.setPopularityChange(numberEvidence(result, "popularityChange"));
        log.setWatchHeatChange(numberEvidence(result, "watchHeatChange"));
        log.setReputationChange(numberEvidence(result, "reputationChange"));
        log.setMemeChange(numberEvidence(result, "memeChange"));
        log.setCommercialChange(0);
        log.setCoinChange(0);
        log.setInspirationChange(0);
        log.setMultiplierDetail(jsonService.write(Map.of("pipeline", "P0_LIVE_INTERACTION")));
        log.setCapDetail(jsonService.write(Map.of("stageCap", 100, "hit", false)));
        log.setClampDetail(jsonService.write(Map.of("reputation", vup.getReputation(), "watchHeat", vup.getWatchHeat())));
        log.setWeightDetail(jsonService.write(Map.of(
                "eventKey", pendingInteraction.eventKey(),
                "interactionEventId", pendingInteraction.id(),
                "choiceType", choiceType
        )));
        log.setRngDetail(jsonService.write(Map.of("seed", session.getRandomSeed(), "cursor", session.getRngCursor())));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(Map.of(
                routeTypeEvidence(result), result.routeScoreChange(),
                "source", "interaction:" + pendingInteraction.eventKey() + ":" + choiceType,
                "eventKey", pendingInteraction.eventKey(),
                "choiceType", choiceType
        )));
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(true);
        return log;
    }

    private int numberEvidence(ActionResultDTO result, String key) {
        Object value = result.evidenceRef() == null ? null : result.evidenceRef().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private String routeTypeEvidence(ActionResultDTO result) {
        Object value = result.evidenceRef() == null ? null : result.evidenceRef().get("routeType");
        String routeType = value == null ? "" : String.valueOf(value);
        if (routeType == null || routeType.isBlank()) {
            return RouteType.ELECTRONIC_PICKLE.name();
        }
        return routeType;
    }

}
