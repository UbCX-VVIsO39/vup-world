package com.example.vupworld.service.ending;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.AchievementDtos.EndingAtlasSlotDTO;
import com.example.vupworld.dto.AchievementDtos.EndingCollectionDTO;
import com.example.vupworld.mapper.EndingReviewMapper;
import com.example.vupworld.model.EndingReview;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EndingAtlasService {
    private static final List<EndingMeta> ENDING_ATLAS = List.of(
            new EndingMeta("ELECTRONIC_PICKLE", "电子榨菜", "完成稳健基础路线的陪饭结局", "饭"),
            new EndingMeta("SINGING_IDOL", "歌势遗珠", "完成歌势路线结局", "唱"),
            new EndingMeta("SLICE_SAINT", "切片圣体", "完成切片路线结局", "剪"),
            new EndingMeta("BLACK_RED_MAIN_STAGE", "黑红顶流", "完成黑红基础路线的高热结局", "热"),
            new EndingMeta("MAIN_STAGE_KING", "主会场之王", "完成黑红基础路线的高阶控场结局", "王"),
            new EndingMeta("CYBER_GIRLFRIEND", "赛博女友", "完成赛博女友结局", "心"),
            new EndingMeta("DD_BUS_STOP", "DD公交站", "完成社交联动基础路线的DD结局", "站"),
            new EndingMeta("GLORIOUS_GRADUATION", "光荣毕业", "完成稳健基础路线的体面收束结局", "谢"),
            new EndingMeta("UNKNOWN", "查无此V", "完成查无此V结局", "隐")
    );
    private static final List<String> NEXT_TARGET_ORDER = List.of(
            "SINGING_IDOL",
            "SLICE_SAINT",
            "BLACK_RED_MAIN_STAGE",
            "MAIN_STAGE_KING",
            "CYBER_GIRLFRIEND",
            "DD_BUS_STOP",
            "GLORIOUS_GRADUATION",
            "ELECTRONIC_PICKLE",
            "UNKNOWN"
    );

    private final EndingReviewMapper endingReviewMapper;
    private final JsonService jsonService;

    public EndingAtlasService(EndingReviewMapper endingReviewMapper, JsonService jsonService) {
        this.endingReviewMapper = endingReviewMapper;
        this.jsonService = jsonService;
    }

    public EndingCollectionDTO endingCollection(Long userId) {
        List<EndingReview> reviews = endingReviewMapper.findByUserId(userId);
        Map<String, BestEndingScore> bestScores = bestEndingScoresByType(reviews);
        List<EndingAtlasSlotDTO> endings = ENDING_ATLAS.stream()
                .map(ending -> endingAtlasSlot(ending, bestScores.get(ending.type())))
                .toList();
        long unlockedCount = endings.stream().filter(EndingAtlasSlotDTO::unlocked).count();
        EndingAtlasSlotDTO nextTarget = nextAtlasTarget(endings);
        return new EndingCollectionDTO(
                endings,
                unlockedCount,
                endings.size(),
                reviews.size(),
                nextEndingHint(nextTarget),
                nextTarget == null ? "COMPLETE" : nextTarget.type(),
                nextTarget == null ? "全结局图鉴" : nextTarget.name(),
                nextRunGoal(nextTarget)
        );
    }

    public String nextRestartTargetType(Long userId) {
        String type = endingCollection(userId).nextTargetType();
        return "COMPLETE".equals(type) ? "UNKNOWN" : type;
    }

    public int bestOverallScore(Long userId) {
        return endingReviewMapper.findByUserId(userId).stream()
                .mapToInt(review -> intValue(scorecardFor(review).get("overall")))
                .max()
                .orElse(0);
    }

    public RestartTarget restartTarget(String rawTargetType) {
        String targetType = canonicalEndingType(rawTargetType == null || rawTargetType.isBlank()
                ? "UNKNOWN"
                : rawTargetType.trim());
        return switch (targetType) {
            case "SINGING_IDOL" -> restartTarget(
                    "SINGING_IDOL",
                    RouteType.SINGING_IDOL.name(),
                    "歌势遗珠",
                    "唱歌偶像",
                    "遗珠返场",
                    "复活赛先把基础路线定成唱歌偶像：前7天练歌2次，中段用歌回或投稿把作品证据摆上桌。",
                    "练歌",
                    "直播企划或发布视频",
                    "最后一周保歌力和口碑，不要只练不展示。"
            );
            case "ELECTRONIC_PICKLE" -> restartTarget(
                    "ELECTRONIC_PICKLE",
                    RouteType.ELECTRONIC_PICKLE.name(),
                    "电子榨菜",
                    "电子榨菜",
                    "老粉保温",
                    "复活赛先走稳健基础路线：前7天杂谈复盘和粉丝群维护各留一次证据，别急着冲热搜。",
                    "杂谈复盘",
                    "粉丝群维护",
                    "最后一周补一个可复述的低压记忆点。"
            );
            case "SLICE_SAINT" -> restartTarget(
                    "SLICE_SAINT",
                    RouteType.SLICE_SAINT.name(),
                    "切片圣体",
                    "切片/梗舞",
                    "切片组续约",
                    "复活赛先走切片组或梗舞基础路线：先发视频补素材，再用切片组供货或练舞连续制造可剪证据。",
                    "发布视频",
                    "发布切片或练舞",
                    "最后一周换新梗，别让同一个素材回旋。"
            );
            case "BLACK_RED_MAIN_STAGE" -> restartTarget(
                    "BLACK_RED_MAIN_STAGE",
                    RouteType.BLACK_RED_MAIN_STAGE.name(),
                    "黑红顶流",
                    "黑红主会场",
                    "主会场余温",
                    "复活赛先走黑红主会场基础路线：用直播企划或切片接热度，但每次爆点后都要留降温证据。",
                    "直播企划",
                    "发布切片",
                    "最后一周用粉丝群维护或杂谈复盘把旧账压住。"
            );
            case "CYBER_GIRLFRIEND" -> restartTarget(
                    "CYBER_GIRLFRIEND",
                    RouteType.ELECTRONIC_PICKLE.name(),
                    "赛博女友",
                    "稳健/陪伴",
                    "陪伴感回声",
                    "复活赛目标是赛博女友，先走稳健陪伴基础路线：低压杂谈开局，粉丝群维护留证，中段再推高亮互动回应和边界处理。",
                    "杂谈复盘",
                    "粉丝群维护",
                    "最后一周保持商业化和边界同时成型。"
            );
            case "DD_BUS_STOP" -> restartTarget(
                    "DD_BUS_STOP",
                    RouteType.SOCIAL_COLLAB.name(),
                    "DD公交站",
                    "社交联动",
                    "站台回流",
                    "复活赛先走社交联动基础路线：同台互动接车流，再用粉丝群维护把老粉留在自家站牌。",
                    "同台互动",
                    "粉丝群维护",
                    "最后一周确认DD占比和老粉安抚都达标。"
            );
            case "MAIN_STAGE_KING" -> restartTarget(
                    "MAIN_STAGE_KING",
                    RouteType.BLACK_RED_MAIN_STAGE.name(),
                    "主会场之王",
                    "黑红主会场",
                    "录播硬盘",
                    "复活赛先走黑红主会场基础路线：接住热度和事故素材，再用米线工具证明你能控场。",
                    "直播企划",
                    "发布切片",
                    "最后一周冲高热度前先清高危旧账。"
            );
            case "GLORIOUS_GRADUATION" -> restartTarget(
                    "GLORIOUS_GRADUATION",
                    RouteType.ELECTRONIC_PICKLE.name(),
                    "光荣毕业",
                    "电子榨菜",
                    "体面返场",
                    "复活赛先走稳健基础路线：粉丝群维护、杂谈复盘和休息把口碑做高，最后不要乱冲热度。",
                    "粉丝群维护",
                    "杂谈复盘或休息",
                    "最后一周清高危旧账，把告别排班留进日志。"
            );
            case "UNKNOWN" -> restartTarget(
                    "UNKNOWN",
                    RouteType.UNKNOWN.name(),
                    "查无此V",
                    "未定型",
                    "复活赛档案",
                    "复活赛第一周先选定一条基础路线，连续做3次同路行动，至少留下3条能进复盘的证据。",
                    "任选一条基础路线的代表行动",
                    "补直播、投稿、切片或同台证据",
                    "最后一周别频繁转线。"
            );
            default -> throw new GameException("CONFIG_FIELD_INVALID", "未知重开倾向，别把天赋点加到虚空赛道。");
        };
    }

    private RestartTarget restartTarget(
            String targetType,
            String routeBiasType,
            String label,
            String baseRouteLabel,
            String legacyTag,
            String openingAdvice,
            String firstWeekAction,
            String midgameAction,
            String closingAction
    ) {
        return new RestartTarget(
                targetType,
                routeBiasType,
                label,
                openingAdvice,
                legacyTag,
                "本周目目标：基础路线「" + baseRouteLabel + "」->派生结局「" + label + "」。"
                        + "前7天优先做「" + firstWeekAction + "」，中段补「" + midgameAction + "」，"
                        + closingAction + " 30天结算只看本轮表现；"
                        + "重开只给路线提示和5%粉丝偏置，不给永久数值膨胀。",
                restartBoundary()
        );
    }

    public String restartBoundary() {
        return "只继承5%粉丝偏置和路线提示，不继承旧债、资源、属性倍率或未结事件。";
    }

    private EndingAtlasSlotDTO nextAtlasTarget(List<EndingAtlasSlotDTO> endings) {
        for (String targetType : NEXT_TARGET_ORDER) {
            Optional<EndingAtlasSlotDTO> locked = endings.stream()
                    .filter(ending -> targetType.equals(ending.type()))
                    .filter(ending -> !ending.unlocked())
                    .findFirst();
            if (locked.isPresent()) {
                return locked.get();
            }
        }
        return endings.stream()
                .filter(ending -> ending.bestScore() < 90)
                .min(Comparator
                        .comparingInt(EndingAtlasSlotDTO::nextGradeScore)
                        .thenComparingInt(ending -> -ending.bestScore()))
                .orElse(null);
    }

    private String nextEndingHint(EndingAtlasSlotDTO nextTarget) {
        if (nextTarget == null) {
            return "九线图鉴已收齐且都有S档记录，复活赛可以挑战更干净的无债收官。";
        }
        if (nextTarget.unlocked()) {
            return "下一档目标：" + nextTarget.name() + " 冲" + nextTarget.nextGrade()
                    + "，当前最佳" + nextTarget.bestGrade() + " " + nextTarget.bestScore()
                    + "分；下一局按「" + baseRouteLabel(nextTarget.type()) + "」基础路线补短板。";
        }
        return "下一张图鉴：" + nextTarget.name() + "。先走「"
                + baseRouteLabel(nextTarget.type()) + "」基础路线，再把条件推到派生结局。";
    }

    private String nextRunGoal(EndingAtlasSlotDTO nextTarget) {
        if (nextTarget == null) {
            return "多周目目标：保持无旧债继承，用更干净的30天证据链复盘任意路线。";
        }
        if (nextTarget.unlocked()) {
            return "多周目目标：重打一轮「" + nextTarget.name() + "」，把最佳记录从"
                    + nextTarget.bestGrade() + " " + nextTarget.bestScore()
                    + "分推到" + nextTarget.nextGrade() + "档；"
                    + nextRunActionPlan(nextTarget.type());
        }
        return "多周目目标：下一轮优先冲「" + nextTarget.name()
                + "」，" + nextRunActionPlan(nextTarget.type())
                + " 重开只给路线提示和5%粉丝偏置，不给永久数值膨胀。";
    }

    private EndingAtlasSlotDTO endingAtlasSlot(EndingMeta ending, BestEndingScore best) {
        int bestScore = best == null ? 0 : best.score();
        String bestGrade = best == null ? "未收录" : nonBlank(best.grade()) ? best.grade() : endingGrade(bestScore);
        String bestGradeLabel = best == null ? "等待首通" : nonBlank(best.gradeLabel()) ? best.gradeLabel() : endingGradeLabel(bestScore);
        String nextGrade = nextEndingGrade(bestScore);
        int nextGradeScore = nextEndingGradeScore(bestScore);
        boolean unlocked = best != null;
        String progressText = unlocked
                ? "最佳" + bestGrade + " " + bestScore + "分"
                : "未解锁";
        String nextGradeHint = unlocked
                ? bestScore >= 90
                        ? "S档已达成，下一轮挑战无严重旧账或更高证据链。"
                        : "下一目标" + nextGrade + "档，还差" + Math.max(0, nextGradeScore - bestScore) + "分。"
                : "先完成这条路线，首通后会记录个人最佳。";
        return new EndingAtlasSlotDTO(
                "ENDING_" + ending.type(),
                ending.type(),
                ending.title(),
                ending.description(),
                ending.icon(),
                unlocked,
                progressText,
                bestScore,
                bestGrade,
                bestGradeLabel,
                nextGrade,
                nextGradeScore,
                nextGradeHint,
                routeRecipe(ending.type()),
                gateHint(ending.type()),
                trapHint(ending.type())
        );
    }

    private String routeRecipe(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "基础路线：唱歌偶像。前7天练歌定调，中段用歌回或投稿补证据，结算前歌力至少11。";
            case "SLICE_SAINT" -> "基础路线：切片/梗舞。先发布视频补素材，再连续切片或练舞供货；避免同一梗回旋期硬剪。";
            case "BLACK_RED_MAIN_STAGE" -> "基础路线：黑红主会场。用直播企划或争议切片抬热度，但每次爆点后都要留处理证据。";
            case "MAIN_STAGE_KING" -> "基础路线：黑红主会场。把高热度、高梗浓度和控场证据同时推满，旧账必须能复盘。";
            case "CYBER_GIRLFRIEND" -> "基础路线：稳健/陪伴。低压陪伴起盘，高亮互动回应和粉丝服务推进商业化，边界处理不能断。";
            case "GLORIOUS_GRADUATION" -> "基础路线：电子榨菜。粉丝群维护、杂谈复盘和休息把口碑做高，收官少高压债。";
            case "ELECTRONIC_PICKLE" -> "基础路线：电子榨菜。杂谈复盘、粉丝群维护和低压直播连续留证，少冲热搜。";
            case "DD_BUS_STOP" -> "基础路线：社交联动。同台互动持续接DD，穿插粉丝群维护稳住老粉预期。";
            case "UNKNOWN" -> "基础路线：未定型。少做主线行动或频繁摇摆会进查无此V；下一局前7天先连续同路行动。";
            default -> "选一条路线连续做出证据，别只看最后一天行动。";
        };
    }

    private String gateHint(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "门槛：歌势路线分16，临门转线需18；近7天歌势证据2、歌力11。";
            case "SLICE_SAINT" -> "门槛：切片/梗舞路线分16，临门转线需18；近7天素材证据2、梗浓度40或乐子人45。";
            case "BLACK_RED_MAIN_STAGE" -> "门槛：主会场路线分15、围观热度65、口碑低于70，并有主会场证据。";
            case "MAIN_STAGE_KING" -> "门槛：主会场路线分32、梗浓度75、围观热度85；失控黑红还需旧账支撑。";
            case "CYBER_GIRLFRIEND" -> "门槛：稳健路线分12、粉丝服务证据4、商业化50，且独角兽占比30%或独角兽150。";
            case "GLORIOUS_GRADUATION" -> "门槛：最终粉丝群维护、口碑65、围观不超50、无严重债务，且稳健证据足够。";
            case "ELECTRONIC_PICKLE" -> "门槛：稳健路线分12、近7天稳健证据2、粉丝过基础线。";
            case "DD_BUS_STOP" -> "门槛：社交路线分14，临门转线需16；近7天联动证据1、DD占比33%。";
            case "UNKNOWN" -> "门槛：粉丝不足或路线证据不成型。";
            default -> "门槛：粉丝、路线分、近7天证据三项一起看。";
        };
    }

    private String trapHint(String endingType) {
        return switch (endingType) {
            case "SINGING_IDOL" -> "避坑：只练歌不直播/投稿，结局会缺代表事件。";
            case "SLICE_SAINT" -> "避坑：没有新素材还硬切，会触发梗疲劳和回旋镖。";
            case "BLACK_RED_MAIN_STAGE" -> "避坑：热度上来后不拆债，容易滑向主会场失控。";
            case "MAIN_STAGE_KING" -> "避坑：只堆热度没有证据，会变成普通黑红而不是主会场之王。";
            case "CYBER_GIRLFRIEND" -> "避坑：商业化过高但边界不清，会被独角兽债务反噬。";
            case "GLORIOUS_GRADUATION" -> "避坑：最后几天冲热搜或留严重旧账，会破坏体面收束。";
            case "ELECTRONIC_PICKLE" -> "避坑：全程休息会保守到查无此V，要有稳定代表行动。";
            case "DD_BUS_STOP" -> "避坑：只接DD不维护老粉，社交路线会变成低粘性过站。";
            case "UNKNOWN" -> "避坑：如果不想查无此V，前7天就选定一条主打法。";
            default -> "避坑：频繁转线会让观众预期和结局证据同时变散。";
        };
    }

    private Map<String, BestEndingScore> bestEndingScoresByType(List<EndingReview> reviews) {
        Map<String, BestEndingScore> bestScores = new HashMap<>();
        for (EndingReview review : reviews) {
            String endingType = canonicalEndingType(review.getEndingType());
            if (!nonBlank(endingType)) {
                continue;
            }
            Map<String, Object> scorecard = scorecardFor(review);
            int score = intValue(scorecard.get("overall"));
            String grade = stringValue(scorecard.get("grade"));
            String gradeLabel = stringValue(scorecard.get("gradeLabel"));
            BestEndingScore current = bestScores.get(endingType);
            if (current == null || score > current.score()) {
                bestScores.put(endingType, new BestEndingScore(score, grade, gradeLabel));
            }
        }
        return bestScores;
    }

    private Map<String, Object> scorecardFor(EndingReview review) {
        Object scorecardRaw = safeReadMap(review.getRouteReviewJson()).get("scorecard");
        if (scorecardRaw instanceof Map<?, ?> scorecard) {
            Map<String, Object> normalized = new HashMap<>();
            for (Map.Entry<?, ?> entry : scorecard.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    normalized.put(key, entry.getValue());
                }
            }
            return normalized;
        }
        return Map.of();
    }

    private Map<String, Object> safeReadMap(String json) {
        if (!nonBlank(json)) {
            return Map.of();
        }
        try {
            return jsonService.readMap(json);
        } catch (IllegalStateException ignored) {
            return Map.of();
        }
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean nonBlank(String text) {
        return text != null && !text.isBlank();
    }

    private String canonicalEndingType(String endingType) {
        if (!nonBlank(endingType)) {
            return "";
        }
        return switch (endingType.trim().toUpperCase()) {
            case "LEGEND" -> "MAIN_STAGE_KING";
            case "GRADUATION" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE" -> "CYBER_GIRLFRIEND";
            case "DANCE_MEME" -> "SLICE_SAINT";
            case "SOCIAL_COLLAB" -> "DD_BUS_STOP";
            default -> endingType.trim().toUpperCase();
        };
    }

    private String baseRouteLabel(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> "唱歌偶像";
            case "SLICE_SAINT" -> "切片/梗舞";
            case "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING" -> "黑红主会场";
            case "CYBER_GIRLFRIEND" -> "稳健/陪伴";
            case "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE" -> "电子榨菜";
            case "DD_BUS_STOP" -> "社交联动";
            default -> "未定型";
        };
    }

    private String nextRunActionPlan(String endingType) {
        return switch (canonicalEndingType(endingType)) {
            case "SINGING_IDOL" -> "前7天练歌2次，中段补歌回或投稿，最后保口碑。";
            case "SLICE_SAINT" -> "前7天发视频补素材，中段发布切片或练舞，最后换新梗防回旋。";
            case "BLACK_RED_MAIN_STAGE" -> "前7天用直播企划定调，中段接切片热度，爆点后立刻粉丝群维护。";
            case "MAIN_STAGE_KING" -> "先走黑红主会场，再把围观热度、梗浓度和旧账处理一起推上去。";
            case "CYBER_GIRLFRIEND" -> "先低压陪伴和粉丝群维护，中段推高亮互动回应，最后处理边界和商业化。";
            case "GLORIOUS_GRADUATION" -> "先稳口碑，中段清旧账，最后用粉丝群维护或休息完成体面收束。";
            case "ELECTRONIC_PICKLE" -> "前7天杂谈复盘和粉丝群维护连做，中段补固定饭点记忆点。";
            case "DD_BUS_STOP" -> "前7天同台互动接车流，中段粉丝群维护，最后确认DD占比和老粉留存。";
            default -> "前7天选一条基础路线连续做3次，先把证据链立起来。";
        };
    }

    private String endingGrade(int score) {
        if (score >= 90) return "S";
        if (score >= 78) return "A";
        if (score >= 62) return "B";
        if (score >= 45) return "C";
        return "D";
    }

    private String endingGradeLabel(int score) {
        if (score >= 90) {
            return "精品证据链";
        }
        if (score >= 78) {
            return "稳定好局";
        }
        if (score >= 62) {
            return "成型但有缺口";
        }
        if (score >= 45) {
            return "能收官但不干净";
        }
        return "复活赛重打";
    }

    private String nextEndingGrade(int score) {
        if (score < 45) {
            return "C";
        }
        if (score < 62) {
            return "B";
        }
        if (score < 78) {
            return "A";
        }
        return "S";
    }

    private int nextEndingGradeScore(int score) {
        if (score < 45) {
            return 45;
        }
        if (score < 62) {
            return 62;
        }
        if (score < 78) {
            return 78;
        }
        return 90;
    }

    private record EndingMeta(String type, String title, String description, String icon) {
    }

    private record BestEndingScore(int score, String grade, String gradeLabel) {
    }

    public record RestartTarget(
            String targetType,
            String routeBiasType,
            String label,
            String openingAdvice,
            String legacyTag,
            String runObjective,
            String boundary
    ) {
    }
}
