package com.example.vupworld.service.dev;

import com.example.vupworld.service.infra.PasswordHasher;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.ActionService;
import com.example.vupworld.service.TitleService;
import com.example.vupworld.service.core.DayService;
import com.example.vupworld.service.event.EventService;
import com.example.vupworld.service.event.InteractionService;
import com.example.vupworld.service.operating.OperatingPressureService;
import com.example.vupworld.service.risk.RiskToolService;
import com.example.vupworld.service.report.ReportService;
import com.example.vupworld.service.ending.EndingService;
import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.VupStatus;
import com.example.vupworld.dto.ActionDtos.ChooseTitleRequest;
import com.example.vupworld.dto.ActionDtos.SubmitActionRequest;
import com.example.vupworld.dto.DayDtos.NextDayRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetResult;
import com.example.vupworld.dto.DevDemoDtos.DemoFastForwardRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoLastErrorDTO;
import com.example.vupworld.dto.DevDemoDtos.DemoRunScriptRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoRunScriptResult;
import com.example.vupworld.dto.DevDemoDtos.DemoStatusResult;
import com.example.vupworld.dto.EndingDtos.EndingReviewDTO;
import com.example.vupworld.dto.EventDtos.ChooseEventRequest;
import com.example.vupworld.dto.InteractionDtos.ChooseInteractionRequest;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import com.example.vupworld.dto.VupDtos.CreateVupRequest;
import com.example.vupworld.mapper.UserAccountMapper;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.UserAccount;
import com.example.vupworld.model.Vup;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!prod & (dev | test | manual)")
public class DevDemoService {
    private static final String DEMO_USERNAME = "demo_teacher";
    private static final String DEMO_PASSWORD = "demo-pass";
    private static final String DEMO_NICKNAME = "本地试玩账号";
    private static final String DEMO_VUP_NAME = "演示小灯";
    private static final List<String> SUPPORTED_STRATEGIES = List.of(
            "steady",
            "clip",
            "black_red",
            "social",
            "singing",
            "cyber_girlfriend",
            "main_stage_king",
            "glorious_graduation",
            "idle",
            "defense",
            "random"
    );
    private static final String SUPPORTED_STRATEGY_LABEL = String.join("、", SUPPORTED_STRATEGIES);
    private static final int RUN_SEED_MAX_LENGTH = 64;
    private static final ActionType[] RANDOM_ACTION_CYCLE = {
            ActionType.PUBLISH_VIDEO,
            ActionType.PUBLISH_CLIP,
            ActionType.NPC_INTERACT,
            ActionType.FAN_GROUP_MAINTAIN,
            ActionType.TRAIN_SONG,
            ActionType.REST,
            ActionType.TRAIN_DANCE,
            ActionType.TRAIN_TALK
    };
    private static final ActionType[] RANDOM_FINAL_ACTION_CYCLE = {
            ActionType.TRAIN_TALK,
            ActionType.FAN_GROUP_MAINTAIN,
            ActionType.REST,
            ActionType.TRAIN_SONG,
            ActionType.TRAIN_DANCE,
            ActionType.NPC_INTERACT
    };

    private final UserAccountMapper userAccountMapper;
    private final BusinessLogMapper businessLogMapper;
    private final VupMapper vupMapper;
    private final PasswordHasher passwordHasher;
    private final VupService vupService;
    private final ActionService actionService;
    private final TitleService titleService;
    private final DayService dayService;
    private final EventService eventService;
    private final InteractionService interactionService;
    private final RiskToolService riskToolService;
    private final ReportService reportService;
    private final EndingService endingService;
    private final BalanceConfig balanceConfig;
    private final OperatingPressureService operatingPressureService;
    private final Map<Long, DemoLastErrorDTO> lastErrors = new ConcurrentHashMap<>();

    public DevDemoService(
            UserAccountMapper userAccountMapper,
            BusinessLogMapper businessLogMapper,
            VupMapper vupMapper,
            PasswordHasher passwordHasher,
            VupService vupService,
            ActionService actionService,
            TitleService titleService,
            DayService dayService,
            EventService eventService,
            InteractionService interactionService,
            RiskToolService riskToolService,
            ReportService reportService,
            EndingService endingService,
            BalanceConfig balanceConfig,
            OperatingPressureService operatingPressureService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.businessLogMapper = businessLogMapper;
        this.vupMapper = vupMapper;
        this.passwordHasher = passwordHasher;
        this.vupService = vupService;
        this.actionService = actionService;
        this.titleService = titleService;
        this.dayService = dayService;
        this.eventService = eventService;
        this.interactionService = interactionService;
        this.riskToolService = riskToolService;
        this.reportService = reportService;
        this.endingService = endingService;
        this.balanceConfig = balanceConfig;
        this.operatingPressureService = operatingPressureService;
    }

    @Transactional
    public DemoResetResult reset(DemoResetRequest request) {
        String scenario = normalizeStrategy(request == null ? null : request.scenario(), "steady");
        if (!isSupportedStrategy(scenario)) {
            throw new GameException("DEMO_SCENARIO_NOT_SUPPORTED", "开发验证暂时只开放 " + SUPPORTED_STRATEGY_LABEL + " 路线。");
        }
        String runSeed = normalizeRunSeed(request == null ? null : request.runSeed(), defaultRunSeedFor(scenario));

        UserAccount user = ensureDemoUser();
        lastErrors.remove(user.getId());
        Vup active = vupMapper.findActiveByUserId(user.getId());
        if (active != null) {
            vupMapper.updateStatus(active.getId(), VupStatus.ABANDONED.name());
        }

        var vup = vupService.createVupWithRunSeed(
                user.getId(),
                new CreateVupRequest(DEMO_VUP_NAME, demoPersonaFor(scenario), "STANDARD"),
                runSeed
        );
        return new DemoResetResult(user.getId(), user.getUsername(), runSeed, vup, vupService.currentSession(user.getId()));
    }

    public DemoRunScriptResult runScript(Long userId, DemoRunScriptRequest request) {
        boolean stopOnError = request == null || request.stopOnError() == null || request.stopOnError();
        try {
            DemoRunScriptResult result = runScriptInternal(userId, request);
            lastErrors.remove(userId);
            return result;
        } catch (GameException exception) {
            lastErrors.put(userId, new DemoLastErrorDTO(
                    "run-script",
                    exception.code(),
                    exception.getMessage(),
                    stopOnError
            ));
            throw exception;
        }
    }

    private DemoRunScriptResult runScriptInternal(Long userId, DemoRunScriptRequest request) {
        String strategy = normalizeStrategy(request == null ? null : request.strategy(), "steady");
        if (!isSupportedStrategy(strategy)) {
            throw new GameException("DEMO_STRATEGY_NOT_SUPPORTED", "开发验证暂时只开放 " + SUPPORTED_STRATEGY_LABEL + " 路线。");
        }

        int maxDay = balanceConfig.maxDay();
        int targetDay = request == null || request.targetDay() == null ? maxDay : request.targetDay();
        if (targetDay < 1 || targetDay > maxDay) {
            throw new GameException("CONFIG_FIELD_INVALID", "验证目标天数必须在1到" + maxDay + "之间。");
        }

        Vup activeVup = vupService.requireActiveVup(userId);
        String runSeed = normalizeRunSeed(request == null ? null : request.runSeed(), activeVup.getRunSeed());
        if (!runSeed.equals(activeVup.getRunSeed())) {
            throw new GameException("DEMO_RUN_SEED_MISMATCH", "验证脚本的 runSeed 必须和当前轮次一致，请先 reset 指定 seed。");
        }
        String idempotencyPrefix = "demo-" + strategy + "-vup-" + activeVup.getId();
        while (true) {
            var session = vupService.currentSession(userId);
            if (DayPhase.ENDING_READY.name().equals(session.phase()) || session.day() >= targetDay) {
                break;
            }
            advanceOneScriptDay(userId, strategy, session.day(), idempotencyPrefix);
        }

        var session = vupService.currentSession(userId);
        if (session.day() == targetDay && DayPhase.READY.name().equals(session.phase())) {
            completeScriptDay(userId, strategy, session.day(), idempotencyPrefix);
            session = vupService.currentSession(userId);
        }

        Vup vup = vupService.requireActiveVup(userId);
        var reports = reportService.historyReports(vup);
        int reportCount = reports.size();
        List<Long> reportIds = reports.stream()
                .map(report -> report.id())
                .toList();
        int logCount = businessLogMapper.countByVupId(vup.getId());
        String endingType = null;
        String endingReason = null;
        if (DayPhase.ENDING_READY.name().equals(session.phase())) {
            EndingReviewDTO ending = endingService.latestReview(vup);
            endingType = ending.endingType();
            endingReason = ending.endingReason();
        }

        return new DemoRunScriptResult(
                strategy,
                vup.getRunSeed(),
                vup.getId(),
                session.day(),
                session.day(),
                session.phase(),
                reportCount,
                reportIds,
                logCount,
                logCount,
                session.endingReviewId(),
                endingType,
                demoSummary(strategy, session.phase(), endingType),
                lastDaySnapshot(userId),
                endingReason
        );
    }

    public DemoRunScriptResult fastForward(Long userId, DemoFastForwardRequest request) {
        Integer targetDay = balanceConfig.maxDay();
        if (request != null && request.targetDay() != null) {
            targetDay = request.targetDay();
        }
        return runScript(userId, new DemoRunScriptRequest("steady", targetDay, null, true));
    }

    public DemoStatusResult status(Long userId) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new GameException("UNAUTHORIZED", "验证账号不存在，先 reset 一轮。");
        }
        var session = vupService.currentSession(userId);
        Vup vup = vupService.requireActiveVup(userId);
        int reportCount = reportService.historyReports(vup).size();
        int logCount = businessLogMapper.countByVupId(vup.getId());
        String endingType = null;
        String endingReason = null;
        if (DayPhase.ENDING_READY.name().equals(session.phase())) {
            EndingReviewDTO ending = endingService.latestReview(vup);
            endingType = ending.endingType();
            endingReason = ending.endingReason();
        }
        return new DemoStatusResult(
                user.getId(),
                user.getUsername(),
                vup.getRunSeed(),
                session.day(),
                session.day(),
                session.phase(),
                reportCount,
                logCount,
                logCount,
                session.endingReviewId(),
                endingType,
                demoSummary("status", session.phase(), endingType),
                lastDaySnapshot(userId),
                endingReason,
                lastErrors.get(userId)
        );
    }

    private String demoSummary(String strategy, String phase, String endingType) {
        if (DayPhase.ENDING_READY.name().equals(phase) && endingType != null) {
            return demoStrategyLabel(strategy) + "验证已跑完，结局【" + endingTypeLabel(endingType) + "】已生成。";
        }
        return demoStrategyLabel(strategy) + "验证已推进到【" + phaseLabel(phase) + "】。";
    }

    private String demoStrategyLabel(String strategy) {
        return switch (strategy) {
            case "steady" -> "稳健线";
            case "clip" -> "切片线";
            case "social" -> "联动线";
            case "black_red" -> "黑红线";
            case "singing" -> "歌势线";
            case "cyber_girlfriend" -> "赛博女友线";
            case "main_stage_king" -> "主会场之王线";
            case "glorious_graduation" -> "光荣毕业线";
            case "idle" -> "摸鱼线";
            case "defense" -> "米线防守线";
            case "random" -> "随机整活线";
            case "status" -> "验证状态";
            default -> "验证线";
        };
    }

    private String phaseLabel(String phase) {
        if (phase == null) {
            return "等待开播";
        }
        return switch (phase) {
            case "READY" -> "今日行动";
            case "NEED_TITLE" -> "选择直播标题";
            case "NEED_INTERACTION_CHOICE" -> "直播现场选择";
            case "NEED_EVENT_CHOICE" -> "突发事件选择";
            case "REPORT_READY" -> "今日日报";
            case "ENDING_READY" -> "结局复盘";
            default -> "当前流程";
        };
    }

    private String endingTypeLabel(String endingType) {
        if (endingType == null) {
            return "未定结局";
        }
        return switch (endingType) {
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "SINGING_IDOL" -> "歌势偶像";
            case "DANCE_MEME" -> "舞势达人";
            case "SLICE_SAINT" -> "切片圣体";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "DD_BUS_STOP" -> "DD公交站";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "UNKNOWN" -> "查无此V";
            default -> "未归档结局";
        };
    }

    private Map<String, Object> lastDaySnapshot(Long userId) {
        var state = vupService.currentState(userId);
        return Map.of(
                "fans", state.fanStructure().fans(),
                "watchHeat", state.opinion().watchHeat(),
                "reputation", state.opinion().reputation(),
                "memeLevel", state.opinion().memeLevel(),
                "unresolvedDebtCount", state.debts().size()
        );
    }

    private void advanceOneSteadyDay(Long userId, int day, String idempotencyPrefix) {
        advanceOneScriptDay(userId, "steady", day, idempotencyPrefix);
    }

    private void advanceOneScriptDay(Long userId, String strategy, int day, String idempotencyPrefix) {
        completeScriptDay(userId, strategy, day, idempotencyPrefix);
        dayService.nextDay(userId, new NextDayRequest(idempotencyPrefix + "-go-day-" + (day + 1)));
    }

    private void completeScriptDay(Long userId, String strategy, int day, String idempotencyPrefix) {
        if ("main_stage_king".equals(strategy)) {
            completeBlackRedStreamDay(userId, day, idempotencyPrefix);
            return;
        }
        if ("cyber_girlfriend".equals(strategy)) {
            completeFanServiceScThanksDay(userId, day, idempotencyPrefix);
            return;
        }
        if ("steady".equals(strategy) && (day == 1 || day == 4)) {
            completeSteadyTalkStreamDay(userId, day, idempotencyPrefix);
            return;
        }
        if ("black_red".equals(strategy)) {
            if (day <= 14) {
                completeBlackRedStreamDay(userId, day, idempotencyPrefix);
                return;
            }
        }
        if ("defense".equals(strategy)) {
            completeDefenseDay(userId, day, idempotencyPrefix);
            return;
        }
        Vup vup = vupService.requireActiveVup(userId);
        ActionType actionType = actionFor(strategy, day, vup);
        actionType = resolveDemoActionBeforeSubmit(userId, day, actionType);
        actionService.submitAction(userId, new SubmitActionRequest(
                actionType.name(),
                null,
                idempotencyPrefix + "-day-" + day + "-" + actionType.name().toLowerCase()
        ));
        settlePendingFlowAfterAction(userId, day, idempotencyPrefix);
    }

    private void completeBlackRedStreamDay(Long userId, int day, String idempotencyPrefix) {
        actionService.submitAction(userId, new SubmitActionRequest(
                ActionType.STREAM_PLAN.name(),
                "SINGING",
                idempotencyPrefix + "-day-" + day + "-stream-plan"
        ));
        titleService.chooseTitle(userId, new ChooseTitleRequest(23L, idempotencyPrefix + "-day-" + day + "-hard-mouth-title"));
        settlePendingFlowAfterAction(userId, day, idempotencyPrefix);
    }

    private void completeSteadyTalkStreamDay(Long userId, int day, String idempotencyPrefix) {
        actionService.submitAction(userId, new SubmitActionRequest(
                ActionType.STREAM_PLAN.name(),
                "TALK",
                idempotencyPrefix + "-day-" + day + "-stream-plan"
        ));
        titleService.chooseTitle(userId, new ChooseTitleRequest(11L, idempotencyPrefix + "-day-" + day + "-safe-talk-title"));
        settlePendingFlowAfterAction(userId, day, idempotencyPrefix);
    }

    private void completeFanServiceScThanksDay(Long userId, int day, String idempotencyPrefix) {
        actionService.submitAction(userId, new SubmitActionRequest(
                ActionType.STREAM_PLAN.name(),
                "SC_THANKS",
                idempotencyPrefix + "-day-" + day + "-stream-plan"
        ));
        titleService.chooseTitle(userId, new ChooseTitleRequest(123L, idempotencyPrefix + "-day-" + day + "-fan-service-title"));
        settlePendingFlowAfterAction(userId, day, idempotencyPrefix);
    }

    private void completeDefenseDay(Long userId, int day, String idempotencyPrefix) {
        if (day == 1) {
            completeBlackRedStreamDay(userId, day, idempotencyPrefix);
            return;
        }
        if (day == 2) {
            riskToolService.useTool(userId, new UseRiskToolRequest(
                    "FULL_CLIP_CONTEXT",
                    null,
                    idempotencyPrefix + "-day-" + day + "-risk-tool-full-clip"
            ));
        }
        ActionType actionType = defenseActionFor(day);
        actionService.submitAction(userId, new SubmitActionRequest(
                actionType.name(),
                null,
                idempotencyPrefix + "-day-" + day + "-" + actionType.name().toLowerCase()
        ));
        settlePendingFlowAfterAction(userId, day, idempotencyPrefix);
    }

    private void settlePendingFlowAfterAction(Long userId, int day, String idempotencyPrefix) {
        for (int step = 0; step < 4; step++) {
            var session = vupService.currentSession(userId);
            if (DayPhase.NEED_EVENT_CHOICE.name().equals(session.phase())) {
                String choiceType = eventChoiceForStrategy(idempotencyPrefix, day);
                eventService.choose(userId, new ChooseEventRequest(choiceType, idempotencyPrefix + "-day-" + day + "-event-" + choiceType + "-" + step));
                continue;
            }
            if (DayPhase.NEED_INTERACTION_CHOICE.name().equals(session.phase())) {
                interactionService.choose(userId, new ChooseInteractionRequest("safe", idempotencyPrefix + "-day-" + day + "-interaction-safe-" + step));
                continue;
            }
            if (DayPhase.OFF_STREAM_READY.name().equals(session.phase())) {
                actionService.skipOffStream(userId);
                continue;
            }
            return;
        }
    }

    private void settlePendingEventIfNeeded(Long userId, int day, String idempotencyPrefix) {
        var session = vupService.currentSession(userId);
        if (!DayPhase.NEED_EVENT_CHOICE.name().equals(session.phase())) {
            return;
        }
        String choiceType = eventChoiceForStrategy(idempotencyPrefix, day);
        eventService.choose(userId, new ChooseEventRequest(choiceType, idempotencyPrefix + "-day-" + day + "-event-" + choiceType));
    }

    private void settlePendingInteractionIfNeeded(Long userId, int day, String idempotencyPrefix) {
        var session = vupService.currentSession(userId);
        if (!DayPhase.NEED_INTERACTION_CHOICE.name().equals(session.phase())) {
            return;
        }
        interactionService.choose(userId, new ChooseInteractionRequest("safe", idempotencyPrefix + "-day-" + day + "-interaction-safe"));
    }

    private ActionType actionFor(String strategy, int day, Vup vup) {
        if ("clip".equals(strategy)) {
            if (day % 3 == 1) {
                return firstAvailableDemoAction(vup, day,
                        ActionType.PUBLISH_VIDEO,
                        ActionType.PUBLISH_CLIP,
                        ActionType.TRAIN_DANCE,
                        ActionType.TRAIN_TALK,
                        ActionType.REST);
            }
            return firstAvailableDemoAction(vup, day,
                    ActionType.PUBLISH_CLIP,
                    ActionType.PUBLISH_VIDEO,
                    ActionType.TRAIN_DANCE,
                    ActionType.TRAIN_TALK,
                    ActionType.REST);
        }
        if ("social".equals(strategy)) {
            return firstAvailableDemoAction(vup, day,
                    ActionType.NPC_INTERACT,
                    ActionType.REST,
                    ActionType.TRAIN_TALK,
                    ActionType.FAN_GROUP_MAINTAIN);
        }
        if ("singing".equals(strategy)) {
            return ActionType.TRAIN_SONG;
        }
        if ("glorious_graduation".equals(strategy)) {
            return gloriousGraduationActionFor(vup, day);
        }
        if ("black_red".equals(strategy)) {
            return day <= 14 ? ActionType.STREAM_PLAN : ActionType.REST;
        }
        if ("defense".equals(strategy)) {
            return defenseActionFor(day);
        }
        if ("random".equals(strategy)) {
            return randomActionFor(day, vup);
        }
        if ("idle".equals(strategy)) {
            return ActionType.REST;
        }
        if ("steady".equals(strategy) && day < balanceConfig.maxDay() && day % 3 == 0) {
            return ActionType.FAN_GROUP_MAINTAIN;
        }
        return ActionType.TRAIN_TALK;
    }

    private String eventChoiceForStrategy(String idempotencyPrefix, int day) {
        if (idempotencyPrefix.contains("black_red")) {
            return day == 24 ? "traffic" : "safe";
        }
        if (idempotencyPrefix.contains("main_stage_king")) {
            return "meme";
        }
        if (idempotencyPrefix.contains("clip") || idempotencyPrefix.contains("random")) {
            return "meme";
        }
        return "safe";
    }

    private ActionType randomActionFor(int day, Vup vup) {
        if (day == 1 || day == 8 || day == 15 || day == 22 || day == 29) {
            if (canUseDemoAction(ActionType.PUBLISH_VIDEO, vup, day)) {
                return ActionType.PUBLISH_VIDEO;
            }
        }
        ActionType[] cycle = day >= balanceConfig.maxDay() ? RANDOM_FINAL_ACTION_CYCLE : RANDOM_ACTION_CYCLE;
        int offset = Math.floorMod(stableSeedHash(vup.getRunSeed()), cycle.length);
        int start = Math.floorMod(day - 1 + offset, cycle.length);
        for (int index = 0; index < cycle.length; index++) {
            ActionType candidate = cycle[Math.floorMod(start + index, cycle.length)];
            if (canUseDemoAction(candidate, vup, day)) {
                return candidate;
            }
        }
        return ActionType.REST;
    }

    private ActionType firstAvailableDemoAction(Vup vup, int day, ActionType... candidates) {
        for (ActionType candidate : candidates) {
            if (canUseDemoAction(candidate, vup, day)) {
                return candidate;
            }
        }
        return ActionType.REST;
    }

    private ActionType resolveDemoActionBeforeSubmit(Long userId, int day, ActionType selected) {
        Vup fresh = vupService.requireActiveVup(userId);
        if (canUseDemoAction(selected, fresh, day)) {
            return selected;
        }
        OperatingPressureService.PressureReplacement replacement =
                operatingPressureService.replacementForAction(fresh, day, selected);
        if (replacement.available()) {
            ActionType replacementType = ActionType.valueOf(replacement.actionType());
            if (canUseDemoAction(replacementType, fresh, day)) {
                return replacementType;
            }
        }
        return firstAvailableDemoAction(fresh, day,
                ActionType.TRAIN_TALK,
                ActionType.REST,
                ActionType.FAN_GROUP_MAINTAIN,
                ActionType.TRAIN_DANCE,
                ActionType.TRAIN_SONG);
    }

    private ActionType gloriousGraduationActionFor(Vup vup, int day) {
        if (day >= balanceConfig.maxDay()) {
            return firstAvailableDemoAction(vup, day,
                    ActionType.FAN_GROUP_MAINTAIN,
                    ActionType.REST,
                    ActionType.TRAIN_TALK);
        }
        if (day % 5 == 0) {
            return firstAvailableDemoAction(vup, day,
                    ActionType.REST,
                    ActionType.TRAIN_TALK);
        }
        return firstAvailableDemoAction(vup, day,
                ActionType.TRAIN_TALK,
                ActionType.REST);
    }

    private ActionType defenseActionFor(int day) {
        if (day == balanceConfig.maxDay() || day % 3 == 0) {
            return ActionType.FAN_GROUP_MAINTAIN;
        }
        if (day % 3 == 1) {
            return ActionType.REST;
        }
        return ActionType.TRAIN_TALK;
    }

    private boolean isSupportedStrategy(String value) {
        return SUPPORTED_STRATEGIES.contains(value);
    }

    private boolean canUseDemoAction(ActionType actionType, Vup vup, int day) {
        if (operatingPressureService.isLocked(vup, day, actionType)) {
            return false;
        }
        return isActionAvailableForDemo(actionType, vup);
    }

    private boolean isActionAvailableForDemo(ActionType actionType, Vup vup) {
        int staminaCost = switch (actionType) {
            case PUBLISH_VIDEO -> 4;
            case STREAM_PLAN -> 3;
            case PUBLISH_CLIP -> 1;
            case REST -> 0;
            default -> 2;
        };
        if (vup.getStamina() < staminaCost) {
            return false;
        }
        if (actionType == ActionType.PUBLISH_VIDEO && vup.getInspiration() < 1) {
            return false;
        }
        return actionType != ActionType.PUBLISH_CLIP || businessLogMapper.countMaterialStockByVupId(vup.getId()) >= 1;
    }

    private int stableSeedHash(String seed) {
        int hash = 0;
        for (int index = 0; index < seed.length(); index++) {
            hash = 31 * hash + seed.charAt(index);
        }
        return hash;
    }

    private String normalizeStrategy(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }

    private String normalizeRunSeed(String raw, String fallback) {
        String seed = raw == null || raw.isBlank() ? fallback : raw.trim();
        if (seed.length() > RUN_SEED_MAX_LENGTH) {
            throw new GameException("CONFIG_FIELD_INVALID", "runSeed 长度不能超过64个字符。");
        }
        return seed;
    }

    private String defaultRunSeedFor(String scenario) {
        if ("random".equals(scenario)) {
            return "demo-random-007";
        }
        return "demo-" + scenario + "-001";
    }

    private String demoPersonaFor(String scenario) {
        return switch (scenario) {
            case "clip" -> "切片圣体路线试玩新人";
            case "social" -> "DD公交站路线试玩新人";
            case "black_red" -> "黑红主会场路线试玩新人";
            case "singing" -> "歌势偶像路线试玩新人";
            case "cyber_girlfriend" -> "赛博女友路线试玩新人";
            case "main_stage_king" -> "主会场之王路线试玩新人";
            case "glorious_graduation" -> "光荣毕业路线试玩新人";
            case "idle" -> "查无此V路线试玩新人";
            case "defense" -> "防守降温路线试玩新人";
            case "random" -> "随机整活路线试玩新人";
            default -> "稳健电子榨菜路线试玩新人";
        };
    }

    private UserAccount ensureDemoUser() {
        UserAccount existing = userAccountMapper.findByUsername(DEMO_USERNAME);
        if (existing != null) {
            if (!DEMO_NICKNAME.equals(existing.getNickname())) {
                userAccountMapper.updateNickname(existing.getId(), DEMO_NICKNAME);
                existing.setNickname(DEMO_NICKNAME);
            }
            return existing;
        }

        UserAccount user = new UserAccount();
        user.setUsername(DEMO_USERNAME);
        user.setPasswordHash(passwordHasher.hash(DEMO_USERNAME, DEMO_PASSWORD));
        user.setNickname(DEMO_NICKNAME);
        user.setCoin(0);
        user.setRestartCount(0);
        userAccountMapper.insert(user);
        return user;
    }
}
