package com.example.vupworld.service.content;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.BuzzDtos.BuzzBriefingDTO;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BuzzBriefingService {
    private static final String DASHBOARD_IMAGE = "v4/backgrounds/algorithm-dashboard.png";

    private final VupService vupService;
    private final PlatformTrendService platformTrendService;

    public BuzzBriefingService(VupService vupService, PlatformTrendService platformTrendService) {
        this.vupService = vupService;
        this.platformTrendService = platformTrendService;
    }

    public BuzzBriefingDTO briefing(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        Map<String, Object> trend = platformTrendService.currentTrendForDay(vup.getDayCount());
        String trendLabel = String.valueOf(trend.get("label"));
        String trendDescription = String.valueOf(trend.get("description"));
        return new BuzzBriefingDTO(
                headline(vup, trendLabel),
                heatLabel(vup),
                DASHBOARD_IMAGE,
                forumLine(vup),
                clipperLine(vup),
                trendLabel,
                trendDescription,
                nextMoveHint(vup, trendLabel)
        );
    }

    private String headline(Vup vup, String trendLabel) {
        if (vup.getWatchHeat() >= 70) {
            return "热搜预警：主会场门口已经排队，" + trendLabel + "也压不住楼友手里的法槌。";
        }
        if (vup.getMemeLevel() >= 55) {
            return "热搜边缘：同一个梗又被顶上来，标题组开始担心被查重。";
        }
        if (vup.getCommercialLevel() >= 35) {
            return "热搜观察：商单味上桌，品牌和老粉都在看今天怎么收口。";
        }
        String[] headlines = {
                "热搜低压：算法还没把你推上广场，" + trendLabel + "适合先攒基本盘。",
                "热搜安静：今天没有大活，录播组在调时间轴，标题组在等素材。",
                "热搜观察：楼友在外圈观望，老粉在群里写小作文，DD在坐一站就下车。",
                "热搜潜伏：平台算法还没给量，切片组在等一个能出圈的钩子。",
                "热搜边缘：乐子人在录，考据组在翻，标题组在等一个名场面。"
        };
        return headlines[vup.getDayCount() % headlines.length];
    }

    private String heatLabel(Vup vup) {
        if (vup.getWatchHeat() >= 70) {
            return "全网开庭";
        }
        if (vup.getWatchHeat() >= 40 || vup.getMemeLevel() >= 45) {
            return "楼里升温";
        }
        if (vup.getPopularity() >= 60) {
            return "路人围观";
        }
        return "低压潜伏";
    }

    private String forumLine(Vup vup) {
        if (vup.getWatchHeat() >= 70) {
            return "楼友已经开楼，标题、截图和半句话都被搬到主会场。";
        }
        if (vup.getReputation() < 45) {
            return "楼友还没坐满，但已经开始贷款你是不是又要嘴硬。";
        }
        if (vup.getDdFans() >= 60) {
            return "楼友说DD像公交站换乘，联动热度能来，也可能很快下车。";
        }
        String[] forumLines = {
                "楼友暂时只是在外圈观察，老粉和DD都还没把话说死。",
                "楼友说新人V不容易，有人说先观望再说，气氛很微妙。",
                "楼友在等一个名场面，老粉在等一个解释，DD在等一个联动。",
                "楼友说论坛帖标题：新人V的第一周，大家怎么看？回复数：3",
                "楼友说今天没有大活，但录播组已经开始调时间轴了。"
        };
        return forumLines[(vup.getDayCount() - 1) % forumLines.length];
    }

    private String clipperLine(Vup vup) {
        if (vup.getMemeLevel() >= 55 || vup.getWatchHeat() >= 55) {
            return "录播组已经在调时间轴，标题组把完整版见评论区写进备忘录。";
        }
        if (vup.getFans() >= 180) {
            return "录播组有素材可剪，但标题组还在等一个能出圈的钩子。";
        }
        String[] clipperLines = {
                "录播组还在待机，标题组今天先别硬拱热搜。",
                "录播组说今天素材不够，切片组说再等等看有没有名场面。",
                "录播组在等一个能出圈的片段，标题组在等一个能上热搜的标题。",
                "切片组说今天的素材太平淡，录播组说再观察观察。",
                "录播组已经开始写复盘了，标题组还在等一个爆点。"
        };
        return clipperLines[vup.getDayCount() % clipperLines.length];
    }

    private String nextMoveHint(Vup vup, String trendLabel) {
        if (vup.getWatchHeat() >= 70) {
            return "建议先用杂谈复盘、粉丝群维护或米线工具降温，别让日报组直接写成判决书。";
        }
        if (vup.getMemeLevel() >= 55) {
            return "建议换一个内容角度，复读期继续加码只会让梗回旋。";
        }
        if (vup.getStamina() <= 2) {
            return "体力见底，今天硬冲热搜容易把低压变成事故素材。";
        }
        return "当前是" + trendLabel + "，可以选顺版本行动，也可以逆版本但要准备解释成本。";
    }
}
