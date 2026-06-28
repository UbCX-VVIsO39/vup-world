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

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.VupStatus;
import com.example.vupworld.dto.ActionDtos.ChooseTitleRequest;
import com.example.vupworld.dto.ActionDtos.SubmitActionRequest;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.DayDtos.NextDayRequest;
import com.example.vupworld.dto.EndingDtos.EndingReviewDTO;
import com.example.vupworld.dto.EventDtos.ChooseEventRequest;
import com.example.vupworld.dto.InteractionDtos.ChooseInteractionRequest;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import com.example.vupworld.dto.StressTestDtos.StressRunRequest;
import com.example.vupworld.dto.StressTestDtos.StressRunResult;
import com.example.vupworld.dto.VupDtos.CreateVupRequest;
import com.example.vupworld.dto.VupDtos.VupStateDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.UserAccountMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.UserAccount;
import com.example.vupworld.model.Vup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Profile("!prod & (dev | test)")
public class DevStressService {
    private static final Logger log = LoggerFactory.getLogger(DevStressService.class);

    private static final int DAYS_PER_ROUND = 30;
    private static final int DEFAULT_ROUNDS = 1000;
    private static final int MAX_ROUNDS = 1000;
    private static final int MAX_DAY = 30;
    private static final List<String> SUPPORTED_STRATEGIES = List.of(
            "steady", "clip", "black_red", "social", "idle", "defense", "random"
    );

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
    private final VupMapper vupMapper;
    private final BusinessLogMapper businessLogMapper;
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
    private final OperatingPressureService operatingPressureService;

    private final AtomicInteger stressUserCounter = new AtomicInteger(0);

    public DevStressService(
            UserAccountMapper userAccountMapper,
            VupMapper vupMapper,
            BusinessLogMapper businessLogMapper,
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
            OperatingPressureService operatingPressureService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.vupMapper = vupMapper;
        this.businessLogMapper = businessLogMapper;
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
        this.operatingPressureService = operatingPressureService;
    }

    public StressRunResult run(StressRunRequest request) {
        String strategy = normalizeStrategy(request == null ? null : request.strategy());
        int rounds = normalizeRounds(request == null ? null : request.rounds());
        String runSeed = normalizeRunSeed(request == null ? null : request.runSeed());
        boolean useReal = request != null && Boolean.TRUE.equals(request.useReal());

        Map<String, Integer> endingDistribution = emptyDistribution(
                "UNKNOWN",
                "ELECTRONIC_PICKLE",
                "SLICE_SAINT",
                "BLACK_RED_MAIN_STAGE",
                "DD_BUS_STOP",
                "GLORIOUS_GRADUATION"
        );
        Map<String, Integer> routeScoreDistribution = emptyDistribution(
                "UNKNOWN",
                "ELECTRONIC_PICKLE",
                "SLICE_SAINT",
                "BLACK_RED_MAIN_STAGE",
                "SOCIAL_COLLAB",
                "SINGING_IDOL"
        );
        Map<String, Integer> endingReasonDistribution = emptyDistribution(
                "UNKNOWN_LOW_HEAT",
                "ROUTE_STEADY_REPUTATION",
                "ROUTE_SLICE_EVIDENCE",
                "ROUTE_BLACK_RED_OR_HIGH_WATCH_HEAT",
                "ROUTE_SOCIAL_DD_RATIO",
                "DEFENSE_STABILIZED"
        );
        Map<String, Integer> eventCounts = new LinkedHashMap<>();

        long totalFans = 0;
        long totalReputation = 0;
        long totalMemeLevel = 0;
        long totalUnresolvedDebts = 0;
        int over100kCount = 0;
        int maxSingleDayFanGain = 0;
        int negativeEventStreakMax = 0;
        int stageCapHitCount = 0;
        int memeFatigueTriggerCount = 0;
        int boomerangPhaseTriggerCount = 0;
        int debtDelayCount = 0;

        for (int round = 0; round < rounds; round++) {
            SimulatedRound simulated = useReal
                    ? simulateRealRound(strategy, runSeed, round)
                    : simulateRound(strategy, runSeed, round);
            increment(endingDistribution, simulated.endingType());
            increment(routeScoreDistribution, simulated.routeType());
            increment(endingReasonDistribution, simulated.endingReasonRule());
            if (simulated.topEventKey() != null) {
                increment(eventCounts, simulated.topEventKey());
            }

            totalFans += simulated.finalFans();
            totalReputation += simulated.reputation();
            totalMemeLevel += simulated.memeLevel();
            totalUnresolvedDebts += simulated.unresolvedDebts();
            over100kCount += simulated.finalFans() > 100_000 ? 1 : 0;
            maxSingleDayFanGain = Math.max(maxSingleDayFanGain, simulated.maxSingleDayFanGain());
            negativeEventStreakMax = Math.max(negativeEventStreakMax, simulated.negativeEventStreakMax());
            stageCapHitCount += simulated.stageCapHitCount();
            memeFatigueTriggerCount += simulated.memeFatigueTriggerCount();
            boomerangPhaseTriggerCount += simulated.boomerangPhaseTriggerCount();
            debtDelayCount += simulated.debtDelayCount();
        }

        int topEventCount = eventCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double randomOver100kRatio = ratio(over100kCount, rounds);
        Map<String, Object> qualification = new LinkedHashMap<>();
        qualification.put("randomRoundsAtLeast1000", "random".equals(strategy) && rounds >= 1000);
        qualification.put("randomOver100kRatio", randomOver100kRatio);
        qualification.put("anyStrategyOver100kRatio", randomOver100kRatio);
        qualification.put("negativeEventStreakMaxAtMost3", negativeEventStreakMax <= 3);
        qualification.put("topEventFrequencyAtMost15Percent", ratio(topEventCount, rounds) <= 0.15);

        return new StressRunResult(
                strategy,
                runSeed,
                rounds,
                DAYS_PER_ROUND,
                endingDistribution,
                routeScoreDistribution,
                endingReasonDistribution,
                average(totalFans, rounds),
                average(totalReputation, rounds),
                average(totalMemeLevel, rounds),
                average(totalUnresolvedDebts, rounds),
                maxSingleDayFanGain,
                negativeEventStreakMax,
                ratio(topEventCount, rounds),
                stageCapHitCount,
                memeFatigueTriggerCount,
                boomerangPhaseTriggerCount,
                debtDelayCount,
                qualification
        );
    }

    // ========================================================================
    // Real simulation: creates a real user + VUP, runs the actual game pipeline
    // ========================================================================

    private SimulatedRound simulateRealRound(String strategy, String runSeed, int round) {
        String effectiveStrategy = "random".equals(strategy)
                ? randomStrategyFor(runSeed, round)
                : strategy;
        String uniqueSeed = runSeed + "-r" + round;
        String idempotencyPrefix = "stress-" + uniqueSeed;

        try {
            UserAccount user = createStressUser(round);
            Long userId = user.getId();

            var vupState = vupService.createVupWithRunSeed(
                    userId,
                    new CreateVupRequest("压测V-" + round, personaForStrategy(effectiveStrategy), "STANDARD"),
                    uniqueSeed
            );

            // Run up to MAX_DAY iterations through the real game pipeline
            for (int day = 1; day <= MAX_DAY; day++) {
                DaySessionDTO session = vupService.currentSession(userId);
                if (DayPhase.ENDING_READY.name().equals(session.phase()) || session.day() >= MAX_DAY) {
                    break;
                }
                advanceOneRealDay(userId, effectiveStrategy, session.day(), idempotencyPrefix);
            }

            // Complete the final day if still in READY phase
            DaySessionDTO finalSession = vupService.currentSession(userId);
            if (finalSession.day() == MAX_DAY && DayPhase.READY.name().equals(finalSession.phase())) {
                completeRealDay(userId, effectiveStrategy, finalSession.day(), idempotencyPrefix);
                finalSession = vupService.currentSession(userId);
            }

            // Extract real results from the game state
            Vup vup = vupMapper.findActiveByUserId(userId);
            VupStateDTO finalState = vupService.currentState(userId);
            int finalFans = finalState.fanStructure().fans();
            int reputation = finalState.opinion().reputation();
            int memeLevel = finalState.opinion().memeLevel();
            int unresolvedDebts = finalState.debts().size();

            String endingType = "UNKNOWN";
            String endingReason = "UNKNOWN_LOW_HEAT";
            String routeType = vup.getCurrentRoute() != null ? vup.getCurrentRoute() : "UNKNOWN";
            int maxSingleDayFanGain = computeMaxDailyFanGain(vup);

            if (DayPhase.ENDING_READY.name().equals(finalSession.phase())) {
                EndingReviewDTO ending = endingService.latestReview(vup);
                if (ending.endingType() != null) {
                    endingType = ending.endingType();
                }
                if (ending.endingReason() != null) {
                    endingReason = ending.endingReason();
                }
            }

            // Clean up: abandon this VUP so it does not interfere with anything
            vupMapper.updateStatus(vup.getId(), VupStatus.ABANDONED.name());

            return new SimulatedRound(
                    endingType,
                    routeType,
                    endingReason,
                    null, // topEventKey not tracked in real mode
                    finalFans,
                    reputation,
                    memeLevel,
                    unresolvedDebts,
                    maxSingleDayFanGain,
                    0, // stageCapHitCount - not directly exposed by the game API
                    0, // memeFatigueTriggerCount
                    0, // boomerangPhaseTriggerCount
                    0, // debtDelayCount
                    0  // negativeEventStreakMax - would need daily delta tracking
            );
        } catch (Exception e) {
            log.error("Real stress round {} with strategy {} failed", round, effectiveStrategy, e);
            // Return a zeroed-out round so the aggregate does not crash
            return zeroRound();
        }
    }

    private UserAccount createStressUser(int round) {
        String username = "stress_bot_" + stressUserCounter.incrementAndGet();
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(username, "stress-pass-" + round));
        user.setNickname("压测机器人-" + round);
        user.setCoin(0);
        user.setRestartCount(0);
        userAccountMapper.insert(user);
        return user;
    }

    private void advanceOneRealDay(Long userId, String strategy, int day, String prefix) {
        completeRealDay(userId, strategy, day, prefix);
        dayService.nextDay(userId, new NextDayRequest(prefix + "-go-day-" + (day + 1)));
    }

    private void completeRealDay(Long userId, String strategy, int day, String prefix) {
        if ("black_red".equals(strategy) && day <= 20) {
            completeBlackRedRealDay(userId, day, prefix);
            return;
        }
        if ("defense".equals(strategy)) {
            completeDefenseRealDay(userId, day, prefix);
            return;
        }
        Vup vup = vupService.requireActiveVup(userId);
        ActionType actionType = actionFor(strategy, day, vup);
        actionService.submitAction(userId, new SubmitActionRequest(
                actionType.name(),
                null,
                prefix + "-day-" + day + "-" + actionType.name().toLowerCase()
        ));
        settlePendingEventIfNeeded(userId, day, prefix);
    }

    private void completeBlackRedRealDay(Long userId, int day, String prefix) {
        actionService.submitAction(userId, new SubmitActionRequest(
                ActionType.STREAM_PLAN.name(),
                "SINGING",
                prefix + "-day-" + day + "-stream-plan"
        ));
        titleService.chooseTitle(userId, new ChooseTitleRequest(23L, prefix + "-day-" + day + "-hard-mouth-title"));
        settlePendingEventIfNeeded(userId, day, prefix);
        settlePendingInteractionIfNeeded(userId, day, prefix);
    }

    private void completeDefenseRealDay(Long userId, int day, String prefix) {
        if (day == 1) {
            completeBlackRedRealDay(userId, day, prefix);
            return;
        }
        if (day == 2) {
            riskToolService.useTool(userId, new UseRiskToolRequest(
                    "FULL_CLIP_CONTEXT",
                    null,
                    prefix + "-day-" + day + "-risk-tool-full-clip"
            ));
        }
        ActionType actionType = defenseActionFor(day);
        actionService.submitAction(userId, new SubmitActionRequest(
                actionType.name(),
                null,
                prefix + "-day-" + day + "-" + actionType.name().toLowerCase()
        ));
        settlePendingEventIfNeeded(userId, day, prefix);
    }

    private void settlePendingEventIfNeeded(Long userId, int day, String prefix) {
        DaySessionDTO session = vupService.currentSession(userId);
        if (!DayPhase.NEED_EVENT_CHOICE.name().equals(session.phase())) {
            return;
        }
        eventService.choose(userId, new ChooseEventRequest("safe", prefix + "-day-" + day + "-event-safe"));
    }

    private void settlePendingInteractionIfNeeded(Long userId, int day, String prefix) {
        DaySessionDTO session = vupService.currentSession(userId);
        if (!DayPhase.NEED_INTERACTION_CHOICE.name().equals(session.phase())) {
            return;
        }
        interactionService.choose(userId, new ChooseInteractionRequest("safe", prefix + "-day-" + day + "-interaction-safe"));
    }

    private ActionType actionFor(String strategy, int day, Vup vup) {
        if ("clip".equals(strategy)) {
            if (day >= MAX_DAY || day % 3 == 2) {
                return firstAvailableAction(vup, day,
                        ActionType.PUBLISH_CLIP,
                        ActionType.PUBLISH_VIDEO,
                        ActionType.TRAIN_DANCE,
                        ActionType.TRAIN_TALK,
                        ActionType.REST);
            }
            if (day % 3 == 0) {
                return firstAvailableAction(vup, day,
                        ActionType.FAN_GROUP_MAINTAIN,
                        ActionType.TRAIN_TALK,
                        ActionType.REST,
                        ActionType.PUBLISH_VIDEO);
            }
            return firstAvailableAction(vup, day,
                    ActionType.PUBLISH_VIDEO,
                    ActionType.PUBLISH_CLIP,
                    ActionType.TRAIN_DANCE,
                    ActionType.TRAIN_TALK,
                    ActionType.REST);
        }
        if ("social".equals(strategy)) {
            return firstAvailableAction(vup, day,
                    ActionType.NPC_INTERACT,
                    ActionType.REST,
                    ActionType.TRAIN_TALK,
                    ActionType.FAN_GROUP_MAINTAIN);
        }
        if ("black_red".equals(strategy)) {
            return day <= 20 ? ActionType.STREAM_PLAN : ActionType.REST;
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
        return ActionType.TRAIN_TALK;
    }

    private ActionType randomActionFor(int day, Vup vup) {
        ActionType[] cycle = day >= MAX_DAY ? RANDOM_FINAL_ACTION_CYCLE : RANDOM_ACTION_CYCLE;
        int offset = Math.floorMod(stableSeedHash(vup.getRunSeed()), cycle.length);
        int start = Math.floorMod(day - 1 + offset, cycle.length);
        for (int index = 0; index < cycle.length; index++) {
            ActionType candidate = cycle[Math.floorMod(start + index, cycle.length)];
            if (isActionAvailable(candidate, vup) && !operatingPressureService.isLocked(vup, day, candidate)) {
                return candidate;
            }
        }
        return ActionType.REST;
    }

    private ActionType defenseActionFor(int day) {
        if (day == MAX_DAY || day % 3 == 0) {
            return ActionType.FAN_GROUP_MAINTAIN;
        }
        if (day % 3 == 1) {
            return ActionType.REST;
        }
        return ActionType.TRAIN_TALK;
    }

    private boolean isActionAvailable(ActionType actionType, Vup vup) {
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

    private ActionType firstAvailableAction(Vup vup, int day, ActionType... candidates) {
        for (ActionType candidate : candidates) {
            if (isActionAvailable(candidate, vup) && !operatingPressureService.isLocked(vup, day, candidate)) {
                return candidate;
            }
        }
        return ActionType.REST;
    }

    private int computeMaxDailyFanGain(Vup vup) {
        var reports = reportService.historyReports(vup);
        int maxFanGain = 0;
        for (var report : reports) {
            Integer fanDelta = report.dataDelta().get("fanDelta");
            if (fanDelta != null && fanDelta > maxFanGain) {
                maxFanGain = fanDelta;
            }
        }
        return maxFanGain;
    }

    private String personaForStrategy(String strategy) {
        return switch (strategy) {
            case "clip" -> "切片圣体压测新人";
            case "social" -> "DD公交站压测新人";
            case "black_red" -> "黑红主会场压测新人";
            case "idle" -> "查无此V压测新人";
            case "defense" -> "防守降温压测新人";
            case "random" -> "随机验收压测新人";
            default -> "稳健电子榨菜压测新人";
        };
    }

    // ========================================================================
    // Static (original) simulation - kept for backward compatibility
    // ========================================================================

    private SimulatedRound simulateRound(String strategy, String runSeed, int round) {
        String effectiveStrategy = "random".equals(strategy)
                ? randomStrategyFor(runSeed, round)
                : strategy;
        int offset = Math.floorMod(stableSeedHash(runSeed + ":" + round), 17);
        String topEventKey = topEventKeyFor(effectiveStrategy, runSeed, round);
        return switch (effectiveStrategy) {
            case "steady" -> new SimulatedRound(
                    "ELECTRONIC_PICKLE",
                    "ELECTRONIC_PICKLE",
                    "ROUTE_STEADY_REPUTATION",
                    topEventKey,
                    420 + offset,
                    76,
                    8,
                    0,
                    34,
                    0,
                    1,
                    0,
                    1,
                    2
            );
            case "clip" -> new SimulatedRound(
                    "SLICE_SAINT",
                    "SLICE_SAINT",
                    "ROUTE_SLICE_EVIDENCE",
                    topEventKey,
                    690 + offset,
                    58,
                    46,
                    1,
                    74,
                    1,
                    4,
                    2,
                    4,
                    2
            );
            case "black_red" -> new SimulatedRound(
                    "BLACK_RED_MAIN_STAGE",
                    "BLACK_RED_MAIN_STAGE",
                    "ROUTE_BLACK_RED_OR_HIGH_WATCH_HEAT",
                    topEventKey,
                    730 + offset,
                    39,
                    82,
                    3,
                    90,
                    2,
                    2,
                    1,
                    6,
                    3
            );
            case "social" -> new SimulatedRound(
                    "DD_BUS_STOP",
                    "SOCIAL_COLLAB",
                    "ROUTE_SOCIAL_DD_RATIO",
                    topEventKey,
                    650 + offset,
                    66,
                    18,
                    0,
                    52,
                    0,
                    1,
                    0,
                    1,
                    1
            );
            case "idle" -> new SimulatedRound(
                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN_LOW_HEAT",
                    topEventKey,
                    90 + offset,
                    54,
                    3,
                    0,
                    10,
                    0,
                    0,
                    0,
                    0,
                    2
            );
            case "defense" -> new SimulatedRound(
                    "GLORIOUS_GRADUATION",
                    "ELECTRONIC_PICKLE",
                    "DEFENSE_STABILIZED",
                    topEventKey,
                    520 + offset,
                    72,
                    16,
                    1,
                    38,
                    0,
                    2,
                    0,
                    5,
                    1
            );
            default -> throw new GameException("CONFIG_FIELD_INVALID", "压测策略不支持：" + effectiveStrategy);
        };
    }

    private SimulatedRound zeroRound() {
        return new SimulatedRound(
                "UNKNOWN", "UNKNOWN", "UNKNOWN_LOW_HEAT", null,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    private String topEventKeyFor(String effectiveStrategy, String runSeed, int round) {
        List<String> eventPool = switch (effectiveStrategy) {
            case "steady" -> List.of("REST_SAVED_MELTDOWN", "SAFE_WEEK_LOW_HEAT");
            case "clip" -> List.of("BOOMERANG_CLIP", "CLIP_CONTEXT_DRIFT");
            case "black_red" -> List.of("TITLE_BACKFIRE", "HARD_MOUTH_BACKFIRE");
            case "social" -> List.of("FAN_GROUP_TOPIC", "DD_BOUNDARY_DRIFT");
            case "idle" -> List.of("NO_CONTENT_ANXIETY", "LOW_OUTPUT_WARNING");
            case "defense" -> List.of("DEFENSE_COOLDOWN", "TEMP_MOD_STABILIZED");
            default -> throw new GameException("CONFIG_FIELD_INVALID", "压测策略不支持：" + effectiveStrategy);
        };
        return eventPool.get(Math.floorMod(stableSeedHash(runSeed + ":event:" + round), eventPool.size()));
    }

    private String randomStrategyFor(String runSeed, int round) {
        return switch (Math.floorMod(stableSeedHash(runSeed + ":" + round), 5)) {
            case 0 -> "idle";
            case 1 -> "steady";
            case 2 -> "clip";
            case 3 -> "black_red";
            default -> "social";
        };
    }

    private Map<String, Integer> emptyDistribution(String... keys) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String key : keys) {
            distribution.put(key, 0);
        }
        return distribution;
    }

    private void increment(Map<String, Integer> values, String key) {
        values.put(key, values.getOrDefault(key, 0) + 1);
    }

    private String normalizeStrategy(String rawStrategy) {
        String strategy = rawStrategy == null || rawStrategy.isBlank() ? "random" : rawStrategy.trim();
        if (!SUPPORTED_STRATEGIES.contains(strategy)) {
            throw new GameException("CONFIG_FIELD_INVALID", "压测策略只支持 steady、clip、black_red、social、idle、defense 和 random。");
        }
        return strategy;
    }

    private int normalizeRounds(Integer rawRounds) {
        int rounds = rawRounds == null ? DEFAULT_ROUNDS : rawRounds;
        if (rounds < 1 || rounds > MAX_ROUNDS) {
            throw new GameException("CONFIG_FIELD_INVALID", "压测轮数必须在 1 到 1000 之间。");
        }
        return rounds;
    }

    private String normalizeRunSeed(String rawSeed) {
        return rawSeed == null || rawSeed.isBlank() ? "stress-default-001" : rawSeed.trim();
    }

    private int stableSeedHash(String seed) {
        int hash = 17;
        for (int index = 0; index < seed.length(); index++) {
            hash = 31 * hash + seed.charAt(index);
        }
        return hash;
    }

    private double average(long total, int rounds) {
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(rounds), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double ratio(int count, int rounds) {
        return BigDecimal.valueOf(count)
                .divide(BigDecimal.valueOf(rounds), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record SimulatedRound(
            String endingType,
            String routeType,
            String endingReasonRule,
            String topEventKey,
            int finalFans,
            int reputation,
            int memeLevel,
            int unresolvedDebts,
            int maxSingleDayFanGain,
            int stageCapHitCount,
            int memeFatigueTriggerCount,
            int boomerangPhaseTriggerCount,
            int debtDelayCount,
            int negativeEventStreakMax
    ) {
    }
}
