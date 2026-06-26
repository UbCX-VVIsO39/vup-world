package com.example.vupworld.service.fan;

import com.example.vupworld.service.risk.DebtService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.AudienceDtos.FunAudienceMetricDTO;
import com.example.vupworld.dto.AudienceDtos.FunAudienceProfileDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class FunAudienceProfileService {
    private static final String EDITING_IMAGE = "v4/backgrounds/rush-clip-room.png";

    private final VupService vupService;
    private final DebtService debtService;
    private final BusinessLogMapper businessLogMapper;

    public FunAudienceProfileService(VupService vupService, DebtService debtService, BusinessLogMapper businessLogMapper) {
        this.vupService = vupService;
        this.debtService = debtService;
        this.businessLogMapper = businessLogMapper;
    }

    public FunAudienceProfileDTO profile(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        List<RiskDebt> debts = debtService.openDebts(vup);
        int funRatio = percent(vup.getFunFans(), vup.getFans());
        int materialStock = materialStock(vup);
        int debtPressure = clamp(debts.size() * 8 + debts.stream().mapToInt(RiskDebt::getSeverity).sum() * 12);
        List<FunAudienceMetricDTO> metrics = List.of(
                metric("RECORDING", "录播欲", recordingDesire(vup, funRatio, materialStock), "录播组", recordingLine(materialStock)),
                metric("INSTIGATE", "拱火欲", instigateDesire(vup, funRatio, debtPressure), "弹幕递话筒", instigateLine(vup)),
                metric("ARCHIVE", "考据欲", archiveDesire(vup, debtPressure), "楼友查重", archiveLine(vup, debts.size())),
                metric("MEME_PLAY", "玩梗欲", memePlayDesire(vup, funRatio, materialStock), "复读机", memePlayLine(vup))
        );
        FunAudienceMetricDTO dominant = metrics.stream()
                .max(Comparator.comparingInt(FunAudienceMetricDTO::value))
                .orElse(metrics.get(0));
        return new FunAudienceProfileDTO(
                headline(vup, dominant),
                riskLabel(dominant.value()),
                EDITING_IMAGE,
                metrics,
                dominant.label() + "最高：" + dominant.line(),
                nextMoveHint(vup, dominant)
        );
    }

    private FunAudienceMetricDTO metric(String key, String label, int value, String tone, String line) {
        int clamped = clamp(value);
        return new FunAudienceMetricDTO(key, label, clamped, toneFor(clamped, tone), line);
    }

    private int recordingDesire(Vup vup, int funRatio, int materialStock) {
        return 18 + funRatio / 2 + materialStock * 6 + vup.getMemeLevel() / 3
                + vup.getWatchHeat() / 4 + vup.getPopularity() / 6;
    }

    private int instigateDesire(Vup vup, int funRatio, int debtPressure) {
        return 12 + (int) (vup.getWatchHeat() * 0.55) + funRatio / 3
                + (100 - vup.getReputation()) / 3 + debtPressure / 2;
    }

    private int archiveDesire(Vup vup, int debtPressure) {
        return 10 + debtPressure + vup.getWatchHeat() / 3
                + (100 - vup.getReputation()) / 4 + vup.getCommercialLevel() / 4;
    }

    private int memePlayDesire(Vup vup, int funRatio, int materialStock) {
        return 20 + (int) (vup.getMemeLevel() * 0.65) + funRatio / 2
                + vup.getPopularity() / 5 + materialStock * 4;
    }

    private String headline(Vup vup, FunAudienceMetricDTO dominant) {
        if (dominant.value() >= 70) {
            return "乐子人画像：主会场门口已经有椅子，" + dominant.label() + "正在接管弹幕。";
        }
        if (dominant.value() >= 45) {
            return "乐子人画像：楼友开始预热，" + dominant.label() + "是今天最容易被点燃的方向。";
        }
        if (vup.getFunFans() >= 30) {
            return "乐子人画像：录播组有人值班，但还没到全员开麦。";
        }
        return "乐子人画像：低压旁观，标题组暂时不用把法槌擦太亮。";
    }

    private String riskLabel(int value) {
        if (value >= 70) {
            return "主会场预警";
        }
        if (value >= 45) {
            return "楼友预热";
        }
        return "低压旁观";
    }

    private String nextMoveHint(Vup vup, FunAudienceMetricDTO dominant) {
        if ("ARCHIVE".equals(dominant.key()) && dominant.value() >= 45) {
            return "建议用杂谈复盘或粉丝群维护补上下文，不然考据楼会替你写年表。";
        }
        if ("INSTIGATE".equals(dominant.key()) && dominant.value() >= 45) {
            return "今天少嘴硬，多给确定答复；弹幕递话筒时别把自己架上去。";
        }
        if ("MEME_PLAY".equals(dominant.key()) && vup.getMemeLevel() >= 55) {
            return "梗已经有复读苗头，换素材比继续加感叹号更稳。";
        }
        if ("RECORDING".equals(dominant.key()) && materialStock(vup) > 0) {
            return "录播组有素材可剪，发布视频或切片能转化，但标题别太贷款。";
        }
        return "当前乐子人还在外圈观察，可以先稳基本盘，再决定要不要整活。";
    }

    private String recordingLine(int materialStock) {
        if (materialStock > 0) {
            return "素材库存够剪，录播组已经在找能出圈的三十秒。";
        }
        return "素材还少，录播组只能先剪开场和沉默片段。";
    }

    private String instigateLine(Vup vup) {
        if (vup.getWatchHeat() >= 45) {
            return "围观热度升温，弹幕开始把半句话递成选择题。";
        }
        return "弹幕还没坐满，拱火主要停留在试探。";
    }

    private String archiveLine(Vup vup, int debtCount) {
        if (debtCount > 0) {
            return "未结清欠账会吸引考据组，旧标题和截图容易被并排展示。";
        }
        if (vup.getCommercialLevel() >= 35) {
            return "商业味上桌，楼友开始翻你前几天是不是也这么说。";
        }
        return "暂时没有明显旧账，考据组还在收藏夹里潜水。";
    }

    private String memePlayLine(Vup vup) {
        if (vup.getMemeLevel() >= 55) {
            return "同一个梗快进复读期，乐子人开始等回旋镖。";
        }
        return "玩梗欲还在新梗期，偶尔整活能加记忆点。";
    }

    private String toneFor(int value, String base) {
        if (value >= 70) {
            return base + " / 高";
        }
        if (value >= 45) {
            return base + " / 中";
        }
        return base + " / 低";
    }

    private int percent(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return part * 100 / total;
    }

    private int materialStock(Vup vup) {
        return businessLogMapper.countMaterialStockByVupId(vup.getId());
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
