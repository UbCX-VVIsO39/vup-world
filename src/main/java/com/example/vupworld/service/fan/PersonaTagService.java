package com.example.vupworld.service.fan;

import com.example.vupworld.service.core.VupService;
import com.example.vupworld.dto.PersonaDtos.PersonaTagBoardDTO;
import com.example.vupworld.dto.PersonaDtos.PersonaTagDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.RiskDebtMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PersonaTagService {
    private final VupService vupService;
    private final BusinessLogMapper businessLogMapper;
    private final RiskDebtMapper riskDebtMapper;

    public PersonaTagService(VupService vupService, BusinessLogMapper businessLogMapper, RiskDebtMapper riskDebtMapper) {
        this.vupService = vupService;
        this.businessLogMapper = businessLogMapper;
        this.riskDebtMapper = riskDebtMapper;
    }

    public PersonaTagBoardDTO tags(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        List<BusinessLog> logs = businessLogMapper.findEndingReferenceLogs(vup.getId());
        long clipCount = actionCount(logs, "PUBLISH_CLIP");
        long videoCount = actionCount(logs, "PUBLISH_VIDEO");
        long talkCount = actionCount(logs, "TRAIN_TALK");
        long fanGroupCount = actionCount(logs, "FAN_GROUP_MAINTAIN");
        long npcCount = actionCount(logs, "NPC_INTERACT");
        long debtCount = riskDebtMapper.findEndingRefsByVupId(vup.getId()).size();

        List<PersonaTagDTO> tags = List.of(
                        clipSupplier(vup, clipCount, videoCount),
                        oldFanThermos(vup, fanGroupCount, talkCount),
                        forumEmployer(vup, debtCount),
                        unicornPressureCooker(vup),
                        ddBusStation(vup, npcCount),
                        gracefulSignoff(vup, fanGroupCount)
                ).stream()
                .sorted(Comparator.comparingInt(PersonaTagDTO::strength).reversed().thenComparing(PersonaTagDTO::tagKey))
                .toList();

        long activeCount = tags.stream().filter(tag -> "ACTIVE".equals(tag.status())).count();
        PersonaTagDTO lead = tags.get(0);
        return new PersonaTagBoardDTO(
                headline(lead, activeCount),
                imageFor(lead),
                (int) activeCount,
                tags,
                nextHint(lead)
        );
    }

    private PersonaTagDTO clipSupplier(Vup vup, long clipCount, long videoCount) {
        int strength = clamp((int) (clipCount * 32 + videoCount * 10 + vup.getMemeLevel()));
        String evidence = clipCount == 0
                ? "切片组还没拿到连续供货证据。"
                : "本轮发布切片%d次，切片组已经开始按素材排班。".formatted(clipCount);
        return tag(
                "CLIP_SUPPLIER",
                "切片组供货商",
                strength,
                evidence,
                strength > 70 ? "继续硬供会加速复读，最好先补新素材。" : "先投稿或直播铺素材，再让切片组接住传播。",
                "violet"
        );
    }

    private PersonaTagDTO oldFanThermos(Vup vup, long fanGroupCount, long talkCount) {
        int trueFanRatio = ratio(vup.getTrueFans(), vup.getFans());
        int strength = clamp(trueFanRatio + vup.getReputation() / 2 + (int) fanGroupCount * 12 + (int) talkCount * 8 - vup.getWatchHeat() / 3);
        return tag(
                "OLD_FAN_THERMOS",
                "老粉保温杯",
                strength,
                "真爱粉占比%d%%，口碑%d，老粉正在判断这轮能不能长期陪。".formatted(trueFanRatio, vup.getReputation()),
                strength > 70 ? "可以继续稳住陪伴感，但别让内容完全没活。" : "粉丝群维护和低压杂谈能把这个标签养起来。",
                "teal"
        );
    }

    private PersonaTagDTO forumEmployer(Vup vup, long debtCount) {
        int lowReputationPressure = Math.max(0, 60 - vup.getReputation());
        int strength = clamp(vup.getWatchHeat() + vup.getMemeLevel() / 2 + (int) debtCount * 24 + lowReputationPressure);
        return tag(
                "FORUM_EMPLOYER",
                "楼友就业保障",
                strength,
                "围观热度%d、梗浓度%d，楼友会按这些信号决定要不要开楼。".formatted(vup.getWatchHeat(), vup.getMemeLevel()),
                strength > 70 ? "这类标签会推高可看性，也更容易留下事故素材。" : "高风险标题、回旋镖和欠账会让楼友更快入场。",
                "rose"
        );
    }

    private PersonaTagDTO unicornPressureCooker(Vup vup) {
        int unicornRatio = ratio(vup.getUnicornFans(), vup.getFans());
        int strength = clamp(unicornRatio * 2 + vup.getCommercialLevel() + Math.max(0, 75 - vup.getReputation()) / 2);
        return tag(
                "UNICORN_PRESSURE_COOKER",
                "高压锅营业",
                strength,
                "独角兽占比%d%%，商业化%d，期待感越高越容易变成高压锅。".formatted(unicornRatio, vup.getCommercialLevel()),
                strength > 70 ? "需要用边界感和粉丝群维护降压。" : "高亮互动回应和强绑定营业会抬高这个标签。",
                "rose"
        );
    }

    private PersonaTagDTO ddBusStation(Vup vup, long npcCount) {
        int ddRatio = ratio(vup.getDdFans(), vup.getFans());
        int strength = clamp(ddRatio * 2 + (int) npcCount * 18 + vup.getPopularity() / 6);
        return tag(
                "DD_BUS_STATION",
                "DD公交站牌",
                strength,
                "DD占比%d%%，同台互动%d次，路过观众正在决定要不要坐一站。".formatted(ddRatio, npcCount),
                strength > 70 ? "社交路线已经成型，注意别让老粉觉得你只会端水。" : "查房互动和联动企划会让DD更容易上车。",
                "teal"
        );
    }

    private PersonaTagDTO gracefulSignoff(Vup vup, long fanGroupCount) {
        int strength = clamp((int) fanGroupCount * 30 + vup.getReputation() / 2 + ratio(vup.getTrueFans(), vup.getFans()) / 2);
        return tag(
                "GRACEFUL_SIGNOFF",
                "体面下播预备役",
                strength,
                "粉丝群维护%d次，口碑%d，体面收束需要这些证据兜底。".formatted(fanGroupCount, vup.getReputation()),
                strength > 70 ? "如果第30天主动收束，这个标签会支撑光荣毕业。" : "多做粉丝群维护，能把收束路线从口号变成证据。",
                "gold"
        );
    }

    private PersonaTagDTO tag(String key, String label, int strength, String evidence, String nextHint, String tone) {
        Status status = statusFor(strength);
        return new PersonaTagDTO(key, label, strength, status.name(), status.label, evidence, nextHint, tone);
    }

    private Status statusFor(int strength) {
        if (strength > 70) {
            return Status.ACTIVE;
        }
        if (strength < 30) {
            return Status.DORMANT;
        }
        return Status.FORMING;
    }

    private String headline(PersonaTagDTO lead, long activeCount) {
        if (activeCount == 0) {
            return "观众还没把你钉成固定人设，标签都在试探期。";
        }
        return "当前最响的人设是【%s】，已有%d个标签被观众叫顺口。".formatted(lead.label(), activeCount);
    }

    private String nextHint(PersonaTagDTO lead) {
        return "下一个可控方向：%s".formatted(lead.nextHint());
    }

    private String imageFor(PersonaTagDTO lead) {
        return switch (lead.tagKey()) {
            case "CLIP_SUPPLIER" -> "v4/backgrounds/rush-clip-room.png";
            case "FORUM_EMPLOYER", "UNICORN_PRESSURE_COOKER" -> "v4/events/risk/chat-meltdown.png";
            case "DD_BUS_STATION" -> "v4/routes/dd-bus-stop/room-bg.png";
            case "GRACEFUL_SIGNOFF", "OLD_FAN_THERMOS" -> "v4/backgrounds/rush-fan-room.png";
            default -> "v4/backgrounds/algorithm-dashboard.png";
        };
    }

    private long actionCount(List<BusinessLog> logs, String action) {
        return logs.stream().filter(log -> action.equals(log.getAction())).count();
    }

    private int ratio(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(part * 100f / total);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private enum Status {
        ACTIVE("已激活"),
        FORMING("成型中"),
        DORMANT("休眠");

        private final String label;

        Status(String label) {
            this.label = label;
        }
    }
}
