package com.example.vupworld.service.content;

import com.example.vupworld.model.Vup;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.service.infra.JsonService;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RouteNarrativeService {

    private final JsonService jsonService;

    public RouteNarrativeService(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    // 每条路线在Day 5/10/15/20/25各有1段叙事
    private static final Map<String, Map<Integer, String>> ROUTE_NARRATIVES = new LinkedHashMap<>();

    static {
        // 歌势路线
        Map<Integer, String> singing = new LinkedHashMap<>();
        singing.put(5, "你开始认真练歌了。直播间里，你的歌声虽然还有些生涩，但已经有人在弹幕里说\"唱得不错\"。这让你很开心。");
        singing.put(10, "海声前辈注意到了你。她说你的嗓音有潜力，但需要更多练习。你决定加倍努力。");
        singing.put(15, "你的歌声开始有了自己的风格。有粉丝说\"听到你的歌就很安心\"。这是你最想听到的话。");
        singing.put(20, "越来越多的人因为你的歌声而来。你的直播间成了一个安静的角落，让人可以放松下来。");
        singing.put(25, "最后的冲刺。你知道自己的歌声已经和出道时不一样了。不管结局如何，你为自己的成长感到骄傲。");
        ROUTE_NARRATIVES.put("SINGING_IDOL", singing);

        // 切片路线
        Map<Integer, String> slice = new LinkedHashMap<>();
        slice.put(5, "你发现切片的传播力比直播本身还强。一个精彩的切片可以让更多人认识你。");
        slice.put(10, "切片组开始频繁光顾你的直播间。他们说你的内容\"很适合切\"。这是个好兆头。");
        slice.put(15, "你的切片登上了热门。虽然只是短短几十秒，但足以让更多人知道你的名字。");
        slice.put(20, "\"切片圣体\"——有人开始这样称呼你。你笑着接受了这个称号。");
        slice.put(25, "你的切片播放量已经超过了直播观看人数。这就是传播的力量。");
        ROUTE_NARRATIVES.put("SLICE_SAINT", slice);

        // 黑红路线
        Map<Integer, String> blackRed = new LinkedHashMap<>();
        blackRed.put(5, "你开始尝试一些有争议的内容。弹幕里有人骂，有人护，但至少大家都在讨论你。");
        blackRed.put(10, "争议带来了流量。你的热度在飙升，但口碑在下降。你在走钢丝。");
        blackRed.put(15, "有人在论坛开了你的楼。你选择不看，但你知道那里面一定有很多难听的话。");
        blackRed.put(20, "你学会了在争议中保持主动。不是被动挨打，而是主动引导话题。");
        blackRed.put(25, "最后几天，你已经习惯了被讨论的感觉。不管别人怎么说，你就是你。");
        ROUTE_NARRATIVES.put("BLACK_RED_MAIN_STAGE", blackRed);

        // 社交联动路线
        Map<Integer, String> social = new LinkedHashMap<>();
        social.put(5, "你开始主动联系其他VTuber。第一次联动时有点紧张，但对方很友好。");
        social.put(10, "你的联动频率越来越高。有人说你是\"DD公交站\"，但你享受和不同的人合作。");
        social.put(15, "通过联动，你认识了很多有趣的人。你的圈子在不断扩大。");
        social.put(20, "你开始收到大V的联动邀请。这是你社交能力的证明。");
        social.put(25, "30天下来，你拥有了最广的圈子。虽然粉丝忠诚度不高，但你的影响力无人能及。");
        ROUTE_NARRATIVES.put("SOCIAL_COLLAB", social);

        // 梗舞路线
        Map<Integer, String> dance = new LinkedHashMap<>();
        dance.put(5, "你尝试在直播中跳舞。虽然跳得一般，但弹幕说\"节目效果拉满\"。");
        dance.put(10, "你的一段魔性舞蹈被做成了表情包。虽然是恶搞，但你火了。");
        dance.put(15, "\"梗舞达人\"——你的新称号。每次跳舞，弹幕都会刷\"哈哈哈哈哈\"。");
        dance.put(20, "你的梗浓度已经很高了。随便说句话都能被做成切片。");
        dance.put(25, "最后几天，你回顾自己的梗集锦。每一个梗都是一段回忆。");
        ROUTE_NARRATIVES.put("DANCE_MEME", dance);

        // 电子榨菜路线
        Map<Integer, String> pickle = new LinkedHashMap<>();
        pickle.put(5, "你选择了最稳健的路。不追热点，不搞争议，踏踏实实做内容。");
        pickle.put(10, "有人说你无聊，但你的粉丝粘性很高。他们说\"看你直播很安心\"。");
        pickle.put(15, "你的口碑一直在稳步上升。虽然热度不高，但你的粉丝质量是最好的。");
        pickle.put(20, "你成了\"电子榨菜\"——下饭必备。这个称号很土，但你很喜欢。");
        pickle.put(25, "最后几天，你庆幸自己选择了稳健的路。虽然不出圈，但也没有翻车。");
        ROUTE_NARRATIVES.put("ELECTRONIC_PICKLE", pickle);
    }

    // 获取当日的路线叙事
    public Optional<String> getNarrative(Vup vup, DaySession session) {
        String route = vup.getCurrentRoute();
        if (route == null || "UNKNOWN".equals(route)) return Optional.empty();

        Map<Integer, String> narratives = ROUTE_NARRATIVES.get(route);
        if (narratives == null) return Optional.empty();

        int day = vup.getDayCount();
        // 找到最近的叙事点（<=当前天数）
        Integer closestDay = null;
        for (Integer d : narratives.keySet()) {
            if (d <= day) closestDay = d;
        }
        if (closestDay == null) return Optional.empty();

        // 检查是否已经展示过（用tutorialFlagsJson记录）
        String flagKey = "routeNarrative_" + route + "_" + closestDay;
        try {
            Map<String, Object> flags = jsonService.readMap(vup.getTutorialFlagsJson());
            if (Boolean.TRUE.equals(flags.get(flagKey))) return Optional.empty();
            flags.put(flagKey, true);
            vup.setTutorialFlagsJson(jsonService.write(flags));
        } catch (Exception ignored) {}

        return Optional.of(narratives.get(closestDay));
    }
}
