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
import com.example.vupworld.dto.EventDtos.ChooseEventRequest;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.dto.RewardDelta;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
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
public class EventService {
    private static final String EVENT_CHOOSE_PATH = "/api/event/choose";

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final BusinessLogMapper businessLogMapper;
    private final ReportService reportService;
    private final JsonService jsonService;
    private final IdempotencyRunner idempotencyRunner;
    private final FormalEventPresenter formalEventPresenter;
    private final RequestHashService requestHashService;
    private final DayFlowService dayFlowService;
    private final EventOutcomeResolver eventOutcomeResolver;
    private final RewardCalculator rewardCalculator;
    private final DebtService debtService;
    private final CommercialRouteContentService commercialRouteContentService;
    private final MidgameEventContentService midgameEventContentService;

    public EventService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            RiskDebtMapper riskDebtMapper,
            BusinessLogMapper businessLogMapper,
            ReportService reportService,
            JsonService jsonService,
            IdempotencyRunner idempotencyRunner,
            FormalEventPresenter formalEventPresenter,
            RequestHashService requestHashService,
            DayFlowService dayFlowService,
            EventOutcomeResolver eventOutcomeResolver,
            RewardCalculator rewardCalculator,
            DebtService debtService,
            CommercialRouteContentService commercialRouteContentService,
            MidgameEventContentService midgameEventContentService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.businessLogMapper = businessLogMapper;
        this.reportService = reportService;
        this.jsonService = jsonService;
        this.idempotencyRunner = idempotencyRunner;
        this.formalEventPresenter = formalEventPresenter;
        this.requestHashService = requestHashService;
        this.dayFlowService = dayFlowService;
        this.eventOutcomeResolver = eventOutcomeResolver;
        this.rewardCalculator = rewardCalculator;
        this.debtService = debtService;
        this.commercialRouteContentService = commercialRouteContentService;
        this.midgameEventContentService = midgameEventContentService;
    }

    public PendingEventDTO pending(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        return formalEventPresenter.pending(vup, session);
    }

    @Transactional
    public DayResultDTO choose(Long userId, ChooseEventRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(EVENT_CHOOSE_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, EVENT_CHOOSE_PATH, idempotencyKey, requestHash,
                () -> doChoose(userId, vup, session, request, idempotencyKey),
                DayResultDTO.class, buildRecord);
    }

    private DayResultDTO doChoose(Long userId, Vup vup, DaySession session, ChooseEventRequest request, String idempotencyKey) {
        if (!DayPhase.NEED_EVENT_CHOICE.name().equals(session.getPhase())) {
            throw new GameException("EVENT_NOT_PENDING", "当前没有待处理正式事件。");
        }
        String choiceType = normalizeChoiceType(request.selectedChoiceType());
        if (formalEventPresenter.isOrdinaryEventPending(session)) {
            if (request.eventId() != null) {
                throw new GameException("EVENT_NOT_PENDING", "请求事件不是当前待处理正式事件。");
            }
            return doChooseOrdinaryEvent(userId, vup, session, choiceType, idempotencyKey);
        }
        if (session.getPendingFormalEventId() == null) {
            throw new GameException("EVENT_NOT_PENDING", "当前没有待处理正式事件。");
        }
        if (request.eventId() != null && !request.eventId().equals(session.getPendingFormalEventId())) {
            throw new GameException("EVENT_NOT_PENDING", "请求事件不是当前待处理正式事件。");
        }

        RiskDebt debt = riskDebtMapper.findById(session.getPendingFormalEventId());
        if (debt == null || !debt.getVupId().equals(vup.getId())) {
            throw new GameException("EVENT_NOT_PENDING", "待处理事件债务不存在。");
        }

        List<RiskDebt> dueDebtCandidates = dueDebtCandidates(vup, session);
        ActionResultDTO eventResult = resolveDebtEvent(vup, session, debt, choiceType);
        vupMapper.updateState(vup);
        riskDebtMapper.updateMitigation(debt);

        BusinessLog log = toBusinessLog(vup, session, debt, dueDebtCandidates, eventResult, idempotencyKey, choiceType);
        businessLogMapper.insert(log);
        List<RiskDebt> rolloverDebts = createRolloverDebtIfNeeded(vup, session, debt, log, choiceType);
        if (!rolloverDebts.isEmpty()) {
            addRolloverEvidence(eventResult, rolloverDebts.get(0), session);
            eventResult = eventResult.withDebtCreated(rolloverDebts.stream()
                    .map(rolloverDebt -> new DebtCreatedDTO(
                            rolloverDebt.getDebtType(),
                            rolloverDebt.getSeverity(),
                            Math.max(0, rolloverDebt.getDueDay() - session.getDay())
                    ))
                    .toList());
            log.setDebtIds(jsonService.write(java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(debt.getId()),
                            rolloverDebts.stream().map(RiskDebt::getId)
                    ).toList()));
            businessLogMapper.updateDebtIds(log);
        }

        var report = reportService.createReport(
                vup,
                session,
                eventResult,
                log,
                null,
                List.of(eventResult.evidenceRef())
        );
        log.setReportId(report.getId());
        businessLogMapper.attachReport(log);

        session.setPendingFormalEventId(null);
        session.setPendingEventResultJson(jsonService.write(eventResult));
        session.setReportId(report.getId());
        boolean endingReady = dayFlowService.finishSessionAfterReport(vup, session, report, log);
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(session.getPhase(), eventResult, null, report.getId(), endingReady, null, null);
    }

    private void addRolloverEvidence(ActionResultDTO eventResult, RiskDebt rolloverDebt, DaySession session) {
        if (eventResult.evidenceRef() == null) {
            return;
        }
        eventResult.evidenceRef().put("rolloverDebtId", rolloverDebt.getId());
        eventResult.evidenceRef().put("rolloverDebtType", rolloverDebt.getDebtType());
        eventResult.evidenceRef().put("rolloverSeverity", rolloverDebt.getSeverity());
        eventResult.evidenceRef().put("rolloverDueDay", rolloverDebt.getDueDay());
        eventResult.evidenceRef().put("rolloverRemainingDays", Math.max(0, rolloverDebt.getDueDay() - session.getDay()));
    }

    private DayResultDTO doChooseOrdinaryEvent(
            Long userId,
            Vup vup,
            DaySession session,
            String choiceType,
            String idempotencyKey
    ) {
        ActionResultDTO eventResult = resolveOrdinaryEvent(vup, session, choiceType);
        vupMapper.updateState(vup);

        BusinessLog log = toOrdinaryBusinessLog(vup, session, eventResult, idempotencyKey, choiceType);
        businessLogMapper.insert(log);
        RiskDebt ordinaryDebt = debtService.createForOrdinaryEventChoiceIfNeeded(
                vup,
                session,
                log,
                ordinaryEventKey(session),
                ordinaryEventText(session, "eventTitle", ordinaryEventKey(session)),
                ordinaryEventText(session, "eventType", "ORDINARY_EVENT"),
                choiceType
        );
        if (ordinaryDebt != null) {
            addOrdinaryDebtEvidence(eventResult, ordinaryDebt, session);
            eventResult = eventResult.withDebtCreated(List.of(new DebtCreatedDTO(
                    ordinaryDebt.getDebtType(),
                    ordinaryDebt.getSeverity(),
                    Math.max(0, ordinaryDebt.getDueDay() - session.getDay())
            )));
            log.setDebtIds(jsonService.write(List.of(ordinaryDebt.getId())));
            businessLogMapper.updateDebtIds(log);
        }

        var report = reportService.createReport(
                vup,
                session,
                eventResult,
                log,
                null,
                List.of(eventResult.evidenceRef())
        );
        log.setReportId(report.getId());
        businessLogMapper.attachReport(log);

        session.setFormalEventSlotStatus("USED");
        session.setFormalEventSource(null);
        session.setFormalEventPriority(0);
        session.setFormalEventRollDetailJson(null);
        session.setPendingEventResultJson(jsonService.write(eventResult));
        session.setReportId(report.getId());
        boolean endingReady = dayFlowService.finishSessionAfterReport(vup, session, report, log);
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(session.getPhase(), eventResult, null, report.getId(), endingReady, null, null);
    }

    private void addOrdinaryDebtEvidence(ActionResultDTO eventResult, RiskDebt debt, DaySession session) {
        if (eventResult.evidenceRef() == null) {
            return;
        }
        eventResult.evidenceRef().put("ordinaryDebtId", debt.getId());
        eventResult.evidenceRef().put("ordinaryDebtType", debt.getDebtType());
        eventResult.evidenceRef().put("ordinaryDebtSeverity", debt.getSeverity());
        eventResult.evidenceRef().put("ordinaryDebtDueDay", debt.getDueDay());
        eventResult.evidenceRef().put("ordinaryDebtRemainingDays", Math.max(0, debt.getDueDay() - session.getDay()));
    }

    private ActionResultDTO resolveDebtEvent(Vup vup, DaySession session, RiskDebt debt, String choiceType) {
        int originalSeverity = debt.getSeverity();
        EventOutcomeResolver.EventOutcome outcome = debtSeverityAdjustedOutcome(
                eventOutcomeResolver.resolveFormalEvent(vup, choiceType, debt.getDebtType(), true),
                choiceType,
                originalSeverity
        );
        rewardCalculator.applyToVup(vup, outcome.delta());

        debt.setStatus("CLEARED");
        debt.setSeverity(0);
        debt.setSummary(debt.getSummary() + debtResolutionSuffix(session, choiceType));
        dayFlowService.applyRouteScoreChange(vup, outcome.delta().routeType(), outcome.delta().routeScoreChange());

        return new ActionResultDTO(
                "EVENT_CHOICE",
                debtEventSummary(debt, session, choiceType, originalSeverity)
                        + debtSeverityImpactSummary(choiceType, originalSeverity),
                fanChange(outcome),
                outcome.delta().trueFanChange(),
                outcome.delta().funFanChange(),
                outcome.delta().unicornFanChange(),
                outcome.delta().ddFanChange(),
                outcome.delta().staminaChange(),
                0,
                outcome.delta().routeScoreChange(),
                debtEventEvidenceRef(debt, choiceType, originalSeverity, outcome)
        );
    }

    private EventOutcomeResolver.EventOutcome debtSeverityAdjustedOutcome(
            EventOutcomeResolver.EventOutcome outcome,
            String choiceType,
            int originalSeverity
    ) {
        int pressure = Math.max(0, Math.min(3, originalSeverity - 2));
        if (pressure == 0) {
            return outcome;
        }

        RewardDelta delta = outcome.delta();
        int trueFanChange = delta.trueFanChange();
        int funFanChange = delta.funFanChange();
        int ddFanChange = delta.ddFanChange();
        int popularityChange = delta.popularityChange();
        int watchHeatChange = delta.watchHeatChange();
        int reputationChange = delta.reputationChange();
        int memeChange = delta.memeChange();
        int staminaChange = delta.staminaChange();
        int routeScoreChange = delta.routeScoreChange();

        if ("traffic".equals(choiceType)) {
            funFanChange += pressure * 5;
            ddFanChange += pressure * 2;
            popularityChange += pressure * 8;
            watchHeatChange += pressure * 6;
            reputationChange -= pressure * 2;
            memeChange += pressure;
            routeScoreChange += Math.min(2, pressure);
        } else if ("meme".equals(choiceType)) {
            funFanChange += pressure * 3;
            popularityChange += pressure * 5;
            watchHeatChange += pressure * 3;
            reputationChange -= pressure;
            memeChange += pressure * 3;
            routeScoreChange += Math.min(2, pressure);
        } else {
            trueFanChange -= pressure * 3;
            funFanChange -= pressure;
            popularityChange -= pressure * 2;
            watchHeatChange += pressure * 3;
            reputationChange -= pressure * 2;
            staminaChange -= pressure;
            routeScoreChange -= Math.min(1, pressure);
        }

        Map<String, Object> evidence = new LinkedHashMap<>(
                delta.evidenceRef() == null ? Map.of() : delta.evidenceRef()
        );
        evidence.put("severityPressure", pressure);
        evidence.put("severityAdjusted", true);

        RewardDelta adjusted = new RewardDelta(
                trueFanChange,
                funFanChange,
                delta.unicornFanChange(),
                ddFanChange,
                trueFanChange + funFanChange + delta.unicornFanChange() + ddFanChange,
                popularityChange,
                watchHeatChange,
                reputationChange,
                memeChange,
                delta.commercialChange(),
                delta.coinChange(),
                delta.inspirationChange(),
                staminaChange,
                routeScoreChange,
                delta.routeType(),
                evidence,
                delta.multiplierDetail(),
                delta.capDetail(),
                delta.clampDetail(),
                delta.rngDetail(),
                delta.weightDetail()
        );
        return new EventOutcomeResolver.EventOutcome(adjusted, outcome.summary(), outcome.safe(), outcome.hasRisk());
    }

    private String normalizeChoiceType(String choiceType) {
        if ("safe".equals(choiceType) || "traffic".equals(choiceType) || "meme".equals(choiceType)) {
            return choiceType;
        }
        throw new GameException("CHOICE_NOT_AVAILABLE", "这个处理方式现在不可用。");
    }

    private String debtResolutionSuffix(DaySession session, String choiceType) {
        if ("FAN_TOPIC_ESCALATION".equals(session.getFormalEventSource())) {
            return switch (choiceType) {
                case "traffic" -> " 粉丝群升级后选择硬接流量，旧帖暂时变成节目效果，但群聊截图换了新楼继续发酵。";
                case "meme" -> " 粉丝群升级后选择顺势玩梗，旧帖换成梗图流通，但群聊翻旧账证据仍保留。";
                default -> " 粉丝群升级后选择降温处理，热度被压住，但群聊翻旧账证据仍保留。";
            };
        }
        return switch (choiceType) {
            case "traffic" -> " 到期后选择硬接流量，旧楼暂时变成主会场收益，但新短账继续发酵。";
            case "meme" -> " 到期后选择顺势玩梗，旧证据被包装成切片素材，新一轮旧账继续发酵。";
            default -> " 到期后选择降温处理，热度被压住，但证据仍保留。";
        };
    }

    private String debtEventSummary(RiskDebt debt, DaySession session, String choiceType, int originalSeverity) {
        if ("traffic".equals(choiceType)) {
            return RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                    + "到期，你选择硬接流量。主会场人气和围观热度上涨，乐子人与DD进场，口碑承压，"
                    + "旧账被结算但没有免费消失，新的短期旧账会按原严重度" + originalSeverity + "继续入账。";
        }
        if ("meme".equals(choiceType)) {
            return RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                    + "到期，你选择顺势玩梗。梗浓度和切片路线收益上涨，旧证据换成梗图继续传播，"
                    + "新一轮短期旧账会按原严重度" + originalSeverity + "继续入账。";
        }
        if ("FAN_TOPIC_ESCALATION".equals(session.getFormalEventSource())) {
            return "粉丝群把未结清债务提前翻旧账，你发出降温说明。房管撤掉重复截图，老粉暂时不继续写小作文，但"
                    + RiskDebtLabels.debtTypeLabel(debt.getDebtType())
                    + "的证据仍被留档。";
        }
        if ("UNICORN_EXPECTATION".equals(debt.getDebtType())) {
            return "独角兽期待到期，你选择降温安抚。小作文没有继续扩散，但榜一排班表和陪伴期待仍被留档。";
        }
        if ("COMMERCIAL_BACKLASH".equals(debt.getDebtType())) {
            return "商业反噬到期，你发出降温说明。味儿变了的弹幕被压住，品牌避险暂缓，但商单味证据仍被留档。";
        }
        if ("BOOMERANG_CLIP".equals(debt.getDebtType())) {
            return "回旋镖切片到期，你补上上下文降温。录播组没有继续开庭，但笑点、上下文和切片证据仍被留档。";
        }
        if ("TITLE_BACKFIRE".equals(debt.getDebtType())) {
            return "标题党反噬到期，你选择降温解释。标题组补口径，录播组暂缓开庭，但高音贷款和考据楼证据仍被留档。";
        }
        return "未归档旧账到期，你选择降温解释。楼友没有完全散场，但主会场暂时没坐满。";
    }

    private String debtSeverityImpactSummary(String choiceType, int originalSeverity) {
        if (originalSeverity <= 2) {
            return "";
        }
        if ("traffic".equals(choiceType)) {
            return " 这笔旧账严重度较高，硬接后流量更猛，口碑和围观压力也被一起放大。";
        }
        if ("meme".equals(choiceType)) {
            return " 这笔旧账严重度较高，玩梗更容易出圈，但切片扩散和后续误读也更难收。";
        }
        return " 这笔旧账严重度较高，降温能止血，但没法完全抹平围观压力。";
    }

    private ActionResultDTO resolveOrdinaryEvent(Vup vup, DaySession session, String choiceType) {
        String eventKey = ordinaryEventKey(session);
        String eventType = ordinaryEventText(session, "eventType", "ORDINARY_EVENT");
        EventOutcomeResolver.EventOutcome outcome = eventOutcomeResolver.resolveFormalEvent(vup, choiceType, eventKey, false);
        Map<String, Object> rollDetail = ordinaryRollDetail(session);
        RewardDelta delta = ordinaryEventFlavorAdjustedDelta(outcome.delta(), eventKey, eventType, choiceType, rollDetail);
        EventOutcomeResolver.EventOutcome adjustedOutcome = new EventOutcomeResolver.EventOutcome(
                delta,
                outcome.summary(),
                outcome.safe(),
                outcome.hasRisk()
        );
        rewardCalculator.applyToVup(vup, delta);
        dayFlowService.applyRouteScoreChange(vup, delta.routeType(), delta.routeScoreChange());

        Map<String, Object> evidence = eventEvidenceRef("ordinary_event", null, eventKey, choiceType, adjustedOutcome);
        evidence.put("eventTitle", ordinaryEventText(session, "eventTitle", eventKey));
        evidence.put("eventType", eventType);
        evidence.put("rollDetail", rollDetail);
        if (midgameEventContentService.isMidgameRoll(rollDetail)) {
            evidence.put("midgameEventApplied", true);
            evidence.put("midgameEventId", textValue(rollDetail, "midgameEventId"));
            evidence.put("midgameWindow", textValue(rollDetail, "midgameWindow"));
            evidence.put("midgameRouteGroup", textValue(rollDetail, "routeGroup"));
            evidence.put("midgamePrimaryRouteType", textValue(rollDetail, "primaryRouteType"));
            evidence.put("contentVersion", textValue(rollDetail, "contentVersion"));
        }
        if (commercialRouteContentService.isLateGameRoll(rollDetail)) {
            evidence.put("lateGameEventApplied", true);
            evidence.put("lateGameEventId", textValue(rollDetail, "lateGameEventId"));
            evidence.put("lateGameWindow", textValue(rollDetail, "lateGameWindow"));
            evidence.put("lateGameRouteGroup", textValue(rollDetail, "routeGroup"));
            evidence.put("lateGamePrimaryRouteType", textValue(rollDetail, "primaryRouteType"));
            evidence.put("contentVersion", textValue(rollDetail, "contentVersion"));
        }

        return new ActionResultDTO(
                "EVENT_CHOICE",
                ordinaryEventSummary(session, choiceType),
                fanChange(adjustedOutcome),
                delta.trueFanChange(),
                delta.funFanChange(),
                delta.unicornFanChange(),
                delta.ddFanChange(),
                delta.staminaChange(),
                0,
                delta.routeScoreChange(),
                evidence
        );
    }

    private RewardDelta ordinaryEventFlavorAdjustedDelta(
            RewardDelta delta,
            String eventKey,
            String eventType,
            String choiceType,
            Map<String, Object> rollDetail
    ) {
        int trueFanChange = delta.trueFanChange();
        int funFanChange = delta.funFanChange();
        int unicornFanChange = delta.unicornFanChange();
        int ddFanChange = delta.ddFanChange();
        int popularityChange = delta.popularityChange();
        int watchHeatChange = delta.watchHeatChange();
        int reputationChange = delta.reputationChange();
        int memeChange = delta.memeChange();
        int commercialChange = delta.commercialChange();
        int coinChange = delta.coinChange();
        int inspirationChange = delta.inspirationChange();
        int staminaChange = delta.staminaChange();
        int routeScoreChange = delta.routeScoreChange();
        String routeType = delta.routeType();
        Map<String, Object> modifiers = new LinkedHashMap<>();

        if (midgameEventContentService.isMidgameRoll(rollDetail)) {
            String routeGroup = textValue(rollDetail, "routeGroup");
            String primaryRouteType = textValue(rollDetail, "primaryRouteType");
            String coreRouteType = textValue(rollDetail, "coreRouteType");
            if (coreRouteType.isBlank()) {
                coreRouteType = midgameEventContentService.coreRouteType(primaryRouteType);
            }
            routeType = coreRouteType;
            routeScoreChange += "unknown".equals(routeGroup) ? 0 : ("safe".equals(choiceType) ? 2 : 3);
            switch (routeGroup) {
                case "unknown" -> {
                    trueFanChange += "safe".equals(choiceType) ? 1 : 0;
                    popularityChange += "traffic".equals(choiceType) ? 1 : 0;
                    watchHeatChange += "traffic".equals(choiceType) ? 1 : -1;
                    reputationChange += "safe".equals(choiceType) ? 1 : 0;
                    routeScoreChange = 0;
                    routeType = RouteType.UNKNOWN.name();
                }
                case "burst" -> {
                    funFanChange += "safe".equals(choiceType) ? 4 : 10;
                    memeChange += "meme".equals(choiceType) ? 4 : 1;
                    popularityChange += "traffic".equals(choiceType) ? 10 : 3;
                    reputationChange += "safe".equals(choiceType) ? 1 : -1;
                }
                case "heat" -> {
                    funFanChange += "safe".equals(choiceType) ? 2 : 7;
                    popularityChange += "traffic".equals(choiceType) ? 16 : 5;
                    watchHeatChange += "safe".equals(choiceType) ? -1 : 8;
                    reputationChange += "safe".equals(choiceType) ? 1 : -3;
                    memeChange += "meme".equals(choiceType) ? 3 : 0;
                }
                case "singing" -> {
                    trueFanChange += "safe".equals(choiceType) ? 8 : 4;
                    ddFanChange += 2;
                    popularityChange += "traffic".equals(choiceType) ? 8 : 3;
                    reputationChange += "safe".equals(choiceType) ? 2 : -1;
                    inspirationChange += "safe".equals(choiceType) ? 1 : 0;
                }
                case "relationship" -> {
                    trueFanChange += "safe".equals(choiceType) ? 4 : 1;
                    unicornFanChange += "safe".equals(choiceType) ? 2 : 8;
                    commercialChange += "traffic".equals(choiceType) ? 4 : 1;
                    watchHeatChange += "safe".equals(choiceType) ? 0 : 4;
                    reputationChange += "safe".equals(choiceType) ? 2 : -1;
                }
                case "social" -> {
                    ddFanChange += "safe".equals(choiceType) ? 5 : 12;
                    popularityChange += "traffic".equals(choiceType) ? 10 : 3;
                    trueFanChange += "safe".equals(choiceType) ? 3 : 0;
                    reputationChange += "safe".equals(choiceType) ? 1 : -1;
                }
                default -> {
                    trueFanChange += "safe".equals(choiceType) ? 7 : 3;
                    reputationChange += "safe".equals(choiceType) ? 2 : -1;
                    watchHeatChange += "traffic".equals(choiceType) ? 5 : -1;
                    funFanChange += "meme".equals(choiceType) ? 5 : 0;
                }
            }
            modifiers.put("midgameContentPack", textValue(rollDetail, "contentVersion"));
            modifiers.put("midgameEventId", textValue(rollDetail, "midgameEventId"));
            modifiers.put("midgameWindow", textValue(rollDetail, "midgameWindow"));
            modifiers.put("midgameRouteGroup", routeGroup);
            modifiers.put("midgamePrimaryRouteType", primaryRouteType);
            modifiers.put("midgameCoreRouteType", coreRouteType);
        }

        if (commercialRouteContentService.isLateGameRoll(rollDetail)) {
            String routeGroup = textValue(rollDetail, "routeGroup");
            String primaryRouteType = textValue(rollDetail, "primaryRouteType");
            String coreRouteType = textValue(rollDetail, "coreRouteType");
            if (coreRouteType.isBlank()) {
                coreRouteType = commercialRouteContentService.coreRouteType(primaryRouteType);
            }
            routeType = coreRouteType;
            routeScoreChange += "unknown".equals(routeGroup) ? 0 : ("safe".equals(choiceType) ? 3 : 5);
            switch (routeGroup) {
                case "unknown" -> {
                    trueFanChange += "safe".equals(choiceType) ? 1 : 0;
                    funFanChange += "meme".equals(choiceType) ? 2 : 0;
                    popularityChange += "traffic".equals(choiceType) ? 2 : 0;
                    watchHeatChange += "traffic".equals(choiceType) ? 2 : -1;
                    reputationChange += "safe".equals(choiceType) ? 1 : 0;
                    memeChange += "meme".equals(choiceType) ? 2 : 0;
                    routeScoreChange = 0;
                    routeType = RouteType.UNKNOWN.name();
                }
                case "burst" -> {
                    funFanChange += "safe".equals(choiceType) ? 5 : 12;
                    memeChange += "meme".equals(choiceType) ? 6 : 2;
                    popularityChange += "traffic".equals(choiceType) ? 12 : 4;
                    reputationChange += "safe".equals(choiceType) ? 1 : -1;
                }
                case "heat" -> {
                    funFanChange += "safe".equals(choiceType) ? 2 : 8;
                    popularityChange += "traffic".equals(choiceType) ? 18 : 6;
                    watchHeatChange += "safe".equals(choiceType) ? -1 : 9;
                    reputationChange += "safe".equals(choiceType) ? 2 : -3;
                    memeChange += "meme".equals(choiceType) ? 4 : 0;
                }
                case "singing" -> {
                    trueFanChange += "safe".equals(choiceType) ? 10 : 5;
                    ddFanChange += "traffic".equals(choiceType) ? 4 : 2;
                    popularityChange += "traffic".equals(choiceType) ? 10 : 3;
                    reputationChange += "safe".equals(choiceType) ? 3 : -1;
                    inspirationChange += "safe".equals(choiceType) ? 1 : 0;
                }
                case "relationship" -> {
                    trueFanChange += "safe".equals(choiceType) ? 5 : 1;
                    unicornFanChange += "safe".equals(choiceType) ? 3 : 10;
                    commercialChange += "traffic".equals(choiceType) ? 5 : 1;
                    watchHeatChange += "safe".equals(choiceType) ? 0 : 5;
                    reputationChange += "safe".equals(choiceType) ? 2 : -2;
                }
                case "social" -> {
                    ddFanChange += "safe".equals(choiceType) ? 6 : 14;
                    popularityChange += "traffic".equals(choiceType) ? 12 : 4;
                    trueFanChange += "safe".equals(choiceType) ? 4 : 0;
                    reputationChange += "safe".equals(choiceType) ? 2 : -1;
                }
                default -> {
                    trueFanChange += "safe".equals(choiceType) ? 8 : 4;
                    reputationChange += "safe".equals(choiceType) ? 3 : -1;
                    watchHeatChange += "traffic".equals(choiceType) ? 6 : -1;
                    funFanChange += "meme".equals(choiceType) ? 6 : 0;
                }
            }
            modifiers.put("lateGameContentPack", textValue(rollDetail, "contentVersion"));
            modifiers.put("lateGameEventId", textValue(rollDetail, "lateGameEventId"));
            modifiers.put("lateGameWindow", textValue(rollDetail, "lateGameWindow"));
            modifiers.put("lateGameRouteGroup", routeGroup);
            modifiers.put("lateGamePrimaryRouteType", primaryRouteType);
            modifiers.put("lateGameCoreRouteType", coreRouteType);
        }

        switch (eventKey == null ? "" : eventKey) {
            case "TITLE_TEAM_EARLY" -> {
                inspirationChange += 1;
                modifiers.put("inspiration", 1);
            }
            case "CLIP_POPULAR" -> {
                funFanChange += 10;
                popularityChange += 12;
                memeChange += 1;
                routeScoreChange += 1;
                routeType = RouteType.SLICE_SAINT.name();
                modifiers.put("clipMomentum", 1);
            }
            case "LOW_PRESSURE_COMEDY", "OLD_FAN_ESSAY", "SCHEDULE_PATCH_NOTES", "COZY_DINNER_TABLE" -> {
                trueFanChange += 6;
                reputationChange += 2;
                routeScoreChange += 1;
                routeType = RouteType.ELECTRONIC_PICKLE.name();
                modifiers.put("stableCommunity", 1);
            }
            case "SONG_REQUEST_QUEUE" -> {
                trueFanChange += 8;
                routeScoreChange += 1;
                routeType = RouteType.SINGING_IDOL.name();
                modifiers.put("singingEvidence", 1);
            }
            case "FAN_ART_DELIVERY", "ARCHIVE_TIMESTAMP", "CHAT_INSIDE_JOKE", "OLD_CLIP_RESURFACE" -> {
                funFanChange += 5;
                memeChange += 2;
                routeScoreChange += 1;
                routeType = RouteType.SLICE_SAINT.name();
                modifiers.put("memeMaterial", 1);
            }
            case "COLLAB_CALENDAR_PING", "COLLAB_MISREAD" -> {
                ddFanChange += 6;
                routeScoreChange += 1;
                routeType = RouteType.SOCIAL_COLLAB.name();
                modifiers.put("collabEvidence", 1);
            }
            case "DANCE_MOVE_REUSED" -> {
                funFanChange += 6;
                memeChange += 2;
                routeScoreChange += 1;
                routeType = RouteType.DANCE_MEME.name();
                modifiers.put("danceLoop", 1);
            }
            case "MOD_PINNED_RULES", "MELINE_SAVE" -> {
                reputationChange += 3;
                watchHeatChange -= 2;
                modifiers.put("riskCooldown", 1);
            }
            case "BOSS_QUESTION", "BOSS_GIFT_TIMING", "SPONSOR_BRIEF_TOO_LONG" -> {
                commercialChange += 3;
                unicornFanChange += 3;
                modifiers.put("commercialPressure", 1);
            }
            case "PLATFORM_ALGORITHM", "HOT_SEARCH_SIDE_DOOR", "TITLE_TEAM_PUSH", "TITLE_TOO_EFFECTIVE", "HOST_ESCAPE" -> {
                popularityChange += 10;
                watchHeatChange += 5;
                memeChange += 1;
                routeScoreChange += 1;
                routeType = RouteType.BLACK_RED_MAIN_STAGE.name();
                modifiers.put("trafficSpike", 1);
            }
            case "INTERNET_MEMORY", "RESEARCH_TEAM_OLD_DEBT", "COMMENT_SECTION_TRIAL" -> {
                watchHeatChange += 6;
                reputationChange -= 2;
                routeScoreChange += "safe".equals(choiceType) ? 0 : 1;
                routeType = "safe".equals(choiceType) ? routeType : RouteType.BLACK_RED_MAIN_STAGE.name();
                modifiers.put("archivePressure", 1);
            }
            case "MEME_OVERCOOKED" -> {
                funFanChange += 4;
                memeChange += 3;
                reputationChange -= 2;
                modifiers.put("memeFatigue", 1);
            }
            case "FAN_GROUP_TEMPERATURE", "BOUNDARY_LINE_BLUR", "MOD_SLIP" -> {
                trueFanChange -= 3;
                reputationChange -= 2;
                watchHeatChange += 2;
                modifiers.put("communityPressure", 1);
            }
            case "DD_ONE_STOP" -> {
                ddFanChange += 5;
                modifiers.put("ddPassThrough", 1);
            }
            case "DANMAKU_LIAR", "RECORDING_TEAM_READY", "ALGORITHM_TEST_BUCKET" -> {
                popularityChange += 6;
                watchHeatChange += 3;
                modifiers.put("visibilityWindow", 1);
            }
            default -> {
                if ("POSITIVE".equals(eventType)) {
                    reputationChange += 1;
                } else if ("NEGATIVE".equals(eventType)) {
                    reputationChange -= 1;
                }
            }
        }

        Map<String, Object> evidence = new LinkedHashMap<>(delta.evidenceRef() == null ? Map.of() : delta.evidenceRef());
        if (!modifiers.isEmpty()) {
            evidence.put("ordinaryFlavorApplied", true);
            evidence.put("ordinaryFlavorModifiers", modifiers);
            if (modifiers.containsKey("midgameEventId")) {
                evidence.put("midgameEventApplied", true);
                evidence.put("midgameEventId", modifiers.get("midgameEventId"));
                evidence.put("midgameWindow", modifiers.get("midgameWindow"));
                evidence.put("midgameRouteGroup", modifiers.get("midgameRouteGroup"));
            }
            if (modifiers.containsKey("lateGameEventId")) {
                evidence.put("lateGameEventApplied", true);
                evidence.put("lateGameEventId", modifiers.get("lateGameEventId"));
                evidence.put("lateGameWindow", modifiers.get("lateGameWindow"));
                evidence.put("lateGameRouteGroup", modifiers.get("lateGameRouteGroup"));
            }
        }

        int fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        return new RewardDelta(
                trueFanChange,
                funFanChange,
                unicornFanChange,
                ddFanChange,
                fanChange,
                popularityChange,
                watchHeatChange,
                reputationChange,
                memeChange,
                commercialChange,
                coinChange,
                inspirationChange,
                staminaChange,
                routeScoreChange,
                routeType,
                evidence,
                delta.multiplierDetail(),
                delta.capDetail(),
                delta.clampDetail(),
                delta.rngDetail(),
                delta.weightDetail()
        );
    }

    private String ordinaryEventSummary(DaySession session, String choiceType) {
        if (!"REST_SAVED_MELTDOWN".equals(ordinaryEventKey(session))) {
            String title = ordinaryEventText(session, "eventTitle", "突发小事件");
            return switch (choiceType) {
                case "traffic" -> title + "发酵成了临时主会场，你选择硬接流量，围观热度上升但口碑承压。";
                case "meme" -> title + "被切片组抓住了，你选择顺势玩梗，梗浓度和切片路线收益上涨。";
                default -> title + "冒头，你选择先降温说明，把事件控制在可复盘范围内。";
            };
        }
        return switch (choiceType) {
            case "traffic" -> "连续低压休息把潜在翻车熬成流量话题，你选择硬接讨论，围观热度上升但口碑承压。";
            case "meme" -> "连续低压休息把潜在翻车熬成无事发生，你选择顺势玩梗，楼友剪成小切片继续传播。";
            default -> "连续低压休息把潜在翻车熬成无事发生，老粉续了一碗电子榨菜，楼友今天没开成大庭。";
        };
    }

    private String ordinaryEventKey(DaySession session) {
        return ordinaryEventText(session, "eventKey", "REST_SAVED_MELTDOWN");
    }

    private Map<String, Object> ordinaryRollDetail(DaySession session) {
        if (session.getFormalEventRollDetailJson() == null || session.getFormalEventRollDetailJson().isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(session.getFormalEventRollDetailJson());
    }

    private String ordinaryEventText(DaySession session, String key, String fallback) {
        return textValue(ordinaryRollDetail(session), key, fallback);
    }

    private String textValue(Map<String, Object> source, String key) {
        return textValue(source, key, "");
    }

    private String textValue(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private Map<String, Object> eventEvidenceRef(
            String type,
            Long debtId,
            String eventKey,
            String choiceType,
            EventOutcomeResolver.EventOutcome outcome
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("type", type);
        if (debtId != null) {
            evidence.put("id", debtId);
        }
        evidence.put("eventKey", eventKey);
        evidence.put("debtType", eventKey);
        evidence.put("choiceType", choiceType);
        evidence.put("popularityChange", outcome.delta().popularityChange());
        evidence.put("watchHeatChange", outcome.delta().watchHeatChange());
        evidence.put("reputationChange", outcome.delta().reputationChange());
        evidence.put("memeChange", outcome.delta().memeChange());
        evidence.put("routeType", outcome.delta().routeType());
        evidence.put("routeScoreChange", outcome.delta().routeScoreChange());
        evidence.put("hasRisk", outcome.hasRisk());
        return evidence;
    }

    private Map<String, Object> debtEventEvidenceRef(
            RiskDebt debt,
            String choiceType,
            int originalSeverity,
            EventOutcomeResolver.EventOutcome outcome
    ) {
        Map<String, Object> evidence = eventEvidenceRef("risk_debt", debt.getId(), debt.getDebtType(), choiceType, outcome);
        evidence.put("originalSeverity", originalSeverity);
        return evidence;
    }

    private int fanChange(EventOutcomeResolver.EventOutcome outcome) {
        return outcome.delta().trueFanChange()
                + outcome.delta().funFanChange()
                + outcome.delta().unicornFanChange()
                + outcome.delta().ddFanChange();
    }

    private List<RiskDebt> createRolloverDebtIfNeeded(
            Vup vup,
            DaySession session,
            RiskDebt debt,
            BusinessLog log,
            String choiceType
    ) {
        RiskDebt rolloverDebt = debtService.createForEventChoiceIfNeeded(
                vup,
                session,
                debt,
                choiceType,
                eventOriginalSeverity(log),
                log.getId()
        );
        return rolloverDebt == null ? List.of() : List.of(rolloverDebt);
    }

    private int eventOriginalSeverity(BusinessLog log) {
        if (log.getWeightDetail() == null || log.getWeightDetail().isBlank()) {
            return 1;
        }
        Object value = jsonService.readMap(log.getWeightDetail()).get("hitOriginalSeverity");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private List<RiskDebt> dueDebtCandidates(Vup vup, DaySession session) {
        return riskDebtMapper.findOpenByVupId(vup.getId()).stream()
                .filter(candidate -> candidate.getDueDay() <= session.getDay())
                .filter(candidate -> candidate.getCreateDay() < session.getDay())
                .toList();
    }

    private BusinessLog toBusinessLog(
            Vup vup,
            DaySession session,
            RiskDebt debt,
            List<RiskDebt> dueDebtCandidates,
            ActionResultDTO result,
            String idempotencyKey,
            String choiceType
    ) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction("EVENT_CHOICE");
        log.setEventId(debt.getId());
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
        log.setMultiplierDetail(jsonService.write(Map.of("pipeline", "P0_DEBT_RETURN")));
        log.setCapDetail(jsonService.write(Map.of("eventSlot", "formal")));
        int originalSeverity = Math.max(1, numberEvidence(result, "originalSeverity"));
        log.setClampDetail(jsonService.write(Map.of(
                "debtSeverity", originalSeverity,
                "watchHeat", vup.getWatchHeat(),
                "reputation", vup.getReputation(),
                "memeLevel", vup.getMemeLevel()
        )));
        log.setWeightDetail(jsonService.write(debtReplayWeightDetail(session, debt, dueDebtCandidates, originalSeverity)));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hitDebtId", debt.getId(),
                "hitDebtType", debt.getDebtType()
        )));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(Map.of(
                routeTypeEvidence(result), result.routeScoreChange(),
                "source", "event:" + debt.getDebtType() + ":" + choiceType,
                "debtType", debt.getDebtType(),
                "choiceType", choiceType
        )));
        log.setDebtIds(jsonService.write(java.util.List.of(debt.getId())));
        log.setAccidentMaterialIds(jsonService.write(List.of(accidentMaterialIdFor(debt.getDebtType()))));
        log.setEndingRefFlag(true);
        return log;
    }

    private String accidentMaterialIdFor(String debtType) {
        return switch (debtType) {
            case "BOOMERANG_CLIP" -> "BOOMERANG_CLIP_CONTEXT";
            case "UNICORN_EXPECTATION" -> "UNICORN_EXPECTATION_SCREENSHOT";
            case "COMMERCIAL_BACKLASH" -> "COMMERCIAL_BACKLASH_RECEIPT";
            default -> "TITLE_BACKFIRE_CONTEXT";
        };
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

    private Map<String, Object> debtReplayWeightDetail(DaySession session, RiskDebt hitDebt, List<RiskDebt> dueDebtCandidates, int originalSeverity) {
        boolean fanTopicEscalation = "FAN_TOPIC_ESCALATION".equals(session.getFormalEventSource());
        List<RiskDebt> candidates = fanTopicEscalation ? List.of(hitDebt) : dueDebtCandidates;
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rollType", fanTopicEscalation ? "fan_topic_escalation" : "risk_debt_due");
        detail.put("debtCandidates", debtReplayCandidates(candidates));
        detail.put("weights", debtReplayWeights(candidates, hitDebt));
        detail.put("hitDebtId", hitDebt.getId());
        detail.put("hitDebtType", hitDebt.getDebtType());
        detail.put("hitOriginalSeverity", originalSeverity);
        detail.put("hitDueDay", hitDebt.getDueDay());
        detail.put("targetDebtId", hitDebt.getId());
        detail.put("debtType", hitDebt.getDebtType());
        if (fanTopicEscalation) {
            detail.put("formalEventSource", session.getFormalEventSource());
            detail.put("formalEventPriority", session.getFormalEventPriority());
        }
        return detail;
    }

    private List<Map<String, Object>> debtReplayCandidates(List<RiskDebt> dueDebtCandidates) {
        return dueDebtCandidates.stream()
                .map(debt -> {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("id", debt.getId());
                    candidate.put("debtType", debt.getDebtType());
                    candidate.put("severity", debt.getSeverity());
                    candidate.put("createDay", debt.getCreateDay());
                    candidate.put("dueDay", debt.getDueDay());
                    candidate.put("sourceAction", debt.getSourceAction());
                    candidate.put("sourceTitle", debt.getSourceTitle());
                    return candidate;
                })
                .toList();
    }

    private Map<String, Integer> debtReplayWeights(List<RiskDebt> dueDebtCandidates, RiskDebt hitDebt) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (RiskDebt candidate : dueDebtCandidates) {
            weights.put(String.valueOf(candidate.getId()), candidate.getId().equals(hitDebt.getId()) ? 1 : 0);
        }
        return weights;
    }

    private BusinessLog toOrdinaryBusinessLog(Vup vup, DaySession session, ActionResultDTO result, String idempotencyKey, String choiceType) {
        String eventKey = ordinaryEventKey(session);
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction("EVENT_CHOICE");
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
        log.setMultiplierDetail(jsonService.write(Map.of("pipeline", "P0_ORDINARY_EVENT")));
        log.setCapDetail(jsonService.write(Map.of("eventSlot", "formal")));
        log.setClampDetail(jsonService.write(Map.of("watchHeat", vup.getWatchHeat(), "reputation", vup.getReputation())));
        log.setWeightDetail(ordinaryEventWeightDetail(session));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hitEventKey", eventKey
        )));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(Map.of(
                routeTypeEvidence(result), result.routeScoreChange(),
                "source", "event:" + eventKey + ":" + choiceType,
                "eventKey", eventKey,
                "choiceType", choiceType
        )));
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(true);
        return log;
    }

    private String ordinaryEventWeightDetail(DaySession session) {
        if (session.getFormalEventRollDetailJson() != null && !session.getFormalEventRollDetailJson().isBlank()) {
            return session.getFormalEventRollDetailJson();
        }
        return jsonService.write(Map.of(
                "rollType", "ordinary_event_roll",
                "eventKey", "REST_SAVED_MELTDOWN",
                "eventTitle", "低压运营救场",
                "source", "ordinary",
                "eventCandidates", List.of(Map.of("eventKey", "REST_SAVED_MELTDOWN", "eventType", "ORDINARY_EVENT")),
                "weights", Map.of("REST_SAVED_MELTDOWN", 1),
                "hitEventKey", "REST_SAVED_MELTDOWN"
        ));
    }

}
