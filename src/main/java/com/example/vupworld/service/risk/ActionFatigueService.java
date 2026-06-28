package com.example.vupworld.service.risk;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.dto.ActionDtos.FatigueInfoDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActionFatigueService {

    private final BusinessLogMapper businessLogMapper;
    private final BalanceConfig balanceConfig;

    public ActionFatigueService(BusinessLogMapper businessLogMapper, BalanceConfig balanceConfig) {
        this.businessLogMapper = businessLogMapper;
        this.balanceConfig = balanceConfig;
    }

    public FatigueInfoDTO calculateFatigue(Long vupId, String actionType) {
        if ("REST".equals(actionType)) {
            return new FatigueInfoDTO(0, 100, null);
        }
        int consecutive = businessLogMapper.countConsecutiveSameAction(vupId, actionType);
        int crossCategoryFatigue = calculateCrossCategoryFatigue(vupId, actionType);
        int effectiveConsecutive = Math.max(consecutive, crossCategoryFatigue);
        int threshold = balanceConfig.actionFatigueConsecutiveThreshold();

        if (effectiveConsecutive < threshold) {
            return new FatigueInfoDTO(effectiveConsecutive, 100, null);
        }

        int fatiguePercent = fatiguePercent(effectiveConsecutive);
        String hint = effectiveConsecutive > consecutive
                ? crossCategoryHint(effectiveConsecutive, actionType)
                : fatigueHint(effectiveConsecutive, actionType);
        return new FatigueInfoDTO(effectiveConsecutive, fatiguePercent, hint);
    }

    public int applyFatigue(int baseValue, FatigueInfoDTO fatigueInfo) {
        if (fatigueInfo == null || fatigueInfo.fatiguePercent() >= 100) {
            return baseValue;
        }
        return baseValue * fatigueInfo.fatiguePercent() / 100;
    }

    private int fatiguePercent(int consecutive) {
        if (consecutive >= 10) {
            return balanceConfig.actionFatiguePercentAfter10Plus();
        }
        if (consecutive >= 7) {
            return balanceConfig.actionFatiguePercentAfter7();
        }
        return balanceConfig.actionFatiguePercentAfter5();
    }

    private String fatigueHint(int consecutive, String actionType) {
        String label = actionLabel(actionType);
        if (consecutive >= 10) {
            return label + "连续做了" + consecutive + "天，观众审美严重疲劳，收益大幅下降。";
        }
        if (consecutive >= 7) {
            return label + "连续做了" + consecutive + "天，观众开始觉得重复，收益明显下降。";
        }
        return label + "连续做了" + consecutive + "天，收益略有下降，建议换换内容。";
    }

    /**
     * 创作类行动共享疲劳：练歌/练舞/杂谈/发视频/发切片互相关联。
     * 连续创作不同子类也会产生疲劳，但阈值更高。
     */
    private int calculateCrossCategoryFatigue(Long vupId, String actionType) {
        List<String> sameCategory = creativeCategoryFor(actionType);
        if (sameCategory.size() <= 1) {
            return 0;
        }
        // Check recent logs for any action in the same category
        var recentLogs = businessLogMapper.findRecentByVupId(vupId, 6);
        int consecutive = 0;
        for (var log : recentLogs) {
            if (log.getAction() != null && sameCategory.contains(log.getAction())) {
                consecutive++;
            } else if (!"REST".equals(log.getAction())) {
                break;
            }
        }
        // Cross-category fatigue has 1.5x threshold
        return consecutive >= (int)(balanceConfig.actionFatigueConsecutiveThreshold() * 1.5) ? consecutive : 0;
    }

    private List<String> creativeCategoryFor(String actionType) {
        return switch (actionType) {
            case "TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK" ->
                List.of("TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK");
            case "PUBLISH_VIDEO", "PUBLISH_CLIP" ->
                List.of("PUBLISH_VIDEO", "PUBLISH_CLIP");
            case "STREAM_PLAN" ->
                List.of("STREAM_PLAN");
            default -> List.of(actionType);
        };
    }

    private String crossCategoryHint(int consecutive, String actionType) {
        String label = actionLabel(actionType);
        return label + "所在类连续做了" + consecutive + "天，内容类型开始重复，建议换个完全不同的行动。";
    }

    private String actionLabel(String actionType) {
        return switch (actionType) {
            case "TRAIN_SONG" -> "练歌";
            case "TRAIN_DANCE" -> "练舞";
            case "TRAIN_TALK" -> "杂谈复盘";
            case "STREAM_PLAN" -> "直播企划";
            case "PUBLISH_VIDEO" -> "发布视频";
            case "PUBLISH_CLIP" -> "发布切片";
            case "FAN_GROUP_MAINTAIN" -> "粉丝群维护";
            case "NPC_INTERACT" -> "同台互动";
            case "REST" -> "休息";
            default -> "行动";
        };
    }
}
