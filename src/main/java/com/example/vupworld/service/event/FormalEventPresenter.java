package com.example.vupworld.service.event;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.dto.EventDtos.EventChoiceDTO;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FormalEventPresenter {
    private final RiskDebtMapper riskDebtMapper;
    private final DayFlowService dayFlowService;
    private final JsonService jsonService;

    public FormalEventPresenter(RiskDebtMapper riskDebtMapper, DayFlowService dayFlowService, JsonService jsonService) {
        this.riskDebtMapper = riskDebtMapper;
        this.dayFlowService = dayFlowService;
        this.jsonService = jsonService;
    }

    public PendingEventDTO pending(Vup vup, DaySession session) {
        if (!DayPhase.NEED_EVENT_CHOICE.name().equals(session.getPhase())) {
            return noPendingEvent();
        }
        if (isOrdinaryEventPending(session)) {
            return ordinaryPendingEvent(session);
        }
        if (session.getPendingFormalEventId() == null) {
            return noPendingEvent();
        }

        RiskDebt debt = riskDebtMapper.findById(session.getPendingFormalEventId());
        if (debt == null || !debt.getVupId().equals(vup.getId())) {
            return noPendingEvent();
        }

        return new PendingEventDTO(
                true,
                debt.getId(),
                debt.getDebtType(),
                eventTitle(debt, session),
                eventDescription(debt, session),
                debt.getSeverity(),
                debt.getDueDay(),
                debt.getSourceAction(),
                debt.getSourceTitle(),
                List.of(safeChoice(), trafficChoice(), memeChoice())
        );
    }

    public boolean isOrdinaryEventPending(DaySession session) {
        return session.getPendingFormalEventId() == null
                && "RESERVED".equals(session.getFormalEventSlotStatus())
                && ("REST_SAVED_MELTDOWN".equals(session.getFormalEventSource())
                || "MIDGAME_EVENT".equals(session.getFormalEventSource())
                || "LATE_GAME_EVENT".equals(session.getFormalEventSource())
                || "RANDOM_EVENT".equals(session.getFormalEventSource()));
    }

    private PendingEventDTO noPendingEvent() {
        return new PendingEventDTO(false, null, null, null, null, 0, 0, null, null, List.of());
    }

    private PendingEventDTO ordinaryPendingEvent(DaySession session) {
        Map<String, Object> rollDetail = rollDetail(session);
        String eventType = textValue(rollDetail, "eventType", "ORDINARY_EVENT");
        String title = textValue(rollDetail, "eventTitle", "低压运营救场");
        String description = ordinaryEventDescription(session, rollDetail);
        return new PendingEventDTO(
                true,
                null,
                eventType,
                title,
                description,
                0,
                session.getDay(),
                session.getSelectedAction(),
                session.getFormalEventSource(),
                ordinaryEventChoices(rollDetail)
        );
    }

    private String ordinaryEventDescription(DaySession session, Map<String, Object> rollDetail) {
        String description = textValue(
                rollDetail,
                "eventDescription",
                "连续低压休息把一次潜在翻车熬成了无事发生。楼友没吃到大的，但老粉表示今天这碗电子榨菜很稳。"
        );
        String effect = textValue(rollDetail, "eventEffect", "");
        if (effect.isBlank()) {
            return description;
        }
        return description + " 预期影响：" + effect + "。现在要决定是降温、接流量，还是把它做成梗。";
    }

    private Map<String, Object> rollDetail(DaySession session) {
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

    @SuppressWarnings("unchecked")
    private List<EventChoiceDTO> ordinaryEventChoices(Map<String, Object> rollDetail) {
        Object rawChoices = rollDetail.get("choices");
        if (rawChoices instanceof Map<?, ?> choices) {
            return List.of(
                    contentChoice("safe", (Map<String, Object>) choices.get("safe"), safeChoice()),
                    contentChoice("traffic", (Map<String, Object>) choices.get("traffic"), trafficChoice()),
                    contentChoice("meme", (Map<String, Object>) choices.get("meme"), memeChoice())
            );
        }
        return List.of(safeChoice(), trafficChoice(), memeChoice());
    }

    private EventChoiceDTO contentChoice(String choiceType, Map<String, Object> source, EventChoiceDTO fallback) {
        if (source == null || source.isEmpty()) {
            return fallback;
        }
        return new EventChoiceDTO(
                choiceType,
                textValue(source, "label", fallback.label()),
                textValue(source, "costPreview", fallback.costPreview()),
                textValue(source, "riskPreview", fallback.riskPreview()),
                textValue(source, "effectPreview", fallback.effectPreview()),
                true,
                null
        );
    }

    private EventChoiceDTO safeChoice() {
        return new EventChoiceDTO(
                "safe",
                "降温说明，先把米线扶正",
                "无资源成本",
                "热度下降，口碑小幅回稳，证据仍会留档",
                "严重度 -1，围观热度 -5，口碑 +2",
                true,
                null
        );
    }

    private EventChoiceDTO trafficChoice() {
        return new EventChoiceDTO(
                "traffic",
                "硬接流量，主会场继续开庭",
                "围观热度大幅上升，口碑承压",
                "围观热度飙升，大量粉丝涌入，但口碑承压",
                "围观热度 +20，口碑 -4，可能产生新债务",
                true,
                null
        );
    }

    private EventChoiceDTO memeChoice() {
        return new EventChoiceDTO(
                "meme",
                "顺势玩梗，让楼友自己施工",
                "梗等级飙升，切片路线",
                "顺势玩梗，梗等级飙升，但可能被切片组放大",
                "梗等级 +5，围观热度 +5，口碑 -1，可能产生事故素材",
                true,
                null
        );
    }

    private String eventTitle(RiskDebt debt, DaySession session) {
        if ("FAN_TOPIC_ESCALATION".equals(session.getFormalEventSource())) {
            return "粉丝群升级正式事件";
        }
        if ("TITLE_BACKFIRE".equals(debt.getDebtType())) {
            return "标题党反噬正式事件";
        }
        if ("BOOMERANG_CLIP".equals(debt.getDebtType())) {
            return "回旋镖切片正式事件";
        }
        if ("UNICORN_EXPECTATION".equals(debt.getDebtType())) {
            return "独角兽期待正式事件";
        }
        if ("COMMERCIAL_BACKLASH".equals(debt.getDebtType())) {
            return "商业反噬正式事件";
        }
        return "舆论债务正式事件";
    }

    private String eventDescription(RiskDebt debt, DaySession session) {
        String source = debt.getSourceTitle() == null || debt.getSourceTitle().isBlank()
                ? dayFlowService.actionLabel(debt.getSourceAction())
                : "《" + debt.getSourceTitle() + "》";
        if ("FAN_TOPIC_ESCALATION".equals(session.getFormalEventSource())) {
            return "粉丝群议题没有及时下场处理，房管删帖、老粉小作文和楼友贷款后续挤成一个小主会场。"
                    + " 一笔未结清债务被群聊提前翻出来：" + debt.getSummary()
                    + " 现在必须先降温，日报组才能继续写今天的主行动。";
        }
        if ("COMMERCIAL_BACKLASH".equals(debt.getDebtType())) {
            return "第 " + debt.getDueDay() + " 天，来自 " + source + " 的商业反噬到账：观众开始刷“味儿变了”，品牌避险雷达抬头。"
                    + debt.getSummary()
                    + " 现在必须先降温，别让老板满意变成观众退订。";
        }
        if ("TITLE_BACKFIRE".equals(debt.getDebtType())) {
            return "第 " + debt.getDueDay() + " 天，来自 " + source + " 的标题党反噬到期：标题组被录播组查重，考据楼开始翻高音贷款。"
                    + debt.getSummary()
                    + " 现在必须先降温，别让米线小作文把日报组也拖进主会场。";
        }
        if ("BOOMERANG_CLIP".equals(debt.getDebtType())) {
            return "第 " + debt.getDueDay() + " 天，来自 " + source + " 的回旋镖切片到账：切片组补上下文，录播组翻时间轴。"
                    + debt.getSummary()
                    + " 现在必须先处理这口锅，日报组才能继续写小作文。";
        }
        return "第 " + debt.getDueDay() + " 天，来自 " + source + " 的舆论旧账到账："
                + debt.getSummary()
                + " 现在必须先处理这口锅，日报组才能继续写小作文。";
    }

}
