package com.example.vupworld.service;

import com.example.vupworld.service.report.ReportService;

import com.example.vupworld.service.risk.ActionFatigueService;

import com.example.vupworld.service.risk.DebtService;

import com.example.vupworld.service.core.VupStateMapper;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.progression.StageObjectiveService;

import com.example.vupworld.service.content.PlatformTrendService;

import com.example.vupworld.service.fan.PersonaTagService;

import com.example.vupworld.service.fan.AudiencePressureService;

import com.example.vupworld.service.fan.NpcRelationshipService;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.service.infra.BalanceConfig;
import com.example.vupworld.service.content.ContentCatalogService;
import com.example.vupworld.service.event.FormalEventPresenter;
import com.example.vupworld.service.event.PendingInteractionPresenter;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.ActionDtos.ChooseTitleRequest;
import com.example.vupworld.dto.ActionDtos.DanmakuAccumulateDTO;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.ActionDtos.FatigueInfoDTO;
import com.example.vupworld.dto.ActionDtos.GiftAccumulateDTO;
import com.example.vupworld.dto.ActionDtos.RerollTitleRequest;
import com.example.vupworld.dto.ActionDtos.SendDanmakuRequest;
import com.example.vupworld.dto.ActionDtos.SendGiftRequest;
import com.example.vupworld.dto.ActionDtos.StreamPlanOptionDTO;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.dto.ActionDtos.TitleRerollResultDTO;
import com.example.vupworld.dto.ActionDtos.ActionResultDTO;
import com.example.vupworld.dto.InteractionDtos.InteractionChoiceDTO;
import com.example.vupworld.dto.InteractionDtos.InteractionEventDTO;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class TitleService {
    private static final String CHOOSE_TITLE_PATH = "/api/stream/title/choose";
    private static final String REROLL_TITLE_PATH = "/api/stream/titles/reroll";
    private static final int TITLE_REROLL_LIMIT = 2;
    private static final List<StreamPlanOptionDTO> STREAM_PLAN_OPTIONS = List.of(
            new StreamPlanOptionDTO("TALK", "杂谈回"),
            new StreamPlanOptionDTO("SINGING", "唱歌回"),
            new StreamPlanOptionDTO("DANCE", "舞蹈回"),
            new StreamPlanOptionDTO("GAME", "游戏回"),
            new StreamPlanOptionDTO("SURPRISE", "突击回"),
            new StreamPlanOptionDTO("MARSHMALLOW", "棉花糖回"),
            new StreamPlanOptionDTO("SHARP_COMMENT", "锐评回"),
            new StreamPlanOptionDTO("ENDURANCE", "耐久回"),
            new StreamPlanOptionDTO("COLLAB", "联动回"),
            new StreamPlanOptionDTO("SING_TALK", "歌杂混合回"),
            new StreamPlanOptionDTO("SHORT_CHALLENGE", "短视频挑战"),
            new StreamPlanOptionDTO("SC_THANKS", "醒目留言回应回")
    );

    private final VupService vupService;
    private final VupMapper vupMapper;
    private final DaySessionMapper daySessionMapper;
    private final BusinessLogMapper businessLogMapper;
    private final ReportService reportService;
    private final JsonService jsonService;
    private final RequestHashService requestHashService;
    private final IdempotencyRunner idempotencyRunner;
    private final VupStateMapper vupStateMapper;
    private final DebtService debtService;
    private final ActionFatigueService actionFatigueService;
    private final FormalEventPresenter formalEventPresenter;
    private final PendingInteractionPresenter pendingInteractionPresenter;
    private final DayFlowService dayFlowService;
    private final ActionService actionService;
    private final AudiencePressureService audiencePressureService;
    private final StageObjectiveService stageObjectiveService;
    private final NpcRelationshipService npcRelationshipService;
    private final BalanceConfig balanceConfig;
    private final ContentCatalogService contentCatalogService;

    public TitleService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            BusinessLogMapper businessLogMapper,
            ReportService reportService,
            JsonService jsonService,
            RequestHashService requestHashService,
            IdempotencyRunner idempotencyRunner,
            VupStateMapper vupStateMapper,
            DebtService debtService,
            ActionFatigueService actionFatigueService,
            FormalEventPresenter formalEventPresenter,
            PendingInteractionPresenter pendingInteractionPresenter,
            DayFlowService dayFlowService,
            @Lazy ActionService actionService,
            AudiencePressureService audiencePressureService,
            StageObjectiveService stageObjectiveService,
            NpcRelationshipService npcRelationshipService,
            BalanceConfig balanceConfig,
            ContentCatalogService contentCatalogService
    ) {
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.daySessionMapper = daySessionMapper;
        this.businessLogMapper = businessLogMapper;
        this.reportService = reportService;
        this.jsonService = jsonService;
        this.requestHashService = requestHashService;
        this.idempotencyRunner = idempotencyRunner;
        this.vupStateMapper = vupStateMapper;
        this.debtService = debtService;
        this.actionFatigueService = actionFatigueService;
        this.formalEventPresenter = formalEventPresenter;
        this.pendingInteractionPresenter = pendingInteractionPresenter;
        this.dayFlowService = dayFlowService;
        this.actionService = actionService;
        this.audiencePressureService = audiencePressureService;
        this.stageObjectiveService = stageObjectiveService;
        this.npcRelationshipService = npcRelationshipService;
        this.balanceConfig = balanceConfig;
        this.contentCatalogService = contentCatalogService;
    }

    public List<StreamPlanOptionDTO> streamPlans() {
        return STREAM_PLAN_OPTIONS;
    }

    /**
     * 礼物累积：直播中收到的礼物先记到 day_session，等标题结算时折算为 watchHeat + coin。
     */
    @Transactional
    public GiftAccumulateDTO accumulateGift(Long userId, SendGiftRequest request) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        int count = Math.max(1, request.qty());
        int coinValue = count;
        session.setGiftCount(session.getGiftCount() + count);
        session.setGiftCoinValue(session.getGiftCoinValue() + coinValue);
        daySessionMapper.updateAfterAction(session);
        return new GiftAccumulateDTO(session.getGiftCount(), session.getGiftCoinValue(), vup.getCoin());
    }

    /**
     * 弹幕累积：直播中弹幕密度提升 watchHeat，负面弹幕累计过多会扣口碑（结算时应用）。
     */
    @Transactional
    public DanmakuAccumulateDTO accumulateDanmaku(Long userId, SendDanmakuRequest request) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        boolean negative = "NEGATIVE".equalsIgnoreCase(request.mood());
        int heatDelta = negative ? -1 : 1;
        int reputationPenalty = negative ? balanceConfig.danmakuNegativeReputationPenalty() : 0;
        session.setDanmakuCount(session.getDanmakuCount() + 1);
        session.setDanmakuHeat(session.getDanmakuHeat() + heatDelta);
        daySessionMapper.updateAfterAction(session);
        return new DanmakuAccumulateDTO(session.getDanmakuCount(), session.getDanmakuHeat(), reputationPenalty);
    }

    public List<TitleOptionDTO> savedTitleCandidates(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        if (!DayPhase.NEED_TITLE.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "当前没有待选标题，标题组还没上班。");
        }
        return jsonService.readTitleOptions(session.getTitleCandidatesJson());
    }

    @Transactional
    public TitleRerollResultDTO rerollTitles(Long userId, RerollTitleRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, TitleRerollResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(REROLL_TITLE_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, REROLL_TITLE_PATH, idempotencyKey, requestHash,
                () -> doRerollTitles(vup, session, request),
                TitleRerollResultDTO.class, buildRecord);
    }

    @Transactional
    public DayResultDTO chooseTitle(Long userId, ChooseTitleRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(CHOOSE_TITLE_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, CHOOSE_TITLE_PATH, idempotencyKey, requestHash,
                () -> doChooseTitle(userId, vup, session, request, idempotencyKey, requestHash, buildRecord),
                DayResultDTO.class, buildRecord);
    }

    List<TitleOptionDTO> titleCandidatesFor(String planType, int day) {
        if ("TALK".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(11L, "深夜低压聊天，陪大家下饭", "SAFE", "真爱粉和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(12L, "聊聊最近新人V怎么活下来", "BAIT_TRAFFIC", "人气和杂谈热度上升", "标题党风险", "warning"),
                    new TitleOptionDTO(13L, "今天有话直说，别录屏太快", "HARD_MOUTH", "围观热度和主会场期待上升", "开庭和米线风险", "trial")
            );
        }
        if ("SINGING".equals(planType)) {
            if (isSingingBoostWeek(day)) {
                return List.of(
                        new TitleOptionDTO(21L, "低压歌回，今天不证明自己", "SAFE", "歌回扶持周有平台口味加成，稳唱更容易进推荐", "风险低", "safe"),
                        new TitleOptionDTO(22L, "新人歌势第一次认真交作业", "BAIT_TRAFFIC", "歌回扶持周人气和歌势期待上升", "实力审判风险", "warning"),
                        new TitleOptionDTO(23L, "不会真有人觉得我唱不了高音吧", "HARD_MOUTH", "歌回扶持周围观热度更高", "破音事故和回旋镖风险", "trial")
                );
            }
            return List.of(
                    new TitleOptionDTO(21L, "低压歌回，今天不证明自己", "SAFE", "真爱粉和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(22L, "新人歌势第一次认真交作业", "BAIT_TRAFFIC", "人气和歌势期待上升", "实力审判风险", "warning"),
                    new TitleOptionDTO(23L, "不会真有人觉得我唱不了高音吧", "HARD_MOUTH", "围观热度更高", "破音事故和回旋镖风险", "trial")
            );
        }
        if ("GAME".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(41L, "低压游戏回，输了也不急", "SAFE", "陪伴和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(42L, "今晚上分，弹幕不许笑", "BAIT_TRAFFIC", "人气和DD小涨", "操作审判风险", "warning"),
                    new TitleOptionDTO(43L, "这把我来指挥，队友先别急", "HARD_MOUTH", "围观热度和串味上升", "坐牢切片风险", "trial")
            );
        }
        if ("SURPRISE".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(51L, "突击十分钟，看看麦好没好", "SAFE", "灵感和低风险收益更稳", "风险低", "safe"),
                    new TitleOptionDTO(52L, "突击开播，看看谁还醒着", "BAIT_TRAFFIC", "人气和小涨粉提高", "准备不足风险", "warning"),
                    new TitleOptionDTO(53L, "临时开播，主打一个没准备", "ABSTRACT_MEME", "串味和乐子人上升", "内容事故风险", "funny")
            );
        }
        if ("ENDURANCE".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(81L, "低强度陪伴耐久，别卷主播", "SAFE", "真爱粉和陪伴更稳", "风险低", "safe"),
                    new TitleOptionDTO(82L, "今天试试能播多久", "BAIT_TRAFFIC", "人气和陪伴期待上升", "过劳风险", "warning"),
                    new TitleOptionDTO(83L, "不下播挑战，谁先困谁输", "ABSTRACT_MEME", "串味和人气上升", "嗓子/体力债务风险", "funny")
            );
        }
        if ("SING_TALK".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(101L, "唱几首，聊几句，慢慢来", "SAFE", "真爱粉和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(102L, "歌杂混合，看看今晚哪边更有活", "BAIT_TRAFFIC", "人气和路线平衡上升", "期待落差风险", "warning"),
                    new TitleOptionDTO(103L, "唱歌不够，杂谈来凑", "ABSTRACT_MEME", "串味和切片素材上升", "歌势审判风险", "funny")
            );
        }
        if ("SC_THANKS".equals(planType)) {
            if (isCommercialReviewWeek(day)) {
                return List.of(
                        new TitleOptionDTO(121L, "慢慢谢，老板别卷", "SAFE", "商业复审周低压陪伴更稳，口碑和老粉稳定", "风险低", "safe"),
                        new TitleOptionDTO(122L, "今天把欠的高亮互动都还了", "BUSINESS_SAFE", "商业复审周运营预算和商业化提高，稳定排班更容易过审", "商业味上桌", "business"),
                        new TitleOptionDTO(123L, "榜一问题能不能不要这么懂", "FAN_SERVICE", "商业复审周运营预算和商业化提高，独角兽和高亮互动热度上升", "独角兽期待", "fan_service")
                );
            }
            return List.of(
                    new TitleOptionDTO(121L, "慢慢谢，老板别卷", "SAFE", "口碑和老粉稳定", "风险低", "safe"),
                    new TitleOptionDTO(122L, "今天把欠的高亮互动都还了", "BUSINESS_SAFE", "运营预算和商业化上升", "商业味上桌", "business"),
                    new TitleOptionDTO(123L, "榜一问题能不能不要这么懂", "FAN_SERVICE", "独角兽和高亮互动热度上升", "独角兽期待", "fan_service")
            );
        }
        if ("MARSHMALLOW".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(61L, "只读可爱的，攻击性太强先叉出去", "SAFE", "真爱粉和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(62L, "今晚什么都能问，轻点拷打", "BAIT_TRAFFIC", "人气和围观热度小涨", "棉花糖标题党风险", "warning"),
                    new TitleOptionDTO(63L, "逆天棉花糖专场，房管先别急", "HARD_MOUTH", "节目效果和围观热度上升", "棉花糖事故风险", "trial")
            );
        }
        if ("SHARP_COMMENT".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(71L, "理性复盘，不点名不开团", "SAFE", "口碑和杂谈力更稳", "风险低", "safe"),
                    new TitleOptionDTO(72L, "聊聊最近直播区版本答案", "BAIT_TRAFFIC", "人气和乐子人上升", "蹭热度债务风险", "warning"),
                    new TitleOptionDTO(73L, "锐评前先声明不点名不挂人", "HARD_MOUTH", "高人气和围观热度上升", "节奏债和同台避嫌风险", "trial")
            );
        }
        if ("COLLAB".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(91L, "低压联动，认识新朋友", "SAFE", "DD和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(92L, "和隔壁一起试试版本答案", "BAIT_TRAFFIC", "DD和人气上升", "蹭热度风险", "warning"),
                    new TitleOptionDTO(93L, "联动前先写清不引战公告", "HARD_MOUTH", "围观热度和节目效果上升", "独角兽期待和同台避嫌风险", "trial")
            );
        }
        if ("SHORT_CHALLENGE".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(111L, "低压短视频挑战，失败也算素材", "SAFE", "灵感和低风险收益更稳", "风险低", "safe"),
                    new TitleOptionDTO(112L, "跟一下今天的热门挑战", "BAIT_TRAFFIC", "人气和切片热度上升", "梗疲劳风险", "warning"),
                    new TitleOptionDTO(113L, "这也能火？我来复刻一下", "ABSTRACT_MEME", "串味和乐子人上升", "动作翻车风险", "funny")
            );
        }
        if ("DANCE".equals(planType)) {
            return List.of(
                    new TitleOptionDTO(31L, "低强度练舞，动作慢慢来", "SAFE", "舞力和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(32L, "挑战一下最近很火的舞", "BAIT_TRAFFIC", "短视频人气上升", "动作对比风险", "warning"),
                    new TitleOptionDTO(33L, "这动作很难吗？我试试", "ABSTRACT_MEME", "梗舞和乐子上升", "鬼畜素材风险", "funny")
            );
        }
        return List.of(
                new TitleOptionDTO(planIdFor(planType) * 10 + 1, "低压开播，今天先稳住", "SAFE", "口碑更稳", "风险低", "safe"),
                new TitleOptionDTO(planIdFor(planType) * 10 + 2, "试试今天的版本答案", "BAIT_TRAFFIC", "人气小涨", "标题党风险", "warning"),
                new TitleOptionDTO(planIdFor(planType) * 10 + 3, "主打一个没准备但有节目效果", "ABSTRACT_MEME", "串味和乐子上升", "梗疲劳风险", "funny")
        );
    }

    List<TitleOptionDTO> titleCandidatesFor(Vup vup, DaySession session, String planType) {
        List<TitleOptionDTO> base = titleCandidatesFor(planType, session.getDay());
        List<TitleOptionDTO> inherited = inheritedTitleCandidates(vup);
        if (inherited.isEmpty()) {
            return enrichTitleCandidates(vup, session, base);
        }
        List<TitleOptionDTO> combined = new ArrayList<>(base);
        combined.addAll(inherited);
        return enrichTitleCandidates(vup, session, combined);
    }

    /**
     * 继承标题池：如果 vup 有 previousEndingId（前世结局），从 game_content 的 INHERITED_TITLE 类目加载专属标题。
     */
    private List<TitleOptionDTO> inheritedTitleCandidates(Vup vup) {
        if (vup.getPreviousEndingId() == null) {
            return List.of();
        }
        Map<String, List<ContentCatalogService.ContentEntry>> entries = contentCatalogService.getEntries("INHERITED_TITLE");
        if (entries.isEmpty()) {
            return List.of();
        }
        List<TitleOptionDTO> result = new ArrayList<>();
        for (List<ContentCatalogService.ContentEntry> entryList : entries.values()) {
            for (ContentCatalogService.ContentEntry ce : entryList) {
                result.add(new TitleOptionDTO(
                        900L + result.size(),
                        ce.text(),
                        "INHERITED",
                        "继承前世结局的专属标题，自带路线倾向",
                        "继承标题无额外旧账风险",
                        "inherited"
                ));
            }
        }
        return result;
    }

    private List<TitleOptionDTO> enrichTitleCandidates(Vup vup, DaySession session, List<TitleOptionDTO> candidates) {
        return candidates.stream()
                .map(title -> enrichTitleCandidate(vup, session, title))
                .toList();
    }

    private TitleOptionDTO enrichTitleCandidate(Vup vup, DaySession session, TitleOptionDTO title) {
        TitlePreview preview = titlePreview(vup, session, title);
        return new TitleOptionDTO(
                title.id(),
                title.titleText(),
                title.style(),
                preview.effectLine(),
                preview.riskLine(),
                title.reportTone()
        );
    }

    private TitlePreview titlePreview(Vup vup, DaySession session, TitleOptionDTO title) {
        boolean safeTitle = "SAFE".equals(title.style());
        boolean blackRedTitle = "HARD_MOUTH".equals(title.style()) || "ABSTRACT_MEME".equals(title.style());
        boolean danceMemePlan = isDanceMemePlan(session.getSelectedPlanId());
        boolean danceMemeTitle = danceMemePlan && ("BAIT_TRAFFIC".equals(title.style()) || "ABSTRACT_MEME".equals(title.style()));
        blackRedTitle = blackRedTitle && !danceMemeTitle;
        boolean fanServiceTitle = "FAN_SERVICE".equals(title.style());
        boolean businessSafeTitle = "BUSINESS_SAFE".equals(title.style());
        boolean commercialReviewWeek = isCommercialReviewWeek(session.getDay());
        boolean singingBoostWeek = isSingingBoostWeek(session.getDay());
        boolean singingSafeTitle = isSingingPlan(session) && safeTitle;
        boolean practiceToStreamCombo = practiceToStreamComboActive(vup, session);
        boolean videoToStreamCombo = videoToStreamComboActive(vup, session);

        int trueFanChange = fanServiceTitle ? 8 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 36 : 28) : 10;
        int funFanChange = danceMemeTitle ? 18 : blackRedTitle ? 20 : 0;
        int unicornFanChange = fanServiceTitle ? 30 : safeTitle || businessSafeTitle ? 4 : 0;
        int ddFanChange = fanServiceTitle ? 1 : 4;
        boolean newPlayerProtection = session.getDay() <= 3 && (blackRedTitle || danceMemeTitle);
        if (newPlayerProtection) {
            funFanChange = funFanChange * 6 / 10;
            trueFanChange = trueFanChange * 6 / 10;
            ddFanChange = ddFanChange * 6 / 10;
        }
        if (practiceToStreamCombo) {
            trueFanChange += 8;
        }
        if (videoToStreamCombo) {
            funFanChange += 8;
        }
        int popularityGain = fanServiceTitle ? 100 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 95 : 80) : 140;
        int reputationGain = fanServiceTitle ? 1 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 4 : 3) : -3;
        int watchHeatGain = fanServiceTitle ? 4 : safeTitle ? 5 : 20;
        if (practiceToStreamCombo) {
            popularityGain += 15;
            reputationGain += 1;
            watchHeatGain += 1;
        }
        if (videoToStreamCombo) {
            popularityGain += 30;
            watchHeatGain += 3;
        }
        int coinChange = coinChangeForTitle(title, session.getDay());
        String targetRoute = titleTargetRoute(vup, session, danceMemeTitle, blackRedTitle, fanServiceTitle, businessSafeTitle);
        int routeScoreChange = danceMemeTitle || blackRedTitle ? 4 : fanServiceTitle || businessSafeTitle ? 2 : 3;
        int commercialGain = 0;
        if (fanServiceTitle || businessSafeTitle) {
            commercialGain = commercialReviewWeek
                    ? (fanServiceTitle ? 6 : 4)
                    : (fanServiceTitle ? 4 : 3);
        }

        AudiencePressureService.AudiencePressure audiencePressure =
                audiencePressureService.pressureForRoute(vup, session.getDay(), targetRoute);
        if (audiencePressure.active()) {
            int keepPercent = audiencePressureService.fanKeepPercent(audiencePressure);
            trueFanChange = trueFanChange * keepPercent / 100;
            funFanChange = funFanChange * keepPercent / 100;
            unicornFanChange = unicornFanChange * keepPercent / 100;
            ddFanChange = ddFanChange * keepPercent / 100;
            reputationGain += audiencePressureService.reputationPenalty(audiencePressure);
            watchHeatGain += audiencePressureService.watchHeatGain(audiencePressure);
        }

        StageObjectiveService.StageMomentum stageMomentum = stageObjectiveService.stageMomentumFor(vup);
        ActionService.StageMomentumDelta stageMomentumDelta =
                actionService.stageMomentumDeltaFor(stageMomentum, ActionType.STREAM_PLAN.name(), targetRoute);
        if (stageMomentumDelta.applied()) {
            trueFanChange += stageMomentumDelta.trueFanChange();
            popularityGain += stageMomentumDelta.popularityChange();
            watchHeatGain += stageMomentumDelta.watchHeatChange();
            reputationGain += stageMomentumDelta.reputationChange();
            routeScoreChange += stageMomentumDelta.routeScoreChange();
        }

        ActionService.ComboImpact comboImpact = actionService.comboImpactFor(vup, session, ActionType.STREAM_PLAN, targetRoute);
        if (comboImpact.applied()) {
            trueFanChange += comboImpact.trueFanBonus();
            funFanChange += comboImpact.funFanBonus();
            ddFanChange += comboImpact.ddFanBonus();
            popularityGain += comboImpact.popularityBonus();
            watchHeatGain += comboImpact.watchHeatBonus();
            reputationGain += comboImpact.reputationBonus();
            routeScoreChange += comboImpact.routeScoreBonus();
        }

        ActionService.StageObjectiveBonus stageObjectiveBonus =
                actionService.stageObjectiveBonusFor(vup, session, ActionType.STREAM_PLAN, targetRoute);
        if (stageObjectiveBonus.applied()) {
            trueFanChange += stageObjectiveBonus.trueFanBonus();
            popularityGain += stageObjectiveBonus.popularityBonus();
            reputationGain += stageObjectiveBonus.reputationBonus();
            routeScoreChange += stageObjectiveBonus.routeScoreBonus();
        }

        ActionService.PlatformTrendModifier platformTrendModifier =
                actionService.platformTrendModifierFor(session.getDay(), ActionType.STREAM_PLAN, targetRoute, title.style(), session.getSelectedPlanId());
        if (platformTrendModifier.applied()) {
            trueFanChange += platformTrendModifier.trueFanBonus();
            funFanChange += platformTrendModifier.funFanBonus();
            ddFanChange += platformTrendModifier.ddFanBonus();
            popularityGain += platformTrendModifier.popularityBonus();
            watchHeatGain += platformTrendModifier.watchHeatBonus();
            reputationGain += platformTrendModifier.reputationBonus();
            coinChange += platformTrendModifier.coinBonus();
            commercialGain += platformTrendModifier.commercialBonus();
            routeScoreChange += platformTrendModifier.routeScoreBonus();
        }

        ActionService.FortuneModifier fortuneModifier =
                actionService.fortuneModifierFor(vup, session, ActionType.STREAM_PLAN, !safeTitle || fanServiceTitle);
        if (fortuneModifier.applied()) {
            trueFanChange += fortuneModifier.trueFanBonus();
            popularityGain += fortuneModifier.popularityBonus();
            watchHeatGain += fortuneModifier.watchHeatBonus();
            reputationGain += fortuneModifier.reputationBonus();
            routeScoreChange += fortuneModifier.routeScoreBonus();
        }

        // 礼物/弹幕热度加成预览（不修改 vup，仅展示）
        int giftHeatBonus = session.getGiftCount() > 0
                ? Math.min(balanceConfig.giftHeatBonusCap(), session.getGiftCoinValue() / balanceConfig.giftHeatBonusPerCoin())
                : 0;
        if (giftHeatBonus > 0) {
            watchHeatGain += giftHeatBonus;
            coinChange += session.getGiftCoinValue();
        }
        int danmakuHeatBonus = Math.min(balanceConfig.danmakuHeatBonusCap(), Math.max(0, session.getDanmakuHeat()));
        if (danmakuHeatBonus > 0) {
            watchHeatGain += danmakuHeatBonus;
        }
        if (session.getDanmakuCount() > 0 && session.getDanmakuHeat() < 0) {
            reputationGain -= balanceConfig.danmakuNegativeReputationPenalty();
        }

        int fanChange = Math.max(0, trueFanChange + funFanChange + unicornFanChange + ddFanChange);
        String effectLine = "预计 粉丝+" + fanChange
                + "，人气+" + Math.max(0, popularityGain)
                + "，围观+" + Math.max(0, watchHeatGain)
                + signedMetric("，口碑", reputationGain)
                + (coinChange > 0 ? "，运营预算+" + coinChange : "")
                + (commercialGain > 0 ? "，商业化+" + commercialGain : "")
                + "，" + routeLabel(targetRoute) + "路线+" + Math.max(0, routeScoreChange);
        if (platformTrendModifier.applied()) {
            effectLine += platformTrendModifier.matched()
                    ? "；" + platformTrendModifier.label() + "顺风：" + platformTrendModifier.primaryDeltaLabel()
                    : "；" + platformTrendModifier.label() + "逆风：" + platformTrendModifier.primaryDeltaLabel();
            if (singingSafeTitle && singingBoostWeek && !effectLine.contains("推荐")) {
                effectLine += "，更容易进推荐";
            }
        }
        if (stageObjectiveBonus.applied()) {
            effectLine += "；阶段委托：" + stageObjectiveBonus.primaryDeltaLabel();
        }
        if (comboImpact.applied()) {
            effectLine += "；连招：" + comboImpact.label();
        }

        String riskLine = titleDebtPreview(vup, session, title);
        if (audiencePressure.active()) {
            riskLine += "；" + audiencePressure.label() + "：" + audiencePressure.hint();
        }
        if (newPlayerProtection) {
            riskLine += "；新人保护：前3天黑红收益被压低";
        }
        return new TitlePreview(effectLine, riskLine);
    }

    private String signedMetric(String label, int value) {
        if (value == 0) {
            return label + "+0";
        }
        return label + (value > 0 ? "+" : "") + value;
    }

    private String titleDebtPreview(Vup vup, DaySession session, TitleOptionDTO title) {
        return debtService.previewForTitle(vup, session, title).riskLine();
    }

    private String routeLabel(String routeType) {
        return switch (routeType) {
            case "SINGING_IDOL" -> "歌势";
            case "DANCE_MEME" -> "梗舞";
            case "SOCIAL_COLLAB" -> "联动";
            case "BLACK_RED_MAIN_STAGE" -> "主会场";
            case "ELECTRONIC_PICKLE" -> "稳健";
            default -> "当前";
        };
    }

    private record TitlePreview(
            String effectLine,
            String riskLine
    ) {
    }

    Long planIdFor(String planType) {
        for (int index = 0; index < STREAM_PLAN_OPTIONS.size(); index++) {
            if (STREAM_PLAN_OPTIONS.get(index).planType().equals(planType)) {
                return (long) index + 1;
            }
        }
        throw new GameException("CONFIG_FIELD_INVALID", "未知直播企划。");
    }

    private TitleRerollResultDTO doRerollTitles(Vup vup, DaySession session, RerollTitleRequest request) {
        if (!DayPhase.NEED_TITLE.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "当前不在标题选择阶段，不能换标题。");
        }
        if (request.planId() != null && !request.planId().equals(session.getSelectedPlanId())) {
            throw new GameException("CONFIG_FIELD_INVALID", "换批企划和当前直播企划不一致。");
        }
        if (session.getTitleRerollCount() >= TITLE_REROLL_LIMIT) {
            throw new GameException("TITLE_REROLL_LIMIT", "今日标题换批次数已满，标题组下班了。");
        }
        if (vup.getInspiration() < 1) {
            throw new GameException("INSUFFICIENT_INSPIRATION", "灵感不足，标题组没有新活了。");
        }

        int nextRerollCount = session.getTitleRerollCount() + 1;
        List<TitleOptionDTO> titleCandidates = enrichTitleCandidates(
                vup,
                session,
                rerolledTitleCandidatesFor(session.getSelectedPlanId(), nextRerollCount)
        );
        vup.setInspiration(vup.getInspiration() - 1);
        vupMapper.updateState(vup);

        session.setTitleRerollCount(nextRerollCount);
        session.setTitleCandidatesJson(jsonService.write(titleCandidates));
        daySessionMapper.updateAfterAction(session);

        return new TitleRerollResultDTO(
                session.getPhase(),
                session.getTitleRerollCount(),
                vup.getInspiration(),
                titleCandidates
        );
    }

    private DayResultDTO doChooseTitle(
            Long userId, Vup vup, DaySession session, ChooseTitleRequest request,
            String idempotencyKey, String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        if (!DayPhase.NEED_TITLE.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "当前不在标题选择阶段。");
        }
        List<TitleOptionDTO> titleCandidates = jsonService.readTitleOptions(session.getTitleCandidatesJson());
        TitleOptionDTO title = titleCandidates.stream()
                .filter(candidate -> candidate.id().equals(request.titleTemplateId()))
                .findFirst()
                .orElseThrow(() -> new GameException("TITLE_NOT_AVAILABLE", "这个标题不在今日候选里，标题组不能空降。"));
        if (vup.getStamina() < 3) {
            throw new GameException("INSUFFICIENT_STAMINA", "体力不够，唱不动了。");
        }

        ActionResultDTO actionResult = settleStreamTitle(vup, session, title);

        // 消费偷学buff：下次直播所有能力+10%（以固定加成体现）
        String stealBuffNpc = npcRelationshipService.consumeStealBuff(vup);
        if (stealBuffNpc != null) {
            vup.setTrueFans(vup.getTrueFans() + 10);
            vup.setFunFans(vup.getFunFans() + 5);
            vup.setPopularity(vup.getPopularity() + 15);
            vup.setWatchHeat(clamp(vup.getWatchHeat() + 5, 0, 100));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        }

        vupMapper.updateState(vup);

        BusinessLog log = actionService.toBusinessLog(vup, session, actionResult, idempotencyKey);
        log.setTitleTemplateId(title.id());
        attachTitleReplayDetails(log, session, titleCandidates, title);
        if (stealBuffNpc != null) {
            log.setResult(log.getResult() + " 偷学自" + stealBuffNpc + "的技巧生效：本次直播收益提升。");
        }
        businessLogMapper.insert(log);
        actionResult = actionResult.withDebtCreated(debtService.createForStreamTitleIfNeeded(vup, session, log, title));

        session.setSelectedTitleTemplateId(title.id());
        session.setPendingActionResultJson(jsonService.write(actionResult));
        DayResultDTO midgameEvent = actionService.pauseForMidgameEventIfNeeded(vup, session, actionResult);
        if (midgameEvent != null) {
            return midgameEvent;
        }
        DayResultDTO lateGameEvent = actionService.pauseForLateGameEventIfNeeded(vup, session, actionResult);
        if (lateGameEvent != null) {
            return lateGameEvent;
        }
        DayResultDTO pendingEvent = actionService.pauseForDueDebtIfNeeded(userId, vup, session, actionResult, idempotencyKey, requestHash, buildRecord);
        if (pendingEvent != null) {
            return pendingEvent;
        }
        DayResultDTO fanTopicEvent = actionService.pauseForFanTopicEscalationIfNeeded(userId, vup, session, actionResult, idempotencyKey, requestHash, buildRecord);
        if (fanTopicEvent != null) {
            return fanTopicEvent;
        }

        if (shouldPauseForInteraction(title)) {
            session.setPhase(DayPhase.NEED_INTERACTION_CHOICE.name());
            session.setPendingInteractionEventId(interactionEventIdFor(session));
            daySessionMapper.updateAfterAction(session);

            DayResultDTO result = new DayResultDTO(
                    session.getPhase(),
                    actionResult,
                    interactionEventFor(session),
                    null,
                    false,
                    null,
                    null
            );
            return result;
        }

        return actionService.queueOffStreamSettlement(session, actionResult);
    }

    private boolean shouldPauseForInteraction(TitleOptionDTO title) {
        return "HARD_MOUTH".equals(title.style()) || "FAN_SERVICE".equals(title.style());
    }

    private Long interactionEventIdFor(DaySession session) {
        if (session.getSelectedPlanId() != null && session.getSelectedPlanId() == 6L) {
            return 2L;
        }
        if (session.getSelectedPlanId() != null && session.getSelectedPlanId() == 12L) {
            return 3L;
        }
        if (session.getSelectedPlanId() != null && session.getSelectedPlanId() == 9L) {
            return 4L;
        }
        return 1L;
    }

    private InteractionEventDTO interactionEventFor(DaySession session) {
        PendingInteractionDTO pendingInteraction = pendingInteractionPresenter.pending(session);
        return new InteractionEventDTO(
                pendingInteraction.id(),
                pendingInteraction.eventKey(),
                pendingInteraction.description(),
                pendingInteraction.choices().stream()
                        .map(choice -> new InteractionChoiceDTO(choice.choiceType(), choice.label(), choice.riskPreview()))
                        .toList()
        );
    }

    private ActionResultDTO settleStreamTitle(Vup vup, DaySession session, TitleOptionDTO title) {
        boolean safeTitle = "SAFE".equals(title.style());
        boolean blackRedTitle = "HARD_MOUTH".equals(title.style()) || "ABSTRACT_MEME".equals(title.style());
        boolean danceMemePlan = isDanceMemePlan(session.getSelectedPlanId());
        boolean danceMemeTitle = danceMemePlan && ("BAIT_TRAFFIC".equals(title.style()) || "ABSTRACT_MEME".equals(title.style()));
        blackRedTitle = blackRedTitle && !danceMemeTitle;
        boolean fanServiceTitle = "FAN_SERVICE".equals(title.style());
        boolean businessSafeTitle = "BUSINESS_SAFE".equals(title.style());
        boolean commercialReviewWeek = isCommercialReviewWeek(session.getDay());
        boolean singingBoostWeek = isSingingBoostWeek(session.getDay());
        boolean singingSafeTitle = isSingingPlan(session) && safeTitle;
        boolean practiceToStreamCombo = practiceToStreamComboActive(vup, session);
        boolean videoToStreamCombo = videoToStreamComboActive(vup, session);
        ActionService.FortuneModifier fortuneModifier =
                actionService.fortuneModifierFor(vup, session, ActionType.STREAM_PLAN, !safeTitle || fanServiceTitle);
        ActionService.StageObjectiveSnapshot stageObjectiveSnapshot = actionService.stageObjectiveSnapshot(vup, session);
        int trueFanChange = fanServiceTitle ? 8 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 36 : 28) : 10;
        int funFanChange = danceMemeTitle ? 18 : blackRedTitle ? 20 : 0;
        int unicornFanChange = fanServiceTitle ? 30 : safeTitle || businessSafeTitle ? 4 : 0;
        int ddFanChange = fanServiceTitle ? 1 : 4;
        boolean newPlayerProtection = session.getDay() <= 3 && (blackRedTitle || danceMemeTitle);
        if (newPlayerProtection) {
            funFanChange = funFanChange * 6 / 10;
            trueFanChange = trueFanChange * 6 / 10;
            ddFanChange = ddFanChange * 6 / 10;
        }
        if (practiceToStreamCombo) {
            trueFanChange += 8;
        }
        if (videoToStreamCombo) {
            funFanChange += 8;
        }
        int fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        int coinChange = coinChangeForTitle(title, session.getDay());
        int popularityGain = fanServiceTitle ? 100 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 95 : 80) : 140;
        int reputationGain = fanServiceTitle ? 1 : safeTitle ? (singingSafeTitle && singingBoostWeek ? 4 : 3) : -3;
        int watchHeatGain = fanServiceTitle ? 4 : safeTitle ? 5 : 20;
        if (practiceToStreamCombo) {
            popularityGain += 15;
            reputationGain += 1;
            watchHeatGain += 1;
        }
        if (videoToStreamCombo) {
            popularityGain += 30;
            watchHeatGain += 3;
        }
        String targetRoute = titleTargetRoute(vup, session, danceMemeTitle, blackRedTitle, fanServiceTitle, businessSafeTitle);
        int routeScoreChange = danceMemeTitle || blackRedTitle ? 4 : fanServiceTitle || businessSafeTitle ? 2 : 3;
        int commercialGain = 0;
        FatigueInfoDTO fatigueInfo = actionFatigueService.calculateFatigue(vup.getId(), ActionType.STREAM_PLAN.name());
        int fatigueReductionPercent = 0;
        int fatigueRouteScoreReduction = 0;
        if (fatigueInfo.fatiguePercent() < 100) {
            int originalRouteScore = routeScoreChange;
            trueFanChange = actionFatigueService.applyFatigue(trueFanChange, fatigueInfo);
            funFanChange = actionFatigueService.applyFatigue(funFanChange, fatigueInfo);
            unicornFanChange = actionFatigueService.applyFatigue(unicornFanChange, fatigueInfo);
            ddFanChange = actionFatigueService.applyFatigue(ddFanChange, fatigueInfo);
            popularityGain = actionFatigueService.applyFatigue(popularityGain, fatigueInfo);
            routeScoreChange = actionFatigueService.applyFatigue(routeScoreChange, fatigueInfo);
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            fatigueReductionPercent = 100 - fatigueInfo.fatiguePercent();
            fatigueRouteScoreReduction = originalRouteScore - routeScoreChange;
        }

        vup.setStamina(vup.getStamina() - 3);
        vup.setCoin(vup.getCoin() + coinChange);
        vup.setTrueFans(vup.getTrueFans() + trueFanChange);
        vup.setFunFans(vup.getFunFans() + funFanChange);
        vup.setUnicornFans(vup.getUnicornFans() + unicornFanChange);
        vup.setDdFans(vup.getDdFans() + ddFanChange);
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        vup.setPopularity(vup.getPopularity() + popularityGain);
        vup.setReputation(clamp(vup.getReputation() + reputationGain, 0, 100));
        vup.setWatchHeat(clamp(vup.getWatchHeat() + watchHeatGain, 0, 100));
        if (videoToStreamCombo) {
            vup.setMemeLevel(clamp(vup.getMemeLevel() + 1, 0, 100));
        }
        if (danceMemeTitle) {
            vup.setDancePower(clamp(vup.getDancePower() + 1, 0, 100));
            vup.setMemeLevel(clamp(vup.getMemeLevel() + 5, 0, 100));
            dayFlowService.applyRouteScoreChange(vup, RouteType.DANCE_MEME.name(), routeScoreChange);
        } else if (blackRedTitle) {
            vup.setMemeLevel(clamp(vup.getMemeLevel() + 4, 0, 100));
            dayFlowService.applyRouteScoreChange(vup, RouteType.BLACK_RED_MAIN_STAGE.name(), routeScoreChange);
        } else if (fanServiceTitle || businessSafeTitle) {
            commercialGain = commercialReviewWeek
                    ? (fanServiceTitle ? 6 : 4)
                    : (fanServiceTitle ? 4 : 3);
            vup.setCommercialLevel(clamp(vup.getCommercialLevel() + commercialGain, 0, 100));
            dayFlowService.applyRouteScoreChange(vup, RouteType.ELECTRONIC_PICKLE.name(), routeScoreChange);
        } else {
            dayFlowService.applyRouteScoreChange(vup, targetRoute, routeScoreChange);
        }

        AudiencePressureService.AudiencePressure audiencePressure =
                audiencePressureService.pressureForRoute(vup, session.getDay(), targetRoute);
        String summary = streamTitleSummary(title, danceMemeTitle, commercialReviewWeek, singingSafeTitle && singingBoostWeek, practiceToStreamCombo, videoToStreamCombo);
        Map<String, Object> evidenceRef = new LinkedHashMap<>();
        evidenceRef.put("type", "business_log");
        evidenceRef.put("source", "pending");
        evidenceRef.put("targetRoute", targetRoute);
        evidenceRef.put("titleStyle", title.style());
        if (fatigueInfo.fatiguePercent() < 100) {
            evidenceRef.put("fatigueConsecutive", fatigueInfo.consecutiveDays());
            evidenceRef.put("fatigueReductionPercent", fatigueReductionPercent);
            evidenceRef.put("fatigueRouteScoreReduction", fatigueRouteScoreReduction);
            if (fatigueInfo.fatigueHint() != null && !fatigueInfo.fatigueHint().isBlank()) {
                summary = summary + " " + fatigueInfo.fatigueHint();
            }
        }
        if (audiencePressure.active()) {
            int keepPercent = audiencePressureService.fanKeepPercent(audiencePressure);
            int adjustedTrueFanChange = trueFanChange * keepPercent / 100;
            int adjustedFunFanChange = funFanChange * keepPercent / 100;
            int adjustedUnicornFanChange = unicornFanChange * keepPercent / 100;
            int adjustedDdFanChange = ddFanChange * keepPercent / 100;

            vup.setTrueFans(vup.getTrueFans() + adjustedTrueFanChange - trueFanChange);
            vup.setFunFans(vup.getFunFans() + adjustedFunFanChange - funFanChange);
            vup.setUnicornFans(vup.getUnicornFans() + adjustedUnicornFanChange - unicornFanChange);
            vup.setDdFans(vup.getDdFans() + adjustedDdFanChange - ddFanChange);
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setReputation(clamp(vup.getReputation() + audiencePressureService.reputationPenalty(audiencePressure), 0, 100));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + audiencePressureService.watchHeatGain(audiencePressure), 0, 100));

            trueFanChange = adjustedTrueFanChange;
            funFanChange = adjustedFunFanChange;
            unicornFanChange = adjustedUnicornFanChange;
            ddFanChange = adjustedDdFanChange;
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + audiencePressureService.summarySuffix(audiencePressure);
            evidenceRef.put("audiencePressure", audiencePressureService.evidence(audiencePressure));
        }

        StageObjectiveService.StageMomentum stageMomentum = stageObjectiveService.stageMomentumFor(vup);
        ActionService.StageMomentumDelta stageMomentumDelta =
                actionService.stageMomentumDeltaFor(stageMomentum, "STREAM_PLAN", targetRoute);
        if (stageMomentumDelta.applied()) {
            trueFanChange += stageMomentumDelta.trueFanChange();
            popularityGain += stageMomentumDelta.popularityChange();
            watchHeatGain += stageMomentumDelta.watchHeatChange();
            reputationGain += stageMomentumDelta.reputationChange();
            routeScoreChange += stageMomentumDelta.routeScoreChange();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + stageMomentumDelta.trueFanChange()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + stageMomentumDelta.popularityChange()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + stageMomentumDelta.watchHeatChange(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + stageMomentumDelta.reputationChange(), 0, 100));
            if (stageMomentumDelta.routeScoreChange() != 0) {
                dayFlowService.applyRouteScoreChange(vup, targetRoute, stageMomentumDelta.routeScoreChange());
            }
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + " " + stageMomentum.hint();
            evidenceRef.put("stageMomentum", actionService.stageMomentumEvidence(stageMomentum, stageMomentumDelta));
        }

        ActionService.ComboImpact comboImpact = actionService.comboImpactFor(vup, session, ActionType.STREAM_PLAN, targetRoute);
        if (comboImpact.applied()) {
            trueFanChange += comboImpact.trueFanBonus();
            funFanChange += comboImpact.funFanBonus();
            ddFanChange += comboImpact.ddFanBonus();
            popularityGain += comboImpact.popularityBonus();
            watchHeatGain += comboImpact.watchHeatBonus();
            reputationGain += comboImpact.reputationBonus();
            routeScoreChange += comboImpact.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + comboImpact.trueFanBonus()));
            vup.setFunFans(Math.max(0, vup.getFunFans() + comboImpact.funFanBonus()));
            vup.setDdFans(Math.max(0, vup.getDdFans() + comboImpact.ddFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + comboImpact.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + comboImpact.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + comboImpact.reputationBonus(), 0, 100));
            vup.setMemeLevel(clamp(vup.getMemeLevel() + comboImpact.memeBonus(), 0, 100));
            if (comboImpact.routeScoreBonus() != 0 && comboImpact.targetRoute() != null && !comboImpact.targetRoute().isBlank()) {
                dayFlowService.applyRouteScoreChange(vup, comboImpact.targetRoute(), comboImpact.routeScoreBonus());
            }
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + " " + comboImpact.hint();
            evidenceRef.put("comboImpact", actionService.comboImpactEvidence(comboImpact));
        }

        ActionService.StageObjectiveBonus stageObjectiveBonus =
                actionService.stageObjectiveBonusFor(vup, session, stageObjectiveSnapshot, ActionType.STREAM_PLAN, targetRoute);
        if (stageObjectiveBonus.applied()) {
            trueFanChange += stageObjectiveBonus.trueFanBonus();
            popularityGain += stageObjectiveBonus.popularityBonus();
            reputationGain += stageObjectiveBonus.reputationBonus();
            routeScoreChange += stageObjectiveBonus.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + stageObjectiveBonus.trueFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + stageObjectiveBonus.popularityBonus()));
            vup.setReputation(clamp(vup.getReputation() + stageObjectiveBonus.reputationBonus(), 0, 100));
            if (stageObjectiveBonus.routeScoreBonus() != 0
                    && stageObjectiveBonus.targetRoute() != null
                    && !stageObjectiveBonus.targetRoute().isBlank()) {
                dayFlowService.applyRouteScoreChange(vup, stageObjectiveBonus.targetRoute(), stageObjectiveBonus.routeScoreBonus());
            }
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + " " + stageObjectiveBonus.hint();
            evidenceRef.put("stageObjectiveBonus", actionService.stageObjectiveEvidence(stageObjectiveBonus));
        }

        ActionService.PlatformTrendModifier platformTrendModifier =
                actionService.platformTrendModifierFor(session.getDay(), ActionType.STREAM_PLAN, targetRoute, title.style(), session.getSelectedPlanId());
        if (platformTrendModifier.applied()) {
            trueFanChange += platformTrendModifier.trueFanBonus();
            funFanChange += platformTrendModifier.funFanBonus();
            ddFanChange += platformTrendModifier.ddFanBonus();
            popularityGain += platformTrendModifier.popularityBonus();
            watchHeatGain += platformTrendModifier.watchHeatBonus();
            reputationGain += platformTrendModifier.reputationBonus();
            coinChange += platformTrendModifier.coinBonus();
            commercialGain += platformTrendModifier.commercialBonus();
            routeScoreChange += platformTrendModifier.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + platformTrendModifier.trueFanBonus()));
            vup.setFunFans(Math.max(0, vup.getFunFans() + platformTrendModifier.funFanBonus()));
            vup.setDdFans(Math.max(0, vup.getDdFans() + platformTrendModifier.ddFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + platformTrendModifier.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + platformTrendModifier.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + platformTrendModifier.reputationBonus(), 0, 100));
            vup.setMemeLevel(clamp(vup.getMemeLevel() + platformTrendModifier.memeBonus(), 0, 100));
            vup.setCoin(Math.max(0, vup.getCoin() + platformTrendModifier.coinBonus()));
            vup.setCommercialLevel(clamp(vup.getCommercialLevel() + platformTrendModifier.commercialBonus(), 0, 100));
            if (platformTrendModifier.routeScoreBonus() != 0) {
                dayFlowService.applyRouteScoreChange(vup, targetRoute, platformTrendModifier.routeScoreBonus());
            }
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + " " + platformTrendModifier.hint();
            evidenceRef.put("platformTrendModifier", actionService.platformTrendEvidence(platformTrendModifier));
        }

        if (fortuneModifier.applied()) {
            trueFanChange += fortuneModifier.trueFanBonus();
            popularityGain += fortuneModifier.popularityBonus();
            watchHeatGain += fortuneModifier.watchHeatBonus();
            reputationGain += fortuneModifier.reputationBonus();
            routeScoreChange += fortuneModifier.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + fortuneModifier.trueFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + fortuneModifier.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + fortuneModifier.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + fortuneModifier.reputationBonus(), 0, 100));
            if (fortuneModifier.routeScoreBonus() != 0) {
                dayFlowService.applyRouteScoreChange(vup, targetRoute, fortuneModifier.routeScoreBonus());
            }
            fanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
            summary = summary + " " + fortuneModifier.hint();
            evidenceRef.put("fortuneModifier", actionService.fortuneEvidence(fortuneModifier));
        }

        // 礼物热度加成：折算为 watchHeat + coin
        int giftHeatBonus = session.getGiftCount() > 0
                ? Math.min(balanceConfig.giftHeatBonusCap(), session.getGiftCoinValue() / balanceConfig.giftHeatBonusPerCoin())
                : 0;
        if (giftHeatBonus > 0) {
            watchHeatGain += giftHeatBonus;
            coinChange += session.getGiftCoinValue();
            vup.setWatchHeat(clamp(vup.getWatchHeat() + giftHeatBonus, 0, 100));
            vup.setCoin(Math.max(0, vup.getCoin() + session.getGiftCoinValue()));
            evidenceRef.put("giftBonus", giftHeatBonus);
        }
        // 弹幕热度加成：密度提升 watchHeat；负面弹幕累计过多扣口碑
        int danmakuHeatBonus = Math.min(balanceConfig.danmakuHeatBonusCap(), session.getDanmakuHeat());
        if (danmakuHeatBonus > 0) {
            watchHeatGain += danmakuHeatBonus;
            vup.setWatchHeat(clamp(vup.getWatchHeat() + danmakuHeatBonus, 0, 100));
            evidenceRef.put("danmakuHeatBonus", danmakuHeatBonus);
        }
        if (session.getDanmakuCount() > 0 && session.getDanmakuHeat() < 0) {
            int reputationPenalty = balanceConfig.danmakuNegativeReputationPenalty();
            reputationGain -= reputationPenalty;
            vup.setReputation(clamp(vup.getReputation() - reputationPenalty, 0, 100));
            evidenceRef.put("danmakuNegativePenalty", reputationPenalty);
        }

        return new ActionResultDTO(
                "STREAM_PLAN",
                title.titleText(),
                summary,
                fanChange,
                trueFanChange,
                funFanChange,
                unicornFanChange,
                ddFanChange,
                -3,
                danceMemeTitle ? 1 : 0,
                routeScoreChange,
                evidenceRef
        );
    }

    private String titleTargetRoute(
            Vup vup,
            DaySession session,
            boolean danceMemeTitle,
            boolean blackRedTitle,
            boolean fanServiceTitle,
            boolean businessSafeTitle
    ) {
        if (danceMemeTitle) {
            return RouteType.DANCE_MEME.name();
        }
        if (blackRedTitle) {
            return RouteType.BLACK_RED_MAIN_STAGE.name();
        }
        if (fanServiceTitle || businessSafeTitle) {
            return RouteType.ELECTRONIC_PICKLE.name();
        }
        if (session.getSelectedPlanId() != null && session.getSelectedPlanId() == 9L) {
            return RouteType.SOCIAL_COLLAB.name();
        }
        if (isSocialGamePlan(vup, session)) {
            return RouteType.SOCIAL_COLLAB.name();
        }
        if (session.getSelectedPlanId() != null && session.getSelectedPlanId() == 2L) {
            return RouteType.SINGING_IDOL.name();
        }
        return RouteType.ELECTRONIC_PICKLE.name();
    }

    private boolean isSocialGamePlan(Vup vup, DaySession session) {
        if (session.getSelectedPlanId() == null || session.getSelectedPlanId() != 4L) {
            return false;
        }
        if (RouteType.SOCIAL_COLLAB.name().equals(vup.getCurrentRoute())) {
            return true;
        }
        int currentDay = session.getDay();
        return businessLogMapper.findRecentByVupId(vup.getId(), 10).stream()
                .filter(log -> log.getDay() < currentDay)
                .filter(log -> currentDay - log.getDay() <= 7)
                .anyMatch(log -> isCollabInteraction(log)
                        || containsRouteMarker(log.getRouteScoreChange(), RouteType.SOCIAL_COLLAB.name())
                        || containsRouteMarker(log.getWeightDetail(), RouteType.SOCIAL_COLLAB.name()));
    }

    private boolean isCollabInteraction(BusinessLog log) {
        return ActionType.NPC_INTERACT.name().equals(log.getAction())
                && containsRouteMarker(log.getWeightDetail(), "COLLAB");
    }

    private boolean containsRouteMarker(String text, String marker) {
        return text != null && text.contains(marker);
    }

    private String streamTitleSummary(
            TitleOptionDTO title,
            boolean danceMemeTitle,
            boolean commercialReviewWeek,
            boolean singingBoostWeek,
            boolean practiceToStreamCombo,
            boolean videoToStreamCombo
    ) {
        String comboSuffix = "";
        if (practiceToStreamCombo) {
            comboSuffix += " 练习后直播触发：昨天练过的基本功今天被观众听出来了。";
        }
        if (videoToStreamCombo) {
            comboSuffix += " 视频引流直播触发：昨天的视频把路人送到直播间，乐子人顺手也进来了。";
        }
        if (danceMemeTitle) {
            if ("ABSTRACT_MEME".equals(title.style())) {
                return "你用《" + title.titleText() + "》开了梗舞挑战回，梗舞路线让乐子人开始逐帧鉴赏，舞蹈区和切片组同时上班。" + comboSuffix;
            }
            if ("BAIT_TRAFFIC".equals(title.style())) {
                return "你用《" + title.titleText() + "》蹭了一把热门舞蹈，梗舞路线开始成型，短视频区先替你热身。" + comboSuffix;
            }
        }
        if (title.titleText().contains("舞")) {
            return "你用《" + title.titleText() + "》开了低压练舞回，老粉看见努力，乐子人暂时没有开庭素材。" + comboSuffix;
        }
        if ("FAN_SERVICE".equals(title.style())) {
            if (commercialReviewWeek) {
                return "你用《" + title.titleText() + "》开了醒目留言陪伴回，陪伴营业撞上商业复审周，榜一排班表和活动预算同时刷新，商业味更浓但运营账面很好看。" + comboSuffix;
            }
            return "你用《" + title.titleText() + "》开了醒目留言陪伴回，陪伴营业让独角兽和榜一同时开始排班，商业味上桌但气压还算可控。" + comboSuffix;
        }
        if ("BUSINESS_SAFE".equals(title.style())) {
            if (commercialReviewWeek) {
                return "你用《" + title.titleText() + "》稳稳回应醒目留言，稳健处理模拟礼物撞上商业复审周，把稳定排班推给品牌方看，运营预算和商业化都更好看。" + comboSuffix;
            }
            return "你用《" + title.titleText() + "》稳稳回应醒目留言，稳健处理模拟礼物让品牌方满意，老粉觉得有点商业但还能下饭。" + comboSuffix;
        }
        if ("HARD_MOUTH".equals(title.style())) {
            return "你用《" + title.titleText() + "》开了高风险歌回，硬嘴标题把围观热度拉满，标题组和楼友同时开始记笔记。" + comboSuffix;
        }
        if ("ABSTRACT_MEME".equals(title.style())) {
            return "你用《" + title.titleText() + "》开了抽象企划，抽象企划让乐子人进场，主会场味儿开始冒头。" + comboSuffix;
        }
        if (singingBoostWeek) {
            return "你用《" + title.titleText() + "》开了低压歌回，歌回扶持周把稳唱推上推荐边缘，老粉听得很稳，DD顺手坐了一站。" + comboSuffix;
        }
        return "你用《" + title.titleText() + "》开了歌回，老粉听得很稳，DD顺手坐了一站。" + comboSuffix;
    }

    private void attachTitleReplayDetails(
            BusinessLog log,
            DaySession session,
            List<TitleOptionDTO> titleCandidates,
            TitleOptionDTO hitTitle
    ) {
        log.setWeightDetail(jsonService.write(Map.of(
                "rollType", "title_choice",
                "action", log.getAction(),
                "selectedPlanId", session.getSelectedPlanId(),
                "titleCandidates", titleReplayCandidates(titleCandidates),
                "weights", titleChoiceWeights(titleCandidates, hitTitle),
                "hitTitleId", hitTitle.id(),
                "hitTitleText", hitTitle.titleText(),
                "hitTitleStyle", hitTitle.style()
        )));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hitTitleId", hitTitle.id(),
                "hitTitleText", hitTitle.titleText()
        )));
        Map<String, Object> routeScoreChange = new LinkedHashMap<>(jsonService.readMap(log.getRouteScoreChange()));
        routeScoreChange.put("source", "title:" + hitTitle.style());
        routeScoreChange.put("selectedPlanId", session.getSelectedPlanId());
        routeScoreChange.put("hitTitleId", hitTitle.id());
        routeScoreChange.put("hitTitleStyle", hitTitle.style());
        log.setRouteScoreChange(jsonService.write(routeScoreChange));
    }

    private List<Map<String, Object>> titleReplayCandidates(List<TitleOptionDTO> titleCandidates) {
        return titleCandidates.stream()
                .map(title -> {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("id", title.id());
                    candidate.put("titleText", title.titleText());
                    candidate.put("style", title.style());
                    candidate.put("effectPreview", title.effectPreview());
                    candidate.put("debtRiskPreview", title.debtRiskPreview());
                    return candidate;
                })
                .toList();
    }

    private Map<Long, Integer> titleChoiceWeights(List<TitleOptionDTO> titleCandidates, TitleOptionDTO hitTitle) {
        Map<Long, Integer> weights = new LinkedHashMap<>();
        for (TitleOptionDTO candidate : titleCandidates) {
            weights.put(candidate.id(), candidate.id().equals(hitTitle.id()) ? 1 : 0);
        }
        return weights;
    }

    private List<TitleOptionDTO> rerolledTitleCandidatesFor(Long planId, int rerollCount) {
        long baseId = planId * 100 + rerollCount * 10L;
        if (planId == 2L) {
            return List.of(
                    new TitleOptionDTO(baseId + 1, "换个低压歌单，先把气口找回来", "SAFE", "口碑和真爱粉更稳", "风险低", "safe"),
                    new TitleOptionDTO(baseId + 2, "今晚唱到录播组能交差为止", "BAIT_TRAFFIC", "围观热度和歌势期待上升", "录播组考据风险", "warning"),
                    new TitleOptionDTO(baseId + 3, "高音上不去就让弹幕替我上", "ABSTRACT_MEME", "节目效果和乐子人上升", "梗疲劳风险", "funny")
            );
        }
        if (isDanceMemePlan(planId)) {
            return List.of(
                    new TitleOptionDTO(baseId + 1, "换个低压舞单，动作先别上强度", "SAFE", "舞力和口碑更稳", "风险低", "safe"),
                    new TitleOptionDTO(baseId + 2, "热门舞挑战，录播组别逐帧", "BAIT_TRAFFIC", "短视频人气和梗舞上升", "动作对比风险", "warning"),
                    new TitleOptionDTO(baseId + 3, "这段舞主打一个灵魂跟拍", "ABSTRACT_MEME", "梗舞和乐子上升", "鬼畜素材风险", "funny")
            );
        }
        return List.of(
                new TitleOptionDTO(baseId + 1, "换个低压版本，今天先别开庭", "SAFE", "口碑更稳", "风险低", "safe"),
                new TitleOptionDTO(baseId + 2, "标题组说这版比较能打", "BAIT_TRAFFIC", "人气小涨", "标题党风险", "warning"),
                new TitleOptionDTO(baseId + 3, "抽象一点，但还没到需要房管救场", "ABSTRACT_MEME", "乐子和围观上升", "梗疲劳风险", "funny")
        );
    }

    private boolean isDanceMemePlan(Long planId) {
        return planId != null && (planId == 3L || planId == 11L);
    }

    private boolean isSingingPlan(DaySession session) {
        return session.getSelectedPlanId() != null && session.getSelectedPlanId() == 2L;
    }

    private int coinChangeForTitle(TitleOptionDTO title, int day) {
        if ("FAN_SERVICE".equals(title.style())) {
            return isCommercialReviewWeek(day) ? 800 : 600;
        }
        if ("BUSINESS_SAFE".equals(title.style())) {
            return isCommercialReviewWeek(day) ? 700 : 500;
        }
        return 0;
    }

    private boolean practiceToStreamComboActive(Vup vup, DaySession session) {
        if (session.getDay() <= 1) {
            return false;
        }
        BusinessLog previous = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), session.getDay() - 1);
        return previous != null && isTrainingAction(previous.getAction());
    }

    private boolean videoToStreamComboActive(Vup vup, DaySession session) {
        if (session.getDay() <= 1) {
            return false;
        }
        return businessLogMapper.findRecentByVupId(vup.getId(), 10).stream()
                .anyMatch(log -> "PUBLISH_VIDEO".equals(log.getAction())
                        && log.getDay() < session.getDay()
                        && session.getDay() - log.getDay() <= 3);
    }

    private boolean isTrainingAction(String actionType) {
        return "TRAIN_SONG".equals(actionType)
                || "TRAIN_DANCE".equals(actionType)
                || "TRAIN_TALK".equals(actionType);
    }

    private boolean isCommercialReviewWeek(int day) {
        return day >= 22 && day <= 30;
    }

    private boolean isSingingBoostWeek(int day) {
        return day >= 8 && day <= 14;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
