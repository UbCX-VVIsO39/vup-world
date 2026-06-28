package com.example.vupworld.service.fan;

import com.example.vupworld.domain.NpcType;
import com.example.vupworld.dto.NpcDtos.LeaderboardDTO;
import com.example.vupworld.dto.NpcDtos.LeaderboardItem;
import com.example.vupworld.dto.NpcDtos.RivalDTO;
import com.example.vupworld.dto.NpcDtos.RivalProgressDTO;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.infra.BalanceConfig;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RivalProgressService {

    // 3个模拟竞争对手
    private static final String[] RIVAL_NAMES = {"星海あかり", "月城レン", "風間ミコ"};
    private static final String[] RIVAL_ROUTES = {"SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE"};

    private final VupMapper vupMapper;
    private final BalanceConfig balanceConfig;

    public RivalProgressService(VupMapper vupMapper, BalanceConfig balanceConfig) {
        this.vupMapper = vupMapper;
        this.balanceConfig = balanceConfig;
    }

    /**
     * 返回 3 个模拟竞争对手的当前进度。
     * 对手粉丝按路线相关性、天数和玩家表现每日增长。
     */
    public List<RivalDTO> getRivals(Vup vup) {
        int day = vup.getDayCount();
        List<RivalDTO> rivals = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            int baseFans = 30 + i * 20;
            int dailyGrowth = calculateRivalGrowth(i, day, vup);
            int totalFans = baseFans + dailyGrowth * day;
            int variance = (int) (Math.sin(day * (i + 1) * 0.7 + i * 31) * 8);
            totalFans = Math.max(0, totalFans + variance);

            rivals.add(new RivalDTO(
                    RIVAL_NAMES[i],
                    RIVAL_ROUTES[i],
                    totalFans,
                    dailyGrowth,
                    threatLevel(totalFans, vup.getFans())
            ));
        }
        return rivals;
    }

    private int calculateRivalGrowth(int index, int day, Vup vup) {
        int baseGrowth = switch (index) {
            case 0 -> 15 + day / 5; // 稳健增长
            case 1 -> 10 + (day > 15 ? 20 : 0); // 后期爆发
            case 2 -> 25 - day / 3; // 前期猛后期衰
            default -> 15;
        };
        if (vup.getFans() > 30 + index * 20 + baseGrowth * day) {
            baseGrowth += 3;
        }
        if (vup.getWatchHeat() > 50) {
            baseGrowth += 2;
        }
        return Math.max(3, baseGrowth);
    }

    private String threatLevel(int rivalFans, int playerFans) {
        if (playerFans <= 0) {
            return "HIGH";
        }
        int ratio = rivalFans * 100 / playerFans;
        if (ratio >= balanceConfig.rivalOvertakeThresholdPercent()) {
            return "HIGH";
        }
        if (ratio >= 90) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 返回对手进度与是否被超越。对手粉丝超过玩家×1.2 视为被超越。
     */
    public RivalProgressDTO progress(Vup vup) {
        List<RivalDTO> rivals = getRivals(vup);
        int playerFans = vup.getFans();
        boolean overtaken = rivals.stream()
                .anyMatch(r -> r.fans() > playerFans * balanceConfig.rivalOvertakeThresholdPercent() / 100);
        return new RivalProgressDTO(rivals, overtaken);
    }

    /**
     * 排行榜：玩家 + 8 NPC 的粉丝/礼物/弹幕三榜。
     * NPC 分数基于其粉丝量与天数确定性推导。
     */
    public LeaderboardDTO leaderboard(Long userId) {
        Vup vup = vupMapper.findActiveByUserId(userId);
        int day = vup == null ? 1 : vup.getDayCount();
        int playerFans = vup == null ? 0 : vup.getFans();

        List<LeaderboardItem> fansBoard = new ArrayList<>();
        List<LeaderboardItem> giftsBoard = new ArrayList<>();
        List<LeaderboardItem> danmakuBoard = new ArrayList<>();

        if (vup != null) {
            fansBoard.add(new LeaderboardItem(vup.getName(), playerFans, "player", true));
            int playerGifts = vup.getCoin() / 100;
            giftsBoard.add(new LeaderboardItem(vup.getName(), playerGifts, "player", true));
            int playerDanmaku = vup.getWatchHeat() * day / 10;
            danmakuBoard.add(new LeaderboardItem(vup.getName(), playerDanmaku, "player", true));
        }

        for (NpcType npc : NpcType.all().values()) {
            int npcFans = npcFanEstimate(npc, day, playerFans);
            fansBoard.add(new LeaderboardItem(npc.displayName(), npcFans, npc.key(), false));
            int npcGifts = npcFans / 8;
            giftsBoard.add(new LeaderboardItem(npc.displayName(), npcGifts, npc.key(), false));
            int npcDanmaku = npcFans / 5;
            danmakuBoard.add(new LeaderboardItem(npc.displayName(), npcDanmaku, npc.key(), false));
        }

        fansBoard.sort(Comparator.comparingInt(LeaderboardItem::score).reversed());
        giftsBoard.sort(Comparator.comparingInt(LeaderboardItem::score).reversed());
        danmakuBoard.sort(Comparator.comparingInt(LeaderboardItem::score).reversed());

        return new LeaderboardDTO(
                List.copyOf(fansBoard),
                List.copyOf(giftsBoard),
                List.copyOf(danmakuBoard)
        );
    }

    private int npcFanEstimate(NpcType npc, int day, int playerFans) {
        int base = 40 + day * 12;
        int hash = (npc.key() + ":" + day).hashCode();
        int variance = Math.abs(hash % 40) - 20;
        int fans = base + variance;
        if (playerFans > 0) {
            fans = fans + playerFans / 8;
        }
        return Math.max(10, fans);
    }
}
