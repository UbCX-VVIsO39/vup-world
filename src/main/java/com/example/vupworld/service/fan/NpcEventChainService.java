package com.example.vupworld.service.fan;

import com.example.vupworld.domain.NpcType;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.Vup;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * NPC事件链系统。按亲密度门槛（20/40/60）解锁路线NPC的事件链。
 *
 * 统一使用 {@link NpcType} 的 key 作为 NPC 标识，与 NpcRelationshipService 共享
 * 亲密度数据（tutorialFlagsJson.npcAffinity）。这样玩家培养的亲密度能真正触发事件链。
 *
 * markResolved 时会根据 effectKey 应用实际效果（歌力/梗力/粉丝/路线分/亲密度等），
 * 并通过 VupMapper 持久化，避免"做完事件链零反馈"。
 */
@Component
public class NpcEventChainService {

    private final NpcRelationshipService npcRelationshipService;
    private final JsonService jsonService;
    private final VupMapper vupMapper;

    public NpcEventChainService(NpcRelationshipService npcRelationshipService,
                                 JsonService jsonService,
                                 VupMapper vupMapper) {
        this.npcRelationshipService = npcRelationshipService;
        this.jsonService = jsonService;
        this.vupMapper = vupMapper;
    }

    // NPC事件链：每条路线1个NPC，3个事件按affinity解锁。
    // key 统一使用 NpcType 枚举名（如 SENN_LIN），与 NpcRelationshipService 对齐。
    private static final Map<String, List<NpcChainEvent>> CHAIN_EVENTS = new LinkedHashMap<>();

    static {
        // SENN_LIN（原"海声前辈"）- 歌势路线
        CHAIN_EVENTS.put(NpcType.SENN_LIN.key(), Arrays.asList(
            new NpcChainEvent("VOICE_LESSON", 20, "森岭教你一首新歌", "练歌收益+20%，持续3天", "songPower", 2),
            new NpcChainEvent("COLLAB_RECOMMEND", 40, "森岭推荐你参加歌回联动", "歌势路线分+5，DD粉+10", "routeScore", 5),
            new NpcChainEvent("SHIELD_CONTROVERSY", 60, "森岭帮你挡了一次争议", "口碑+10，消除1个OPEN债务", "reputation", 10)
        ));
        // MIKU_QI（原"夜班剪辑"）- 切片路线
        CHAIN_EVENTS.put(NpcType.MIKU_QI.key(), Arrays.asList(
            new NpcChainEvent("TITLE_TIPS", 20, "米库七教你优化切片标题", "切片传播速度+30%，持续3天", "memeLevel", 3),
            new NpcChainEvent("EDIT_SKILLS", 40, "米库七教你剪辑技巧", "梗力+3，切片收益翻倍1天", "memePower", 3),
            new NpcChainEvent("RECOMMEND_SPOT", 60, "米库七推荐你上推荐位", "热度+20，人气+15", "watchHeat", 20)
        ));
        // XIAO_YU（原"隔壁锐评员"）- 黑红路线
        CHAIN_EVENTS.put(NpcType.XIAO_YU.key(), Arrays.asList(
            new NpcChainEvent("ROAST_BACK", 20, "小鱼帮你回怼黑粉", "热度+10，口碑-2", "watchHeat", 10),
            new NpcChainEvent("CONTROVERSY_TIPS", 40, "小鱼教你驾驭争议", "黑红路线分+5，抗压+2", "routeScore", 5),
            new NpcChainEvent("JOINT_STREAM", 60, "小鱼邀请你联合直播", "粉丝+50，热度+25，口碑-5", "fanCount", 50)
        ));
        // DA_HAO（原"联动搭子"）- 社交路线
        CHAIN_EVENTS.put(NpcType.DA_HAO.key(), Arrays.asList(
            new NpcChainEvent("SMALL_COLLAB", 20, "大豪邀请你小型联动", "DD粉+15，社交路线分+3", "ddFanCount", 15),
            new NpcChainEvent("GROUP_EVENT", 40, "大豪带你参加多人联动", "粉丝+30，人气+10", "fanCount", 30),
            new NpcChainEvent("NETWORK_INTRO", 60, "大豪介绍你认识大V", "粉丝+80，商业+10", "fanCount", 80)
        ));
        // YUE_YA（原"节拍教练"）- 梗舞路线
        CHAIN_EVENTS.put(NpcType.YUE_YA.key(), Arrays.asList(
            new NpcChainEvent("DANCE_BASICS", 20, "月牙教你基础舞步", "舞蹈+2，梗浓度+5", "dancePower", 2),
            new NpcChainEvent("VIRAL_CHOREO", 40, "月牙编了一支魔性舞蹈", "梗浓度+15，乐子粉+20", "memeLevel", 15),
            new NpcChainEvent("DANCE_CHALLENGE", 60, "月牙发起舞蹈挑战", "粉丝+100，梗浓度+20", "fanCount", 100)
        ));
        // BILI_MIKO（原"值班房管"）- 粉丝管理
        CHAIN_EVENTS.put(NpcType.BILI_MIKO.key(), Arrays.asList(
            new NpcChainEvent("GROUP_CLEAN", 20, "B站miko帮你清理粉丝群", "口碑+3，热度-5", "reputation", 3),
            new NpcChainEvent("FAN_SURVEY", 40, "B站miko做了粉丝调研", "真粉+10，独角兽粉+5", "trueFanCount", 10),
            new NpcChainEvent("CRISIS_HANDLE", 60, "B站miko帮你处理了一次危机", "口碑+8，消除1个债务", "reputation", 8)
        ));
    }

    public static class NpcChainEvent {
        public final String eventKey;
        public final int affinityRequired;
        public final String title;
        public final String description;
        public final String effectKey;
        public final int effectValue;

        public NpcChainEvent(String eventKey, int affinityRequired, String title,
                            String description, String effectKey, int effectValue) {
            this.eventKey = eventKey;
            this.affinityRequired = affinityRequired;
            this.title = title;
            this.description = description;
            this.effectKey = effectKey;
            this.effectValue = effectValue;
        }
    }

    // 检查是否有可触发的NPC事件链事件
    public Optional<NpcChainEvent> checkForChainEvent(Vup vup, DaySession session) {
        String npcKey = getNpcForRoute(vup.getCurrentRoute());
        if (npcKey == null) return Optional.empty();

        int affinity = npcRelationshipService.getAffinity(vup, npcKey);
        List<NpcChainEvent> chain = CHAIN_EVENTS.get(npcKey);
        if (chain == null) return Optional.empty();

        // 查找下一个未解锁的事件
        Map<String, Object> resolved = getResolvedChainEvents(vup, npcKey);
        for (NpcChainEvent event : chain) {
            if (affinity >= event.affinityRequired && !resolved.containsKey(event.eventKey)) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    // 获取当前路线对应的NPC key（供Controller使用）
    public String getNpcForCurrentRoute(Vup vup) {
        return getNpcForRoute(vup.getCurrentRoute());
    }

    // 路线 -> NPC key，统一走 NpcType.forRoute
    private String getNpcForRoute(String route) {
        NpcType npcType = NpcType.forRoute(route);
        return npcType == null ? null : npcType.key();
    }

    /**
     * 标记事件已解决，并应用 effectKey 描述的实际效果。
     * 加 @Transactional 保证"标记 + 应用效果 + 持久化"原子性。
     * 返回效果描述字符串（供上层反馈给玩家），找不到事件时返回空串。
     */
    @Transactional
    public String markResolved(Vup vup, String npcKey, String eventKey) {
        // 标记已解决
        Map<String, Object> resolved = getResolvedChainEvents(vup, npcKey);
        resolved.put(eventKey, true);
        saveResolvedChainEvents(vup, npcKey, resolved);

        // 查找事件定义，应用 effectKey
        NpcChainEvent target = findEvent(npcKey, eventKey);
        if (target == null) {
            // 没有匹配的事件定义，仅持久化已解决标记
            vupMapper.updateState(vup);
            return "";
        }
        applyEffect(vup, npcKey, target);
        vupMapper.updateState(vup);
        return target.description;
    }

    private NpcChainEvent findEvent(String npcKey, String eventKey) {
        List<NpcChainEvent> chain = CHAIN_EVENTS.get(npcKey);
        if (chain == null) return null;
        for (NpcChainEvent e : chain) {
            if (e.eventKey.equals(eventKey)) return e;
        }
        return null;
    }

    /**
     * 根据 effectKey 应用实际效果到 Vup。
     * effectKey 命名约定：直接对应 Vup 字段名（如 songPower/dancePower/fanCount），
     * 或为语义化的特殊效果（affinity_boost/inspiration_gain/buff_steal_learn）。
     */
    private void applyEffect(Vup vup, String npcKey, NpcChainEvent event) {
        int v = event.effectValue;
        switch (event.effectKey) {
            case "songPower" -> vup.setSongPower(vup.getSongPower() + v);
            case "dancePower" -> vup.setDancePower(vup.getDancePower() + v);
            case "memePower" -> vup.setMemePower(vup.getMemePower() + v);
            case "memeLevel" -> vup.setMemeLevel(vup.getMemeLevel() + v);
            case "watchHeat" -> vup.setWatchHeat(vup.getWatchHeat() + v);
            case "reputation" -> vup.setReputation(vup.getReputation() + v);
            case "popularity" -> vup.setPopularity(vup.getPopularity() + v);
            case "fanCount" -> vup.setFans(vup.getFans() + v);
            case "ddFanCount" -> vup.setDdFans(vup.getDdFans() + v);
            case "trueFanCount" -> vup.setTrueFans(vup.getTrueFans() + v);
            case "routeScore" -> addRouteScore(vup, v);
            // 语义化特殊效果
            case "affinity_boost" -> npcRelationshipService.addAffinity(vup, npcKey, v);
            case "inspiration_gain" -> vup.setInspiration(vup.getInspiration() + v);
            case "buff_steal_learn" -> npcRelationshipService.grantStealBuff(vup, npcKey);
            default -> { /* 未知 effectKey 静默忽略，避免阻断事件链流程 */ }
        }
    }

    /**
     * 给当前路线增加路线分（routeScoreJson[currentRoute] += value）。
     */
    private void addRouteScore(Vup vup, int value) {
        String route = vup.getCurrentRoute();
        if (route == null) return;
        String json = vup.getRouteScoreJson();
        Map<String, Object> scores;
        if (json == null || json.isBlank()) {
            scores = new LinkedHashMap<>();
        } else {
            try {
                scores = new LinkedHashMap<>(jsonService.readMap(json));
            } catch (Exception e) {
                scores = new LinkedHashMap<>();
            }
        }
        int current = 0;
        Object obj = scores.get(route);
        if (obj instanceof Number number) {
            current = number.intValue();
        }
        scores.put(route, current + value);
        vup.setRouteScoreJson(jsonService.write(scores));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResolvedChainEvents(Vup vup, String npcKey) {
        String json = vup.getTutorialFlagsJson();
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> flags = jsonService.readMap(json);
            Object chainObj = flags.get("npcChain_" + npcKey);
            if (chainObj instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) chainObj);
            }
        } catch (Exception ignored) {
        }
        return new LinkedHashMap<>();
    }

    private void saveResolvedChainEvents(Vup vup, String npcKey, Map<String, Object> resolved) {
        String json = vup.getTutorialFlagsJson();
        Map<String, Object> flags;
        if (json == null || json.isBlank()) {
            flags = new LinkedHashMap<>();
        } else {
            try {
                flags = new LinkedHashMap<>(jsonService.readMap(json));
            } catch (Exception e) {
                flags = new LinkedHashMap<>();
            }
        }
        flags.put("npcChain_" + npcKey, resolved);
        vup.setTutorialFlagsJson(jsonService.write(flags));
    }
}
