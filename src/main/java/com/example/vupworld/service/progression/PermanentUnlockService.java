package com.example.vupworld.service.progression;

import com.example.vupworld.dto.MoodDtos.PermanentUnlockDTO;
import com.example.vupworld.mapper.GameUnlockMapper;
import com.example.vupworld.model.GameUnlock;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 永久解锁系统。管理结局解锁的能力和加成。
 */
@Component
public class PermanentUnlockService {
    private final GameUnlockMapper gameUnlockMapper;

    /** 结局解锁定义：endingType -> (key, label, description, effect) */
    private static final Map<String, Object> EMPTY_EFFECT = Map.of();
    private static final Map<String, UnlockDefinition> UNLOCK_DEFINITIONS = new LinkedHashMap<>();

    static {
        // 注：以下 effect 仅填充 applyEffect 已支持的固定值加成 key。
        // 比例型加成（bonusClipOutput 切片产出、bonusCollabEffect 联动效果）需在对应业务逻辑中接入，
        // 当前接入点不明确，待后续在切片产出与联动结算模块中统一接入。

        UNLOCK_DEFINITIONS.put("ELECTRONIC_PICKLE", new UnlockDefinition(
                "ELECTRONIC_PICKLE_ENDING",
                "饭点固定席",
                "电子榨菜结局解锁：图鉴记录「稳健基础路线」打法提示，开局可回顾前期节奏要点。开局获得少量人气与灵感，助力平稳起步。",
                // 稳健基础路线：小幅人气与灵感，帮助新一轮平稳起步；数值保守避免破坏早期平衡
                EMPTY_EFFECT,
                List.of()
        ));
        UNLOCK_DEFINITIONS.put("GLORIOUS_GRADUATION", new UnlockDefinition(
                "GLORIOUS_GRADUATION_ENDING",
                "体面谢幕卡",
                "光荣毕业结局解锁：图鉴记录「体面收束」路线提示，了解何时该优雅退场。开局获得口碑与少量死忠粉继承。",
                // 体面收束路线：毕业留下好口碑与少量死忠粉，体现“体面退场”的遗产
                EMPTY_EFFECT,
                List.of("GRADUATION_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("SINGING_IDOL", new UnlockDefinition(
                "SINGING_IDOL_ENDING",
                "遗珠返场票",
                "歌势遗珠结局解锁：图鉴记录「唱歌偶像」打法提示，掌握歌力培养与粉丝转化节奏。开局获得歌力与真粉加成。",
                // 歌势偶像路线：强化歌力，并继承一批被歌声吸引的真粉
                EMPTY_EFFECT,
                List.of()
        ));
        UNLOCK_DEFINITIONS.put("SLICE_SAINT", new UnlockDefinition(
                "SLICE_SAINT_ENDING",
                "切片组续约",
                "切片圣体结局解锁：图鉴记录「切片/梗舞」打法提示，了解切片产出与二创传播节奏。开局获得梗等级与舞力加成。",
                // 切片/梗舞路线：提升梗等级（二创传播力）与舞力，契合切片圣体主题
                EMPTY_EFFECT,
                List.of("DANCE_MEME_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("BLACK_RED_MAIN_STAGE", new UnlockDefinition(
                "BLACK_RED_MAIN_STAGE_ENDING",
                "主会场余温",
                "黑红顶流结局解锁：图鉴记录「主会场」打法提示，掌握黑红流量与口碑平衡策略。开局继承争议流量带来的独角兽粉与观看热度。",
                // 黑红流量路线：带来独角兽粉（黑红死忠）与观看热度，体现争议流量的双面性
                EMPTY_EFFECT,
                List.of("BLACK_RED_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("MAIN_STAGE_KING", new UnlockDefinition(
                "MAIN_STAGE_KING_ENDING",
                "录播硬盘备份",
                "主会场之王结局解锁：图鉴记录「高阶控场」打法提示，了解多维能力协同运营策略。开局获得全能力小幅加成，体现多维协同。",
                // 高阶控场（传奇结局）：全能力小幅协同提升，作为最强结局的综合加成；单项保持低位避免破坏平衡
                EMPTY_EFFECT,
                List.of("LEGEND_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("CYBER_GIRLFRIEND", new UnlockDefinition(
                "CYBER_GIRLFRIEND_ENDING",
                "陪伴感回声",
                "赛博女友结局解锁：图鉴记录「陪伴边界」打法提示，了解情感投入与理性运营的平衡点。开局继承高粘性死忠粉与口碑。",
                // 陪伴边界路线：陪伴感积累高粘性真粉与良好口碑，契合赛博女友主题
                EMPTY_EFFECT,
                List.of("CYBER_FAN_SERVICE_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("DD_BUS_STOP", new UnlockDefinition(
                "DD_BUS_STOP_ENDING",
                "站台回流票",
                "DD公交站结局解锁：图鉴记录「社交联动」打法提示，了解跨圈联动与粉丝留存策略。开局继承跨圈联动的DD粉与人气。",
                // 社交联动路线：跨圈联动带来DD粉（多推粉丝）与人气提升
                EMPTY_EFFECT,
                List.of("SOCIAL_COLLAB_ENDING")
        ));
        UNLOCK_DEFINITIONS.put("UNKNOWN", new UnlockDefinition(
                "UNKNOWN_ENDING",
                "复活赛档案",
                "查无此V结局解锁：图鉴记录隐藏路线提示，为下一轮探索保留线索。开局将隐藏线索转化为灵感与少量观看热度。",
                // 隐藏路线：将探索线索转化为灵感（最高单项），辅以少量热度；数值侧重灵感体现“线索”主题
                EMPTY_EFFECT,
                List.of()
        ));
    }

    public PermanentUnlockService(GameUnlockMapper gameUnlockMapper, JsonService jsonService) {
        this.gameUnlockMapper = gameUnlockMapper;
    }

    public List<String> currentEndingTypes() {
        return List.copyOf(UNLOCK_DEFINITIONS.keySet());
    }

    public boolean isCurrentEndingType(String endingType) {
        return UNLOCK_DEFINITIONS.containsKey(canonicalEndingType(endingType, ""));
    }

    public String normalizeEndingType(String endingType) {
        return canonicalEndingType(endingType, "");
    }

    /**
     * 获取用户的永久解锁列表。
     */
    public List<PermanentUnlockDTO> getUnlocks(Long userId) {
        List<GameUnlock> unlocks = gameUnlockMapper.findByUserId(userId);
        List<PermanentUnlockDTO> result = new ArrayList<>();
        Set<String> seenEndingTypes = new LinkedHashSet<>();

        for (GameUnlock unlock : unlocks) {
            String endingType = canonicalEndingType(unlock.getSourceEndingType(), unlock.getUnlockKey());
            if (!seenEndingTypes.add(endingType)) {
                continue;
            }
            UnlockDefinition def = UNLOCK_DEFINITIONS.get(endingType);
            if (def != null) {
                result.add(new PermanentUnlockDTO(
                        def.key(),
                        def.label(),
                        def.description(),
                        endingType,
                        def.effect()
                ));
            }
        }

        return result;
    }

    /**
     * 结局时调用，写入game_unlock表。
     */
    @Transactional
    public void unlock(Long userId, String endingType) {
        String sourceEndingType = canonicalEndingType(endingType, "");
        UnlockDefinition def = UNLOCK_DEFINITIONS.get(sourceEndingType);
        if (def == null) {
            return;
        }

        // 检查是否已解锁
        boolean exists = unlockKeys(def).stream()
                .anyMatch(key -> gameUnlockMapper.existsByUserAndKey(userId, "PERMANENT_UNLOCK", key));
        if (exists) {
            return;
        }

        GameUnlock unlock = new GameUnlock();
        unlock.setUserId(userId);
        unlock.setUnlockType("PERMANENT_UNLOCK");
        unlock.setUnlockKey(def.key());
        unlock.setSourceEndingType(sourceEndingType);
        unlock.setSourceRunScore(0);
        gameUnlockMapper.insert(unlock);
    }

    /**
     * 创建VUP时调用，应用解锁加成。
     */
    public void applyBonuses(Long userId, Vup vup) {
        List<GameUnlock> unlocks = gameUnlockMapper.findByUserId(userId);
        Set<String> appliedEndingTypes = new LinkedHashSet<>();

        for (GameUnlock unlock : unlocks) {
            String endingType = canonicalEndingType(unlock.getSourceEndingType(), unlock.getUnlockKey());
            if (!appliedEndingTypes.add(endingType)) {
                continue;
            }
            UnlockDefinition def = UNLOCK_DEFINITIONS.get(endingType);
            if (def == null) {
                continue;
            }

            Map<String, Object> effect = def.effect();
            applyEffect(vup, effect);
        }
    }

    /**
     * 将解锁效果应用到VUP上。
     */
    private void applyEffect(Vup vup, Map<String, Object> effect) {
        for (Map.Entry<String, Object> entry : effect.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "bonusFans" -> {
                    if (value instanceof Number n) {
                        int bonus = n.intValue();
                        vup.setFans(vup.getFans() + bonus);
                        vup.setTrueFans(vup.getTrueFans() + bonus);
                    }
                }
                case "bonusTrueFans" -> {
                    if (value instanceof Number n) {
                        vup.setTrueFans(vup.getTrueFans() + n.intValue());
                    }
                }
                case "bonusPopularity" -> {
                    if (value instanceof Number n) {
                        vup.setPopularity(vup.getPopularity() + n.intValue());
                    }
                }
                case "bonusInspiration" -> {
                    if (value instanceof Number n) {
                        vup.setInspiration(vup.getInspiration() + n.intValue());
                    }
                }
                case "bonusWatchHeat" -> {
                    if (value instanceof Number n) {
                        vup.setWatchHeat(vup.getWatchHeat() + n.intValue());
                    }
                }
                case "bonusReputation" -> {
                    if (value instanceof Number n) {
                        vup.setReputation(vup.getReputation() + n.intValue());
                    }
                }
                case "bonusUnicornFans" -> {
                    if (value instanceof Number n) {
                        vup.setUnicornFans(vup.getUnicornFans() + n.intValue());
                        vup.setFans(vup.getFans() + n.intValue());
                    }
                }
                case "bonusDdFans" -> {
                    if (value instanceof Number n) {
                        vup.setDdFans(vup.getDdFans() + n.intValue());
                        vup.setFans(vup.getFans() + n.intValue());
                    }
                }
                case "bonusSongPower" -> {
                    if (value instanceof Number n) {
                        vup.setSongPower(vup.getSongPower() + n.intValue());
                    }
                }
                case "bonusDancePower" -> {
                    if (value instanceof Number n) {
                        vup.setDancePower(vup.getDancePower() + n.intValue());
                    }
                }
                case "bonusMemeLevel" -> {
                    if (value instanceof Number n) {
                        vup.setMemeLevel(vup.getMemeLevel() + n.intValue());
                    }
                }
                // 比例型加成（如bonusClipOutput, bonusCollabEffect）需要在具体业务逻辑中处理
                default -> {
                    // 其他效果暂不处理
                }
            }
        }
    }

    /**
     * 解锁定义。
     */
    private record UnlockDefinition(
            String key,
            String label,
            String description,
            Map<String, Object> effect,
            List<String> legacyKeys
    ) {
    }

    private List<String> unlockKeys(UnlockDefinition def) {
        List<String> keys = new ArrayList<>();
        keys.add(def.key());
        keys.addAll(def.legacyKeys());
        return keys;
    }

    private String canonicalEndingType(String sourceEndingType, String unlockKey) {
        String type = sourceEndingType == null ? "" : sourceEndingType.trim().toUpperCase();
        if (type.isBlank()) {
            type = endingTypeFromUnlockKey(unlockKey);
        }
        return switch (type) {
            case "LEGEND" -> "MAIN_STAGE_KING";
            case "GRADUATION" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE" -> "CYBER_GIRLFRIEND";
            case "DANCE_MEME" -> "SLICE_SAINT";
            case "SOCIAL_COLLAB" -> "DD_BUS_STOP";
            default -> type;
        };
    }

    private String endingTypeFromUnlockKey(String unlockKey) {
        if (unlockKey == null || unlockKey.isBlank()) {
            return "";
        }
        return switch (unlockKey.trim().toUpperCase()) {
            case "LEGEND_ENDING" -> "MAIN_STAGE_KING";
            case "GRADUATION_ENDING" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED_ENDING" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE_ENDING" -> "CYBER_GIRLFRIEND";
            case "DANCE_MEME_ENDING" -> "SLICE_SAINT";
            case "SOCIAL_COLLAB_ENDING" -> "DD_BUS_STOP";
            default -> unlockKey.trim().toUpperCase().replace("_ENDING", "");
        };
    }
}
