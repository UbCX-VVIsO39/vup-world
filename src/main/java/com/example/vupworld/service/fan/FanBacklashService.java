package com.example.vupworld.service.fan;

import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.DeterministicRngService;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FanBacklashService {

    private final DeterministicRngService rngService;

    public FanBacklashService(DeterministicRngService rngService) {
        this.rngService = rngService;
    }

    // 检查是否触发脱粉回踩，按粉丝类型差异化判定
    public Optional<BacklashResult> checkBacklash(Vup vup) {
        int trueFans = vup.getTrueFans();
        int funFans = vup.getFunFans();
        int unicornFans = vup.getUnicornFans();
        int ddFans = vup.getDdFans();
        int totalFans = trueFans + funFans + unicornFans + ddFans;

        // 条件1：口碑过低——全局性前粉丝回踩（保留原逻辑）
        if (vup.getReputation() < 30) {
            return Optional.of(new BacklashResult(
                "LOW_REPUTATION",
                "前粉丝回踩",
                "你的口碑太低了。一些曾经支持你的粉丝开始在网上发布负面评价。他们说\"我以前真的很喜欢TA，但现在变了\"。",
                10, -5, 15, -3, 0, 0, -2
            ));
        }

        // 没有粉丝时不触发分群反噬
        if (totalFans <= 0) {
            return Optional.empty();
        }

        double unicornRatio = (double) unicornFans / totalFans;
        double ddRatio = (double) ddFans / totalFans;
        double funRatio = (double) funFans / totalFans;

        // 条件2：独角兽粉反噬——独角兽占比过高且做了联动/DD相关行动
        // commercialLevel 反映近期商业化/联动动作的累积感知，作为联动DD行动的状态代理
        if (unicornRatio > 0.40 && vup.getCommercialLevel() >= 8) {
            return Optional.of(new BacklashResult(
                "UNICORN_BACKLASH",
                "独角兽粉反噬",
                "独角兽粉占比过高，最近的联动和商业化动作让他们感到陪伴边界被侵犯。群里开始流传\"主播以前不是这样的\"，有人把互动秒数做成了表格。",
                5, -10, -5, -2, 0, -8, 0
            ));
        }

        // 条件3：DD粉过站流失——DD占比高且连续无新内容（围观热度低作为内容断档代理）
        if (ddRatio > 0.35 && vup.getWatchHeat() < 25) {
            return Optional.of(new BacklashResult(
                "DD_TRANSIT_LOSS",
                "DD粉过站流失",
                "DD粉占比高，最近又没有新内容留住他们。他们像坐过路车一样陆续下车，灯牌亮了又灭，粉丝群只剩下\"下次再来\"。",
                -8, -2, -12, 0, 0, 0, -10
            ));
        }

        // 条件4：乐子粉节目效果诉求——乐子粉占比高且梗浓度低
        if (funRatio > 0.45 && vup.getMemeLevel() < 20) {
            return Optional.of(new BacklashResult(
                "FUN_FAN_BORED",
                "乐子粉嫌节目效果不够",
                "乐子粉占比高但梗浓度太低，他们开始觉得无聊。切片组没有素材可剪，弹幕稀稀拉拉，围观热度肉眼可见地往下掉。",
                -10, -2, -6, 0, -5, 0, 0
            ));
        }

        // 条件5：真粉流失（保留原逻辑）
        if (trueFans < 20) {
            return Optional.of(new BacklashResult(
                "FAN_EXODUS",
                "粉丝大流失",
                "你的核心粉丝大批流失。有人在粉丝群里说\"我已经脱粉了，大家保重\"。更多人开始动摇。",
                15, -8, 20, -10, 0, 0, 0
            ));
        }

        return Optional.empty();
    }

    public static class BacklashResult {
        public final String key;
        public final String title;
        public final String description;
        public final int heatChange;        // 围观热度变化
        public final int reputationChange;  // 口碑变化
        public final int popularityChange;  // 人气变化
        public final int trueFanChange;     // 真粉变化
        public final int funFanChange;      // 乐子粉变化
        public final int unicornFanChange;  // 独角兽粉变化
        public final int ddFanChange;       // DD粉变化

        // 保留旧构造器，向后兼容：粉丝变化默认为0
        public BacklashResult(String key, String title, String description,
                            int heatChange, int reputationChange, int popularityChange) {
            this(key, title, description, heatChange, reputationChange, popularityChange, 0, 0, 0, 0);
        }

        public BacklashResult(String key, String title, String description,
                            int heatChange, int reputationChange, int popularityChange,
                            int trueFanChange, int funFanChange, int unicornFanChange, int ddFanChange) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.heatChange = heatChange;
            this.reputationChange = reputationChange;
            this.popularityChange = popularityChange;
            this.trueFanChange = trueFanChange;
            this.funFanChange = funFanChange;
            this.unicornFanChange = unicornFanChange;
            this.ddFanChange = ddFanChange;
        }
    }
}
