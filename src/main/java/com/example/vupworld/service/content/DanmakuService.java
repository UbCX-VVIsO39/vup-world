package com.example.vupworld.service.content;

import com.example.vupworld.service.content.ContentCatalogService;

import com.example.vupworld.service.infra.RngLedger;

import com.example.vupworld.service.infra.DeterministicRngService;

import com.example.vupworld.dto.DanmakuDtos.DanmakuDTO;
import com.example.vupworld.dto.DanmakuDtos.DanmakuListDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DanmakuService {

    private final DeterministicRngService deterministicRngService;
    private final ContentCatalogService contentCatalogService;

    public DanmakuService(DeterministicRngService deterministicRngService, ContentCatalogService contentCatalogService) {
        this.deterministicRngService = deterministicRngService;
        this.contentCatalogService = contentCatalogService;
    }

    private static final Map<String, List<String>> DANMAKU_TEMPLATES = Map.ofEntries(
            Map.entry("老粉", List.of(
                    "主播慢慢来，但这次真有点难绷。",
                    "老粉还在，只是椅子坐得更靠后了。",
                    "今天状态不错，别被弹幕架住。",
                    "稳一点也行，别把米线当跳绳。",
                    "老粉没破防，只是在写小作文标题。"
            )),
            Map.entry("切片组", List.of(
                    "别急，时间轴已经打好了。",
                    "这段能剪，标题我都想好了。",
                    "素材+1，先存档再说。",
                    "正片还没结束，切片已经在路上。",
                    "这句有点东西，剪出来应该能活。"
            )),
            Map.entry("录播组", List.of(
                    "全程录着，互联网记忆别急。",
                    "完整版见评论区，别问有没有。",
                    "时间轴已打好，证据链也顺手补了。",
                    "今天不录？那我更要录了。",
                    "录播组已存档，撤回也没用了。"
            )),
            Map.entry("楼友", List.of(
                    "这事不能只看表面，建议开楼。",
                    "主会场已就位，先观望再定调。",
                    "楼里已经开始做时间线了。",
                    "别急，先看后续三天走势。",
                    "这波像小火慢炖，不像当场爆炸。"
            )),
            Map.entry("独角兽", List.of(
                    "她以前不是这样的。",
                    "陪伴感呢？我再确认一下。",
                    "榜一只是问问，没有别的意思。",
                    "联动可以，但别太熟。",
                    "今天群里气压有点低。"
            )),
            Map.entry("陪伴粉", List.of(
                    "陪伴感在线，但排班表别写成恋爱日记。",
                    "今天像低压电台，适合挂着写作业。",
                    "可以甜一点，但别把边界感下播。",
                    "赛博陪伴也要看排班，别预支永动机。",
                    "主播在营业，我在自愿上头。"
            )),
            Map.entry("DD", List.of(
                    "我就路过，但这瓜我先吃一口。",
                    "DD坐了一站就下车。",
                    "认识一下，顺手点个关注。",
                    "我不是单推，我只是刚好在场。",
                    "这个V有点东西，先蹲个切片。"
            )),
            Map.entry("房管", List.of(
                    "已禁言，但弹幕已经截图。",
                    "房管手速拉满，群里开始问谁在急。",
                    "米线施工队今天短暂上班。",
                    "控场中，别把法槌当连点器。",
                    "先别刷屏，主播还在找台阶。"
            )),
            Map.entry("标题组", List.of(
                    "这个标题我能再冲一点。",
                    "标题赢了，内容在后面追。",
                    "热搜预定，但债务也预定了。",
                    "标题组加班中，理智暂时下班。",
                    "普通句子包装一下就有节目效果。"
            )),
            Map.entry("考据组", List.of(
                    "三天前不是这么说的。",
                    "旧切片已调出，时间线正在拼。",
                    "这个梗好像开始回旋了。",
                    "先别急着定性，我去翻录播。",
                    "考据组新建了一个文件夹。"
            )),
            Map.entry("乐子人", List.of(
                    "别急，我已经在录了。",
                    "这波不亏，节目效果到了。",
                    "主播想下桌，弹幕把椅子撤了。",
                    "今天不是开庭，是互联网记忆复习课。",
                    "乐子人嘴上说不看，手上在刷新。"
            )),
            Map.entry("榜一", List.of(
                    "我只是问问，没有别的意思。",
                    "回应醒目留言可以，但别让味儿上桌。",
                    "榜一今天很安静，安静得群里开始不安静。",
                    "排班表不是我做的，我只是关心。",
                    "这句我先记一下。"
            )),
            Map.entry("平台算法", List.of(
                    "平台算法给量给到最抽象那段。",
                    "平台口味今天像在找主会场。",
                    "算法说这段有传播潜力。",
                    "压流不一定，可能只是在等切片。",
                    "数据没说话，但推荐位已经探头。"
            )),
            Map.entry("烤肉组", List.of(
                    "烤肉组已就位，翻译进度同步中。",
                    "名场面已标记，二创授权先问清楚。",
                    "烤肉组今天可以翻几段，但别翻车。",
                    "翻译不是直译，语境先保住。",
                    "烤肉组值班中，弹幕已存双语版。"
            )),
            Map.entry("新观众", List.of(
                    "第一次进来，这是什么直播间？",
                    "从推荐来的，主播在干嘛？",
                    "新人报到，先看看再说。",
                    "路过，弹幕好多，有点看不懂。",
                    "新观众问一下，这个V平时都播什么？"
            )),
            Map.entry("黑粉", List.of(
                    "又来了，看看今天翻不翻车。",
                    "黑粉也是粉，只是方向不同。",
                    "我就看看，不说话。",
                    "评论区已开庭，弹幕跟上。",
                    "黑粉的日常：来、看、截图、走。"
            ))
    );

    /**
     * Returns danmaku templates for a persona, preferring DB content with hardcoded fallback.
     */
    private List<String> getDanmakuTemplates(String persona) {
        List<String> dbTemplates = contentCatalogService.getStringList("DANMAKU_PERSONA", persona);
        if (!dbTemplates.isEmpty()) {
            return dbTemplates;
        }
        return DANMAKU_TEMPLATES.getOrDefault(persona, List.of());
    }

    public DanmakuListDTO generateDanmaku(Vup vup, String actionType, String summary, DaySession session) {
        return generateDanmaku(vup, actionType, summary, session, true);
    }

    /**
     * GET 端点调用：finalize=false，不推进 rng_cursor。
     */
    public DanmakuListDTO generateDanmakuReadOnly(Vup vup, String actionType, String summary, DaySession session) {
        return generateDanmaku(vup, actionType, summary, session, false);
    }

    private DanmakuListDTO generateDanmaku(Vup vup, String actionType, String summary, DaySession session, boolean finalize) {
        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            List<DanmakuDTO> danmakus = new ArrayList<>();

            if ("STREAM_PLAN".equals(actionType)) {
                danmakus.addAll(generateDanmakuFrom(List.copyOf(DANMAKU_TEMPLATES.keySet()), 6 + ledger.nextInt("danmaku-stream-count", 3), ledger));
            } else if ("PUBLISH_VIDEO".equals(actionType) || "PUBLISH_CLIP".equals(actionType)) {
                danmakus.addAll(generateDanmakuFrom(List.of("切片组", "录播组", "标题组", "乐子人", "DD"), 4 + ledger.nextInt("danmaku-clip-count", 2), ledger));
            } else {
                danmakus.addAll(generateDanmakuFrom(List.of("老粉", "DD", "乐子人", "楼友"), 2 + ledger.nextInt("danmaku-default-count", 2), ledger));
            }

            addStateSensitiveDanmaku(vup, danmakus);
            addRouteSensitiveDanmaku(vup, danmakus);
            addPlatformTasteDanmaku(vup, actionType, danmakus);
            addSummaryDanmaku(summary, danmakus);

            ledger.shuffle("danmaku-shuffle", danmakus);
            if (danmakus.size() > 10) {
                danmakus = danmakus.subList(0, 10);
            }

            return new DanmakuListDTO(danmakus, danmakus.size(), getDanmakuMood(vup));
        } finally {
            if (finalize) {
                deterministicRngService.finalizeLedger(session, ledger);
            }
        }
    }

    private List<DanmakuDTO> generateDanmakuFrom(List<String> personas, int count, RngLedger ledger) {
        List<DanmakuDTO> danmakus = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String persona = personas.get(ledger.nextInt("danmaku-persona-" + i, personas.size()));
            List<String> templates = getDanmakuTemplates(persona);
            if (templates.isEmpty()) {
                continue;
            }
            String text = templates.get(ledger.nextInt("danmaku-text-" + i, templates.size()));
            danmakus.add(new DanmakuDTO(persona, text, getToneForPersona(persona)));
        }
        return danmakus;
    }

    private void addStateSensitiveDanmaku(Vup vup, List<DanmakuDTO> danmakus) {
        if (vup.getWatchHeat() >= 70) {
            danmakus.add(new DanmakuDTO("乐子人", "全网开庭了，主会场已就位。", "rose"));
            danmakus.add(new DanmakuDTO("楼友", "楼里已经从复盘变成预判走势了。", "gold"));
        }
        if (vup.getMemeLevel() >= 55) {
            danmakus.add(new DanmakuDTO("考据组", "这个梗又被复读了，查重开始。", "violet"));
            danmakus.add(new DanmakuDTO("切片组", "素材有点重复，但还能包装一下。", "teal"));
        }
        if (vup.getReputation() < 45) {
            danmakus.add(new DanmakuDTO("老粉", "主播要注意口碑啊，别硬接。", "rose"));
            danmakus.add(new DanmakuDTO("房管", "控场中，弹幕截图速度比禁言快。", "teal"));
        }
        if (vup.getCommercialLevel() >= 60) {
            danmakus.add(new DanmakuDTO("榜一", "今天商业味有点上桌了。", "gold"));
        }
        if (vup.getWatchHeat() > 30) {
            List<String> newViewerTemplates = getDanmakuTemplates("新观众");
            if (!newViewerTemplates.isEmpty()) {
                danmakus.add(new DanmakuDTO("新观众", newViewerTemplates.get(0), getToneForPersona("新观众")));
            }
        }
        if (vup.getReputation() < 40) {
            List<String> haterTemplates = getDanmakuTemplates("黑粉");
            if (!haterTemplates.isEmpty()) {
                danmakus.add(new DanmakuDTO("黑粉", haterTemplates.get(0), getToneForPersona("黑粉")));
            }
        }
        List<String> roastTemplates = getDanmakuTemplates("烤肉组");
        if (!roastTemplates.isEmpty()) {
            danmakus.add(new DanmakuDTO("烤肉组", roastTemplates.get(0), getToneForPersona("烤肉组")));
        }
    }

    private void addRouteSensitiveDanmaku(Vup vup, List<DanmakuDTO> danmakus) {
        switch (vup.getCurrentRoute() == null ? "UNKNOWN" : vup.getCurrentRoute()) {
            case "SINGING_IDOL" -> danmakus.add(new DanmakuDTO(
                    "歌势民",
                    vup.getSongPower() >= 30
                            ? "高音可以预告，但气口和咬字得现货交付。"
                            : "先别预支高音，今天把气口站稳就算赢。",
                    "teal"
            ));
            case "SLICE_SAINT" -> danmakus.add(new DanmakuDTO(
                    "切片民",
                    "素材库有货，切片组才不会饿着剪。",
                    "teal"
            ));
            case "BLACK_RED_MAIN_STAGE" -> danmakus.add(new DanmakuDTO(
                    "楼友",
                    "主会场不是不能坐，先把米线图铺好。",
                    "gold"
            ));
            case "SOCIAL_COLLAB" -> danmakus.add(new DanmakuDTO(
                    "DD",
                    "DD可以坐公交，但别把老粉站台拆了。",
                    "violet"
            ));
            case "DANCE_MEME" -> danmakus.add(new DanmakuDTO(
                    "鬼畜组",
                    "这个动作如果稳，二创区今天能开工。",
                    "violet"
            ));
            case "ELECTRONIC_PICKLE" -> danmakus.add(new DanmakuDTO(
                    "老粉",
                    "低压可以，但别低到连标题组都下班。",
                    "rose"
            ));
            case "CYBER_GIRLFRIEND" -> danmakus.add(new DanmakuDTO(
                    "陪伴粉",
                    "陪伴感可以营业，但排班表别写成恋爱日记。",
                    "rose"
            ));
            default -> {
            }
        }
    }

    private void addPlatformTasteDanmaku(Vup vup, String actionType, List<DanmakuDTO> danmakus) {
        int day = vup.getDayCount();
        if (day >= 1 && day <= 7 && ("TRAIN_TALK".equals(actionType) || "FAN_GROUP_MAINTAIN".equals(actionType))) {
            danmakus.add(new DanmakuDTO(
                    "平台算法",
                    "清朗低压周在给稳健内容递话筒，标题组先别急着预判开庭。",
                    "violet"
            ));
            return;
        }
        if (day >= 8 && day <= 14 && ("TRAIN_SONG".equals(actionType) || "PUBLISH_VIDEO".equals(actionType))) {
            danmakus.add(new DanmakuDTO(
                    "平台算法",
                    "歌回扶持周正在推基本功，高音可以预告，但气口和咬字要现货交付。",
                    "violet"
            ));
            return;
        }
        if (day >= 15 && day <= 21 && "PUBLISH_CLIP".equals(actionType)) {
            danmakus.add(new DanmakuDTO(
                    "平台算法",
                    "抽象出圈周给切片开了风口，乐子人来得快，米线也会亮得快。",
                    "violet"
            ));
            return;
        }
        if (day >= 22 && day <= 30 && ("FAN_GROUP_MAINTAIN".equals(actionType) || "STREAM_PLAN".equals(actionType))) {
            danmakus.add(new DanmakuDTO(
                    "平台算法",
                    "商业复审周在看稳定排班，老板预算想上桌，老粉也在看味儿变没变。",
                    "violet"
            ));
        }
    }

    private void addSummaryDanmaku(String summary, List<DanmakuDTO> danmakus) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        String brief = summary.length() > 26 ? summary.substring(0, 26) + "..." : summary;
        danmakus.add(new DanmakuDTO("日报组", "今日摘要已存档：" + brief, "gold"));
    }

    private String getToneForPersona(String persona) {
        return switch (persona) {
            case "老粉", "独角兽", "陪伴粉" -> "rose";
            case "切片组", "录播组", "房管", "歌势民", "切片民", "烤肉组" -> "teal";
            case "楼友", "考据组", "榜一" -> "gold";
            case "DD", "乐子人", "标题组", "平台算法", "鬼畜组", "新观众" -> "violet";
            case "黑粉" -> "rose";
            default -> "teal";
        };
    }

    private String getDanmakuMood(Vup vup) {
        if (vup.getWatchHeat() >= 70) {
            return "弹幕爆炸";
        }
        if (vup.getWatchHeat() >= 40) {
            return "弹幕活跃";
        }
        if (vup.getPopularity() >= 60) {
            return "弹幕正常";
        }
        return "弹幕稀疏";
    }
}
