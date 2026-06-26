package com.example.vupworld.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NPC类型枚举。统一三套NPC定义的基准。
 * NpcRelationshipService / NpcEventChainService / NpcSpotlightService 共用此枚举，
 * 避免出现"中文名/英文key/角色key"三套互不一致的孤岛。
 *
 * route 字段表示该NPC在事件链中关联的路线（可为null，表示无事件链绑定）。
 */
public enum NpcType {
    SENN_LIN("森岭", "一个专注游戏实况的VUP，性格直爽。", "SINGING_IDOL"),
    MIKU_QI("米库七", "技术流VUP，擅长编程和科技内容。", "SLICE_SAINT"),
    XIAO_YU("小鱼", "治愈系VUP，以温柔的杂谈闻名。", "BLACK_RED_MAIN_STAGE"),
    DA_HAO("大豪", "搞笑艺人型VUP，直播间永远充满欢笑。", "SOCIAL_COLLAB"),
    YUE_YA("月牙", "音乐创作型VUP，原创歌曲深受喜爱。", "DANCE_MEME"),
    BILI_MIKO("B站miko", "老牌VUP，以杂谈和游戏实况为主。", "ELECTRONIC_PICKLE"),
    TUAN_ZI("团子", "新人VUP，热情洋溢，正在努力成长。", null),
    MI_SHE("米设", "美术型VUP，擅长绘画和设计内容。", null);

    private final String displayName;
    private final String description;
    private final String route;

    NpcType(String displayName, String description, String route) {
        this.displayName = displayName;
        this.description = description;
        this.route = route;
    }

    /** 枚举名作为对外稳定的key。 */
    public String key() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String route() {
        return route;
    }

    private static final Map<String, NpcType> KEY_MAP = new LinkedHashMap<>();

    static {
        for (NpcType t : values()) {
            KEY_MAP.put(t.name(), t);
        }
    }

    /** 按 key 查找 NPC，找不到返回 null。 */
    public static NpcType fromKey(String key) {
        if (key == null) {
            return null;
        }
        return KEY_MAP.get(key);
    }

    /** 按路线查找关联的 NPC，找不到返回 null。 */
    public static NpcType forRoute(String route) {
        if (route == null) {
            return null;
        }
        for (NpcType t : values()) {
            if (route.equals(t.route)) {
                return t;
            }
        }
        return null;
    }

    /** 全部 NPC（按声明顺序）。 */
    public static Map<String, NpcType> all() {
        return KEY_MAP;
    }
}
