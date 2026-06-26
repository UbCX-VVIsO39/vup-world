package com.example.vupworld.service.event;

import com.example.vupworld.service.infra.RngLedger;

import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.content.ContentCatalogService;

import com.example.vupworld.dto.RandomEventDtos.RandomEventDTO;
import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RandomEventService {

    private final ContentCatalogService contentCatalogService;

    private static final List<RandomEventDTO> EVENTS = List.of(
            new RandomEventDTO(
                    "TITLE_TEAM_EARLY",
                    "标题组提前上班",
                    "标题组今天特别积极，提前把标题准备好了。",
                    "灵感+1",
                    "📝",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "CLIP_POPULAR",
                    "切片比正片火",
                    "你的切片比直播正片还火，切片组表示素材管够。",
                    "人气+20，乐子人+10",
                    "🔥",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "LOW_PRESSURE_COMEDY",
                    "低压杂谈变相声",
                    "今天的低压杂谈意外变成了相声现场，弹幕笑疯了。",
                    "口碑+5，真爱粉+15",
                    "😂",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "MOD_SLIP",
                    "房管手滑禁错人",
                    "房管手速太快，不小心禁言了一个真爱粉。",
                    "口碑-3，房管争议+1",
                    "😅",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "DD_ONE_STOP",
                    "DD坐了一站就下车",
                    "DD看完联动就走了，留下一句下次再来。",
                    "DD+5，但粘性低",
                    "🚌",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "PLATFORM_ALGORITHM",
                    "平台算法给量给到最抽象那段",
                    "平台算法把你最抽象的那段推给了路人。",
                    "人气+30，串味+10",
                    "📈",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "BOSS_QUESTION",
                    "榜一发问，群里气压下降",
                    "榜一在粉丝群问了一个问题，群里瞬间安静。",
                    "商业化+5，独角兽+3",
                    "💰",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "OLD_FAN_ESSAY",
                    "老粉没破防，只是写了三千字",
                    "老粉在群里写了一篇三千字的小作文，标题是《致主播的一封信》。",
                    "真爱粉+10，口碑+3",
                    "📝",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "DANMAKU_LIAR",
                    "弹幕说今天不录，然后全程录播",
                    "弹幕说今天不录了，结果录播组全程在线。",
                    "围观+20",
                    "🎥",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "HOST_ESCAPE",
                    "主播想下桌，弹幕把椅子撤了",
                    "你试图优雅下桌，但弹幕把椅子撤了。",
                    "人气+15，串味+5",
                    "🪑",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "INTERNET_MEMORY",
                    "这不是开庭，是互联网记忆复习课",
                    "楼友把你的历史发言整理成了合集。",
                    "围观+30，串味+10",
                    "📚",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "MELINE_SAVE",
                    "米线施工队今天短暂保住边界",
                    "米线施工队今天成功守住了底线。",
                    "口碑+2，债务-1",
                    "🛡️",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "RECORDING_TEAM_READY",
                    "录播组已经开始调时间轴了",
                    "录播组已经开始准备今天的素材了。",
                    "围观+10",
                    "⏱️",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "TITLE_TEAM_PUSH",
                    "标题组把标题又往前推了一步",
                    "标题组今天特别给力，标题效果很好。",
                    "人气+25，串味+8",
                    "📰",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "RESEARCH_TEAM_OLD_DEBT",
                    "考据组正在翻旧账",
                    "考据组找到了你三天前说的话。",
                    "围观+15，串味+5",
                    "🔍",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "SCHEDULE_PATCH_NOTES",
                    "排班表像版本更新公告",
                    "运营把本周排班拆成了三条补丁说明，观众开始研究路线图。",
                    "口碑+4，真爱粉+8",
                    "🗓️",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "SONG_REQUEST_QUEUE",
                    "点歌队列排到下周",
                    "歌回还没开，点歌队列已经排到了下周三。",
                    "真爱粉+12，歌势证据+1",
                    "🎤",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "COZY_DINNER_TABLE",
                    "陪饭台突然坐满",
                    "你只是安静吃饭，弹幕却把今晚当成固定饭点。",
                    "粘性+10，口碑+4",
                    "🍱",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "FAN_ART_DELIVERY",
                    "画师递来表情包",
                    "画师把你的口癖画成了表情包，群里开始自发扩散。",
                    "真爱粉+8，串味+6",
                    "🎨",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "MOD_PINNED_RULES",
                    "房管把米线钉在公告栏",
                    "房管把边界规则钉到公告栏，今晚的弹幕明显懂事一点。",
                    "口碑+6，风险-1",
                    "📌",
                    "POSITIVE"
            ),
            new RandomEventDTO(
                    "ARCHIVE_TIMESTAMP",
                    "录播组标了黄金时间轴",
                    "录播组把高光片段标好时间轴，切片组不用再盲剪。",
                    "围观+12，素材+1",
                    "⏲️",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "CHAT_INSIDE_JOKE",
                    "弹幕黑话更新",
                    "弹幕发明了新黑话，老粉秒懂，新粉还在查词典。",
                    "串味+8，社群粘性+5",
                    "💬",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "COLLAB_CALENDAR_PING",
                    "联动日历亮了一格",
                    "隔壁主播在日历上给你留了个空位，DD已经开始换乘。",
                    "DD+8，联动证据+1",
                    "📅",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "ALGORITHM_TEST_BUCKET",
                    "平台把你塞进测试桶",
                    "平台测试流量给了你一小段窗口期，来的人有点杂但都看见了。",
                    "围观+18，口碑波动",
                    "🧪",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "OLD_CLIP_RESURFACE",
                    "旧切片突然回锅",
                    "一个旧切片被重新转出来，弹幕开始对比你现在的说话方式。",
                    "围观+16，记忆点+1",
                    "♻️",
                    "NEUTRAL"
            ),
            new RandomEventDTO(
                    "HOT_SEARCH_SIDE_DOOR",
                    "热搜从侧门进来",
                    "你没买热搜，但一个边角词条把路人带进了直播间。",
                    "人气+28，风险+1",
                    "🚪",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "MEME_OVERCOOKED",
                    "梗被复读到有点糊",
                    "同一个梗被弹幕复读太久，乐子人还笑，老粉已经开始叹气。",
                    "串味+12，口碑-2",
                    "🍳",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "BOSS_GIFT_TIMING",
                    "榜一礼物卡在微妙时刻",
                    "榜一在你刚讲边界时送了大礼物，场面一时很难接。",
                    "商业化+8，独角兽+5，风险+1",
                    "🎁",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "DANCE_MOVE_REUSED",
                    "舞步被做成循环素材",
                    "你练舞时一个动作不够准，但刚好适合做成循环素材。",
                    "串味+10，围观+14",
                    "🔁",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "TITLE_TOO_EFFECTIVE",
                    "标题效果好到需要灭火",
                    "标题组这次太会写，点进来的人多了，误读也跟着多了。",
                    "人气+24，口碑-3",
                    "🧯",
                    "MIXED"
            ),
            new RandomEventDTO(
                    "COMMENT_SECTION_TRIAL",
                    "评论区临时开庭",
                    "动态评论区突然开始逐句审稿，运营只好把上下文搬出来。",
                    "围观+20，风险+2",
                    "⚖️",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "FAN_GROUP_TEMPERATURE",
                    "粉丝群气温升高",
                    "粉丝群因为一句玩笑吵起来，老粉开始劝大家先喝水。",
                    "口碑-4，真爱粉压力+1",
                    "🌡️",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "COLLAB_MISREAD",
                    "联动被剪成误读版",
                    "联动里的玩笑被单独剪出，没上下文的人开始认真解读。",
                    "DD+6，口碑-5，风险+1",
                    "✂️",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "BOUNDARY_LINE_BLUR",
                    "边界线被弹幕蹭花",
                    "弹幕把玩笑往前推了一步，你不得不花时间把线重新画清楚。",
                    "风险+2，口碑-2",
                    "🚧",
                    "NEGATIVE"
            ),
            new RandomEventDTO(
                    "SPONSOR_BRIEF_TOO_LONG",
                    "商单口播长到像说明书",
                    "商单口播写得太满，观众开始在弹幕里帮你删减。",
                    "商业化+7，粘性-4",
                    "📄",
                    "NEGATIVE"
            )
    );

    private final DeterministicRngService deterministicRngService;

    public RandomEventService(DeterministicRngService deterministicRngService, ContentCatalogService contentCatalogService) {
        this.deterministicRngService = deterministicRngService;
        this.contentCatalogService = contentCatalogService;
    }

    public RandomEventDTO getRandomEvent(int day, int watchHeat, int memeLevel, DaySession session) {
        List<RandomEventDTO> candidates = candidateEventsFor(day, watchHeat, memeLevel);

        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            return candidates.get(ledger.nextInt("random-event", candidates.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public RandomEventDTO tryTriggerEvent(int day, int watchHeat, int memeLevel, int randomValue, DaySession session) {
        if (randomValue < triggerThresholdFor(day, watchHeat, memeLevel)) {
            return getRandomEvent(day, watchHeat, memeLevel, session);
        }
        return null;
    }

    public List<RandomEventDTO> candidateEventsFor(int day, int watchHeat, int memeLevel) {
        List<RandomEventDTO> allEvents = loadEventsFromCatalog();
        List<RandomEventDTO> candidates = new ArrayList<>();

        for (WeightedCandidate candidate : weightedCandidatesFor(day, watchHeat, memeLevel, allEvents)) {
            for (int i = 0; i < candidate.bias().weight(); i++) {
                candidates.add(candidate.event());
            }
        }

        return candidates.isEmpty() ? allEvents : List.copyOf(candidates);
    }

    private List<RandomEventDTO> loadEventsFromCatalog() {
        Map<String, List<ContentCatalogService.ContentEntry>> entries = contentCatalogService.getEntries("RANDOM_EVENT");
        if (entries.isEmpty()) {
            return EVENTS;
        }
        List<RandomEventDTO> dbEvents = new ArrayList<>();
        for (Map.Entry<String, List<ContentCatalogService.ContentEntry>> entry : entries.entrySet()) {
            for (ContentCatalogService.ContentEntry ce : entry.getValue()) {
                String icon = "";
                String eventType = ce.subKey() != null && !ce.subKey().isBlank() ? ce.subKey() : entry.getKey();
                if (ce.json() != null && !ce.json().isBlank()) {
                    try {
                        int iconIdx = ce.json().indexOf("\"icon\"");
                        if (iconIdx >= 0) {
                            int colonIdx = ce.json().indexOf(':', iconIdx);
                            int startQ = ce.json().indexOf('"', colonIdx + 1);
                            int endQ = ce.json().indexOf('"', startQ + 1);
                            if (startQ >= 0 && endQ >= 0) icon = ce.json().substring(startQ + 1, endQ);
                        }
                        int typeIdx = ce.json().indexOf("\"eventType\"");
                        if (typeIdx >= 0) {
                            int colonIdx = ce.json().indexOf(':', typeIdx);
                            int startQ = ce.json().indexOf('"', colonIdx + 1);
                            int endQ = ce.json().indexOf('"', startQ + 1);
                            if (startQ >= 0 && endQ >= 0) eventType = ce.json().substring(startQ + 1, endQ);
                        }
                    } catch (Exception ignored) {}
                }
                dbEvents.add(new RandomEventDTO(ce.key(), ce.subKey(), ce.text(), "", icon, eventType));
            }
        }
        return dbEvents.isEmpty() ? EVENTS : dbEvents;
    }

    public List<CandidateBias> candidateBiasesFor(int day, int watchHeat, int memeLevel) {
        return weightedCandidatesFor(day, watchHeat, memeLevel, loadEventsFromCatalog()).stream()
                .map(WeightedCandidate::bias)
                .toList();
    }

    public record CandidateBias(String eventId, String eventType, int weight, List<String> reasons) {
    }

    private List<WeightedCandidate> weightedCandidatesFor(int day, int watchHeat, int memeLevel) {
        return weightedCandidatesFor(day, watchHeat, memeLevel, loadEventsFromCatalog());
    }

    private List<WeightedCandidate> weightedCandidatesFor(int day, int watchHeat, int memeLevel, List<RandomEventDTO> events) {
        int heat = clampPercent(watchHeat);
        int meme = clampPercent(memeLevel);
        List<WeightedCandidate> candidates = new ArrayList<>();

        for (RandomEventDTO event : events) {
            BiasScore score = biasScoreFor(event.eventType(), day, heat, meme);
            candidates.add(new WeightedCandidate(
                    event,
                    new CandidateBias(event.id(), event.eventType(), score.weight(), score.reasons())
            ));
        }

        return candidates;
    }

    private BiasScore biasScoreFor(String eventType, int day, int watchHeat, int memeLevel) {
        List<String> reasons = new ArrayList<>();
        int weight = 1;
        reasons.add("base random-event weight");

        int dayWeight = dayBiasFor(eventType, day, reasons);
        weight += dayWeight;

        int heatWeight = heatBiasFor(eventType, watchHeat, reasons);
        weight += heatWeight;

        int memeWeight = memeBiasFor(eventType, memeLevel, reasons);
        weight += memeWeight;

        return new BiasScore(weight, List.copyOf(reasons));
    }

    private int dayBiasFor(String eventType, int day, List<String> reasons) {
        if (day <= 7) {
            if ("POSITIVE".equals(eventType)) {
                reasons.add("day 1-7 opening favors supportive beats");
                return 2;
            }
            if ("NEUTRAL".equals(eventType)) {
                reasons.add("day 1-7 opening keeps light flavor events visible");
                return 1;
            }
            return 0;
        }

        if (day <= 14) {
            if ("MIXED".equals(eventType) || "POSITIVE".equals(eventType)) {
                reasons.add("day 8-14 growth phase favors momentum with some volatility");
                return 1;
            }
            return 0;
        }

        if (day <= 21) {
            if ("MIXED".equals(eventType)) {
                reasons.add("day 15-21 middle phase favors controversy turning into material");
                return 2;
            }
            if ("NEGATIVE".equals(eventType)) {
                reasons.add("day 15-21 middle phase starts surfacing backlash");
                return 1;
            }
            return 0;
        }

        if ("MIXED".equals(eventType) || "NEGATIVE".equals(eventType)) {
            reasons.add("day 22+ endgame favors payoff, backlash, and review material");
            return 2;
        }
        if ("NEUTRAL".equals(eventType)) {
            reasons.add("day 22+ endgame keeps archive and audience-memory events available");
            return 1;
        }
        return 0;
    }

    private int heatBiasFor(String eventType, int watchHeat, List<String> reasons) {
        if (watchHeat >= 70) {
            if ("MIXED".equals(eventType) || "NEGATIVE".equals(eventType)) {
                reasons.add("watch_heat >= 70 favors volatile public reactions");
                return 3;
            }
            if ("NEUTRAL".equals(eventType)) {
                reasons.add("watch_heat >= 70 can still spill into neutral archive events");
                return 1;
            }
            return 0;
        }

        if (watchHeat >= 50) {
            if ("MIXED".equals(eventType)) {
                reasons.add("watch_heat >= 50 favors mixed traffic events");
                return 2;
            }
            if ("NEGATIVE".equals(eventType) || "NEUTRAL".equals(eventType)) {
                reasons.add("watch_heat >= 50 adds some crowd-pressure events");
                return 1;
            }
            return 0;
        }

        if (watchHeat < 30) {
            if ("POSITIVE".equals(eventType)) {
                reasons.add("watch_heat < 30 favors recovery and onboarding events");
                return 2;
            }
            if ("NEUTRAL".equals(eventType)) {
                reasons.add("watch_heat < 30 keeps low-pressure flavor events likely");
                return 1;
            }
        }

        return 0;
    }

    private int memeBiasFor(String eventType, int memeLevel, List<String> reasons) {
        if (memeLevel >= 70) {
            if ("MIXED".equals(eventType)) {
                reasons.add("meme_level >= 70 favors meme spillover with upside and risk");
                return 3;
            }
            if ("NEGATIVE".equals(eventType)) {
                reasons.add("meme_level >= 70 favors backlash from overplayed memes");
                return 2;
            }
            if ("NEUTRAL".equals(eventType)) {
                reasons.add("meme_level >= 70 keeps quote and archive events active");
                return 1;
            }
            return 0;
        }

        if (memeLevel >= 40) {
            if ("MIXED".equals(eventType) || "NEUTRAL".equals(eventType)) {
                reasons.add("meme_level >= 40 favors quote, clip, and remixable events");
                return 2;
            }
            if ("POSITIVE".equals(eventType)) {
                reasons.add("meme_level >= 40 can convert some jokes into goodwill");
                return 1;
            }
            return 0;
        }

        if ("POSITIVE".equals(eventType) || "NEUTRAL".equals(eventType)) {
            reasons.add("meme_level < 40 favors grounded events before meme loops form");
            return 1;
        }

        return 0;
    }

    private record WeightedCandidate(RandomEventDTO event, CandidateBias bias) {
    }

    private record BiasScore(int weight, List<String> reasons) {
    }

    public int triggerThresholdFor(int day, int watchHeat, int memeLevel) {
        int threshold = 20;
        if (day >= 8) {
            threshold += 5;
        }
        if (watchHeat >= 50) {
            threshold += 10;
        }
        if (watchHeat >= 70) {
            threshold += 10;
        }
        if (memeLevel >= 40) {
            threshold += 5;
        }
        if (memeLevel >= 70) {
            threshold += 5;
        }
        return Math.min(55, threshold);
    }

    public int getEventCount() {
        return EVENTS.size();
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
