package com.example.vupworld.service.fan;

import com.example.vupworld.domain.NpcType;
import com.example.vupworld.dto.MoodDtos.NpcInteractionResult;
import com.example.vupworld.dto.MoodDtos.StealLearnResult;
import com.example.vupworld.dto.NpcDtos.NpcRelationshipDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.infra.RngLedger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NPC关系系统。管理VUP与NPC之间的亲密度。
 * 亲密度数据存储在VUP的tutorialFlagsJson中的npcAffinity字段。
 * NPC定义统一来自 {@link NpcType} 枚举，避免与其他NPC子系统出现key不一致。
 */
@Component
public class NpcRelationshipService {
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final JsonService jsonService;
    private final DeterministicRngService deterministicRngService;

    public NpcRelationshipService(
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            JsonService jsonService,
            DeterministicRngService deterministicRngService
    ) {
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.jsonService = jsonService;
        this.deterministicRngService = deterministicRngService;
    }

    /**
     * 获取VUP与指定NPC的亲密度。
     */
    public int getAffinity(Vup vup, String npcKey) {
        Map<String, Object> affinityMap = loadAffinityMap(vup);
        Object value = affinityMap.get(npcKey);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    /**
     * 获取所有NPC的亲密度列表。
     */
    public List<NpcRelationshipDTO> getAllAffinity(Vup vup) {
        Map<String, Object> affinityMap = loadAffinityMap(vup);
        List<NpcRelationshipDTO> result = new ArrayList<>();
        for (NpcType npcType : NpcType.all().values()) {
            String key = npcType.key();
            int affinity = 0;
            Object value = affinityMap.get(key);
            if (value instanceof Number number) {
                affinity = number.intValue();
            }
            result.add(new NpcRelationshipDTO(
                    key,
                    npcType.displayName(),
                    affinity,
                    affinityLabel(affinity)
            ));
        }
        return result;
    }

    /**
     * 检查是否可以对指定NPC进行偷学。
     * 需要已发送技术弹幕（解锁标记）且亲密度≥15（偷学消耗门槛）。
     */
    public boolean canStealLearn(Vup vup, String npcKey) {
        if (getAffinity(vup, npcKey) < 15) {
            return false;
        }
        Map<String, Object> unlockMap = loadStealUnlockMap(vup);
        Object value = unlockMap.get(npcKey);
        return Boolean.TRUE.equals(value);
    }

    /**
     * 标记NPC为可偷学状态。
     */
    private void markStealUnlocked(Vup vup, String npcKey) {
        Map<String, Object> unlockMap = loadStealUnlockMap(vup);
        unlockMap.put(npcKey, true);
        saveStealUnlockMap(vup, unlockMap);
    }

    /**
     * 偷学NPC的直播技巧。
     * 消耗15点亲密度，获得下次直播的临时buff。
     * 有被发现的风险（30%）：被发现时额外-10亲密度、-5声望。
     */
    @Transactional
    public StealLearnResult stealLearn(Vup vup, String npcKey) {
        NpcType npcType = NpcType.fromKey(npcKey);
        if (npcType == null) {
            return new StealLearnResult(npcKey, "未知", 0, 0, false, 0, null, "找不到这个NPC。");
        }
        if (!canStealLearn(vup, npcKey)) {
            return new StealLearnResult(npcKey, npcType.displayName(), 0, getAffinity(vup, npcKey),
                    false, 0, null, "尚未解锁对该主播的偷学。先发一条技术弹幕吧。");
        }

        int currentAffinity = getAffinity(vup, npcKey);
        int affinityCost = 15;
        int newAffinity = Math.max(-100, currentAffinity - affinityCost);

        // 确定性随机检查是否被发现（30%概率）
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        String seed = session != null ? session.getRandomSeed() : vup.getRunSeed();
        int cursor = session != null ? session.getRngCursor() : 0;
        RngLedger ledger = new RngLedger(seed, cursor);
        boolean discovered = ledger.nextInt("steal_discovery:" + npcKey, 100) < 30;

        int reputationChange = 0;
        int totalAffinityChange = -affinityCost;
        if (discovered) {
            totalAffinityChange -= 10;
            reputationChange = -5;
            newAffinity = Math.max(-100, newAffinity - 10);
            vup.setReputation(vup.getReputation() - 5);
        }

        Map<String, Object> affinityMap = loadAffinityMap(vup);
        affinityMap.put(npcKey, newAffinity);
        saveAffinityMap(vup, affinityMap);

        // 存储临时buff，供下次直播使用
        grantStealBuff(vup, npcKey, npcType.displayName());

        String buffDesc = "偷学自" + npcType.displayName() + "的技巧：下次直播对应能力+" + (10 + currentAffinity / 10) + "%";
        String message = discovered
                ? "你偷学了" + npcType.displayName() + "的技巧，但被发现了！声望-5。"
                : "你悄悄研究了" + npcType.displayName() + "的直播技巧。";
        return new StealLearnResult(npcKey, npcType.displayName(), totalAffinityChange, newAffinity,
                discovered, reputationChange, buffDesc, message);
    }

    /**
     * 获取当前活跃的偷学buff描述。无buff时返回null。
     */
    public String getActiveStealBuff(Vup vup) {
        String json = vup.getTutorialFlagsJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> flags = jsonService.readMap(json);
            Object buff = flags.get("stealBuff");
            if (buff instanceof Map<?, ?> buffMap) {
                Object name = buffMap.get("npcName");
                if (name instanceof String npcName) {
                    return "偷学自" + npcName + "的技巧：下次直播所有能力+10%";
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 消费偷学buff：读取并清除buff，返回NPC名称。无buff时返回null。
     * 在下次直播结算时调用，应用加成后清除。
     */
    @Transactional
    public String consumeStealBuff(Vup vup) {
        String json = vup.getTutorialFlagsJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> flags = new LinkedHashMap<>(jsonService.readMap(json));
            Object buff = flags.get("stealBuff");
            if (!(buff instanceof Map<?, ?> buffMap)) {
                return null;
            }
            Object name = buffMap.get("npcName");
            flags.remove("stealBuff");
            vup.setTutorialFlagsJson(jsonService.write(flags));
            vupMapper.updateState(vup);
            return name instanceof String npcName ? npcName : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 存储偷学buff到tutorialFlagsJson。
     */
    private void grantStealBuff(Vup vup, String npcKey, String npcName) {
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
        Map<String, Object> buff = new LinkedHashMap<>();
        buff.put("npcKey", npcKey);
        buff.put("npcName", npcName);
        buff.put("buffDay", vup.getDayCount());
        flags.put("stealBuff", buff);
        vup.setTutorialFlagsJson(jsonService.write(flags));
        vupMapper.updateState(vup);
    }

    /**
     * 授予偷学buff（供事件链等外部调用，不消耗亲密度）。
     * 找不到NPC时静默忽略。
     */
    @Transactional
    public void grantStealBuff(Vup vup, String npcKey) {
        NpcType npcType = NpcType.fromKey(npcKey);
        if (npcType == null) {
            return;
        }
        grantStealBuff(vup, npcKey, npcType.displayName());
    }

    /**
     * 增加NPC亲密度（供事件链等外部调用）。
     * 亲密度限制在-100到100。返回调整后的亲密度。找不到NPC时返回0。
     */
    @Transactional
    public int addAffinity(Vup vup, String npcKey, int delta) {
        if (NpcType.fromKey(npcKey) == null) {
            return 0;
        }
        int currentAffinity = getAffinity(vup, npcKey);
        int newAffinity = Math.max(-100, Math.min(100, currentAffinity + delta));
        Map<String, Object> affinityMap = loadAffinityMap(vup);
        affinityMap.put(npcKey, newAffinity);
        saveAffinityMap(vup, affinityMap);
        return newAffinity;
    }

    /**
     * 与NPC互动，更新亲密度。
     * 使用确定性随机生成互动结果。
     */
    @Transactional
    public NpcInteractionResult interact(Vup vup, String npcKey, String interactionType) {
        NpcType npcType = NpcType.fromKey(npcKey);
        if (npcType == null) {
            return new NpcInteractionResult(npcKey, "未知", 0, 0, "找不到这个NPC。");
        }

        // 加载当前亲密度
        Map<String, Object> affinityMap = loadAffinityMap(vup);
        int currentAffinity = 0;
        Object value = affinityMap.get(npcKey);
        if (value instanceof Number number) {
            currentAffinity = number.intValue();
        }

        // 使用确定性随机计算亲密度变化
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        String seed = session != null ? session.getRandomSeed() : vup.getRunSeed();
        int cursor = session != null ? session.getRngCursor() : 0;

        // 创建临时ledger，不更新cursor
        RngLedger ledger = new RngLedger(seed, cursor);
        int affinityChange = calculateAffinityChange(ledger, npcKey, interactionType);

        // 更新亲密度（限制在-100到100）
        int newAffinity = Math.max(-100, Math.min(100, currentAffinity + affinityChange));
        affinityMap.put(npcKey, newAffinity);

        // 保存到VUP的tutorialFlagsJson
        saveAffinityMap(vup, affinityMap);

        // 技术弹幕解锁偷学
        if ("DANMAKU_TECHNICAL".equals(interactionType)) {
            markStealUnlocked(vup, npcKey);
        }

        // 生成对话
        String dialogue = generateDialogue(npcType, npcKey, newAffinity, interactionType);

        return new NpcInteractionResult(npcKey, npcType.displayName(), affinityChange, newAffinity, dialogue);
    }

    /**
     * 获取NPC对话。根据亲密度等级生成不同风格的对话。
     */
    public String getNpcDialogue(Vup vup, String npcKey, DaySession session) {
        NpcType npcType = NpcType.fromKey(npcKey);
        if (npcType == null) {
            return "...";
        }

        int affinity = getAffinity(vup, npcKey);

        // 使用确定性随机选择对话
        String seed = session.getRandomSeed();
        int hash = (seed + ":npc_dialogue:" + npcKey + ":" + affinity).hashCode();
        int index = Math.floorMod(Math.abs(hash), 3);

        return selectDialogue(npcKey, affinity, index);
    }

    /**
     * 计算亲密度变化。正面互动+5~+10，负面互动-3~-8。
     */
    private int calculateAffinityChange(RngLedger ledger, String npcKey, String interactionType) {
        int baseChange;
        boolean positive;

        switch (interactionType) {
            case "COLLAB", "CHAT", "GIFT", "PRAISE", "DANMAKU_PRAISE" -> {
                baseChange = 5 + ledger.nextInt("npc_affinity:" + npcKey, 6); // 5~10
                positive = true;
            }
            case "COMPETE", "IGNORE", "CRITICIZE", "INSULT", "DANMAKU_FLAME" -> {
                baseChange = 3 + ledger.nextInt("npc_affinity:" + npcKey, 6); // 3~8
                positive = false;
            }
            default -> {
                baseChange = 1 + ledger.nextInt("npc_affinity:" + npcKey, 3); // 1~3
                positive = true;
            }
        }

        return positive ? baseChange : -baseChange;
    }

    /**
     * 生成互动后的对话。
     */
    private String generateDialogue(NpcType npcType, String npcKey, int affinity, String interactionType) {
        String affinityDesc = affinityLabel(affinity);
        String name = npcType.displayName();

        return switch (interactionType) {
            case "COLLAB" -> name + "：" + selectDialogue(npcKey, affinity, 0);
            case "CHAT" -> name + "：" + selectDialogue(npcKey, affinity, 1);
            case "GIFT" -> name + "：" + selectDialogue(npcKey, affinity, 2);
            case "PRAISE" -> name + "：谢谢你的认可！" + affinityDesc + "的关系更近了一步。";
            case "COMPETE" -> name + "：哼，下次我不会输的。" + affinityDesc + "。";
            case "IGNORE" -> name + "：...（似乎有点失落）" + affinityDesc + "。";
            case "CRITICIZE" -> name + "：你说得对，我会改进的。" + affinityDesc + "。";
            case "INSULT" -> name + "：你这样说让我很难过。" + affinityDesc + "。";
            default -> name + "：" + selectDialogue(npcKey, affinity, 0);
        };
    }

    /**
     * 根据亲密度等级选择对话。
     */
    private String selectDialogue(String npcKey, int affinity, int index) {
        if (affinity < 0) {
            // 冷淡
            return switch (index) {
                case 0 -> "嗯。";
                case 1 -> "有事吗？";
                default -> "...";
            };
        } else if (affinity < 20) {
            // 普通
            return switch (index) {
                case 0 -> "你好，今天过得怎么样？";
                case 1 -> "最近在忙什么内容？";
                default -> "加油，继续努力。";
            };
        } else if (affinity < 50) {
            // 友好
            return switch (index) {
                case 0 -> "好久不见！最近怎么样？";
                case 1 -> "你的内容越来越好了，我很喜欢。";
                default -> "有机会一起联动吧！";
            };
        } else {
            // 亲密
            return switch (index) {
                case 0 -> "你来了！我一直在等你。";
                case 1 -> "我们是最好的搭档，对吧？";
                default -> "有什么需要帮忙的尽管说！";
            };
        }
    }

    /**
     * 获取亲密度等级标签。
     */
    private String affinityLabel(int affinity) {
        if (affinity < 0) {
            return "冷淡";
        } else if (affinity < 20) {
            return "普通";
        } else if (affinity < 50) {
            return "友好";
        } else {
            return "亲密";
        }
    }

    /**
     * 从VUP的tutorialFlagsJson中加载NPC亲密度数据。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadAffinityMap(Vup vup) {
        String json = vup.getTutorialFlagsJson();
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> flags = jsonService.readMap(json);
            Object npcAffinity = flags.get("npcAffinity");
            if (npcAffinity instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) npcAffinity);
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将NPC亲密度数据保存到VUP的tutorialFlagsJson。
     */
    private void saveAffinityMap(Vup vup, Map<String, Object> affinityMap) {
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
        flags.put("npcAffinity", affinityMap);
        vup.setTutorialFlagsJson(jsonService.write(flags));
        vupMapper.updateState(vup);
    }

    /**
     * 从VUP的tutorialFlagsJson中加载偷学解锁数据。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadStealUnlockMap(Vup vup) {
        String json = vup.getTutorialFlagsJson();
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> flags = jsonService.readMap(json);
            Object unlocked = flags.get("npcStealUnlocked");
            if (unlocked instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) unlocked);
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将偷学解锁数据保存到VUP的tutorialFlagsJson。
     */
    private void saveStealUnlockMap(Vup vup, Map<String, Object> unlockMap) {
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
        flags.put("npcStealUnlocked", unlockMap);
        vup.setTutorialFlagsJson(jsonService.write(flags));
        vupMapper.updateState(vup);
    }
}
