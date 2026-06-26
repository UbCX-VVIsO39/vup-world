package com.example.vupworld.service.fan;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.FanTopicDtos.ChooseFanTopicRequest;
import com.example.vupworld.dto.FanTopicDtos.FanTopicChoiceDTO;
import com.example.vupworld.dto.FanTopicDtos.FanTopicOptionDTO;
import com.example.vupworld.dto.FanTopicDtos.FanTopicResultDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Service
public class FanTopicService {
    private static final String FAN_TOPIC_PATH = "/api/fan-topic/choose";
    private static final String CLIP_SUBMISSION_STOCK = "CLIP_SUBMISSION_STOCK";
    private static final Set<String> TOPIC_KEYS = Set.of(
            "OLD_FANS_WORRY",
            "CLIP_TEAM_VS_OLD_FANS",
            "UNICORN_LOW_PRESSURE",
            "BOSS_TOO_LOUD",
            "MOD_OVERWORKED"
    );

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final BusinessLogMapper businessLogMapper;
    private final JsonService jsonService;
    private final IdempotencyRunner idempotencyRunner;
    private final RequestHashService requestHashService;
    private final DayFlowService dayFlowService;

    public FanTopicService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            BusinessLogMapper businessLogMapper,
            JsonService jsonService,
            IdempotencyRunner idempotencyRunner,
            RequestHashService requestHashService,
            DayFlowService dayFlowService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.businessLogMapper = businessLogMapper;
        this.jsonService = jsonService;
        this.idempotencyRunner = idempotencyRunner;
        this.requestHashService = requestHashService;
        this.dayFlowService = dayFlowService;
    }

    public List<FanTopicOptionDTO> options(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        boolean phaseReady = DayPhase.READY.name().equals(session.getPhase());
        boolean handledToday = phaseReady && businessLogMapper.findLatestFanTopicByVupIdAndDay(vup.getId(), session.getDay()) != null;
        boolean enabled = phaseReady && !handledToday;
        String disabledReason = !phaseReady ? "PHASE_NOT_ALLOWED" : (handledToday ? "FAN_TOPIC_ALREADY_HANDLED" : null);
        List<FanTopicOptionDTO> topics = new ArrayList<>();
        topics.add(topicOption(
                vup,
                enabled,
                disabledReason,
                "OLD_FANS_WORRY",
                "老粉说不是破防，只是聊聊",
                "粉丝群开始复盘最近的标题和路线，老粉嘴上说冷静，手里已经写了三段小作文。",
                "真爱粉较高、频繁转型或围观热度上来时更容易出现",
                "口碑波动，小作文风险"
        ));
        if (clipRouteTopicVisible(vup)) {
            topics.add(topicOption(
                    vup,
                    enabled,
                    disabledReason,
                    "CLIP_TEAM_VS_OLD_FANS",
                    "切片组和老粉在标题尺度上拉扯",
                    "切片组想继续供货，老粉担心首页只剩标题党，群里开始讨论要不要换素材。",
                    "最近7天有投稿或切片，且乐子人/素材库存已经抬头",
                    "切片收益高，但复读和回旋镖风险会上升"
            ));
        }
        if (unicornRatioOf(vup) >= 0.20 || vup.getCommercialLevel() >= 8) {
            topics.add(topicOption(
                    vup,
                    enabled,
                    disabledReason,
                    "UNICORN_LOW_PRESSURE",
                    "独角兽开始计算互动秒数",
                    "独角兽把陪伴感做成表格，老粉和DD都在看群公告怎么写。",
                    "独角兽占比≥20%或商业化感知较高时出现",
                    "处理不好会让陪伴期待变成小作文"
            ));
        }
        if (vup.getCommercialLevel() >= 6 || vup.getCoin() >= 3200) {
            topics.add(topicOption(
                    vup,
                    enabled,
                    disabledReason,
                    "BOSS_TOO_LOUD",
                    "榜一声音太大，群里开始脑补",
                    "榜一排班和礼物截图被反复转发，群友开始问这是支持还是运营。",
                    "运营预算和商业化上升后更容易出现",
                    "商业味会被放大，口碑需要稳住"
            ));
        }
        if (vup.getWatchHeat() >= 35 || vup.getMemeLevel() >= 35) {
            topics.add(topicOption(
                    vup,
                    enabled,
                    disabledReason,
                    "MOD_OVERWORKED",
                    "房管手速拉满但群友还在急",
                    "房管在删重复梗，切片组在催素材，群里像开了三个分会场。",
                    "围观热度或串味较高时出现",
                    "继续装死会让群聊变成小主会场"
            ));
        }
        return topics;
    }

    @Transactional
    public FanTopicResultDTO choose(Long userId, ChooseFanTopicRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        requireStableFanTopicKey(idempotencyKey);
        String topicKey = normalizeTopicKey(request.topicKey());
        String choiceType = normalizeChoiceType(request.choiceType());
        ChooseFanTopicRequest normalizedRequest = new ChooseFanTopicRequest(topicKey, choiceType, idempotencyKey);
        String requestHash = requestHashService.hash(normalizedRequest);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, FanTopicResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(FAN_TOPIC_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, FAN_TOPIC_PATH, idempotencyKey, requestHash,
                () -> doChoose(vup, session, normalizedRequest, idempotencyKey),
                FanTopicResultDTO.class, buildRecord);
    }

    private void requireStableFanTopicKey(String idempotencyKey) {
        if (idempotencyKey.matches("fan-topic-\\d+")) {
            throw new GameException("CONFIG_FIELD_INVALID", "粉丝群议题写接口必须使用稳定幂等键。");
        }
    }

    private FanTopicResultDTO doChoose(Vup vup, DaySession session, ChooseFanTopicRequest request, String idempotencyKey) {
        if (!DayPhase.READY.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "当前阶段不能处理粉丝群议题，先把手头直播间状态收完。");
        }
        BusinessLog handledToday = businessLogMapper.findLatestFanTopicByVupIdAndDay(vup.getId(), session.getDay());
        if (handledToday != null) {
            throw new GameException("FAN_TOPIC_ALREADY_HANDLED", "今天已经处理过粉丝群议题，群友的主会场先别反复开新楼。");
        }

        String topicKey = normalizeTopicKey(request.topicKey());
        String choiceType = normalizeChoiceType(request.choiceType());
        int oldReputation = vup.getReputation();
        int oldWatchHeat = vup.getWatchHeat();
        int oldInspiration = vup.getInspiration();

        ChoiceOutcome outcome = applyChoice(vup, topicKey, choiceType);
        vupMapper.updateState(vup);

        int reputationDelta = vup.getReputation() - oldReputation;
        int watchHeatDelta = vup.getWatchHeat() - oldWatchHeat;
        int inspirationDelta = vup.getInspiration() - oldInspiration;
        BusinessLog log = toBusinessLog(vup, session, topicKey, choiceType, idempotencyKey,
                outcome, reputationDelta, watchHeatDelta, inspirationDelta);
        businessLogMapper.insert(log);

        return new FanTopicResultDTO(
                session.getPhase(),
                topicKey,
                choiceType,
                reputationDelta,
                watchHeatDelta,
                inspirationDelta,
                outcome.summary(),
                Map.of(
                        "type", "business_log",
                        "id", log.getId(),
                        "day", session.getDay(),
                        "action", "FAN_TOPIC",
                        "fanGroupTopicId", topicKey,
                        "choiceType", choiceType
                )
        );
    }

    private List<FanTopicChoiceDTO> fanTopicChoices(Vup vup, boolean enabled, String disabledReason) {
        boolean fanMeetingEnabled = enabled && vup.getInspiration() > 0;
        String fanMeetingDisabledReason = enabled ? "INSUFFICIENT_INSPIRATION" : disabledReason;
        return List.of(
                new FanTopicChoiceDTO(
                        "APPEASE_OLD_FANS",
                        "认真安抚老粉",
                        "无资源成本",
                        "口碑+2，围观热度-5，真粉+1，群友暂时收起小作文",
                        "人气收益偏低，但米线稳，偏真粉回血",
                        enabled,
                        disabledReason
                ),
                new FanTopicChoiceDTO(
                        "COLLECT_SUBMISSIONS",
                        "改成投稿征集",
                        "无资源成本",
                        "灵感+1，乐子人+2，素材库+1，切片组递素材",
                        "偏乐子粉，可能把正经讨论转成整活现场",
                        enabled,
                        disabledReason
                ),
                new FanTopicChoiceDTO(
                        "HOLD_FAN_MEETING",
                        "开低压粉丝会",
                        "1灵感",
                        "口碑+3，围观热度-8，真粉+1，独角兽+1，房管终于能喝口水",
                        "偏真粉与独角兽，灵感不足时不可用，开不好就像复读大会",
                        fanMeetingEnabled,
                        fanMeetingEnabled ? null : fanMeetingDisabledReason
                ),
                new FanTopicChoiceDTO(
                        "OBSERVE",
                        "装死观察",
                        "无资源成本",
                        "围观热度+2，DD+1，今天先不下场",
                        "偏DD围观，后续可能被楼友贷款成新剧情",
                        enabled,
                        disabledReason
                )
        );
    }

    private FanTopicOptionDTO topicOption(
            Vup vup,
            boolean enabled,
            String disabledReason,
            String topicKey,
            String title,
            String description,
            String triggerPreview,
            String riskPreview
    ) {
        return new FanTopicOptionDTO(
                topicKey,
                title,
                description,
                triggerPreview,
                riskPreview,
                fanTopicChoices(vup, enabled, disabledReason)
        );
    }

    private boolean clipRouteTopicVisible(Vup vup) {
        if (vup.getFunFans() >= 35 || materialStock(vup) > 0 || RouteType.SLICE_SAINT.name().equals(vup.getCurrentRoute())) {
            return true;
        }
        return businessLogMapper.findRecentByVupId(vup.getId(), 10).stream()
                .anyMatch(log -> ActionType.PUBLISH_VIDEO.name().equals(log.getAction())
                        || ActionType.PUBLISH_CLIP.name().equals(log.getAction()));
    }

    private String normalizeTopicKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "OLD_FANS_WORRY";
        }
        String value = raw.trim();
        if (!TOPIC_KEYS.contains(value)) {
            throw new GameException("CONFIG_FIELD_INVALID", "未知粉丝群议题，群友还没开这个楼。");
        }
        return value;
    }

    private String normalizeChoiceType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new GameException("CHOICE_NOT_AVAILABLE", "粉丝群议题必须提供处理方式。");
        }
        return raw.trim();
    }

    private ChoiceOutcome applyChoice(Vup vup, String topicKey, String choiceType) {
        int trueFanDelta = 0;
        int funFanDelta = 0;
        int unicornDelta = 0;
        int ddDelta = 0;
        String summary;
        switch (choiceType) {
            case "APPEASE_OLD_FANS" -> {
                vup.setReputation(clamp(vup.getReputation() + 2, 0, 100));
                vup.setWatchHeat(clamp(vup.getWatchHeat() - 5, 0, 100));
                // 偏真粉：安抚让真粉回血；独角兽议题下独角兽也稍安
                trueFanDelta = 1;
                if ("UNICORN_LOW_PRESSURE".equals(topicKey)) {
                    unicornDelta = 1;
                }
                summary = "你认真安抚了老粉：" + topicLabel(topicKey) + "。群里暂时没继续写小作文，老粉表示先观望。";
            }
            case "COLLECT_SUBMISSIONS" -> {
                vup.setInspiration(clamp(vup.getInspiration() + 1, 0, 10));
                // 偏乐子粉；切片组与老粉拉扯议题下DD也会被素材吸引
                funFanDelta = 2;
                if ("CLIP_TEAM_VS_OLD_FANS".equals(topicKey)) {
                    ddDelta = 1;
                }
                summary = "你把【" + topicLabel(topicKey) + "】改成投稿征集，切片组递来素材，标题组获得一点灵感。素材小票：投稿箱开张 +1，现有"
                        + (materialStock(vup) + 1) + "份；切片组开始查重。";
            }
            case "HOLD_FAN_MEETING" -> {
                if (vup.getInspiration() <= 0) {
                    throw new GameException("INSUFFICIENT_INSPIRATION", "灵感不足，粉丝会开成复读大会。");
                }
                vup.setInspiration(vup.getInspiration() - 1);
                vup.setReputation(clamp(vup.getReputation() + 3, 0, 100));
                vup.setWatchHeat(clamp(vup.getWatchHeat() - 8, 0, 100));
                // 偏真粉+独角兽：低压粉丝会兑现陪伴感
                trueFanDelta = 1;
                unicornDelta = 1;
                summary = "你围绕【" + topicLabel(topicKey) + "】开了一个低压粉丝会，群友暂时撤庭，房管终于能喝口水。";
            }
            case "OBSERVE" -> {
                vup.setWatchHeat(clamp(vup.getWatchHeat() + 2, 0, 100));
                // 偏DD：装死围观，DD顺势坐一站
                ddDelta = 1;
                summary = "你选择先观察【" + topicLabel(topicKey) + "】，群里没立刻爆，但楼友开始贷款后续。";
            }
            default -> throw new GameException("CHOICE_NOT_AVAILABLE", "未知粉丝群处理方式，群友看不懂这套运营。");
        }
        // 统一施加粉丝变化并重算总粉丝
        vup.setTrueFans(Math.max(0, vup.getTrueFans() + trueFanDelta));
        vup.setFunFans(Math.max(0, vup.getFunFans() + funFanDelta));
        vup.setUnicornFans(Math.max(0, vup.getUnicornFans() + unicornDelta));
        vup.setDdFans(Math.max(0, vup.getDdFans() + ddDelta));
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        return new ChoiceOutcome(summary, trueFanDelta, funFanDelta, unicornDelta, ddDelta);
    }

    // 处理选项的效果载体，携带各粉丝类型变化以便落库
    private record ChoiceOutcome(String summary, int trueFanChange, int funFanChange,
                                 int unicornFanChange, int ddFanChange) {
    }

    private int materialStock(Vup vup) {
        return businessLogMapper.countMaterialStockByVupId(vup.getId());
    }

    // 独角兽粉占总粉丝的比例，无粉丝时返回0
    private double unicornRatioOf(Vup vup) {
        int totalFans = vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans();
        if (totalFans <= 0) {
            return 0.0;
        }
        return (double) vup.getUnicornFans() / totalFans;
    }

    private String topicLabel(String topicKey) {
        return switch (topicKey) {
            case "CLIP_TEAM_VS_OLD_FANS" -> "切片组和老粉在标题尺度上拉扯";
            case "UNICORN_LOW_PRESSURE" -> "独角兽开始计算互动秒数";
            case "BOSS_TOO_LOUD" -> "榜一声音太大，群里开始脑补";
            case "MOD_OVERWORKED" -> "房管手速拉满但群友还在急";
            default -> "老粉说不是破防，只是聊聊";
        };
    }

    private BusinessLog toBusinessLog(
            Vup vup,
            DaySession session,
            String topicKey,
            String choiceType,
            String idempotencyKey,
            ChoiceOutcome outcome,
            int reputationDelta,
            int watchHeatDelta,
            int inspirationDelta
    ) {
        int trueFanChange = outcome.trueFanChange();
        int funFanChange = outcome.funFanChange();
        int unicornFanChange = outcome.unicornFanChange();
        int ddFanChange = outcome.ddFanChange();
        int fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase("FAN_TOPIC_HANDLED");
        log.setIdempotencyKey(idempotencyKey);
        log.setAction("FAN_TOPIC");
        log.setMemeSubtype("COLLECT_SUBMISSIONS".equals(choiceType) ? CLIP_SUBMISSION_STOCK : null);
        log.setFanGroupTopicId(topicKey);
        log.setResult(outcome.summary());
        log.setRawFanGain(fanChange);
        log.setFinalFanGain(fanChange);
        log.setFanChange(fanChange);
        log.setTrueFanChange(trueFanChange);
        log.setFunFanChange(funFanChange);
        log.setUnicornFanChange(unicornFanChange);
        log.setDdFanChange(ddFanChange);
        log.setPopularityChange(0);
        log.setWatchHeatChange(watchHeatDelta);
        log.setReputationChange(reputationDelta);
        log.setMemeChange(0);
        log.setCommercialChange(0);
        log.setCoinChange(0);
        log.setInspirationChange(inspirationDelta);
        log.setMultiplierDetail(jsonService.write(Map.of("pipeline", "P0_FAN_TOPIC")));
        log.setCapDetail(jsonService.write(Map.of("stageCap", 50, "hit", false)));
        log.setClampDetail(jsonService.write(Map.of("reputation", vup.getReputation(), "watchHeat", vup.getWatchHeat())));
        log.setWeightDetail(jsonService.write(Map.of("topicKey", topicKey, "choiceType", choiceType)));
        log.setRngDetail(jsonService.write(Map.of("seed", session.getRandomSeed(), "cursor", session.getRngCursor())));
        log.setExpectationChange(jsonService.write(Map.of("fanTopic", topicKey, "choice", choiceType)));
        log.setRouteScoreChange("{}");
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(true);
        return log;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
