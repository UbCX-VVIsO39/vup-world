package com.example.vupworld.service.event;

import com.example.vupworld.model.Vup;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.dto.RewardDelta;
import com.example.vupworld.service.infra.DeterministicRngService;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BlackSwanService {

    private final DeterministicRngService rngService;

    public BlackSwanService(DeterministicRngService rngService) {
        this.rngService = rngService;
    }

    private static final List<BlackSwanEvent> EVENTS = Arrays.asList(
        // === 平台政策变动 ===
        new BlackSwanEvent("PLATFORM_CRACKDOWN", "平台大整顿",
            "平台突然发起内容整顿，大量直播间被临时关闭。你的直播间也受到了波及。",
            -20, -15, -10, 0),
        new BlackSwanEvent("PLATFORM_POLICY_SHIFT", "平台规则突变",
            "平台更新了分成与推荐规则，旧的内容标签一夜失效，你的部分投稿被限流。",
            -15, -8, -18, -300),
        new BlackSwanEvent("PLATFORM_AGE_VERIFY", "强制实名年龄认证",
            "平台要求所有主播在48小时内完成实名年龄认证，未完成的直播间会被临时下线。",
            -12, -5, -15, 0),
        new BlackSwanEvent("PLATFORM_DEMONETIZE", "批量取消收益",
            "平台以\"内容质量不达标\"为由批量取消了你的投稿收益，月结收入直接腰斩。",
            -10, -10, -20, -800),
        // === 同行争议 ===
        new BlackSwanEvent("BIG_V_CRITICISM", "被大V点名批评",
            "一个百万粉大V在视频中点名批评了你。虽然可能是断章取义，但舆论已经开始发酵。",
            30, -25, 20, 0),
        new BlackSwanEvent("RIVAL_PICKS_FIGHT", "同行公开开团",
            "一个同体量主播在直播里直接点名开团你，对方的粉丝涌进你的直播间刷屏。",
            25, -18, 22, 0),
        new BlackSwanEvent("PEER_PLAGIARISM", "被指控抄袭同行",
            "有人放出对比视频，指控你的直播企划抄袭了另一位主播，评论区吵成一锅粥。",
            15, -20, 10, 0),
        new BlackSwanEvent("COLLAB_PARTNER_TRASH_TALK", "联动对象背后吐槽",
            "上次联动的对象在私下被录到吐槽你，录音外流，两边粉丝开始互相戳。",
            20, -15, 18, 0),
        // === 粉丝群体异动 ===
        new BlackSwanEvent("FAN_WAR", "粉丝大战",
            "你的粉丝和其他V的粉丝发生了大规模冲突。你被卷入了舆论漩涡。",
            35, -15, 30, 0),
        new BlackSwanEvent("FAN_FRACTION_SPLIT", "粉丝群体分裂",
            "粉丝群里因为路线问题吵翻，一批人拉了新群宣布\"脱粉不回踩但不再支持\"。",
            -5, -12, -15, 0),
        new BlackSwanEvent("TOXIC_FANS_BAN_WAVE", "核心粉被平台封禁",
            "平台对你的核心粉丝群发起封禁波，几位房管和活跃粉被永封，群内人心惶惶。",
            -8, -8, -20, 0),
        new BlackSwanEvent("FAN_CLUB_COUP", "粉丝后援会政变",
            "官方后援会的几名管理宣布集体辞职并带走群成员，另立\"非官方\"后援会。",
            -10, -10, -18, 0),
        // === 设备故障 ===
        new BlackSwanEvent("EQUIPMENT_MAJOR_FAIL", "设备重大故障",
            "直播到一半，电脑突然蓝屏。你被迫中断直播，粉丝们在弹幕里刷\"主播还好吗\"。",
            -15, -5, -10, -200),
        new BlackSwanEvent("MIC_DIES_MID_STREAM", "麦克风突然失灵",
            "直播高潮时麦克风突然没声音，弹幕瞬间刷满问号，你只能用手写板跟观众沟通。",
            -12, -3, -8, -150),
        new BlackSwanEvent("INTERNET_OUTAGE", "断网事故",
            "小区光纤被施工挖断，你整整一天没法开播，也没法发动态解释。",
            -18, -5, -15, -100),
        new BlackSwanEvent("AVATAR_RIG_BREAK", "皮套骨骼崩坏",
            "直播中皮套的骨骼绑定突然错乱，脸和手飞出屏幕，切片组连夜抢救也没救回来。",
            10, -8, -5, -250),
        // === 个人健康 ===
        new BlackSwanEvent("VOICE_LOSS", "突发失声",
            "连续高强度直播后你突然失声，医生要求至少静养三天，档期全乱。",
            -20, -3, -18, -200),
        new BlackSwanEvent("BURNOUT_BREAKDOWN", "情绪崩溃",
            "压力积累到临界点，你在直播前情绪崩溃大哭，不得不临时取消当天的回。",
            -15, -8, -12, 0),
        new BlackSwanEvent("HEALTH_SCARE", "健康警报",
            "体检报告出现异常指标，你需要请假复查，粉丝群开始流传各种猜测。",
            -22, -5, -20, -300),
        // === 商业合作意外 ===
        new BlackSwanEvent("PARTNER_CANCEL", "合作方突然取消",
            "你精心准备的联动，对方突然取消了。粉丝们很失望，你也很沮丧。",
            -10, -5, -15, -500),
        new BlackSwanEvent("SPONSOR_RETRACTS", "赞助商撤资",
            "已经谈好的赞助商因为\"舆论风险\"临时撤资，到手的合同飞了。",
            -8, -8, -18, -1200),
        new BlackSwanEvent("CONTRACT_LOOPHOLE", "合同条款陷阱",
            "法务发现之前签的商务合同里有隐藏的排他条款，你不得不赔钱解约。",
            -5, -10, -10, -700),
        new BlackSwanEvent("MERCH_QUALITY_DISASTER", "周边质量问题",
            "新出的周边被爆出质量问题，粉丝晒图维权，你只能全量退款并道歉。",
            -12, -15, -8, -600),
        // === 舆论反转 ===
        new BlackSwanEvent("MEDIA_EXPOSURE", "媒体曝光",
            "一家自媒体报道了你的\"黑料\"。虽然内容不实，但传播很广。",
            50, -20, 35, 0),
        new BlackSwanEvent("PUBLIC_SYMPATHY_WAVE", "舆论同情反转",
            "一段你被恶意剪辑的真相被路人放出，舆论瞬间倒向你，路人纷纷路人转粉。",
            35, 15, 30, 0),
        new BlackSwanEvent("RUMOR_DEBUNKED", "谣言被官方辟谣",
            "平台官方下场为你之前的争议辟谣，黑粉哑火，你的口碑意外回升。",
            20, 20, 15, 0),
        new BlackSwanEvent("BACKLASH_AGAINST_HATERS", "黑粉反噬",
            "带头黑你的账号被平台因违规封禁，路人对你的同情转化为关注。",
            25, 12, 22, 0),
        new BlackSwanEvent("OLD_CLIP_RESURFACES_POSITIVE", "旧切片翻红",
            "你半年前的一段直播切片突然被算法重新推上首页，正向二创刷屏。",
            30, 8, 28, 200),
        // === 版权 / 算法 / 信息 ===
        new BlackSwanEvent("COPYRIGHT_STRIKE", "版权警告",
            "你收到了一条版权警告。虽然可能是误判，但你的账号被临时限制了。",
            -25, -10, -20, 0),
        new BlackSwanEvent("LEAKED_INFO", "个人信息泄露",
            "有人在网上曝光了你的部分个人信息。你很恐慌，不知道该怎么办。",
            20, -30, 15, 0),
        new BlackSwanEvent("ALGORITHM_PENALTY", "算法惩罚",
            "平台算法突然不再推荐你的内容。你的观看人数断崖式下跌。",
            -30, -5, -25, 0),
        // === 切片搬运 ===
        new BlackSwanEvent("CLIP_VIRAL_NEGATIVE", "切片被搬运引发争议",
            "你的一段直播被切片搬运到微博，但被断章取义。评论区一片骂声。",
            40, -20, 25, 0)
    );

    public Optional<BlackSwanEvent> checkTrigger(Vup vup, DaySession session) {
        if (vup.getDayCount() < 5) return Optional.empty();

        var ledger = rngService.createLedger(session);
        int roll = ledger.nextInt("black_swan", 100);

        // Context-aware trigger rate based on player state
        int triggerThreshold = 5; // base 5%
        // High reputation or heat = more public exposure = more risk
        if (vup.getReputation() >= 60 && vup.getWatchHeat() >= 40) {
            triggerThreshold = 8; // 8%
        } else if (vup.getReputation() < 30) {
            triggerThreshold = 3; // 3% - less exposure, less risk
        }
        // DD-heavy fanbase = more drama potential
        int totalFans = vup.getFans();
        if (totalFans > 0 && (double) vup.getDdFans() / totalFans > 0.4) {
            triggerThreshold += 2;
        }
        triggerThreshold = Math.min(12, triggerThreshold);

        rngService.finalizeLedger(session, ledger);

        if (roll < triggerThreshold) {
            // Filter events by context: don't trigger "media exposure" for low-heat players
            List<BlackSwanEvent> filtered = filterContextEvents(vup);
            int index = ledger.nextInt("black_swan_pick", filtered.size());
            return Optional.of(filtered.get(index));
        }
        return Optional.empty();
    }

    private List<BlackSwanEvent> filterContextEvents(Vup vup) {
        if (vup.getWatchHeat() < 20) {
            // Low heat: filter out events that require high public visibility
            return EVENTS.stream()
                    .filter(e -> !"MEDIA_EXPOSURE".equals(e.key) && !"BACKLASH_AGAINST_HATERS".equals(e.key) && !"RUMOR_DEBUNKED".equals(e.key))
                    .toList();
        }
        if (vup.getReputation() > 70) {
            // High reputation: filter out events that are more relevant to controversial streamers
            return EVENTS.stream()
                    .filter(e -> !"AVATAR_RIG_BREAK".equals(e.key) && !"LEAKED_INFO".equals(e.key))
                    .toList();
        }
        return EVENTS;
    }

    public static class BlackSwanEvent {
        public final String key;
        public final String title;
        public final String description;
        public final int heatChange;
        public final int reputationChange;
        public final int popularityChange;
        public final int coinChange;

        public BlackSwanEvent(String key, String title, String description,
                            int heatChange, int reputationChange, int popularityChange, int coinChange) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.heatChange = heatChange;
            this.reputationChange = reputationChange;
            this.popularityChange = popularityChange;
            this.coinChange = coinChange;
        }
    }
}
