package com.example.vupworld.service.content;

import com.example.vupworld.service.infra.RngLedger;

import com.example.vupworld.service.infra.DeterministicRngService;

import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MemeQuoteService {

    private static final List<String> GENERAL_QUOTES = List.of(
            "标题组提前上班。",
            "切片比正片火。",
            "低压杂谈变相声。",
            "房管手滑禁错人。",
            "DD坐了一站就下车。",
            "平台算法给量给到最抽象那段。",
            "榜一发问，群里气压下降。",
            "老粉没破防，只是写了三千字。",
            "弹幕说今天不录，然后全程录播。",
            "主播想下桌，弹幕把椅子撤了。",
            "这不是开庭，是互联网记忆复习课。",
            "米线施工队今天短暂保住边界。",
            "录播组已经开始调时间轴了。",
            "标题组把标题又往前推了一步。",
            "考据组正在翻旧账。",
            "切片组说素材管够。",
            "楼友已经在开楼了。",
            "推荐位探头，标题组开始调音量。",
            "独角兽开始数互动秒数了。",
            "老粉在群里写小作文。",
            "老粉牌面没有上价值，只是把低压台稳到下个月。",
            "烤肉组翻译名场面前先问二创授权。",
            "粉丝群公告一发，气压从红区回到黄区。",
            "录播组补时间轴，考据组先别急着开庭。",
            "标题组把悬念写成欠条，三天后录播组来收款。",
            "切片组开始等饭点，主播本人负责出餐。",
            "楼友先别抢跑，等日报把证据链补齐。",
            "红温之前先喝水，米线工具别当摆设。",
            "老粉陪伴不是免死金牌，低压台也要按时下播。",
            "企划写得像答辩提纲，老师问流程就切业务日志。",
            "主会场别急着落槌，日报还在路上。"
    );

    private static final Map<String, List<String>> ACTION_QUOTES = Map.of(
            "STREAM_PLAN", List.of(
                    "直播企划已定，标题组开始加班。",
                    "弹幕已经准备好拱火了。",
                    "录播组已经开始调时间轴。",
                    "切片组在等一个名场面。",
                    "开播前先把米线工具摆好，整活也要有护栏。",
                    "企划表写清楚，答辩老师能顺着 Service 调用链往下问。"
            ),
            "PUBLISH_VIDEO", List.of(
                    "视频已发布，切片组开始工作。",
                    "评论区开始给直播间导流。",
                    "标题组说这个标题能再冲一点。",
                    "封面别太抢跑，三天后的回旋镖也算 KPI。",
                    "投稿像交作业，标题、日志、日报都得对上。"
            ),
            "PUBLISH_CLIP", List.of(
                    "切片已发布，乐子人进场。",
                    "录播组说这个素材能剪。",
                    "标题组已经开始查重了。",
                    "切片组开始等饭点，主播本人负责出餐。",
                    "标题组把悬念写成欠条，三天后录播组来收款。"
            ),
            "TRAIN_SONG", List.of(
                    "练歌中，老粉表示欣慰。",
                    "歌力+1，高音预告人又近了一步。",
                    "低压歌回先稳气息，别把陪伴感唱成债务。"
            ),
            "TRAIN_DANCE", List.of(
                    "练舞中，切片组在等动作梗。",
                    "舞力+1，短视频素材又多了一点。",
                    "动作梗可以抽象，膝盖和米线都要保住。"
            ),
            "TRAIN_TALK", List.of(
                    "杂谈复盘中，标题组暂时待机。",
                    "杂谈+1，低压陪伴感又稳了一点。",
                    "红温之前先喝水，米线工具别当摆设。",
                    "楼友先别抢跑，等日报把证据链补齐。"
            ),
            "FAN_GROUP_MAINTAIN", List.of(
                    "粉丝群维护中，小作文标题换了三版。",
                    "房管表示今天气压稳定。",
                    "群公告先讲边界，再讲节目效果。",
                    "老粉陪伴不是免死金牌，低压台也要按时下播。"
            ),
            "NPC_INTERACT", List.of(
                    "同行动态轻轻飘过来。",
                    "DD顺手坐了一站。"
            ),
            "REST", List.of(
                    "休息中，体力恢复中。",
                    "今天没大活，但也没翻车。",
                    "装死不是跑路，是给风向和体力都留缓冲。"
            )
    );

    private static final Map<String, List<String>> MOOD_QUOTES = Map.of(
            "high_heat", List.of(
                    "全网开庭了，楼友已经就位。",
                    "主会场门口已经排队。",
                    "标题组把标题又往前推了一步，录播组已经在调时间轴。",
                    "考据组正在翻旧账，楼友开始拼时间线。",
                    "楼友先别抢跑，等日报把证据链补齐。",
                    "主会场别急着落槌，先看主播有没有补公告。"
            ),
            "low_heat", List.of(
                    "今天风平浪静，直播间还没到开庭时间。",
                    "平台今天口味收紧，标题太冲容易压流。",
                    "低压潜伏中，先攒基本盘。"
            ),
            "high_meme", List.of(
                    "同一个梗又被顶上来，标题组开始担心被查重。",
                    "切片组说素材有点重复，建议换新梗。",
                    "楼里已经开始查重了。",
                    "复读到第三轮就该换包袱，别让抽象变成作业。"
            ),
            "low_meme", List.of(
                    "梗库还没存货，切片组在等新素材。",
                    "标题组说今天没有新活。"
            )
    );

    private final DeterministicRngService deterministicRngService;
    private final ContentCatalogService contentCatalogService;

    public MemeQuoteService(DeterministicRngService deterministicRngService, ContentCatalogService contentCatalogService) {
        this.deterministicRngService = deterministicRngService;
        this.contentCatalogService = contentCatalogService;
    }

    /**
     * Returns meme quotes for a category, preferring DB content with hardcoded fallback.
     */
    private List<String> getQuotes(String category) {
        List<String> dbQuotes = contentCatalogService.getStringList("MEME_QUOTE", category);
        if (!dbQuotes.isEmpty()) {
            return dbQuotes;
        }
        // Fallback to hardcoded
        if ("GENERAL".equals(category)) {
            return GENERAL_QUOTES;
        }
        List<String> actionQuotes = ACTION_QUOTES.get(category);
        if (actionQuotes != null) {
            return actionQuotes;
        }
        List<String> moodQuotes = MOOD_QUOTES.get(category);
        if (moodQuotes != null) {
            return moodQuotes;
        }
        return GENERAL_QUOTES;
    }

    public String getRandomQuote(DaySession session) {
        List<String> quotes = getQuotes("GENERAL");
        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            return quotes.get(ledger.nextInt("meme-quote-general", quotes.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public String getQuoteForAction(String actionType, DaySession session) {
        List<String> quotes = getQuotes(actionType);
        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            return quotes.get(ledger.nextInt("meme-quote-action", quotes.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public String getQuoteForMood(String mood, DaySession session) {
        List<String> quotes = getQuotes(mood);
        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            return quotes.get(ledger.nextInt("meme-quote-mood", quotes.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public int getQuoteCount() {
        int count = GENERAL_QUOTES.size();
        for (List<String> quotes : ACTION_QUOTES.values()) {
            count += quotes.size();
        }
        for (List<String> quotes : MOOD_QUOTES.values()) {
            count += quotes.size();
        }
        // Add DB counts if available
        Map<String, List<ContentCatalogService.ContentEntry>> memeEntries = contentCatalogService.getEntries("MEME_QUOTE");
        int dbCount = 0;
        for (List<ContentCatalogService.ContentEntry> list : memeEntries.values()) {
            dbCount += list.size();
        }
        return Math.max(count, dbCount);
    }
}
