package com.example.vupworld.service.progression;

import com.example.vupworld.mapper.GameStatsHistoryMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.GameStatsHistory;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.infra.JsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CareerService {

    private static final Logger log = LoggerFactory.getLogger(CareerService.class);
    private final VupService vupService;
    private final VupMapper vupMapper;
    private final JsonService jsonService;
    private final GameStatsHistoryMapper gameStatsHistoryMapper;

    public CareerService(VupService vupService, VupMapper vupMapper, JsonService jsonService,
                         GameStatsHistoryMapper gameStatsHistoryMapper) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.jsonService = jsonService;
        this.gameStatsHistoryMapper = gameStatsHistoryMapper;
    }

    // 生涯模式：3局连续，每局30天
    public static final int CAREER_RUNS = 3;
    public static final int DAYS_PER_RUN = 30;

    // 生涯阶段目标
    private static final Map<Integer, CareerGoal> CAREER_GOALS = new LinkedHashMap<>();
    static {
        CAREER_GOALS.put(1, new CareerGoal("出道期", 1000, "找到自己的路线，积累第一批粉丝"));
        CAREER_GOALS.put(2, new CareerGoal("成长期", 5000, "扩大影响力，建立粉丝基础"));
        CAREER_GOALS.put(3, new CareerGoal("巅峰期", 20000, "成为头部VTuber，达成最终结局"));
    }

    // 获取生涯进度
    public CareerProgress getProgress(Long userId) {
        // 从game_stats_history表查询已完成的局数和历史记录
        int completedRuns = 0;
        int historicalFans = 0;
        String lastEnding = null;
        try {
            completedRuns = gameStatsHistoryMapper.countByUserId(userId);
            if (completedRuns > 0) {
                List<GameStatsHistory> history = gameStatsHistoryMapper.findByUserId(userId);
                if (history != null && !history.isEmpty()) {
                    historicalFans = history.get(0).getFinalFans();
                    lastEnding = history.get(0).getEndingType();
                }
            }
        } catch (Exception e) {
            log.warn("查询生涯历史失败 userId={}: {}", userId, e.getMessage());
        }

        // 检查是否有进行中的VUP
        Vup activeVup = null;
        try {
            activeVup = vupMapper.findActiveByUserId(userId);
        } catch (Exception ignored) {}

        int currentRun = Math.min(CAREER_RUNS, completedRuns + 1);
        int totalFans = 0;
        String currentGoal = CAREER_GOALS.get(1).description;

        if (activeVup != null) {
            totalFans = activeVup.getFans();
        } else if (completedRuns > 0) {
            // 没有活跃VUP但有历史记录时，使用最近一次的粉丝数
            totalFans = historicalFans;
            currentRun = Math.min(CAREER_RUNS, completedRuns);
        }

        CareerGoal goal = CAREER_GOALS.get(currentRun);
        if (goal != null) currentGoal = goal.description;

        return new CareerProgress(
            CAREER_RUNS,
            currentRun,
            completedRuns,
            totalFans,
            currentGoal,
            CAREER_GOALS.get(currentRun) != null ? CAREER_GOALS.get(currentRun).fanTarget : 1000,
            buildTimeline(completedRuns)
        );
    }

    // 计算生涯继承加成
    public CareerInheritance calculateInheritance(int completedRuns, String lastEnding) {
        double fanInheritance = Math.min(0.2, 0.05 + completedRuns * 0.05); // 5%-20%
        double abilityInheritance = Math.min(0.15, completedRuns * 0.05); // 0%-15%
        String bonusDescription = "";

        if (lastEnding != null) {
            switch (lastEnding) {
                case "SINGING_IDOL": bonusDescription = "歌势底蕴：练歌收益+15%"; break;
                case "SLICE_SAINT": bonusDescription = "切片直觉：切片传播+20%"; break;
                case "BLACK_RED_MAIN_STAGE": bonusDescription = "抗压体质：债务severity-1"; break;
                case "SOCIAL_COLLAB": bonusDescription = "人脉广：联动冷却-1天"; break;
                case "DANCE_MEME": bonusDescription = "节奏感：舞蹈消耗-1体力"; break;
                case "ELECTRONIC_PICKLE": bonusDescription = "老粉来信：开局真粉+10"; break;
                default: bonusDescription = "经验值：所有收益+5%"; break;
            }
        }

        return new CareerInheritance(fanInheritance, abilityInheritance, bonusDescription);
    }

    private List<Map<String, Object>> buildTimeline(int completedRuns) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 1; i <= completedRuns; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("run", i);
            entry.put("label", CAREER_GOALS.get(i).label);
            entry.put("status", "completed");
            timeline.add(entry);
        }
        return timeline;
    }

    public static class CareerGoal {
        public final String label;
        public final int fanTarget;
        public final String description;
        public CareerGoal(String label, int fanTarget, String description) {
            this.label = label;
            this.fanTarget = fanTarget;
            this.description = description;
        }
    }

    public static class CareerProgress {
        public final int totalRuns;
        public final int currentRun;
        public final int completedRuns;
        public final int totalFans;
        public final String currentGoal;
        public final int fanTarget;
        public final List<Map<String, Object>> timeline;
        public CareerProgress(int totalRuns, int currentRun, int completedRuns, int totalFans,
                            String currentGoal, int fanTarget, List<Map<String, Object>> timeline) {
            this.totalRuns = totalRuns;
            this.currentRun = currentRun;
            this.completedRuns = completedRuns;
            this.totalFans = totalFans;
            this.currentGoal = currentGoal;
            this.fanTarget = fanTarget;
            this.timeline = timeline;
        }
    }

    public static class CareerInheritance {
        public final double fanInheritance;
        public final double abilityInheritance;
        public final String bonusDescription;
        public CareerInheritance(double fanInheritance, double abilityInheritance, String bonusDescription) {
            this.fanInheritance = fanInheritance;
            this.abilityInheritance = abilityInheritance;
            this.bonusDescription = bonusDescription;
        }
    }
}
