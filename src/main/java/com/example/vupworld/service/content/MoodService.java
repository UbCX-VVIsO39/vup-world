package com.example.vupworld.service.content;

import com.example.vupworld.dto.MoodDtos.MoodResult;
import com.example.vupworld.dto.MoodDtos.MoodType;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.operating.OperatingPressureService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * VUP心情系统。根据当前状态和历史数据计算VUP心情。
 */
@Component
public class MoodService {
    private final BusinessLogMapper businessLogMapper;
    private final RiskDebtMapper riskDebtMapper;
    private final JsonService jsonService;
    private final OperatingPressureService operatingPressureService;

    public MoodService(
            BusinessLogMapper businessLogMapper,
            RiskDebtMapper riskDebtMapper,
            JsonService jsonService,
            OperatingPressureService operatingPressureService
    ) {
        this.businessLogMapper = businessLogMapper;
        this.riskDebtMapper = riskDebtMapper;
        this.jsonService = jsonService;
        this.operatingPressureService = operatingPressureService;
    }

    /** 心情台词池 */
    private static final String[] MOOD_LINES = {
            "今天状态不错，想搞点事情。",
            "稍微有点累，但还能撑住。",
            "感觉今天能出好活。",
            "有点紧张，不知道观众反应会怎样。",
            "灵感涌上来了，必须抓住。",
            "今天想休息一下，调整状态。",
            "心情很好，和大家聊聊天吧。",
            "压力有点大，但这就是VUP的日常。",
            "今天运气不错，做什么都顺。",
            "有点迷茫，不确定该走哪条路。",
            "精神饱满，准备迎接挑战。",
            "粉丝的支持让我充满动力。",
            "今天想尝试一些新东西。",
            "有点疲惫，但不想停下来。",
            "感觉直播间气氛会很好。"
    };

    /**
     * 计算VUP当前心情。
     * 根据昨天的行动、状态指标和历史数据综合判断。
     */
    public MoodResult getMood(Vup vup, DaySession session) {
        int currentDay = session.getDay();

        // 检查是否有OPEN债务
        boolean hasOpenDebt = !riskDebtMapper.findOpenByVupId(vup.getId()).isEmpty();

        // 检查昨天是否休息
        boolean restedYesterday = false;
        if (currentDay > 1) {
            BusinessLog yesterdayLog = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), currentDay - 1);
            restedYesterday = yesterdayLog != null && "REST".equals(yesterdayLog.getAction());
        }

        // 检查粉丝增长和口碑变化
        int fanGrowthYesterday = 0;
        int reputationGrowthYesterday = 0;
        if (currentDay > 1) {
            List<BusinessLog> yesterdayLogs = businessLogMapper.findByVupIdAndDay(vup.getId(), currentDay - 1);
            for (BusinessLog log : yesterdayLogs) {
                fanGrowthYesterday += log.getFanChange();
                reputationGrowthYesterday += log.getReputationChange();
            }
        }

        // 检查连续同行动天数
        int consecutiveDays = calculateConsecutiveSameActionDays(vup.getId(), currentDay);

        // Phase 5: Check pressure overload for mood integration
        String pressureEffect = operatingPressureService.pressureMoodEffect(vup, currentDay);

        // 心情判定（按优先级从高到低）
        MoodType mood;
        String effect;

        if (restedYesterday || vup.getStamina() > 8) {
            mood = MoodType.ENERGIZED;
            effect = "体力充沛，行动效果+10%";
        } else if (fanGrowthYesterday > 10 || reputationGrowthYesterday > 3) {
            mood = MoodType.HAPPY;
            effect = "心情愉悦，粉丝增长+5%";
        } else if (pressureEffect != null) {
            mood = MoodType.STRESSED;
            effect = pressureEffect;
        } else if (hasOpenDebt || vup.getReputation() < 45) {
            mood = MoodType.STRESSED;
            effect = "压力较大，行动失败率+15%";
        } else if (consecutiveDays >= 5) {
            mood = MoodType.TIRED;
            effect = "行动疲劳，产出效率-20%";
        } else if (vup.getWatchHeat() > 60 || vup.getMemeLevel() > 50) {
            mood = MoodType.EXCITED;
            effect = "状态火热，围观增长+15%";
        } else {
            mood = MoodType.NEUTRAL;
            effect = "状态正常，无额外效果";
        }

        // 确定性选择心情台词
        String seed = session.getRandomSeed();
        int hash = (seed + ":mood:" + mood.name()).hashCode();
        int index = Math.floorMod(Math.abs(hash), MOOD_LINES.length);
        String moodLine = MOOD_LINES[index];

        return new MoodResult(mood, moodLine, effect);
    }

    /**
     * 计算连续相同行动的天数。
     * 从最近的记录开始，向前检查连续使用同一行动的天数。
     */
    private int calculateConsecutiveSameActionDays(Long vupId, int currentDay) {
        // 获取最近的有参考价值的业务日志
        List<BusinessLog> recentLogs = businessLogMapper.findRecentByVupId(vupId, 10);
        if (recentLogs.isEmpty()) {
            return 0;
        }

        // 找到最近的ending_ref_flag为true的日志，确定当前行动类型
        String currentAction = null;
        for (BusinessLog log : recentLogs) {
            if (log.isEndingRefFlag() && log.getAction() != null) {
                currentAction = log.getAction();
                break;
            }
        }
        if (currentAction == null) {
            return 0;
        }

        // 使用mapper方法计算连续天数
        return businessLogMapper.countConsecutiveSameAction(vupId, currentAction);
    }
}
