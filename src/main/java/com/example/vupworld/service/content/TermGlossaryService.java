package com.example.vupworld.service.content;

import com.example.vupworld.dto.MoodDtos.TermDTO;
import com.example.vupworld.service.content.ContentCatalogService.ContentEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 术语词典系统。提供VTuber圈核心术语的解释。
 */
@Component
public class TermGlossaryService {
    private final ContentCatalogService contentCatalogService;

    /** 核心术语硬编码fallback */
    private static final Map<String, TermDTO> DEFAULT_TERMS = new LinkedHashMap<>();

    static {
        DEFAULT_TERMS.put("VUP", new TermDTO(
                "VUP",
                "VUP",
                "Virtual Up主，使用虚拟形象进行内容创作的主播。",
                "新人VUP出道前需要准备好人设和内容规划。"
        ));
        DEFAULT_TERMS.put("TRUE_FAN", new TermDTO(
                "TRUE_FAN",
                "真爱粉",
                "真正欣赏VUP内容和人格的核心粉丝，忠诚度最高。",
                "真爱粉会主动维护VUP的声誉，是VUP最坚实的后盾。"
        ));
        DEFAULT_TERMS.put("FUN_FAN", new TermDTO(
                "FUN_FAN",
                "乐子粉",
                "被VUP的梗和娱乐内容吸引的粉丝，喜欢看热闹。",
                "乐子粉数量多但粘性较低，需要持续输出有趣内容来维持。"
        ));
        DEFAULT_TERMS.put("UNICORN", new TermDTO(
                "UNICORN",
                "独角兽",
                "指对陪伴感和互动回应高度敏感的核心粉丝，常会关注SC高亮留言、舰长称号等生态符号。",
                "独角兽粉能放大陪伴感和边界压力，但不能让运营判断被单一群体绑架。"
        ));
        DEFAULT_TERMS.put("DD", new TermDTO(
                "DD",
                "DD",
                "日语「誰でも大好き」缩写，指同时关注多个VUP的粉丝。",
                "DD粉虽然分散，但能带来跨圈层的曝光。"
        ));
        DEFAULT_TERMS.put("CLIP", new TermDTO(
                "CLIP",
                "切片",
                "将直播中的精彩片段剪辑成短视频，便于传播。",
                "优质切片能带来大量新观众，是增长的重要渠道。"
        ));
        DEFAULT_TERMS.put("MEME_LEVEL", new TermDTO(
                "MEME_LEVEL",
                "梗浓度",
                "VUP内容中梗和流行元素的密集程度。",
                "梗浓度高能吸引乐子粉，但过高可能导致梗疲劳。"
        ));
        DEFAULT_TERMS.put("MI_XIAN", new TermDTO(
                "MI_XIAN",
                "米线",
                "「底线」的谐音梗，指VUP在内容和互动中的边界感。",
                "保持适当的米线能维护VUP的长期形象。"
        ));
        DEFAULT_TERMS.put("SC", new TermDTO(
                "SC",
                "SC",
                "SC在本作中指游戏内模拟的高亮留言/直播间高亮互动，只影响本局生态。",
                "回应SC会影响陪伴感、商业化和边界压力，不连接任何现实交易。"
        ));
        DEFAULT_TERMS.put("COLLAB", new TermDTO(
                "COLLAB",
                "联动",
                "两个或多个VUP一起进行直播或内容创作。",
                "联动能带来双方粉丝的交叉关注，是增长的重要方式。"
        ));
        DEFAULT_TERMS.put("TRIAL", new TermDTO(
                "TRIAL",
                "开庭",
                "指VUP遭遇争议或被质疑的情况，通常引发大量讨论。",
                "开庭期间需要冷静应对，避免事态升级。"
        ));
        DEFAULT_TERMS.put("BUILDING_FRIENDS", new TermDTO(
                "BUILDING_FRIENDS",
                "楼友",
                "指在同一栋虚拟大楼中活动的VUP，即同平台的同行。",
                "楼友之间的良性竞争能促进整个社区的发展。"
        ));
        DEFAULT_TERMS.put("OLD_DEBT", new TermDTO(
                "OLD_DEBT",
                "旧账",
                "指VUP之前积累的负面事件或争议，可能在特定时机被翻出。",
                "旧账是VUP需要持续管理的风险因素。"
        ));
        DEFAULT_TERMS.put("ROUTE", new TermDTO(
                "ROUTE",
                "路线",
                "VUP选择的发展方向，如歌势、舞势、切片圣手等。",
                "确定路线后需要专注投入，频繁切换会影响成长。"
        ));
        DEFAULT_TERMS.put("REPUTATION", new TermDTO(
                "REPUTATION",
                "口碑",
                "VUP在观众和同行中的声誉评价。",
                "口碑是VUP长期发展的基石，需要持续维护。"
        ));
    }

    public TermGlossaryService(ContentCatalogService contentCatalogService) {
        this.contentCatalogService = contentCatalogService;
    }

    /**
     * 获取单个术语。优先从内容目录查询，找不到则用硬编码fallback。
     */
    public TermDTO getTerm(String key) {
        // 先从内容目录查询
        ContentEntry entry = contentCatalogService.getEntry("TERM_GLOSSARY", key);
        if (entry != null) {
            return new TermDTO(
                    key,
                    entry.key(),
                    entry.text() != null ? entry.text() : "",
                    entry.json() != null ? entry.json() : ""
            );
        }

        // 硬编码fallback
        TermDTO fallback = DEFAULT_TERMS.get(key);
        if (fallback != null) {
            return fallback;
        }

        // 最终fallback
        return new TermDTO(key, key, "暂无解释", "");
    }

    /**
     * 获取所有术语列表。优先从内容目录查询，找不到则用硬编码fallback。
     */
    public List<TermDTO> getAllTerms() {
        // 先从内容目录查询
        Map<String, List<ContentEntry>> catalogEntries = contentCatalogService.getEntries("TERM_GLOSSARY");
        if (catalogEntries != null && !catalogEntries.isEmpty()) {
            List<TermDTO> result = new ArrayList<>();
            for (Map.Entry<String, List<ContentEntry>> mapEntry : catalogEntries.entrySet()) {
                String key = mapEntry.getKey();
                List<ContentEntry> entries = mapEntry.getValue();
                if (!entries.isEmpty()) {
                    ContentEntry entry = entries.get(0);
                    result.add(new TermDTO(
                            key,
                            entry.key(),
                            entry.text() != null ? entry.text() : "",
                            entry.json() != null ? entry.json() : ""
                    ));
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        // 硬编码fallback
        return List.copyOf(DEFAULT_TERMS.values());
    }
}
