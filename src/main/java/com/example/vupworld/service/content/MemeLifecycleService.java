package com.example.vupworld.service.content;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.MemeDtos.MemeLifecycleDTO;
import com.example.vupworld.dto.MemeDtos.MemeLifecycleItemDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MemeLifecycleService {
    private static final String CLIP_SPREAD = "CLIP_SPREAD";

    private final VupService vupService;
    private final BusinessLogMapper businessLogMapper;

    public MemeLifecycleService(VupService vupService, BusinessLogMapper businessLogMapper) {
        this.vupService = vupService;
        this.businessLogMapper = businessLogMapper;
    }

    public MemeLifecycleDTO lifecycle(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        MemeLifecycleItemDTO clipSpread = clipSpreadItem(vup);
        return new MemeLifecycleDTO(
                headlineFor(clipSpread),
                List.of(clipSpread),
                nextHintFor(clipSpread)
        );
    }

    private MemeLifecycleItemDTO clipSpreadItem(Vup vup) {
        List<BusinessLog> logs = businessLogMapper.findRecentClipLogsBySubtype(vup.getId(), CLIP_SPREAD);
        if (logs.isEmpty()) {
            return new MemeLifecycleItemDTO(
                    CLIP_SPREAD,
                    "切片传播梗",
                    "NEW",
                    "新梗期",
                    0,
                    null,
                    null,
                    "还没有切片传播记录，下一条切片会按新梗处理。"
            );
        }

        int currentDay = vup.getDayCount();
        BusinessLog lastLog = logs.get(0);
        int lastSeenDay = lastLog.getDay();
        int firstSeenDay = logs.stream()
                .map(BusinessLog::getDay)
                .min(Comparator.naturalOrder())
                .orElse(lastSeenDay);
        int usesInWindow = (int) logs.stream()
                .filter(log -> currentDay - log.getDay() <= 7)
                .count();

        Stage stage = stageFor(currentDay, lastSeenDay, usesInWindow);
        return new MemeLifecycleItemDTO(
                CLIP_SPREAD,
                "切片传播梗",
                stage.name(),
                stage.label,
                usesInWindow,
                firstSeenDay,
                lastSeenDay,
                hintFor(stage)
        );
    }

    private Stage stageFor(int currentDay, int lastSeenDay, int usesInWindow) {
        if (currentDay - lastSeenDay > 7) {
            return Stage.BOOMERANG;
        }
        if (usesInWindow >= 2) {
            return Stage.HEAVY_REPEAT;
        }
        return Stage.REPEAT;
    }

    private String headlineFor(MemeLifecycleItemDTO item) {
        return switch (item.stage()) {
            case "NEW" -> "梗库还在新梗期，切片组等第一条素材。";
            case "REPEAT" -> "切片传播梗进入复读期，继续发会开始递减。";
            case "HEAVY_REPEAT" -> "同一个切片梗正在重度复读，乐子人开始嫌没有新意。";
            case "BOOMERANG" -> "旧切片梗进入回旋期，再拿出来容易变复盘素材。";
            default -> "梗生命周期已读取，先看阶段再决定今天怎么营业。";
        };
    }

    private String nextHintFor(MemeLifecycleItemDTO item) {
        return switch (item.stage()) {
            case "NEW" -> "先投稿或直播铺素材，再让切片组接住传播。";
            case "REPEAT" -> "复读期最好换素材，或者用直播、投稿把上下文补新。";
            case "HEAVY_REPEAT" -> "重度复读会明显压低乐子人转化，先停手换节奏。";
            case "BOOMERANG" -> "回旋期切片更像旧账翻出，适合谨慎处理而不是硬冲涨粉。";
            default -> "先看梗阶段，再决定要不要继续切。";
        };
    }

    private String hintFor(Stage stage) {
        return switch (stage) {
            case NEW -> "还没有切片传播记录，下一条切片会按新梗处理。";
            case REPEAT -> "复读期继续切会收益递减，日报也会开始阴阳。";
            case HEAVY_REPEAT -> "同一梗 7 天内已经多次复读，切片组开始标注重复笑点。";
            case BOOMERANG -> "超过一周没续上的旧梗进入回旋期，容易留下复盘材料。";
        };
    }

    private enum Stage {
        NEW("新梗期"),
        REPEAT("复读期"),
        HEAVY_REPEAT("重度复读"),
        BOOMERANG("回旋期");

        private final String label;

        Stage(String label) {
            this.label = label;
        }
    }
}
