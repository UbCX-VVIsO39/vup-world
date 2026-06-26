package com.example.vupworld.service.progression;

import com.example.vupworld.dto.MoodDtos.StrategyAdviceDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.risk.RiskDebtLabels;
import com.example.vupworld.service.risk.RiskDebtPresenter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 策略建议系统。根据VUP当前状态生成建议。
 */
@Component
public class StrategyAdviceService {
    private final RiskDebtMapper riskDebtMapper;
    private final BusinessLogMapper businessLogMapper;
    private final BalanceConfig balanceConfig;
    private final JsonService jsonService;

    public StrategyAdviceService(
            RiskDebtMapper riskDebtMapper,
            BusinessLogMapper businessLogMapper,
            BalanceConfig balanceConfig,
            JsonService jsonService
    ) {
        this.riskDebtMapper = riskDebtMapper;
        this.businessLogMapper = businessLogMapper;
        this.balanceConfig = balanceConfig;
        this.jsonService = jsonService;
    }

    /**
     * 获取策略建议列表。根据当前状态生成最多10条建议。
     */
    public List<StrategyAdviceDTO> getAdvice(Vup vup, DaySession session) {
        List<StrategyAdviceDTO> advice = new ArrayList<>();
        int currentDay = session.getDay();

        // 1. 口碑危机检查
        if (vup.getReputation() < 45) {
            advice.add(new StrategyAdviceDTO(
                    "URGENT",
                    "⚠️",
                    "口碑已经低于安全线，先用杂谈复盘或粉丝群维护稳住基本盘。",
                    "当前口碑" + vup.getReputation() + "，继续硬冲会让收益被口碑折价，也会让旧账更容易被翻出来。"
            ));
        }

        // 2. 债务到期检查
        List<RiskDebt> openDebts = riskDebtMapper.findOpenByVupId(vup.getId());
        openDebts.stream()
                .filter(debt -> debt.getDueDay() - currentDay <= 2 || debt.getSeverity() >= 4)
                .min(java.util.Comparator
                        .comparingInt((RiskDebt debt) -> Math.max(0, debt.getDueDay() - currentDay))
                        .thenComparing(java.util.Comparator.comparingInt(RiskDebt::getSeverity).reversed()))
                .ifPresent(debt -> advice.add(new StrategyAdviceDTO(
                        "URGENT",
                        "⏰",
                        "先拆最高风险旧账，再继续冲路线。",
                        RiskDebtLabels.debtTypeLabel(debt.getDebtType()) + "，严重度" + debt.getSeverity()
                                + "，" + RiskDebtPresenter.dueText(debt, currentDay)
                                + "；" + RiskDebtPresenter.sourceLine(debt)
                                + "。推荐：" + RiskDebtPresenter.recommendedAction(debt, currentDay)
                )));

        // 3. DD粉占比检查
        int totalFans = vup.getFans();
        if (totalFans > 0) {
            double ddRatio = (double) vup.getDdFans() / totalFans * 100;
            if (ddRatio > 40) {
                advice.add(new StrategyAdviceDTO(
                        "SUGGESTION",
                        "👥",
                        "DD流量偏厚，补一次自家节目或粉丝群维护，把过站流量沉淀下来。",
                        "DD粉占比" + String.format("%.1f", ddRatio) + "%，如果不补主体内容，社交路线会有车流但缺站牌。"
                ));
            }
        }

        // 4. 连续同行动检查
        int consecutiveDays = calculateConsecutiveDays(vup.getId());
        if (consecutiveDays >= 3) {
            advice.add(new StrategyAdviceDTO(
                    "SUGGESTION",
                    "🔄",
                    "连续同一行动开始疲劳，换一手能补同路线的相邻行动。",
                    "已连续" + consecutiveDays + "天执行相同行动，收益会变薄；优先选能接住当前路线、同时处理代价的行动。"
            ));
        }

        // 5. 体力不足检查
        if (vup.getStamina() < 3) {
            advice.add(new StrategyAdviceDTO(
                    "SUGGESTION",
                    "💤",
                    "体力不足，先休息或做低压维护，别把明天的高收益行动打断。",
                    "当前体力" + vup.getStamina() + "，低于安全线3；硬冲会牺牲后续收益，也更难处理旧账。"
            ));
        }

        // 6. 路线竞争检查
        Map<String, Object> routeScoreMap = parseRouteScore(vup.getRouteScoreJson());
        if (!routeScoreMap.isEmpty()) {
            int maxScore = 0;
            int secondMaxScore = 0;
            for (Object value : routeScoreMap.values()) {
                if (value instanceof Number n) {
                    int score = n.intValue();
                    if (score > maxScore) {
                        secondMaxScore = maxScore;
                        maxScore = score;
                    } else if (score > secondMaxScore) {
                        secondMaxScore = score;
                    }
                }
            }
            if (maxScore > 0 && maxScore - secondMaxScore < 3) {
                advice.add(new StrategyAdviceDTO(
                        "TIP",
                        "🎯",
                        "路线还没拉开差距，下一手选路线专属行动加厚证据。",
                        "最高路线分" + maxScore + "，仅领先第二名" + (maxScore - secondMaxScore) + "分；摇摆会让日报证据互相抵消。"
                ));
            }
        }

        // 7. 热度偏低检查
        if (vup.getWatchHeat() < 20) {
            advice.add(new StrategyAdviceDTO(
                    "TIP",
                    "🔥",
                    "围观偏低，可以用直播、视频或切片补一个可传播入口。",
                    "当前围观热度" + vup.getWatchHeat() + "，低于20；稳线可以先补作品证据，冲线要同时看旧账窗口。"
            ));
        }

        // 8. 梗浓度检查
        if (vup.getMemeLevel() > 60) {
            advice.add(new StrategyAdviceDTO(
                    "TIP",
                    "😂",
                    "梗浓度偏高，换新素材或补正片，避免被同一个梗反向定义。",
                    "当前梗浓度" + vup.getMemeLevel() + "，超过60；继续复读会堆围观和旧账，不一定涨粉。"
            ));
        }

        // 9. 收官阶段检查
        if (currentDay >= 25) {
            advice.add(new StrategyAdviceDTO(
                    "TIP",
                    "🏁",
                    "收官阶段，优先补能进结局复盘的路线证据，顺手清最高风险旧账。",
                    "当前第" + currentDay + "天，距离结局还剩" + Math.max(0, balanceConfig.maxDay() - currentDay)
                            + "天；最后几天的收益要能解释路线，代价也会被最终复盘引用。"
            ));
        }

        return advice;
    }

    /**
     * 计算连续相同行动的天数。
     */
    private int calculateConsecutiveDays(Long vupId) {
        var recentLogs = businessLogMapper.findRecentByVupId(vupId, 10);
        if (recentLogs.isEmpty()) {
            return 0;
        }

        String currentAction = null;
        for (var log : recentLogs) {
            if (log.isEndingRefFlag() && log.getAction() != null) {
                currentAction = log.getAction();
                break;
            }
        }
        if (currentAction == null) {
            return 0;
        }

        return businessLogMapper.countConsecutiveSameAction(vupId, currentAction);
    }

    /**
     * 解析路线分数JSON。
     */
    private Map<String, Object> parseRouteScore(String routeScoreJson) {
        if (routeScoreJson == null || routeScoreJson.isBlank()) {
            return Map.of();
        }
        try {
            return jsonService.readMap(routeScoreJson);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
