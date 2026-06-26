package com.example.vupworld.service.event;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionChoiceDTO;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;
import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PendingInteractionPresenter {
    public PendingInteractionDTO pending(DaySession session) {
        if (!DayPhase.NEED_INTERACTION_CHOICE.name().equals(session.getPhase())
                || session.getPendingInteractionEventId() == null) {
            return noPendingInteraction();
        }
        return switch (session.getPendingInteractionEventId().intValue()) {
            case 1 -> pendingInteraction(
                    1L,
                    "CHAT_BAIT",
                    "弹幕开始刷\u201C锐评前先声明不点名不挂人\u201D，标题组和米线组同时抬头。",
                    "不评价，今天唱歌",
                    "人气少涨，口碑稳定",
                    "围观热度-3，口碑+1，今天先不让主会场开庭",
                    "理性聊两句",
                    "人气上涨，蹭热度债务",
                    "围观热度+10，口碑-2，可能产生蹭热度债务",
                    "直接开香槟",
                    "串味大涨，米线风险",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            case 2 -> pendingInteraction(
                    2L,
                    "MARSHMALLOW_BOMB",
                    "棉花糖太逆天，读不读都像节目效果，房管和标题组同时停下手里的活。",
                    "跳过，真爱粉稳定",
                    "少一点节目效果，老粉会松口气",
                    "口碑+1，围观热度-3，先不把棉花糖念成事故素材",
                    "温和回应",
                    "人气小涨，但容易被二创截出去",
                    "围观热度+8，口碑-1，可能被切片组截成梗",
                    "念完并锐评",
                    "事故素材入库，米线风险上升",
                    "串味+5，围观热度+5，口碑-2，可能产生事故素材"
            );
            case 3 -> pendingInteraction(
                    3L,
                    "SC_BOSS_QUESTION",
                    "榜一问题存在感过强，直播间在等你接话，粉丝群气压开始变化。",
                    "感谢带过",
                    "商业化小降，独角兽情绪稳定",
                    "围观热度-3，口碑+1，今天先别让排班表上桌",
                    "认真回答",
                    "运营预算和独角兽会上涨，但陪伴边界变紧",
                    "运营预算+10，独角兽+5，商业化+3，可能产生独角兽期待债务",
                    "反向整活",
                    "串味上涨，独角兽债务风险上升",
                    "串味+5，围观热度+5，口碑-2，可能产生商业化反感债务"
            );
            case 4 -> pendingInteraction(
                    4L,
                    "COLLAB_RHYTHM",
                    "联动对方粉丝开始刷\u201C你是不是蹭\u201D，DD在两边来回坐车。",
                    "夸对方，联动稳定",
                    "DD小涨，口碑稳定",
                    "围观热度-3，口碑+1，今天先把联动节奏压住",
                    "自嘲",
                    "DD和乐子人上升，但蹭热度味道更明显",
                    "DD+5，乐子人+3，口碑-2，可能产生蹭热度债务",
                    "反呛",
                    "主会场启动，连坐风险上升",
                    "围观热度+10，口碑-4，可能产生联动开庭事故素材"
            );
            case 5 -> pendingInteraction(
                    5L,
                    "GIFT_SUPERCOMBO",
                    "榜一突然打出一波礼物连击，弹幕开始刷\u201C这是支持还是包养\u201D，气氛微妙。",
                    "感谢并低调带过",
                    "商业化小降，独角兽情绪稳",
                    "围观热度-3，口碑+1，先不让排班表上桌",
                    "公开鸣谢",
                    "运营预算涨，但陪伴边界变紧",
                    "运营预算+10，独角兽+5，商业化+3，可能产生独角兽期待债务",
                    "整活接梗",
                    "节目效果拉满，但米线风险上升",
                    "串味+5，围观热度+5，口碑-2，可能产生事故素材"
            );
            case 6 -> pendingInteraction(
                    6L,
                    "CLIP_REQUEST_FLOOD",
                    "切片组在弹幕排队催素材，标题组也想抢首发，直播间快变成素材调度会。",
                    "按节奏来，不抢跑",
                    "素材稳，切片组稍安",
                    "围观热度-3，口碑+1，今天先不让标题组抢跑",
                    "现场点名递素材",
                    "乐子人涨，但复读风险上升",
                    "围观热度+8，乐子人+3，口碑-1，可能产生蹭热度债务",
                    "把催更剪成梗",
                    "节目效果足，但事故素材入库",
                    "串味+5，围观热度+5，口碑-2，可能产生事故素材"
            );
            case 7 -> pendingInteraction(
                    7L,
                    "COLLAB_INVITE",
                    "一个比你体量大的主播发来联动邀请，粉丝群一半激动一半担心被说蹭。",
                    "先观望，确认档期",
                    "口碑稳，联动债务低",
                    "围观热度-3，口碑+1，今天先不急着接话",
                    "爽快答应",
                    "DD和人气涨，但蹭热度味道上升",
                    "DD+5，乐子人+3，口碑-2，可能产生蹭热度债务",
                    "反手开个玩笑",
                    "节目效果足，但可能冒犯对方",
                    "围观热度+10，口碑-3，可能产生联动开庭事故素材"
            );
            case 8 -> pendingInteraction(
                    8L,
                    "FAN_STUNT",
                    "有粉丝在直播间搞整活，弹幕一半刷\u201C节目效果\u201D一半刷\u201C过了\u201D，房管在犹豫删不删。",
                    "温和收住，转话题",
                    "口碑稳，老粉松口气",
                    "围观热度-3，口碑+1，今天先不让整活失控",
                    "顺势接梗",
                    "乐子人涨，但米线风险上升",
                    "围观热度+8，乐子人+3，口碑-1，可能被切片组截成梗",
                    "反向整得更狠",
                    "主会场启动，事故素材入库",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            case 9 -> pendingInteraction(
                    9L,
                    "ANTI_FAN_RAID",
                    "一批黑粉有组织地刷屏挂人，房管手速拉满也删不过来，直播间气压骤降。",
                    "冷处理，房管加锁",
                    "口碑稳，黑粉退潮慢",
                    "围观热度-3，口碑+1，今天先不开庭",
                    "正面回应澄清",
                    "人气涨，但可能被放大",
                    "围观热度+8，口碑-1，可能产生蹭热度债务",
                    "把黑粉做成梗",
                    "节目效果足，但米线风险飙升",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            case 10 -> pendingInteraction(
                    10L,
                    "MOD_HELP",
                    "房管在管理群求助\u201C刷太快删不过来，要不要开慢速\u201D，弹幕节奏开始影响直播。",
                    "开慢速，稳住节奏",
                    "口碑稳，老粉安心",
                    "围观热度-3，口碑+1，今天先压住节奏",
                    "现场招临时房管",
                    "人气涨，但管理边界变乱",
                    "围观热度+8，口碑-1，可能产生管理失序债务",
                    "把求助念成梗",
                    "节目效果足，但房管威信下降",
                    "串味+5，围观热度+5，口碑-2，可能产生事故素材"
            );
            case 11 -> pendingInteraction(
                    11L,
                    "PLATFORM_EVENT_INVITE",
                    "平台官方发来活动邀请，参加能拿流量扶持，但档期和你原定的内容计划冲突。",
                    "婉拒，守住原计划",
                    "口碑稳，节奏不乱",
                    "围观热度-3，口碑+1，今天先不接活动",
                    "调整档期参加",
                    "人气和流量涨，但内容债务上升",
                    "围观热度+8，人气+10，口碑-1，可能产生蹭热度债务",
                    "把活动做成整活",
                    "节目效果足，但可能被官方点名",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            case 12 -> pendingInteraction(
                    12L,
                    "SONG_REQUEST_BOMB",
                    "弹幕疯狂点歌，点歌单刷屏，标题组和房管都在看你接不接。",
                    "按自己的歌单来",
                    "口碑稳，老粉安心",
                    "围观热度-3，口碑+1，今天先不被点歌带节奏",
                    "挑几首接一下",
                    "人气小涨，但容易被点歌绑架",
                    "围观热度+8，口碑-1，可能产生蹭热度债务",
                    "把点歌念成梗",
                    "节目效果足，但米线风险上升",
                    "串味+5，围观热度+5，口碑-2，可能产生事故素材"
            );
            case 13 -> pendingInteraction(
                    13L,
                    "SC_CONFESSION",
                    "一条长SC真情告白，弹幕瞬间安静，独角兽和真爱粉都在等你怎么接。",
                    "真诚简短回应",
                    "口碑稳，独角兽情绪缓",
                    "围观热度-3，口碑+1，今天先不让告白变排班",
                    "认真长回应",
                    "独角兽涨，但陪伴边界变紧",
                    "独角兽+5，围观热度+5，口碑-1，可能产生独角兽期待债务",
                    "把告白玩成梗",
                    "节目效果足，但米线风险上升",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            case 14 -> pendingInteraction(
                    14L,
                    "RAID_INCOMING",
                    "别家主播下播后把观众空降到你这，人气突然涌入，弹幕一半欢迎一半警惕。",
                    "平稳欢迎，不抢人",
                    "口碑稳，DD小涨",
                    "围观热度-3，口碑+1，今天先不开主会场",
                    "热情接住流量",
                    "DD和人气涨，但蹭热度味道上升",
                    "DD+5，乐子人+3，口碑-2，可能产生蹭热度债务",
                    "把空降做成梗",
                    "节目效果足，但连坐风险上升",
                    "围观热度+10，口碑-3，可能产生联动开庭事故素材"
            );
            case 15 -> pendingInteraction(
                    15L,
                    "FAN_GIFT_COLLAGE",
                    "粉丝自发做了礼物墙和整活投稿，切片组想发，老粉担心尺度，群里在拉扯。",
                    "感谢但不转发",
                    "口碑稳，老粉安心",
                    "围观热度-3，口碑+1，今天先不让礼物墙失控",
                    "挑安全的转发",
                    "乐子人涨，但复读风险上升",
                    "围观热度+8，乐子人+3，口碑-1，可能产生蹭热度债务",
                    "全转发并加梗",
                    "节目效果拉满，但事故素材入库",
                    "串味+5，围观热度+5，口碑-3，可能产生事故素材"
            );
            default -> noPendingInteraction();
        };
    }

    private PendingInteractionDTO noPendingInteraction() {
        return new PendingInteractionDTO(false, null, null, null, null, List.of());
    }

    private PendingInteractionDTO pendingInteraction(
            Long id,
            String eventKey,
            String description,
            String safeLabel,
            String safeRiskPreview,
            String safeEffectPreview,
            String trafficLabel,
            String trafficRiskPreview,
            String trafficEffectPreview,
            String memeLabel,
            String memeRiskPreview,
            String memeEffectPreview
    ) {
        return new PendingInteractionDTO(
                true,
                id,
                eventKey,
                "直播现场",
                description,
                List.of(
                        new PendingInteractionChoiceDTO(
                                "safe",
                                safeLabel,
                                safeRiskPreview,
                                safeEffectPreview,
                                true,
                                null
                        ),
                        new PendingInteractionChoiceDTO(
                                "traffic",
                                trafficLabel,
                                trafficRiskPreview,
                                trafficEffectPreview,
                                true,
                                null
                        ),
                        new PendingInteractionChoiceDTO(
                                "meme",
                                memeLabel,
                                memeRiskPreview,
                                memeEffectPreview,
                                true,
                                null
                        )
                )
        );
    }
}
