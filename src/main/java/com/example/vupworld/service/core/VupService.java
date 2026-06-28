package com.example.vupworld.service.core;

import com.example.vupworld.service.ending.EndingAtlasService;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.domain.VupStatus;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.VupDtos.CreateVupRequest;
import com.example.vupworld.dto.VupDtos.TutorialHintDTO;
import com.example.vupworld.dto.VupDtos.VupStateDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.EndingReviewMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.EndingReview;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VupService {
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final VupStateMapper vupStateMapper;
    private final JsonService jsonService;
    private final EndingReviewMapper endingReviewMapper;
    private final EndingAtlasService endingAtlasService;
    private final BalanceConfig balanceConfig;

    public VupService(
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            VupStateMapper vupStateMapper,
            JsonService jsonService,
            EndingReviewMapper endingReviewMapper,
            EndingAtlasService endingAtlasService,
            BalanceConfig balanceConfig
    ) {
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.vupStateMapper = vupStateMapper;
        this.jsonService = jsonService;
        this.endingReviewMapper = endingReviewMapper;
        this.endingAtlasService = endingAtlasService;
        this.balanceConfig = balanceConfig;
    }

    @Transactional
    public VupStateDTO createVup(Long userId, CreateVupRequest request) {
        return createVupInternal(userId, request, null);
    }

    @Transactional
    public VupStateDTO createVup(Long userId, CreateVupRequest request, int slotNumber) {
        return createVupInternal(userId, request, null, slotNumber);
    }

    @Transactional
    public VupStateDTO createOrResumeVup(Long userId, CreateVupRequest request) {
        Vup active = vupMapper.findActiveByUserId(userId);
        if (active != null) {
            DaySession session = daySessionMapper.findByVupIdAndDay(active.getId(), active.getDayCount());
            if (session == null) {
                throw new GameException("SYSTEM_ERROR", "当前天数状态丢了，主播还在，排班表没了。");
            }
            return vupStateMapper.toStateDto(active, session, consumeTutorialHints(active, session));
        }
        return createVupInternal(userId, request, null);
    }

    @Transactional
    public VupStateDTO createVupWithRunSeed(Long userId, CreateVupRequest request, String runSeed) {
        return createVupInternal(userId, request, requireText(runSeed, "runSeed不能为空"));
    }

    private VupStateDTO createVupInternal(Long userId, CreateVupRequest request, String runSeedOverride) {
        return createVupInternal(userId, request, runSeedOverride, 1);
    }

    private VupStateDTO createVupInternal(Long userId, CreateVupRequest request, String runSeedOverride, int slotNumber) {
        if (vupMapper.findActiveByUserId(userId) != null) {
            throw new GameException("ACTIVE_VUP_EXISTS", "已经有一个活跃VUP了，别让两个皮套抢同一个排班表。");
        }

        Vup vup = defaultVup(userId, request);
        vup.setSlotNumber(slotNumber);
        if (runSeedOverride != null) {
            vup.setRunSeed(runSeedOverride);
        }
        vupMapper.insert(vup);
        DaySession session = dayOneSession(vup);
        daySessionMapper.insert(session);
        return vupStateMapper.toStateDto(vup, session);
    }

    @Transactional
    public VupStateDTO currentState(Long userId) {
        Vup vup = requireActiveVup(userId);
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (session == null) {
            throw new GameException("SYSTEM_ERROR", "当前天数状态丢了，主播还在，排班表没了。");
        }
        return vupStateMapper.toStateDto(vup, session, consumeTutorialHints(vup, session));
    }

    public DaySessionDTO currentSession(Long userId) {
        Vup vup = requireActiveVup(userId);
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (session == null) {
            throw new GameException("SYSTEM_ERROR", "当前天数状态丢了，主播还在，排班表没了。");
        }
        return vupStateMapper.toDaySessionDto(session);
    }

    public Vup createRestartedRun(
            Vup previous,
            Long previousEndingId,
            String fanBiasType,
            String routeBiasType,
            String restartTargetType
    ) {
        Vup vup = defaultVup(previous.getUserId(), new CreateVupRequest(previous.getName(), previous.getPersona(), previous.getDifficulty()));
        vup.setSlotNumber(Math.max(1, previous.getSlotNumber()));
        vup.setPreviousEndingId(previousEndingId);
        applyRestartBias(vup, fanBiasType);
        applyRestartRouteBias(vup, routeBiasType, restartTargetType, previousEndingId);
        vupMapper.insert(vup);
        daySessionMapper.insert(dayOneSession(vup));
        return vup;
    }

    public Vup requireActiveVup(Long userId) {
        Vup vup = vupMapper.findActiveByUserId(userId);
        if (vup == null) {
            throw new GameException("VUP_NOT_FOUND", "还没有当前VUP，请先创建。");
        }
        return vup;
    }

    private Vup defaultVup(Long userId, CreateVupRequest request) {
        String name = requireText(request.name(), "VUP名称不能为空");
        String persona = request.persona() == null || request.persona().isBlank()
                ? "低压杂谈新人"
                : request.persona().trim();

        Vup vup = new Vup();
        vup.setUserId(userId);
        vup.setSlotNumber(1);
        vup.setName(name);
        vup.setPersona(persona);
        vup.setStatus(VupStatus.ACTIVE.name());
        vup.setDayCount(1);
        vup.setRunSeed("run-" + UUID.randomUUID());
        vup.setSongPower(10);
        vup.setDancePower(10);
        vup.setTalkPower(12);
        vup.setMemePower(10);
        vup.setPlanPower(10);
        vup.setStressPower(10);
        vup.setStamina(balanceConfig.startingStamina());
        vup.setMaxStamina(balanceConfig.startingStamina());
        vup.setCoin(balanceConfig.startingCoin());
        vup.setInspiration(balanceConfig.startingInspiration());
        vup.setTrueFans(balanceConfig.startingTrueFans());
        vup.setFunFans(balanceConfig.startingFunFans());
        vup.setUnicornFans(balanceConfig.startingUnicornFans());
        vup.setDdFans(balanceConfig.startingDdFans());
        vup.setFans(balanceConfig.startingTrueFans()
                + balanceConfig.startingFunFans()
                + balanceConfig.startingUnicornFans()
                + balanceConfig.startingDdFans());
        vup.setPopularity(0);
        vup.setWatchHeat(0);
        vup.setReputation(balanceConfig.startingReputation());
        vup.setMemeLevel(0);
        vup.setCommercialLevel(0);
        vup.setCurrentRoute(RouteType.UNKNOWN.name());
        vup.setDifficulty(BalanceConfig.parseDifficulty(request.difficulty()).name());
        vup.setExpectationJson("{}");
        vup.setRouteScoreJson("""
                {"ELECTRONIC_PICKLE":0,"SINGING_IDOL":0,"SLICE_SAINT":0,"SOCIAL_COLLAB":0,"DANCE_MEME":0,"BLACK_RED_MAIN_STAGE":0,"UNKNOWN":0}
                """.trim());
        applyCreationStyleProfile(vup, creationStyleProfileFor(persona));
        vup.setTutorialFlagsJson("{}");
        return vup;
    }

    private CreationStyleProfile creationStyleProfileFor(String persona) {
        if (persona == null || !persona.contains("出道风格")) {
            return null;
        }
        if (persona.contains("歌势练习生")) {
            return new CreationStyleProfile(
                    "歌势练习生",
                    RouteType.SINGING_IDOL.name(),
                    "歌回练功",
                    "首日适合先练歌或开低风险歌回，把灯牌点歌、录播复盘和基本功证据先攒起来。",
                    3, 0, 0, 0, 0, 0,
                    1,
                    6, 0, 0, 0,
                    0, 0, 2, 0, 0,
                    3
            );
        }
        if (persona.contains("国风茶馆")) {
            return new CreationStyleProfile(
                    "国风茶馆",
                    RouteType.ELECTRONIC_PICKLE.name(),
                    "茶馆杂谈",
                    "首日适合开低压茶馆杂谈或粉丝群维护，把小曲、闲谈和舰长群气压稳住。",
                    0, 0, 2, 0, 1, 0,
                    0,
                    5, 0, 0, 2,
                    0, 0, 4, 0, 0,
                    3
            );
        }
        if (persona.contains("切片工坊")) {
            return new CreationStyleProfile(
                    "切片工坊",
                    RouteType.SLICE_SAINT.name(),
                    "投稿箱供货",
                    "首日适合先投视频或维护投稿箱，给切片组、烤肉组和二创征集准备可剪素材。",
                    0, 0, 0, 3, 0, 0,
                    1,
                    0, 8, 0, 0,
                    0, 5, 0, 4, 0,
                    3
            );
        }
        if (persona.contains("低压电台")) {
            return new CreationStyleProfile(
                    "低压电台",
                    RouteType.ELECTRONIC_PICKLE.name(),
                    "低压杂谈",
                    "首日适合杂谈复盘或粉丝群维护，先让老粉确认陪饭台稳定开麦。",
                    0, 0, 2, 0, 0, 1,
                    0,
                    7, 0, 0, 0,
                    0, 0, 4, 0, 0,
                    3
            );
        }
        return null;
    }

    private void applyCreationStyleProfile(Vup vup, CreationStyleProfile profile) {
        if (profile == null) {
            return;
        }

        vup.setSongPower(vup.getSongPower() + profile.songPowerDelta());
        vup.setDancePower(vup.getDancePower() + profile.dancePowerDelta());
        vup.setTalkPower(vup.getTalkPower() + profile.talkPowerDelta());
        vup.setMemePower(vup.getMemePower() + profile.memePowerDelta());
        vup.setPlanPower(vup.getPlanPower() + profile.planPowerDelta());
        vup.setStressPower(vup.getStressPower() + profile.stressPowerDelta());
        vup.setInspiration(vup.getInspiration() + profile.inspirationDelta());
        vup.setTrueFans(vup.getTrueFans() + profile.trueFanDelta());
        vup.setFunFans(vup.getFunFans() + profile.funFanDelta());
        vup.setUnicornFans(vup.getUnicornFans() + profile.unicornFanDelta());
        vup.setDdFans(vup.getDdFans() + profile.ddFanDelta());
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        vup.setPopularity(vup.getPopularity() + profile.popularityDelta());
        vup.setWatchHeat(vup.getWatchHeat() + profile.watchHeatDelta());
        vup.setReputation(vup.getReputation() + profile.reputationDelta());
        vup.setMemeLevel(vup.getMemeLevel() + profile.memeLevelDelta());
        vup.setCommercialLevel(vup.getCommercialLevel() + profile.commercialLevelDelta());
        vup.setCurrentRoute(profile.routeKey());
        vup.setRouteScoreJson(jsonService.write(routeScoreSeed(profile.routeKey(), profile.routeScore())));
        vup.setExpectationJson(jsonService.write(Map.of(
                "creationStyle", profile.label(),
                "recommendedPlan", profile.recommendedPlan(),
                "openingAdvice", profile.openingAdvice()
        )));
    }

    private Map<String, Integer> routeScoreSeed(String routeKey, int routeScore) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), 0);
        }
        scores.put(routeKey, routeScore);
        return scores;
    }

    private record CreationStyleProfile(
            String label,
            String routeKey,
            String recommendedPlan,
            String openingAdvice,
            int songPowerDelta,
            int dancePowerDelta,
            int talkPowerDelta,
            int memePowerDelta,
            int planPowerDelta,
            int stressPowerDelta,
            int inspirationDelta,
            int trueFanDelta,
            int funFanDelta,
            int unicornFanDelta,
            int ddFanDelta,
            int popularityDelta,
            int watchHeatDelta,
            int reputationDelta,
            int memeLevelDelta,
            int commercialLevelDelta,
            int routeScore
    ) {
    }

    private DaySession dayOneSession(Vup vup) {
        DaySession session = new DaySession();
        session.setVupId(vup.getId());
        session.setDay(1);
        session.setPhase(DayPhase.READY.name());
        session.setTitleCandidatesJson("[]");
        session.setTitleRerollCount(0);
        session.setStreamPlanCancelled(false);
        session.setRiskToolUsed(false);
        session.setFormalEventSlotStatus("EMPTY");
        session.setFormalEventPriority(0);
        session.setRandomSeed(vup.getRunSeed() + "-day-1");
        session.setRngCursor(0);
        session.setLocked(false);
        return session;
    }

    private List<TutorialHintDTO> consumeTutorialHints(Vup vup, DaySession session) {
        List<TutorialHintDTO> hints = new ArrayList<>();
        Map<String, Object> flags = new LinkedHashMap<>(jsonMap(vup.getTutorialFlagsJson()));
        if (vup.getDayCount() == 1
                && DayPhase.READY.name().equals(session.getPhase())
                && !Boolean.TRUE.equals(flags.get("DAY_ONE_ACTION"))) {
            hints.add(new TutorialHintDTO("DAY_ONE_ACTION", "每天选择1个主行动；直播企划会先进入标题选择，非直播行动会直接生成日报。"));
            flags.put("DAY_ONE_ACTION", true);
        }
        if (!hints.isEmpty()) {
            vup.setTutorialFlagsJson(jsonService.write(flags));
            vupMapper.updateState(vup);
        }
        return hints;
    }

    private Map<String, Object> jsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(json);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String endingGrade(int score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 78) {
            return "A";
        }
        if (score >= 62) {
            return "B";
        }
        if (score >= 45) {
            return "C";
        }
        return "D";
    }

    private String nextEndingGrade(int score) {
        if (score < 45) {
            return "C";
        }
        if (score < 62) {
            return "B";
        }
        if (score < 78) {
            return "A";
        }
        return "S";
    }

    private int nextEndingGradeScore(int score) {
        if (score < 45) {
            return 45;
        }
        if (score < 62) {
            return 62;
        }
        if (score < 78) {
            return 78;
        }
        return 90;
    }

    private void applyRestartBias(Vup vup, String fanBiasType) {
        int fanBias = vup.getFans() * 5 / 100;
        switch (fanBiasType) {
            case "TRUE_FANS" -> vup.setTrueFans(vup.getTrueFans() + fanBias);
            case "FUN_FANS" -> vup.setFunFans(vup.getFunFans() + fanBias);
            case "UNICORN_FANS" -> vup.setUnicornFans(vup.getUnicornFans() + fanBias);
            case "DD_FANS" -> vup.setDdFans(vup.getDdFans() + fanBias);
            default -> {
                return;
            }
        }
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
    }

    private void applyRestartRouteBias(Vup vup, String routeBiasType, String restartTargetType, Long previousEndingId) {
        String route = normalizeRouteBias(routeBiasType);
        String requestedTarget = restartTargetType == null || restartTargetType.isBlank() ? route : restartTargetType.trim();
        EndingAtlasService.RestartTarget restartTarget = endingAtlasService.restartTarget(requestedTarget);
        String target = restartTarget.targetType();
        if (RouteType.UNKNOWN.name().equals(route) && !RouteType.UNKNOWN.name().equals(restartTarget.routeBiasType())) {
            route = restartTarget.routeBiasType();
        }
        EndingReview previousReview = previousEndingId == null ? null : endingReviewMapper.findById(previousEndingId);
        if (!RouteType.UNKNOWN.name().equals(route)) {
            vup.setCurrentRoute(route);
            vup.setRouteScoreJson(jsonService.write(routeScoreSeed(route, 4)));
            applyRestartRouteStarterStats(vup, route);
        } else {
            vup.setCurrentRoute(RouteType.UNKNOWN.name());
            vup.setRouteScoreJson(jsonService.write(routeScoreSeed(RouteType.UNKNOWN.name(), 0)));
        }

        Map<String, Object> expectations = new LinkedHashMap<>(jsonMap(vup.getExpectationJson()));
        expectations.put("restartTargetType", target);
        expectations.put("restartRouteBiasType", route);
        expectations.put("restartTargetLabel", restartTarget.label());
        expectations.put("restartRunObjective", restartTarget.runObjective());
        expectations.put("openingAdvice", restartTarget.openingAdvice());
        expectations.put("previousEndingId", previousEndingId);
        Map<String, Object> gradeContract = restartGradeContract(previousReview, target);
        expectations.put("restartGradeContract", gradeContract);
        expectations.put("previousEndingMemory", previousEndingMemory(target, route, previousEndingId, gradeContract));
        expectations.put("restartLegacyTag", restartTarget.legacyTag());
        expectations.put("restartBoundary", restartTarget.boundary());
        vup.setExpectationJson(jsonService.write(expectations));
    }

    private String normalizeRouteBias(String routeBiasType) {
        if (routeBiasType == null || routeBiasType.isBlank()) {
            return RouteType.UNKNOWN.name();
        }
        try {
            return RouteType.valueOf(routeBiasType).name();
        } catch (IllegalArgumentException exception) {
            return RouteType.UNKNOWN.name();
        }
    }

    private void applyRestartRouteStarterStats(Vup vup, String route) {
        switch (route) {
            case "SINGING_IDOL" -> {
                vup.setSongPower(vup.getSongPower() + 2);
                vup.setInspiration(vup.getInspiration() + 1);
            }
            case "SLICE_SAINT" -> {
                vup.setMemePower(vup.getMemePower() + 2);
                vup.setInspiration(vup.getInspiration() + 1);
            }
            case "DANCE_MEME" -> {
                vup.setDancePower(vup.getDancePower() + 2);
                vup.setMemeLevel(vup.getMemeLevel() + 4);
            }
            case "SOCIAL_COLLAB" -> {
                vup.setPlanPower(vup.getPlanPower() + 1);
                vup.setTalkPower(vup.getTalkPower() + 1);
            }
            case "BLACK_RED_MAIN_STAGE" -> {
                vup.setStressPower(vup.getStressPower() + 2);
                vup.setWatchHeat(vup.getWatchHeat() + 4);
            }
            case "ELECTRONIC_PICKLE" -> {
                vup.setTalkPower(vup.getTalkPower() + 1);
                vup.setReputation(vup.getReputation() + 2);
            }
            default -> {
            }
        }
    }

    private String restartTargetLabel(String target) {
        return endingAtlasService.restartTarget(target).label();
    }

    private Map<String, Object> previousEndingMemory(
            String target,
            String route,
            Long previousEndingId,
            Map<String, Object> gradeContract
    ) {
        String targetLabel = restartTargetLabel(target);
        String routeLabel = restartTargetLabel(route);
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("previousEndingId", previousEndingId);
        memory.put("targetType", target);
        memory.put("targetLabel", targetLabel);
        memory.put("legacyTag", restartLegacyTag(target));
        memory.put("runObjective", restartRunObjective(target));
        memory.put("summary", "上轮结局已归档为「" + targetLabel + "」。这轮开局继承少量"
                + routeLabel + "路线倾向，但不继承旧债和永久倍率。");
        memory.put("nextGoal", endingAtlasService.restartTarget(target).openingAdvice());
        memory.put("gradeContract", gradeContract);
        memory.put("gradeContractLine", gradeContractLine(gradeContract, targetLabel));
        memory.put("boundary", "复活赛是目标提示，不是永久成长。粉丝偏置封顶5%，债务和事件重新开局。");
        return memory;
    }

    private Map<String, Object> restartGradeContract(EndingReview previousReview, String target) {
        Map<String, Object> scorecard = scorecardFor(previousReview);
        int previousScore = intValue(scorecard.get("overall"));
        int routeFocusScore = intValue(scorecard.get("routeFocusScore"));
        int evidenceScore = intValue(scorecard.get("evidenceScore"));
        int riskControlScore = intValue(scorecard.get("riskControlScore"));
        String previousGrade = nonBlank(stringValue(scorecard.get("grade")))
                ? stringValue(scorecard.get("grade"))
                : endingGrade(previousScore);
        String nextGrade = nextEndingGrade(previousScore);
        int nextGradeScore = nextEndingGradeScore(previousScore);
        String weakness = primaryWeakness(routeFocusScore, evidenceScore, riskControlScore);
        String targetLabel = restartTargetLabel(target);
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("targetType", target);
        contract.put("targetLabel", targetLabel);
        contract.put("previousScore", previousScore);
        contract.put("previousGrade", previousScore > 0 ? previousGrade : "未评分");
        contract.put("nextGrade", nextGrade);
        contract.put("nextGradeScore", nextGradeScore);
        contract.put("scoreGap", Math.max(0, nextGradeScore - previousScore));
        contract.put("primaryWeakness", weakness);
        contract.put("primaryWeaknessLabel", weaknessLabel(weakness));
        contract.put("stageOneGoal", stageOneContractGoal(targetLabel, weakness));
        contract.put("stageTwoGoal", stageTwoContractGoal(targetLabel, weakness, nextGrade));
        contract.put("summary", gradeContractLine(contract, targetLabel));
        return contract;
    }

    private Map<String, Object> scorecardFor(EndingReview review) {
        if (review == null) {
            return Map.of();
        }
        Object scorecardRaw = jsonMap(review.getRouteReviewJson()).get("scorecard");
        if (scorecardRaw instanceof Map<?, ?> rawMap) {
            Map<String, Object> scorecard = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    scorecard.put(key, entry.getValue());
                }
            }
            return scorecard;
        }
        return Map.of();
    }

    private String gradeContractLine(Map<String, Object> contract, String targetLabel) {
        int previousScore = intValue(contract.get("previousScore"));
        if (previousScore <= 0) {
            return "本轮先完成「" + targetLabel + "」首通，再记录个人最佳评级。";
        }
        return "上轮" + stringValue(contract.get("previousGrade")) + " " + previousScore
                + "分，本轮冲" + stringValue(contract.get("nextGrade"))
                + "档；主短板：" + stringValue(contract.get("primaryWeaknessLabel")) + "。";
    }

    private String primaryWeakness(int routeFocusScore, int evidenceScore, int riskControlScore) {
        if (riskControlScore > 0 && riskControlScore <= routeFocusScore && riskControlScore <= evidenceScore) {
            return "RISK_CONTROL";
        }
        if (evidenceScore > 0 && evidenceScore <= routeFocusScore) {
            return "EVIDENCE";
        }
        return "ROUTE_FOCUS";
    }

    private String weaknessLabel(String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "风险控制";
            case "EVIDENCE" -> "证据链";
            default -> "路线专注";
        };
    }

    private String stageOneContractGoal(String targetLabel, String weakness) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "第1周先稳口碑和旧账，高危债务不要进中段。";
            case "EVIDENCE" -> "第1周给「" + targetLabel + "」留下3条可复盘行动。";
            default -> "第1周连续补「" + targetLabel + "」同类路线证据，别摇摆。";
        };
    }

    private String stageTwoContractGoal(String targetLabel, String weakness, String nextGrade) {
        return switch (weakness) {
            case "RISK_CONTROL" -> "第2周把未结旧账压到2笔以内，再冲「" + targetLabel + "」" + nextGrade + "档。";
            case "EVIDENCE" -> "第2周让内容资产或代表事件上桌，给「" + targetLabel + "」补够证据链。";
            default -> "第2周把路线分拉到12以上，给「" + targetLabel + "」冲" + nextGrade + "档打底。";
        };
    }

    private String restartLegacyTag(String target) {
        return endingAtlasService.restartTarget(target).legacyTag();
    }

    private String restartRunObjective(String target) {
        return endingAtlasService.restartTarget(target).runObjective();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new GameException("CONFIG_FIELD_INVALID", message);
        }
        return value.trim();
    }
}
