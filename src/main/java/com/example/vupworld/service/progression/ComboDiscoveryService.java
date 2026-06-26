package com.example.vupworld.service.progression;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.ActionType;
import com.example.vupworld.dto.ComboDtos.ComboDiscoveryDTO;
import com.example.vupworld.dto.ComboDtos.ComboItemDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComboDiscoveryService {
    private static final int RECENT_LOG_LIMIT = 30;
    private static final int ACCOUNT_LOG_LIMIT = 1000;
    private static final Set<String> TRAIN_ACTIONS = Set.of(
            ActionType.TRAIN_SONG.name(),
            ActionType.TRAIN_DANCE.name(),
            ActionType.TRAIN_TALK.name()
    );

    private final VupService vupService;
    private final BusinessLogMapper businessLogMapper;
    private final JsonService jsonService;

    public ComboDiscoveryService(VupService vupService, BusinessLogMapper businessLogMapper, JsonService jsonService) {
        this.vupService = vupService;
        this.businessLogMapper = businessLogMapper;
        this.jsonService = jsonService;
    }

    public ComboDiscoveryDTO discovery(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        List<BusinessLog> currentLogs = recentLogs(vup);
        List<BusinessLog> accountLogs = accountLogs(userId);

        List<ComboDefinition> definitions = definitions();
        Map<String, ComboHit> currentHits = hitsByDefinition(definitions, currentLogs);
        Map<String, ComboHit> accountHits = accountHits(definitions, accountLogs);
        Set<String> currentKeys = currentHits.keySet();
        List<ComboItemDTO> discovered = new ArrayList<>();
        List<ComboItemDTO> locked = new ArrayList<>();
        for (ComboDefinition definition : definitions) {
            ComboHit accountHit = accountHits.get(definition.comboKey());
            if (accountHit != null) {
                ComboHit currentHit = currentHits.get(definition.comboKey());
                discovered.add(definition.discovered(currentHit == null ? accountHit.evidence() : currentHit.evidence()));
                continue;
            }
            locked.add(definition.locked());
        }
        int currentRunDiscoveredCount = (int) definitions.stream()
                .filter(definition -> currentKeys.contains(definition.comboKey()))
                .count();
        int newThisRunCount = (int) definitions.stream()
                .filter(definition -> currentKeys.contains(definition.comboKey()))
                .filter(definition -> firstSeenInCurrentRun(definition.comboKey(), accountLogs, vup.getId()))
                .count();

        return new ComboDiscoveryDTO(
                headline(discovered.size(), definitions.size(), currentRunDiscoveredCount, newThisRunCount),
                discovered.size(),
                definitions.size(),
                locked.size(),
                definitions.isEmpty() ? 0 : discovered.size() * 100 / definitions.size(),
                currentRunDiscoveredCount,
                newThisRunCount,
                "账号图鉴",
                locked.isEmpty() ? "" : locked.get(0).comboKey(),
                locked.isEmpty() ? "全部组合技已发现" : locked.get(0).label(),
                discovered,
                locked,
                nextHint(discovered, locked, newThisRunCount)
        );
    }

    public List<ComboItemDTO> reportHits(Vup vup, BusinessLog currentLog) {
        if (currentLog == null || currentLog.getId() == null) {
            return List.of();
        }
        List<BusinessLog> logs = recentLogs(vup);
        return definitions().stream()
                .map(definition -> {
                    ComboHit hit = definition.detector.apply(logs);
                    if (hit == null || !currentLog.getId().equals(hit.triggerLogId())) {
                        return null;
                    }
                    return definition.discovered(hit.evidence());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<BusinessLog> recentLogs(Vup vup) {
        List<BusinessLog> logs = new ArrayList<>(businessLogMapper.findRecentByVupId(vup.getId(), RECENT_LOG_LIMIT));
        logs.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId));
        return logs;
    }

    private List<BusinessLog> accountLogs(Long userId) {
        List<BusinessLog> logs = new ArrayList<>(businessLogMapper.findRecentByUserId(userId, ACCOUNT_LOG_LIMIT));
        logs.sort(Comparator.comparing(BusinessLog::getVupId)
                .thenComparingInt(BusinessLog::getDay)
                .thenComparing(BusinessLog::getId));
        return logs;
    }

    private Map<String, ComboHit> hitsByDefinition(List<ComboDefinition> definitions, List<BusinessLog> logs) {
        Map<String, ComboHit> hits = new LinkedHashMap<>();
        for (ComboDefinition definition : definitions) {
            ComboHit hit = definition.detector.apply(logs);
            if (hit != null) {
                hits.put(definition.comboKey(), hit);
            }
        }
        hits.putAll(loggedComboHits(logs));
        return hits;
    }

    private Map<String, ComboHit> accountHits(List<ComboDefinition> definitions, List<BusinessLog> logs) {
        Map<String, ComboHit> hits = new LinkedHashMap<>();
        for (List<BusinessLog> runLogs : logsByRun(logs).values()) {
            hits.putAll(hitsByDefinition(definitions, runLogs));
        }
        return hits;
    }

    private Map<Long, List<BusinessLog>> logsByRun(List<BusinessLog> logs) {
        return logs.stream()
                .filter(log -> log.getVupId() != null)
                .collect(Collectors.groupingBy(
                        BusinessLog::getVupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<String, ComboHit> loggedComboHits(List<BusinessLog> logs) {
        Map<String, ComboHit> hits = new HashMap<>();
        for (BusinessLog log : logs) {
            Map<String, Object> detail = safeReadMap(log.getMultiplierDetail());
            Object comboRaw = detail.get("comboImpact");
            if (!(comboRaw instanceof Map<?, ?> combo)) {
                continue;
            }
            Object keyRaw = combo.get("comboKey");
            if (!(keyRaw instanceof String comboKey) || comboKey.isBlank()) {
                continue;
            }
            String label = combo.get("label") instanceof String text && !text.isBlank()
                    ? text
                    : comboKey;
            hits.put(comboKey, new ComboHit(
                    "第%d天触发「%s」，已写入账号打法图鉴。".formatted(log.getDay(), label),
                    log.getId()
            ));
        }
        return hits;
    }

    private boolean firstSeenInCurrentRun(String comboKey, List<BusinessLog> accountLogs, Long currentVupId) {
        Long firstVupId = null;
        Long firstLogId = null;
        for (Map.Entry<Long, List<BusinessLog>> entry : logsByRun(accountLogs).entrySet()) {
            Map<String, ComboHit> runHits = hitsByDefinition(definitions(), entry.getValue());
            ComboHit hit = runHits.get(comboKey);
            if (hit == null || hit.triggerLogId() == null) {
                continue;
            }
            if (firstLogId == null || hit.triggerLogId() < firstLogId) {
                firstLogId = hit.triggerLogId();
                firstVupId = entry.getKey();
            }
        }
        return currentVupId != null && currentVupId.equals(firstVupId);
    }

    private Map<String, Object> safeReadMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonService.readMap(json);
        } catch (IllegalStateException ignored) {
            return Map.of();
        }
    }

    private List<ComboDefinition> definitions() {
        return List.of(
                new ComboDefinition(
                        "PRACTICE_TO_STREAM",
                        "练习后直播",
                        "练完基本功再把灯打开，观众更容易听出进步。",
                        "正面",
                        this::practiceToStream
                ),
                new ComboDefinition(
                        "VIDEO_TO_STREAM",
                        "视频引流",
                        "先把可转发素材铺出去，再开播接住回流。",
                        "正面",
                        logs -> orderedPair(
                                logs,
                                log -> ActionType.PUBLISH_VIDEO.name().equals(log.getAction()),
                                log -> ActionType.STREAM_PLAN.name().equals(log.getAction()),
                                3,
                                "第%d天投稿后，第%d天直播接住回流。"
                        )
                ),
                new ComboDefinition(
                        "VIDEO_TO_CLIP",
                        "一鱼两剪",
                        "长视频给素材，切片再把能传播的三十秒抠出来。",
                        "正面",
                        this::videoToClip
                ),
                new ComboDefinition(
                        "CHARGE_COMPLETE",
                        "充电完毕",
                        "连续低压收工，第二天再营业时不容易把嗓子抵押出去。",
                        "正面",
                        logs -> consecutiveAction(logs, ActionType.REST.name(), 2, "第%d天到第%d天连续休息，电量回到可营业区。")
                ),
                new ComboDefinition(
                        "HARD_PRACTICE",
                        "刻苦练习",
                        "一周里把基本功多次搬上桌，路线会记住这段训练。",
                        "正面",
                        logs -> atLeastInWindow(logs, TRAIN_ACTIONS, 3, 7, "最近7天练习%d次，练功房地板已经记住鞋印。")
                ),
                new ComboDefinition(
                        "OVERWORK",
                        "过劳边缘",
                        "直播太密时，观众习惯了，主播身体未必答应。",
                        "负面",
                        logs -> atLeastInWindow(logs, Set.of(ActionType.STREAM_PLAN.name()), 3, 7, "最近7天直播%d次，嗓子开始提交工单。")
                ),
                new ComboDefinition(
                        "FORGOTTEN",
                        "被遗忘",
                        "休息太多也会让楼友先去隔壁排队。",
                        "负面",
                        logs -> atLeastInWindow(logs, Set.of(ActionType.REST.name()), 4, 7, "最近7天休息%d次，首页推荐位开始装作不认识你。")
                ),
                new ComboDefinition(
                        "SOCIAL_WARMUP",
                        "查房预热",
                        "先去同行场子露个脸，再开播更像有人把门推开了。",
                        "特殊",
                        logs -> orderedPair(
                                logs,
                                log -> ActionType.NPC_INTERACT.name().equals(log.getAction()),
                                log -> ActionType.STREAM_PLAN.name().equals(log.getAction()),
                                7,
                                "第%d天查房互动后，第%d天直播带着熟人气进场。"
                        )
                )
        );
    }

    private ComboHit practiceToStream(List<BusinessLog> logs) {
        return orderedPair(
                logs,
                log -> TRAIN_ACTIONS.contains(log.getAction()),
                log -> ActionType.STREAM_PLAN.name().equals(log.getAction()),
                3,
                "第%d天练习后，第%d天直播把进步端上桌。"
        );
    }

    private ComboHit videoToClip(List<BusinessLog> logs) {
        ComboHit latestHit = null;
        for (int i = 0; i < logs.size(); i++) {
            BusinessLog video = logs.get(i);
            if (!ActionType.PUBLISH_VIDEO.name().equals(video.getAction())) {
                continue;
            }
            for (int j = i + 1; j < logs.size(); j++) {
                BusinessLog current = logs.get(j);
                int gap = current.getDay() - video.getDay();
                if (gap > 7) {
                    break;
                }
                if (ActionType.PUBLISH_CLIP.name().equals(current.getAction())) {
                    latestHit = new ComboHit(
                            "第%d天投稿后，第%d天切片组继续供货。".formatted(video.getDay(), current.getDay()),
                            current.getId()
                    );
                    break;
                }
            }
        }
        return latestHit;
    }

    private ComboHit orderedPair(
            List<BusinessLog> logs,
            java.util.function.Predicate<BusinessLog> first,
            java.util.function.Predicate<BusinessLog> second,
            int maxDayGap,
            String evidenceTemplate
    ) {
        ComboHit latestHit = null;
        for (int i = 0; i < logs.size(); i++) {
            BusinessLog left = logs.get(i);
            if (!first.test(left)) {
                continue;
            }
            for (int j = i + 1; j < logs.size(); j++) {
                BusinessLog right = logs.get(j);
                int gap = right.getDay() - left.getDay();
                if (gap > maxDayGap) {
                    break;
                }
                if (gap >= 0 && second.test(right)) {
                    latestHit = new ComboHit(evidenceTemplate.formatted(left.getDay(), right.getDay()), right.getId());
                }
            }
        }
        return latestHit;
    }

    private ComboHit consecutiveAction(List<BusinessLog> logs, String action, int count, String evidenceTemplate) {
        ComboHit latestHit = null;
        int streak = 0;
        int startDay = 0;
        int previousDay = -1;
        for (BusinessLog log : logs) {
            if (!action.equals(log.getAction())) {
                streak = 0;
                previousDay = -1;
                continue;
            }
            if (streak == 0 || log.getDay() != previousDay + 1) {
                streak = 1;
                startDay = log.getDay();
            } else {
                streak++;
            }
            previousDay = log.getDay();
            if (streak >= count) {
                latestHit = new ComboHit(evidenceTemplate.formatted(startDay, log.getDay()), log.getId());
            }
        }
        return latestHit;
    }

    private ComboHit atLeastInWindow(List<BusinessLog> logs, Set<String> actions, int count, int windowDays, String evidenceTemplate) {
        ComboHit latestHit = null;
        for (int i = 0; i < logs.size(); i++) {
            BusinessLog start = logs.get(i);
            if (!actions.contains(start.getAction())) {
                continue;
            }
            int matched = 0;
            for (int j = i; j < logs.size(); j++) {
                BusinessLog current = logs.get(j);
                int gap = current.getDay() - start.getDay();
                if (gap >= windowDays) {
                    break;
                }
                if (actions.contains(current.getAction())) {
                    matched++;
                }
                if (matched >= count) {
                    latestHit = new ComboHit(evidenceTemplate.formatted(matched), current.getId());
                }
            }
        }
        return latestHit;
    }

    private String headline(int discoveredCount, int totalCount, int currentRunDiscoveredCount, int newThisRunCount) {
        if (newThisRunCount > 0) {
            return "组合技图鉴：本周目新收录%d个套路，账号打法库正在变厚。".formatted(newThisRunCount);
        }
        if (currentRunDiscoveredCount > 0) {
            return "组合技图鉴：本周目已复现%d个旧套路，可以围绕熟练打法冲评分。".formatted(currentRunDiscoveredCount);
        }
        if (discoveredCount == 0) {
            return "组合技图鉴：还没有账号级套路记录，先让行动历史攒点上下文。";
        }
        if (discoveredCount >= totalCount / 2) {
            return "组合技图鉴：账号打法开始成型，复活赛可以带着套路库重打。";
        }
        return "组合技图鉴：账号已收录%d个连招，下一步看你要稳基本功还是追热度。".formatted(discoveredCount);
    }

    private String nextHint(List<ComboItemDTO> discovered, List<ComboItemDTO> locked, int newThisRunCount) {
        if (newThisRunCount > 0) {
            return "本周目有新套路入库，结局后仍会保留在账号图鉴里。";
        }
        if (locked.isEmpty()) {
            return "所有核心组合技都已露面，接下来可以围绕已掌握套路安排冲刺节奏。";
        }
        if (discovered.isEmpty()) {
            return "试试先练习再直播，或者先投稿再切片，系统会从最近行动里认出套路。";
        }
        return "下一个灰色提示可以顺手解锁，但别为了凑连招把体力和米线一起透支。";
    }

    private record ComboDefinition(
            String comboKey,
            String label,
            String hint,
            String tone,
            Function<List<BusinessLog>, ComboHit> detector
    ) {
        ComboItemDTO discovered(String evidence) {
            return new ComboItemDTO(comboKey, label, hint, true, evidence, tone);
        }

        ComboItemDTO locked() {
            return new ComboItemDTO(comboKey, label, hint, false, "还没有足够行动证据。", "锁定");
        }
    }

    private record ComboHit(String evidence, Long triggerLogId) {
    }
}
