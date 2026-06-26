package com.example.vupworld.service.content;

import com.example.vupworld.dto.MoodDtos.NarrativeResult;
import com.example.vupworld.service.content.ContentCatalogService.ContentEntry;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 叙事文本系统。从内容目录查询叙事文本，提供硬编码fallback。
 */
@Component
public class NarrativeService {
    private final ContentCatalogService contentCatalogService;
    private final JsonService jsonService;

    /** 默认叙事文本池，按eventKey+choiceType组合 */
    private static final Map<String, NarrativeResult> DEFAULT_NARRATIVES = new LinkedHashMap<>();

    private static void put(String eventKey, String choiceType, String safe, String traffic, String meme) {
        DEFAULT_NARRATIVES.put(eventKey + ":" + choiceType, new NarrativeResult(safe, traffic, meme));
    }

    static {
        // 安全直播相关 (5条)
        put("STREAM_START", "SAFE",
                "你调好设备，打开直播间。几个老粉第一时间进来打招呼，气氛温馨。",
                "今天的直播标题很稳，预计观看人数稳定增长。",
                "弹幕里有人开始刷\"日常开播\"的梗。");
        put("STREAM_START", "BOLD",
                "你深吸一口气，今天的直播企划有点冒险。但机会总是留给敢拼的人。",
                "大胆的标题吸引了不少新观众，数据可能会有波动。",
                "弹幕已经开始讨论今天的\"整活\"预期了。");
        put("STREAM_MID", "SAFE",
                "直播进行中，你和观众的互动很自然。老粉们配合默契。",
                "稳定的直播节奏让推荐算法持续推送。",
                "有人在弹幕里发\"稳如老狗\"。");
        put("STREAM_MID", "BOLD",
                "直播进入高潮，你决定即兴发挥。观众的反应很热烈。",
                "突然的内容转变引起了讨论，弹幕量激增。",
                "这段直播素材注定要被切片组疯狂截取。");
        put("STREAM_END", "SAFE",
                "直播顺利结束，你和观众道别。今天的直播很平稳。",
                "稳定的直播数据为明天的内容积累打下基础。",
                "粉丝群里有人发\"今天也是安稳的一天\"。");

        // 切片投稿相关 (5条)
        put("CLIP_SUBMIT", "SAFE",
                "你仔细筛选了今天的直播素材，剪辑出一个稳妥的切片。",
                "切片内容中规中矩，但胜在质量稳定。",
                "切片组表示\"这个素材很好剪\"。");
        put("CLIP_SUBMIT", "BOLD",
                "你决定投稿一个高风险高回报的切片。标题党？不存在的，是真材实料。",
                "切片标题很有冲击力，可能会引发讨论。",
                "评论区已经开始争论\"这是不是标题党\"了。");
        put("CLIP_VIRAL", "SAFE",
                "切片数据稳步上升，虽然没有爆发，但长尾流量不错。",
                "持续的曝光让频道订阅数缓慢增长。",
                "有人留言\"这个切片让我入坑了\"。");
        put("CLIP_VIRAL", "BOLD",
                "切片突然爆了！数据飙升，新观众涌入。",
                "爆发式增长带来大量关注，但也引来了一些争议。",
                "\"切片圣手\"的名号开始在圈子里流传。");
        put("CLIP_FAIL", "SAFE",
                "切片数据平平，但你不在意。每一次投稿都是积累。",
                "这次的切片没有引起太大反响，但频道数据保持稳定。",
                "\"下次一定会更好\"，你在粉丝群里这样说道。");

        // 联动相关 (5条)
        put("COLLAB_START", "SAFE",
                "你和一个同级别的VUP约了一次轻松的联动。气氛很融洽。",
                "联动内容稳定，双方粉丝都有不错的互动。",
                "弹幕里有人刷\"双倍快乐\"。");
        put("COLLAB_START", "BOLD",
                "你决定挑战一次大联动。对方的粉丝量是你的三倍。",
                "这次联动可能会带来大量新观众，但也存在被压制的风险。",
                "\"这是要蹭还是要被蹭？\"弹幕里议论纷纷。");
        put("COLLAB_MID", "SAFE",
                "联动进行中，你和对方配合默契。话题不断。",
                "双方粉丝的互动很积极，弹幕氛围良好。",
                "有人开始讨论\"下次还要一起\"。");
        put("COLLAB_MID", "BOLD",
                "联动进入高潮，你展示了自己独特的一面。对方也很配合。",
                "你的个人特色在联动中脱颖而出，数据开始上涨。",
                "\"原来她这么有趣\"，新观众开始关注你。");
        put("COLLAB_END", "SAFE",
                "联动结束，你和对方互相关注。这次联动很成功。",
                "联动带来的新粉丝开始关注你的频道。",
                "\"期待下次合作\"成为弹幕里的高频词。");

        // 休息相关 (5条)
        put("REST_DAY", "SAFE",
                "今天休息，你关掉直播设备，泡了杯茶。窗外阳光很好。",
                "休息日虽然没有内容产出，但体力恢复很重要。",
                "粉丝群里有人说\"主播今天休息，我们也休息\"。");
        put("REST_DAY", "BOLD",
                "虽然今天休息，但你忍不住刷了刷同行的直播。学习一下。",
                "休息日也在关注行业动态，为明天的内容做准备。",
                "\"休息日还这么卷\"，粉丝在群里调侃。");
        put("REST_RECOVERY", "SAFE",
                "休息让你恢复了精力。明天可以全力以赴了。",
                "充足的休息让你的状态回到最佳水平。",
                "\"充满电了！\"你在粉丝群里发了一条消息。");
        put("REST_RECOVERY", "BOLD",
                "休息期间你灵感迸发，想出了好几个直播企划。",
                "灵感的积累让明天的内容有了更多可能性。",
                "\"明天有大动作\"，你在群里暗示了一下。");
        put("REST_IDLE", "SAFE",
                "休息日没什么特别的，你享受了一段安静的时光。",
                "平静的一天，但也是必要的调整。",
                "\"岁月静好\"，你在社交媒体上发了一条动态。");

        // 负面事件相关 (5条)
        put("CONTROVERSY_START", "SAFE",
                "网上出现了一些关于你的负面讨论。你决定冷静应对。",
                "争议初期的处理方式很重要，沉默有时是最好的回应。",
                "粉丝群里有人在讨论\"要不要回应\"。");
        put("CONTROVERSY_START", "BOLD",
                "网上出现了负面讨论，你决定正面回应。",
                "直接回应可能会平息争议，也可能会火上浇油。",
                "\"刚正面\"还是\"装死\"，弹幕里分成两派。");
        put("CONTROVERSY_MID", "SAFE",
                "争议还在发酵，但你保持冷静。时间会证明一切。",
                "持续的沉默策略让部分质疑者失去兴趣。",
                "有粉丝自发帮你澄清，社区开始自我修复。");
        put("CONTROVERSY_MID", "BOLD",
                "你的回应引发了更多讨论。但至少你表达了自己的立场。",
                "争议带来的关注度也在上升，这是双刃剑。",
                "\"有态度\"成为你的新标签。");
        put("CONTROVERSY_END", "SAFE",
                "争议逐渐平息。你松了一口气，这次算是平稳度过了。",
                "危机处理得当，口碑开始恢复。",
                "\"平安无事\"，粉丝群里恢复了往日的热闹。");

        // 粉丝互动相关 (5条)
        put("FAN_INTERACT", "SAFE",
                "你和粉丝们聊了聊天，回答了一些问题。气氛很温馨。",
                "粉丝互动能增加粘性，让观众更有归属感。",
                "\"主播好温柔\"，弹幕里一片好评。");
        put("FAN_INTERACT", "BOLD",
                "你决定搞一次粉丝互动活动。抽奖？还是问答挑战？",
                "互动活动可能会吸引新观众参与。",
                "\"主播要发福利了！\"弹幕瞬间热闹起来。");
        put("FAN_SERVICE", "SAFE",
                "你满足了粉丝的一些小要求。虽然有点累，但看到他们开心你就满足了。",
                "粉丝服务能提升口碑，但要注意别过度消耗自己。",
                "\"主播最好了\"，粉丝群里一片感动。");
        put("FAN_SERVICE", "BOLD",
                "你决定给粉丝一个大惊喜。这需要一些准备，但值得。",
                "惊喜内容可能会成为热门话题。",
                "\"这是什么神仙主播\"，新观众开始被圈粉。");
        put("FAN_CONFLICT", "SAFE",
                "粉丝之间出现了一些小摩擦。你及时调解，避免了事态扩大。",
                "及时的调解能维护社区氛围。",
                "\"主播说得对\"，争吵平息了。");

        // 路线选择相关 (5条)
        put("ROUTE_CHOICE", "SAFE",
                "你仔细分析了当前的形势，选择了一条稳妥的路线。",
                "稳健的路线选择能保证持续发展。",
                "\"稳扎稳打\"是你的策略。");
        put("ROUTE_CHOICE", "BOLD",
                "你决定走一条少有人走的路。风险大，但机会也大。",
                "差异化路线可能会让你脱颖而出。",
                "\"不走寻常路\"，弹幕里有人这样评价。");
        put("ROUTE_COMMIT", "SAFE",
                "你坚定了自己的路线选择。这是长期主义的胜利。",
                "路线的坚持会带来复利效应。",
                "\"专注的人最可怕\"，同行开始关注你。");
        put("ROUTE_COMMIT", "BOLD",
                "你全力押注这条路线。All in，没有退路。",
                "全力以赴的态度会感染观众和粉丝。",
                "\"赌一把大的\"，弹幕里充满了期待。");
        put("ROUTE_SWITCH", "SAFE",
                "你决定调整路线。这不是放弃，是战略转型。",
                "及时的路线调整能避免更大的损失。",
                "\"识时务者为俊杰\"，粉丝表示理解。");

        // 商业合作相关 (5条)
        put("COMMERCIAL_START", "SAFE",
                "你接了一个商业合作。虽然有点紧张，但这是VUP成长的必经之路。",
                "商业合作能带来收入，但要注意保持内容的真实性。",
                "\"主播开始恰饭了\"，弹幕里有人调侃。");
        put("COMMERCIAL_START", "BOLD",
                "你决定挑战一个大型商业合作。这是提升商业价值的好机会。",
                "大型商业合作可能会提升你的商业等级。",
                "\"主播出息了\"，粉丝们很兴奋。");
        put("COMMERCIAL_MID", "SAFE",
                "商业合作进行中，你尽力保持自然。观众的反应还不错。",
                "自然的商业内容能平衡收入和口碑。",
                "\"恰饭恰得这么自然\"，弹幕里有人称赞。");
        put("COMMERCIAL_MID", "BOLD",
                "你在商业合作中加入了自己的创意。品牌方也很满意。",
                "创意的商业内容可能会成为爆款。",
                "\"这是恰饭？这明明是艺术\"，弹幕沸腾了。");
        put("COMMERCIAL_END", "SAFE",
                "商业合作结束，你松了一口气。效果还不错。",
                "这次商业合作为未来的合作打下基础。",
                "\"期待下次恰饭\"，粉丝们表示支持。");

        // 突发事件相关 (5条)
        put("ACCIDENT_START", "SAFE",
                "突然出了点状况。你冷静应对，尽量不让观众察觉。",
                "冷静的处理能避免事态扩大。",
                "\"主播好像有点不对劲\"，细心的粉丝发现了。");
        put("ACCIDENT_START", "BOLD",
                "出了意外！你决定把这次意外变成直播内容。",
                "化危为机的处理方式可能会带来意外的收获。",
                "\"这是直播事故？还是整活？\"弹幕里一片混乱。");
        put("ACCIDENT_RESOLVE", "SAFE",
                "状况解除了。你长舒一口气，还好没出大问题。",
                "危机的解除让你积累了应对经验。",
                "\"平安就好\"，粉丝们松了一口气。");
        put("ACCIDENT_RESOLVE", "BOLD",
                "意外居然成了今天的高光时刻！观众的反应出乎意料。",
                "意外带来的热度可能会持续一段时间。",
                "\"神回！\"弹幕里刷起了这两个字。");
        put("ACCIDENT_AFTERMATH", "SAFE",
                "事后你复盘了这次意外，总结了一些经验教训。",
                "经验的积累让你的抗风险能力更强了。",
                "\"下次一定能处理得更好\"，你这样鼓励自己。");

        // 日常训练相关 (5条)
        put("TRAIN_SONG", "SAFE",
                "你花了几个小时练歌。虽然枯燥，但你知道这是必要的。",
                "歌唱训练能提升歌力，为歌回直播做准备。",
                "\"主播在练歌吗？期待新歌回\"，粉丝群里有人问。");
        put("TRAIN_SONG", "BOLD",
                "你挑战了一首高难度的歌曲。失败了几次，但最终拿下了。",
                "挑战高难度内容能快速提升能力。",
                "\"主播又在卷了\"，粉丝们既心疼又佩服。");
        put("TRAIN_DANCE", "SAFE",
                "你对着镜子练习舞蹈动作。每一个节拍都要精准。",
                "舞蹈训练能提升舞力，为舞蹈直播做准备。",
                "\"主播在练舞吗？期待新舞蹈\"，粉丝群里有人问。");
        put("TRAIN_DANCE", "BOLD",
                "你决定学习一个复杂的舞蹈。这需要时间和耐心。",
                "高难度舞蹈的学习过程本身就是内容。",
                "\"主播的舞蹈进步好快\"，粉丝们很惊讶。");
        put("TRAIN_TALK", "SAFE",
                "你练习了聊天技巧和段子。杂谈是VUP的基本功。",
                "杂谈能力的提升能增加直播的观赏性。",
                "\"主播越来越会聊天了\"，粉丝们注意到了变化。");
    }

    public NarrativeService(ContentCatalogService contentCatalogService, JsonService jsonService) {
        this.contentCatalogService = contentCatalogService;
        this.jsonService = jsonService;
    }

    /**
     * 获取叙事文本。优先从内容目录查询，找不到则用硬编码fallback。
     */
    public NarrativeResult getNarrative(String eventKey, String choiceType) {
        // 先从内容目录查询
        String catalogKey = eventKey + ":" + choiceType;
        List<ContentEntry> entries = contentCatalogService.getEntries("NARRATIVE_TEXT").get(catalogKey);
        if (entries != null && !entries.isEmpty()) {
            ContentEntry entry = entries.get(0);
            if (entry.json() != null && !entry.json().isBlank()) {
                try {
                    Map<String, Object> jsonMap = jsonService.readMap(entry.json());
                    return new NarrativeResult(
                            stringValue(jsonMap.get("safeText")),
                            stringValue(jsonMap.get("trafficText")),
                            stringValue(jsonMap.get("memeText"))
                    );
                } catch (Exception ignored) {
                    // JSON解析失败，尝试用text字段
                }
            }
            // 使用text字段作为safeText
            if (entry.text() != null && !entry.text().isBlank()) {
                return new NarrativeResult(entry.text(), "", "");
            }
        }

        // 硬编码fallback
        NarrativeResult fallback = DEFAULT_NARRATIVES.get(eventKey + ":" + choiceType);
        if (fallback != null) {
            return fallback;
        }

        // 最终fallback
        return new NarrativeResult(
                "一切按计划进行，没有意外发生。",
                "",
                ""
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
