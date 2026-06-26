package com.example.vupworld.service.fan;

import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RivalProgressService {

    // 3个模拟竞争对手
    private static final String[] RIVAL_NAMES = {"星海あかり", "月城レン", "風間ミコ"};
    private static final String[] RIVAL_ROUTES = {"SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE"};
    private static final String[] RIVAL_STYLES = {"歌势稳健型", "切片出圈型", "黑红流量型"};

    public List<RivalDTO> getRivals(Vup vup) {
        int day = vup.getDayCount();
        List<RivalDTO> rivals = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            // 模拟竞争对手的增长曲线
            int baseFans = 30 + i * 20;
            int dailyGrowth = calculateRivalGrowth(i, day);
            int totalFans = baseFans + dailyGrowth * day;
            int heat = Math.min(80, 20 + day * 2 + i * 10);

            rivals.add(new RivalDTO(
                RIVAL_NAMES[i],
                RIVAL_ROUTES[i],
                RIVAL_STYLES[i],
                totalFans,
                heat,
                day,
                totalFans > vup.getFans()
            ));
        }
        return rivals;
    }

    private int calculateRivalGrowth(int index, int day) {
        // 不同竞争对手有不同的增长策略
        switch (index) {
            case 0: return 15 + day / 5; // 稳健增长
            case 1: return 10 + (day > 15 ? 20 : 0); // 后期爆发
            case 2: return 25 - day / 3; // 前期猛后期衰
            default: return 15;
        }
    }

    public static class RivalDTO {
        public final String name;
        public final String route;
        public final String style;
        public final int fans;
        public final int heat;
        public final int day;
        public final boolean ahead;

        public RivalDTO(String name, String route, String style, int fans, int heat, int day, boolean ahead) {
            this.name = name;
            this.route = route;
            this.style = style;
            this.fans = fans;
            this.heat = heat;
            this.day = day;
            this.ahead = ahead;
        }
    }
}
