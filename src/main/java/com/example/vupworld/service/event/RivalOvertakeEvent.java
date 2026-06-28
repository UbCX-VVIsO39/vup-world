package com.example.vupworld.service.event;

import com.example.vupworld.dto.EventDtos.EventChoiceDTO;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.dto.NpcDtos.RivalDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对手超越事件（NEED_EVENT_CHOICE）。
 * 当对手粉丝超过玩家×1.2 时触发，玩家需选择：硬刚 / 联动 / 放任。
 */
@Service
public class RivalOvertakeEvent {
    public static final String SOURCE = "RIVAL_OVERTAKE";
    public static final String EVENT_TYPE = "RIVAL_OVERTAKE";

    private final JsonService jsonService;

    public RivalOvertakeEvent(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    public boolean supports(DaySession session) {
        return SOURCE.equals(session.getFormalEventSource());
    }

    /** 构造对手超越事件的 rollDetail JSON，供 pauseForRivalOvertakeIfNeeded 写入 session。 */
    public String buildRollDetailJson(Vup vup, RivalDTO topRival) {
        int gap = topRival.fans() - vup.getFans();
        Map<String, Object> rollDetail = new LinkedHashMap<>();
        rollDetail.put("rollType", "rival_overtake");
        rollDetail.put("eventKey", EVENT_TYPE);
        rollDetail.put("eventType", EVENT_TYPE);
        rollDetail.put("source", "rival");
        rollDetail.put("eventTitle", topRival.name() + "的流量超越");
        rollDetail.put("eventDescription", topRival.name() + "（" + topRival.route() + "路线）粉丝已达 " + topRival.fans()
                + "，超过你 " + gap + "。需要立刻回应。");
        rollDetail.put("rivalName", topRival.name());
        rollDetail.put("rivalRoute", topRival.route());
        rollDetail.put("rivalFans", topRival.fans());
        rollDetail.put("playerFans", vup.getFans());
        rollDetail.put("choices", List.of(
                Map.of("key", "HARD_CARRY", "label", "硬刚", "hint", "消耗AP+口碑风险，赢了抢回流量"),
                Map.of("key", "COLLAB", "label", "联动", "hint", "需NPC羁绊，借势分流"),
                Map.of("key", "IGNORE", "label", "放任", "hint", "掉粉但保留资源")
        ));
        return jsonService.write(rollDetail);
    }

    /** 从 session 的 rollDetail 构造 PendingEventDTO，供前端渲染选项。 */
    public PendingEventDTO toPendingEvent(DaySession session) {
        Map<String, Object> rollDetail = readRollDetail(session);
        String title = textValue(rollDetail, "eventTitle", "对手流量超越");
        String description = textValue(rollDetail, "eventDescription", "对手粉丝已经超越你，需要立刻回应。");
        return new PendingEventDTO(
                true,
                null,
                EVENT_TYPE,
                title,
                description,
                0,
                session.getDay(),
                session.getSelectedAction(),
                SOURCE,
                List.of(hardCarryChoice(), collabChoice(), ignoreChoice())
        );
    }

    private Map<String, Object> readRollDetail(DaySession session) {
        if (session.getFormalEventRollDetailJson() == null || session.getFormalEventRollDetailJson().isBlank()) {
            return Map.of();
        }
        return jsonService.readMap(session.getFormalEventRollDetailJson());
    }

    private String textValue(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return fallback;
    }

    private EventChoiceDTO hardCarryChoice() {
        return new EventChoiceDTO(
                "HARD_CARRY",
                "硬刚",
                "消耗额外AP，口碑承压",
                "赢了抢回流量，输了掉粉加倍",
                "若胜：粉丝+50~80，围观+10；若败：粉丝-30，口碑-5",
                true,
                null
        );
    }

    private EventChoiceDTO collabChoice() {
        return new EventChoiceDTO(
                "COLLAB",
                "联动",
                "需要NPC羁绊≥20",
                "借势分流，双方各涨一部分",
                "粉丝+30~50，NPC羁绊+5",
                true,
                null
        );
    }

    private EventChoiceDTO ignoreChoice() {
        return new EventChoiceDTO(
                "IGNORE",
                "放任",
                "无资源成本",
                "掉粉但不消耗资源",
                "粉丝-40~-60，保留AP和口碑",
                true,
                null
        );
    }
}
