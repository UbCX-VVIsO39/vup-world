package com.example.vupworld.service.fan;

import com.example.vupworld.service.infra.RngLedger;

import com.example.vupworld.service.infra.DeterministicRngService;
import com.example.vupworld.service.content.ContentCatalogService;

import com.example.vupworld.dto.FanLetterDtos.FanLetterDTO;
import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FanLetterService {

    private static final List<FanLetterDTO> TRUE_FAN_LETTERS = List.of(
            new FanLetterDTO("tf1", "true_fan", "老粉小明", "主播慢慢来，我会一直支持你的。", "温暖", "❤️"),
            new FanLetterDTO("tf2", "true_fan", "真爱粉一号", "今天的直播很好看，期待明天！", "开心", "🥰"),
            new FanLetterDTO("tf3", "true_fan", "铁粉老王", "主播加油，我们都在！", "鼓励", "💪"),
            new FanLetterDTO("tf4", "true_fan", "忠实观众", "虽然今天有点失误，但没关系，继续加油！", "理解", "🙏"),
            new FanLetterDTO("tf5", "true_fan", "老粉小红", "主播的杂谈越来越有意思了！", "欣赏", "✨"),
            new FanLetterDTO("tf6", "true_fan", "应援席三月", "应援席只是想说今晚低压台很安心，别为了节目效果硬扛。", "安心", "⚓"),
            new FanLetterDTO("tf7", "true_fan", "群公告bot", "粉丝群公告写清楚就好，大家按公告来，不催不拱火。", "稳定", "📌")
    );

    private static final List<FanLetterDTO> FUN_FAN_LETTERS = List.of(
            new FanLetterDTO("ff1", "fun_fan", "乐子人A", "今天的节目效果很好，哈哈哈！", "开心", "😂"),
            new FanLetterDTO("ff2", "fun_fan", "围观群众", "这个切片我能看十遍！", "兴奋", "🔥"),
            new FanLetterDTO("ff3", "fun_fan", "弹幕高手", "主播的梗越来越有意思了！", "欣赏", "👏"),
            new FanLetterDTO("ff4", "fun_fan", "切片组成员", "今天的素材太棒了，我已经开始剪了！", "期待", "✂️"),
            new FanLetterDTO("ff5", "fun_fan", "乐子人B", "主播今天又整活了，爱看！", "开心", "🤣"),
            new FanLetterDTO("ff6", "fun_fan", "烤肉组值班", "烤肉组可以翻译名场面，但二创授权先问清楚，我不想给主播添麻烦。", "谨慎", "🥩"),
            new FanLetterDTO("ff7", "fun_fan", "录播组夜班", "录播组补时间轴补好了，标题组这次别抢跑。", "可靠", "⏱️")
    );

    private static final List<FanLetterDTO> UNICORN_LETTERS = List.of(
            new FanLetterDTO("u1", "unicorn", "独角兽一号", "主播今天和谁联动了？", "好奇", "👀"),
            new FanLetterDTO("u2", "unicorn", "榜一", "今天的高亮互动问题主播还没回答呢。", "期待", "📌"),
            new FanLetterDTO("u3", "unicorn", "陪伴粉", "主播今天怎么没开陪伴回？", "疑惑", "❓"),
            new FanLetterDTO("u4", "unicorn", "独角兽二号", "主播以前不是这样的...", "怀念", "😢"),
            new FanLetterDTO("u5", "unicorn", "陪伴粉", "陪伴回别排没了，我只是有点不适应。", "不安", "🕯️")
    );

    private static final List<FanLetterDTO> DD_LETTERS = List.of(
            new FanLetterDTO("dd1", "dd", "DD小张", "路过看看，主播不错！", "随意", "👋"),
            new FanLetterDTO("dd2", "dd", "多推人", "今天看了三个主播，你是其中一个！", "开心", "🚌"),
            new FanLetterDTO("dd3", "dd", "联动观众", "从隔壁过来的，主播好！", "友好", "🤝"),
            new FanLetterDTO("dd4", "dd", "路人粉", "今天第一次看，感觉不错！", "新鲜", "✨"),
            new FanLetterDTO("dd5", "dd", "DD老手", "又来坐一站了！", "习惯", "🚂"),
            new FanLetterDTO("dd6", "dd", "路过DD", "从联动过来坐一会儿，灯牌先点上，气氛挺舒服。", "轻松", "🚌")
    );

    private final DeterministicRngService deterministicRngService;
    private final ContentCatalogService contentCatalogService;

    public FanLetterService(DeterministicRngService deterministicRngService, ContentCatalogService contentCatalogService) {
        this.deterministicRngService = deterministicRngService;
        this.contentCatalogService = contentCatalogService;
    }

    /**
     * Parses a FanLetterDTO from a ContentCatalogService.ContentEntry.
     */
    private FanLetterDTO parseFanLetter(String subKey, String contentText, String contentJson) {
        String name = "";
        String mood = "";
        String icon = "";
        if (contentJson != null && !contentJson.isBlank()) {
            try {
                // Simple JSON field extraction without extra dependencies
                name = extractJsonString(contentJson, "name");
                mood = extractJsonString(contentJson, "mood");
                icon = extractJsonString(contentJson, "icon");
            } catch (Exception ignored) {
                // Fall through with defaults
            }
        }
        return new FanLetterDTO(subKey, "", name, contentText, mood, icon);
    }

    /**
     * Minimal JSON string extraction for simple flat objects.
     */
    private String extractJsonString(String json, String field) {
        String pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) return "";
        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote < 0) return "";
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) return "";
        return json.substring(startQuote + 1, endQuote);
    }

    /**
     * Returns fan letter candidates for a fan type, preferring DB content with hardcoded fallback.
     */
    private List<FanLetterDTO> getLettersForType(String fanType) {
        Map<String, List<ContentCatalogService.ContentEntry>> entries = contentCatalogService.getEntries("FAN_LETTER");
        List<ContentCatalogService.ContentEntry> dbEntries = entries.getOrDefault(fanType, Collections.emptyList());
        if (!dbEntries.isEmpty()) {
            List<FanLetterDTO> result = new ArrayList<>();
            for (ContentCatalogService.ContentEntry entry : dbEntries) {
                result.add(parseFanLetter(entry.subKey(), entry.text(), entry.json()));
            }
            return result;
        }
        // Fallback to hardcoded values
        return switch (fanType) {
            case "true_fan" -> List.copyOf(TRUE_FAN_LETTERS);
            case "fun_fan" -> List.copyOf(FUN_FAN_LETTERS);
            case "unicorn" -> List.copyOf(UNICORN_LETTERS);
            case "dd" -> List.copyOf(DD_LETTERS);
            default -> List.copyOf(TRUE_FAN_LETTERS);
        };
    }

    public FanLetterDTO getRandomLetter(int trueFans, int funFans, int reputation, DaySession session) {
        // 旧入口仅传入真粉/乐子粉，独角兽和DD按0处理，保持向后兼容
        return getRandomLetter(trueFans, funFans, 0, 0, reputation, session);
    }

    // 按 4 种粉丝类型各自占比加权随机选类型，再从该类型信件池取信
    public FanLetterDTO getRandomLetter(int trueFans, int funFans, int unicornFans, int ddFans,
                                        int reputation, DaySession session) {
        int total = trueFans + funFans + unicornFans + ddFans;
        if (total <= 0) {
            return getLettersForType("true_fan").get(0);
        }

        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            int rand = ledger.nextInt("fan-letter-type", total);
            String fanType;
            if (rand < trueFans) {
                fanType = "true_fan";
            } else if (rand < trueFans + funFans) {
                fanType = "fun_fan";
            } else if (rand < trueFans + funFans + unicornFans) {
                fanType = "unicorn";
            } else {
                fanType = "dd";
            }
            List<FanLetterDTO> letters = getLettersForType(fanType);
            return letters.get(ledger.nextInt("fan-letter-" + fanType, letters.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public FanLetterDTO getLetterForFanType(String fanType, int count, DaySession session) {
        RngLedger ledger = deterministicRngService.createLedger(session);
        try {
            List<FanLetterDTO> candidates = getLettersForType(fanType);
            return candidates.get(ledger.nextInt("fan-letter-" + fanType, candidates.size()));
        } finally {
            deterministicRngService.finalizeLedger(session, ledger);
        }
    }

    public List<FanLetterDTO> getRecentLetters(int count, int trueFans, int funFans, int reputation, DaySession session) {
        return getRecentLetters(count, trueFans, funFans, 0, 0, reputation, session);
    }

    public List<FanLetterDTO> getRecentLetters(int count, int trueFans, int funFans, int unicornFans, int ddFans,
                                               int reputation, DaySession session) {
        List<FanLetterDTO> letters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            letters.add(getRandomLetter(trueFans, funFans, unicornFans, ddFans, reputation, session));
        }
        return letters;
    }

    public int getLetterCount() {
        int count = TRUE_FAN_LETTERS.size() + FUN_FAN_LETTERS.size() + UNICORN_LETTERS.size() + DD_LETTERS.size();
        // Add DB counts if available
        Map<String, List<ContentCatalogService.ContentEntry>> entries = contentCatalogService.getEntries("FAN_LETTER");
        int dbCount = 0;
        for (List<ContentCatalogService.ContentEntry> list : entries.values()) {
            dbCount += list.size();
        }
        return Math.max(count, dbCount);
    }
}
