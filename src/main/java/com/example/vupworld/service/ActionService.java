package com.example.vupworld.service;

import com.example.vupworld.service.report.ReportService;

import com.example.vupworld.service.risk.ActionFatigueService;

import com.example.vupworld.service.risk.DebtService;
import com.example.vupworld.service.operating.OperatingPressureService;

import com.example.vupworld.service.core.VupStateMapper;

import com.example.vupworld.service.core.DayFlowService;

import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.progression.StageObjectiveService;

import com.example.vupworld.service.content.DailyFortuneService;

import com.example.vupworld.service.content.PlatformTrendService;

import com.example.vupworld.service.fan.PersonaTagService;

import com.example.vupworld.service.fan.AudiencePressureService;

import com.example.vupworld.service.ending.EndingForecastService;

import com.example.vupworld.service.ending.EndingService;

import com.example.vupworld.service.infra.RngLedger;

import com.example.vupworld.service.infra.DeterministicRngService;

import com.example.vupworld.service.infra.BalanceConfig;

import com.example.vupworld.service.infra.IdempotencyRunner;

import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.service.event.EventService;
import com.example.vupworld.service.event.FormalEventPresenter;
import com.example.vupworld.service.event.CommercialRouteContentService;
import com.example.vupworld.service.event.MidgameEventContentService;
import com.example.vupworld.service.event.RandomEventService;
import com.example.vupworld.service.event.InteractionService;
import com.example.vupworld.service.event.PendingInteractionPresenter;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.ActionType;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.domain.RouteType;
import com.example.vupworld.dto.ActionDtos.ActionOptionDTO;
import com.example.vupworld.dto.ActionDtos.ActionResultDTO;
import com.example.vupworld.dto.ActionDtos.CancelActionRequest;
import com.example.vupworld.dto.ActionDtos.DebtCreatedDTO;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.ActionDtos.OffStreamOptionDTO;
import com.example.vupworld.dto.ActionDtos.ScheduleSlotRequest;
import com.example.vupworld.dto.ActionDtos.SubmitActionRequest;
import com.example.vupworld.dto.ActionDtos.SubmitScheduleRequest;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;

import com.example.vupworld.dto.DailyFortuneDtos.DailyFortuneDTO;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.InteractionDtos.InteractionChoiceDTO;
import com.example.vupworld.dto.InteractionDtos.InteractionEventDTO;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;
import com.example.vupworld.dto.RandomEventDtos.RandomEventDTO;
import com.example.vupworld.mapper.BusinessLogMapper;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.RiskDebt;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Service
public class ActionService {
    private static final String DAY_ACTION_PATH = "/api/day/action";
    private static final String DAY_SCHEDULE_PATH = "/api/day/schedule";
    private static final String CANCEL_ACTION_PATH = "/api/day/action/cancel";
    private static final List<String> MAIN_ACTION_CANDIDATE_POOL = List.of(
            ActionType.TRAIN_SONG.name(),
            ActionType.TRAIN_DANCE.name(),
            ActionType.TRAIN_TALK.name(),
            ActionType.STREAM_PLAN.name(),
            ActionType.PUBLISH_VIDEO.name(),
            ActionType.PUBLISH_CLIP.name(),
            ActionType.FAN_GROUP_MAINTAIN.name(),
            ActionType.NPC_INTERACT.name(),
            ActionType.REST.name()
    );
    private static final String NPC_TENDENCY_RAID = "RAID";
    private static final String NPC_TENDENCY_COLLAB = "COLLAB";
    private static final String NPC_TENDENCY_BORROW_HEAT = "BORROW_HEAT";
    private static final String NPC_TENDENCY_AVOID = "AVOID";
    private static final int NPC_COLLAB_COOLDOWN_DAYS = 3;
    private static final List<String> SCHEDULE_SLOT_ORDER = List.of("MORNING", "NOON", "AFTERNOON", "NIGHT");

    private record OffStreamConfig(
            String name,
            String effectPreview,
            String benefitPreview,
            String costPreview,
            String bestFor,
            String riskPreview,
            String recommendedReason,
            String routeBias,
            OffStreamImpact impact
    ) {
    }

    private record OffStreamImpact(
            int songPowerDelta,
            int dancePowerDelta,
            int talkPowerDelta,
            int popularityDelta,
            int watchHeatDelta,
            int reputationDelta,
            int memeDelta,
            int commercialDelta,
            int coinDelta,
            int inspirationDelta,
            int trueFanDelta,
            int funFanDelta,
            int unicornFanDelta,
            int ddFanDelta,
            int routeScoreDelta
    ) {
    }

    private record OffStreamResolution(
            String type,
            String name,
            String summary,
            String benefitPreview,
            String costPreview,
            String bestFor,
            String riskPreview,
            String recommendedReason,
            String routeBias,
            boolean recommended,
            OffStreamImpact impact
    ) {
    }

    @SuppressWarnings("unused")
    private static final Map<String, OffStreamConfig> OFF_STREAM_CATALOG = Map.ofEntries(
            Map.entry("VOCAL_PRACTICE", new OffStreamConfig(
                    "练声", "收益：歌力+1，歌势路线证据+1；代价：热度不涨",
                    "稳稳补一格歌力，并给歌势路线多留一条训练证据。",
                    "没有即时涨粉和热度，适合把今天的余波收窄。",
                    "明天想继续歌回、但主行动刚刚偏向保守时。",
                    "风险低，但连续只练不营业会显得存在感偏弱。",
                    "当前路线吃歌力和连续训练证据，这手能把明天歌回成功率垫高。",
                    "SINGING_IDOL",
                    new OffStreamImpact(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1))),
            Map.entry("SONG_SELECTION", new OffStreamConfig(
                    "选曲", "收益：灵感+1，口碑+1；代价：制作预算-20",
                    "补一格灵感，顺手把歌单做得更像正经排班。",
                    "要花一点版权/伴奏预算，热度不会立刻体现。",
                    "灵感紧张、明天可能要投稿或开歌回时。",
                    "预算很低时会挤压后续制作余地。",
                    "歌势线需要稳定供给，选曲能把明天的选择池撑开。",
                    "SINGING_IDOL",
                    new OffStreamImpact(0, 0, 0, 0, 0, 1, 0, 0, -20, 1, 0, 0, 0, 0, 1))),
            Map.entry("CLIP_SCOUTING", new OffStreamConfig(
                    "素材挖掘", "收益：切片素材+1，梗浓度+1；代价：口碑-1",
                    "给切片线补素材感，日报会记录这次翻录播的来源。",
                    "翻旧素材容易显得标题组又在考古，老粉耐心略降。",
                    "明天想发切片、但手上素材不够时。",
                    "低口碑时继续考古可能让粉丝觉得只会消费事故。",
                    "切片线最缺的是可复盘素材，这手能把明天的切片理由补齐。",
                    "SLICE_SAINT",
                    new OffStreamImpact(0, 0, 0, 0, 1, -1, 1, 0, 0, 0, 0, 0, 0, 0, 1))),
            Map.entry("THUMBNAIL_DESIGN", new OffStreamConfig(
                    "封面设计", "收益：人气+2，商业化+1；代价：灵感-1",
                    "把今天内容包装得更好看，给传播和商业观感各补一点。",
                    "会消耗一格灵感，明天想投稿时要确认库存。",
                    "主行动有内容产出、但曝光还差一口气时。",
                    "灵感见底时容易让明天选项变窄。",
                    "切片线需要可点击的外壳，这手让日报里的传播收益更明确。",
                    "SLICE_SAINT",
                    new OffStreamImpact(0, 0, 0, 2, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 1))),
            Map.entry("READ_LETTERS", new OffStreamConfig(
                    "读粉丝信", "收益：真爱粉+1，口碑+2；代价：围观热度-1",
                    "把低压陪伴落成证据，稳住真爱粉和口碑。",
                    "不追热点，今晚的场外围观会淡一点。",
                    "口碑受压、或电子榨菜线想往体面陪伴靠时。",
                    "热度冲榜期选择它会牺牲一点扩散速度。",
                    "电子榨菜线吃长期信任，这手能把陪伴感写进日报。",
                    "ELECTRONIC_PICKLE",
                    new OffStreamImpact(0, 0, 0, 0, -1, 2, 0, 0, 0, 0, 1, 0, 0, 0, 1))),
            Map.entry("CHAT_ROOM", new OffStreamConfig(
                    "陪聊", "收益：灵感+1，独角兽粉+1；代价：口碑-1",
                    "维持陪伴感并补一点灵感，关系感会更强。",
                    "边界感会被多消耗一点，口碑略有压力。",
                    "想保住陪伴盘、但明天还需要灵感时。",
                    "独角兽压力高时容易把期待再抬起来。",
                    "当前路线需要陪伴黏性，这手收益小但方向很准。",
                    "ELECTRONIC_PICKLE",
                    new OffStreamImpact(0, 0, 0, 0, 1, -1, 0, 0, 0, 1, 0, 0, 1, 0, 1))),
            Map.entry("TREND_WATCH", new OffStreamConfig(
                    "舆情监控", "收益：围观热度-2，口碑+1；代价：人气不涨",
                    "提前拆掉一点围观压力，让明天的风险回流更温和。",
                    "这是防守回合，不会直接涨粉或冲榜。",
                    "热度高、旧账快到期、或黑红线需要控场时。",
                    "连续防守会拖慢路线爆点。",
                    "黑红线最怕失控，这手能把主会场压力压低一点。",
                    "BLACK_RED_MAIN_STAGE",
                    new OffStreamImpact(0, 0, 0, 0, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1))),
            Map.entry("CRISIS_PR", new OffStreamConfig(
                    "危机公关", "收益：围观热度+2，商业化+1；代价：口碑-1",
                    "把争议整理成可控说法，短期场外关注会更集中。",
                    "会承认争议存在，口碑略受压。",
                    "黑红线需要把热度留在自己手里时。",
                    "旧账严重时继续公关可能引来二次围观。",
                    "当前路线需要主会场感，这手能把余波导向可用热度。",
                    "BLACK_RED_MAIN_STAGE",
                    new OffStreamImpact(0, 0, 0, 1, 2, -1, 0, 1, 0, 0, 0, 0, 0, 0, 1))),
            Map.entry("DM_MAINTAIN", new OffStreamConfig(
                    "私信维护", "收益：DD粉+1，口碑+1；代价：体面热度不涨",
                    "把场外关系维护成可见证据，DD和口碑各稳一点。",
                    "私下沟通不制造大声量，今晚扩散偏弱。",
                    "社交联动线缺少关系证据时。",
                    "关系盘太散时收益会被稀释。",
                    "社交线看重可持续关系，这手能让联动动机更可信。",
                    "SOCIAL_COLLAB",
                    new OffStreamImpact(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1))),
            Map.entry("COLLAB_PLAN", new OffStreamConfig(
                    "联动排班", "收益：DD粉+2，热度+1；代价：活动预算-30",
                    "把联动从口嗨推进到排班，DD盘和热度会小幅抬头。",
                    "要占一点场务和排期成本。",
                    "明天准备 NPC 互动或轻联动时。",
                    "预算紧张时会压缩制作空间。",
                    "当前路线需要同台证据，这手能把明天的查房/联动接上。",
                    "SOCIAL_COLLAB",
                    new OffStreamImpact(0, 0, 0, 0, 1, 0, 0, 0, -30, 0, 0, 0, 0, 2, 1))),
            Map.entry("MEME_RESEARCH", new OffStreamConfig(
                    "刷梗", "收益：梗浓度+2，乐子粉+1；代价：口碑-1",
                    "补梗感和乐子粉，让明天抽象内容更顺手。",
                    "会牺牲一点正经观感。",
                    "梗舞/抽象路线需要补弹药时。",
                    "口碑偏低时继续刷梗会加重轻浮感。",
                    "梗舞线需要新梗库存，这手让路线表达更鲜明。",
                    "DANCE_MEME",
                    new OffStreamImpact(0, 0, 0, 0, 1, -1, 2, 0, 0, 0, 0, 1, 0, 0, 1))),
            Map.entry("SHORT_VIDEO_IDEA", new OffStreamConfig(
                    "短视频构思", "收益：乐子粉+2，灵感+1；代价：制作预算-20",
                    "把梗转成短视频企划，乐子粉和灵感都有小补。",
                    "要花一点素材整理预算。",
                    "明天想投稿、跳舞或冲短视频扩散时。",
                    "预算低时别把最后一口制作钱用掉。",
                    "梗舞线要把梗变成作品，这手能补明天行动理由。",
                    "DANCE_MEME",
                    new OffStreamImpact(0, 0, 0, 0, 0, 0, 1, 0, -20, 1, 0, 2, 0, 0, 1))),
            Map.entry("BROWSE_SOCIAL", new OffStreamConfig(
                    "刷手机", "收益：灵感+1，热度+1；代价：口碑-1",
                    "摸到一点平台风向，明天有新话题可接。",
                    "容易被场外节奏带走，口碑小降。",
                    "路线还没定、需要找方向时。",
                    "旧账多或热度高时容易越刷越乱。",
                    "没有明确路线时，这手给明天留一个轻量入口。",
                    null,
                    new OffStreamImpact(0, 0, 0, 0, 1, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0))),
            Map.entry("ORGANIZE_MATERIALS", new OffStreamConfig(
                    "整理素材", "收益：口碑+1，商业化+1；代价：热度不涨",
                    "把后台资料和素材归档，日报会显示运营更稳。",
                    "没有直接涨粉，短期存在感偏低。",
                    "路线未定、或明天要做商业/投稿前的稳手。",
                    "连续整理会拖慢爆点积累。",
                    "通用保守手，适合不想扩大风险但又不想空过。",
                    null,
                    new OffStreamImpact(0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0)))
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
    private final EndingService endingService;
    private final DebtService debtService;
    private final FormalEventPresenter formalEventPresenter;
    private final PendingInteractionPresenter pendingInteractionPresenter;
    private final BalanceConfig balanceConfig;
    private final DayFlowService dayFlowService;
    private final TitleService titleService;
    private final ActionFatigueService actionFatigueService;
    private final AudiencePressureService audiencePressureService;
    private final OperatingPressureService operatingPressureService;
    private final StageObjectiveService stageObjectiveService;
    private final RandomEventService randomEventService;
    private final CommercialRouteContentService commercialRouteContentService;
    private final MidgameEventContentService midgameEventContentService;
    private final DeterministicRngService deterministicRngService;
    private final PlatformTrendService platformTrendService;
    private final DailyFortuneService dailyFortuneService;
    private final com.example.vupworld.service.fan.RivalProgressService rivalProgressService;
    private final com.example.vupworld.service.event.RivalOvertakeEvent rivalOvertakeEvent;

    public ActionService(
            VupService vupService,
            VupMapper vupMapper,
            DaySessionMapper daySessionMapper,
            BusinessLogMapper businessLogMapper,
            ReportService reportService,
            JsonService jsonService,
            RequestHashService requestHashService,
            IdempotencyRunner idempotencyRunner,
            VupStateMapper vupStateMapper,
            EndingService endingService,
            DebtService debtService,
            FormalEventPresenter formalEventPresenter,
            PendingInteractionPresenter pendingInteractionPresenter,
            BalanceConfig balanceConfig,
            DayFlowService dayFlowService,
            TitleService titleService,
            ActionFatigueService actionFatigueService,
            AudiencePressureService audiencePressureService,
            OperatingPressureService operatingPressureService,
            StageObjectiveService stageObjectiveService,
            RandomEventService randomEventService,
            CommercialRouteContentService commercialRouteContentService,
            MidgameEventContentService midgameEventContentService,
            DeterministicRngService deterministicRngService,
            PlatformTrendService platformTrendService,
            DailyFortuneService dailyFortuneService,
            com.example.vupworld.service.fan.RivalProgressService rivalProgressService,
            com.example.vupworld.service.event.RivalOvertakeEvent rivalOvertakeEvent
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
        this.endingService = endingService;
        this.debtService = debtService;
        this.formalEventPresenter = formalEventPresenter;
        this.pendingInteractionPresenter = pendingInteractionPresenter;
        this.balanceConfig = balanceConfig;
        this.dayFlowService = dayFlowService;
        this.titleService = titleService;
        this.actionFatigueService = actionFatigueService;
        this.audiencePressureService = audiencePressureService;
        this.operatingPressureService = operatingPressureService;
        this.stageObjectiveService = stageObjectiveService;
        this.randomEventService = randomEventService;
        this.commercialRouteContentService = commercialRouteContentService;
        this.midgameEventContentService = midgameEventContentService;
        this.deterministicRngService = deterministicRngService;
        this.platformTrendService = platformTrendService;
        this.dailyFortuneService = dailyFortuneService;
        this.rivalProgressService = rivalProgressService;
        this.rivalOvertakeEvent = rivalOvertakeEvent;
    }

    public List<ActionOptionDTO> listActions(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);
        List<RiskDebt> openDebts = debtService.openDebts(vup);
        OperatingPressureService.PressureState pressureState = operatingPressureService.pressureState(vup, session.getDay());
        return catalog().stream()
                .map(config -> toOption(config, vup, session, openDebts, pressureState))
                .toList();
    }

    @Transactional
    public DayResultDTO submitAction(Long userId, SubmitActionRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(DAY_ACTION_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, DAY_ACTION_PATH, idempotencyKey, requestHash,
                () -> doSubmitAction(userId, vup, session, request, idempotencyKey, requestHash, buildRecord),
                DayResultDTO.class, buildRecord);
    }

    @Transactional
    public DayResultDTO submitSchedule(Long userId, SubmitScheduleRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(DAY_SCHEDULE_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, DAY_SCHEDULE_PATH, idempotencyKey, requestHash,
                () -> doSubmitSchedule(userId, vup, session, request, idempotencyKey),
                DayResultDTO.class, buildRecord);
    }

    private DayResultDTO doSubmitSchedule(
            Long userId,
            Vup vup,
            DaySession session,
            SubmitScheduleRequest request,
            String idempotencyKey
    ) {
        if (!DayPhase.READY.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的排班已经处理过了，先把当前流程收完。");
        }

        String planKey = normalizeSchedulePlanKey(request.planKey());
        List<ScheduleSlotPlan> slots = normalizeScheduleSlots(request.slots());
        validateScheduleResources(vup, session, slots);

        List<ScheduleSlotOutcome> outcomes = new ArrayList<>();
        List<DebtCreatedDTO> debtCreated = new ArrayList<>();
        List<Long> scheduleDebtIds = new ArrayList<>();
        for (ScheduleSlotPlan slot : slots) {
            ScheduleSlotOutcome outcome = resolveScheduleSlot(vup, session, planKey, slot);
            outcomes.add(outcome);
            applyScheduleOutcome(vup, outcome);
            if (outcome.routeScoreChange() > 0 && outcome.routeType() != null && !RouteType.UNKNOWN.name().equals(outcome.routeType())) {
                dayFlowService.applyRouteScoreChange(vup, outcome.routeType(), outcome.routeScoreChange());
            }
            BusinessLog slotLog = toScheduleSlotLog(vup, session, outcome, idempotencyKey + ":" + slot.slotKey());
            businessLogMapper.insert(slotLog);
            DebtService.ActionDebtCreation actionDebtCreation = debtService.createForActionIfNeeded(vup, session, slotLog);
            if (!actionDebtCreation.created().isEmpty()) {
                debtCreated.addAll(actionDebtCreation.created());
            }
            if (!actionDebtCreation.debtIds().isEmpty()) {
                scheduleDebtIds.addAll(actionDebtCreation.debtIds());
                slotLog.setDebtIds(jsonService.write(actionDebtCreation.debtIds()));
                businessLogMapper.updateDebtIds(slotLog);
            }
            List<Map<String, Object>> defenseMitigations = debtService.applyDefensiveAction(vup, session, slotLog);
            if (!defenseMitigations.isEmpty()) {
                List<Long> mitigationIds = defenseMitigations.stream()
                        .map(item -> item.get("debtId"))
                        .filter(Number.class::isInstance)
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .toList();
                scheduleDebtIds.addAll(mitigationIds);
                slotLog.setDebtIds(jsonService.write(mitigationIds));
                businessLogMapper.updateDebtIds(slotLog);
            }
        }

        ActionResultDTO actionResult = aggregateScheduleResult(planKey, outcomes, debtCreated);
        vupMapper.updateState(vup);

        BusinessLog summaryLog = toScheduleSummaryLog(vup, session, actionResult, idempotencyKey, outcomes, scheduleDebtIds);
        businessLogMapper.insert(summaryLog);

        session.setSelectedAction("DAY_SCHEDULE");
        session.setOffStreamAction("SCHEDULED_DAY");
        session.setPhase(DayPhase.OFF_STREAM_RESOLVED.name());
        session.setPendingActionResultJson(jsonService.write(actionResult));
        daySessionMapper.updateAfterAction(session);

        return settleAfterOffStream(userId, vup, session);
    }

    private String normalizeSchedulePlanKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "FREE";
        }
        String value = raw.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (value) {
            case "STEADY", "STEADY_ROUTE", "STABLE_REPUTATION_ROUTE", "REPUTATION_ROUTE" -> "STEADY_ROUTE";
            case "HEAT", "HEAT_PUSH", "BURST_HEAT" -> "HEAT_PUSH";
            case "CONTENT", "CONTENT_BUILD", "MAKE_CONTENT" -> "CONTENT_BUILD";
            case "REPAIR", "REPAIR_STATUS", "RECOVERY" -> "REPAIR_STATUS";
            case "COMMERCIAL", "COMMERCIAL_CONVERSION", "BUSINESS" -> "COMMERCIAL_CONVERSION";
            case "FREE", "FREE_SCHEDULE" -> "FREE";
            default -> "FREE";
        };
    }

    private List<ScheduleSlotPlan> normalizeScheduleSlots(List<ScheduleSlotRequest> requests) {
        Map<String, ScheduleSlotRequest> bySlot = new LinkedHashMap<>();
        for (ScheduleSlotRequest request : requests == null ? List.<ScheduleSlotRequest>of() : requests) {
            String slotKey = normalizeScheduleSlotKey(request.slotKey());
            if (bySlot.containsKey(slotKey)) {
                throw new GameException("CONFIG_FIELD_INVALID", "同一个时段不能重复排班：" + slotLabelFor(slotKey));
            }
            bySlot.put(slotKey, request);
        }

        List<ScheduleSlotPlan> result = new ArrayList<>();
        for (String slotKey : SCHEDULE_SLOT_ORDER) {
            ScheduleSlotRequest request = bySlot.get(slotKey);
            if (request == null) {
                result.add(new ScheduleSlotPlan(slotKey, ActionType.REST, ScheduleIntensity.LIGHT));
                continue;
            }
            result.add(new ScheduleSlotPlan(
                    slotKey,
                    parseAction(request.actionType()),
                    normalizeScheduleIntensity(request.intensity())
            ));
        }
        return result;
    }

    private String normalizeScheduleSlotKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new GameException("CONFIG_FIELD_INVALID", "排班时段不能为空。");
        }
        String value = raw.trim();
        String upper = value.toUpperCase(java.util.Locale.ROOT);
        return switch (upper) {
            case "MORNING", "AM", "上午" -> "MORNING";
            case "NOON", "MIDDAY", "LUNCH", "中午" -> "NOON";
            case "AFTERNOON", "PM", "下午" -> "AFTERNOON";
            case "NIGHT", "EVENING", "晚上" -> "NIGHT";
            default -> throw new GameException("CONFIG_FIELD_INVALID", "未知排班时段：" + raw);
        };
    }

    private ScheduleIntensity normalizeScheduleIntensity(String raw) {
        if (raw == null || raw.isBlank()) {
            return ScheduleIntensity.STANDARD;
        }
        return switch (raw.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "LIGHT", "LOW", "轻量" -> ScheduleIntensity.LIGHT;
            case "STANDARD", "NORMAL", "标准" -> ScheduleIntensity.STANDARD;
            case "SPRINT", "HIGH", "冲刺" -> ScheduleIntensity.SPRINT;
            default -> throw new GameException("CONFIG_FIELD_INVALID", "未知排班强度：" + raw);
        };
    }

    private void validateScheduleResources(Vup vup, DaySession session, List<ScheduleSlotPlan> slots) {
        int inspiration = vup.getInspiration();
        int coin = vup.getCoin();
        int material = materialStock(vup);

        for (ScheduleSlotPlan slot : slots) {
            if (slot.actionType() == ActionType.STREAM_PLAN && session.isStreamPlanCancelled()) {
                throw new GameException("STREAM_PLAN_CANCELLED_TODAY", "今天已经取消过直播企划，排班里不能再塞直播企划。");
            }

            int inspirationChange = scheduleInspirationChange(slot.actionType());
            if (inspiration + inspirationChange < 0) {
                throw new GameException(
                        "INSUFFICIENT_INSPIRATION",
                        slotLabelFor(slot.slotKey()) + "的发布视频需要灵感，先把该时段改成收集素材、粉丝群维护或休息。"
                );
            }
            inspiration = clamp(inspiration + inspirationChange, 0, balanceConfig.inspirationMax());

            int materialChange = scheduleMaterialChange(slot.actionType());
            if (material + materialChange < 0) {
                throw new GameException(
                        "INSUFFICIENT_MATERIAL_STOCK",
                        slotLabelFor(slot.slotKey()) + "的发布切片需要切片素材，先安排收集素材或发布视频。"
                );
            }
            material += materialChange;

            int coinChange = scheduleProductionCoinChange(slot.actionType(), session.getDay());
            if (coin + coinChange < 0) {
                throw new GameException(
                        "INSUFFICIENT_COIN",
                        slotLabelFor(slot.slotKey()) + "的制作预算不足，先改成粉丝互动、商业回或休息。"
                );
            }
            coin = Math.max(0, coin + coinChange);
        }

        // AP 总量校验 + 扣减
        int totalApCost = slots.stream().mapToInt(s -> scheduleSlotApCost(s.actionType(), s.intensity())).sum();
        if (session.getActionPoints() < totalApCost) {
            throw new GameException("INSUFFICIENT_ACTION_POINTS",
                    "今日行动点不足（需要 " + totalApCost + " 点，剩余 " + session.getActionPoints() + " 点），调整排班强度或减少高消耗时段。");
        }
        session.setActionPoints(Math.max(0, session.getActionPoints() - totalApCost));
    }

    /** 排班时段 AP 消耗：强度决定成本，REST 固定1点。 */
    private int scheduleSlotApCost(ActionType actionType, ScheduleIntensity intensity) {
        if (actionType == ActionType.REST) return 1;
        return switch (intensity) {
            case LIGHT -> 1;
            case STANDARD -> 2;
            case SPRINT -> 3;
        };
    }

    private ScheduleSlotOutcome resolveScheduleSlot(
            Vup vup,
            DaySession session,
            String planKey,
            ScheduleSlotPlan slot
    ) {
        ActionType actionType = slot.actionType();
        ScheduleIntensity intensity = slot.intensity();
        String routeType = scheduleRouteType(planKey, actionType);
        int day = session.getDay();

        int trueFanChange = 0;
        int funFanChange = 0;
        int unicornFanChange = 0;
        int ddFanChange = 0;
        int popularityChange = 0;
        int watchHeatChange = 0;
        int reputationChange = 0;
        int memeChange = 0;
        int commercialChange = 0;
        int coinChange = 0;
        int inspirationChange = scheduleInspirationChange(actionType);
        int materialChange = scheduleMaterialChange(actionType);
        int routeScoreChange = 0;
        int attributeChange = scheduleAttributeChange(actionType, intensity);

        switch (actionType) {
            case TRAIN_SONG -> {
                trueFanChange = 2;
                ddFanChange = 1;
                popularityChange = 5;
                reputationChange = 1;
                routeScoreChange = 1;
            }
            case TRAIN_DANCE -> {
                trueFanChange = 1;
                funFanChange = 2;
                ddFanChange = 1;
                popularityChange = 5;
                watchHeatChange = 1;
                memeChange = 1;
                routeScoreChange = 1;
            }
            case TRAIN_TALK -> {
                trueFanChange = 2;
                popularityChange = 2;
                watchHeatChange = -1;
                reputationChange = 2;
                routeScoreChange = 1;
            }
            case STREAM_PLAN -> {
                trueFanChange = 3;
                funFanChange = 3;
                ddFanChange = 2;
                popularityChange = 16;
                watchHeatChange = 3;
                reputationChange = -1;
                routeScoreChange = 1;
                if ("STEADY_ROUTE".equals(planKey)) {
                    trueFanChange += 1;
                    funFanChange -= 1;
                    popularityChange -= 6;
                    watchHeatChange -= 2;
                    reputationChange += 2;
                } else if ("HEAT_PUSH".equals(planKey)) {
                    funFanChange += 3;
                    popularityChange += 8;
                    watchHeatChange += 3;
                    reputationChange -= 1;
                    memeChange += 1;
                } else if ("CONTENT_BUILD".equals(planKey)) {
                    funFanChange += 1;
                    popularityChange -= 3;
                    watchHeatChange -= 1;
                    memeChange += 1;
                } else if ("COMMERCIAL_CONVERSION".equals(planKey)) {
                    unicornFanChange += 3;
                    popularityChange -= 2;
                    watchHeatChange -= 2;
                    reputationChange += 1;
                    commercialChange += isCommercialReviewWeek(day) ? 2 : 1;
                    coinChange += isCommercialReviewWeek(day) ? 360 : 260;
                }
            }
            case PUBLISH_VIDEO -> {
                trueFanChange = 2;
                funFanChange = 4;
                ddFanChange = 1;
                popularityChange = 10;
                watchHeatChange = 2;
                reputationChange = 1;
                memeChange = 1;
                routeScoreChange = 1;
            }
            case PUBLISH_CLIP -> {
                funFanChange = 5;
                ddFanChange = 1;
                popularityChange = 12;
                watchHeatChange = 3;
                reputationChange = -1;
                memeChange = 2;
                routeScoreChange = 1;
            }
            case FAN_GROUP_MAINTAIN -> {
                trueFanChange = 3;
                popularityChange = 2;
                watchHeatChange = -1;
                reputationChange = 2;
                routeScoreChange = 1;
                if ("COMMERCIAL_CONVERSION".equals(planKey)) {
                    unicornFanChange += 2;
                    commercialChange += 1;
                    coinChange += isCommercialReviewWeek(day) ? 180 : 120;
                }
            }
            case NPC_INTERACT -> {
                trueFanChange = 1;
                ddFanChange = 4;
                popularityChange = 5;
                watchHeatChange = 2;
                reputationChange = 1;
                routeScoreChange = 1;
            }
            case REST -> {
                trueFanChange = 1;
                watchHeatChange = -1;
                reputationChange = 2;
                routeScoreChange = 0;
            }
        }

        if ("REPAIR_STATUS".equals(planKey)
                && (actionType == ActionType.TRAIN_TALK || actionType == ActionType.FAN_GROUP_MAINTAIN || actionType == ActionType.REST)) {
            reputationChange += 1;
            watchHeatChange -= 1;
        }

        int staminaChange = scheduleStaminaChange(actionType, intensity);
        popularityChange = scaleScheduleValue(popularityChange, intensity);
        watchHeatChange = scaleScheduleValue(watchHeatChange, intensity);
        reputationChange = scaleScheduleValue(reputationChange, intensity);
        memeChange = scaleScheduleValue(memeChange, intensity);
        commercialChange = scaleScheduleValue(commercialChange, intensity);
        coinChange = scaleScheduleValue(coinChange, intensity) + scheduleProductionCoinChange(actionType, day);
        trueFanChange = scaleScheduleValue(trueFanChange, intensity);
        funFanChange = scaleScheduleValue(funFanChange, intensity);
        unicornFanChange = scaleScheduleValue(unicornFanChange, intensity);
        ddFanChange = scaleScheduleValue(ddFanChange, intensity);
        routeScoreChange = scaleScheduleRouteScore(routeScoreChange, intensity);

        String slotName = scheduleSlotName(planKey, slot);
        String summary = slotLabelFor(slot.slotKey()) + "：" + slotName + "，" + scheduleIntensityLabel(intensity)
                + "。收益倾向：" + scheduleSlotOutcomeLine(actionType, trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                popularityChange, watchHeatChange, reputationChange, memeChange, coinChange, inspirationChange, routeScoreChange);

        return new ScheduleSlotOutcome(
                planKey,
                slot.slotKey(),
                slotLabelFor(slot.slotKey()),
                actionType,
                slotName,
                intensity,
                staminaChange,
                trueFanChange,
                funFanChange,
                unicornFanChange,
                ddFanChange,
                popularityChange,
                watchHeatChange,
                reputationChange,
                memeChange,
                commercialChange,
                coinChange,
                inspirationChange,
                materialChange,
                attributeChange,
                routeScoreChange,
                routeType,
                summary
        );
    }

    private void applyScheduleOutcome(Vup vup, ScheduleSlotOutcome outcome) {
        vup.setStamina(clamp(vup.getStamina() + outcome.staminaChange(), 0, vup.getMaxStamina()));
        vup.setTrueFans(Math.max(0, vup.getTrueFans() + outcome.trueFanChange()));
        vup.setFunFans(Math.max(0, vup.getFunFans() + outcome.funFanChange()));
        vup.setUnicornFans(Math.max(0, vup.getUnicornFans() + outcome.unicornFanChange()));
        vup.setDdFans(Math.max(0, vup.getDdFans() + outcome.ddFanChange()));
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        vup.setPopularity(Math.max(0, vup.getPopularity() + outcome.popularityChange()));
        vup.setWatchHeat(clamp(vup.getWatchHeat() + outcome.watchHeatChange(), 0, 100));
        vup.setReputation(clamp(vup.getReputation() + outcome.reputationChange(), 0, 100));
        vup.setMemeLevel(clamp(vup.getMemeLevel() + outcome.memeChange(), 0, 100));
        vup.setCommercialLevel(clamp(vup.getCommercialLevel() + outcome.commercialChange(), 0, 100));
        vup.setCoin(Math.max(0, vup.getCoin() + outcome.coinChange()));
        vup.setInspiration(clamp(vup.getInspiration() + outcome.inspirationChange(), 0, balanceConfig.inspirationMax()));

        switch (outcome.actionType()) {
            case TRAIN_SONG -> vup.setSongPower(clamp(vup.getSongPower() + outcome.attributeChange(), 0, 100));
            case TRAIN_DANCE -> vup.setDancePower(clamp(vup.getDancePower() + outcome.attributeChange(), 0, 100));
            case TRAIN_TALK -> vup.setTalkPower(clamp(vup.getTalkPower() + outcome.attributeChange(), 0, 100));
            case STREAM_PLAN, PUBLISH_VIDEO -> vup.setPlanPower(clamp(vup.getPlanPower() + outcome.attributeChange(), 0, 100));
            case PUBLISH_CLIP -> vup.setMemePower(clamp(vup.getMemePower() + outcome.attributeChange(), 0, 100));
            default -> {
            }
        }
    }

    private ActionResultDTO aggregateScheduleResult(
            String planKey,
            List<ScheduleSlotOutcome> outcomes,
            List<DebtCreatedDTO> debtCreated
    ) {
        int trueFanChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::trueFanChange).sum();
        int funFanChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::funFanChange).sum();
        int unicornFanChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::unicornFanChange).sum();
        int ddFanChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::ddFanChange).sum();
        int staminaChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::staminaChange).sum();
        int attributeChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::attributeChange).sum();
        int routeScoreChange = outcomes.stream().mapToInt(ScheduleSlotOutcome::routeScoreChange).sum();

        Map<String, Object> evidenceRef = new LinkedHashMap<>();
        evidenceRef.put("type", "day_schedule");
        evidenceRef.put("planKey", planKey);
        evidenceRef.put("planLabel", schedulePlanLabel(planKey));
        evidenceRef.put("targetRoute", dominantScheduleRoute(outcomes));
        evidenceRef.put("slots", outcomes.stream().map(this::scheduleSlotEvidence).toList());
        evidenceRef.put("resourceBudget", scheduleResourceBudgetEvidence(outcomes));

        String slotSummary = outcomes.stream()
                .map(outcome -> outcome.slotLabel() + outcome.actionName() + "（" + scheduleIntensityLabel(outcome.intensity()) + "）")
                .reduce((left, right) -> left + "；" + right)
                .orElse("自由排班");
        String summary = "今日运营方案：" + schedulePlanLabel(planKey) + "。"
                + slotSummary + "。结果倾向：" + schedulePlanResultHint(planKey)
                + " 合计：" + actionBenefitLine(
                trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                outcomes.stream().mapToInt(ScheduleSlotOutcome::popularityChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::watchHeatChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::reputationChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::memeChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::coinChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::inspirationChange).sum(),
                routeScoreChange
        ) + "；代价：" + actionCostLine(
                staminaChange,
                outcomes.stream().mapToInt(ScheduleSlotOutcome::reputationChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::watchHeatChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::coinChange).sum(),
                outcomes.stream().mapToInt(ScheduleSlotOutcome::inspirationChange).sum()
        ) + "。";

        return new ActionResultDTO(
                "DAY_SCHEDULE",
                null,
                summary,
                trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                trueFanChange,
                funFanChange,
                unicornFanChange,
                ddFanChange,
                staminaChange,
                attributeChange,
                routeScoreChange,
                debtCreated == null ? List.of() : List.copyOf(debtCreated),
                evidenceRef,
                null
        );
    }

    private BusinessLog toScheduleSlotLog(Vup vup, DaySession session, ScheduleSlotOutcome outcome, String idempotencyKey) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.ACTION_RESOLVED.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction(outcome.actionType().name());
        log.setMemeSubtype(scheduleMemeSubtypeFor(outcome.actionType()));
        log.setResult(outcome.summary());
        log.setRawFanGain(outcome.fanChange());
        log.setFinalFanGain(outcome.fanChange());
        log.setFanChange(outcome.fanChange());
        log.setTrueFanChange(outcome.trueFanChange());
        log.setFunFanChange(outcome.funFanChange());
        log.setUnicornFanChange(outcome.unicornFanChange());
        log.setDdFanChange(outcome.ddFanChange());
        log.setPopularityChange(outcome.popularityChange());
        log.setWatchHeatChange(outcome.watchHeatChange());
        log.setReputationChange(outcome.reputationChange());
        log.setMemeChange(outcome.memeChange());
        log.setCommercialChange(outcome.commercialChange());
        log.setCoinChange(outcome.coinChange());
        log.setInspirationChange(outcome.inspirationChange());
        log.setMultiplierDetail(jsonService.write(Map.of(
                "pipeline", "SCHEDULE_SLOT_V1",
                "intensity", outcome.intensity().name(),
                "intensityLabel", scheduleIntensityLabel(outcome.intensity())
        )));
        log.setCapDetail(jsonService.write(Map.of(
                "source", "SchedulePreset",
                "stageCap", stageFanCap(vup.getFans()),
                "hit", false
        )));
        log.setClampDetail(jsonService.write(Map.of(
                "stamina", vup.getStamina(),
                "reputation", vup.getReputation(),
                "watchHeat", vup.getWatchHeat()
        )));
        log.setWeightDetail(jsonService.write(scheduleSlotWeightDetail(outcome)));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hit", outcome.slotKey()
        )));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(scheduleRouteScoreChange(outcome)));
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(true);
        return log;
    }

    private BusinessLog toScheduleSummaryLog(
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            String idempotencyKey,
            List<ScheduleSlotOutcome> outcomes,
            List<Long> debtIds
    ) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.ACTION_RESOLVED.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction(actionResult.actionType());
        log.setResult(actionResult.summary());
        log.setRawFanGain(actionResult.fanChange());
        log.setFinalFanGain(actionResult.fanChange());
        log.setFanChange(actionResult.fanChange());
        log.setTrueFanChange(actionResult.trueFanChange());
        log.setFunFanChange(actionResult.funFanChange());
        log.setUnicornFanChange(actionResult.unicornFanChange());
        log.setDdFanChange(actionResult.ddFanChange());
        log.setPopularityChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::popularityChange).sum());
        log.setWatchHeatChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::watchHeatChange).sum());
        log.setReputationChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::reputationChange).sum());
        log.setMemeChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::memeChange).sum());
        log.setCommercialChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::commercialChange).sum());
        log.setCoinChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::coinChange).sum());
        log.setInspirationChange(outcomes.stream().mapToInt(ScheduleSlotOutcome::inspirationChange).sum());
        log.setMultiplierDetail(jsonService.write(Map.of(
                "pipeline", "DAY_SCHEDULE_V1",
                "planKey", actionResult.evidenceRef().get("planKey"),
                "slotCount", outcomes.size()
        )));
        log.setCapDetail(jsonService.write(Map.of(
                "source", "SchedulePreset",
                "stageCap", stageFanCap(vup.getFans()),
                "hit", actionResult.fanChange() >= stageFanCap(vup.getFans())
        )));
        log.setClampDetail(jsonService.write(Map.of(
                "stamina", vup.getStamina(),
                "reputation", vup.getReputation(),
                "watchHeat", vup.getWatchHeat()
        )));
        log.setWeightDetail(jsonService.write(Map.of(
                "action", actionResult.actionType(),
                "planKey", actionResult.evidenceRef().get("planKey"),
                "slots", outcomes.stream().map(this::scheduleSlotEvidence).toList()
        )));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hit", actionResult.actionType()
        )));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(routeScoreMapForSchedule(outcomes)));
        log.setDebtIds(jsonService.write(debtIds == null ? List.of() : debtIds.stream().distinct().toList()));
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(false);
        return log;
    }

    private int scheduleStaminaChange(ActionType actionType, ScheduleIntensity intensity) {
        if (actionType == ActionType.REST) {
            return switch (intensity) {
                case LIGHT -> Math.max(2, balanceConfig.restStaminaRecovery() - 1);
                case STANDARD, SPRINT -> balanceConfig.restStaminaRecovery();
            };
        }
        // 非REST行动不再消耗体力，体力通过压力/惩罚机制间接下降
        return 0;
    }

    private int scheduleInspirationChange(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_TALK, FAN_GROUP_MAINTAIN -> 1;
            case PUBLISH_VIDEO -> -1;
            default -> 0;
        };
    }

    private int scheduleMaterialChange(ActionType actionType) {
        return switch (actionType) {
            case PUBLISH_VIDEO -> 2;
            case FAN_GROUP_MAINTAIN -> 1;
            case PUBLISH_CLIP -> -1;
            default -> 0;
        };
    }

    private int scheduleProductionCoinChange(ActionType actionType, int day) {
        return -endgameProductionCoinCost(actionType, day);
    }

    private int scheduleAttributeChange(ActionType actionType, ScheduleIntensity intensity) {
        if (intensity == ScheduleIntensity.LIGHT) {
            return 0;
        }
        return switch (actionType) {
            case TRAIN_SONG, TRAIN_DANCE, TRAIN_TALK, STREAM_PLAN, PUBLISH_VIDEO, PUBLISH_CLIP -> 1;
            default -> 0;
        };
    }

    private int scaleScheduleValue(int value, ScheduleIntensity intensity) {
        if (value == 0) {
            return 0;
        }
        double multiplier = switch (intensity) {
            case LIGHT -> value > 0 ? 0.65 : 0.5;
            case STANDARD -> 1.0;
            case SPRINT -> value > 0 ? 1.45 : 1.5;
        };
        int scaled = (int) Math.round(Math.abs(value) * multiplier) * Integer.signum(value);
        return scaled == 0 ? Integer.signum(value) : scaled;
    }

    private int scaleScheduleRouteScore(int routeScoreChange, ScheduleIntensity intensity) {
        if (routeScoreChange <= 0) {
            return 0;
        }
        return switch (intensity) {
            case LIGHT -> Math.max(0, routeScoreChange - 1);
            case STANDARD -> routeScoreChange;
            case SPRINT -> routeScoreChange + 1;
        };
    }

    private String scheduleRouteType(String planKey, ActionType actionType) {
        return switch (actionType) {
            case TRAIN_SONG -> RouteType.SINGING_IDOL.name();
            case TRAIN_DANCE -> RouteType.DANCE_MEME.name();
            case TRAIN_TALK, FAN_GROUP_MAINTAIN -> RouteType.ELECTRONIC_PICKLE.name();
            case PUBLISH_VIDEO, PUBLISH_CLIP -> RouteType.SLICE_SAINT.name();
            case NPC_INTERACT -> RouteType.SOCIAL_COLLAB.name();
            case STREAM_PLAN -> switch (planKey) {
                case "HEAT_PUSH" -> RouteType.BLACK_RED_MAIN_STAGE.name();
                case "CONTENT_BUILD" -> RouteType.SLICE_SAINT.name();
                case "STEADY_ROUTE", "COMMERCIAL_CONVERSION" -> RouteType.ELECTRONIC_PICKLE.name();
                default -> RouteType.SINGING_IDOL.name();
            };
            case REST -> null;
        };
    }

    private String dominantScheduleRoute(List<ScheduleSlotOutcome> outcomes) {
        return routeScoreMapForSchedule(outcomes).entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Number)
                .map(entry -> Map.entry(entry.getKey(), ((Number) entry.getValue()).intValue()))
                .filter(entry -> entry.getValue() > 0 && !RouteType.UNKNOWN.name().equals(entry.getKey()))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(RouteType.UNKNOWN.name());
    }

    private Map<String, Object> routeScoreMapForSchedule(List<ScheduleSlotOutcome> outcomes) {
        Map<String, Object> routeScores = new LinkedHashMap<>();
        for (ScheduleSlotOutcome outcome : outcomes) {
            String routeType = outcome.routeType() == null || outcome.routeType().isBlank()
                    ? RouteType.UNKNOWN.name()
                    : outcome.routeType();
            routeScores.put(routeType, ((Number) routeScores.getOrDefault(routeType, 0)).intValue() + outcome.routeScoreChange());
        }
        routeScores.put("source", "DAY_SCHEDULE");
        return routeScores;
    }

    private Map<String, Object> scheduleRouteScoreChange(ScheduleSlotOutcome outcome) {
        Map<String, Object> routeScoreChange = new LinkedHashMap<>();
        String routeType = outcome.routeType() == null || outcome.routeType().isBlank()
                ? RouteType.UNKNOWN.name()
                : outcome.routeType();
        routeScoreChange.put(routeType, outcome.routeScoreChange());
        routeScoreChange.put("source", "schedule:" + outcome.slotKey());
        routeScoreChange.put("planKey", outcome.planKey());
        return routeScoreChange;
    }

    private Map<String, Object> scheduleSlotWeightDetail(ScheduleSlotOutcome outcome) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rollType", "schedule_slot");
        detail.put("planKey", outcome.planKey());
        detail.put("slotKey", outcome.slotKey());
        detail.put("slotLabel", outcome.slotLabel());
        detail.put("action", outcome.actionType().name());
        detail.put("actionName", outcome.actionName());
        detail.put("intensity", outcome.intensity().name());
        detail.put("routeType", outcome.routeType());
        detail.put("materialDelta", outcome.materialChange());
        return detail;
    }

    private Map<String, Object> scheduleSlotEvidence(ScheduleSlotOutcome outcome) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("type", "schedule_slot");
        ref.put("slotKey", outcome.slotKey());
        ref.put("slotLabel", outcome.slotLabel());
        ref.put("actionType", outcome.actionType().name());
        ref.put("actionName", outcome.actionName());
        ref.put("intensity", outcome.intensity().name());
        ref.put("intensityLabel", scheduleIntensityLabel(outcome.intensity()));
        ref.put("fanDelta", outcome.fanChange());
        ref.put("staminaDelta", outcome.staminaChange());
        ref.put("routeType", outcome.routeType());
        ref.put("routeScoreDelta", outcome.routeScoreChange());
        ref.put("materialDelta", outcome.materialChange());
        return ref;
    }

    private Map<String, Object> scheduleResourceBudgetEvidence(List<ScheduleSlotOutcome> outcomes) {
        Map<String, Object> budget = new LinkedHashMap<>();
        budget.put("staminaDelta", outcomes.stream().mapToInt(ScheduleSlotOutcome::staminaChange).sum());
        budget.put("coinDelta", outcomes.stream().mapToInt(ScheduleSlotOutcome::coinChange).sum());
        budget.put("inspirationDelta", outcomes.stream().mapToInt(ScheduleSlotOutcome::inspirationChange).sum());
        budget.put("materialDelta", outcomes.stream().mapToInt(ScheduleSlotOutcome::materialChange).sum());
        return budget;
    }

    private String scheduleMemeSubtypeFor(ActionType actionType) {
        if (actionType == ActionType.PUBLISH_CLIP) {
            return "CLIP_SPREAD";
        }
        if (actionType == ActionType.PUBLISH_VIDEO) {
            return "VIDEO_PUBLISH";
        }
        return null;
    }

    private String slotLabelFor(String slotKey) {
        return switch (slotKey) {
            case "MORNING" -> "上午";
            case "NOON" -> "中午";
            case "AFTERNOON" -> "下午";
            case "NIGHT" -> "晚上";
            default -> "时段";
        };
    }

    private String scheduleIntensityLabel(ScheduleIntensity intensity) {
        return switch (intensity) {
            case LIGHT -> "轻量";
            case STANDARD -> "标准";
            case SPRINT -> "冲刺";
        };
    }

    private String scheduleActionLabel(ActionType actionType) {
        return dayFlowService.actionLabel(actionType.name());
    }

    private String scheduleSlotName(String planKey, ScheduleSlotPlan slot) {
        ActionType presetType = presetScheduleActionType(planKey, slot.slotKey());
        String presetName = presetScheduleSlotName(planKey, slot.slotKey());
        if (presetType == slot.actionType() && presetName != null) {
            return presetName;
        }
        return scheduleActionLabel(slot.actionType());
    }

    private ActionType presetScheduleActionType(String planKey, String slotKey) {
        return switch (planKey + ":" + slotKey) {
            case "STEADY_ROUTE:MORNING" -> ActionType.TRAIN_TALK;
            case "STEADY_ROUTE:NOON" -> ActionType.FAN_GROUP_MAINTAIN;
            case "STEADY_ROUTE:AFTERNOON" -> ActionType.STREAM_PLAN;
            case "STEADY_ROUTE:NIGHT" -> ActionType.TRAIN_TALK;
            case "HEAT_PUSH:MORNING", "HEAT_PUSH:NOON", "HEAT_PUSH:NIGHT" -> ActionType.STREAM_PLAN;
            case "HEAT_PUSH:AFTERNOON" -> ActionType.PUBLISH_CLIP;
            case "CONTENT_BUILD:MORNING" -> ActionType.FAN_GROUP_MAINTAIN;
            case "CONTENT_BUILD:NOON" -> ActionType.REST;
            case "CONTENT_BUILD:AFTERNOON" -> ActionType.PUBLISH_VIDEO;
            case "CONTENT_BUILD:NIGHT" -> ActionType.STREAM_PLAN;
            case "REPAIR_STATUS:MORNING" -> ActionType.REST;
            case "REPAIR_STATUS:NOON" -> ActionType.FAN_GROUP_MAINTAIN;
            case "REPAIR_STATUS:AFTERNOON", "REPAIR_STATUS:NIGHT" -> ActionType.TRAIN_TALK;
            case "COMMERCIAL_CONVERSION:MORNING", "COMMERCIAL_CONVERSION:AFTERNOON", "COMMERCIAL_CONVERSION:NIGHT" -> ActionType.STREAM_PLAN;
            case "COMMERCIAL_CONVERSION:NOON" -> ActionType.FAN_GROUP_MAINTAIN;
            default -> null;
        };
    }

    private String presetScheduleSlotName(String planKey, String slotKey) {
        return switch (planKey + ":" + slotKey) {
            case "STEADY_ROUTE:MORNING" -> "练谈吐";
            case "STEADY_ROUTE:NOON" -> "粉丝群维护";
            case "STEADY_ROUTE:AFTERNOON" -> "直播企划";
            case "STEADY_ROUTE:NIGHT" -> "低压杂谈";
            case "HEAT_PUSH:MORNING" -> "刷趋势";
            case "HEAT_PUSH:NOON" -> "标题企划";
            case "HEAT_PUSH:AFTERNOON" -> "切片准备";
            case "HEAT_PUSH:NIGHT" -> "高热直播";
            case "CONTENT_BUILD:MORNING" -> "收集素材";
            case "CONTENT_BUILD:NOON" -> "休息/读信";
            case "CONTENT_BUILD:AFTERNOON" -> "投稿制作";
            case "CONTENT_BUILD:NIGHT" -> "短直播预热";
            case "REPAIR_STATUS:MORNING" -> "休息";
            case "REPAIR_STATUS:NOON" -> "读粉丝信";
            case "REPAIR_STATUS:AFTERNOON" -> "危机处理/复盘";
            case "REPAIR_STATUS:NIGHT" -> "低压陪聊";
            case "COMMERCIAL_CONVERSION:MORNING" -> "直播企划";
            case "COMMERCIAL_CONVERSION:NOON" -> "粉丝互动";
            case "COMMERCIAL_CONVERSION:AFTERNOON" -> "商务准备";
            case "COMMERCIAL_CONVERSION:NIGHT" -> "感谢礼物/商业回";
            default -> null;
        };
    }

    private String schedulePlanLabel(String planKey) {
        return switch (planKey) {
            case "STEADY_ROUTE" -> "稳口碑补路线";
            case "HEAT_PUSH" -> "冲热度";
            case "CONTENT_BUILD" -> "做内容";
            case "REPAIR_STATUS" -> "修状态";
            case "COMMERCIAL_CONVERSION" -> "商业转化";
            default -> "自由排班";
        };
    }

    private String schedulePlanResultHint(String planKey) {
        return switch (planKey) {
            case "STEADY_ROUTE" -> "口碑稳、旧账低、涨粉中等";
            case "HEAT_PUSH" -> "围观高、涨粉快、风险升";
            case "CONTENT_BUILD" -> "灵感和素材转化，路线证据更清楚";
            case "REPAIR_STATUS" -> "体力回一点，口碑和旧账更安全";
            case "COMMERCIAL_CONVERSION" -> "礼物收入、运营预算上升，但商业味和压力也会上升";
            default -> "按手动排班结算";
        };
    }

    private String scheduleSlotOutcomeLine(
            ActionType actionType,
            int fanChange,
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange,
            int coinChange,
            int inspirationChange,
            int routeScoreChange
    ) {
        List<String> pieces = new ArrayList<>();
        if (fanChange != 0) {
            pieces.add("粉丝" + signed(fanChange));
        }
        if (popularityChange != 0) {
            pieces.add("人气" + signed(popularityChange));
        }
        if (watchHeatChange != 0) {
            pieces.add("围观" + signed(watchHeatChange));
        }
        if (reputationChange != 0) {
            pieces.add("口碑" + signed(reputationChange));
        }
        if (memeChange != 0) {
            pieces.add("梗" + signed(memeChange));
        }
        if (coinChange != 0) {
            pieces.add("预算" + signed(coinChange));
        }
        if (inspirationChange != 0) {
            pieces.add("灵感" + signed(inspirationChange));
        }
        if (routeScoreChange != 0) {
            pieces.add("路线" + signed(routeScoreChange));
        }
        if (pieces.isEmpty()) {
            return actionType == ActionType.REST ? "恢复体力，降低压力" : "稳住节奏";
        }
        return String.join("，", pieces);
    }

    private DayResultDTO doSubmitAction(
            Long userId, Vup vup, DaySession session, SubmitActionRequest request,
            String idempotencyKey, String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        if (!DayPhase.READY.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的主行动已经处理过了，标题组不能无限加班。");
        }

        ActionType actionType = parseAction(request.actionType());
        OperatingPressureService.PressureState pressureState = operatingPressureService.pressureState(vup, session.getDay());
        ActionConfig config = catalog().stream()
                .filter(item -> item.actionType == actionType)
                .findFirst()
                .orElseThrow(() -> new GameException("CONFIG_FIELD_INVALID", "未知行动。"));

        if (operatingPressureService.isLocked(vup, session.getDay(), actionType)) {
            OperatingPressureService.PressureReplacement replacement = operatingPressureService.replacementForAction(vup, session.getDay(), actionType);
            throw new GameException("OPERATIONAL_PRESSURE_LOCKED", replacement.hint());
        }
        int actionPointCost = actionType.actionPointCost();
        if (actionType != ActionType.REST && session.getActionPoints() < actionPointCost) {
            throw new GameException("INSUFFICIENT_ACTION_POINTS", "行动点不足，今天只能休息或进入场外。");
        }
        if (actionType == ActionType.PUBLISH_VIDEO && vup.getInspiration() < 1) {
            throw new GameException("INSUFFICIENT_INSPIRATION", "灵感不足，投稿组还没攒出能发布的视频。");
        }
        if (actionType == ActionType.PUBLISH_CLIP && materialStock(vup) < 1) {
            throw new GameException("INSUFFICIENT_MATERIAL_STOCK", "素材库为0，切片组不能空剪。先发布视频、开直播企划，或在粉丝群议题里改成投稿征集。");
        }
        int productionCoinCost = endgameProductionCoinCost(actionType, session.getDay());
        if (productionCoinCost > 0 && vup.getCoin() < productionCoinCost) {
            throw new GameException("INSUFFICIENT_COIN", "收官冲刺制作预算不足，先用商业标题或粉丝群维护补一点运营预算。");
        }
        // 行动点扣减（REST 在 AP 不足时仍允许，但同样扣减）
        session.setActionPoints(Math.max(0, session.getActionPoints() - actionPointCost));
        if (actionType == ActionType.STREAM_PLAN && session.isStreamPlanCancelled()) {
            throw new GameException("STREAM_PLAN_CANCELLED_TODAY", "今天已经取消过直播企划，标题组不能靠刷新池子加班。");
        }
        if (actionType == ActionType.STREAM_PLAN) {
            return doStartStreamPlan(vup, session, request, idempotencyKey, requestHash, buildRecord);
        }

        String npcTendency = null;
        if (actionType == ActionType.NPC_INTERACT) {
            npcTendency = normalizeNpcTendency(request.planType());
            validateNpcInteractionCooldown(vup, session, npcTendency);
        }

        ActionResultDTO actionResult = resolveNonStreamAction(vup, config, npcTendency);
        operatingPressureService.recordActionPressure(vup, session.getDay(), actionType);
        pressureState = operatingPressureService.pressureState(vup, session.getDay());
        vupMapper.updateState(vup);
        BusinessLog log = toBusinessLog(vup, session, actionResult, idempotencyKey);
        businessLogMapper.insert(log);
        DebtService.ActionDebtCreation actionDebtCreation = debtService.createForActionIfNeeded(vup, session, log);
        if (!actionDebtCreation.created().isEmpty()) {
            actionResult = actionResult.withDebtCreated(actionDebtCreation.created());
        }
        if (!actionDebtCreation.debtIds().isEmpty()) {
            log.setDebtIds(jsonService.write(actionDebtCreation.debtIds()));
            businessLogMapper.updateDebtIds(log);
        }
        session.setSelectedAction(actionType.name());
        session.setPendingActionResultJson(jsonService.write(actionResult));
        if (!shouldKeepRouteActionUninterrupted(vup, session, actionResult)) {
            DayResultDTO midgameEvent = pauseForMidgameEventIfNeeded(vup, session, actionResult);
            if (midgameEvent != null) {
                return midgameEvent;
            }
            DayResultDTO lateGameEvent = pauseForLateGameEventIfNeeded(vup, session, actionResult);
            if (lateGameEvent != null) {
                return lateGameEvent;
            }
        }
        DayResultDTO pendingEvent = pauseForDueDebtIfNeeded(userId, vup, session, actionResult, idempotencyKey, requestHash, buildRecord);
        if (pendingEvent != null) {
            return pendingEvent;
        }

        List<Map<String, Object>> defenseMitigations = debtService.applyDefensiveAction(vup, session, log);
        if (!defenseMitigations.isEmpty()) {
            actionResult.evidenceRef().put("defenseMitigation", defenseMitigations);
            log.setDebtIds(jsonService.write(defenseMitigations.stream()
                    .map(item -> item.get("debtId"))
                    .toList()));
            businessLogMapper.updateDebtIds(log);
        }

        session.setPendingActionResultJson(jsonService.write(actionResult));
        DayResultDTO fanTopicEvent = pauseForFanTopicEscalationIfNeeded(userId, vup, session, actionResult, idempotencyKey, requestHash, buildRecord);
        if (fanTopicEvent != null) {
            return fanTopicEvent;
        }
        DayResultDTO ordinaryEvent = pauseForOrdinaryEventIfNeeded(userId, vup, session, actionResult, idempotencyKey, requestHash, buildRecord);
        if (ordinaryEvent != null) {
            return ordinaryEvent;
        }

        DayResultDTO rivalOvertake = pauseForRivalOvertakeIfNeeded(vup, session, actionResult);
        if (rivalOvertake != null) {
            return rivalOvertake;
        }

        return queueOffStreamSettlement(session, actionResult);
    }

    DayResultDTO queueOffStreamSettlement(DaySession session, ActionResultDTO actionResult) {
        if (!balanceConfig.offStreamRequired()) {
            session.setPendingActionResultJson(jsonService.write(actionResult));
            session.setOffStreamAction("AUTO_SKIPPED");
            session.setPhase(DayPhase.OFF_STREAM_RESOLVED.name());
            daySessionMapper.updateAfterAction(session);
            return settleAfterOffStream(null, null, session);
        }
        session.setPendingActionResultJson(jsonService.write(actionResult));
        session.setPhase(DayPhase.OFF_STREAM_READY.name());
        daySessionMapper.updateAfterAction(session);
        return new DayResultDTO(session.getPhase(), actionResult, null, null, false, null, null);
    }

    @Transactional
    public DaySessionDTO cancelPendingAction(Long userId, CancelActionRequest request) {
        String idempotencyKey = idempotencyRunner.requireKey(request.idempotencyKey());
        String requestHash = requestHashService.hash(request);

        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        BiFunction<String, DaySessionDTO, ApiIdempotencyRecord> buildRecord = (hash, result) -> {
            ApiIdempotencyRecord record = new ApiIdempotencyRecord();
            record.setUserId(userId);
            record.setVupId(vup.getId());
            record.setDay(session.getDay());
            record.setApiPath(CANCEL_ACTION_PATH);
            record.setPhase(session.getPhase());
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestHash(hash);
            record.setResponseJson(jsonService.write(result));
            record.setStatus("SUCCESS");
            return record;
        };

        return idempotencyRunner.execute(userId, CANCEL_ACTION_PATH, idempotencyKey, requestHash,
                () -> doCancelPendingAction(vup, session),
                DaySessionDTO.class, buildRecord);
    }

    private DaySessionDTO doCancelPendingAction(Vup vup, DaySession session) {
        if (!DayPhase.NEED_TITLE.name().equals(session.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "当前没有待取消的直播企划。");
        }

        session.setCancelledPlanSnapshotJson(jsonService.write(Map.of(
                "selectedAction", session.getSelectedAction(),
                "selectedPlanId", session.getSelectedPlanId(),
                "titleCandidatesJson", session.getTitleCandidatesJson(),
                "titleRerollCount", session.getTitleRerollCount()
        )));
        session.setPhase(DayPhase.READY.name());
        session.setSelectedAction(null);
        session.setSelectedPlanId(null);
        session.setSelectedTitleTemplateId(null);
        session.setTitleCandidatesJson("[]");
        session.setTitleRerollCount(0);
        session.setPendingActionResultJson(null);
        session.setReportId(null);
        session.setStreamPlanCancelled(true);
        daySessionMapper.updateAfterAction(session);

        return vupStateMapper.toDaySessionDto(session);
    }

    public List<OffStreamOptionDTO> getOffStreamOptions(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        String phase = session.getPhase();
        if (!DayPhase.OFF_STREAM_READY.name().equals(phase)) {
            throw new GameException("PHASE_NOT_ALLOWED", "下播行动只能在今日主行动结算后、日报生成前选择。");
        }
        if (session.getOffStreamAction() != null && !session.getOffStreamAction().isBlank()) {
            throw new GameException("OFF_STREAM_ALREADY_USED", "今天已经安排过下播行动，不能在日报后免费补点。");
        }

        String currentRoute = vup.getCurrentRoute();
        String effectiveRoute = (currentRoute == null || currentRoute.isBlank()) ? "UNKNOWN" : currentRoute;

        List<OffStreamOptionDTO> options = new ArrayList<>();
        OFF_STREAM_CATALOG.forEach((type, config) -> {
            boolean routeMatch = config.routeBias() != null && config.routeBias().equals(effectiveRoute);
            boolean isGeneric = config.routeBias() == null;
            boolean recommended = routeMatch;
            String routeBiasLabel = isGeneric ? "通用" : routeBiasLabel(config.routeBias());
            String recommendedReason = recommended
                    ? config.recommendedReason()
                    : (isGeneric
                    ? "通用稳手：不强推路线，适合看不清明天方向时保留余地。"
                    : "非当前路线：收益仍然有效，但不会优先补你现在的路线证据。");
            options.add(new OffStreamOptionDTO(
                    type,
                    config.name(),
                    config.effectPreview(),
                    config.benefitPreview(),
                    config.costPreview(),
                    config.bestFor(),
                    config.riskPreview(),
                    recommendedReason,
                    routeBiasLabel,
                    recommended
            ));
        });

        // Sort: recommended first, then route-specific, then generic
        options.sort(Comparator.<OffStreamOptionDTO, Boolean>comparing(o -> !o.recommended())
                .thenComparing(o -> o.routeBias().equals("通用") ? 1 : 0));
        return options;
    }

    @Transactional
    public DayResultDTO submitOffStreamAction(Long userId, String offStreamType) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        String phase = session.getPhase();
        if (!DayPhase.OFF_STREAM_READY.name().equals(phase)) {
            throw new GameException("PHASE_NOT_ALLOWED", "下播行动只能在今日主行动结算后、日报生成前执行。");
        }
        if (session.getOffStreamAction() != null && !session.getOffStreamAction().isBlank()) {
            throw new GameException("OFF_STREAM_ALREADY_USED", "今天已经安排过下播行动，不能重复执行。");
        }

        OffStreamConfig config = OFF_STREAM_CATALOG.get(offStreamType);
        if (config == null) {
            throw new GameException("CONFIG_FIELD_INVALID", "未知下播行动类型。");
        }

        OffStreamResolution offStreamResolution = resolveOffStream(vup, offStreamType, config);
        applyOffStreamEffect(vup, offStreamResolution);
        ActionResultDTO actionResult = withOffStreamResolution(pendingActionResultFor(session), offStreamResolution);
        session.setPendingActionResultJson(jsonService.write(actionResult));
        session.setOffStreamAction(offStreamType);
        session.setPhase(DayPhase.OFF_STREAM_RESOLVED.name());
        vupMapper.updateState(vup);
        daySessionMapper.updateAfterAction(session);

        return settleAfterOffStream(userId, vup, session);
    }

    @Transactional
    public DayResultDTO skipOffStream(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        DaySession session = dayFlowService.requireCurrentSession(vup);

        String phase = session.getPhase();
        if (!DayPhase.OFF_STREAM_READY.name().equals(phase)) {
            throw new GameException("PHASE_NOT_ALLOWED", "Off-stream can only be skipped before the daily report is created.");
        }
        if (session.getOffStreamAction() != null && !session.getOffStreamAction().isBlank()) {
            throw new GameException("OFF_STREAM_ALREADY_USED", "Off-stream has already been handled today.");
        }

        OffStreamResolution offStreamResolution = skippedOffStreamResolution(vup);
        ActionResultDTO actionResult = withOffStreamResolution(pendingActionResultFor(session), offStreamResolution);
        session.setPendingActionResultJson(jsonService.write(actionResult));
        session.setOffStreamAction("SKIPPED");
        session.setPhase(DayPhase.OFF_STREAM_RESOLVED.name());
        daySessionMapper.updateAfterAction(session);

        return settleAfterOffStream(userId, vup, session);
    }

    private DayResultDTO settleAfterOffStream(Long userId, Vup vup, DaySession session) {
        if (vup == null) {
            vup = vupMapper.findById(session.getVupId());
            if (vup == null) {
                throw new GameException("VUP_NOT_FOUND", "当前VUP不存在，无法结算下播行动。");
            }
        }
        ActionResultDTO actionResult = pendingActionResultFor(session);
        BusinessLog log = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), session.getDay());
        if (log == null) {
            throw new GameException("SYSTEM_ERROR", "Daily action log is missing for off-stream settlement.");
        }
        if (log.getReportId() != null) {
            throw new GameException("REPORT_ALREADY_CREATED", "Daily report has already been created.");
        }
        applyOffStreamLogImpact(log, actionResult);

        if (!shouldKeepRouteActionUninterrupted(vup, session, actionResult)) {
            DayResultDTO midgameEvent = pauseForMidgameEventIfNeeded(vup, session, actionResult);
            if (midgameEvent != null) {
                return midgameEvent;
            }
            DayResultDTO lateGameEvent = pauseForLateGameEventIfNeeded(vup, session, actionResult);
            if (lateGameEvent != null) {
                return lateGameEvent;
            }
        }

        DayResultDTO pendingEvent = pauseForDueDebtIfNeeded(userId, vup, session, actionResult, null, null, null);
        if (pendingEvent != null) {
            return pendingEvent;
        }

        List<Map<String, Object>> defenseMitigations = debtService.applyDefensiveAction(vup, session, log);
        if (!defenseMitigations.isEmpty()) {
            actionResult.evidenceRef().put("defenseMitigation", defenseMitigations);
            log.setDebtIds(jsonService.write(defenseMitigations.stream()
                    .map(item -> item.get("debtId"))
                    .toList()));
            businessLogMapper.updateDebtIds(log);
        }

        session.setPendingActionResultJson(jsonService.write(actionResult));
        DayResultDTO fanTopicEvent = pauseForFanTopicEscalationIfNeeded(userId, vup, session, actionResult, null, null, null);
        if (fanTopicEvent != null) {
            return fanTopicEvent;
        }
        DayResultDTO ordinaryEvent = pauseForOrdinaryEventIfNeeded(userId, vup, session, actionResult, null, null, null);
        if (ordinaryEvent != null) {
            return ordinaryEvent;
        }

        var report = reportService.createReport(
                vup,
                session,
                actionResult,
                log,
                selectedTitleText(session),
                offStreamEvidenceRefs(actionResult)
        );
        log.setReportId(report.getId());
        businessLogMapper.attachReport(log);

        boolean endingReady = dayFlowService.finishSessionAfterReport(vup, session, report, log);
        session.setReportId(report.getId());
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(session.getPhase(), actionResult, null, report.getId(), endingReady, null, null);
    }

    private ActionResultDTO pendingActionResultFor(DaySession session) {
        if (session.getPendingActionResultJson() == null || session.getPendingActionResultJson().isBlank()) {
            throw new GameException("SYSTEM_ERROR", "Daily action result is missing for off-stream settlement.");
        }
        return jsonService.read(session.getPendingActionResultJson(), ActionResultDTO.class);
    }

    private String selectedTitleText(DaySession session) {
        if (session.getSelectedTitleTemplateId() == null
                || session.getTitleCandidatesJson() == null
                || session.getTitleCandidatesJson().isBlank()) {
            return null;
        }
        return jsonService.readTitleOptions(session.getTitleCandidatesJson()).stream()
                .filter(candidate -> candidate.id().equals(session.getSelectedTitleTemplateId()))
                .map(TitleOptionDTO::titleText)
                .findFirst()
                .orElse(null);
    }

    private OffStreamResolution resolveOffStream(Vup vup, String type, OffStreamConfig config) {
        String effectiveRoute = effectiveRoute(vup);
        boolean recommended = config.routeBias() != null && config.routeBias().equals(effectiveRoute);
        String routeBiasLabel = config.routeBias() == null ? "通用" : routeBiasLabel(config.routeBias());
        String recommendedReason = recommended
                ? config.recommendedReason()
                : (config.routeBias() == null
                ? "通用稳手：不强推路线，适合看不清明天方向时保留余地。"
                : "非当前路线：收益仍然有效，但不会优先补你现在的路线证据。");
        return new OffStreamResolution(
                type,
                config.name(),
                "下播行动：" + config.name() + "。收益：" + config.benefitPreview()
                        + " 代价：" + config.costPreview()
                        + " 风险预览：" + config.riskPreview(),
                config.benefitPreview(),
                config.costPreview(),
                config.bestFor(),
                config.riskPreview(),
                recommendedReason,
                routeBiasLabel,
                recommended,
                actualOffStreamImpact(vup, config.impact())
        );
    }

    private OffStreamResolution skippedOffStreamResolution(Vup vup) {
        return new OffStreamResolution(
                "SKIPPED",
                "跳过下播行动",
                "下播行动：跳过。收益：没有追加补强。代价：今天的余波没有被二次处理。风险预览：日报只记录主行动结果。",
                "没有追加收益，保留当前资源。",
                "错过一次免费微调窗口。",
                "已经满意今天结果，或想让主行动独自承担后果时。",
                "没有新风险，但也没有补救旧账。",
                "跳过不会惩罚路线，只是少一次把余波写进证据链的机会。",
                routeBiasLabel(effectiveRoute(vup)),
                false,
                new OffStreamImpact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
    }

    private OffStreamImpact actualOffStreamImpact(Vup vup, OffStreamImpact requested) {
        int coinDelta = requested.coinDelta() < 0
                ? -Math.min(vup.getCoin(), Math.abs(requested.coinDelta()))
                : requested.coinDelta();
        int inspirationDelta = requested.inspirationDelta() < 0
                ? -Math.min(vup.getInspiration(), Math.abs(requested.inspirationDelta()))
                : Math.min(balanceConfig.inspirationMax() - vup.getInspiration(), requested.inspirationDelta());
        return new OffStreamImpact(
                requested.songPowerDelta(),
                requested.dancePowerDelta(),
                requested.talkPowerDelta(),
                requested.popularityDelta(),
                requested.watchHeatDelta(),
                requested.reputationDelta(),
                requested.memeDelta(),
                requested.commercialDelta(),
                coinDelta,
                inspirationDelta,
                requested.trueFanDelta(),
                requested.funFanDelta(),
                requested.unicornFanDelta(),
                requested.ddFanDelta(),
                requested.routeScoreDelta()
        );
    }

    private void applyOffStreamEffect(Vup vup, OffStreamResolution resolution) {
        OffStreamImpact impact = resolution.impact();
        vup.setSongPower(clamp(vup.getSongPower() + impact.songPowerDelta(), 0, 100));
        vup.setDancePower(clamp(vup.getDancePower() + impact.dancePowerDelta(), 0, 100));
        vup.setTalkPower(clamp(vup.getTalkPower() + impact.talkPowerDelta(), 0, 100));
        vup.setPopularity(Math.max(0, vup.getPopularity() + impact.popularityDelta()));
        vup.setWatchHeat(clamp(vup.getWatchHeat() + impact.watchHeatDelta(), 0, 100));
        vup.setReputation(clamp(vup.getReputation() + impact.reputationDelta(), 0, 100));
        vup.setMemeLevel(clamp(vup.getMemeLevel() + impact.memeDelta(), 0, 100));
        vup.setCommercialLevel(clamp(vup.getCommercialLevel() + impact.commercialDelta(), 0, 100));
        vup.setCoin(Math.max(0, vup.getCoin() + impact.coinDelta()));
        vup.setInspiration(clamp(vup.getInspiration() + impact.inspirationDelta(), 0, balanceConfig.inspirationMax()));
        vup.setTrueFans(Math.max(0, vup.getTrueFans() + impact.trueFanDelta()));
        vup.setFunFans(Math.max(0, vup.getFunFans() + impact.funFanDelta()));
        vup.setUnicornFans(Math.max(0, vup.getUnicornFans() + impact.unicornFanDelta()));
        vup.setDdFans(Math.max(0, vup.getDdFans() + impact.ddFanDelta()));
        vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
    }

    private ActionResultDTO withOffStreamResolution(ActionResultDTO actionResult, OffStreamResolution resolution) {
        Map<String, Object> evidenceRef = new LinkedHashMap<>(actionResult.evidenceRef() == null
                ? Map.of()
                : actionResult.evidenceRef());
        evidenceRef.put("offStreamAction", offStreamEvidence(resolution));
        OffStreamImpact impact = resolution.impact();
        return new ActionResultDTO(
                actionResult.actionType(),
                actionResult.selectedTitle(),
                actionResult.summary() + " " + resolution.summary(),
                actionResult.fanChange() + fanDelta(impact),
                actionResult.trueFanChange() + impact.trueFanDelta(),
                actionResult.funFanChange() + impact.funFanDelta(),
                actionResult.unicornFanChange() + impact.unicornFanDelta(),
                actionResult.ddFanChange() + impact.ddFanDelta(),
                actionResult.staminaChange(),
                actionResult.attributeChange() + attributeDelta(impact),
                actionResult.routeScoreChange(),
                actionResult.debtCreated(),
                evidenceRef,
                actionResult.fatigueInfo()
        );
    }

    private void applyOffStreamLogImpact(BusinessLog log, ActionResultDTO actionResult) {
        Object raw = actionResult.evidenceRef() == null ? null : actionResult.evidenceRef().get("offStreamAction");
        if (!(raw instanceof Map<?, ?> evidence)) {
            return;
        }
        int fanDelta = intFromEvidence(evidence, "fanDelta");
        Object summary = evidence.get("summary");
        log.setResult(log.getResult() + " " + (summary == null ? "" : String.valueOf(summary)));
        log.setRawFanGain(log.getRawFanGain() + fanDelta);
        log.setFinalFanGain(log.getFinalFanGain() + fanDelta);
        log.setFanChange(log.getFanChange() + fanDelta);
        log.setTrueFanChange(log.getTrueFanChange() + intFromEvidence(evidence, "trueFanDelta"));
        log.setFunFanChange(log.getFunFanChange() + intFromEvidence(evidence, "funFanDelta"));
        log.setUnicornFanChange(log.getUnicornFanChange() + intFromEvidence(evidence, "unicornFanDelta"));
        log.setDdFanChange(log.getDdFanChange() + intFromEvidence(evidence, "ddFanDelta"));
        log.setPopularityChange(log.getPopularityChange() + intFromEvidence(evidence, "popularityDelta"));
        log.setWatchHeatChange(log.getWatchHeatChange() + intFromEvidence(evidence, "watchHeatDelta"));
        log.setReputationChange(log.getReputationChange() + intFromEvidence(evidence, "reputationDelta"));
        log.setMemeChange(log.getMemeChange() + intFromEvidence(evidence, "memeDelta"));
        log.setCommercialChange(log.getCommercialChange() + intFromEvidence(evidence, "commercialDelta"));
        log.setCoinChange(log.getCoinChange() + intFromEvidence(evidence, "coinDelta"));
        log.setInspirationChange(log.getInspirationChange() + intFromEvidence(evidence, "inspirationDelta"));
    }

    private List<Map<String, Object>> offStreamEvidenceRefs(ActionResultDTO actionResult) {
        Object raw = actionResult.evidenceRef() == null ? null : actionResult.evidenceRef().get("offStreamAction");
        if (raw instanceof Map<?, ?> evidence) {
            Map<String, Object> ref = new LinkedHashMap<>();
            evidence.forEach((key, value) -> {
                if (key instanceof String textKey && value != null) {
                    ref.put(textKey, value);
                }
            });
            return List.of(ref);
        }
        return List.of();
    }

    private Map<String, Object> offStreamEvidence(OffStreamResolution resolution) {
        OffStreamImpact impact = resolution.impact();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("type", "off_stream_action");
        evidence.put("offStreamType", resolution.type());
        evidence.put("name", resolution.name());
        evidence.put("summary", resolution.summary());
        evidence.put("benefitPreview", resolution.benefitPreview());
        evidence.put("costPreview", resolution.costPreview());
        evidence.put("bestFor", resolution.bestFor());
        evidence.put("riskPreview", resolution.riskPreview());
        evidence.put("recommendedReason", resolution.recommendedReason());
        evidence.put("routeBias", resolution.routeBias());
        evidence.put("recommended", resolution.recommended());
        evidence.put("songPowerDelta", impact.songPowerDelta());
        evidence.put("dancePowerDelta", impact.dancePowerDelta());
        evidence.put("talkPowerDelta", impact.talkPowerDelta());
        evidence.put("popularityDelta", impact.popularityDelta());
        evidence.put("watchHeatDelta", impact.watchHeatDelta());
        evidence.put("reputationDelta", impact.reputationDelta());
        evidence.put("memeDelta", impact.memeDelta());
        evidence.put("commercialDelta", impact.commercialDelta());
        evidence.put("coinDelta", impact.coinDelta());
        evidence.put("inspirationDelta", impact.inspirationDelta());
        evidence.put("trueFanDelta", impact.trueFanDelta());
        evidence.put("funFanDelta", impact.funFanDelta());
        evidence.put("unicornFanDelta", impact.unicornFanDelta());
        evidence.put("ddFanDelta", impact.ddFanDelta());
        evidence.put("fanDelta", fanDelta(impact));
        evidence.put("routeScoreDelta", impact.routeScoreDelta());
        return evidence;
    }

    private int fanDelta(OffStreamImpact impact) {
        return impact.trueFanDelta() + impact.funFanDelta() + impact.unicornFanDelta() + impact.ddFanDelta();
    }

    private int attributeDelta(OffStreamImpact impact) {
        return impact.songPowerDelta() + impact.dancePowerDelta() + impact.talkPowerDelta();
    }

    private String effectiveRoute(Vup vup) {
        String currentRoute = vup.getCurrentRoute();
        return (currentRoute == null || currentRoute.isBlank()) ? RouteType.UNKNOWN.name() : currentRoute;
    }

    DayResultDTO pauseForDueDebtIfNeeded(
            Long userId,
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            String idempotencyKey,
            String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        RiskDebt dueDebt = debtService.dueDebt(vup, session.getDay());
        if (dueDebt == null) {
            return null;
        }
        session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        session.setPendingFormalEventId(dueDebt.getId());
        daySessionMapper.updateAfterAction(session);

        DayResultDTO result = new DayResultDTO(
                session.getPhase(),
                actionResult,
                null,
                formalEventPresenter.pending(vup, session),
                null,
                false,
                null,
                null
        );
        return result;
    }

    DayResultDTO pauseForFanTopicEscalationIfNeeded(
            Long userId,
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            String idempotencyKey,
            String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        BusinessLog fanTopicLog = businessLogMapper.findLatestFanTopicByVupIdAndDay(vup.getId(), session.getDay());
        if (!shouldTriggerFanTopicEscalation(vup, session, fanTopicLog)) {
            return null;
        }
        RiskDebt escalatedDebt = debtService.escalatableOpenDebt(vup, session.getDay());
        if (escalatedDebt == null) {
            return null;
        }

        session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        session.setPendingFormalEventId(escalatedDebt.getId());
        session.setFormalEventSlotStatus("RESERVED");
        session.setFormalEventSource("FAN_TOPIC_ESCALATION");
        session.setFormalEventPriority(80);
        session.setFormalEventRollDetailJson(jsonService.write(Map.of(
                "rollType", "fan_topic_escalation",
                "eventKey", "FAN_TOPIC_ESCALATION",
                "source", "fan_topic",
                "trigger", fanTopicLog.getFanGroupTopicId() + ":" + fanTopicChoiceType(fanTopicLog),
                "fanGroupTopicId", fanTopicLog.getFanGroupTopicId(),
                "choiceType", fanTopicChoiceType(fanTopicLog),
                "watchHeat", vup.getWatchHeat(),
                "targetDebtId", escalatedDebt.getId(),
                "targetDebtType", escalatedDebt.getDebtType()
        )));
        daySessionMapper.updateAfterAction(session);

        DayResultDTO result = new DayResultDTO(
                session.getPhase(),
                actionResult,
                null,
                formalEventPresenter.pending(vup, session),
                null,
                false,
                null,
                null
        );
        return result;
    }

    private DayResultDTO pauseForOrdinaryEventIfNeeded(
            Long userId,
            Vup vup,
            DaySession session,
            ActionResultDTO actionResult,
            String idempotencyKey,
            String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        if (shouldTriggerRestSavedMeltdown(vup, session, actionResult)) {
            session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
            session.setFormalEventSlotStatus("RESERVED");
            session.setFormalEventSource("REST_SAVED_MELTDOWN");
            session.setFormalEventPriority(20);
            session.setFormalEventRollDetailJson(jsonService.write(Map.of(
                    "rollType", "ordinary_event_roll",
                    "eventKey", "REST_SAVED_MELTDOWN",
                    "eventTitle", "低压运营救场",
                    "eventDescription", "连续低压休息把一次潜在翻车熬成了无事发生。",
                    "eventType", "ORDINARY_EVENT",
                    "source", "ordinary",
                    "trigger", "CONSECUTIVE_REST_DAY_3",
                    "eventCandidates", List.of(Map.of(
                            "eventKey", "REST_SAVED_MELTDOWN",
                            "eventType", "ORDINARY_EVENT",
                            "sourceAction", ActionType.REST.name(),
                            "trigger", "CONSECUTIVE_REST_DAY_3",
                            "priority", 20
                    )),
                    "weights", Map.of("REST_SAVED_MELTDOWN", 1),
                    "hitEventKey", "REST_SAVED_MELTDOWN"
            )));
            daySessionMapper.updateAfterAction(session);

            return new DayResultDTO(
                    session.getPhase(),
                    actionResult,
                    null,
                    formalEventPresenter.pending(vup, session),
                    null,
                    false,
                    null,
                    null
            );
        }

        if (!shouldKeepRouteActionUninterrupted(vup, session, actionResult)) {
            DayResultDTO midgameEvent = pauseForMidgameEventIfNeeded(vup, session, actionResult);
            if (midgameEvent != null) {
                return midgameEvent;
            }
            DayResultDTO lateGameEvent = pauseForLateGameEventIfNeeded(vup, session, actionResult);
            if (lateGameEvent != null) {
                return lateGameEvent;
            }
        }

        RandomEventDTO randomEvent = rollRandomOrdinaryEvent(vup, session, actionResult);
        if (randomEvent == null) {
            return null;
        }

        session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        session.setFormalEventSlotStatus("RESERVED");
        session.setFormalEventSource("RANDOM_EVENT");
        session.setFormalEventPriority(30);
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(
                session.getPhase(),
                actionResult,
                null,
                formalEventPresenter.pending(vup, session),
                null,
                false,
                null,
                null
        );
    }

    DayResultDTO pauseForRivalOvertakeIfNeeded(Vup vup, DaySession session, ActionResultDTO actionResult) {
        com.example.vupworld.dto.NpcDtos.RivalProgressDTO progress = rivalProgressService.progress(vup);
        if (progress == null || !progress.overtaken()) {
            return null;
        }
        com.example.vupworld.dto.NpcDtos.RivalDTO topRival = progress.rivals().stream()
                .max(java.util.Comparator.comparingInt(com.example.vupworld.dto.NpcDtos.RivalDTO::fans))
                .orElse(null);
        if (topRival == null || topRival.fans() <= vup.getFans()) {
            return null;
        }
        session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
        session.setFormalEventSlotStatus("RESERVED");
        session.setFormalEventSource(rivalOvertakeEvent.SOURCE);
        session.setFormalEventPriority(25);
        session.setFormalEventRollDetailJson(rivalOvertakeEvent.buildRollDetailJson(vup, topRival));
        daySessionMapper.updateAfterAction(session);

        return new DayResultDTO(
                session.getPhase(),
                actionResult,
                null,
                rivalOvertakeEvent.toPendingEvent(session),
                null,
                false,
                null,
                null
        );
    }

    DayResultDTO pauseForMidgameEventIfNeeded(Vup vup, DaySession session, ActionResultDTO actionResult) {
        List<BusinessLog> recentLogs = businessLogMapper.findByVupIdBetweenDays(
                vup.getId(),
                Math.max(1, session.getDay() - 20),
                session.getDay()
        );
        return midgameEventContentService.selectFor(vup, session, recentLogs)
                .map(selected -> {
                    session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
                    session.setFormalEventSlotStatus("RESERVED");
                    session.setFormalEventSource("MIDGAME_EVENT");
                    session.setFormalEventPriority(40);
                    session.setFormalEventRollDetailJson(jsonService.write(
                            midgameEventContentService.toRollDetail(selected, actionResult.actionType())
                    ));
                    daySessionMapper.updateAfterAction(session);
                    return new DayResultDTO(
                            session.getPhase(),
                            actionResult,
                            null,
                            formalEventPresenter.pending(vup, session),
                            null,
                            false,
                            null,
                            null
                    );
                })
                .orElse(null);
    }

    DayResultDTO pauseForLateGameEventIfNeeded(Vup vup, DaySession session, ActionResultDTO actionResult) {
        List<BusinessLog> recentLogs = businessLogMapper.findByVupIdBetweenDays(
                vup.getId(),
                Math.max(1, session.getDay() - 20),
                session.getDay()
        );
        return commercialRouteContentService.selectLateGameFor(vup, session, recentLogs)
                .map(selected -> {
                    session.setPhase(DayPhase.NEED_EVENT_CHOICE.name());
                    session.setFormalEventSlotStatus("RESERVED");
                    session.setFormalEventSource("LATE_GAME_EVENT");
                    session.setFormalEventPriority(45);
                    session.setFormalEventRollDetailJson(jsonService.write(
                            commercialRouteContentService.toLateGameRollDetail(selected, actionResult.actionType())
                    ));
                    daySessionMapper.updateAfterAction(session);
                    return new DayResultDTO(
                            session.getPhase(),
                            actionResult,
                            null,
                            formalEventPresenter.pending(vup, session),
                            null,
                            false,
                            null,
                            null
                    );
                })
                .orElse(null);
    }

    private RandomEventDTO rollRandomOrdinaryEvent(Vup vup, DaySession session, ActionResultDTO actionResult) {
        if (session.getDay() <= 7 || ActionType.REST.name().equals(actionResult.actionType())
                || shouldKeepRouteActionUninterrupted(vup, session, actionResult)) {
            return null;
        }
        int threshold = Math.max(5, randomEventService.triggerThresholdFor(
                session.getDay(),
                vup.getWatchHeat(),
                vup.getMemeLevel()
        ) - 10);

        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            int roll = ledger.nextInt("ordinary-random-event-trigger", 100);
            List<RandomEventDTO> candidates = randomEventService.candidateEventsFor(
                    session.getDay(),
                    vup.getWatchHeat(),
                    vup.getMemeLevel()
            );
            if (roll >= threshold || candidates.isEmpty()) {
                Map<String, Object> rollDetail = new LinkedHashMap<>();
                rollDetail.put("rollType", "ordinary_random_event_roll");
                rollDetail.put("source", "random_event_pool");
                rollDetail.put("triggerRoll", roll);
                rollDetail.put("threshold", threshold);
                rollDetail.put("hit", false);
                rollDetail.put("actionType", actionResult.actionType());
                rollDetail.put("candidateBiases", randomEventService.candidateBiasesFor(session.getDay(), vup.getWatchHeat(), vup.getMemeLevel()));
                session.setFormalEventRollDetailJson(jsonService.write(rollDetail));
                return null;
            }

            RandomEventDTO hit = candidates.get(ledger.nextInt("ordinary-random-event-pick", candidates.size()));
            Map<String, Object> rollDetail = new LinkedHashMap<>();
            rollDetail.put("rollType", "ordinary_random_event_roll");
            rollDetail.put("source", "random_event_pool");
            rollDetail.put("triggerRoll", roll);
            rollDetail.put("threshold", threshold);
            rollDetail.put("hit", true);
            rollDetail.put("eventKey", hit.id());
            rollDetail.put("hitEventKey", hit.id());
            rollDetail.put("eventTitle", hit.title());
            rollDetail.put("eventDescription", hit.description());
            rollDetail.put("eventEffect", hit.effect());
            rollDetail.put("eventType", hit.eventType());
            rollDetail.put("actionType", actionResult.actionType());
            rollDetail.put("candidateBiases", randomEventService.candidateBiasesFor(session.getDay(), vup.getWatchHeat(), vup.getMemeLevel()));
            session.setFormalEventRollDetailJson(jsonService.write(rollDetail));
            return hit;
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    private boolean shouldTriggerRestSavedMeltdown(Vup vup, DaySession session, ActionResultDTO actionResult) {
        if (session.getDay() != 3 || !ActionType.REST.name().equals(actionResult.actionType())) {
            return false;
        }
        BusinessLog previous = businessLogMapper.findLatestByVupIdAndDay(vup.getId(), session.getDay() - 1);
        return previous != null && ActionType.REST.name().equals(previous.getAction());
    }

    private boolean shouldKeepRouteActionUninterrupted(Vup vup, DaySession session, ActionResultDTO actionResult) {
        if (actionResult == null || actionResult.actionType() == null) {
            return false;
        }
        String actionType = actionResult.actionType();
        if (ActionType.TRAIN_DANCE.name().equals(actionType)
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(actionType)) {
            return true;
        }
        if (ActionType.NPC_INTERACT.name().equals(actionType)
                && vup.getWatchHeat() < 70
                && session.getDay() < 30) {
            return true;
        }
        return false;
    }

    private boolean shouldTriggerFanTopicEscalation(Vup vup, DaySession session, BusinessLog fanTopicLog) {
        if (fanTopicLog == null || fanTopicLog.getFanGroupTopicId() == null) {
            return false;
        }
        if (!"OBSERVE".equals(fanTopicChoiceType(fanTopicLog))) {
            return false;
        }
        return vup.getWatchHeat() >= 70 && debtService.escalatableOpenDebt(vup, session.getDay()) != null;
    }

    private String fanTopicChoiceType(BusinessLog fanTopicLog) {
        if (fanTopicLog.getWeightDetail() == null || fanTopicLog.getWeightDetail().isBlank()) {
            return "";
        }
        Object value = jsonService.readMap(fanTopicLog.getWeightDetail()).get("choiceType");
        return value == null ? "" : String.valueOf(value);
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
                .anyMatch(log -> ActionType.PUBLISH_VIDEO.name().equals(log.getAction())
                        && log.getDay() < session.getDay()
                        && session.getDay() - log.getDay() <= 3);
    }

    private boolean videoToClipComboActive(Vup vup, int currentDay) {
        if (currentDay <= 1) {
            return false;
        }
        List<BusinessLog> recentLogs = businessLogMapper.findRecentByVupId(vup.getId(), 10);
        return recentLogs.stream()
                .filter(log -> ActionType.PUBLISH_VIDEO.name().equals(log.getAction()))
                .anyMatch(video -> video.getDay() < currentDay
                        && currentDay - video.getDay() <= 7
                        && recentLogs.stream().noneMatch(log -> ActionType.PUBLISH_CLIP.name().equals(log.getAction())
                        && log.getDay() > video.getDay()
                        && log.getDay() < currentDay));
    }

    ComboImpact comboImpactFor(Vup vup, DaySession session, ActionType actionType, String routeType) {
        List<BusinessLog> logs = new ArrayList<>(businessLogMapper.findRecentByVupId(vup.getId(), 30));
        logs.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId));
        int currentDay = session.getDay();
        boolean training = isTrainingAction(actionType.name());

        if (training && recentActionCount(logs, currentDay, 6,
                ActionType.TRAIN_SONG.name(), ActionType.TRAIN_DANCE.name(), ActionType.TRAIN_TALK.name()) >= 2) {
            return new ComboImpact(
                    true,
                    "HARD_PRACTICE",
                    "刻苦练习",
                    routeType,
                    3,
                    0,
                    0,
                    4,
                    0,
                    1,
                    0,
                    1,
                    "一周内多次练功形成了可复用基本功，路线证据更硬。",
                    "路线+1"
            );
        }
        if (actionType == ActionType.PUBLISH_CLIP && videoToClipComboActive(vup, currentDay)) {
            return new ComboImpact(
                    true,
                    "VIDEO_TO_CLIP",
                    "一鱼两剪",
                    routeType,
                    0,
                    0,
                    0,
                    4,
                    1,
                    0,
                    1,
                    1,
                    "长视频素材被二剪继续榨出传播价值，切片供货链更稳定。",
                    "路线+1"
            );
        }
        if (actionType != ActionType.REST && consecutivePreviousActions(logs, currentDay, ActionType.REST.name(), 2)) {
            return new ComboImpact(
                    true,
                    "CHARGE_COMPLETE",
                    "充电完毕",
                    routeType,
                    3,
                    0,
                    0,
                    4,
                    -2,
                    1,
                    0,
                    0,
                    "连续低压收工后再营业，主播状态和直播间气压一起回来了。",
                    "粉丝+3"
            );
        }
        if (actionType == ActionType.REST && recentActionCount(logs, currentDay, 6, ActionType.REST.name()) >= 3) {
            return new ComboImpact(
                    true,
                    "FORGOTTEN",
                    "被遗忘",
                    routeType,
                    0,
                    0,
                    0,
                    -6,
                    -1,
                    1,
                    0,
                    0,
                    "连续低压太久，老粉还在，首页推荐位开始装作不认识你。",
                    "人气-6"
            );
        }
        if (actionType == ActionType.STREAM_PLAN && recentActionCount(logs, currentDay, 6, ActionType.STREAM_PLAN.name()) >= 2) {
            return new ComboImpact(
                    true,
                    "OVERWORK",
                    "过劳边缘",
                    routeType,
                    0,
                    0,
                    0,
                    -4,
                    4,
                    -2,
                    0,
                    0,
                    "最近直播太密，观众习惯了高频营业，主播身体和口碑先开始报警。",
                    "压力+4"
            );
        }
        if (actionType == ActionType.STREAM_PLAN && recentActionCount(logs, currentDay, 7, ActionType.NPC_INTERACT.name()) >= 1) {
            return new ComboImpact(
                    true,
                    "SOCIAL_WARMUP",
                    "查房预热",
                    routeType,
                    0,
                    0,
                    4,
                    8,
                    1,
                    0,
                    0,
                    1,
                    "刚在同行场子露过脸，开播更像有人把门推开。",
                    "DD+4"
            );
        }
        return ComboImpact.empty();
    }

    Map<String, Object> comboImpactEvidence(ComboImpact impact) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("comboKey", impact.comboKey());
        evidence.put("label", impact.label());
        evidence.put("targetRoute", impact.targetRoute());
        evidence.put("trueFanBonus", impact.trueFanBonus());
        evidence.put("funFanBonus", impact.funFanBonus());
        evidence.put("ddFanBonus", impact.ddFanBonus());
        evidence.put("popularityBonus", impact.popularityBonus());
        evidence.put("watchHeatBonus", impact.watchHeatBonus());
        evidence.put("reputationBonus", impact.reputationBonus());
        evidence.put("memeBonus", impact.memeBonus());
        evidence.put("routeScoreBonus", impact.routeScoreBonus());
        evidence.put("primaryDeltaLabel", impact.primaryDeltaLabel());
        evidence.put("hint", impact.hint());
        return evidence;
    }

    private int recentActionCount(List<BusinessLog> logs, int currentDay, int windowDays, String... actions) {
        List<String> actionList = List.of(actions);
        int count = 0;
        for (BusinessLog log : logs) {
            if (log.getDay() >= currentDay || currentDay - log.getDay() > windowDays) {
                continue;
            }
            if (actionList.contains(log.getAction())) {
                count++;
            }
        }
        return count;
    }

    private boolean consecutivePreviousActions(List<BusinessLog> logs, int currentDay, String action, int count) {
        int expectedDay = currentDay - 1;
        int matched = 0;
        for (int i = logs.size() - 1; i >= 0 && matched < count; i--) {
            BusinessLog log = logs.get(i);
            if (log.getDay() > expectedDay) {
                continue;
            }
            if (log.getDay() < expectedDay) {
                return false;
            }
            if (!action.equals(log.getAction())) {
                return false;
            }
            matched++;
            expectedDay--;
        }
        return matched >= count;
    }

    private void validateNpcInteractionCooldown(Vup vup, DaySession session, String npcTendency) {
        if (!NPC_TENDENCY_COLLAB.equals(npcTendency)) {
            return;
        }
        businessLogMapper.findRecentByVupId(vup.getId(), 10).stream()
                .filter(log -> ActionType.NPC_INTERACT.name().equals(log.getAction()))
                .filter(log -> NPC_TENDENCY_COLLAB.equals(npcTendencyFor(log)))
                .filter(log -> log.getDay() < session.getDay())
                .filter(log -> session.getDay() - log.getDay() < NPC_COLLAB_COOLDOWN_DAYS)
                .findFirst()
                .ifPresent(log -> {
                    int nextDay = log.getDay() + NPC_COLLAB_COOLDOWN_DAYS;
                    throw new GameException(
                            "NPC_INTERACTION_COOLDOWN",
                            "轻联动还在冷却，至少等到第" + nextDay + "天再排同框。"
                    );
                });
    }

    private String npcTendencyFor(BusinessLog log) {
        if (log.getWeightDetail() == null || log.getWeightDetail().isBlank()) {
            return "";
        }
        Object value = jsonService.readMap(log.getWeightDetail()).get("npcTendency");
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isTrainingAction(String actionType) {
        return ActionType.TRAIN_SONG.name().equals(actionType)
                || ActionType.TRAIN_DANCE.name().equals(actionType)
                || ActionType.TRAIN_TALK.name().equals(actionType);
    }

    private DayResultDTO doStartStreamPlan(
            Vup vup,
            DaySession session,
            SubmitActionRequest request,
            String idempotencyKey,
            String requestHash,
            BiFunction<String, DayResultDTO, ApiIdempotencyRecord> buildRecord
    ) {
        String planType = requirePlanType(request.planType());
        Long planId = titleService.planIdFor(planType);
        session.setPhase(DayPhase.NEED_TITLE.name());
        session.setSelectedAction(ActionType.STREAM_PLAN.name());
        session.setSelectedPlanId(planId);
        List<TitleOptionDTO> titleCandidates = titleService.titleCandidatesFor(vup, session, planType);
        session.setTitleCandidatesJson(jsonService.write(titleCandidates));
        session.setTitleRerollCount(0);
        daySessionMapper.updateAfterAction(session);

        DayResultDTO result = new DayResultDTO(
                session.getPhase(),
                null,
                null,
                null,
                false,
                planId,
                titleCandidates
        );
        return result;
    }

    private int attributeChangeFor(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_SONG, TRAIN_DANCE, TRAIN_TALK, PUBLISH_VIDEO -> 1;
            default -> 0;
        };
    }

    BusinessLog toBusinessLog(Vup vup, DaySession session, ActionResultDTO result, String idempotencyKey) {
        BusinessLog log = new BusinessLog();
        log.setVupId(vup.getId());
        log.setDaySessionId(session.getId());
        log.setDay(session.getDay());
        log.setPhase(DayPhase.ACTION_RESOLVED.name());
        log.setIdempotencyKey(idempotencyKey);
        log.setAction(result.actionType());
        log.setMemeSubtype(memeSubtypeFor(result));
        log.setPlanId(session.getSelectedPlanId());
        log.setResult(result.summary());
        log.setRawFanGain(result.fanChange());
        log.setFinalFanGain(result.fanChange());
        log.setFanChange(result.fanChange());
        log.setTrueFanChange(result.trueFanChange());
        log.setFunFanChange(result.funFanChange());
        log.setUnicornFanChange(result.unicornFanChange());
        log.setDdFanChange(result.ddFanChange());
        ActionLogDelta delta = actionLogDeltaWithEvidence(result, logDeltaFor(result, session.getDay()));
        log.setPopularityChange(delta.popularityChange());
        log.setWatchHeatChange(delta.watchHeatChange());
        log.setReputationChange(delta.reputationChange());
        log.setMemeChange(delta.memeChange());
        log.setCommercialChange(commercialChangeFor(result));
        log.setCoinChange(coinChangeFor(result));
        log.setInspirationChange(inspirationChangeFor(result));
        log.setMultiplierDetail(jsonService.write(multiplierDetailFor(result, delta)));
        log.setCapDetail(jsonService.write(Map.of(
                "source", "BalanceConfig",
                "stageCap", stageFanCap(vup.getFans()),
                "hit", result.fanChange() >= stageFanCap(vup.getFans())
        )));
        log.setClampDetail(jsonService.write(Map.of("reputation", vup.getReputation(), "songPower", vup.getSongPower())));
        log.setWeightDetail(jsonService.write(weightDetailFor(result)));
        log.setRngDetail(jsonService.write(Map.of(
                "seed", session.getRandomSeed(),
                "cursor", session.getRngCursor(),
                "hit", result.actionType()
        )));
        log.setExpectationChange("{}");
        log.setRouteScoreChange(jsonService.write(Map.of(routeScoreKey(result), result.routeScoreChange(), "source", result.actionType())));
        log.setDebtIds("[]");
        log.setAccidentMaterialIds("[]");
        log.setEndingRefFlag(true);
        return log;
    }

    private ActionResultDTO resolveNonStreamAction(Vup vup, ActionConfig config, String npcTendency) {
        ActionType actionType = config.actionType;
        int day = vup.getDayCount();
        String sprintAnchorRoute = vup.getCurrentRoute();

        // --- combo checks (before modifying Vup state) ---
        DaySession session = dayFlowService.requireCurrentSession(vup);
        boolean practiceToStreamCombo = practiceToStreamComboActive(vup, session);
        boolean videoToStreamCombo = videoToStreamComboActive(vup, session);
        boolean videoToClipCombo = videoToClipComboActive(vup, session.getDay());
        boolean singingBoostWeek = isSingingBoostWeek(day);
        boolean commercialReviewWeek = isCommercialReviewWeek(day);
        boolean memeOutbreakWeek = day >= 15 && day <= 21;
        boolean safeWeek = day <= 7;
        RotationBonus rotationBonus = rotationBonusFor(vup, session, actionType);
        FortuneModifier fortuneModifier = fortuneModifierFor(vup, session, actionType, isHighRiskAction(actionType));
        StageObjectiveSnapshot stageObjectiveSnapshot = stageObjectiveSnapshot(vup, session);

        // --- action fatigue check ---
        com.example.vupworld.dto.ActionDtos.FatigueInfoDTO fatigueInfo = actionFatigueService.calculateFatigue(vup.getId(), actionType.name());
        int fatiguePercent = fatigueInfo.fatiguePercent();

        int trueFanChange = 0;
        int funFanChange = 0;
        int unicornFanChange = 0;
        int ddFanChange = 0;
        int popularityChange = 0;
        int watchHeatChange = 0;
        int reputationChange = 0;
        int memeChange = 0;
        int commercialChange = 0;
        int coinChange = 0;
        int inspirationChange = 0;
        int staminaChange = 0;
        int routeScoreChange = 0;
        String routeType = null;
        String summary = "";
        boolean blockFunFanBonuses = false;

        switch (actionType) {
            case TRAIN_SONG -> {
                trueFanChange = singingBoostWeek ? 14 : 10;
                ddFanChange = 3;
                popularityChange = singingBoostWeek ? 20 : 15;
                reputationChange = singingBoostWeek ? 2 : 1;
                routeScoreChange = 2;
                routeType = "SINGING_IDOL";
                staminaChange = -2;
                vup.setSongPower(Math.min(100, vup.getSongPower() + 1));
                vup.setStamina(vup.getStamina() - 2);
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setDdFans(vup.getDdFans() + ddFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                if (practiceToStreamCombo) {
                    trueFanChange += 8;
                    vup.setTrueFans(vup.getTrueFans() + 8);
                    vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                }
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
                summary = singingBoostWeek
                        ? "你练了一天歌，歌回扶持周把基本功推上推荐边缘，老粉觉得有在补课，DD顺手坐了一站。"
                        : "你练了一天歌，老粉觉得基本功有在补，DD顺手坐了一站。";
            }
            case TRAIN_DANCE -> {
                trueFanChange = 6;
                funFanChange = 5;
                ddFanChange = 2;
                popularityChange = 18;
                watchHeatChange = 2;
                memeChange = 2;
                routeScoreChange = 4;
                routeType = "DANCE_MEME";
                staminaChange = -2;
                vup.setDancePower(Math.min(100, vup.getDancePower() + 1));
                vup.setStamina(vup.getStamina() - 2);
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFunFans(vup.getFunFans() + funFanChange);
                vup.setDdFans(vup.getDdFans() + ddFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setWatchHeat(clamp(vup.getWatchHeat() + watchHeatChange, 0, 100));
                vup.setMemeLevel(clamp(vup.getMemeLevel() + memeChange, 0, 100));
                if (practiceToStreamCombo) {
                    trueFanChange += 8;
                    vup.setTrueFans(vup.getTrueFans() + 8);
                    vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                }
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
                summary = "你练了一天梗舞，动作还没到鬼畜区，但乐子人已经开始预定逐帧鉴赏。";
            }
            case TRAIN_TALK -> {
                trueFanChange = safeWeek ? 6 : 4;
                popularityChange = 5;
                reputationChange = safeWeek ? 3 : 2;
                inspirationChange = 1;
                routeScoreChange = 2;
                routeType = "ELECTRONIC_PICKLE";
                staminaChange = -2;
                vup.setTalkPower(Math.min(100, vup.getTalkPower() + 1));
                vup.setStamina(vup.getStamina() - 2);
                vup.setInspiration(Math.max(0, vup.getInspiration() + 1));
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
                summary = safeWeek
                        ? "你做了一场杂谈复盘，灵感+1，清朗低压周给了低压内容一点平台口味加成，标题组终于从空白文档里挤出一点活。"
                        : "你做了一场杂谈复盘，灵感+1，标题组终于从空白文档里挤出一点活。";
            }
            case PUBLISH_VIDEO -> {
                trueFanChange = singingBoostWeek ? 10 : 8;
                funFanChange = 9;
                ddFanChange = singingBoostWeek ? 6 : 3;
                popularityChange = singingBoostWeek ? 42 : 35;
                watchHeatChange = 4;
                memeChange = 1;
                reputationChange = 1;
                inspirationChange = -1;
                routeScoreChange = 2;
                routeType = "SLICE_SAINT";
                staminaChange = -4;
                vup.setPlanPower(Math.min(100, vup.getPlanPower() + 1));
                vup.setStamina(vup.getStamina() - 4);
                vup.setInspiration(Math.max(0, vup.getInspiration() - 1));
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFunFans(vup.getFunFans() + funFanChange);
                vup.setDdFans(vup.getDdFans() + ddFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setWatchHeat(clamp(vup.getWatchHeat() + watchHeatChange, 0, 100));
                vup.setMemeLevel(clamp(vup.getMemeLevel() + memeChange, 0, 100));
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                if (practiceToStreamCombo) {
                    trueFanChange += 8;
                    vup.setTrueFans(vup.getTrueFans() + 8);
                    vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                }
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
                summary = singingBoostWeek
                        ? "你发布视频撞上歌回扶持周，投稿更容易进推荐，评论区开始问直播时间，切片组把这条当成引流素材存档。"
                        : "你发布视频蹭到一点平台口味，评论区开始问直播时间，切片组把这条当成引流素材存档。";
            }
            case PUBLISH_CLIP -> {
                MemeFatigueState fatigue = calculateMemeFatigue(vup.getId(), day);
                boolean hasVideoCombo = videoToClipCombo;
                if (hasVideoCombo) {
                    funFanChange = memeOutbreakWeek ? 26 : 20;
                    ddFanChange = memeOutbreakWeek ? 4 : 3;
                    funFanChange += 7;
                    popularityChange = memeOutbreakWeek ? 48 : 40;
                    watchHeatChange = memeOutbreakWeek ? 8 : 5;
                    memeChange = memeOutbreakWeek ? 7 : 5;
                    reputationChange = memeOutbreakWeek ? -2 : -1;
                    summary = memeOutbreakWeek
                            ? "你发了一条切片正好撞上抽象出圈周，乐子人进场更快，切片组开始逐帧标注笑点，DD也顺手坐了一站，但米线压力跟着亮灯。"
                            : "你发了一条切片，一鱼两剪让切片组和素材档同时开工，乐子人进场找素材，DD也顺手坐了一站。";
                } else {
                    funFanChange = memeOutbreakWeek ? 26 : 20;
                    ddFanChange = memeOutbreakWeek ? 4 : 3;
                    popularityChange = memeOutbreakWeek ? 36 : 28;
                    watchHeatChange = memeOutbreakWeek ? 6 : 3;
                    memeChange = memeOutbreakWeek ? 5 : 2;
                    reputationChange = memeOutbreakWeek ? -2 : -1;
                    summary = memeOutbreakWeek
                            ? "你发了一条切片正好撞上抽象出圈周，乐子人进场更快，切片组开始逐帧标注笑点，DD也顺手坐了一站，但米线压力跟着亮灯。"
                            : "你发了一条切片，切片组开始排班，乐子人进场找素材，DD也顺手坐了一站。";
                }

                // Apply meme fatigue scaling to fun fans
                funFanChange = fatigue.apply(funFanChange);
                funFanChange = Math.max(0, funFanChange);

                // Boomerang: no main fan gain, only watchHeat and meme
                if (fatigue == MemeFatigueState.BOOMERANG) {
                    funFanChange = 0;
                    blockFunFanBonuses = true;
                    summary = "你发了一条切片，但这梗已经进入回旋期，乐子人已经看过太多遍，切片组开始写复盘而不是剪新素材。围观热度还在，但转化几乎为零。";
                }
                // Heavy repeat: reduced gains
                if (fatigue == MemeFatigueState.HEAVY_REPEAT) {
                    summary = memeOutbreakWeek
                            ? "你发了一条切片撞上抽象出圈周，但这梗已经复读过了，乐子人进场速度比第一次慢，切片组开始标注重复笑点。"
                            : "你发了一条切片，但这梗已经复读过了，乐子人觉得没有新意，切片组调了一下时间轴就收工了。";
                }
                // Repeat: still some gains but note the repeat
                if (fatigue == MemeFatigueState.REPEAT) {
                    summary = memeOutbreakWeek
                            ? "你发了一条切片撞上抽象出圈周，但这个梗已经有人发过了，乐子人进场速度比第一次慢，切片组开始标注重复笑点。"
                            : "你发了一条切片，但这梗已经有复读的趋势，乐子人觉得还算新鲜但切片组已经开始标注重复笑点了。";
                }

                int totalFanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
                staminaChange = -1;
                vup.setStamina(vup.getStamina() - 1);
                vup.setFunFans(vup.getFunFans() + funFanChange);
                vup.setDdFans(vup.getDdFans() + ddFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setWatchHeat(clamp(vup.getWatchHeat() + watchHeatChange, 0, 100));
                vup.setMemeLevel(clamp(vup.getMemeLevel() + memeChange, 0, 100));
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                routeScoreChange = 2;
                routeType = "SLICE_SAINT";
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
            }
            case NPC_INTERACT -> {
                String tendency = npcTendency == null ? NPC_TENDENCY_RAID : npcTendency;
                trueFanChange = 2;
                routeScoreChange = 2;
                routeType = "SOCIAL_COLLAB";
                staminaChange = -3;
                vup.setStamina(vup.getStamina() - 3);
                switch (tendency) {
                    case NPC_TENDENCY_COLLAB -> {
                        ddFanChange = 18;
                        popularityChange = 10;
                        watchHeatChange = 5;
                        reputationChange = 2;
                        routeScoreChange = 5;
                        routeType = "SOCIAL_COLLAB";
                        summary = "你安排了一次轻联动，把低压同框做成可复盘节目，DD多坐了一站，老粉开始观察营业边界。";
                    }
                    case NPC_TENDENCY_BORROW_HEAT -> {
                        funFanChange = 8;
                        ddFanChange = 16;
                        popularityChange = 15;
                        watchHeatChange = 12;
                        reputationChange = -2;
                        routeScoreChange = 1;
                        routeType = "BLACK_RED_MAIN_STAGE";
                        summary = "你蹭热度接住了隔壁话题，楼友开始搬运切片，DD进场更快，但独角兽已经在记仇。";
                    }
                    case NPC_TENDENCY_AVOID -> {
                        ddFanChange = 4;
                        popularityChange = 2;
                        reputationChange = 1;
                        routeScoreChange = 0;
                        routeType = "ELECTRONIC_PICKLE";
                        summary = "你选择避嫌，把互动距离拉开，主会场没什么新料，老粉觉得边界清楚但DD少坐了几站。";
                    }
                    default -> {
                        ddFanChange = 12;
                        popularityChange = 18;
                        reputationChange = 1;
                        summary = "你轻轻查房并接住了一次低压互动，DD坐了一站，老粉表示先观察端水技术。";
                    }
                }
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFunFans(vup.getFunFans() + funFanChange);
                vup.setDdFans(vup.getDdFans() + ddFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setWatchHeat(clamp(vup.getWatchHeat() + watchHeatChange, 0, 100));
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
            }
            case FAN_GROUP_MAINTAIN -> {
                trueFanChange = 4;
                popularityChange = 3;
                reputationChange = commercialReviewWeek ? 3 : 2;
                inspirationChange = 1;
                if (commercialReviewWeek) {
                    commercialChange = 2;
                    coinChange = 200;
                    summary = "粉丝群维护完成了，商业复审周把稳定排班推给老板看，群友递来投稿素材，灵感+1，运营预算+200，商业化+2，标题组今天像正经打工人。";
                } else {
                    summary = "粉丝群维护完成了，老粉不用写小作文，群友递来投稿素材，灵感+1，标题组终于不用对着空白文档坐牢。";
                }
                routeScoreChange = 2;
                routeType = "ELECTRONIC_PICKLE";
                staminaChange = -2;
                vup.setStamina(vup.getStamina() - 2);
                vup.setInspiration(Math.max(0, vup.getInspiration() + 1));
                vup.setCoin(vup.getCoin() + coinChange);
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setPopularity(vup.getPopularity() + popularityChange);
                vup.setReputation(clamp(vup.getReputation() + reputationChange, 0, 100));
                vup.setCommercialLevel(clamp(vup.getCommercialLevel() + commercialChange, 0, 100));
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreChange);
            }
            case REST -> {
                trueFanChange = 2;
                staminaChange = balanceConfig.restStaminaRecovery();
                routeScoreChange = 0;
                routeType = null;
                int reputationRecovery = "BLACK_RED_MAIN_STAGE".equals(vup.getCurrentRoute()) && vup.getWatchHeat() >= 70 ? 3 : 2;
                summary = config.name + "完成了，今天主打稳健低压陪伴，体力回满一截，老粉不用写小作文，标题组暂时没有大的。";
                vup.setStamina(clamp(vup.getStamina() + staminaChange, 0, vup.getMaxStamina()));
                vup.setTrueFans(vup.getTrueFans() + trueFanChange);
                vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
                vup.setReputation(clamp(vup.getReputation() + reputationRecovery, 0, 100));
                vup.setWatchHeat(clamp(vup.getWatchHeat() - 1, 0, 100));
            }
            default -> {
                summary = config.name + "完成了，今天先稳住，别急着开庭。";
            }
        }

        // --- Day 1-3 new player fan boost (+50%) ---
        if (day <= 3) {
            int bonusTrue = trueFanChange / 2;
            int bonusFun = funFanChange / 2;
            int bonusDd = ddFanChange / 2;
            trueFanChange += bonusTrue;
            funFanChange += bonusFun;
            ddFanChange += bonusDd;
            vup.setTrueFans(vup.getTrueFans() + bonusTrue);
            vup.setFunFans(vup.getFunFans() + bonusFun);
            vup.setDdFans(vup.getDdFans() + bonusDd);
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
        }

        int totalFanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        int attributeChange = attributeChangeFor(actionType);
        Map<String, Object> evidenceRef = evidenceRefFor(actionType, npcTendency);
        if (routeType != null && !routeType.isBlank()) {
            evidenceRef.put("targetRoute", routeType);
        }
        int productionCoinCost = endgameProductionCoinCost(actionType, day);

        // --- apply action fatigue to fan/popularity gains ---
        if (fatiguePercent < 100) {
            int fatigueReduction = 100 - fatiguePercent;
            int originalTrueFan = trueFanChange;
            int originalFunFan = funFanChange;
            int originalDdFan = ddFanChange;
            int originalPopularity = popularityChange;
            int originalRouteScore = routeScoreChange;

            trueFanChange = trueFanChange * fatiguePercent / 100;
            funFanChange = funFanChange * fatiguePercent / 100;
            ddFanChange = ddFanChange * fatiguePercent / 100;
            popularityChange = popularityChange * fatiguePercent / 100;
            routeScoreChange = routeScoreChange * fatiguePercent / 100;

            // Adjust vup state to reflect fatigue-adjusted values
            int trueFanDelta = trueFanChange - originalTrueFan;
            int funFanDelta = funFanChange - originalFunFan;
            int ddFanDelta = ddFanChange - originalDdFan;
            int popularityDelta = popularityChange - originalPopularity;
            int routeScoreDelta = routeScoreChange - originalRouteScore;

            vup.setTrueFans(vup.getTrueFans() + trueFanDelta);
            vup.setFunFans(vup.getFunFans() + funFanDelta);
            vup.setDdFans(vup.getDdFans() + ddFanDelta);
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(vup.getPopularity() + popularityDelta);
            if (routeType != null && routeScoreDelta != 0) {
                dayFlowService.applyRouteScoreChange(vup, routeType, routeScoreDelta);
            }
            evidenceRef.put("fatigueReductionPercent", fatigueReduction);
            evidenceRef.put("fatigueRouteScoreReduction", originalRouteScore - routeScoreChange);
        }

        totalFanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;

        AudiencePressureService.AudiencePressure audiencePressure =
                audiencePressureService.pressureForAction(vup, session.getDay(), actionType, routeType);
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
            summary = summary + audiencePressureService.summarySuffix(audiencePressure);
        }

        totalFanChange = trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        if (audiencePressure.active()) {
            evidenceRef.put("audiencePressure", audiencePressureService.evidence(audiencePressure));
        }

        if (rotationBonus.active()) {
            trueFanChange += rotationBonus.trueFanBonus();
            popularityChange += rotationBonus.popularityBonus();
            reputationChange += rotationBonus.reputationBonus();
            inspirationChange += rotationBonus.inspirationBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + rotationBonus.trueFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + rotationBonus.popularityBonus()));
            vup.setReputation(clamp(vup.getReputation() + rotationBonus.reputationBonus(), 0, 100));
            vup.setInspiration(clamp(vup.getInspiration() + rotationBonus.inspirationBonus(), 0, balanceConfig.inspirationMax()));
            summary = summary + " " + rotationBonus.hint();
            evidenceRef.put("rotationBonus", rotationBonusEvidence(rotationBonus));
        }

        ComboImpact comboImpact = comboImpactFor(vup, session, actionType, routeType);
        if (comboImpact.applied()) {
            int comboFunFanBonus = blockFunFanBonuses ? 0 : comboImpact.funFanBonus();
            trueFanChange += comboImpact.trueFanBonus();
            funFanChange += comboFunFanBonus;
            ddFanChange += comboImpact.ddFanBonus();
            popularityChange += comboImpact.popularityBonus();
            watchHeatChange += comboImpact.watchHeatBonus();
            reputationChange += comboImpact.reputationBonus();
            memeChange += comboImpact.memeBonus();
            routeScoreChange += comboImpact.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + comboImpact.trueFanBonus()));
            vup.setFunFans(Math.max(0, vup.getFunFans() + comboFunFanBonus));
            vup.setDdFans(Math.max(0, vup.getDdFans() + comboImpact.ddFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + comboImpact.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + comboImpact.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + comboImpact.reputationBonus(), 0, 100));
            vup.setMemeLevel(clamp(vup.getMemeLevel() + comboImpact.memeBonus(), 0, 100));
            if (comboImpact.routeScoreBonus() != 0 && comboImpact.targetRoute() != null && !comboImpact.targetRoute().isBlank()) {
                dayFlowService.applyRouteScoreChange(vup, comboImpact.targetRoute(), comboImpact.routeScoreBonus());
            }
            summary = summary + " " + comboImpact.hint();
            evidenceRef.put("comboImpact", comboImpactEvidence(comboImpact));
        }

        StageObjectiveBonus stageObjectiveBonus = stageObjectiveBonusFor(vup, session, stageObjectiveSnapshot, actionType, routeType);
        if (stageObjectiveBonus.applied()) {
            trueFanChange += stageObjectiveBonus.trueFanBonus();
            popularityChange += stageObjectiveBonus.popularityBonus();
            reputationChange += stageObjectiveBonus.reputationBonus();
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
            summary = summary + " " + stageObjectiveBonus.hint();
            evidenceRef.put("stageObjectiveBonus", stageObjectiveEvidence(stageObjectiveBonus));
        }

        StageObjectiveService.StageMomentum stageMomentum = stageObjectiveService.stageMomentumFor(vup);
        StageMomentumDelta stageMomentumDelta = stageMomentumDeltaFor(stageMomentum, actionType.name(), routeType);
        if (stageMomentumDelta.applied()) {
            trueFanChange += stageMomentumDelta.trueFanChange();
            popularityChange += stageMomentumDelta.popularityChange();
            watchHeatChange += stageMomentumDelta.watchHeatChange();
            reputationChange += stageMomentumDelta.reputationChange();
            routeScoreChange += stageMomentumDelta.routeScoreChange();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + stageMomentumDelta.trueFanChange()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + stageMomentumDelta.popularityChange()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + stageMomentumDelta.watchHeatChange(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + stageMomentumDelta.reputationChange(), 0, 100));
            if (stageMomentumDelta.routeScoreChange() != 0 && routeType != null) {
                dayFlowService.applyRouteScoreChange(vup, routeType, stageMomentumDelta.routeScoreChange());
            }
            summary = summary + " " + stageMomentum.hint();
            evidenceRef.put("stageMomentum", stageMomentumEvidence(stageMomentum, stageMomentumDelta));
        }

        PlatformTrendModifier platformTrendModifier =
                platformTrendModifierFor(session.getDay(), actionType, routeType, null, session.getSelectedPlanId());
        if (platformTrendModifier.applied()) {
            int trendFunFanBonus = blockFunFanBonuses ? 0 : platformTrendModifier.funFanBonus();
            trueFanChange += platformTrendModifier.trueFanBonus();
            funFanChange += trendFunFanBonus;
            ddFanChange += platformTrendModifier.ddFanBonus();
            popularityChange += platformTrendModifier.popularityBonus();
            watchHeatChange += platformTrendModifier.watchHeatBonus();
            reputationChange += platformTrendModifier.reputationBonus();
            memeChange += platformTrendModifier.memeBonus();
            coinChange += platformTrendModifier.coinBonus();
            commercialChange += platformTrendModifier.commercialBonus();
            routeScoreChange += platformTrendModifier.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + platformTrendModifier.trueFanBonus()));
            vup.setFunFans(Math.max(0, vup.getFunFans() + trendFunFanBonus));
            vup.setDdFans(Math.max(0, vup.getDdFans() + platformTrendModifier.ddFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + platformTrendModifier.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + platformTrendModifier.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + platformTrendModifier.reputationBonus(), 0, 100));
            vup.setMemeLevel(clamp(vup.getMemeLevel() + platformTrendModifier.memeBonus(), 0, 100));
            vup.setCoin(Math.max(0, vup.getCoin() + platformTrendModifier.coinBonus()));
            vup.setCommercialLevel(clamp(vup.getCommercialLevel() + platformTrendModifier.commercialBonus(), 0, 100));
            if (platformTrendModifier.routeScoreBonus() != 0 && routeType != null) {
                dayFlowService.applyRouteScoreChange(vup, routeType, platformTrendModifier.routeScoreBonus());
            }
            summary = summary + " " + platformTrendModifier.hint();
            evidenceRef.put("platformTrendModifier", platformTrendEvidence(platformTrendModifier));
        }

        if (fortuneModifier.applied()) {
            trueFanChange += fortuneModifier.trueFanBonus();
            popularityChange += fortuneModifier.popularityBonus();
            watchHeatChange += fortuneModifier.watchHeatBonus();
            reputationChange += fortuneModifier.reputationBonus();
            routeScoreChange += fortuneModifier.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + fortuneModifier.trueFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + fortuneModifier.popularityBonus()));
            vup.setWatchHeat(clamp(vup.getWatchHeat() + fortuneModifier.watchHeatBonus(), 0, 100));
            vup.setReputation(clamp(vup.getReputation() + fortuneModifier.reputationBonus(), 0, 100));
            if (fortuneModifier.routeScoreBonus() != 0 && routeType != null) {
                dayFlowService.applyRouteScoreChange(vup, routeType, fortuneModifier.routeScoreBonus());
            }
            summary = summary + " " + fortuneModifier.hint();
            evidenceRef.put("fortuneModifier", fortuneEvidence(fortuneModifier));
        }

        EndgameSprintBonus endgameSprintBonus = endgameSprintBonusFor(sprintAnchorRoute, session.getDay(), actionType, routeType);
        if (endgameSprintBonus.active()) {
            trueFanChange += endgameSprintBonus.trueFanBonus();
            popularityChange += endgameSprintBonus.popularityBonus();
            routeScoreChange += endgameSprintBonus.routeScoreBonus();
            vup.setTrueFans(Math.max(0, vup.getTrueFans() + endgameSprintBonus.trueFanBonus()));
            vup.setFans(vup.getTrueFans() + vup.getFunFans() + vup.getUnicornFans() + vup.getDdFans());
            vup.setPopularity(Math.max(0, vup.getPopularity() + endgameSprintBonus.popularityBonus()));
            if (routeType != null && endgameSprintBonus.routeScoreBonus() != 0) {
                dayFlowService.applyRouteScoreChange(vup, routeType, endgameSprintBonus.routeScoreBonus());
            }
            summary = summary + " " + endgameSprintBonus.hint();
            evidenceRef.put("endgameSprint", endgameSprintEvidence(endgameSprintBonus));
        }

        if (productionCoinCost > 0) {
            coinChange -= productionCoinCost;
            vup.setCoin(Math.max(0, vup.getCoin() - productionCoinCost));
            summary = summary + " " + endgameProductionSummary(actionType, productionCoinCost);
            evidenceRef.put("productionBudget", Map.of(
                    "window", "ENDGAME_SPRINT",
                    "coinCost", productionCoinCost,
                    "day", day
            ));
        }

        summary = summary + " " + actionFrontDeskSummary(
                actionType,
                routeType,
                trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                staminaChange,
                popularityChange,
                watchHeatChange,
                reputationChange,
                memeChange,
                coinChange,
                inspirationChange,
                routeScoreChange
        );
        evidenceRef.put("frontDesk", Map.of(
                "benefit", actionBenefitLine(trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                        popularityChange, watchHeatChange, reputationChange, memeChange, coinChange, inspirationChange, routeScoreChange),
                "cost", actionCostLine(staminaChange, reputationChange, watchHeatChange, coinChange, inspirationChange),
                "route", routeLineFor(routeType, routeScoreChange),
                "next", actionNextHint(actionType, routeType, routeScoreChange, watchHeatChange, reputationChange)
        ));

        return new ActionResultDTO(
                actionType.name(),
                summary,
                trueFanChange + funFanChange + unicornFanChange + ddFanChange,
                trueFanChange,
                funFanChange,
                unicornFanChange,
                ddFanChange,
                staminaChange,
                attributeChange,
                routeScoreChange,
                evidenceRef
        ).withFatigueInfo(fatigueInfo);
    }

    private String actionFrontDeskSummary(
            ActionType actionType,
            String routeType,
            int fanChange,
            int staminaChange,
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange,
            int coinChange,
            int inspirationChange,
            int routeScoreChange
    ) {
        return "结算说明：收益：" + actionBenefitLine(fanChange, popularityChange, watchHeatChange, reputationChange,
                memeChange, coinChange, inspirationChange, routeScoreChange)
                + "；代价：" + actionCostLine(staminaChange, reputationChange, watchHeatChange, coinChange, inspirationChange)
                + "；路线：" + routeLineFor(routeType, routeScoreChange)
                + "；下一步：" + actionNextHint(actionType, routeType, routeScoreChange, watchHeatChange, reputationChange) + "。";
    }

    private String actionBenefitLine(
            int fanChange,
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange,
            int coinChange,
            int inspirationChange,
            int routeScoreChange
    ) {
        List<String> pieces = new ArrayList<>();
        if (fanChange != 0) {
            pieces.add("粉丝" + signed(fanChange));
        }
        if (popularityChange > 0) {
            pieces.add("人气" + signed(popularityChange));
        }
        if (watchHeatChange > 0) {
            pieces.add("围观" + signed(watchHeatChange));
        }
        if (reputationChange > 0) {
            pieces.add("口碑" + signed(reputationChange));
        }
        if (memeChange > 0) {
            pieces.add("梗浓度" + signed(memeChange));
        }
        if (coinChange > 0) {
            pieces.add("运营预算" + signed(coinChange));
        }
        if (inspirationChange > 0) {
            pieces.add("灵感" + signed(inspirationChange));
        }
        if (routeScoreChange > 0) {
            pieces.add("路线证据" + signed(routeScoreChange));
        }
        if (pieces.isEmpty()) {
            return "主要收益是保住节奏和状态";
        }
        return String.join("，", pieces);
    }

    private String actionCostLine(
            int staminaChange,
            int reputationChange,
            int watchHeatChange,
            int coinChange,
            int inspirationChange
    ) {
        List<String> pieces = new ArrayList<>();
        if (staminaChange < 0) {
            pieces.add("体力" + signed(staminaChange));
        }
        if (reputationChange < 0) {
            pieces.add("口碑" + signed(reputationChange));
        }
        if (watchHeatChange > 6) {
            pieces.add("围观压力" + signed(watchHeatChange));
        }
        if (coinChange < 0) {
            pieces.add("运营预算" + signed(coinChange));
        }
        if (inspirationChange < 0) {
            pieces.add("灵感" + signed(inspirationChange));
        }
        if (pieces.isEmpty()) {
            return "没有新增明显代价，但低风险不等于免费收益";
        }
        return String.join("，", pieces);
    }

    private String routeLineFor(String routeType, int routeScoreChange) {
        if (routeType == null || routeType.isBlank() || routeScoreChange <= 0) {
            return "这手偏防守或资源调整，不直接加厚路线";
        }
        return routeBiasLabel(routeType) + "证据+" + routeScoreChange + "，会进入日报和结局材料";
    }

    private String actionNextHint(
            ActionType actionType,
            String routeType,
            int routeScoreChange,
            int watchHeatChange,
            int reputationChange
    ) {
        if (watchHeatChange >= 8 || reputationChange < 0) {
            return "明天优先看旧账窗口，用杂谈复盘或粉丝群维护接住代价";
        }
        if (routeType != null && !routeType.isBlank() && routeScoreChange > 0) {
            return "明天继续补" + routeBiasLabel(routeType) + "同类证据，别临时摇摆";
        }
        if (actionType == ActionType.REST) {
            return "体力回来了，下一天接一个能写进结局的主行动";
        }
        return "看结局预演缺口，决定补路线还是拆旧账";
    }

    private String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }

    private Map<String, Object> weightDetailFor(ActionResultDTO result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", result.actionType());
        detail.put("candidatePool", MAIN_ACTION_CANDIDATE_POOL);
        detail.put("weights", mainActionWeights(result.actionType()));
        detail.put("hit", result.actionType());
        if (ActionType.NPC_INTERACT.name().equals(result.actionType())) {
            Object npcTendency = result.evidenceRef().get("npcTendency");
            detail.put("npcTendency", npcTendency == null ? NPC_TENDENCY_RAID : npcTendency);
        }
        return detail;
    }

    private Map<String, Object> evidenceRefFor(ActionType actionType, String npcTendency) {
        Map<String, Object> evidenceRef = new LinkedHashMap<>();
        evidenceRef.put("type", "business_log");
        evidenceRef.put("source", "pending");
        if (actionType == ActionType.NPC_INTERACT) {
            evidenceRef.put("npcTendency", npcTendency == null ? NPC_TENDENCY_RAID : npcTendency);
        }
        return evidenceRef;
    }

    private Map<String, Object> mainActionWeights(String hitAction) {
        Map<String, Object> weights = new LinkedHashMap<>();
        for (String actionType : MAIN_ACTION_CANDIDATE_POOL) {
            weights.put(actionType, actionType.equals(hitAction) ? 1 : 0);
        }
        return weights;
    }

    PlatformTrendModifier platformTrendModifierFor(
            int day,
            ActionType actionType,
            String routeType,
            String titleStyle,
            Long planId
    ) {
        String trendId = platformTrendService.trendIdForDay(day);
        Map<String, Object> trend = platformTrendService.trendFor(trendId);
        String trendLabel = String.valueOf(trend.getOrDefault("label", "平台风向"));
        boolean safeTitle = "SAFE".equals(titleStyle);
        boolean blackRedTitle = "HARD_MOUTH".equals(titleStyle) || "ABSTRACT_MEME".equals(titleStyle);
        boolean commercialTitle = "FAN_SERVICE".equals(titleStyle) || "BUSINESS_SAFE".equals(titleStyle);
        boolean singingPlan = planId != null && planId == 2L;
        boolean danceMemePlan = planId != null && (planId == 3L || planId == 11L);

        if (PlatformTrendService.DEFAULT_TREND_ID.equals(trendId)) {
            if (actionType == ActionType.TRAIN_TALK || actionType == ActionType.FAN_GROUP_MAINTAIN || actionType == ActionType.REST
                    || (actionType == ActionType.STREAM_PLAN && safeTitle)) {
                return new PlatformTrendModifier(true, trendId, trendLabel, true, 2, 0, 0, 0, -1, 1, 0, 0, 0, 1,
                        "平台清朗期吃低压内容，稳盘行动多留住了一点老粉。", "粉丝+2");
            }
            if (actionType == ActionType.PUBLISH_CLIP || (actionType == ActionType.STREAM_PLAN && blackRedTitle)) {
                return new PlatformTrendModifier(true, trendId, trendLabel, false, 0, 0, 0, -4, 3, -2, 0, 0, 0, 0,
                        "清朗低压周逆风硬冲，围观还在，但口碑被平台风控刮了一层。", "口碑-2");
            }
        }

        if (PlatformTrendService.SINGING_BOOST_WEEK.equals(trendId)) {
            if (actionType == ActionType.TRAIN_SONG
                    || actionType == ActionType.PUBLISH_VIDEO
                    || (actionType == ActionType.STREAM_PLAN && (singingPlan || "SINGING_IDOL".equals(routeType)))) {
                int trueFanBonus = actionType == ActionType.STREAM_PLAN ? 2 : 3;
                int ddFanBonus = actionType == ActionType.STREAM_PLAN ? 0 : 1;
                int popularityBonus = actionType == ActionType.STREAM_PLAN ? 5 : 10;
                return new PlatformTrendModifier(true, trendId, trendLabel, true, trueFanBonus, 0, ddFanBonus, popularityBonus, 0, 1, 0, 0, 0, 1,
                        "歌回扶持周踩中平台口味，基本功证据更容易被推荐接住。", "路线+1");
            }
            if (actionType == ActionType.STREAM_PLAN && blackRedTitle && singingPlan) {
                return new PlatformTrendModifier(true, trendId, trendLabel, false, 0, 2, 0, 8, 4, -1, 1, 0, 0, 0,
                        "歌回扶持周硬嘴开歌，流量确实更高，录播组也更爱逐帧审判。", "压力+4");
            }
        }

        if (PlatformTrendService.MEME_OUTBREAK_WEEK.equals(trendId)) {
            if (actionType == ActionType.PUBLISH_CLIP
                    || actionType == ActionType.TRAIN_DANCE
                    || (actionType == ActionType.STREAM_PLAN && (danceMemePlan || "DANCE_MEME".equals(routeType)))) {
                int funFanBonus = actionType == ActionType.STREAM_PLAN ? 2 : 4;
                int ddFanBonus = actionType == ActionType.STREAM_PLAN ? 0 : 1;
                int popularityBonus = actionType == ActionType.STREAM_PLAN ? 5 : 12;
                int memeBonus = actionType == ActionType.STREAM_PLAN ? 1 : 2;
                int watchHeatBonus = actionType == ActionType.STREAM_PLAN ? 2 : 3;
                return new PlatformTrendModifier(true, trendId, trendLabel, true, 0, funFanBonus, ddFanBonus, popularityBonus, watchHeatBonus, -1, memeBonus, 0, 0, 1,
                        "抽象出圈周接住整活窗口，切片和二创多了一点自来水。", "人气+" + popularityBonus);
            }
            if (actionType == ActionType.FAN_GROUP_MAINTAIN || actionType == ActionType.REST) {
                return new PlatformTrendModifier(true, trendId, trendLabel, false, 1, 0, 0, -3, -1, 1, 0, 0, 0, 0,
                        "抽象出圈周选择低压收束，爆点少一点，但粉丝群气压稳住了。", "口碑+1");
            }
        }

        if (PlatformTrendService.COMMERCIAL_REVIEW_WEEK.equals(trendId)) {
            if (actionType == ActionType.FAN_GROUP_MAINTAIN
                    || (actionType == ActionType.STREAM_PLAN && commercialTitle)
                    || actionType == ActionType.TRAIN_TALK) {
                int trueFanBonus = actionType == ActionType.STREAM_PLAN ? 1 : 2;
                int coinBonus = actionType == ActionType.STREAM_PLAN ? 0 : 100;
                int commercialBonus = actionType == ActionType.STREAM_PLAN ? 0 : 1;
                return new PlatformTrendModifier(true, trendId, trendLabel, true, trueFanBonus, 0, 0, 5, -1, 1, 0, coinBonus, commercialBonus, 1,
                        "商业复审周偏爱稳定排班，本手把账面和观众耐心都补了一点。", "运营预算+" + coinBonus);
            }
            if (actionType == ActionType.PUBLISH_CLIP || (actionType == ActionType.STREAM_PLAN && blackRedTitle)) {
                return new PlatformTrendModifier(true, trendId, trendLabel, false, 0, 2, 0, 6, 4, -2, 1, 0, 0, 0,
                        "商业复审周逆风整活，传播还有，但品牌安全和老粉耐心同时承压。", "口碑-2");
            }
        }

        return PlatformTrendModifier.empty(trendId, trendLabel);
    }

    Map<String, Object> platformTrendEvidence(PlatformTrendModifier modifier) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("trendId", modifier.trendId());
        evidence.put("label", modifier.label());
        evidence.put("matched", modifier.matched());
        evidence.put("trueFanBonus", modifier.trueFanBonus());
        evidence.put("funFanBonus", modifier.funFanBonus());
        evidence.put("ddFanBonus", modifier.ddFanBonus());
        evidence.put("popularityBonus", modifier.popularityBonus());
        evidence.put("watchHeatBonus", modifier.watchHeatBonus());
        evidence.put("reputationBonus", modifier.reputationBonus());
        evidence.put("memeBonus", modifier.memeBonus());
        evidence.put("coinBonus", modifier.coinBonus());
        evidence.put("commercialBonus", modifier.commercialBonus());
        evidence.put("routeScoreBonus", modifier.routeScoreBonus());
        evidence.put("primaryDeltaLabel", modifier.primaryDeltaLabel());
        evidence.put("hint", modifier.hint());
        return evidence;
    }

    FortuneModifier fortuneModifierFor(Vup vup, DaySession session, ActionType actionType, boolean highRisk) {
        DailyFortuneDTO fortune = dailyFortuneService.getFortune(session.getDay(), vup.getFans(), vup.getReputation(), session);
        return FortuneModifier.empty(fortune.fortune(), fortune.luckyAction());
    }

    Map<String, Object> fortuneEvidence(FortuneModifier modifier) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("fortune", modifier.fortune());
        evidence.put("luckyAction", modifier.luckyAction());
        evidence.put("matched", modifier.matched());
        evidence.put("trueFanBonus", modifier.trueFanBonus());
        evidence.put("popularityBonus", modifier.popularityBonus());
        evidence.put("watchHeatBonus", modifier.watchHeatBonus());
        evidence.put("reputationBonus", modifier.reputationBonus());
        evidence.put("routeScoreBonus", modifier.routeScoreBonus());
        evidence.put("primaryDeltaLabel", modifier.primaryDeltaLabel());
        evidence.put("hint", modifier.hint());
        return evidence;
    }

    StageObjectiveSnapshot stageObjectiveSnapshot(Vup vup, DaySession session) {
        DailyFortuneDTO fortune = dailyFortuneService.getFortune(session.getDay(), vup.getFans(), vup.getReputation(), session);
        String objectiveActionType = stageObjectiveService.objectiveActionTypeFor(vup, fortune.luckyAction());
        StageObjectiveService.StageObjectivePlan plan = stageObjectiveService.objectiveFor(vup);
        return new StageObjectiveSnapshot(
                objectiveActionType,
                plan.title(),
                plan.totalCount(),
                stageObjectiveService.objectiveTargetsFor(vup)
        );
    }

    StageObjectiveBonus stageObjectiveBonusFor(Vup vup, DaySession session, StageObjectiveSnapshot snapshot, ActionType actionType, String routeType) {
        StageObjectiveService.StageObjectiveTarget matchedTarget = matchingStageObjectiveTarget(snapshot, actionType, routeType);
        if (snapshot == null || matchedTarget == null || snapshot.totalCount() <= 0) {
            return StageObjectiveBonus.empty(
                    snapshot == null ? "" : snapshot.objectiveActionType(),
                    snapshot == null ? "" : snapshot.objectiveTitle()
            );
        }
        String targetRoute = matchedTarget.targetRouteType() == null || matchedTarget.targetRouteType().isBlank()
                ? routeType == null || routeType.isBlank() ? sprintRouteType(actionType) : routeType
                : matchedTarget.targetRouteType();
        targetRoute = routeScoreTypeForStageTarget(targetRoute);
        boolean routeMatched = stageObjectiveRouteMatches(matchedTarget, routeType);
        int routeScoreBonus = targetRoute == null || targetRoute.isBlank() || "UNKNOWN".equals(targetRoute) || !routeMatched ? 0 : 1;
        int popularityBonus = actionType == ActionType.REST ? 0 : 3;
        int reputationBonus = actionType == ActionType.STREAM_PLAN || actionType == ActionType.PUBLISH_CLIP ? 0 : 1;
        int streak = stageObjectiveHitStreak(vup, session);
        if (streak >= 2) {
            popularityBonus += 2;
        }
        if (streak >= 3 && routeScoreBonus > 0) {
            routeScoreBonus += 1;
        }
        return new StageObjectiveBonus(
                true,
                matchedTarget.actionType(),
                snapshot.objectiveTitle(),
                targetRoute,
                2,
                popularityBonus,
                reputationBonus,
                routeScoreBonus,
                streak,
                streak >= 2
                        ? "今日目标连续命中，阶段复盘组把这手记进了短线连击。"
                        : "今日目标命中，阶段复盘组把这手记进了短线委托。",
                routeScoreBonus > 0 ? "路线+" + routeScoreBonus : "粉丝+2"
        );
    }

    private StageObjectiveService.StageObjectiveTarget matchingStageObjectiveTarget(
            StageObjectiveSnapshot snapshot,
            ActionType actionType,
            String routeType
    ) {
        if (snapshot == null || actionType == null || snapshot.targets() == null || snapshot.targets().isEmpty()) {
            return null;
        }
        String actionName = actionType.name();
        String currentRoute = routeType == null ? "" : routeType;
        return snapshot.targets().stream()
                .filter(target -> actionName.equals(target.actionType()))
                .min(Comparator.comparingInt(target -> stageObjectiveTargetScore(target, currentRoute)))
                .orElse(null);
    }

    private int stageObjectiveTargetScore(StageObjectiveService.StageObjectiveTarget target, String routeType) {
        if (target.targetRouteType() == null || target.targetRouteType().isBlank()) {
            return 1;
        }
        String targetRoute = routeScoreTypeForStageTarget(target.targetRouteType());
        if (routeType != null && !routeType.isBlank() && targetRoute.equals(routeType)) {
            return 0;
        }
        return 2;
    }

    private boolean stageObjectiveRouteMatches(StageObjectiveService.StageObjectiveTarget target, String routeType) {
        if (target.targetRouteType() == null || target.targetRouteType().isBlank()) {
            return true;
        }
        String targetRoute = routeScoreTypeForStageTarget(target.targetRouteType());
        return routeType != null && !routeType.isBlank() && targetRoute.equals(routeType);
    }

    private String routeScoreTypeForStageTarget(String targetRoute) {
        return switch (targetRoute == null ? "" : targetRoute) {
            case "MAIN_STAGE_KING" -> RouteType.BLACK_RED_MAIN_STAGE.name();
            case "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION" -> RouteType.ELECTRONIC_PICKLE.name();
            case "DD_BUS_STOP" -> RouteType.SOCIAL_COLLAB.name();
            default -> targetRoute == null ? "" : targetRoute;
        };
    }

    StageObjectiveBonus stageObjectiveBonusFor(Vup vup, DaySession session, ActionType actionType, String routeType) {
        return stageObjectiveBonusFor(vup, session, stageObjectiveSnapshot(vup, session), actionType, routeType);
    }

    private int stageObjectiveHitStreak(Vup vup, DaySession session) {
        return stageObjectiveService.objectiveHitStreakBefore(vup, session.getDay()) + 1;
    }

    Map<String, Object> stageObjectiveEvidence(StageObjectiveBonus bonus) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("objectiveActionType", bonus.objectiveActionType());
        evidence.put("objectiveTitle", bonus.objectiveTitle());
        evidence.put("targetRoute", bonus.targetRoute());
        evidence.put("trueFanBonus", bonus.trueFanBonus());
        evidence.put("popularityBonus", bonus.popularityBonus());
        evidence.put("reputationBonus", bonus.reputationBonus());
        evidence.put("routeScoreBonus", bonus.routeScoreBonus());
        evidence.put("streak", bonus.streak());
        evidence.put("primaryDeltaLabel", bonus.primaryDeltaLabel());
        evidence.put("hint", bonus.hint());
        return evidence;
    }

    private String fortunateActionType(String luckyAction) {
        if (luckyAction == null || luckyAction.isBlank()) {
            return null;
        }
        if (luckyAction.contains("直播企划")) {
            return ActionType.STREAM_PLAN.name();
        }
        if (luckyAction.contains("练歌")) {
            return ActionType.TRAIN_SONG.name();
        }
        if (luckyAction.contains("练舞")) {
            return ActionType.TRAIN_DANCE.name();
        }
        if (luckyAction.contains("杂谈")) {
            return ActionType.TRAIN_TALK.name();
        }
        if (luckyAction.contains("发布视频")) {
            return ActionType.PUBLISH_VIDEO.name();
        }
        if (luckyAction.contains("发布切片")) {
            return ActionType.PUBLISH_CLIP.name();
        }
        if (luckyAction.contains("粉丝群")) {
            return ActionType.FAN_GROUP_MAINTAIN.name();
        }
        if (luckyAction.contains("同台")) {
            return ActionType.NPC_INTERACT.name();
        }
        if (luckyAction.contains("休息")) {
            return ActionType.REST.name();
        }
        return null;
    }

    private boolean isHighRiskAction(ActionType actionType) {
        return actionType == ActionType.STREAM_PLAN
                || actionType == ActionType.PUBLISH_CLIP
                || actionType == ActionType.NPC_INTERACT;
    }

    StageMomentumDelta stageMomentumDeltaFor(
            StageObjectiveService.StageMomentum momentum,
            String actionType,
            String routeType
    ) {
        if (momentum == null || "none".equals(momentum.tier())) {
            return StageMomentumDelta.empty();
        }
        boolean highRisk = ActionType.PUBLISH_CLIP.name().equals(actionType)
                || ActionType.STREAM_PLAN.name().equals(actionType)
                || RouteType.BLACK_RED_MAIN_STAGE.name().equals(routeType)
                || RouteType.DANCE_MEME.name().equals(routeType);
        if ("weak".equals(momentum.tier()) && highRisk) {
            return new StageMomentumDelta(0, 0, 0, momentum.watchHeatBonus(), -momentum.reputationBonus());
        }
        boolean routeBuilding = ActionType.TRAIN_SONG.name().equals(actionType)
                || ActionType.TRAIN_DANCE.name().equals(actionType)
                || ActionType.TRAIN_TALK.name().equals(actionType)
                || ActionType.PUBLISH_VIDEO.name().equals(actionType)
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(actionType)
                || ActionType.STREAM_PLAN.name().equals(actionType);
        if (!routeBuilding) {
            return StageMomentumDelta.empty();
        }
        return new StageMomentumDelta(
                momentum.trueFanBonus(),
                momentum.popularityBonus(),
                momentum.routeBonus(),
                momentum.watchHeatBonus(),
                momentum.reputationBonus()
        );
    }

    Map<String, Object> stageMomentumEvidence(
            StageObjectiveService.StageMomentum momentum,
            StageMomentumDelta delta
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("tier", momentum.tier());
        evidence.put("previousStageTitle", momentum.previousStageTitle());
        evidence.put("previousStageProgress", momentum.previousStageProgressLabel());
        evidence.put("hint", momentum.hint());
        evidence.put("trueFanBonus", delta.trueFanChange());
        evidence.put("popularityBonus", delta.popularityChange());
        evidence.put("routeBonus", delta.routeScoreChange());
        evidence.put("watchHeatBonus", delta.watchHeatChange());
        evidence.put("reputationBonus", delta.reputationChange());
        return evidence;
    }

    private EndgameSprintBonus endgameSprintBonusFor(String currentRoute, int day, ActionType actionType, String routeType) {
        if (day < 22 || day > 30 || actionType == ActionType.REST || routeType == null || routeType.isBlank()) {
            return EndgameSprintBonus.empty();
        }
        currentRoute = currentRoute == null ? "" : currentRoute;
        if (!isEndgameSprintAligned(currentRoute, routeType)) {
            return EndgameSprintBonus.empty();
        }
        int routeScoreBonus = ActionType.STREAM_PLAN == actionType ? 2 : 1;
        int trueFanBonus = ActionType.PUBLISH_CLIP == actionType || ActionType.NPC_INTERACT == actionType ? 2 : 3;
        int popularityBonus = ActionType.PUBLISH_CLIP == actionType || ActionType.STREAM_PLAN == actionType ? 8 : 5;
        String hint = "收官押线触发：最后阶段继续沿【" + routeBiasLabel(currentRoute)
                + "】补证据，结局组更容易把这一手写进代表事件。";
        return new EndgameSprintBonus(true, currentRoute, routeType, trueFanBonus, popularityBonus, routeScoreBonus, hint);
    }

    private boolean isEndgameSprintAligned(String currentRoute, String routeType) {
        if (currentRoute.equals(routeType)) {
            return true;
        }
        return ("SLICE_SAINT".equals(currentRoute) && "DANCE_MEME".equals(routeType))
                || ("DANCE_MEME".equals(currentRoute) && "SLICE_SAINT".equals(routeType));
    }

    private Map<String, Object> endgameSprintEvidence(EndgameSprintBonus bonus) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("currentRoute", bonus.currentRoute());
        evidence.put("actionRoute", bonus.actionRoute());
        evidence.put("trueFanBonus", bonus.trueFanBonus());
        evidence.put("popularityBonus", bonus.popularityBonus());
        evidence.put("routeScoreBonus", bonus.routeScoreBonus());
        evidence.put("hint", bonus.hint());
        return evidence;
    }

    private RotationBonus rotationBonusFor(Vup vup, DaySession session, ActionType actionType) {
        if (session.getDay() <= 2 || actionType == ActionType.REST) {
            return RotationBonus.empty();
        }
        List<BusinessLog> recentLogs = businessLogMapper.findRecentByVupId(vup.getId(), 3).stream()
                .filter(log -> log.getDay() < session.getDay())
                .sorted(Comparator.comparingInt(BusinessLog::getDay).reversed()
                        .thenComparing(Comparator.comparing(BusinessLog::getId).reversed()))
                .toList();
        if (recentLogs.isEmpty()) {
            return RotationBonus.empty();
        }

        String currentLane = actionLane(actionType.name());
        if (currentLane.isBlank()) {
            return RotationBonus.empty();
        }
        BusinessLog previous = recentLogs.get(0);
        String previousLane = actionLane(previous.getAction());
        if (previousLane.isBlank() || currentLane.equals(previousLane)) {
            return RotationBonus.empty();
        }

        boolean threeLaneMix = recentLogs.size() >= 2
                && recentLogs.stream()
                .limit(2)
                .map(log -> actionLane(log.getAction()))
                .filter(lane -> !lane.isBlank())
                .noneMatch(currentLane::equals)
                && recentLogs.stream()
                .limit(2)
                .map(log -> actionLane(log.getAction()))
                .filter(lane -> !lane.isBlank())
                .distinct()
                .count() >= 2;

        int trueFanBonus = threeLaneMix ? 4 : 2;
        int popularityBonus = threeLaneMix ? 8 : 4;
        int reputationBonus = "CONTENT".equals(currentLane) ? 0 : 1;
        int inspirationBonus = threeLaneMix ? 1 : 0;
        String hint = threeLaneMix
                ? "轮换经营触发：练习、内容和运营没有挤在同一条时间线里，老粉觉得这周排班有节奏，灵感+1。"
                : "轮换经营触发：今天换了经营重心，观众没有被同一种内容刷疲劳。";
        return new RotationBonus(
                true,
                previousLane,
                currentLane,
                threeLaneMix ? "three_lane_mix" : "lane_shift",
                trueFanBonus,
                popularityBonus,
                reputationBonus,
                inspirationBonus,
                hint
        );
    }

    private String actionLane(String actionType) {
        if (ActionType.TRAIN_SONG.name().equals(actionType)
                || ActionType.TRAIN_DANCE.name().equals(actionType)
                || ActionType.TRAIN_TALK.name().equals(actionType)) {
            return "PRACTICE";
        }
        if (ActionType.STREAM_PLAN.name().equals(actionType)
                || ActionType.PUBLISH_VIDEO.name().equals(actionType)
                || ActionType.PUBLISH_CLIP.name().equals(actionType)) {
            return "CONTENT";
        }
        if (ActionType.FAN_GROUP_MAINTAIN.name().equals(actionType)
                || ActionType.NPC_INTERACT.name().equals(actionType)) {
            return "SOCIAL";
        }
        return "";
    }

    private Map<String, Object> rotationBonusEvidence(RotationBonus bonus) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("kind", bonus.kind());
        evidence.put("previousLane", bonus.previousLane());
        evidence.put("currentLane", bonus.currentLane());
        evidence.put("trueFanBonus", bonus.trueFanBonus());
        evidence.put("popularityBonus", bonus.popularityBonus());
        evidence.put("reputationBonus", bonus.reputationBonus());
        evidence.put("inspirationBonus", bonus.inspirationBonus());
        evidence.put("hint", bonus.hint());
        return evidence;
    }

    private int stageFanCap(int fans) {
        if (fans < 1_000) {
            return balanceConfig.newbieStageFanCap();
        }
        if (fans < 10_000) {
            return balanceConfig.startingStageFanCap();
        }
        if (fans < 50_000) {
            return balanceConfig.growthStageFanCap();
        }
        if (fans < 100_000) {
            return balanceConfig.breakoutStageFanCap();
        }
        return balanceConfig.topStageFanCap();
    }

    private Map<String, Object> multiplierDetailFor(ActionResultDTO result, ActionLogDelta delta) {
        int watchHeatGain = Math.max(0, delta.watchHeatChange());
        double watchToFanRate = watchToFanRateFor(result, watchHeatGain);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("pipeline", "P0_SIMPLE");
        detail.put("source", "BalanceConfig");
        detail.put("watchHeatGain", watchHeatGain);
        detail.put("watchToFanRate", watchToFanRate);
        detail.put("watchToFanDetail", Map.of(
                "convertedFunFans", result.funFanChange(),
                "convertedDdFans", result.ddFanChange(),
                "retainedFans", result.fanChange(),
                "explanation", watchHeatGain > 0
                        ? "围观热度只部分转化为乐子人或DD，剩余热度留作舆论和复盘材料。"
                        : "本行动不走围观转粉。"
        ));
        copyEvidenceDetail(result, detail, "comboImpact");
        copyEvidenceDetail(result, detail, "stageObjectiveBonus");
        copyEvidenceDetail(result, detail, "platformTrendModifier");
        copyEvidenceDetail(result, detail, "fortuneModifier");
        copyEvidenceDetail(result, detail, "defenseMitigation");
        if (result.fatigueInfo() != null && result.fatigueInfo().fatiguePercent() < 100) {
            detail.put("fatigueInfo", Map.of(
                    "consecutiveDays", result.fatigueInfo().consecutiveDays(),
                    "fatiguePercent", result.fatigueInfo().fatiguePercent(),
                    "fatigueHint", result.fatigueInfo().fatigueHint()
            ));
        }
        return detail;
    }

    private void copyEvidenceDetail(ActionResultDTO result, Map<String, Object> detail, String key) {
        Object value = result.evidenceRef().get(key);
        if (value != null) {
            detail.put(key, value);
        }
    }

    private double watchToFanRateFor(ActionResultDTO result, int watchHeatGain) {
        if (watchHeatGain <= 0) {
            return 0.0;
        }
        String summary = result.summary();
        if (isHardMouthSummary(summary)) {
            return 0.20;
        }
        if (isAbstractMemeSummary(summary)) {
            return 0.25;
        }
        if (isDanceMemeSummary(summary)) {
            return 0.30;
        }
        if (ActionType.PUBLISH_CLIP.name().equals(result.actionType())) {
            return 0.35;
        }
        return 0.10;
    }

    private boolean isDanceMemeSummary(String summary) {
        return summaryMentions(summary, "DANCE_MEME", "梗舞");
    }

    private boolean isHardMouthSummary(String summary) {
        return summaryMentions(summary, "HARD_MOUTH", "硬嘴", "高风险歌回");
    }

    private boolean isAbstractMemeSummary(String summary) {
        return summaryMentions(summary, "ABSTRACT_MEME", "抽象企划");
    }

    private boolean isFanServiceSummary(String summary) {
        return summaryMentions(summary, "FAN_SERVICE", "陪伴营业", "醒目留言陪伴回", "高亮互动回应", "谢SC陪伴回");
    }

    private boolean isBusinessSafeSummary(String summary) {
        return summaryMentions(summary, "BUSINESS_SAFE", "稳健处理模拟礼物", "稳稳回应醒目留言", "稳健谢礼物", "稳稳谢SC");
    }

    private boolean hasPracticeToStreamCombo(String summary) {
        return summaryMentions(summary, "PRACTICE_TO_STREAM", "练习后直播");
    }

    private boolean hasVideoToStreamCombo(String summary) {
        return summaryMentions(summary, "VIDEO_TO_STREAM", "视频引流直播");
    }

    private boolean hasVideoToClipCombo(String summary) {
        return summaryMentions(summary, "VIDEO_TO_CLIP", "一鱼两剪");
    }

    private boolean summaryMentions(String summary, String... markers) {
        if (summary == null || summary.isBlank()) {
            return false;
        }
        for (String marker : markers) {
            if (summary.contains(marker)) {
                return true;
            }
        }
        return false;
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

    private String routeScoreKey(ActionResultDTO result) {
        if (result.routeScoreChange() == 0) {
            return RouteType.UNKNOWN.name();
        }
        Object targetRoute = result.evidenceRef().get("targetRoute");
        if (targetRoute instanceof String route && !route.isBlank()) {
            return route;
        }
        String actionType = result.actionType();
        if (ActionType.NPC_INTERACT.name().equals(actionType)) {
            return RouteType.SOCIAL_COLLAB.name();
        }
        if (ActionType.PUBLISH_CLIP.name().equals(actionType)
                || ActionType.PUBLISH_VIDEO.name().equals(actionType)) {
            return RouteType.SLICE_SAINT.name();
        }
        if (ActionType.TRAIN_TALK.name().equals(actionType)
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(actionType)
                || ActionType.REST.name().equals(actionType)) {
            return RouteType.ELECTRONIC_PICKLE.name();
        }
        if (ActionType.TRAIN_DANCE.name().equals(actionType)) {
            return RouteType.DANCE_MEME.name();
        }
        if (ActionType.STREAM_PLAN.name().equals(actionType)
                && isDanceMemeSummary(result.summary())) {
            return RouteType.DANCE_MEME.name();
        }
        if (ActionType.STREAM_PLAN.name().equals(actionType)
                && (isHardMouthSummary(result.summary()) || isAbstractMemeSummary(result.summary()))) {
            return RouteType.BLACK_RED_MAIN_STAGE.name();
        }
        if (ActionType.STREAM_PLAN.name().equals(actionType)
                && (isFanServiceSummary(result.summary())
                || isBusinessSafeSummary(result.summary())
                || result.summary().contains("商业"))) {
            return RouteType.ELECTRONIC_PICKLE.name();
        }
        return RouteType.SINGING_IDOL.name();
    }

    private ActionLogDelta actionLogDeltaWithEvidence(ActionResultDTO result, ActionLogDelta base) {
        int popularityChange = base.popularityChange();
        int watchHeatChange = base.watchHeatChange();
        int reputationChange = base.reputationChange();

        Object rotationRaw = result.evidenceRef().get("rotationBonus");
        if (rotationRaw instanceof Map<?, ?> rotation) {
            popularityChange += intFromEvidence(rotation, "popularityBonus");
            reputationChange += intFromEvidence(rotation, "reputationBonus");
        }

        Object stageRaw = result.evidenceRef().get("stageMomentum");
        if (stageRaw instanceof Map<?, ?> stageMomentum) {
            popularityChange += intFromEvidence(stageMomentum, "popularityBonus");
            watchHeatChange += intFromEvidence(stageMomentum, "watchHeatBonus");
            reputationChange += intFromEvidence(stageMomentum, "reputationBonus");
        }

        Object comboRaw = result.evidenceRef().get("comboImpact");
        if (comboRaw instanceof Map<?, ?> combo && !comboImpactAlreadyIncludedInBase(result, combo)) {
            popularityChange += intFromEvidence(combo, "popularityBonus");
            watchHeatChange += intFromEvidence(combo, "watchHeatBonus");
            reputationChange += intFromEvidence(combo, "reputationBonus");
        }

        Object objectiveRaw = result.evidenceRef().get("stageObjectiveBonus");
        if (objectiveRaw instanceof Map<?, ?> objective) {
            popularityChange += intFromEvidence(objective, "popularityBonus");
            reputationChange += intFromEvidence(objective, "reputationBonus");
        }

        Object sprintRaw = result.evidenceRef().get("endgameSprint");
        if (sprintRaw instanceof Map<?, ?> sprint) {
            popularityChange += intFromEvidence(sprint, "popularityBonus");
        }

        Object platformTrendRaw = result.evidenceRef().get("platformTrendModifier");
        if (platformTrendRaw instanceof Map<?, ?> platformTrend) {
            popularityChange += intFromEvidence(platformTrend, "popularityBonus");
            watchHeatChange += intFromEvidence(platformTrend, "watchHeatBonus");
            reputationChange += intFromEvidence(platformTrend, "reputationBonus");
        }

        Object fortuneRaw = result.evidenceRef().get("fortuneModifier");
        if (fortuneRaw instanceof Map<?, ?> fortune) {
            popularityChange += intFromEvidence(fortune, "popularityBonus");
            watchHeatChange += intFromEvidence(fortune, "watchHeatBonus");
            reputationChange += intFromEvidence(fortune, "reputationBonus");
        }

        return new ActionLogDelta(popularityChange, watchHeatChange, reputationChange,
                base.memeChange() + memeEvidenceDelta(result));
    }

    private boolean comboImpactAlreadyIncludedInBase(ActionResultDTO result, Map<?, ?> combo) {
        Object comboKey = combo.get("comboKey");
        return ActionType.PUBLISH_CLIP.name().equals(result.actionType())
                && "VIDEO_TO_CLIP".equals(comboKey)
                && hasVideoToClipCombo(result.summary());
    }

    private int memeEvidenceDelta(ActionResultDTO result) {
        int memeChange = 0;
        Object platformTrendRaw = result.evidenceRef().get("platformTrendModifier");
        if (platformTrendRaw instanceof Map<?, ?> platformTrend) {
            memeChange += intFromEvidence(platformTrend, "memeBonus");
        }
        Object comboRaw = result.evidenceRef().get("comboImpact");
        if (comboRaw instanceof Map<?, ?> combo && !comboImpactAlreadyIncludedInBase(result, combo)) {
            memeChange += intFromEvidence(combo, "memeBonus");
        }
        return memeChange;
    }

    private int intFromEvidence(Map<?, ?> evidence, String key) {
        Object value = evidence.get(key);
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

    private ActionLogDelta logDeltaFor(ActionResultDTO result, int day) {
        String actionType = result.actionType();
        if (ActionType.PUBLISH_CLIP.name().equals(actionType)) {
            if (hasVideoToClipCombo(result.summary())) {
                if (day >= 15 && day <= 21) {
                    return new ActionLogDelta(48, 8, -2, 7);
                }
                return new ActionLogDelta(40, 5, -1, 5);
            }
            if (day >= 15 && day <= 21) {
                return new ActionLogDelta(36, 6, -2, 5);
            }
            return new ActionLogDelta(28, 3, -1, 2);
        }
        if (ActionType.PUBLISH_VIDEO.name().equals(actionType)) {
            if (day >= 8 && day <= 14) {
                return new ActionLogDelta(42, 4, 1, 1);
            }
            return new ActionLogDelta(35, 4, 0, 1);
        }
        if (ActionType.NPC_INTERACT.name().equals(actionType)) {
            return new ActionLogDelta(18, 0, 1, 0);
        }
        if (ActionType.TRAIN_SONG.name().equals(actionType)) {
            if (day >= 8 && day <= 14) {
                return new ActionLogDelta(20, 0, 2, 0);
            }
            return new ActionLogDelta(15, 0, 1, 0);
        }
        if (ActionType.TRAIN_DANCE.name().equals(actionType)) {
            return new ActionLogDelta(18, 2, 0, 2);
        }
        if (ActionType.STREAM_PLAN.name().equals(actionType)) {
            String summary = result.summary();
            Object titleStyle = result.evidenceRef().get("titleStyle");
            if (isDanceMemeSummary(summary)) {
                return new ActionLogDelta(140, 20, -3, 5);
            }
            if (isHardMouthSummary(summary) || isAbstractMemeSummary(summary)) {
                return new ActionLogDelta(140, 20, -3, 4);
            }
            if (isFanServiceSummary(summary)) {
                return new ActionLogDelta(100, 4, 1, 0);
            }
            if (summary.contains("商业") || summary.contains("醒目留言") || summary.contains("高亮互动") || summary.contains("谢SC")) {
                return new ActionLogDelta(140, 20, -3, 0);
            }
            if (hasVideoToStreamCombo(summary)) {
                return new ActionLogDelta(110, 8, 3, 1);
            }
            if (hasPracticeToStreamCombo(summary)) {
                return new ActionLogDelta(95, 6, 4, 0);
            }
            if ("SAFE".equals(titleStyle)) {
                if (summary.contains("歌回扶持周")) {
                    return new ActionLogDelta(95, 5, 4, 0);
                }
                return new ActionLogDelta(80, 5, 3, 0);
            }
            if (result.fanChange() == 44 && day >= 8 && day <= 14) {
                return new ActionLogDelta(95, 5, 4, 0);
            }
            if (result.fanChange() == 36) {
                return new ActionLogDelta(80, 5, 3, 0);
            }
            return new ActionLogDelta(140, 20, -3, 0);
        }
        if (ActionType.TRAIN_TALK.name().equals(actionType)) {
            if (day > 7) {
                return new ActionLogDelta(0, -1, 2, 0);
            }
            return new ActionLogDelta(0, -1, 3, 0);
        }
        if (ActionType.FAN_GROUP_MAINTAIN.name().equals(actionType)) {
            if (day >= 22 && day <= 30) {
                return new ActionLogDelta(0, -1, 3, 0);
            }
            return new ActionLogDelta(0, -1, 2, 0);
        }
        if (ActionType.REST.name().equals(actionType)) {
            return new ActionLogDelta(0, -1, 2, 0);
        }
        return new ActionLogDelta(0, 0, 1, 0);
    }

    private int commercialChangeFor(ActionResultDTO result) {
        int evidenceCommercial = commercialEvidenceDelta(result);
        Object titleStyle = result.evidenceRef().get("titleStyle");
        if (ActionType.FAN_GROUP_MAINTAIN.name().equals(result.actionType())
                && result.summary().contains("商业复审周")) {
            return 2 + evidenceCommercial;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType()) && titleStyle instanceof String style) {
            if ("FAN_SERVICE".equals(style)) {
                return (result.summary().contains("商业复审周") ? 6 : 4) + evidenceCommercial;
            }
            if ("BUSINESS_SAFE".equals(style)) {
                return (result.summary().contains("商业复审周") ? 4 : 3) + evidenceCommercial;
            }
            return evidenceCommercial;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType())
                && isFanServiceSummary(result.summary())) {
            return (result.summary().contains("商业复审周") ? 6 : 4) + evidenceCommercial;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType())
                && (isBusinessSafeSummary(result.summary()) || result.summary().contains("商业"))) {
            return (result.summary().contains("商业复审周") ? 4 : 3) + evidenceCommercial;
        }
        return evidenceCommercial;
    }

    private int coinChangeFor(ActionResultDTO result) {
        int productionCost = productionBudgetCoinCost(result);
        int evidenceCoin = coinEvidenceDelta(result);
        Object titleStyle = result.evidenceRef().get("titleStyle");
        if (ActionType.FAN_GROUP_MAINTAIN.name().equals(result.actionType())
                && result.summary().contains("商业复审周")) {
            return 200 + evidenceCoin - productionCost;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType()) && titleStyle instanceof String style) {
            if ("FAN_SERVICE".equals(style)) {
                return (result.summary().contains("商业复审周") ? 800 : 600) + evidenceCoin - productionCost;
            }
            if ("BUSINESS_SAFE".equals(style)) {
                return (result.summary().contains("商业复审周") ? 700 : 500) + evidenceCoin - productionCost;
            }
            return evidenceCoin - productionCost;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType())
                && isFanServiceSummary(result.summary())) {
            return (result.summary().contains("商业复审周") ? 800 : 600) + evidenceCoin - productionCost;
        }
        if (ActionType.STREAM_PLAN.name().equals(result.actionType())
                && (isBusinessSafeSummary(result.summary()) || result.summary().contains("商业"))) {
            return (result.summary().contains("商业复审周") ? 700 : 500) + evidenceCoin - productionCost;
        }
        return evidenceCoin - productionCost;
    }

    private int commercialEvidenceDelta(ActionResultDTO result) {
        Object platformTrendRaw = result.evidenceRef().get("platformTrendModifier");
        if (platformTrendRaw instanceof Map<?, ?> platformTrend) {
            return intFromEvidence(platformTrend, "commercialBonus");
        }
        return 0;
    }

    private int coinEvidenceDelta(ActionResultDTO result) {
        Object platformTrendRaw = result.evidenceRef().get("platformTrendModifier");
        if (platformTrendRaw instanceof Map<?, ?> platformTrend) {
            return intFromEvidence(platformTrend, "coinBonus");
        }
        return 0;
    }

    private int productionBudgetCoinCost(ActionResultDTO result) {
        Object raw = result.evidenceRef().get("productionBudget");
        if (raw instanceof Map<?, ?> budget) {
            return intFromEvidence(budget, "coinCost");
        }
        return 0;
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

    private boolean isCommercialReviewWeek(int day) {
        return day >= 22 && day <= 30;
    }

    private int endgameProductionCoinCost(ActionType actionType, int day) {
        if (!isCommercialReviewWeek(day)) {
            return 0;
        }
        return switch (actionType) {
            case PUBLISH_VIDEO -> balanceConfig.endgameVideoProductionCoinCost();
            case PUBLISH_CLIP -> balanceConfig.endgameClipBoostCoinCost();
            case NPC_INTERACT -> balanceConfig.endgameCollabBookingCoinCost();
            default -> 0;
        };
    }

    private String endgameProductionSummary(ActionType actionType, int coinCost) {
        return switch (actionType) {
            case PUBLISH_VIDEO -> "收官期投稿要约封面、剪辑和投流窗口，本手制作预算-" + coinCost + "。";
            case PUBLISH_CLIP -> "收官期切片要抢发布时间轴，本手推广预算-" + coinCost + "。";
            case NPC_INTERACT -> "收官期同台要排档期和补运营物料，本手活动预算-" + coinCost + "。";
            default -> "收官期制作预算-" + coinCost + "。";
        };
    }

    private boolean isSingingBoostWeek(int day) {
        return day >= 8 && day <= 14;
    }

    private int inspirationChangeFor(ActionResultDTO result) {
        int rotationBonus = 0;
        Object rotationRaw = result.evidenceRef().get("rotationBonus");
        if (rotationRaw instanceof Map<?, ?> rotation) {
            rotationBonus = intFromEvidence(rotation, "inspirationBonus");
        }
        if (ActionType.TRAIN_TALK.name().equals(result.actionType())
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(result.actionType())) {
            return 1 + rotationBonus;
        }
        if (ActionType.PUBLISH_VIDEO.name().equals(result.actionType())) {
            return -1 + rotationBonus;
        }
        return rotationBonus;
    }

    private ActionOptionDTO toOption(ActionConfig config, Vup vup, DaySession session, List<RiskDebt> openDebts, OperatingPressureService.PressureState pressureState) {
        String disabledReason = null;
        boolean enabled = true;
        OperatingPressureService.PressureReplacement replacement = OperatingPressureService.PressureReplacement.none();
        boolean lockedByPressure = operatingPressureService.isLocked(vup, session.getDay(), config.actionType);
        if (lockedByPressure) {
            enabled = false;
            disabledReason = "OPERATIONAL_PRESSURE_LOCKED";
            replacement = operatingPressureService.replacementForAction(vup, session.getDay(), config.actionType);
        } else if (!DayPhase.READY.name().equals(session.getPhase())) {
            enabled = false;
            disabledReason = "PHASE_NOT_ALLOWED";
        } else if (config.actionType == ActionType.STREAM_PLAN && session.isStreamPlanCancelled()) {
            enabled = false;
            disabledReason = "STREAM_PLAN_CANCELLED_TODAY";
        } else if (vup.getStamina() < config.staminaCost) {
            enabled = false;
            disabledReason = "INSUFFICIENT_STAMINA";
        } else if (config.actionType == ActionType.PUBLISH_VIDEO && vup.getInspiration() < 1) {
            enabled = false;
            disabledReason = "INSUFFICIENT_INSPIRATION";
        } else if (config.actionType == ActionType.PUBLISH_CLIP && materialStock(vup) < 1) {
            enabled = false;
            disabledReason = "INSUFFICIENT_MATERIAL_STOCK";
        } else {
            int productionCoinCost = endgameProductionCoinCost(config.actionType, session.getDay());
            if (productionCoinCost > 0 && vup.getCoin() < productionCoinCost) {
                enabled = false;
                disabledReason = "INSUFFICIENT_COIN";
            }
        }
        int productionCoinCost = endgameProductionCoinCost(config.actionType, session.getDay());
        String requirement = config.requirement;
        String effectPreview = config.effectPreview;
        String riskPreview = config.riskPreview;
        if (productionCoinCost > 0) {
            requirement = requirement + "，运营预算>=" + productionCoinCost;
            effectPreview = effectPreview + "；收官制作预算-" + productionCoinCost;
        }
        boolean clipMaterialBlocked = config.actionType == ActionType.PUBLISH_CLIP && materialStock(vup) < 1;
        if (clipMaterialBlocked) {
            requirement = requirement + "，切片素材>=1";
            effectPreview = "需要先补素材；发布视频、直播企划或粉丝群投稿征集会给切片组弹药";
            riskPreview = "硬等切片会断节奏；先补素材再剪，结局证据更稳";
        }
        if (!clipMaterialBlocked) {
            if (config.actionType == ActionType.TRAIN_TALK && vup.getDayCount() <= 7) {
                effectPreview = "杂谈力+1，灵感小涨；清朗低压周有平台口味加成，真爱粉和口碑小幅提高";
            } else if (config.actionType == ActionType.TRAIN_SONG && vup.getDayCount() >= 8 && vup.getDayCount() <= 14) {
                effectPreview = "歌力+1，真爱粉提高；歌回扶持周有平台口味加成，基本功内容更容易进推荐";
            } else if (config.actionType == ActionType.PUBLISH_VIDEO && vup.getDayCount() >= 8 && vup.getDayCount() <= 14) {
                effectPreview = "消耗1灵感，传播提高；歌回扶持周投稿更容易进推荐，粉丝、人气和口碑小幅提高";
            } else if (config.actionType == ActionType.PUBLISH_CLIP && vup.getDayCount() >= 15 && vup.getDayCount() <= 21) {
                effectPreview = "消耗1素材；抽象出圈周路人粉进场更快，粉丝、人气、热度和梗热度提高，口碑小幅下降";
            } else if (config.actionType == ActionType.FAN_GROUP_MAINTAIN && isCommercialReviewWeek(vup.getDayCount())) {
                effectPreview = "安抚老粉，灵感+1；商业复审周稳定排班更容易过审，运营预算、商业化和口碑小幅提高";
            }
        }
        // Add fatigue warning to preview
        com.example.vupworld.dto.ActionDtos.FatigueInfoDTO fatigueInfo = actionFatigueService.calculateFatigue(vup.getId(), config.actionType.name());
        if (fatigueInfo.fatigueHint() != null) {
            effectPreview = effectPreview + " ⚠ " + fatigueInfo.fatigueHint();
        }
        AudiencePressureService.AudiencePressure audiencePressure =
                audiencePressureService.pressureForAction(vup, session.getDay(), config.actionType, routeBiasType(config.actionType));
        if (audiencePressure.active()) {
            riskPreview = riskPreview + "；" + audiencePressure.label() + "：" + audiencePressure.hint();
        }
        StageObjectiveService.StageMomentum stageMomentum = stageObjectiveService.stageMomentumFor(vup);
        StageMomentumDelta stageMomentumDelta = stageMomentumDeltaFor(stageMomentum, config.actionType.name(), routeBiasType(config.actionType));
        if (stageMomentumDelta.applied()) {
            if (stageMomentumDelta.watchHeatChange() > 0 && stageMomentumDelta.trueFanChange() == 0) {
                riskPreview = riskPreview + "；阶段欠账：" + stageMomentum.hint();
            } else {
                effectPreview = effectPreview + "；阶段动量：" + stageMomentum.hint();
            }
        }
        DebtService.DebtActionPreview debtPreview = debtService.previewForAction(openDebts, session, config.actionType.name());
        if (debtPreview.active()) {
            if (debtPreview.effectLine() != null && !debtPreview.effectLine().isBlank()) {
                effectPreview = effectPreview + "；" + debtPreview.effectLine();
            }
            if (debtPreview.riskLine() != null && !debtPreview.riskLine().isBlank()) {
                riskPreview = riskPreview + "；" + debtPreview.riskLine();
            }
        }
        PlatformTrendModifier platformTrendPreview =
                platformTrendModifierFor(session.getDay(), config.actionType, routeBiasType(config.actionType), null, null);
        if (platformTrendPreview.applied() && !effectPreview.contains("平台口味")) {
            if (platformTrendPreview.matched()) {
                effectPreview = effectPreview + "；平台顺风：" + platformTrendPreview.primaryDeltaLabel();
            } else {
                riskPreview = riskPreview + "；平台逆风：" + platformTrendPreview.primaryDeltaLabel();
            }
        }
        FortuneModifier fortunePreview = fortuneModifierFor(vup, session, config.actionType, isHighRiskAction(config.actionType));
        if (fortunePreview.applied()) {
            if (fortunePreview.matched()) {
                effectPreview = effectPreview + "；今日运势：" + fortunePreview.primaryDeltaLabel();
            } else {
                riskPreview = riskPreview + "；今日运势：" + fortunePreview.primaryDeltaLabel();
            }
        }
        EndgameSprintBonus sprintPreview = endgameSprintBonusFor(vup.getCurrentRoute(), session.getDay(), config.actionType, sprintRouteType(config.actionType));
        if (sprintPreview.active()) {
            effectPreview = effectPreview + "；收官押线：真爱粉+"
                    + sprintPreview.trueFanBonus() + "，路线分+"
                    + sprintPreview.routeScoreBonus();
        }
        StageObjectiveBonus stageObjectivePreview =
                stageObjectiveBonusFor(vup, session, config.actionType, routeBiasType(config.actionType));
        if (stageObjectivePreview.applied()) {
            effectPreview = effectPreview + "；阶段委托：真爱粉+"
                    + stageObjectivePreview.trueFanBonus()
                    + (stageObjectivePreview.popularityBonus() > 0 ? "，人气+" + stageObjectivePreview.popularityBonus() : "")
                    + (stageObjectivePreview.routeScoreBonus() > 0 ? "，路线分+" + stageObjectivePreview.routeScoreBonus() : "");
        }
        if (pressureState != null && pressureState.lockedActionTypes() != null && pressureState.lockedActionTypes().contains(config.actionType.name())) {
            effectPreview = effectPreview + "；压力状态：" + pressureState.stateLabel();
            if (pressureState.woundLeft() > 0) {
                riskPreview = riskPreview + "；带伤恢复还在进行中，收益会更紧。";
            }
        }
        ComboPreview comboPreview = comboPreviewFor(vup, session, config.actionType);
        String comboKey = comboPreview == null ? null : comboPreview.comboKey();
        String comboLabel = comboPreview == null ? null : comboPreview.comboLabel();
        String comboHint = comboPreview == null ? null : comboPreview.comboHint();
        return new ActionOptionDTO(
                config.actionType.name(),
                config.name,
                config.staminaCost,
                requirement,
                effectPreview,
                mainActionCostPreview(config, requirement, productionCoinCost),
                riskPreview,
                pressureState == null ? null : pressureState.stateLabel(),
                pressureState == null ? null : pressureState.lockedGroupLabel(),
                pressureState == null ? null : pressureState.hint(),
                pressureState == null ? 0 : pressureState.cooldownLeft(),
                pressureState == null ? 0 : pressureState.woundLeft(),
                pressureState != null && pressureState.wounded(),
                replacement.actionType(),
                replacement.label(),
                replacement.staminaCost(),
                replacement.inspirationCost(),
                replacement.reputationCost(),
                replacement.hint(),
                routeBiasType(config.actionType),
                routeBiasLabel(routeBiasType(config.actionType)),
                riskLevel(config.actionType),
                planArchetype(config.actionType),
                recommendedReason(config.actionType, vup),
                tempoHint(config.actionType),
                routeFocusHint(config.actionType, vup),
                comboKey,
                comboLabel,
                comboHint,
                enabled,
                disabledReason,
                config.actionType.actionPointCost()
        );
    }

    /**
     * 主行动代价预览：从体力消耗、前置要求和基础风险标签综合生成一行文案，
     * 与下播行动 OffStreamConfig.costPreview 字段口径保持一致。
     * 注意这里只取 config 的基础风险标签，不重复 riskPreview 已拼接的临时加成，避免与 riskPreview 字段冗余。
     */
    private String mainActionCostPreview(ActionConfig config, String requirement, int productionCoinCost) {
        java.util.List<String> pieces = new java.util.ArrayList<>();
        if (config.staminaCost > 0) {
            pieces.add("体力-" + config.staminaCost);
        }
        if (productionCoinCost > 0) {
            pieces.add("运营预算-" + productionCoinCost);
        }
        if (requirement != null && !requirement.isBlank() && !"始终可用".equals(requirement)) {
            pieces.add(requirement);
        }
        if (config.riskPreview != null && !config.riskPreview.isBlank()) {
            pieces.add(config.riskPreview);
        }
        if (pieces.isEmpty()) {
            return "无明显即时代价";
        }
        return String.join("，", pieces);
    }

    private String routeFocusHint(ActionType actionType, Vup vup) {
        String actionRoute = routeBiasType(actionType);
        String currentRoute = vup.getCurrentRoute();
        int currentRouteScore = routeScoreFor(vup, currentRoute);
        int actionRouteScore = routeScoreFor(vup, actionRoute);
        if ("UNKNOWN".equals(actionRoute)) {
            return currentRouteScore >= 12
                    ? "防守回合：不加新路线，但能保护已有证据链。"
                    : "稳盘回合：先保状态，路线证据暂时不会变厚。";
        }
        if (currentRoute == null || currentRoute.isBlank() || "UNKNOWN".equals(currentRoute)) {
            return "定线尝试：这手会把路线往【" + routeBiasLabel(actionRoute) + "】推。";
        }
        if (actionRoute.equals(currentRoute)) {
            if (currentRouteScore >= 18) {
                return "路线专精：继续巩固主线，结局复盘会更稳定。";
            }
            if (currentRouteScore >= 10) {
                return "路线加固：同方向补证据，降低收官摇摆。";
            }
            return "路线成型：连续做同类行动会更快定调。";
        }
        if (currentRouteScore >= 16 && actionRouteScore <= currentRouteScore / 2) {
            return "转线风险：当前主线已厚，临时换向会稀释结局证据。";
        }
        if (actionRouteScore >= currentRouteScore - 2) {
            return "双线试探：这手能测试副路线，但收官前要做取舍。";
        }
        return "路线偏移：收益可拿，但会把证据写到另一条路线上。";
    }

    private int routeScoreFor(Vup vup, String routeType) {
        if (routeType == null || routeType.isBlank()) {
            return 0;
        }
        Object raw;
        try {
            raw = jsonService.readMap(vup.getRouteScoreJson()).get(routeType);
        } catch (IllegalStateException ignored) {
            return 0;
        }
        return raw instanceof Number number ? number.intValue() : 0;
    }

    private ComboPreview comboPreviewFor(Vup vup, DaySession session, ActionType actionType) {
        List<BusinessLog> logs = recentComboLogs(vup);
        int currentDay = session.getDay();
        boolean training = isTrainingAction(actionType.name());
        if (training && recentActionCount(logs, currentDay, 6,
                ActionType.TRAIN_SONG.name(), ActionType.TRAIN_DANCE.name(), ActionType.TRAIN_TALK.name()) >= 2) {
            return new ComboPreview(
                    "HARD_PRACTICE",
                    "刻苦练习",
                    "连招预告：一周内多次练功会沉淀成路线证据，本次训练可追加路线分和口碑。"
            );
        }
        if (actionType != ActionType.REST && consecutivePreviousActions(logs, currentDay, ActionType.REST.name(), 2)) {
            return new ComboPreview(
                    "CHARGE_COMPLETE",
                    "充电完毕",
                    "连招预告：连续休息后再营业，状态回弹，本次行动更容易稳住粉丝和口碑。"
            );
        }
        if (actionType == ActionType.REST && recentActionCount(logs, currentDay, 6, ActionType.REST.name()) >= 3) {
            return new ComboPreview(
                    "FORGOTTEN",
                    "被遗忘",
                    "警报预告：低压太久会掉出首页推荐，继续休息会明显损失人气。"
            );
        }
        if (actionType == ActionType.STREAM_PLAN
                && recentActionCount(logs, currentDay, 6, ActionType.STREAM_PLAN.name()) >= 2) {
            return new ComboPreview(
                    "OVERWORK",
                    "过劳边缘",
                    "警报预告：最近直播太密，继续开播会推高围观压力，并让口碑承压。"
            );
        }
        return switch (actionType) {
            case STREAM_PLAN -> streamComboPreview(logs, currentDay);
            case PUBLISH_CLIP -> videoToClipPreview(logs, currentDay);
            default -> null;
        };
    }

    private List<BusinessLog> recentComboLogs(Vup vup) {
        List<BusinessLog> logs = new ArrayList<>(businessLogMapper.findRecentByVupId(vup.getId(), 30));
        logs.sort(Comparator.comparingInt(BusinessLog::getDay).thenComparing(BusinessLog::getId));
        return logs;
    }

    private ComboPreview streamComboPreview(List<BusinessLog> logs, int currentDay) {
        if (recentActionCount(logs, currentDay, 7, ActionType.NPC_INTERACT.name()) >= 1) {
            return new ComboPreview(
                    "SOCIAL_WARMUP",
                    "查房预热",
                    "连招预告：刚在同行场子露过脸，开播更像有人把门推开，DD和人气都会更快进场。"
            );
        }
        BusinessLog recentPractice = latestRecentLog(logs, 3,
                ActionType.TRAIN_SONG.name(), ActionType.TRAIN_DANCE.name(), ActionType.TRAIN_TALK.name());
        if (recentPractice != null) {
            return new ComboPreview(
                    "PRACTICE_TO_STREAM",
                    "练习后直播",
                    "连招预告：练完基本功再开播，观众更容易听出进步。"
            );
        }
        BusinessLog recentVideo = latestRecentLog(logs, 3, ActionType.PUBLISH_VIDEO.name());
        if (recentVideo != null) {
            return new ComboPreview(
                    "VIDEO_TO_STREAM",
                    "视频引流",
                    "连招预告：先把可转发素材铺出去，再开播接住回流。"
            );
        }
        return null;
    }

    private ComboPreview videoToClipPreview(List<BusinessLog> logs, int currentDay) {
        boolean active = logs.stream()
                .filter(log -> ActionType.PUBLISH_VIDEO.name().equals(log.getAction()))
                .anyMatch(video -> video.getDay() < currentDay
                        && currentDay - video.getDay() <= 7
                        && logs.stream().noneMatch(log -> ActionType.PUBLISH_CLIP.name().equals(log.getAction())
                        && log.getDay() > video.getDay()
                        && log.getDay() < currentDay));
        if (!active) {
            return null;
        }
        return new ComboPreview(
                "VIDEO_TO_CLIP",
                "一鱼两剪",
                "连招预告：长视频给素材，切片再把能传播的三十秒抠出来。"
        );
    }

    private BusinessLog latestRecentLog(List<BusinessLog> logs, int maxDayGap, String... actions) {
        if (logs.isEmpty()) {
            return null;
        }
        int currentDay = logs.get(logs.size() - 1).getDay() + 1;
        List<String> actionList = List.of(actions);
        for (int i = logs.size() - 1; i >= 0; i--) {
            BusinessLog log = logs.get(i);
            if (currentDay - log.getDay() > maxDayGap) {
                break;
            }
            if (actionList.contains(log.getAction())) {
                return log;
            }
        }
        return null;
    }

    private String routeBiasType(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_SONG -> "SINGING_IDOL";
            case TRAIN_DANCE -> "DANCE_MEME";
            case TRAIN_TALK, FAN_GROUP_MAINTAIN -> "ELECTRONIC_PICKLE";
            case REST -> "UNKNOWN";
            case STREAM_PLAN -> "BLACK_RED_MAIN_STAGE";
            case PUBLISH_VIDEO, PUBLISH_CLIP -> "SLICE_SAINT";
            case NPC_INTERACT -> "SOCIAL_COLLAB";
        };
    }

    private String sprintRouteType(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_TALK, FAN_GROUP_MAINTAIN -> "ELECTRONIC_PICKLE";
            case REST -> "UNKNOWN";
            default -> routeBiasType(actionType);
        };
    }

    private String routeBiasLabel(String routeType) {
        return switch (routeType) {
            case "SINGING_IDOL" -> "歌势路线";
            case "DANCE_MEME" -> "梗舞整活";
            case "SLICE_SAINT" -> "切片供货";
            case "SOCIAL_COLLAB" -> "社交联动";
            case "BLACK_RED_MAIN_STAGE" -> "主会场黑红";
            case "MAIN_STAGE_KING" -> "主会场之王";
            case "CYBER_GIRLFRIEND" -> "陪伴路线";
            case "GLORIOUS_GRADUATION" -> "光荣毕业";
            case "DD_BUS_STOP" -> "DD公交站";
            case "ELECTRONIC_PICKLE" -> "电子榨菜";
            default -> "稳盘防守";
        };
    }

    private String riskLevel(ActionType actionType) {
        return switch (actionType) {
            case STREAM_PLAN, PUBLISH_CLIP, NPC_INTERACT -> "HIGH";
            case PUBLISH_VIDEO, TRAIN_TALK, FAN_GROUP_MAINTAIN -> "MEDIUM";
            case TRAIN_SONG, TRAIN_DANCE, REST -> "LOW";
        };
    }

    private String planArchetype(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_TALK, FAN_GROUP_MAINTAIN, REST -> "SAFE_BUSINESS";
            case STREAM_PLAN, PUBLISH_CLIP, NPC_INTERACT -> "DANGER_BURST";
            default -> "ROUTE_PROGRESS";
        };
    }

    private String recommendedReason(ActionType actionType, Vup vup) {
        RecommendationContext context = recommendationContext(vup);
        if (context.needsRecovery()) {
            return switch (actionType) {
                case FAN_GROUP_MAINTAIN -> "主推：旧账或口碑压力在场，先稳住老粉和粉丝群，把事件复盘写得住。";
                case TRAIN_TALK -> "主推：用杂谈复盘给转型和旧账补上下文，顺手攒灵感。";
                case REST -> "主推：体力或热度压力偏高，防守一回合比硬冲更值。";
                case STREAM_PLAN -> "高波动：当前先看债务和口碑，开播要选低风险标题。";
                case PUBLISH_CLIP -> materialStock(vup) < 1
                        ? "缺切片素材：素材库为0，今天先发布视频、直播企划或粉丝群投稿征集。"
                        : "可收割，但风险面板亮灯时容易把旧账剪成新账。";
                default -> withRestartTargetCue(routeRecommendation(actionType, vup, context), context);
            };
        }
        return withRestartTargetCue(routeRecommendation(actionType, vup, context), context);
    }

    private String withRestartTargetCue(String reason, RecommendationContext context) {
        if (!context.restartTargetActive() || reason == null || reason.isBlank()) {
            return reason;
        }
        if (reason.startsWith("复活赛目标")) {
            return reason;
        }
        if (reason.startsWith("主推：")) {
            return "复活赛目标：" + routeBiasLabel(context.targetRoute()) + "，" + reason.substring("主推：".length());
        }
        return "复活赛目标：" + routeBiasLabel(context.targetRoute()) + "，" + reason;
    }

    private String routeRecommendation(ActionType actionType, Vup vup, RecommendationContext context) {
        String route = context.targetRoute();
        if (ActionType.PUBLISH_CLIP == actionType && materialStock(vup) < 1) {
            return "缺切片素材：素材库为0，先发布视频、直播企划或粉丝群投稿征集；下一天再切才有证据。";
        }
        if (ActionType.PUBLISH_VIDEO == actionType && vup.getInspiration() < 1) {
            return "灵感不足，先杂谈复盘或粉丝群维护，把投稿素材写出来。";
        }
        if ("SINGING_IDOL".equals(route)) {
            return switch (actionType) {
                case TRAIN_SONG -> "主推：歌势路线缺基本功证据，练歌会直接补结局门槛。";
                case STREAM_PLAN -> "主推：歌势证据已成型，开歌回能把练习兑现给观众。";
                case PUBLISH_VIDEO -> "主推：把歌势证据投出去，给算法和后续直播铺路。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("SLICE_SAINT".equals(route) || "DANCE_MEME".equals(route)) {
            return switch (actionType) {
                case PUBLISH_VIDEO -> "主推：切片路线先补素材，长视频会给二剪和日报留下证据。";
                case PUBLISH_CLIP -> "主推：素材库存可用，今天适合把乐子人和切片证据接起来。";
                case TRAIN_DANCE -> "主推：梗舞路线需要动作素材，练舞能让后续切片更有根。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("SOCIAL_COLLAB".equals(route) || "DD_BUS_STOP".equals(route)) {
            return switch (actionType) {
                case NPC_INTERACT -> "主推：观众期待偏同台，互动能补DD公交站和社交路线证据。";
                case FAN_GROUP_MAINTAIN -> "主推：联动前先安抚老粉，减少端水带来的期待落差。";
                case STREAM_PLAN -> "主推：同台预热后开播，能把DD回流接进主循环。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("BLACK_RED_MAIN_STAGE".equals(route) || "MAIN_STAGE_KING".equals(route)) {
            return switch (actionType) {
                case FAN_GROUP_MAINTAIN -> "主推：主会场路线先稳米线，避免热度直接变欠账。";
                case TRAIN_TALK -> "主推：补回应话术和上下文，让高热度不只剩开庭素材。";
                case PUBLISH_CLIP -> "主推：热度够时切片能收割，但要确认旧账不会回旋。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("CYBER_GIRLFRIEND".equals(route)) {
            return switch (actionType) {
                case TRAIN_TALK -> "主推：陪伴路线先练话术，别把亲密感直接做成压力。";
                case FAN_GROUP_MAINTAIN -> "主推：粉丝服务需要边界说明，先稳住群内期待。";
                case STREAM_PLAN -> "主推：醒目留言回应或陪伴回能推进路线，但商业味要控住。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("GLORIOUS_GRADUATION".equals(route)) {
            return switch (actionType) {
                case FAN_GROUP_MAINTAIN -> "主推：光荣毕业要少欠账和高口碑，今天先做告别排班。";
                case TRAIN_TALK -> "主推：用复盘把路线证据讲清楚，给最终收束留体面。";
                case REST -> "主推：冲刺期低压收束，避免最后几天把风险抬头。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        if ("ELECTRONIC_PICKLE".equals(route)) {
            return switch (actionType) {
                case TRAIN_TALK -> "主推：电子榨菜路线要稳定陪伴感，杂谈复盘能稳口碑和陪饭证据。";
                case FAN_GROUP_MAINTAIN -> "主推：低压陪饭要靠老粉留存，粉丝群维护能减少掉线感。";
                case REST -> "主推：低压路线不能硬冲过热，休息能保住长期陪伴节奏。";
                default -> baselineRecommendation(actionType, vup, context);
            };
        }
        return baselineRecommendation(actionType, vup, context);
    }

    private String baselineRecommendation(ActionType actionType, Vup vup, RecommendationContext context) {
        return switch (actionType) {
            case TRAIN_SONG -> context.day() <= 14
                    ? "新人期补歌势基本功，后续歌回和投稿更有证据。"
                    : "补歌力能转向歌势，但要用复盘解释路线变化。";
            case TRAIN_DANCE -> "给梗舞路线蓄力，适合等素材和切片接上。";
            case TRAIN_TALK -> "稳住直播间气压，顺手攒灵感，是转型和防守的通用垫步。";
            case STREAM_PLAN -> "开直播爆点，但标题会决定收益和债务。";
            case PUBLISH_VIDEO -> "把路线证据投出去，给切片和算法供素材。";
            case PUBLISH_CLIP -> materialStock(vup) < 1
                    ? "缺切片素材：先发视频、开直播企划或收投稿；素材库有货再剪。"
                    : "用库存素材换传播，乐子人会更快进场。";
            case FAN_GROUP_MAINTAIN -> "稳住老粉和粉丝群，把小作文风险压下去。";
            case NPC_INTERACT -> "同台扩圈接DD，但要小心独角兽压力。";
            case REST -> "防守回合，恢复体力并降低硬冲风险。";
        };
    }

    private RecommendationContext recommendationContext(Vup vup) {
        List<RiskDebt> openDebts = debtService.openDebts(vup);
        int severeDebtCount = (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 3).count();
        String targetRoute = targetRouteForRecommendation(vup, openDebts);
        boolean restartTargetActive = restartTargetApplies(vup, openDebts);
        boolean needsRecovery = vup.getStamina() <= 2
                || vup.getReputation() < 45
                || vup.getWatchHeat() >= 65
                || severeDebtCount > 0;
        return new RecommendationContext(targetRoute, vup.getDayCount(), openDebts.size(), severeDebtCount, needsRecovery, restartTargetActive);
    }

    private String targetRouteForRecommendation(Vup vup, List<RiskDebt> openDebts) {
        String restartTarget = restartTargetType(vup);
        if (restartTarget != null && restartTargetApplies(vup, openDebts)) {
            return restartTarget;
        }
        if (vup.getMemeLevel() > 80 && vup.getReputation() < 30 && !openDebts.isEmpty()) {
            return "MAIN_STAGE_KING";
        }
        if (vup.getCurrentRoute() != null && !"UNKNOWN".equals(vup.getCurrentRoute())) {
            return vup.getCurrentRoute();
        }
        if (vup.getSongPower() >= vup.getDancePower() && vup.getSongPower() >= vup.getPlanPower()) {
            return "SINGING_IDOL";
        }
        if (vup.getPlanPower() > 0 || materialStock(vup) > 0) {
            return "SLICE_SAINT";
        }
        return "ELECTRONIC_PICKLE";
    }

    private boolean restartTargetApplies(Vup vup, List<RiskDebt> openDebts) {
        String restartTarget = restartTargetType(vup);
        if (restartTarget == null || "UNKNOWN".equals(restartTarget) || vup.getDayCount() > 14) {
            return false;
        }
        int severeDebtCount = (int) openDebts.stream().filter(debt -> debt.getSeverity() >= 4).count();
        return severeDebtCount == 0 && vup.getReputation() >= 35 && vup.getWatchHeat() < 80;
    }

    private String restartTargetType(Vup vup) {
        if (vup.getExpectationJson() == null || vup.getExpectationJson().isBlank()) {
            return null;
        }
        Object raw;
        try {
            raw = jsonService.readMap(vup.getExpectationJson()).get("restartTargetType");
        } catch (IllegalStateException ignored) {
            return null;
        }
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "SINGING_IDOL", "SLICE_SAINT", "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING",
                 "CYBER_GIRLFRIEND", "GLORIOUS_GRADUATION", "ELECTRONIC_PICKLE", "DD_BUS_STOP",
                 "SOCIAL_COLLAB", "DANCE_MEME", "UNKNOWN" -> value;
            default -> null;
        };
    }

    private String tempoHint(ActionType actionType) {
        return switch (actionType) {
            case TRAIN_SONG, TRAIN_DANCE -> "前期铺垫";
            case TRAIN_TALK, FAN_GROUP_MAINTAIN, REST -> "防守调整";
            case STREAM_PLAN -> "高波动推进";
            case PUBLISH_VIDEO -> "素材启动";
            case PUBLISH_CLIP -> "爆点收割";
            case NPC_INTERACT -> "扩圈试探";
        };
    }

    private int materialStock(Vup vup) {
        return businessLogMapper.countMaterialStockByVupId(vup.getId());
    }

    private ActionType parseAction(String raw) {
        try {
            return ActionType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new GameException("CONFIG_FIELD_INVALID", "未知行动类型。");
        }
    }

    private String normalizeNpcTendency(String raw) {
        if (raw == null || raw.isBlank()) {
            return NPC_TENDENCY_RAID;
        }
        String tendency = raw.trim();
        if (NPC_TENDENCY_RAID.equals(tendency)
                || NPC_TENDENCY_COLLAB.equals(tendency)
                || NPC_TENDENCY_BORROW_HEAT.equals(tendency)
                || NPC_TENDENCY_AVOID.equals(tendency)) {
            return tendency;
        }
        throw new GameException("CONFIG_FIELD_INVALID", "未知同台互动倾向。");
    }

    private String requirePlanType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new GameException("CONFIG_FIELD_INVALID", "直播企划必须提供 planType。");
        }
        return raw.trim();
    }

    private boolean isDanceMemePlan(Long planId) {
        return planId != null && (planId == 3L || planId == 11L);
    }

    private boolean isSingingPlan(DaySession session) {
        return session.getSelectedPlanId() != null && session.getSelectedPlanId() == 2L;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<ActionConfig> catalog() {
        return List.of(
                new ActionConfig(ActionType.TRAIN_SONG, "练歌", 2, "体力>=2", "歌力↑ 粉丝↑", "低风险"),
                new ActionConfig(ActionType.TRAIN_DANCE, "练舞", 2, "体力>=2", "舞力↑ 粉丝↑", "低风险"),
                new ActionConfig(ActionType.TRAIN_TALK, "杂谈", 2, "体力>=2", "口才↑ 灵感↑", "内容焦虑"),
                new ActionConfig(ActionType.STREAM_PLAN, "直播企划", 3, "体力>=3", "直播！高收益高风险", "翻车可能"),
                new ActionConfig(ActionType.PUBLISH_VIDEO, "发布视频", 4, "体力>=4", "传播↑ 路线↑", "平台口味"),
                new ActionConfig(ActionType.PUBLISH_CLIP, "发布切片", 1, "体力>=1", "热度↑ 粉丝↑", "标题党反噬"),
                new ActionConfig(ActionType.FAN_GROUP_MAINTAIN, "粉丝群维护", 2, "体力>=2", "粉丝稳定 风险↓", "过度会急"),
                new ActionConfig(ActionType.NPC_INTERACT, "同台互动", 2, "体力>=2", "粉丝↑ 影响力↑", "陪伴边界压力"),
                new ActionConfig(ActionType.REST, "休息", 0, "始终可用", "体力恢复 风险↓", "收益低")
        );
    }

    private enum ScheduleIntensity {
        LIGHT,
        STANDARD,
        SPRINT
    }

    private record ScheduleSlotPlan(
            String slotKey,
            ActionType actionType,
            ScheduleIntensity intensity
    ) {
    }

    private record ScheduleSlotOutcome(
            String planKey,
            String slotKey,
            String slotLabel,
            ActionType actionType,
            String actionName,
            ScheduleIntensity intensity,
            int staminaChange,
            int trueFanChange,
            int funFanChange,
            int unicornFanChange,
            int ddFanChange,
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange,
            int commercialChange,
            int coinChange,
            int inspirationChange,
            int materialChange,
            int attributeChange,
            int routeScoreChange,
            String routeType,
            String summary
    ) {
        int fanChange() {
            return trueFanChange + funFanChange + unicornFanChange + ddFanChange;
        }
    }

    private record ActionConfig(
            ActionType actionType,
            String name,
            int staminaCost,
            String requirement,
            String effectPreview,
            String riskPreview
    ) {
    }

    private record ActionLogDelta(
            int popularityChange,
            int watchHeatChange,
            int reputationChange,
            int memeChange
    ) {
    }

    record StageMomentumDelta(
            int trueFanChange,
            int popularityChange,
            int routeScoreChange,
            int watchHeatChange,
            int reputationChange
    ) {
        static StageMomentumDelta empty() {
            return new StageMomentumDelta(0, 0, 0, 0, 0);
        }

        boolean applied() {
            return trueFanChange != 0
                    || popularityChange != 0
                    || routeScoreChange != 0
                    || watchHeatChange != 0
                    || reputationChange != 0;
        }
    }

    private record RotationBonus(
            boolean active,
            String previousLane,
            String currentLane,
            String kind,
            int trueFanBonus,
            int popularityBonus,
            int reputationBonus,
            int inspirationBonus,
            String hint
    ) {
        static RotationBonus empty() {
            return new RotationBonus(false, "", "", "", 0, 0, 0, 0, "");
        }
    }

    private record EndgameSprintBonus(
            boolean active,
            String currentRoute,
            String actionRoute,
            int trueFanBonus,
            int popularityBonus,
            int routeScoreBonus,
            String hint
    ) {
        static EndgameSprintBonus empty() {
            return new EndgameSprintBonus(false, "", "", 0, 0, 0, "");
        }
    }

    record PlatformTrendModifier(
            boolean active,
            String trendId,
            String label,
            boolean matched,
            int trueFanBonus,
            int funFanBonus,
            int ddFanBonus,
            int popularityBonus,
            int watchHeatBonus,
            int reputationBonus,
            int memeBonus,
            int coinBonus,
            int commercialBonus,
            int routeScoreBonus,
            String hint,
            String primaryDeltaLabel
    ) {
        static PlatformTrendModifier empty(String trendId, String label) {
            return new PlatformTrendModifier(false, trendId, label, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", "");
        }

        boolean applied() {
            return active
                    && (trueFanBonus != 0
                    || funFanBonus != 0
                    || ddFanBonus != 0
                    || popularityBonus != 0
                    || watchHeatBonus != 0
                    || reputationBonus != 0
                    || memeBonus != 0
                    || coinBonus != 0
                    || commercialBonus != 0
                    || routeScoreBonus != 0);
        }
    }

    record FortuneModifier(
            boolean active,
            String fortune,
            String luckyAction,
            boolean matched,
            int trueFanBonus,
            int popularityBonus,
            int watchHeatBonus,
            int reputationBonus,
            int routeScoreBonus,
            String hint,
            String primaryDeltaLabel
    ) {
        static FortuneModifier empty(String fortune, String luckyAction) {
            return new FortuneModifier(false, fortune, luckyAction, false, 0, 0, 0, 0, 0, "", "");
        }

        boolean applied() {
            return active
                    && (trueFanBonus != 0
                    || popularityBonus != 0
                    || watchHeatBonus != 0
                    || reputationBonus != 0
                    || routeScoreBonus != 0);
        }
    }

    record StageObjectiveBonus(
            boolean active,
            String objectiveActionType,
            String objectiveTitle,
            String targetRoute,
            int trueFanBonus,
            int popularityBonus,
            int reputationBonus,
            int routeScoreBonus,
            int streak,
            String hint,
            String primaryDeltaLabel
    ) {
        static StageObjectiveBonus empty(String objectiveActionType, String objectiveTitle) {
            return new StageObjectiveBonus(false, objectiveActionType, objectiveTitle, "", 0, 0, 0, 0, 0, "", "");
        }

        boolean applied() {
            return active
                    && (trueFanBonus != 0
                    || popularityBonus != 0
                    || reputationBonus != 0
                    || routeScoreBonus != 0);
        }
    }

    record StageObjectiveSnapshot(
            String objectiveActionType,
            String objectiveTitle,
            int totalCount,
            List<StageObjectiveService.StageObjectiveTarget> targets
    ) {
    }

    record ComboImpact(
            boolean active,
            String comboKey,
            String label,
            String targetRoute,
            int trueFanBonus,
            int funFanBonus,
            int ddFanBonus,
            int popularityBonus,
            int watchHeatBonus,
            int reputationBonus,
            int memeBonus,
            int routeScoreBonus,
            String hint,
            String primaryDeltaLabel
    ) {
        static ComboImpact empty() {
            return new ComboImpact(false, "", "", "", 0, 0, 0, 0, 0, 0, 0, 0, "", "");
        }

        boolean applied() {
            return active
                    && (trueFanBonus != 0
                    || funFanBonus != 0
                    || ddFanBonus != 0
                    || popularityBonus != 0
                    || watchHeatBonus != 0
                    || reputationBonus != 0
                    || memeBonus != 0
                    || routeScoreBonus != 0);
        }
    }

    private record ComboPreview(
            String comboKey,
            String comboLabel,
            String comboHint
    ) {
    }

    private record RecommendationContext(
            String targetRoute,
            int day,
            int openDebtCount,
            int severeDebtCount,
            boolean needsRecovery,
            boolean restartTargetActive
    ) {
    }

    private enum MemeFatigueState {
        NORMAL(100),
        REPEAT(70),
        HEAVY_REPEAT(40),
        BOOMERANG(0);

        private final int percent;

        MemeFatigueState(int percent) {
            this.percent = percent;
        }

        int apply(int base) {
            return base * percent / 100;
        }
    }

    private MemeFatigueState calculateMemeFatigue(Long vupId, int currentDay) {
        String memeSubtype = "CLIP_SPREAD";
        var recentLogs = businessLogMapper.findRecentClipLogsBySubtype(vupId, memeSubtype);
        if (recentLogs.isEmpty()) {
            return MemeFatigueState.NORMAL;
        }
        BusinessLog lastClip = recentLogs.get(0);
        if (hasFreshClipMaterialAfter(vupId, lastClip.getDay(), currentDay)) {
            return MemeFatigueState.NORMAL;
        }
        int daysSinceLastClip = currentDay - lastClip.getDay();
        if (daysSinceLastClip > 7) {
            return MemeFatigueState.BOOMERANG;
        }

        long sameMemeUsesInWindow = recentLogs.stream()
                .filter(log -> currentDay - log.getDay() <= 7)
                .count();
        if (sameMemeUsesInWindow >= 2) {
            return MemeFatigueState.HEAVY_REPEAT;
        }
        if (sameMemeUsesInWindow == 1) {
            return MemeFatigueState.REPEAT;
        }
        return MemeFatigueState.NORMAL;
    }

    private boolean hasFreshClipMaterialAfter(Long vupId, int lastClipDay, int currentDay) {
        return businessLogMapper.findRecentByVupId(vupId, 10).stream()
                .filter(log -> log.getDay() > lastClipDay && log.getDay() <= currentDay)
                .anyMatch(this::isClipMaterialSource);
    }

    private boolean isClipMaterialSource(BusinessLog log) {
        return ActionType.PUBLISH_VIDEO.name().equals(log.getAction())
                || ActionType.FAN_GROUP_MAINTAIN.name().equals(log.getAction())
                || ("FAN_TOPIC".equals(log.getAction()) && "CLIP_SUBMISSION_STOCK".equals(log.getMemeSubtype()));
    }

    private String memeSubtypeFor(ActionResultDTO result) {
        if ("PUBLISH_CLIP".equals(result.actionType())) {
            return "CLIP_SPREAD";
        }
        if ("PUBLISH_VIDEO".equals(result.actionType())) {
            return "VIDEO_PUBLISH";
        }
        return null;
    }
}
