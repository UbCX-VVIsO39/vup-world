package com.example.vupworld.service.event;

import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.EventDtos.EventDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class EventExpansionService {
    private final DeterministicRngService rngService;

    public EventExpansionService(DeterministicRngService rngService) {
        this.rngService = rngService;
    }

    private static final List<EventDTO> GOOD_EVENTS = List.of(
            new EventDTO("GOOD_01", "意外走红", "你的一段直播切片被大V转发", "粉丝+500，人气+200", "GOOD", "🌟"),
            new EventDTO("GOOD_02", "活动预算到位", "一个品牌看中了你的企划潜力", "运营预算+2000，人气+50", "GOOD", "💰"),
            new EventDTO("GOOD_03", "灵感爆发", "你创作了一个爆款内容", "粉丝+200，人气+100，活动预算+500", "GOOD", "💡"),
            new EventDTO("GOOD_04", "参加联动", "你和一位大V进行了联动直播", "粉丝+150，人气+80", "GOOD", "🤝"),
            new EventDTO("GOOD_05", "被官方推荐", "平台算法选中了你", "粉丝+300，人气+150", "GOOD", "📈"),
            new EventDTO("GOOD_06", "粉丝应援", "忠实粉丝为你制作了应援视频", "粉丝+100，人气+60", "GOOD", "❤️"),
            new EventDTO("GOOD_07", "切片爆火", "你的切片在B站首页推荐", "粉丝+600，人气+300", "GOOD", "🔥"),
            new EventDTO("GOOD_08", "被其他V推荐", "其他VUP在直播中推荐了你", "粉丝+250，人气+120", "GOOD", "👋"),
            new EventDTO("GOOD_09", "粉丝制作二创", "粉丝为你的形象制作了二创作品", "粉丝+150，人气+80", "GOOD", "🎨"),
            new EventDTO("GOOD_10", "粉丝应援日", "观众集中参与社群活动支持你的新企划", "活动预算+3000", "GOOD", "💎")
    );

    private static final List<EventDTO> BAD_EVENTS = List.of(
            new EventDTO("BAD_01", "弹幕失控", "外部节奏涌入直播间，房管开始拉慢速和关键词", "粉丝-100，人气-50", "BAD", "😡"),
            new EventDTO("BAD_02", "设备故障", "直播到一半电脑突然蓝屏", "粉丝-50，人气-30，运营预算-200", "BAD", "💻"),
            new EventDTO("BAD_03", "直播事故", "你不小心说了一些不该说的话", "粉丝-200，人气-80", "BAD", "😱"),
            new EventDTO("BAD_04", "被抄袭", "有人抄袭了你的创意内容", "人气-20，运营预算-100", "BAD", "😢"),
            new EventDTO("BAD_05", "误伤举报", "直播间被误伤举报，公告和房管记录要补齐", "粉丝-80，人气-40", "BAD", "🚨")
    );

    private static final List<EventDTO> CHOICE_EVENTS = List.of(
            new EventDTO("CHOICE_01", "节奏刷屏", "弹幕节奏开始刷屏，房管、公告和降温话术要同时上桌", "选择处理方式", "CHOICE", "⚔️"),
            new EventDTO("CHOICE_02", "身份边界危机", "话题被带向现实身份，房管提示不讨论现实身份，只处理直播内容", "选择处理方式", "CHOICE", "🎭"),
            new EventDTO("CHOICE_03", "联动争议", "联动对象的粉丝对你有意见", "选择处理方式", "CHOICE", "🤝"),
            new EventDTO("CHOICE_04", "标题党反噬", "标题太狂内容接不住", "选择处理方式", "CHOICE", "📰"),
            new EventDTO("CHOICE_05", "商业反噬", "老粉觉得直播间味儿变了", "选择处理方式", "CHOICE", "💰")
    );

    private static final List<EventDTO> MEME_EVENTS = List.of(
            new EventDTO("MEME_01", "电子榨菜认证", "观众表示你的直播适合下饭、写作业、当背景音", "真爱粉增加", "MEME", "🥒"),
            new EventDTO("MEME_02", "直播间开庭", "弹幕开始复盘你上周说过的话", "进入选择事件", "MEME", "⚖️"),
            new EventDTO("MEME_03", "切片组连夜上班", "字幕组通宵切出爆点", "切片传播效率提高", "MEME", "✂️"),
            new EventDTO("MEME_04", "社群氛围组", "观众整齐刷屏帮新企划暖场", "活动预算和独角兽关注增长", "MEME", "💕"),
            new EventDTO("MEME_05", "独角兽暴动", "直播间出现她以前不是这样的弹幕", "高亮互动热度保留", "MEME", "🦄"),
            new EventDTO("MEME_06", "活动预算到账", "直播间社群活动奖励被观众集体领取", "活动预算暴击", "MEME", "💰"),
            new EventDTO("MEME_07", "米线保卫战", "玩家认真划清边界", "坏事影响降低", "MEME", "🛡️"),
            new EventDTO("MEME_08", "回旋镖到账", "老切片被翻出", "50%人气暴涨", "MEME", "🪃"),
            new EventDTO("MEME_09", "楼友锐评", "外部论坛开始锐评你的直播内容", "人气增加", "MEME", "📝"),
            new EventDTO("MEME_10", "疯狂星期四刷屏", "粉丝刷屏把企划做成固定梗", "活动预算+50~500", "MEME", "🍗")
    );

    private static final List<EventDTO> LATEGAME_EVENTS = List.of(
            new EventDTO("LATE_01", "二创爆发现象", "粉丝二创数量突然暴增，你的形象在多个平台被传播", "真爱粉+300，围观+50", "GOOD", "🎯"),
            new EventDTO("LATE_02", "回旋镖效应", "早期的某条内容被翻出来，引发了新一轮讨论", "围观+40，梗浓度+5", "MEME", "🪃"),
            new EventDTO("LATE_03", "平台年终盘点", "你的直播入选了平台年度回顾，大量新观众涌入", "粉丝+400，围观+80", "GOOD", "🏆"),
            new EventDTO("LATE_04", "粉丝群体冲突", "不同时期的粉丝群体之间产生了对立", "DD粉-50，真爱粉-30", "BAD", "⚔️"),
            new EventDTO("LATE_05", "后辈效仿风潮", "新人主播开始模仿你的风格", "围观+20，梗浓度+8", "MEME", "🪞")
    );

    public List<EventDTO> getGoodEvents() {
        return GOOD_EVENTS;
    }

    public List<EventDTO> getBadEvents() {
        return BAD_EVENTS;
    }

    public List<EventDTO> getChoiceEvents() {
        return CHOICE_EVENTS;
    }

    public List<EventDTO> getMemeEvents() {
        return MEME_EVENTS;
    }

    public EventDTO getRandomEvent(int day, int fans, int watchHeat, int reputation) {
        List<EventDTO> candidates = candidateEventsFor(day, fans, watchHeat, reputation);
        if (candidates.isEmpty()) return null;
        int index = Math.floorMod(Objects.hash(day, fans, watchHeat, reputation, candidates.size()), candidates.size());
        return candidates.get(index);
    }

    public EventDTO getRandomEventRng(int day, int fans, int watchHeat, int reputation, com.example.vupworld.model.DaySession session) {
        List<EventDTO> candidates = candidateEventsFor(day, fans, watchHeat, reputation);
        if (candidates.isEmpty()) return null;
        var ledger = rngService.createLedger(session);
        int index = ledger.nextInt("event_expansion_pick", candidates.size());
        rngService.finalizeLedger(session, ledger);
        return candidates.get(index);
    }

    public List<EventDTO> candidateEventsFor(int day, int fans, int watchHeat, int reputation) {
        List<EventDTO> candidates = new ArrayList<>();

        if (watchHeat >= 50) {
            candidates.addAll(BAD_EVENTS);
            candidates.addAll(CHOICE_EVENTS);
            candidates.addAll(MEME_EVENTS);
        } else if (reputation >= 70) {
            candidates.addAll(GOOD_EVENTS);
            candidates.addAll(MEME_EVENTS);
        } else {
            candidates.addAll(GOOD_EVENTS);
            candidates.addAll(BAD_EVENTS);
            candidates.addAll(MEME_EVENTS);
        }

        // Day 20+: unlock late-game events for variety
        if (day >= 20) {
            candidates.addAll(LATEGAME_EVENTS);
        }

        return List.copyOf(candidates);
    }

    public List<CandidateReason> candidateReasonsFor(int day, int fans, int watchHeat, int reputation) {
        List<CandidateReason> reasons = new ArrayList<>();
        if (watchHeat >= 50) {
            reasons.add(new CandidateReason("BAD", "watch_heat >= 50 surfaces backlash and moderation events"));
            reasons.add(new CandidateReason("CHOICE", "watch_heat >= 50 turns crowd pressure into player choices"));
            reasons.add(new CandidateReason("MEME", "watch_heat >= 50 keeps remixable public-memory beats active"));
        } else if (reputation >= 70) {
            reasons.add(new CandidateReason("GOOD", "reputation >= 70 favors positive growth events"));
            reasons.add(new CandidateReason("MEME", "reputation >= 70 can convert stable goodwill into memes"));
        } else {
            reasons.add(new CandidateReason("GOOD", "baseline pool keeps growth beats possible"));
            reasons.add(new CandidateReason("BAD", "baseline pool keeps operational risk visible"));
            reasons.add(new CandidateReason("MEME", "baseline pool keeps shareable flavor beats visible"));
        }
        if (day >= 20) {
            reasons.add(new CandidateReason("ENDGAME", "day >= 20 should leave material for ending review"));
        }
        if (fans >= 1000) {
            reasons.add(new CandidateReason("FAN_SCALE", "fans >= 1000 makes audience-scale events easier to justify"));
        }
        return List.copyOf(reasons);
    }

    public int getTotalEventCount() {
        return GOOD_EVENTS.size() + BAD_EVENTS.size() + CHOICE_EVENTS.size() + MEME_EVENTS.size() + LATEGAME_EVENTS.size();
    }

    public record CandidateReason(String eventType, String reason) {
    }
}
