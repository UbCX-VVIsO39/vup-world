package com.example.vupworld.service.risk;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.dto.ActionDtos.FatigueInfoDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import org.springframework.stereotype.Service;

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
        int threshold = balanceConfig.actionFatigueConsecutiveThreshold();

        if (consecutive < threshold) {
            return new FatigueInfoDTO(consecutive, 100, null);
        }

        int fatiguePercent = fatiguePercent(consecutive);
        String hint = fatigueHint(consecutive, actionType);
        return new FatigueInfoDTO(consecutive, fatiguePercent, hint);
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
