package com.example.vupworld.service.fan;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.AudienceDtos.AudienceExpectationBoardDTO;
import com.example.vupworld.dto.AudienceDtos.AudienceExpectationDTO;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AudienceExpectationService {
    private static final String IMAGE = "v4/backgrounds/algorithm-dashboard.png";

    private final VupService vupService;
    private final JsonService jsonService;
    private final DayFlowService dayFlowService;

    public AudienceExpectationService(VupService vupService, JsonService jsonService, DayFlowService dayFlowService) {
        this.vupService = vupService;
        this.jsonService = jsonService;
        this.dayFlowService = dayFlowService;
    }

    public AudienceExpectationBoardDTO board(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        Map<String, Integer> scores = routeScores(vup);
        List<AudienceExpectationDTO> expectations = List.of(
                        item(vup, RouteType.ELECTRONIC_PICKLE.name(), scores),
                        item(vup, RouteType.SINGING_IDOL.name(), scores),
                        item(vup, RouteType.SLICE_SAINT.name(), scores),
                        item(vup, RouteType.SOCIAL_COLLAB.name(), scores),
                        item(vup, RouteType.DANCE_MEME.name(), scores),
                        item(vup, RouteType.BLACK_RED_MAIN_STAGE.name(), scores)
                ).stream()
                .sorted(Comparator.comparingInt(AudienceExpectationDTO::score).reversed()
                        .thenComparing(AudienceExpectationDTO::routeType))
                .toList();
        AudienceExpectationDTO lead = expectations.stream()
                .filter(item -> item.score() > 0)
                .findFirst()
                .orElse(expectations.get(0));

        return new AudienceExpectationBoardDTO(
                headline(lead),
                lead,
                expectations,
                pivotRiskLabel(lead.score()),
                pivotRiskLine(vup, lead),
                nextMoveHint(vup, lead),
                IMAGE
        );
    }

    private AudienceExpectationDTO item(Vup vup, String routeType, Map<String, Integer> scores) {
        int score = Math.max(0, scores.getOrDefault(routeType, 0));
        int total = scores.entrySet().stream()
                .filter(entry -> !RouteType.UNKNOWN.name().equals(entry.getKey()))
                .mapToInt(entry -> Math.max(0, entry.getValue()))
                .sum();
        int share = total <= 0 ? 0 : Math.round(score * 100f / total);
        return new AudienceExpectationDTO(
                routeType,
                dayFlowService.routeLabel(routeType),
                score,
                share,
                lockLabel(score),
                evidenceLine(vup, routeType, score),
                tone(routeType, score)
        );
    }

    private String headline(AudienceExpectationDTO lead) {
        if (lead.score() >= 12) {
            return "观众期待：大家已经把你按【%s】归档，转型要先补解释。".formatted(lead.routeLabel());
        }
        if (lead.score() >= 6) {
            return "观众期待：弹幕开始按【%s】理解你，路线正在成型。".formatted(lead.routeLabel());
        }
        if (lead.score() > 0) {
            return "观众期待：观众刚把你和【%s】连起来，还没到锁死阶段。".formatted(lead.routeLabel());
        }
        return "观众期待：大家还没把你归类，今天的第一批证据会很响。";
    }

    private String pivotRiskLabel(int score) {
        if (score >= 12) {
            return "转型高压";
        }
        if (score >= 6) {
            return "转型要解释";
        }
        return "可试路线";
    }

    private String pivotRiskLine(Vup vup, AudienceExpectationDTO lead) {
        if (lead.score() >= 12) {
            return "转型风险：%s证据已经很厚，老粉会要解释，DD会看你是不是临时蹭版本。".formatted(lead.routeLabel());
        }
        if (lead.score() >= 6) {
            return "转型风险：%s正在成型，换方向前最好用杂谈复盘或投稿补上下文。".formatted(lead.routeLabel());
        }
        if (vup.getWatchHeat() >= 50) {
            return "转型风险：路线还没锁死，但围观热度偏高，标题组一句话就能被剪成新理解。";
        }
        return "转型风险：当前还在新人观望期，先用连续行动让观众知道你想做哪类V。";
    }

    private String nextMoveHint(Vup vup, AudienceExpectationDTO lead) {
        return switch (lead.routeType()) {
            case "SINGING_IDOL" -> "下一步：继续练歌或开低压歌回，把老粉的“有在补课”变成路线证据。";
            case "SLICE_SAINT" -> "下一步：先补视频素材再切，切片组不吃空气，也别把同一个梗复读到回旋。";
            case "BLACK_RED_MAIN_STAGE" -> "下一步：如果还要黑红，先看口碑和旧账；楼友会把标题和截图并排展示。";
            case "SOCIAL_COLLAB" -> "下一步：查房互动能接DD，但要给老粉一个你不是只会端水的理由。";
            case "DANCE_MEME" -> "下一步：练舞和短视频要连起来，动作梗需要新素材，不然乐子人只剩逐帧挑错。";
            case "ELECTRONIC_PICKLE" -> "下一步：杂谈复盘或粉丝群维护能稳期待，但别连续低压到没活。";
            default -> vup.getDayCount() <= 7
                    ? "下一步：新人保护期先定基本盘，不急着贷款主会场。"
                    : "下一步：按当前最强证据补一到两天，结局复盘才有话可说。";
        };
    }

    private String evidenceLine(Vup vup, String routeType, int score) {
        return switch (routeType) {
            case "SINGING_IDOL" -> score > 0
                    ? "老粉会把练歌和歌回兑现算作证据；当前歌力%d、DD%d。".formatted(vup.getSongPower(), vup.getDdFans())
                    : "还缺练歌、歌回或投稿证据，老粉暂时只记得新人标签。";
            case "SLICE_SAINT" -> score > 0
                    ? "切片组开始认素材来源；当前乐子人%d、串味%d。".formatted(vup.getFunFans(), vup.getMemeLevel())
                    : "还缺视频和切片证据，切片组暂时没有排班理由。";
            case "BLACK_RED_MAIN_STAGE" -> score > 0
                    ? "标题组和楼友会记住高风险标题；当前围观%d、口碑%d。".formatted(vup.getWatchHeat(), vup.getReputation())
                    : "还没有稳定黑红证据，楼友暂时只在外圈看热闹。";
            case "SOCIAL_COLLAB" -> score > 0
                    ? "DD会按互动频率判断要不要坐一站；当前DD%d、真爱粉%d。".formatted(vup.getDdFans(), vup.getTrueFans())
                    : "还缺查房、联动或同台互动证据，DD只是路过。";
            case "DANCE_MEME" -> score > 0
                    ? "动作梗需要练舞和短视频接住；当前舞力%d、串味%d。".formatted(vup.getDancePower(), vup.getMemeLevel())
                    : "还缺舞蹈练习或动作梗素材，鬼畜组暂时没开工。";
            default -> score > 0
                    ? "老粉按稳定陪伴和复盘记录理解你；当前真爱粉%d、口碑%d。".formatted(vup.getTrueFans(), vup.getReputation())
                    : "还缺低压陪伴证据，粉丝群暂时只在观望。";
        };
    }

    private String lockLabel(int score) {
        if (score >= 12) {
            return "期待锁定";
        }
        if (score >= 6) {
            return "成型中";
        }
        if (score > 0) {
            return "观望中";
        }
        return "未建立";
    }

    private String tone(String routeType, int score) {
        if (score >= 12 && ("BLACK_RED_MAIN_STAGE".equals(routeType) || "SLICE_SAINT".equals(routeType))) {
            return "rose";
        }
        if (score >= 6) {
            return "teal";
        }
        if (score > 0) {
            return "violet";
        }
        return "gold";
    }

    private Map<String, Integer> routeScores(Vup vup) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (RouteType routeType : RouteType.values()) {
            scores.put(routeType.name(), 0);
        }
        mergeScores(scores, vup.getRouteScoreJson());
        mergeScores(scores, vup.getExpectationJson());
        return scores;
    }

    private void mergeScores(Map<String, Integer> scores, String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        jsonService.readMap(json).forEach((key, value) -> {
            if (value instanceof Number number && scores.containsKey(key)) {
                scores.put(key, Math.max(scores.get(key), number.intValue()));
            }
        });
    }
}
