package com.example.vupworld.service.ending;

import com.example.vupworld.service.risk.RiskDebtLabels;

import com.example.vupworld.service.content.ContentCatalogService;
import com.example.vupworld.service.operating.OperatingPressureService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.progression.PermanentUnlockService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.EndingDtos.EndingTagDTO;
import com.example.vupworld.dto.EndingDtos.EndingReviewDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.EndingReviewMapper;
import com.example.vupworld.mapper.GameStatsHistoryMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DailyReport;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.EndingReview;
import com.example.vupworld.model.GameStatsHistory;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EndingService {

    private static final Logger log = LoggerFactory.getLogger(EndingService.class);
    private static final List<String> MAIN_ACTION_REPLAY_POOL = List.of(
            "STREAM_PLAN",
            "TRAIN_SONG",
            "TRAIN_DANCE",
            "TRAIN_TALK",
            "PUBLISH_VIDEO",
            "PUBLISH_CLIP",
            "NPC_INTERACT",
            "FAN_GROUP_MAINTAIN",
            "REST"
    );

    private final EndingReviewMapper endingReviewMapper;
    private final GameStatsHistoryMapper gameStatsHistoryMapper;
    private final BusinessLogMapper businessLogMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final JsonService jsonService;
    private final ContentCatalogService contentCatalogService;
    private final EndingRuleEngine endingRuleEngine;
    private final PermanentUnlockService permanentUnlockService;
    private final OperatingPressureService operatingPressureService;

    public EndingService(
            EndingReviewMapper endingReviewMapper,
            GameStatsHistoryMapper gameStatsHistoryMapper,
            BusinessLogMapper businessLogMapper,
            RiskDebtMapper riskDebtMapper,
            JsonService jsonService,
            ContentCatalogService contentCatalogService,
            EndingRuleEngine endingRuleEngine,
            PermanentUnlockService permanentUnlockService,
            OperatingPressureService operatingPressureService
    ) {
        this.endingReviewMapper = endingReviewMapper;
        this.gameStatsHistoryMapper = gameStatsHistoryMapper;
        this.businessLogMapper = businessLogMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.jsonService = jsonService;
        this.contentCatalogService = contentCatalogService;
        this.endingRuleEngine = endingRuleEngine;
        this.permanentUnlockService = permanentUnlockService;
        this.operatingPressureService = operatingPressureService;
    }

    @Transactional
    public EndingReview createEndingReview(Vup vup, DaySession session, DailyReport finalReport, BusinessLog finalLog) {
        List<BusinessLog> evidenceLogs = endingReferenceLogs(vup, finalLog);
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        EndingRuleEngine.EndingResult endingResult = endingRuleEngine.evaluate(
                vup,
                session,
                finalReport,
                finalLog,
                openDebts,
                evidenceLogs,
                true
        );
        String endingType = endingResult.endingType();
        log.info("Creating ending review for vupId={}, endingType={}, fans={}", vup.getId(), endingType, vup.getFans());

        EndingCopyDecision copyDecision = endingCopyDecision(endingType, vup, finalLog, evidenceLogs);
        BusinessLog finalDayActionLog = finalDayActionLog(vup, finalLog);
        List<EndingTagDTO> tags = new ArrayList<>(endingTags(endingType, vup));
        // Phase 6: Add pressure ending marks (permanent marks for this run only)
        tags.addAll(pressureEndingMarks(vup));
        List<Map<String, Object>> keyEvents = keyEvents(vup, finalLog, endingType);
        Map<String, Object> fanProfile = Map.of(
                "fans", vup.getFans(),
                "trueFans", vup.getTrueFans(),
                "funFans", vup.getFunFans(),
                "unicornFans", vup.getUnicornFans(),
                "ddFans", vup.getDdFans(),
                "watchHeat", vup.getWatchHeat(),
                "reputation", vup.getReputation(),
                "memeLevel", vup.getMemeLevel(),
                "commercialLevel", vup.getCommercialLevel()
        );
        Map<String, Object> routeReview = new java.util.LinkedHashMap<>();
        routeReview.put("currentRoute", vup.getCurrentRoute());
        routeReview.put("baseRouteType", baseRouteTypeForEnding(endingType, vup.getCurrentRoute()));
        routeReview.put("baseRouteLabel", baseRouteLabelForEnding(endingType, vup.getCurrentRoute()));
        routeReview.put("derivedEndingType", endingType);
        routeReview.put("derivedEndingLabel", finalTitle(endingType));
        routeReview.put("routeEndingRelation", routeEndingRelation(endingType, vup));
        routeReview.put("routeScoreJson", vup.getRouteScoreJson());
        routeReview.put("finalDayAction", actionName(finalDayActionLog));
        routeReview.put("evidence", "business_log#" + finalLog.getId());
        routeReview.put("finalReportRef", Map.of("type", "daily_report", "id", finalReport.getId(), "day", finalReport.getDay()));
        routeReview.put("routeEvidenceTrail", routeEvidenceTrail(vup));
        routeReview.put("accidentMaterials", accidentMaterials(vup));
        Map<String, Object> scorecard = endingScorecard(vup, endingType, openDebts, evidenceLogs, endingResult);
        routeReview.put("scorecard", scorecard);

        EndingReview review = new EndingReview();
        review.setVupId(vup.getId());
        review.setFinalReportId(finalReport.getId());
        review.setEndingType(endingType);
        review.setFinalTitle(finalTitle(endingType));
        review.setSubtitle(copyDecision.subtitle());
        review.setEndingTagsJson(jsonService.write(tags));
        review.setEndingReason(endingReason(endingType, vup, finalDayActionLog, copyDecision, endingResult));
        review.setEndingReasonJson(endingReasonJson(endingType, vup, finalLog, copyDecision, evidenceLogs, endingResult));
        review.setSummary(summary(endingType, vup));
        review.setFanProfileJson(jsonService.write(fanProfile));
        review.setKeyEventsJson(jsonService.write(keyEvents));
        review.setDebtRefsJson(jsonService.write(debtRefs(vup)));
        review.setRouteReviewJson(jsonService.write(routeReview));
        review.setRestartHint(jsonService.write(restartHint(endingType)));
        endingReviewMapper.insert(review);
        if (vup.getUserId() != null) {
            permanentUnlockService.unlock(vup.getUserId(), endingType);
            // 生涯模式历史：写入本局结算统计，供 CareerService 统计进度与历史回顾
            Object overallObj = scorecard.get("overall");
            Object gradeObj = scorecard.get("grade");
            GameStatsHistory history = new GameStatsHistory();
            history.setUserId(vup.getUserId());
            history.setVupId(vup.getId());
            history.setEndingType(endingType);
            history.setFinalScore(overallObj instanceof Number number ? number.intValue() : 0);
            history.setFinalGrade(gradeObj instanceof String grade ? grade : null);
            history.setFinalFans(vup.getFans());
            history.setFinalReputation(vup.getReputation());
            history.setRunDays(vup.getDayCount());
            history.setRouteKey(vup.getCurrentRoute());
            history.setNgPlusLevel(0);
            gameStatsHistoryMapper.insert(history);
        }

        log.info("Ending review created: id={}, vupId={}, endingType={}", review.getId(), vup.getId(), endingType);
        return review;
    }

    public EndingReviewDTO latestReview(Vup vup) {
        EndingReview review = endingReviewMapper.findLatestByVupId(vup.getId());
        if (review == null) {
            throw new GameException("ENDING_NOT_FOUND", "结局复盘还没生成，不能虚空毕业。");
        }
        return toDto(review);
    }

    private EndingReviewDTO toDto(EndingReview review) {
        String endingType = canonicalEndingType(review.getEndingType());
        return new EndingReviewDTO(
                review.getId(),
                endingType,
                finalTitle(endingType),
                review.getSubtitle(),
                jsonService.readEndingTags(review.getEndingTagsJson()),
                review.getEndingReason(),
                jsonService.readMap(review.getEndingReasonJson()),
                review.getSummary(),
                jsonService.readMap(review.getFanProfileJson()),
                jsonService.readEvidenceRefs(review.getKeyEventsJson()),
                jsonService.readEvidenceRefs(review.getDebtRefsJson()),
                jsonService.readMap(review.getRouteReviewJson()),
                jsonService.readMap(review.getRestartHint())
        );
    }

    private Map<String, Object> restartHint(String endingType) {
        endingType = canonicalEndingType(endingType);
        String baseRouteType = baseRouteTypeForEnding(endingType, "");
        Map<String, Object> hint = new java.util.LinkedHashMap<>();
        hint.put("routeBias", baseRouteType);
        hint.put("targetEndingType", endingType);
        hint.put("targetEndingLabel", finalTitle(endingType));
        hint.put("baseRouteType", baseRouteType);
        hint.put("baseRouteLabel", baseRouteLabel(baseRouteType));
        hint.put("fanBiasPercent", 5);
        hint.put("nextRunActions", nextRunActions(endingType));
        hint.put("text", nextRunAdvice(endingType));
        hint.put("boundary", "只给下一局路线提示和5%粉丝偏置，不继承旧账、资源或属性倍率。");
        return hint;
    }

    private List<BusinessLog> endingReferenceLogs(Vup vup, BusinessLog finalLog) {
        List<BusinessLog> logs = businessLogMapper.findEndingReferenceLogs(vup.getId());
        if (!logs.isEmpty()) {
            return logs;
        }
        return finalLog == null ? List.of() : List.of(finalLog);
    }

    private EndingCopyDecision endingCopyDecision(
            String endingType,
            Vup vup,
            BusinessLog finalLog,
            List<BusinessLog> evidenceLogs
    ) {
        return subtitleDecision(endingType, vup, finalLog, evidenceLogs);
    }

    private List<Map<String, Object>> keyEvents(Vup vup, BusinessLog finalLog, String endingType) {
        List<BusinessLog> logs = endingReferenceLogs(vup, finalLog);
        List<BusinessLog> selected = new ArrayList<>(representativeLogs(logs));
        ensureRouteKeyEvent(selected, logs, vup, finalLog, endingType);
        selected.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId));
        return selected.stream()
                .map(this::toKeyEvent)
                .toList();
    }

    private void ensureRouteKeyEvent(
            List<BusinessLog> selected,
            List<BusinessLog> logs,
            Vup vup,
            BusinessLog finalLog,
            String endingType
    ) {
        List<String> routeActions = routeKeyActions(endingType);
        if (routeActions.isEmpty()) {
            return;
        }
        String preferredAction = routeActions.get(0);
        if (selected.stream().anyMatch(log -> preferredAction.equals(log.getAction()))) {
            return;
        }
        BusinessLog routeLog = logs.stream()
                .filter(log -> preferredAction.equals(log.getAction()))
                .findFirst()
                .orElseGet(() -> routeKeyEventFromAllLogs(vup, finalLog, List.of(preferredAction)));
        if (routeLog == null && selected.stream().anyMatch(log -> routeActions.contains(log.getAction()))) {
            return;
        }
        if (routeLog == null) {
            routeLog = routeActions.stream()
                    .skip(1)
                    .flatMap(action -> logs.stream().filter(log -> action.equals(log.getAction())).findFirst().stream())
                    .findFirst()
                    .orElseGet(() -> routeKeyEventFromAllLogs(vup, finalLog, routeActions));
        }
        if (routeLog == null) {
            return;
        }
        if (selected.size() < 5) {
            addUniqueLog(selected, routeLog);
            return;
        }
        selected.set(routeKeyEventReplacementIndex(selected), routeLog);
    }

    private BusinessLog routeKeyEventFromAllLogs(Vup vup, BusinessLog finalLog, List<String> routeActions) {
        if (vup == null || finalLog == null) {
            return null;
        }
        List<BusinessLog> allLogs = businessLogMapper.findByVupIdBetweenDays(vup.getId(), 1, finalLog.getDay());
        return routeActions.stream()
                .flatMap(action -> allLogs.stream().filter(log -> action.equals(log.getAction())).findFirst().stream())
                .findFirst()
                .orElse(null);
    }

    private List<String> routeKeyActions(String endingType) {
        if (endingType == null) {
            return List.of();
        }
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> List.of("TRAIN_SONG", "STREAM_PLAN", "PUBLISH_VIDEO");
            case "SLICE_SAINT" -> List.of("PUBLISH_CLIP", "PUBLISH_VIDEO", "TRAIN_DANCE");
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> List.of("STREAM_PLAN", "PUBLISH_CLIP", "FAN_GROUP_MAINTAIN");
            case "CYBER_GIRLFRIEND" -> List.of("FAN_GROUP_MAINTAIN", "TRAIN_TALK", "STREAM_PLAN");
            case "GLORIOUS_GRADUATION" -> List.of("FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST");
            case "ELECTRONIC_PICKLE" -> List.of("TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST");
            case "DD_BUS_STOP" -> List.of("NPC_INTERACT", "FAN_GROUP_MAINTAIN", "STREAM_PLAN");
            default -> List.of();
        };
    }

    private int routeKeyEventReplacementIndex(List<BusinessLog> selected) {
        int earliestDay = selected.stream().mapToInt(BusinessLog::getDay).min().orElse(0);
        int latestDay = selected.stream().mapToInt(BusinessLog::getDay).max().orElse(0);
        int fallbackIndex = Math.max(0, selected.size() - 2);
        int lowestIndex = -1;
        int lowestScore = Integer.MAX_VALUE;
        for (int index = selected.size() - 1; index >= 0; index--) {
            BusinessLog log = selected.get(index);
            if (log.getDay() == earliestDay || log.getDay() == latestDay) {
                continue;
            }
            if ("EVENT_CHOICE".equals(log.getAction())) {
                fallbackIndex = index;
                continue;
            }
            int score = keyEventScore(log);
            if (score < lowestScore) {
                lowestScore = score;
                lowestIndex = index;
            }
        }
        return lowestIndex >= 0 ? lowestIndex : fallbackIndex;
    }

    private BusinessLog finalDayActionLog(Vup vup, BusinessLog finalLog) {
        if (vup == null || finalLog == null) {
            return finalLog;
        }
        List<BusinessLog> finalDayLogs = businessLogMapper.findByVupIdBetweenDays(
                vup.getId(),
                finalLog.getDay(),
                finalLog.getDay()
        );
        for (int index = finalDayLogs.size() - 1; index >= 0; index--) {
            BusinessLog candidate = finalDayLogs.get(index);
            if (candidate.getAction() != null && MAIN_ACTION_REPLAY_POOL.contains(candidate.getAction())) {
                return candidate;
            }
        }
        return finalLog;
    }

    private String actionName(BusinessLog log) {
        return log == null ? null : log.getAction();
    }

    private List<BusinessLog> representativeLogs(List<BusinessLog> logs) {
        if (logs.size() <= 5) {
            return logs;
        }
        List<BusinessLog> chronological = logs.stream()
                .sorted(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId))
                .toList();
        List<BusinessLog> selected = new ArrayList<>();
        addUniqueLog(selected, chronological.get(0));
        addUniqueLog(selected, chronological.get(chronological.size() - 1));
        chronological.stream()
                .sorted(Comparator.comparingInt(this::keyEventScore).reversed()
                        .thenComparing(Comparator.comparingInt(BusinessLog::getDay).reversed())
                        .thenComparing(Comparator.comparing(BusinessLog::getId).reversed()))
                .forEach(log -> addUniqueLog(selected, log));
        while (selected.size() > 5) {
            selected.remove(replaceableKeyEventIndex(selected));
        }
        selected.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId));
        return selected;
    }

    private void addUniqueLog(List<BusinessLog> selected, BusinessLog log) {
        if (log == null) {
            return;
        }
        boolean exists = selected.stream()
                .anyMatch(item -> item.getId() != null && item.getId().equals(log.getId()));
        if (!exists) {
            selected.add(log);
        }
    }

    private int keyEventScore(BusinessLog log) {
        int score = 0;
        if (log.getDay() >= 24) {
            score += 8;
        }
        if (isCoreRouteAction(log)) {
            score += 14;
        }
        if ("EVENT_CHOICE".equals(log.getAction()) || log.getEventId() != null) {
            score += highImpactEvent(log) ? 9 : 2;
        }
        if (log.getRiskToolId() != null && !log.getRiskToolId().isBlank()) {
            score += 9;
        }
        if (log.getFanGroupTopicId() != null && !log.getFanGroupTopicId().isBlank()) {
            score += 7;
        }
        if (hasJsonRefs(log.getDebtIds())) {
            score += 10;
        }
        if (hasJsonRefs(log.getAccidentMaterialIds())) {
            score += 10;
        }
        if (hasMeaningfulRouteScore(log)) {
            score += 6;
        }
        if (Math.abs(log.getFanChange()) >= 40 || Math.abs(log.getPopularityChange()) >= 80) {
            score += 5;
        }
        if (log.getResult() != null && (log.getResult().contains("轮换经营")
                || log.getResult().contains("观众期待落差")
                || log.getResult().contains("严重度较高"))) {
            score += 4;
        }
        return score;
    }

    private boolean isCoreRouteAction(BusinessLog log) {
        return switch (log.getAction()) {
            case "TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK", "STREAM_PLAN", "PUBLISH_VIDEO",
                    "PUBLISH_CLIP", "FAN_GROUP_MAINTAIN", "NPC_INTERACT" -> hasMeaningfulRouteScore(log);
            default -> false;
        };
    }

    private boolean highImpactEvent(BusinessLog log) {
        return hasJsonRefs(log.getDebtIds())
                || hasJsonRefs(log.getAccidentMaterialIds())
                || Math.abs(log.getFanChange()) >= 30
                || Math.abs(log.getPopularityChange()) >= 60
                || Math.abs(log.getWatchHeatChange()) >= 8
                || Math.abs(log.getReputationChange()) >= 4;
    }

    private int replaceableKeyEventIndex(List<BusinessLog> selected) {
        int earliestDay = selected.stream().mapToInt(BusinessLog::getDay).min().orElse(0);
        int latestDay = selected.stream().mapToInt(BusinessLog::getDay).max().orElse(0);
        int lowestIndex = 0;
        int lowestScore = Integer.MAX_VALUE;
        for (int index = selected.size() - 1; index >= 0; index--) {
            BusinessLog log = selected.get(index);
            if (log.getDay() == earliestDay || log.getDay() == latestDay) {
                continue;
            }
            // EVENT_CHOICE entries are never replaceable - they represent player decisions
            if ("EVENT_CHOICE".equals(log.getAction())) {
                continue;
            }
            int score = keyEventScore(log);
            if (score < lowestScore) {
                lowestScore = score;
                lowestIndex = index;
            }
        }
        if (lowestScore < Integer.MAX_VALUE) {
            return lowestIndex;
        }
        return Math.max(0, selected.size() - 2);
    }

    private boolean hasJsonRefs(String json) {
        return json != null && !json.isBlank() && !"[]".equals(json) && !"{}".equals(json);
    }

    private boolean hasMeaningfulRouteScore(BusinessLog log) {
        if (log.getRouteScoreChange() == null || log.getRouteScoreChange().isBlank() || "{}".equals(log.getRouteScoreChange())) {
            return false;
        }
        return detailMap(log.getRouteScoreChange()).values().stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .anyMatch(number -> number.intValue() != 0);
    }

    private String keyEventReason(BusinessLog log) {
        if ("EVENT_CHOICE".equals(log.getAction()) || log.getEventId() != null) {
            return "正式事件";
        }
        if (hasJsonRefs(log.getDebtIds())) {
            return "旧账回流";
        }
        if (hasJsonRefs(log.getAccidentMaterialIds())) {
            return "事故素材";
        }
        if (log.getRiskToolId() != null && !log.getRiskToolId().isBlank()) {
            return "米线处理";
        }
        if (log.getFanGroupTopicId() != null && !log.getFanGroupTopicId().isBlank()) {
            return "粉丝群议题";
        }
        if (hasMeaningfulRouteScore(log)) {
            return "路线证据";
        }
        if (log.getDay() >= 24) {
            return "收官冲刺";
        }
        return "代表行动";
    }

    private Map<String, Object> toKeyEvent(BusinessLog log) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("type", "business_log");
        event.put("id", log.getId());
        event.put("day", log.getDay());
        event.put("action", log.getAction());
        event.put("summary", log.getResult());
        event.put("spotlightScore", keyEventScore(log));
        event.put("spotlightReason", keyEventReason(log));
        if (log.getPlanId() != null) {
            event.put("planId", log.getPlanId());
        }
        if (log.getTitleTemplateId() != null) {
            event.put("titleTemplateId", log.getTitleTemplateId());
        }
        if (log.getInteractionEventId() != null) {
            event.put("interactionEventId", log.getInteractionEventId());
        }
        if (log.getEventId() != null) {
            event.put("eventId", log.getEventId());
        }
        if (log.getRiskToolId() != null) {
            event.put("riskToolId", log.getRiskToolId());
        }
        if (log.getFanGroupTopicId() != null) {
            event.put("fanGroupTopicId", log.getFanGroupTopicId());
        }
        event.put("rngDetail", replayRngDetail(log));
        event.put("weightDetail", replayWeightDetail(log));
        event.put("multiplierDetail", detailMap(log.getMultiplierDetail()));
        event.put("capDetail", detailMap(log.getCapDetail()));
        event.put("clampDetail", detailMap(log.getClampDetail()));
        event.put("routeScoreChange", replayRouteScoreChange(log));
        List<Map<String, Object>> accidentMaterials = accidentMaterialRefs(log);
        if (!accidentMaterials.isEmpty()) {
            event.put("accidentMaterials", accidentMaterials);
        }
        return event;
    }

    private Map<String, Object> replayRngDetail(BusinessLog log) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>(detailMap(log.getRngDetail()));
        detail.putIfAbsent("hit", firstPresent(
                detail.get("hitTitleId"),
                detail.get("hitDebtId"),
                detail.get("hitEventId"),
                detail.get("hitDebtType"),
                log.getAction()
        ));
        return detail;
    }

    private Map<String, Object> replayWeightDetail(BusinessLog log) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>(detailMap(log.getWeightDetail()));
        detail.putIfAbsent("candidatePool", MAIN_ACTION_REPLAY_POOL);
        detail.putIfAbsent("hit", firstPresent(
                detail.get("hit"),
                detail.get("action"),
                detail.get("hitTitleId"),
                detail.get("hitDebtId"),
                detail.get("targetDebtId"),
                log.getAction()
        ));
        return detail;
    }

    private Map<String, Object> replayRouteScoreChange(BusinessLog log) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>(detailMap(log.getRouteScoreChange()));
        if ("STREAM_PLAN".equals(log.getAction()) && "title:SAFE".equals(detail.get("source"))) {
            detail.put("source", "TRAIN_TALK");
        }
        return detail;
    }

    private List<Map<String, Object>> accidentMaterials(Vup vup) {
        return businessLogMapper.findEndingReferenceLogs(vup.getId()).stream()
                .flatMap(log -> accidentMaterialRefs(log).stream())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                item -> item.get("materialId") + ":" + item.get("sourceAction"),
                                item -> item,
                                (first, ignored) -> first,
                                java.util.LinkedHashMap::new
                        ),
                        map -> map.values().stream().limit(8).toList()
                ));
    }

    private List<Map<String, Object>> accidentMaterialRefs(BusinessLog log) {
        if (log.getAccidentMaterialIds() == null
                || log.getAccidentMaterialIds().isBlank()
                || "[]".equals(log.getAccidentMaterialIds())) {
            return List.of();
        }
        return jsonService.readStringList(log.getAccidentMaterialIds()).stream()
                .filter(materialId -> materialId != null && !materialId.isBlank())
                .distinct()
                .map(materialId -> {
                    Map<String, Object> ref = new java.util.LinkedHashMap<>();
                    ref.put("type", "accident_material");
                    ref.put("materialId", materialId);
                    ref.put("label", accidentMaterialLabel(materialId));
                    ref.put("day", log.getDay());
                    ref.put("businessLogId", log.getId());
                    ref.put("sourceAction", log.getAction());
                    if (log.getEventId() != null) {
                        ref.put("sourceType", "risk_debt");
                        ref.put("eventId", log.getEventId());
                    } else if (log.getRiskToolId() != null) {
                        ref.put("sourceType", "risk_tool");
                        ref.put("riskToolId", log.getRiskToolId());
                    } else if (log.getInteractionEventId() != null) {
                        ref.put("sourceType", "live_interaction");
                        ref.put("interactionEventId", log.getInteractionEventId());
                    } else {
                        ref.put("sourceType", "business_log");
                    }
                    return ref;
                })
                .toList();
    }

    private String accidentMaterialLabel(String materialId) {
        return switch (materialId) {
            case "BOOMERANG_CLIP_CONTEXT" -> "回旋镖切片上下文";
            case "UNICORN_EXPECTATION_SCREENSHOT" -> "独角兽期待截图";
            case "COMMERCIAL_BACKLASH_RECEIPT" -> "商业反噬留档";
            case "TITLE_BACKFIRE_CONTEXT" -> "标题党反噬上下文";
            default -> "未署名事故素材";
        };
    }

    private List<Map<String, Object>> routeEvidenceTrail(Vup vup) {
        return businessLogMapper.findEndingReferenceLogs(vup.getId()).stream()
                .map(this::routeEvidenceFromLog)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private int advancedEvidenceCount(List<BusinessLog> logs, String key) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.stream()
                .map(log -> detailMap(log.getMultiplierDetail()))
                .filter(detail -> detail.containsKey(key))
                .count();
    }

    private int riskRecoveryEvidenceCount(List<BusinessLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        return (int) logs.stream()
                .filter(log -> log.getRiskToolId() != null && !log.getRiskToolId().isBlank()
                        || detailMap(log.getMultiplierDetail()).containsKey("defenseMitigation"))
                .count();
    }

    private Map<String, Object> endingScorecard(
            Vup vup,
            String endingType,
            List<RiskDebt> openDebts,
            List<BusinessLog> evidenceLogs,
            EndingRuleEngine.EndingResult endingResult
    ) {
        int routeScore = currentRouteScore(vup);
        int totalRouteScore = totalPositiveRouteScore(vup);
        int routeFocusScore = totalRouteScore <= 0 ? 0 : clamp(routeScore * 100 / Math.max(1, totalRouteScore), 0, 100);
        routeFocusScore = clamp((routeFocusScore + Math.min(100, routeScore * 5)) / 2, 0, 100);

        int routeEvidenceCount = (int) (evidenceLogs == null ? 0 : evidenceLogs.stream()
                .filter(this::hasMeaningfulRouteScore)
                .count());
        int representativeCount = evidenceLogs == null ? 0 : representativeLogs(evidenceLogs).size();
        int matchedRuleCount = endingResult.matchedRules() == null ? 0 : endingResult.matchedRules().size();
        int comboEvidenceCount = advancedEvidenceCount(evidenceLogs, "comboImpact");
        int stageObjectiveEvidenceCount = advancedEvidenceCount(evidenceLogs, "stageObjectiveBonus");
        int riskRecoveryCount = riskRecoveryEvidenceCount(evidenceLogs);
        int fatiguePenaltyCount = advancedEvidenceCount(evidenceLogs, "fatigueInfo");
        int advancedEvidenceScore = comboEvidenceCount * 8 + stageObjectiveEvidenceCount * 7 + riskRecoveryCount * 6;
        int evidenceScore = clamp(routeEvidenceCount * 8 + representativeCount * 8 + matchedRuleCount * 10
                + advancedEvidenceScore - fatiguePenaltyCount * 4, 0, 100);

        int openDebtCount = openDebts == null ? 0 : openDebts.size();
        int severeDebtCount = openDebts == null ? 0 : (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 4).count();
        int heatPressure = "BLACK_RED_MAIN_STAGE".equals(endingType) || "MAIN_STAGE_KING".equals(endingType)
                ? Math.max(0, vup.getWatchHeat() - 75) / 2
                : Math.max(0, vup.getWatchHeat() - 45);
        int riskPenalty = openDebtCount * 12 + severeDebtCount * 12 + heatPressure
                + Math.max(0, 55 - vup.getReputation())
                + fatiguePenaltyCount * 3
                - riskRecoveryCount * 4;
        int riskControlScore = clamp(100 - riskPenalty, 0, 100);

        int fanQualityScore = clamp(fanRatio(vup, vup.getTrueFans()) + fanRatio(vup, vup.getDdFans()) / 2
                + Math.min(30, vup.getReputation() / 3), 0, 100);
        int overall = clamp(routeFocusScore * 35 / 100
                + evidenceScore * 25 / 100
                + riskControlScore * 25 / 100
                + fanQualityScore * 15 / 100, 0, 100);

        Map<String, Object> scorecard = new java.util.LinkedHashMap<>();
        scorecard.put("overall", overall);
        scorecard.put("grade", endingGrade(overall));
        scorecard.put("gradeLabel", endingGradeLabel(overall));
        scorecard.put("routeFocusScore", routeFocusScore);
        scorecard.put("evidenceScore", evidenceScore);
        scorecard.put("riskControlScore", riskControlScore);
        scorecard.put("fanQualityScore", fanQualityScore);
        scorecard.put("routeScore", routeScore);
        scorecard.put("routeEvidenceCount", routeEvidenceCount);
        scorecard.put("comboEvidenceCount", comboEvidenceCount);
        scorecard.put("stageObjectiveEvidenceCount", stageObjectiveEvidenceCount);
        scorecard.put("riskRecoveryCount", riskRecoveryCount);
        scorecard.put("fatiguePenaltyCount", fatiguePenaltyCount);
        scorecard.put("advancedEvidenceScore", advancedEvidenceScore);
        scorecard.put("openDebtCount", openDebtCount);
        scorecard.put("severeDebtCount", severeDebtCount);
        scorecard.put("nextChallenge", endingNextChallenge(endingType, overall, routeFocusScore, evidenceScore, riskControlScore));
        scorecard.put("nextRunAdvice", nextRunAdvice(endingType));
        scorecard.put("nextRunActions", nextRunActions(endingType));
        scorecard.put("scoreLine", endingScoreLine(overall, routeFocusScore, evidenceScore, riskControlScore));
        return scorecard;
    }

    private int currentRouteScore(Vup vup) {
        Map<String, Object> scoreMap = jsonService.readMap(vup.getRouteScoreJson());
        Object rawScore = scoreMap.get(vup.getCurrentRoute());
        return rawScore instanceof Number number ? number.intValue() : 0;
    }

    private int totalPositiveRouteScore(Vup vup) {
        return jsonService.readMap(vup.getRouteScoreJson()).values().stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToInt(number -> Math.max(0, number.intValue()))
                .sum();
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

    private String endingGradeLabel(int score) {
        if (score >= 90) {
            return "精品证据链";
        }
        if (score >= 78) {
            return "稳定好局";
        }
        if (score >= 62) {
            return "成型但有缺口";
        }
        if (score >= 45) {
            return "能收官但不干净";
        }
        return "复活赛重打";
    }

    private String endingScoreLine(int overall, int routeFocus, int evidenceScore, int riskControl) {
        return "本轮评分 " + overall + "，路线专注" + routeFocus
                + "，证据链" + evidenceScore
                + "，风险控制" + riskControl + "。";
    }

    private String endingNextChallenge(
            String endingType,
            int overall,
            int routeFocus,
            int evidenceScore,
            int riskControl
    ) {
        if (overall >= 90) {
            return "下一局挑战同路线无严重旧账收官，或者换一条基础路线补图鉴。";
        }
        if (riskControl < 55) {
            return "下一局先把旧账和围观热度压住，再从「"
                    + baseRouteLabelForEnding(endingType, "") + "」冲「" + finalTitle(endingType) + "」。";
        }
        if (routeFocus < 65) {
            return "下一局减少路线摇摆，前两周连续补「"
                    + baseRouteLabelForEnding(endingType, "") + "」证据。";
        }
        if (evidenceScore < 65) {
            return "下一局多留代表事件：" + String.join("、", nextRunActions(endingType)) + "都要能进复盘。";
        }
        return nextRunAdvice(endingType);
    }

    private Map<String, Object> routeEvidenceFromLog(BusinessLog log) {
        if (log.getRouteScoreChange() == null || log.getRouteScoreChange().isBlank()) {
            return Map.of();
        }
        Map<String, Object> scoreChange = jsonService.readMap(log.getRouteScoreChange());
        String routeType = scoreChange.entrySet().stream()
                .filter(entry -> !"source".equals(entry.getKey()))
                .filter(entry -> entry.getValue() instanceof Number number && number.intValue() > 0)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (routeType == null) {
            return Map.of();
        }
        Object rawDelta = scoreChange.get(routeType);
        int scoreDelta = rawDelta instanceof Number number ? number.intValue() : 0;
        Map<String, Object> evidence = new java.util.LinkedHashMap<>();
        evidence.put("type", "business_log");
        evidence.put("id", log.getId());
        evidence.put("day", log.getDay());
        evidence.put("action", log.getAction());
        if (log.getInteractionEventId() != null) {
            evidence.put("interactionEventId", log.getInteractionEventId());
        }
        if (log.getEventId() != null) {
            evidence.put("eventId", log.getEventId());
        }
        evidence.put("routeType", routeType);
        evidence.put("scoreDelta", scoreDelta);
        if (scoreChange.containsKey("source")) {
            evidence.put("source", scoreChange.get("source"));
        }
        return evidence;
    }

    private Map<String, Object> detailMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(json);
    }

    private Object firstPresent(Object... values) {
        for (Object value : values) {
            if (value instanceof String text && text.isBlank()) {
                continue;
            }
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<Map<String, Object>> debtRefs(Vup vup) {
        return riskDebtMapper.findEndingRefsByVupId(vup.getId()).stream()
                .limit(5)
                .map(this::toDebtRef)
                .toList();
    }

    private Map<String, Object> toDebtRef(RiskDebt debt) {
        return Map.of(
                "type", "risk_debt",
                "id", debt.getId(),
                "debtType", debt.getDebtType(),
                "status", debt.getStatus(),
                "severity", debt.getSeverity(),
                "createDay", debt.getCreateDay(),
                "dueDay", debt.getDueDay(),
                "sourceAction", debt.getSourceAction(),
                "sourceTitle", debt.getSourceTitle(),
                "summary", debt.getSummary()
        );
    }

    private String determineEndingType(Vup vup, BusinessLog finalLog) {
        if (vup.getFans() < 130 || RouteType.UNKNOWN.name().equals(vup.getCurrentRoute())) {
            return "UNKNOWN";
        }
        if (playerChoseGraduation(vup, finalLog)) {
            return "GLORIOUS_GRADUATION";
        }
        if (vup.getMemeLevel() > 80 && vup.getReputation() < 30 && hasOpenDebt(vup)) {
            return "MAIN_STAGE_KING";
        }
        if (unicornRatio(vup) > 50 && (vup.getCommercialLevel() >= 70 || hasFanServiceEvidence(vup))) {
            return "CYBER_GIRLFRIEND";
        }
        if (RouteType.SOCIAL_COLLAB.name().equals(vup.getCurrentRoute()) && ddRatio(vup) > 40) {
            return "DD_BUS_STOP";
        }
        if (RouteType.SINGING_IDOL.name().equals(vup.getCurrentRoute()) && vup.getSongPower() >= 11) {
            return "SINGING_IDOL";
        }
        if ((RouteType.SLICE_SAINT.name().equals(vup.getCurrentRoute())
                || RouteType.DANCE_MEME.name().equals(vup.getCurrentRoute()))
                && vup.getMemeLevel() >= 40) {
            return "SLICE_SAINT";
        }
        if (RouteType.BLACK_RED_MAIN_STAGE.name().equals(vup.getCurrentRoute())
                || (vup.getWatchHeat() >= 60 && vup.getReputation() < 45)) {
            return "BLACK_RED_MAIN_STAGE";
        }
        return "ELECTRONIC_PICKLE";
    }

    private int ddRatio(Vup vup) {
        if (vup.getFans() <= 0) {
            return 0;
        }
        return vup.getDdFans() * 100 / vup.getFans();
    }

    private int unicornRatio(Vup vup) {
        if (vup.getFans() <= 0) {
            return 0;
        }
        return vup.getUnicornFans() * 100 / vup.getFans();
    }

    private boolean hasOpenDebt(Vup vup) {
        return !riskDebtMapper.findOpenByVupId(vup.getId()).isEmpty();
    }

    private boolean hasFanServiceEvidence(Vup vup) {
        return businessLogMapper.findEndingReferenceLogs(vup.getId()).stream()
                .filter(log -> log.getResult() != null)
                .filter(log -> hasFanServiceCopy(log.getResult()))
                .count() >= 5;
    }

    private boolean playerChoseGraduation(Vup vup, BusinessLog finalLog) {
        return finalLog != null
                && "FAN_GROUP_MAINTAIN".equals(finalLog.getAction())
                && vup.getReputation() > 60
                && severeOpenDebtCount(vup) <= 1;
    }

    private long severeOpenDebtCount(Vup vup) {
        return riskDebtMapper.findOpenByVupId(vup.getId()).stream()
                .filter(debt -> debt.getSeverity() >= 4)
                .count();
    }

    private List<EndingTagDTO> endingTags(String endingType, Vup vup) {
        endingType = canonicalEndingType(endingType);
        List<BusinessLog> logs = businessLogMapper.findEndingReferenceLogs(vup.getId());
        long clipCount = logs.stream().filter(l -> "PUBLISH_CLIP".equals(l.getAction())).count();
        long songCount = logs.stream().filter(l -> "TRAIN_SONG".equals(l.getAction())).count();
        long npcCount = logs.stream().filter(l -> "NPC_INTERACT".equals(l.getAction())).count();
        long fanGroupCount = logs.stream().filter(l -> "FAN_GROUP_MAINTAIN".equals(l.getAction())).count();
        long debtCount = riskDebtMapper.findEndingRefsByVupId(vup.getId()).size();
        long scThanksCount = logs.stream()
                .filter(l -> l.getResult() != null && hasFanServiceCopy(l.getResult()))
                .count();
        long talkCount = logs.stream()
                .filter(l -> "TRAIN_TALK".equals(l.getAction())
                        || (l.getResult() != null && (l.getResult().contains("TALK") || l.getResult().contains("SING_TALK"))))
                .count();

        return switch (endingType) {
            case "SINGING_IDOL" -> evidenceTags(
                    songCount >= 5,
                    endingTag("SINGING_IDOL", "歌势遗珠", "本轮练歌多次进入结局证据"),
                    endingTag("OLD_FAN_THERMOS", "老粉保温杯", "真爱粉路线留下稳定陪伴证据")
            );
            case "SLICE_SAINT" -> evidenceTags(
                    clipCount >= 5,
                    endingTag("SLICE_SAINT", "切片圣体", "本轮发布切片多次出圈"),
                    endingTag("CLIP_SUPPLIER", "切片组供货商", "切片素材进入代表事件")
            );
            case "BLACK_RED_MAIN_STAGE" -> evidenceTags(
                    debtCount >= 1,
                    endingTag("FORUM_EMPLOYER", "楼友就业保障", "本轮欠账和标题党反噬证据进入结局复盘"),
                    endingTag("GAVEL_REGULAR", "法槌常驻嘉宾", "黑红路线和主会场证据进入结局复盘")
            );
            case "MAIN_STAGE_KING" -> evidenceTags(
                    debtCount >= 2,
                    endingTag("FORUM_EMPLOYER", "楼友就业保障", "多笔欠账把本轮推上主会场"),
                    endingTag("RECORDING_ANNUAL_CUSTOMER", "录播组包年客户", "回旋镖和事故素材被多次引用")
            );
            case "CYBER_GIRLFRIEND" -> evidenceTags(
                    scThanksCount >= 5,
                    endingTag("UNICORN_PRESSURE_COOKER", "高压锅营业", "高亮互动回应和独角兽期待证据多次出现"),
                    endingTag("BOSS_SCHEDULE_BOARD", "榜一排班表", "粉丝服务路线留下排班感")
            );
            case "GLORIOUS_GRADUATION" -> evidenceTags(
                    fanGroupCount >= 4 || talkCount >= 5,
                    endingTag("GRACEFUL_SIGNOFF", "体面下播", "粉丝群维护支撑光荣毕业"),
                    endingTag("REBIRTH_RESERVED", "复活赛预约中", "本轮收尾保留重开余地")
            );
            case "ELECTRONIC_PICKLE" -> evidenceTags(
                    vup.getReputation() >= 70 && vup.getWatchHeat() <= 20,
                    endingTag("LOW_PRESSURE_NO_BIG_HIT", "低压但没活", "高口碑、低围观热度支撑低压结局"),
                    talkCount >= 5
                            ? endingTag("BACKGROUND_AUDIO_WORKER", "背景音公务员", "TALK/SING_TALK稳健证据多次出现")
                            : endingTag("OLD_FAN_THERMOS", "老粉保温杯", "真爱粉路线留下稳定陪伴证据")
            );
            case "DD_BUS_STOP" -> List.of(
                    endingTag(
                            "DD_BUS_STATION",
                            "流动观众集散地",
                            npcCount >= 5 ? "同台互动多次进入代表事件" : "DD占比命中社交结局"
                    )
            );
            default -> List.of(
                    endingTag("QUIET_ENDING_PERSON", "安静下播人", "查无此V或低热度结局命中")
            );
        };
    }

    private List<EndingTagDTO> evidenceTags(boolean hasEvidence, EndingTagDTO primaryTag, EndingTagDTO fallbackTag) {
        if (hasEvidence) {
            return List.of(primaryTag, fallbackTag);
        }
        // T15：证据不足时降级结局。
        return List.of(fallbackTag);
    }

    private EndingTagDTO endingTag(String tagKey, String label, String evidence) {
        return new EndingTagDTO(tagKey, label, evidence);
    }

    /**
     * Phase 6: Pressure ending marks — permanent marks for this run only.
     * These appear in ending text but do NOT carry over to next run.
     *
     * - "主舞台体质" if player survived high pressure periods (was ever locked out)
     * - "边界裂痕" if player was locked out (boundary pushed too far)
     * - "内容空心化" if content_dry group was the pressure source
     */
    private List<EndingTagDTO> pressureEndingMarks(Vup vup) {
        List<EndingTagDTO> marks = new ArrayList<>();

        if (operatingPressureService.wasEverLocked(vup)) {
            marks.add(endingTag(
                    "MAIN_STAGE_PHYSIQUE",
                    "主舞台体质",
                    "运营脑曾经触发过动作组锁定，但主播撑过来了。高压期的经历磨出了主会场体质。"
            ));
            marks.add(endingTag(
                    "BOUNDARY_CRACK",
                    "边界裂痕",
                    "运营脑压力触发锁定时，边界线被磨薄。这些裂痕不会带到下一局，但会在结局点名。"
            ));
        }

        String sourceKey = operatingPressureService.lastSourceKey(vup);
        if ("CONTENT_DRY".equals(sourceKey)) {
            marks.add(endingTag(
                    "HOLLOW_CONTENT",
                    "内容空心化",
                    "内容枯竭组曾经被锁定，素材库存一度见底。空心化是创作过度的代价，下一局需要更注意素材补给。"
            ));
        }

        return marks;
    }

    private String finalTitle(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> "歌势遗珠";
            case "SLICE_SAINT" -> "切片圣体";
            case "BLACK_RED_MAIN_STAGE" -> "黑红顶流";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "CYBER_GIRLFRIEND" -> "赛博女友";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "DD_BUS_STOP" -> "DD公交站";
            default -> "查无此V";
        };
    }

    private EndingCopyDecision subtitleDecision(
            String endingType,
            Vup vup,
            BusinessLog finalLog,
            List<BusinessLog> evidenceLogs
    ) {
        List<ContentCatalogService.ContentEntry> subtitles = contentCatalogService
                .getEntries("ENDING_SUBTITLE")
                .getOrDefault(endingType, List.of());
        if (!subtitles.isEmpty()) {
            Map<String, Object> seedData = subtitleSeedData(endingType, vup, finalLog, evidenceLogs);
            List<ContentCatalogService.ContentEntry> candidatePool = subtitleCandidatePool(subtitles);
            seedData.put("catalogPoolSize", subtitles.size());
            seedData.put("candidatePoolSize", candidatePool.size());
            seedData.put("candidateSubKey", candidatePool.equals(subtitles) ? "all" : "signature");
            int seed = java.util.Objects.hash(seedData.values().toArray());
            int candidateIndex = Math.floorMod(seed, candidatePool.size());
            ContentCatalogService.ContentEntry selected = candidatePool.get(candidateIndex);
            int catalogIndex = subtitles.indexOf(selected);
            return new EndingCopyDecision(
                    selected.text(),
                    candidateIndex,
                    candidatePool.size(),
                    catalogIndex,
                    subtitles.size(),
                    selected.subKey(),
                    seedData
            );
        }
        return new EndingCopyDecision(fallbackSubtitle(endingType), 0, 1, 0, 1, "fallback",
                subtitleSeedData(endingType, vup, finalLog, evidenceLogs));
    }

    private List<ContentCatalogService.ContentEntry> subtitleCandidatePool(
            List<ContentCatalogService.ContentEntry> subtitles
    ) {
        List<ContentCatalogService.ContentEntry> signaturePool = subtitles.stream()
                .filter(entry -> "signature".equals(entry.subKey()))
                .toList();
        return signaturePool.isEmpty() ? subtitles : signaturePool;
    }

    private Map<String, Object> subtitleSeedData(
            String endingType,
            Vup vup,
            BusinessLog finalLog,
            List<BusinessLog> evidenceLogs
    ) {
        Map<String, Object> seedData = new java.util.LinkedHashMap<>();
        List<BusinessLog> representative = evidenceLogs == null ? List.of() : representativeLogs(evidenceLogs);
        seedData.put("endingType", endingType);
        seedData.put("currentRoute", vup.getCurrentRoute());
        seedData.put("fans", vup.getFans());
        seedData.put("trueFans", vup.getTrueFans());
        seedData.put("funFans", vup.getFunFans());
        seedData.put("unicornFans", vup.getUnicornFans());
        seedData.put("ddFans", vup.getDdFans());
        seedData.put("watchHeat", vup.getWatchHeat());
        seedData.put("reputation", vup.getReputation());
        seedData.put("memeLevel", vup.getMemeLevel());
        seedData.put("commercialLevel", vup.getCommercialLevel());
        seedData.put("finalAction", finalLog == null ? "" : finalLog.getAction());
        seedData.put("evidenceCount", evidenceLogs == null ? 0 : evidenceLogs.size());
        seedData.put("representativeActions", representative.stream().map(BusinessLog::getAction).toList());
        seedData.put("representativeDays", representative.stream().map(BusinessLog::getDay).toList());
        seedData.put("representativeResultSignals", representative.stream()
                .map(log -> compactSeedText(log.getResult()))
                .toList());
        return seedData;
    }

    private String compactSeedText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", "");
        return normalized.length() <= 24 ? normalized : normalized.substring(0, 24);
    }

    private String fallbackSubtitle(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> "高音可能没全上去，但老粉听见了基本功。";
            case "SLICE_SAINT" -> "你不是每天都在直播，但每天都在被剪。";
            case "BLACK_RED_MAIN_STAGE" -> "热度很响，楼友忙着把证据排成队。";
            case "MAIN_STAGE_KING" -> "录播组的硬盘，比你的排班表更满。";
            case "CYBER_GIRLFRIEND" -> "陪伴感太成功，成功到大家开始要排班表。";
            case "GLORIOUS_GRADUATION" -> "这次没有被抬走，是自己走下舞台。";
            case "ELECTRONIC_PICKLE" -> "不炸场，不开庭，稳定陪饭。";
            case "DD_BUS_STOP" -> "你不是在联动，就是在去联动的路上。";
            default -> "互联网没有讨厌你，它只是没怎么记住你。";
        };
    }

    private String endingReason(String endingType, Vup vup, BusinessLog finalLog) {
        return "命中 " + finalTitle(endingType) + "：第30天代表行动为 " + actionLabel(finalLog == null ? null : finalLog.getAction())
                + "，当前路线 " + routeLabel(vup.getCurrentRoute())
                + "，累计路线代表行动 " + routeEvidenceAction(endingType, vup.getCurrentRoute())
                + "，总粉丝 " + vup.getFans()
                + "，DD占比 " + ddRatio(vup) + "%"
                + "，围观热度 " + vup.getWatchHeat()
                + "。最终参考第30天表现。";
    }

    private String endingReason(String endingType, Vup vup, BusinessLog finalLog, EndingCopyDecision copyDecision) {
        return endingReason(endingType, vup, finalLog);
    }

    private String endingReason(
            String endingType,
            Vup vup,
            BusinessLog finalLog,
            EndingCopyDecision copyDecision,
            EndingRuleEngine.EndingResult endingResult
    ) {
        if ("UNKNOWN".equals(endingType)) {
            return unknownEndingReason(vup, finalLog, endingResult);
        }
        return endingReason(endingType, vup, finalLog, copyDecision);
    }

    private String unknownEndingReason(Vup vup, BusinessLog finalLog, EndingRuleEngine.EndingResult endingResult) {
        Object rawReason = endingResult.conditions().get("unknownFailureReason");
        String failureReason = rawReason instanceof String text && !text.isBlank()
                ? text
                : "出圈证据不足，结局组没有足够素材归档路线。";
        return "命中 查无此V：" + failureReason
                + " 第30天代表行动为 " + actionLabel(finalLog == null ? null : finalLog.getAction())
                + "，当前路线 " + routeLabel(vup.getCurrentRoute())
                + "，总粉丝 " + vup.getFans()
                + "，围观热度 " + vup.getWatchHeat()
                + "。这不是爆炸失败，而是观众翻完30天也找不到一句能安利你的话。下一局先连续7天押一条基础路线，再补3条能进复盘的代表事件。";
    }

    private String endingReasonJson(
            String endingType,
            Vup vup,
            BusinessLog finalLog,
            EndingCopyDecision copyDecision,
            List<BusinessLog> evidenceLogs,
            EndingRuleEngine.EndingResult endingResult
    ) {
        Map<String, Object> reasonData = new java.util.LinkedHashMap<>();
        Map<String, Object> conditions = new java.util.LinkedHashMap<>(endingResult.conditions());
        conditions.put("fansThreshold", 130);
        reasonData.put("endingType", endingType);
        reasonData.put("rule", endingResult.rule());
        reasonData.put("matchedRules", endingResult.matchedRules());
        reasonData.put("conditions", conditions);
        reasonData.put("routeCandidate", routeCandidate(vup, endingType));
        reasonData.put("routeEndingRelation", routeEndingRelation(endingType, vup));
        reasonData.put("evidenceIds", evidenceIds(finalLog, evidenceLogs));
        reasonData.put("evidenceRefs", endingEvidenceRefs(finalLog));
        reasonData.put("keyMetricSnapshot", keyMetricSnapshot(vup));
        reasonData.put("finalDayAction", finalLog.getAction());
        reasonData.put("routeEvidence", routeEvidenceAction(endingType, vup.getCurrentRoute()));
        reasonData.put("nextRunAdvice", nextRunAdvice(endingType));
        reasonData.put("nextRunActions", nextRunActions(endingType));
        Map<String, Object> subtitleSource = new java.util.LinkedHashMap<>();
        subtitleSource.put("category", "ENDING_SUBTITLE");
        subtitleSource.put("endingType", endingType);
        subtitleSource.put("candidateIndex", copyDecision.candidateIndex());
        subtitleSource.put("candidatePoolSize", copyDecision.candidatePoolSize());
        subtitleSource.put("catalogIndex", copyDecision.catalogIndex());
        subtitleSource.put("catalogPoolSize", copyDecision.catalogPoolSize());
        subtitleSource.put("subKey", copyDecision.subKey());
        subtitleSource.put("seed", copyDecision.seedData());
        subtitleSource.put("text", copyDecision.subtitle());
        reasonData.put("subtitleSource", subtitleSource);
        return jsonService.write(reasonData);
    }

    private List<String> evidenceIds(BusinessLog finalLog, List<BusinessLog> evidenceLogs) {
        List<String> ids = new ArrayList<>();
        if (evidenceLogs != null) {
            for (BusinessLog log : representativeLogs(evidenceLogs)) {
                ids.add("business_log#" + log.getId());
            }
        }
        if (ids.isEmpty() && finalLog != null) {
            ids.add("business_log#" + finalLog.getId());
        }
        return ids;
    }

    private Map<String, Object> routeCandidate(Vup vup, String endingType) {
        Map<String, Object> scoreMap = jsonService.readMap(vup.getRouteScoreJson());
        Object rawScore = scoreMap.get(vup.getCurrentRoute());
        int score = rawScore instanceof Number number ? number.intValue() : 0;
        Map<String, Object> candidate = new java.util.LinkedHashMap<>();
        candidate.put("routeType", vup.getCurrentRoute());
        candidate.put("baseRouteType", baseRouteTypeForEnding(endingType, vup.getCurrentRoute()));
        candidate.put("baseRouteLabel", baseRouteLabelForEnding(endingType, vup.getCurrentRoute()));
        candidate.put("derivedEndingType", endingType);
        candidate.put("derivedEndingLabel", finalTitle(endingType));
        candidate.put("score", score);
        candidate.put("evidenceAction", routeEvidenceAction(endingType, vup.getCurrentRoute()));
        return candidate;
    }

    private Map<String, Object> keyMetricSnapshot(Vup vup) {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("fans", vup.getFans());
        snapshot.put("trueFans", vup.getTrueFans());
        snapshot.put("funFans", vup.getFunFans());
        snapshot.put("unicornFans", vup.getUnicornFans());
        snapshot.put("ddFans", vup.getDdFans());
        snapshot.put("trueFanRatio", fanRatio(vup, vup.getTrueFans()));
        snapshot.put("funFanRatio", fanRatio(vup, vup.getFunFans()));
        snapshot.put("ddRatio", ddRatio(vup));
        snapshot.put("unicornRatio", unicornRatio(vup));
        snapshot.put("watchHeat", vup.getWatchHeat());
        snapshot.put("reputation", vup.getReputation());
        snapshot.put("memeLevel", vup.getMemeLevel());
        snapshot.put("commercialLevel", vup.getCommercialLevel());
        snapshot.put("openDebtCount", riskDebtMapper.findOpenByVupId(vup.getId()).size());
        snapshot.put("severeOpenDebtCount", severeOpenDebtCount(vup));
        return snapshot;
    }

    private int fanRatio(Vup vup, int fanCount) {
        if (vup.getFans() <= 0) {
            return 0;
        }
        return fanCount * 100 / vup.getFans();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, Object> routeEndingRelation(String endingType, Vup vup) {
        String canonicalEndingType = canonicalEndingType(endingType);
        String baseRouteType = baseRouteTypeForEnding(canonicalEndingType, vup.getCurrentRoute());
        Map<String, Object> relation = new java.util.LinkedHashMap<>();
        relation.put("baseRouteType", baseRouteType);
        relation.put("baseRouteLabel", baseRouteLabel(baseRouteType));
        relation.put("currentRouteType", vup.getCurrentRoute());
        relation.put("currentRouteLabel", routeLabel(vup.getCurrentRoute()));
        relation.put("derivedEndingType", canonicalEndingType);
        relation.put("derivedEndingLabel", finalTitle(canonicalEndingType));
        relation.put("derivation", derivationLine(canonicalEndingType));
        relation.put("nextRunAdvice", nextRunAdvice(canonicalEndingType));
        relation.put("nextRunActions", nextRunActions(canonicalEndingType));
        return relation;
    }

    private String canonicalEndingType(String endingType) {
        if (endingType == null || endingType.isBlank()) {
            return "UNKNOWN";
        }
        return switch (endingType.trim().toUpperCase()) {
            case "LEGEND" -> "MAIN_STAGE_KING";
            case "GRADUATION" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE" -> "CYBER_GIRLFRIEND";
            case "DANCE_MEME" -> "SLICE_SAINT";
            case "SOCIAL_COLLAB" -> "DD_BUS_STOP";
            default -> endingType.trim().toUpperCase();
        };
    }

    private String baseRouteTypeForEnding(String endingType, String currentRoute) {
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> RouteType.SINGING_IDOL.name();
            case "SLICE_SAINT" -> RouteType.DANCE_MEME.name().equals(currentRoute)
                    ? RouteType.DANCE_MEME.name()
                    : RouteType.SLICE_SAINT.name();
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> RouteType.BLACK_RED_MAIN_STAGE.name();
            case "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> RouteType.ELECTRONIC_PICKLE.name();
            case "DD_BUS_STOP" -> RouteType.SOCIAL_COLLAB.name();
            default -> RouteType.UNKNOWN.name();
        };
    }

    private String baseRouteLabelForEnding(String endingType, String currentRoute) {
        return baseRouteLabel(baseRouteTypeForEnding(endingType, currentRoute));
    }

    private String baseRouteLabel(String baseRouteType) {
        return switch (baseRouteType) {
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "SINGING_IDOL" -> "唱歌偶像";
            case "SLICE_SAINT" -> "切片圣体";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "DANCE_MEME" -> "梗舞整活";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场";
            default -> "未定型";
        };
    }

    private String derivationLine(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "ELECTRONIC_PICKLE" -> "稳健基础路线在口碑和低压证据达标时，派生为稳定陪饭命运。";
            case "GLORIOUS_GRADUATION" -> "稳健基础路线在高口碑、低热度、无高危旧账时，派生为体面收束。";
            case "SINGING_IDOL" -> "唱歌偶像基础路线在歌力、作品证据和真爱粉支撑下，派生为歌势遗珠。";
            case "SLICE_SAINT" -> "切片或梗舞基础路线在素材链和梗浓度足够时，派生为切片圣体。";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场基础路线在高围观、低口碑和旧账证据并存时，派生为黑红顶流。";
            case "MAIN_STAGE_KING" -> "黑红主会场基础路线在热度、梗浓度和控场证据都更高时，派生为主会场之王。";
            case "CYBER_GIRLFRIEND" -> "稳健陪伴基础路线在独角兽浓度、商业化和粉丝服务证据成型时，派生为赛博女友。";
            case "DD_BUS_STOP" -> "社交联动基础路线在DD占比和同台证据达标时，派生为DD公交站。";
            default -> "未定型路线因粉丝基本盘或证据链不足，归档为查无此V。";
        };
    }

    private List<String> nextRunActions(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "ELECTRONIC_PICKLE" -> List.of("杂谈复盘", "粉丝群维护", "低压直播企划");
            case "GLORIOUS_GRADUATION" -> List.of("粉丝群维护", "杂谈复盘", "休息降温");
            case "SINGING_IDOL" -> List.of("练歌", "直播企划", "发布视频");
            case "SLICE_SAINT" -> List.of("发布视频", "发布切片", "练舞");
            case "BLACK_RED_MAIN_STAGE" -> List.of("直播企划", "发布切片", "粉丝群维护");
            case "MAIN_STAGE_KING" -> List.of("直播企划", "发布切片", "米线工具/粉丝群维护");
            case "CYBER_GIRLFRIEND" -> List.of("杂谈复盘", "粉丝群维护", "边界处理");
            case "DD_BUS_STOP" -> List.of("同台互动", "粉丝群维护", "直播企划");
            default -> List.of("选定基础路线", "连续同路行动", "补可引用证据");
        };
    }

    private String nextRunAdvice(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "ELECTRONIC_PICKLE" -> "下一局前7天连续做杂谈复盘或粉丝群维护，中段补固定饭点记忆点，最后少冲热搜。";
            case "GLORIOUS_GRADUATION" -> "下一局先走稳健基础路线，把口碑和低压证据做厚；最后一周清高危旧账，用粉丝群维护收尾。";
            case "SINGING_IDOL" -> "下一局前7天练歌定调，中段至少留一次歌回或投稿，避免只练不展示。";
            case "SLICE_SAINT" -> "下一局先发视频补素材，再用切片或练舞连续供货；最后一周换新梗，防同梗回旋。";
            case "BLACK_RED_MAIN_STAGE" -> "下一局先走黑红主会场基础路线，直播企划和切片接热度；每次爆点后用粉丝群维护拆旧账。";
            case "MAIN_STAGE_KING" -> "下一局仍从黑红主会场起步，但要同时堆热度、梗浓度和控场证据，高危旧账先处理再冲刺。";
            case "CYBER_GIRLFRIEND" -> "下一局先用低压陪伴和粉丝群维护起盘，中段补高亮互动回应/陪伴营业证据，并持续处理边界。";
            case "DD_BUS_STOP" -> "下一局前7天多做同台互动接车流，中段用粉丝群维护安抚老粉，最后看DD占比和留存。";
            default -> "下一局前7天请选一条基础路线连续行动，至少留下3条能被复盘引用的证据。";
        };
    }

    private List<Map<String, Object>> endingEvidenceRefs(BusinessLog finalLog) {
        return List.of(Map.of(
                "type", "business_log",
                "id", finalLog.getId(),
                "day", finalLog.getDay(),
                "action", finalLog.getAction()
        ));
    }

    private String routeEvidenceAction(String endingType, String routeType) {
        endingType = canonicalEndingType(endingType);
        if ("CYBER_GIRLFRIEND".equals(endingType)) {
            return "醒目留言陪伴回/陪伴营业";
        }
        if ("GLORIOUS_GRADUATION".equals(endingType)) {
            return "粉丝群维护";
        }
        if ("SLICE_SAINT".equals(endingType)) {
            return "发布切片";
        }
        if ("DANCE_MEME".equals(endingType)) {
            return "练舞";
        }
        if ("SOCIAL_COLLAB".equals(endingType) || "DD_BUS_STOP".equals(endingType)) {
            return "同台互动";
        }
        if ("SINGING_IDOL".equals(endingType)) {
            return "练歌";
        }
        if (RouteType.SLICE_SAINT.name().equals(routeType)) {
            return "发布切片";
        }
        if (RouteType.DANCE_MEME.name().equals(routeType)) {
            return "练舞";
        }
        if (RouteType.SOCIAL_COLLAB.name().equals(routeType)) {
            return "同台互动";
        }
        if (RouteType.ELECTRONIC_PICKLE.name().equals(routeType)) {
            return "低压陪伴/杂谈复盘";
        }
        if (RouteType.BLACK_RED_MAIN_STAGE.name().equals(routeType)) {
            return "硬嘴标题/抽象企划";
        }
        if (RouteType.SINGING_IDOL.name().equals(routeType)) {
            return "练歌";
        }
        return "最终行动";
    }

    private boolean hasFanServiceCopy(String summary) {
        return summary.contains("FAN_SERVICE")
                || summary.contains("醒目留言")
                || summary.contains("高亮互动")
                || summary.contains("陪伴营业")
                || summary.contains("榜一排班表");
    }

    private String actionLabel(String action) {
        if (action == null || action.isBlank()) {
            return "未归档营业记录";
        }
        String normalizedAction = action.trim();
        return switch (normalizedAction) {
            case "TRAIN_SONG" -> "练歌";
            case "TRAIN_DANCE" -> "练舞";
            case "TRAIN_TALK" -> "杂谈复盘";
            case "STREAM_PLAN" -> "直播企划";
            case "PUBLISH_VIDEO" -> "发布视频";
            case "PUBLISH_CLIP" -> "发布切片";
            case "FAN_GROUP_MAINTAIN" -> "粉丝群维护";
            case "NPC_INTERACT" -> "同台互动";
            case "REST" -> "休息";
            case "EVENT_CHOICE" -> "事件选择";
            case "INTERACTION_CHOICE" -> "直播现场选择";
            default -> normalizedAction.startsWith("RISK_TOOL") ? "米线工具" : "未归档营业记录";
        };
    }

    private String routeLabel(String routeType) {
        return switch (routeType) {
            case "ELECTRONIC_PICKLE" -> "电子榨菜路线";
            case "SINGING_IDOL" -> "歌势路线";
            case "SLICE_SAINT" -> "切片路线";
            case "SOCIAL_COLLAB" -> "社交联动路线";
            case "DANCE_MEME" -> "梗舞整活路线";
            case "BLACK_RED_MAIN_STAGE" -> "黑红主会场路线";
            default -> "待定路线";
        };
    }

    private String summary(String endingType, Vup vup) {
        endingType = canonicalEndingType(endingType);
        if ("UNKNOWN".equals(endingType)) {
            return "30天结束，主播安全下播，但出圈证据不足。标题组翻完排班表，只找到零散努力，没找到能让路人复述的标签。";
        }
        if ("DD_BUS_STOP".equals(endingType)) {
            return "30天结束，DD坐满了几站，老粉学会了在联动表里找自家主播。社交证据和粉丝结构共同把你推向端水路线。";
        }
        if ("SLICE_SAINT".equals(endingType)) {
            if (RouteType.DANCE_MEME.name().equals(vup.getCurrentRoute())) {
                return "30天结束，梗舞练到动作还没完全标准，素材已经完全够用。乐子人逐帧鉴赏，切片组加急出餐，你从舞台训练室一路跳进了鬼畜区。";
            }
            return "30天结束，切片组剪得比你播得还勤。乐子人和围观热度把你推成素材供货商，录播组终于不用失业。";
        }
        if ("MAIN_STAGE_KING".equals(endingType)) {
            return "30天结束，你把直播间运营成了主会场常驻点。嘴硬、回旋镖和未结清标题债务一起上桌，录播组宣布本月素材 KPI 提前完成。";
        }
        if ("CYBER_GIRLFRIEND".equals(endingType)) {
            return "30天结束，醒目留言回应和陪伴营业把独角兽养成了高压锅。榜一很满意，老粉有点沉默，排班表看起来比直播企划还专业。";
        }
        if ("GLORIOUS_GRADUATION".equals(endingType)) {
            return "30天结束，你把最后一天开成了粉丝会而不是事故会。老粉泪目，乐子人问复活赛什么时候开，但至少这次是体面下播。";
        }
        return "30天结束，主播留下了可回放的营业证据。粉丝结构和路线分数共同决定了本轮结局。";
    }

    private record EndingCopyDecision(
            String subtitle,
            int candidateIndex,
            int candidatePoolSize,
            int catalogIndex,
            int catalogPoolSize,
            String subKey,
            Map<String, Object> seedData
    ) {
    }
}
