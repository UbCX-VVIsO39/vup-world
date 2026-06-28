// Extracted from app.js to keep the bootstrap bundle under budget.
const routePortraits = {
    UNKNOWN: "v4/routes/unknown/standing-outfit.png",
    ELECTRONIC_PICKLE: "v4/routes/electronic-pickle/standing-outfit.png",
    SINGING_IDOL: "v4/routes/singing-idol/standing-outfit.png",
    DANCE_MEME: "v4/routes/dance-meme/standing-outfit.png",
    SLICE_SAINT: "v4/routes/slice-saint/standing-outfit.png",
    SOCIAL_COLLAB: "v4/routes/social-collab/standing-outfit.png",
    BLACK_RED_MAIN_STAGE: "v4/routes/black-red-main-stage/standing-outfit.png",
    CYBER_GIRLFRIEND: "v4/routes/cyber-girlfriend/standing-outfit.png",
    DD_BUS_STOP: "v4/routes/dd-bus-stop/standing-outfit.png",
    MAIN_STAGE_KING: "v4/routes/main-stage-king/standing-outfit.png",
    GLORIOUS_GRADUATION: "v4/routes/glorious-graduation/standing-outfit.png"
};

const creationStylePresets = [
    {
        key: "cozy_radio",
        label: "低压杂谈新人",
        badge: "陪饭 / 杂谈",
        name: "露米",
        portrait: "v4/routes/electronic-pickle/standing-outfit.png",
        persona: "低压杂谈新人，歌回偶尔上桌，主打陪饭和老粉回温。",
        personaSignal: "陪饭台稳定开麦，老粉牌面靠稳定陪伴，观众来信优先读不炸锅的。",
        fantasy: "把直播间做成饭点电台，观众不一定狂热，但每天愿意回来坐一会儿。",
        firstMove: "杂谈暖场 + 粉丝群维护",
        strength: "低压陪伴",
        risk: "涨粉慢，靠稳定续航",
        fitRoute: "稳健电子榨菜"
    },
    {
        key: "singing_trainee",
        label: "歌回预备役",
        badge: "歌回 / 练功",
        name: "晴歌",
        portrait: "v4/routes/singing-idol/standing-outfit.png",
        persona: "歌势练习生，直播间常备小歌单，翻唱投稿和打轴切片一起养。",
        personaSignal: "主推歌回和投稿箱，靠灯牌点歌、录播复盘、切片二创慢慢抬声量。",
        fantasy: "把副歌唱成第一批老粉的共同记忆，靠基本功慢慢站上歌回舞台。",
        firstMove: "练歌 + 投稿箱",
        strength: "歌回沉淀",
        risk: "前期热度偏慢",
        fitRoute: "唱歌偶像"
    },
    {
        key: "guofeng_teahouse",
        label: "国风茶馆",
        badge: "茶馆 / 国风",
        name: "知茗",
        portrait: "v4/protagonist/base/base-casual-room.png",
        persona: "国风茶馆系新人，小曲、闲谈和粉丝群茶水摊轮流营业。",
        personaSignal: "主打国风小曲和茶馆杂谈，灯牌叫号像上茶，舰长群气压要稳住。",
        fantasy: "把小曲、茶馆和闲谈揉成一个稳定记忆点，先让观众记住气质。",
        firstMove: "小曲开嗓 + 茶馆杂谈",
        strength: "氛围记忆点",
        risk: "破圈要等切片",
        fitRoute: "稳健电子榨菜"
    },
    {
        key: "clip_workshop",
        label: "素材型新人",
        badge: "切片 / 整活",
        name: "片片",
        portrait: "v4/routes/slice-saint/standing-outfit.png",
        persona: "切片工坊出身，标题组、烤肉组和二创投稿箱都能拉来整活。",
        personaSignal: "素材优先喂给切片组，回旋镖标题要控米线，烤肉组和二创征集别断粮。",
        fantasy: "每天都产出能被剪、能被转、能被二创的素材，让路人从切片认识你。",
        firstMove: "收素材 + 发切片",
        strength: "二创供货",
        risk: "标题容易回旋",
        fitRoute: "切片圣体"
    },
    {
        key: "social_collab_rookie",
        label: "同台社交型",
        badge: "联动 / 接车",
        name: "桥桥",
        portrait: "v4/routes/social-collab/standing-outfit.png",
        persona: "同台社交型新人，擅长接话、约联动和把陌生观众留成二刷。",
        personaSignal: "先靠同行互动和私信维护进场，联动后要把DD流量沉淀成自己的记忆点。",
        fantasy: "别人直播间是入口，你的任务是把每次同台变成下一次回访。",
        firstMove: "同台互动 + 私信维护",
        strength: "关系扩圈",
        risk: "独角兽和站队压力",
        fitRoute: "DD公交站"
    },
    {
        key: "dance_meme_room",
        label: "梗舞练功房",
        badge: "舞蹈 / 短视频",
        name: "跳跳",
        portrait: "v4/routes/dance-meme/standing-outfit.png",
        persona: "梗舞练功房新人，短视频挑战、动作梗和轻整活轮流上桌。",
        personaSignal: "先用练舞和短视频攒动作素材，别让同一个梗跳到楼友开始查重。",
        fantasy: "靠动作梗和短视频先出圈，让观众用一个姿势记住你。",
        firstMove: "练舞 + 短视频",
        strength: "动作梗传播",
        risk: "重复梗会疲劳",
        fitRoute: "切片圣体"
    },
    {
        key: "black_red_courtroom",
        label: "话题型新人",
        badge: "锐评 / 主会场",
        name: "曜曜",
        portrait: "v4/routes/black-red-main-stage/standing-outfit.png",
        persona: "黑红法庭系锐评新人，敢接热度但知道先备好米线工具。",
        personaSignal: "热度、标题和开庭素材都能起飞，但每次嘴硬都要准备证据链和退场台阶。",
        fantasy: "让争议替你开主会场，但每一次上桌都要留下能自保的证据链。",
        firstMove: "标题试爆 + 风险兜底",
        strength: "热度爆发",
        risk: "口碑波动大",
        fitRoute: "黑红主会场"
    },
    {
        key: "botanical_acoustic",
        label: "花房歌会",
        badge: "自然 / 声乐",
        name: "栀栀",
        portrait: "v4/routes/singing-idol/live-halfbody.png",
        persona: "花房歌会新人，温柔声线、清亮小曲和低压读信撑起第一批老粉。",
        personaSignal: "不靠赛博霓虹也能出道，主推舒适歌回、读信和稳定投稿。",
        fantasy: "用清亮声线和读信把观众留下来，先打稳真爱粉底盘。",
        firstMove: "舒适歌回 + 读信",
        strength: "真爱粉黏性",
        risk: "商业热度慢",
        fitRoute: "稳健电子榨菜"
    }
];

// 阶段背景映射
const phaseBackdrops = {
    READY: "v4/commercial-v1/backgrounds/debut-room.png",
    NEED_TITLE: "v4/backgrounds/report-echo-wall.png",
    OFF_STREAM_READY: "v4/backgrounds/day-08-recovery-room.png",
    NEED_INTERACTION_CHOICE: "v4/commercial-v1/backgrounds/event-pressure.png",
    NEED_EVENT_CHOICE: "v4/commercial-v1/backgrounds/event-pressure.png",
    REPORT_READY: "v4/commercial-v1/backgrounds/report-wall.png",
    ENDING_READY: "v4/commercial-v1/backgrounds/ending-stage.png"
};

const routeVisualProfiles = {
    UNKNOWN: {
        slug: "unknown",
        portrait: routePortraits.UNKNOWN,
        cover: "v4/routes/unknown/cover-landscape.png",
        backdrop: "v4/routes/unknown/room-bg.png",
        accent: "#f3c969",
        accent2: "#94b7ff",
        tone: "试播待定"
    },
    ELECTRONIC_PICKLE: {
        slug: "electronic-pickle",
        portrait: routePortraits.ELECTRONIC_PICKLE,
        cover: "v4/routes/electronic-pickle/cover-landscape.png",
        backdrop: "v4/routes/electronic-pickle/room-bg.png",
        accent: "#f0d179",
        accent2: "#78c49a",
        tone: "低压陪饭"
    },
    SINGING_IDOL: {
        slug: "singing-idol",
        portrait: routePortraits.SINGING_IDOL,
        cover: "v4/routes/singing-idol/cover-landscape.png",
        backdrop: "v4/routes/singing-idol/room-bg.png",
        accent: "#ff8fb1",
        accent2: "#76d8ff",
        tone: "小舞台歌回"
    },
    DANCE_MEME: {
        slug: "dance-meme",
        portrait: routePortraits.DANCE_MEME,
        cover: "v4/routes/dance-meme/cover-landscape.png",
        backdrop: "v4/routes/dance-meme/room-bg.png",
        accent: "#8bff7a",
        accent2: "#ffcc4d",
        tone: "短视频练功房"
    },
    SLICE_SAINT: {
        slug: "slice-saint",
        portrait: routePortraits.SLICE_SAINT,
        cover: "v4/routes/slice-saint/cover-landscape.png",
        backdrop: "v4/routes/slice-saint/room-bg.png",
        accent: "#55e6ff",
        accent2: "#f0ff6a",
        tone: "切片工坊"
    },
    SOCIAL_COLLAB: {
        slug: "social-collab",
        portrait: routePortraits.SOCIAL_COLLAB,
        cover: "v4/routes/social-collab/cover-landscape.png",
        backdrop: "v4/routes/social-collab/room-bg.png",
        accent: "#72d7ff",
        accent2: "#ffb86b",
        tone: "联动候场"
    },
    BLACK_RED_MAIN_STAGE: {
        slug: "black-red-main-stage",
        portrait: routePortraits.BLACK_RED_MAIN_STAGE,
        cover: "v4/routes/black-red-main-stage/cover-landscape.png",
        backdrop: "v4/routes/black-red-main-stage/room-bg.png",
        accent: "#ff4f6f",
        accent2: "#f3c969",
        tone: "舆论法庭"
    },
    CYBER_GIRLFRIEND: {
        slug: "cyber-girlfriend",
        portrait: routePortraits.CYBER_GIRLFRIEND,
        cover: "v4/routes/cyber-girlfriend/cover-landscape.png",
        backdrop: "v4/routes/cyber-girlfriend/room-bg.png",
        accent: "#6ff7ff",
        accent2: "#ff7bd5",
        tone: "私信霓虹"
    },
    DD_BUS_STOP: {
        slug: "dd-bus-stop",
        portrait: routePortraits.DD_BUS_STOP,
        cover: "v4/routes/dd-bus-stop/cover-landscape.png",
        backdrop: "v4/routes/dd-bus-stop/room-bg.png",
        accent: "#66e2b3",
        accent2: "#ffd36e",
        tone: "霓虹站台"
    },
    MAIN_STAGE_KING: {
        slug: "main-stage-king",
        portrait: routePortraits.MAIN_STAGE_KING,
        cover: "v4/routes/main-stage-king/cover-landscape.png",
        backdrop: "v4/routes/main-stage-king/room-bg.png",
        accent: "#ff3d4f",
        accent2: "#f7f2a0",
        tone: "主会场压轴"
    },
    GLORIOUS_GRADUATION: {
        slug: "glorious-graduation",
        portrait: routePortraits.GLORIOUS_GRADUATION,
        cover: "v4/routes/glorious-graduation/cover-landscape.png",
        backdrop: "v4/routes/glorious-graduation/room-bg.png",
        accent: "#f5e6a5",
        accent2: "#8bd3ff",
        tone: "毕业舞台"
    }
};

// ============ 路线主题系统 ============
const ROUTE_THEMES = {
    UNKNOWN: {
        name: '查无此V',
        emoji: '?',
        bgGradient: 'linear-gradient(135deg, rgba(148, 163, 184, 0.08), rgba(243, 201, 105, 0.08))',
        cardBorder: 'rgba(148, 163, 184, 0.28)',
        motto: '先让观众记住一个方向',
        actionEmoji: { REST: '...', TRAIN_TALK: '...', STREAM_PLAN: '...' },
    },
    SINGING_IDOL: {
        name: '歌势偶像',
        emoji: '🎤',
        bgGradient: 'linear-gradient(135deg, rgba(255, 143, 177, 0.08), rgba(118, 216, 255, 0.08))',
        cardBorder: 'rgba(255, 143, 177, 0.3)',
        motto: '用歌声打动每一个人',
        actionEmoji: { TRAIN_SONG: '🎤', STREAM_PLAN: '🎵', REST: '😴' },
    },
    SLICE_SAINT: {
        name: '切片圣体',
        emoji: '✂️',
        bgGradient: 'linear-gradient(135deg, rgba(85, 230, 255, 0.08), rgba(240, 255, 106, 0.08))',
        cardBorder: 'rgba(85, 230, 255, 0.3)',
        motto: '每一帧都是素材',
        actionEmoji: { PUBLISH_CLIP: '✂️', PUBLISH_VIDEO: '📹', REST: '😴' },
    },
    ELECTRONIC_PICKLE: {
        name: '电子榨菜',
        emoji: '🥒',
        bgGradient: 'linear-gradient(135deg, rgba(240, 209, 121, 0.08), rgba(120, 196, 154, 0.08))',
        cardBorder: 'rgba(240, 209, 121, 0.3)',
        motto: '陪你度过每一个平淡的日子',
        actionEmoji: { TRAIN_TALK: '☕', FAN_GROUP_MAINTAIN: '💌', REST: '😴' },
    },
    BLACK_RED_MAIN_STAGE: {
        name: '黑红主会场',
        emoji: '⚖️',
        bgGradient: 'linear-gradient(135deg, rgba(255, 79, 111, 0.08), rgba(243, 201, 105, 0.08))',
        cardBorder: 'rgba(255, 79, 111, 0.3)',
        motto: '争议就是流量',
        actionEmoji: { STREAM_PLAN: '🔥', NPC_INTERACT: '⚖️', REST: '😴' },
    },
    SOCIAL_COLLAB: {
        name: '社交联动',
        emoji: '🤝',
        bgGradient: 'linear-gradient(135deg, rgba(114, 215, 255, 0.08), rgba(255, 184, 107, 0.08))',
        cardBorder: 'rgba(114, 215, 255, 0.3)',
        motto: '朋友多了路好走',
        actionEmoji: { NPC_INTERACT: '🤝', FAN_GROUP_MAINTAIN: '💬', REST: '😴' },
    },
    DANCE_MEME: {
        name: '梗舞整活',
        emoji: '💃',
        bgGradient: 'linear-gradient(135deg, rgba(139, 255, 122, 0.08), rgba(255, 204, 77, 0.08))',
        cardBorder: 'rgba(139, 255, 122, 0.3)',
        motto: '下一个爆款就是我',
        actionEmoji: { TRAIN_DANCE: '💃', PUBLISH_CLIP: '📱', REST: '😴' },
    },
    CYBER_GIRLFRIEND: {
        name: '赛博女友',
        emoji: '💗',
        bgGradient: 'linear-gradient(135deg, rgba(111, 247, 255, 0.08), rgba(255, 123, 213, 0.08))',
        cardBorder: 'rgba(255, 123, 213, 0.3)',
        motto: '陪伴感也是战斗力',
        actionEmoji: { FAN_GROUP_MAINTAIN: '💌', TRAIN_TALK: '☕', STREAM_PLAN: '💗', REST: '😴' },
    },
    DD_BUS_STOP: {
        name: 'DD公交站',
        emoji: '🚌',
        bgGradient: 'linear-gradient(135deg, rgba(102, 226, 179, 0.08), rgba(255, 211, 110, 0.08))',
        cardBorder: 'rgba(102, 226, 179, 0.3)',
        motto: '每次同台都要留下回访理由',
        actionEmoji: { NPC_INTERACT: '🤝', FAN_GROUP_MAINTAIN: '🚌', STREAM_PLAN: '🎪', REST: '😴' },
    },
    MAIN_STAGE_KING: {
        name: '主会场之王',
        emoji: '👑',
        bgGradient: 'linear-gradient(135deg, rgba(255, 61, 79, 0.08), rgba(247, 242, 160, 0.08))',
        cardBorder: 'rgba(255, 61, 79, 0.3)',
        motto: '不是被讨论，是掌控讨论',
        actionEmoji: { STREAM_PLAN: '🔥', NPC_INTERACT: '👑', PUBLISH_CLIP: '✂️', REST: '😴' },
    },
    GLORIOUS_GRADUATION: {
        name: '光荣毕业',
        emoji: '🎓',
        bgGradient: 'linear-gradient(135deg, rgba(245, 230, 165, 0.08), rgba(139, 211, 255, 0.08))',
        cardBorder: 'rgba(245, 230, 165, 0.3)',
        motto: '把30天收成一个体面告别',
        actionEmoji: { FAN_GROUP_MAINTAIN: '💌', TRAIN_TALK: '☕', REST: '🎓' },
    },
};

const ROUTE_GALLERY_ORDER = [
    "ELECTRONIC_PICKLE",
    "SINGING_IDOL",
    "DANCE_MEME",
    "SLICE_SAINT",
    "SOCIAL_COLLAB",
    "BLACK_RED_MAIN_STAGE",
    "CYBER_GIRLFRIEND",
    "DD_BUS_STOP",
    "MAIN_STAGE_KING",
    "GLORIOUS_GRADUATION",
    "UNKNOWN"
];

// 事件插画映射
const endingImages = {
    UNKNOWN: "v4/routes/unknown/ending-highlight.png",
    ELECTRONIC_PICKLE: "v4/routes/electronic-pickle/ending-highlight.png",
    SINGING_IDOL: "v4/routes/singing-idol/ending-highlight.png",
    DANCE_MEME: "v4/routes/dance-meme/ending-highlight.png",
    SLICE_SAINT: "v4/routes/slice-saint/ending-highlight.png",
    SOCIAL_COLLAB: "v4/routes/social-collab/ending-highlight.png",
    BLACK_RED_MAIN_STAGE: "v4/routes/black-red-main-stage/ending-highlight.png",
    CYBER_GIRLFRIEND: "v4/routes/cyber-girlfriend/ending-highlight.png",
    DD_BUS_STOP: "v4/routes/dd-bus-stop/ending-highlight.png",
    MAIN_STAGE_KING: "v4/routes/main-stage-king/ending-highlight.png",
    GLORIOUS_GRADUATION: "v4/routes/glorious-graduation/ending-highlight.png"
};

const endingCostImages = {
    UNKNOWN: "v4/routes/unknown/ending-cost.png",
    ELECTRONIC_PICKLE: "v4/routes/electronic-pickle/ending-cost.png",
    SINGING_IDOL: "v4/routes/singing-idol/ending-cost.png",
    DANCE_MEME: "v4/routes/dance-meme/ending-cost.png",
    SLICE_SAINT: "v4/routes/slice-saint/ending-cost.png",
    SOCIAL_COLLAB: "v4/routes/social-collab/ending-cost.png",
    BLACK_RED_MAIN_STAGE: "v4/routes/black-red-main-stage/ending-cost.png",
    CYBER_GIRLFRIEND: "v4/routes/cyber-girlfriend/ending-cost.png",
    DD_BUS_STOP: "v4/routes/dd-bus-stop/ending-cost.png",
    MAIN_STAGE_KING: "v4/routes/main-stage-king/ending-cost.png",
    GLORIOUS_GRADUATION: "v4/routes/glorious-graduation/ending-cost.png"
};

const eventIllustrations = {
    TITLE_BACKLASH: "v4/events/common/title-backlash.png",
    CLIP_EXPLOSION: "v4/events/common/clip-explosion.png",
    UNICORN_EXPECTATION: "v4/events/common/unicorn-expectation.png",
    COMMERCIAL_BACKLASH: "v4/events/common/commercial-backlash.png",
    COLLAB_RUMOR: "v4/events/common/collab-rumor.png",
    HOT_MIC_MISFIRE: "v4/events/common/hot-mic-misfire.png",
    APOLOGY_REVIEW: "v4/events/common/apology-review.png",
    BLACK_RED_BREAKTHROUGH: "v4/events/common/black-red-breakthrough.png",
    PICKLE_SIGNATURE: "v4/events/routes/electronic-pickle/signature.png",
    SLICE_SIGNATURE: "v4/events/routes/slice-saint/signature.png",
    BLACK_RED_SIGNATURE: "v4/events/routes/black-red-main-stage/signature.png"
};

const routeExpressionImages = {
    UNKNOWN: {
        soft: "v4/routes/unknown/expression-soft.png",
        nervous: "v4/routes/unknown/expression-nervous.png",
        smug: "v4/routes/unknown/expression-smug.png",
        breakdown: "v4/routes/unknown/expression-breakdown.png"
    },
    ELECTRONIC_PICKLE: {
        soft: "v4/routes/electronic-pickle/expression-soft.png",
        nervous: "v4/routes/electronic-pickle/expression-nervous.png",
        smug: "v4/routes/electronic-pickle/expression-smug.png",
        breakdown: "v4/routes/electronic-pickle/expression-breakdown.png"
    },
    SINGING_IDOL: {
        soft: "v4/routes/singing-idol/expression-soft.png",
        nervous: "v4/routes/singing-idol/expression-nervous.png",
        smug: "v4/routes/singing-idol/expression-smug.png",
        breakdown: "v4/routes/singing-idol/expression-breakdown.png"
    },
    DANCE_MEME: {
        soft: "v4/routes/dance-meme/expression-soft.png",
        nervous: "v4/routes/dance-meme/expression-nervous.png",
        smug: "v4/routes/dance-meme/expression-smug.png",
        breakdown: "v4/routes/dance-meme/expression-breakdown.png"
    },
    SLICE_SAINT: {
        soft: "v4/routes/slice-saint/expression-soft.png",
        nervous: "v4/routes/slice-saint/expression-nervous.png",
        smug: "v4/routes/slice-saint/expression-smug.png",
        breakdown: "v4/routes/slice-saint/expression-breakdown.png"
    },
    SOCIAL_COLLAB: {
        soft: "v4/routes/social-collab/expression-soft.png",
        nervous: "v4/routes/social-collab/expression-nervous.png",
        smug: "v4/routes/social-collab/expression-smug.png",
        breakdown: "v4/routes/social-collab/expression-breakdown.png"
    },
    BLACK_RED_MAIN_STAGE: {
        soft: "v4/routes/black-red-main-stage/expression-soft.png",
        nervous: "v4/routes/black-red-main-stage/expression-nervous.png",
        smug: "v4/routes/black-red-main-stage/expression-smug.png",
        breakdown: "v4/routes/black-red-main-stage/expression-breakdown.png"
    },
    CYBER_GIRLFRIEND: {
        soft: "v4/routes/cyber-girlfriend/expression-soft.png",
        nervous: "v4/routes/cyber-girlfriend/expression-nervous.png",
        smug: "v4/routes/cyber-girlfriend/expression-smug.png",
        breakdown: "v4/routes/cyber-girlfriend/expression-breakdown.png"
    },
    DD_BUS_STOP: {
        soft: "v4/routes/dd-bus-stop/expression-soft.png",
        nervous: "v4/routes/dd-bus-stop/expression-nervous.png",
        smug: "v4/routes/dd-bus-stop/expression-smug.png",
        breakdown: "v4/routes/dd-bus-stop/expression-breakdown.png"
    },
    MAIN_STAGE_KING: {
        soft: "v4/routes/main-stage-king/expression-soft.png",
        nervous: "v4/routes/main-stage-king/expression-nervous.png",
        smug: "v4/routes/main-stage-king/expression-smug.png",
        breakdown: "v4/routes/main-stage-king/expression-breakdown.png"
    },
    GLORIOUS_GRADUATION: {
        soft: "v4/routes/glorious-graduation/expression-soft.png",
        nervous: "v4/routes/glorious-graduation/expression-nervous.png",
        smug: "v4/routes/glorious-graduation/expression-smug.png",
        breakdown: "v4/routes/glorious-graduation/expression-breakdown.png"
    }
};

// 禁用原因翻译
const disabledReasonCopy = {
    PHASE_NOT_ALLOWED: "当前阶段不能选，先把今天的流程收完。",
    STREAM_PLAN_CANCELLED_TODAY: "今天取消过直播企划，不能再选了。",
    INSUFFICIENT_STAMINA: "体力不够，先休息恢复一下。",
    INSUFFICIENT_INSPIRATION: "灵感不足，先做点别的攒灵感。",
    INSUFFICIENT_COIN: "运营预算不够。",
    INSUFFICIENT_MATERIAL_STOCK: "素材库为0，切片组不能空剪。先发布视频、开直播企划，或在粉丝群议题里改成投稿征集。",
    OPERATIONAL_PRESSURE_LOCKED: "当前压力已经把这条线锁住了，先换个动作稳住局面。",
    RISK_TOOL_DAILY_LIMIT: "今天已经用过风险工具了，明天再来。",
    RISK_TOOL_NO_TARGET: "没有需要处理的风险，不用操作。",
    FAN_TOPIC_ALREADY_HANDLED: "今天已经处理过粉丝群议题了。",
    P0_NOT_AVAILABLE: "本版暂未开放，先用其他方案。"
};

const quickDisabledReasonCopy = {
    INSUFFICIENT_MATERIAL_STOCK: "缺素材，去投稿箱"
};

const quickMaterialStockGuidance = "先投视频或进粉丝群投稿箱补素材";

const errorCodeCopy = {
    ...disabledReasonCopy,
    UNAUTHORIZED: "登录过期了，请重新登录。",
    ACTIVE_VUP_EXISTS: "已经有一个进行中的角色，不能重复创建。",
    VUP_NOT_FOUND: "还没有角色，先创建一个再开始。",
    CONFIG_FIELD_INVALID: "填写内容有误，请检查后重试。",
    IDEMPOTENCY_CONFLICT: "操作重复了，请刷新页面后重试。",
    SYSTEM_ERROR: "系统出错了，请刷新页面。",
    EVENT_NOT_PENDING: "当前没有待处理事件。",
    CHOICE_NOT_AVAILABLE: "这个选项不可用，请选另一个。",
    REPORT_NOT_FOUND: "今天的日报还没生成。",
    RISK_TOOL_NOT_AVAILABLE: "风险工具当前不可用。",
    RESTART_NOT_ALLOWED: "还没打完整轮，不能重新开始。",
    RESTART_CONFIRM_REQUIRED: "重新开始会结束当前进度，确定吗？",
    TITLE_REROLL_LIMIT: "今天已经换过标题了，不能再换了。",
    DAY_LIMIT_REACHED: "本轮已经结束了，去看结局吧。",
    DEV_TOOL_DISABLED: "开发验证工具未开启。",
    USERNAME_EXISTS: "用户名已被占用，请换一个。",
    TITLE_NOT_AVAILABLE: "这个标题不可用了，请刷新。",
    ENDING_NOT_FOUND: "结局还没生成，请先完成本轮。",
    DEMO_SCENARIO_NOT_SUPPORTED: "验证剧本暂不支持。",
    DEMO_STRATEGY_NOT_SUPPORTED: "验证策略暂不支持。",
    DEMO_RUN_SEED_MISMATCH: "验证种子不匹配，请重置。",
    NPC_INTERACTION_COOLDOWN: "同台互动还在冷却中，明天再来。"
};

function errorCodeCopyText(code) {
    const maxDayLabel = runMaxDayLabel();
    const dynamicCopy = {
        RESTART_NOT_ALLOWED: `还没打完${maxDayLabel}，不能重新开始。`,
        DAY_LIMIT_REACHED: `${maxDayLabel}已经结束了，去看结局吧。`,
        ENDING_NOT_FOUND: `结局还没生成，请先完成${maxDayLabel}。`
    };
    return dynamicCopy[code] || errorCodeCopy[code] || "";
}

function disabledReasonText(reason) {
    return disabledReasonCopy[reason] || visibleTextOrFallback(reason, "暂不可用，先推进当前流程。");
}

function quickDisabledReasonText(reason) {
    return quickDisabledReasonCopy[reason] || disabledReasonText(reason);
}

function visibleTextOrFallback(value, fallback) {
    const text = String(value || "").trim();
    if (!text) return fallback;
    return hasUnsafeVisibleToken(text) ? fallback : text;
}

function firstReadableText(values, fallback) {
    for (const value of values || []) {
        const text = visibleTextOrFallback(value, "");
        if (text) return text;
    }
    return fallback;
}

function firstLocalizedReadableText(values, fallback) {
    for (const value of values || []) {
        const text = localizedVisibleTextOrFallback(value, "");
        if (text) return text;
    }
    return fallback;
}

function hasUnsafeVisibleToken(value) {
    const text = String(value || "");
    return /\b[A-Z]{2,}(?:_[A-Z0-9]+)+\b|\b[a-z][a-z0-9]+(?:_[a-z0-9]+)+\b|[A-Za-z0-9_-]+\.(?:png|jpe?g|webp|json)\b|(?:[A-Za-z]:)?[\\/][A-Za-z0-9_.-]+(?:[\\/][A-Za-z0-9_.-]+)+|undefined|null|NaN|Ã|Â|æ|ç|è/.test(text)
        || /^[A-Z0-9_]+$/.test(text.trim());
}

const BUDGET_RESOURCE_PATTERN = "\\u91d1\\u5e01";
const MONEY_RESOURCE_PATTERN = "\\u91d1\\u94b1";

function localizedMessageText(value) {
    return localizedVisibleTextOrFallback(value, "");
}

function localizedVisibleTextOrFallback(value, fallback) {
    const text = visibleTextOrFallback(value, fallback);
    return /[\u4e00-\u9fff]/.test(String(text || "")) ? normalizeNoPayCopy(text) : fallback;
}

function normalizeNoPayCopy(value) {
    const coinToken = "\u91d1\u5e01";
    const moneyToken = "\u91d1\u94b1";
    const sendToken = "\u8d60\u9001\u4e86";
    const tipToken = "\u6253\u8d4f";
    const payToken = "\u82b1\u94b1";
    return String(value || "")
        .replaceAll(`${coinToken}/体力`, "运营预算/体力")
        .replaceAll(coinToken, "运营预算")
        .replaceAll(moneyToken, "运营预算")
        .replaceAll(sendToken, "模拟互动")
        .replaceAll(tipToken, "互动支持")
        .replaceAll(payToken, "投入感");
}

function localizedFieldText(value, fallback = "") {
    if (value && typeof value === "object") {
        return firstLocalizedReadableText([
            value.label,
            value.title,
            value.summary,
            value.description,
            value.line,
            value.text,
            value.value,
            value.name
        ], fallback);
    }
    return localizedVisibleTextOrFallback(value, fallback);
}

function statusMessageText(value) {
    return localizedVisibleTextOrFallback(value, "状态提示已隐藏原始代码，先刷新当前进度再试。");
}

function eventTitleText(event) {
    return localizedVisibleTextOrFallback(event?.title, "突发事件");
}

function eventDescriptionText(event) {
    return localizedVisibleTextOrFallback(event?.description, "现场记录还在整理，先看可选处理方案。");
}

function eventVisibleDescriptionText(event) {
    const fullText = eventDescriptionText(event);
    const firstSentence = String(fullText || "").split(/(?<=[。！？!?；;])\s*/)[0]?.trim() || fullText;
    return firstSentence.length > 44 ? `${firstSentence.slice(0, 44)}...` : firstSentence;
}

function eventBackgroundDetailsHtml(fullText, visibleText) {
    if (!fullText || fullText === visibleText || fullText.length <= visibleText.length + 4) {
        return "";
    }
    return `
        <details class="event-background-details">
            <summary>展开长背景和氛围</summary>
            <p>${html(fullText)}</p>
        </details>
    `;
}

function isOrdinaryFormalEvent(event) {
    if (state.session?.phase !== "NEED_EVENT_CHOICE") {
        return false;
    }
    const source = String(event?.sourceTitle || "");
    return (event?.debtId === null || event?.debtId === undefined)
        && (source === "RANDOM_EVENT"
            || source === "REST_SAVED_MELTDOWN"
            || source === "MIDGAME_EVENT"
            || source === "LATE_GAME_EVENT");
}

function ordinaryEventTypeText(event) {
    const raw = String(event?.debtType || event?.eventType || "").trim();
    const labels = {
        POSITIVE: "正向事件",
        NEGATIVE: "压力事件",
        MIXED: "混合事件",
        NEUTRAL: "中性事件",
        ORDINARY_EVENT: "临时事件",
        REST_SAVED_MELTDOWN: "低压救场"
    };
    return labels[raw] || "临时事件";
}

function eventChoiceLabelText(choice) {
    return localizedVisibleTextOrFallback(choice?.label, "备选方案");
}

function eventChoiceMetaText(value) {
    return localizedVisibleTextOrFallback(value, "场务还在整理数据");
}

function eventChoiceCtaText(choice, index) {
    const label = eventChoiceLabelText(choice);
    const effect = eventChoiceMetaText(choice?.effectPreview || choice?.riskPreview);
    const cost = choice?.costPreview ? eventChoiceMetaText(choice.costPreview) : "";
    const rawKey = [choice?.choiceId, choice?.choiceType].filter(Boolean).join(" ");
    const text = [label, effect, cost, rawKey].join(" ");
    if (/米线|稳定|稳住|澄清|解释|道歉|压住|老粉|口碑|避险|降温|复盘/.test(text)) {
        return "稳住米线";
    }
    if (/热度|围观|热搜|涨粉|流量|主会场|乐子|爆|冲榜|出圈/.test(text)) {
        return "接住热度";
    }
    if (/整点|节目|玩梗|切片|投稿|弹幕|互动|抽奖|连麦|活\b|节目效果/.test(text)) {
        return "整点节目";
    }
    return index >= 0 ? "选此方案" : "选此方案";
}

function formatDayBadge(day) {
    return day ? `第${day}天` : "第--天";
}

function runMaxDay() {
    const maxDay = Number(state.vup?.maxDay);
    return Number.isFinite(maxDay) && maxDay > 0 ? maxDay : 30;
}

function runMaxDayLabel() {
    return `${runMaxDay()}天`;
}

function runFinalDayLabel() {
    return `第${runMaxDay()}天`;
}

function lateGameReadyPhase(day = Number(state.session?.day || state.vup?.day || 1)) {
    const currentDay = Number(day) || 1;
    if (currentDay >= 28) {
        return {
            label: "战报前夜",
            planLine: "战报前夜，每手收益都会进结局复盘。优先补最后的路线证据，别把旧账带进收官。",
            coachLine: "战报前夜：最后几天的收益会进结局复盘，今天的选择会变成最终战报证据。",
            boardLine: "最后一次修正窗口，收益、代价和旧账都会被结局复盘点名。"
        };
    }
    if (currentDay >= 26) {
        return {
            label: "结局锁线",
            planLine: "结局锁线期，最后几天的收益会进结局复盘。先补结局门槛，再决定是否冲热度。",
            coachLine: "结局锁线：最后几天的收益会进结局复盘，推荐先补最短的结局缺口。",
            boardLine: "路线基本定型，今天更适合把短板补到能过结局门槛。"
        };
    }
    if (currentDay >= 24) {
        return {
            label: "收官压力",
            planLine: "收官压力已经出现，最后几天的收益会进结局复盘。看清风险、路线缺口，再点推荐行动。",
            coachLine: "收官压力：最后几天的收益会进结局复盘，推荐理由会优先看风险和路线证据。",
            boardLine: "最后一周开始算总账，行动收益会被结局评分和关键事件一起记录。"
        };
    }
    return null;
}

function demoTargetDay() {
    return runMaxDay();
}

const routeLabelNames = {
    UNKNOWN: "待定路线",
    ELECTRONIC_PICKLE: "电子榨菜",
    SINGING_IDOL: "歌势偶像",
    DANCE_MEME: "梗舞整活",
    SLICE_SAINT: "切片圣体",
    SOCIAL_COLLAB: "社交联动",
    BLACK_RED_MAIN_STAGE: "黑红主会场",
    CYBER_GIRLFRIEND: "赛博女友",
    DD_BUS_STOP: "DD公交站",
    MAIN_STAGE_KING: "主会场之王",
    GLORIOUS_GRADUATION: "光荣毕业"
};

function routeLabelFor(routeKey) {
    return routeLabelNames[routeKey] || visibleTextOrFallback(routeKey, "待定路线");
}

const routePlaybook = {
    UNKNOWN: {
        fantasy: "先定路线：还没有形成标签，今天最重要的是先让观众记住一个方向。",
        cost: "不定型会让结局证据变薄，第30天容易变成查无此V。",
        next: "先定路线，选一个推荐行动连续做两三天。"
    },
    ELECTRONIC_PICKLE: {
        fantasy: "稳定陪伴，低压成长，让直播间变成可回放的饭点。",
        cost: "爆点少，出圈慢，不能指望热搜替你长粉。",
        next: "杂谈复盘、粉丝群维护和低风险直播最稳。"
    },
    SINGING_IDOL: {
        fantasy: "靠作品和基本功留人，把副歌唱成老粉记忆点。",
        cost: "成长慢，需要连续投入，断档会被其它路线稀释。",
        next: "练歌、歌回和投稿要持续露面。"
    },
    DANCE_MEME: {
        fantasy: "舞蹈和短视频先爆，切片区替你预热。",
        cost: "如果只剩动作梗，本人会被素材反向定义。",
        next: "练舞后接切片或短视频，别只做低压直播。"
    },
    SLICE_SAINT: {
        fantasy: "每天都有名场面，素材像自己排队等剪。",
        cost: "梗浓度越高，越容易被乐子人推着走。",
        next: "发布切片、补素材、制造可剪的直播段落。"
    },
    SOCIAL_COLLAB: {
        fantasy: "靠联动接车，把关系网变成流量入口。",
        cost: "自我标签容易变弱，粉丝可能只是路过。",
        next: "同台互动后用粉丝群维护留住人。"
    },
    BLACK_RED_MAIN_STAGE: {
        fantasy: "争议换热度，让主会场开始替你记笔记。",
        cost: "旧账和口碑会一起涨红灯，失控就翻车。",
        next: "高风险标题要配下播降温或风险工具。"
    },
    CYBER_GIRLFRIEND: {
        fantasy: "陪伴感拉满，粘性和商业反馈都很好看。",
        cost: "边界压力会上升，陪伴债不处理会反扑。",
        next: "回应高亮互动和陪伴营业之后，记得维护边界。"
    },
    DD_BUS_STOP: {
        fantasy: "联动不断，大家都在这一站上下车。",
        cost: "DD占比太高时，观众记住的是关系不是你。",
        next: "联动后补一次个人内容，把路过粉留下。"
    },
    MAIN_STAGE_KING: {
        fantasy: "你不只是被讨论，而是能掌控讨论节奏。",
        cost: "这是高维护路线，热度、旧账、素材都要压住。",
        next: "黑红行动后接控场、切片和降温。"
    },
    GLORIOUS_GRADUATION: {
        fantasy: "不被流量抬走，自己体面走下舞台。",
        cost: "爆发不足，不一定爽，但结局尊严感更强。",
        next: "稳口碑、清旧账、维护粉丝群。"
    }
};

function routePlaybookFor(routeKey) {
    return routePlaybook[routeKey] || routePlaybook.UNKNOWN;
}

function routeScoreEntries() {
    const raw = state.vup?.route?.routeScore
        || state.vup?.routeScores
        || state.vup?.routeScore
        || state.vup?.routeScoreJson
        || state.ending?.routeReview?.routeScoreJson
        || state.ending?.routeReviewJson;
    let scores = raw;
    if (typeof raw === "string") {
        try {
            const parsed = JSON.parse(raw);
            scores = parsed?.routeScoreJson || parsed?.routeScore || parsed;
            if (typeof scores === "string") {
                scores = JSON.parse(scores);
            }
        } catch (error) {
            scores = {};
        }
    }
    if (!scores || typeof scores !== "object" || Array.isArray(scores)) {
        scores = {};
    }
    const entries = Object.entries(scores)
        .map(([route, value]) => ({
            route,
            score: Number(value) || 0
        }))
        .filter(item => item.route && item.route !== "source" && item.score > 0)
        .sort((left, right) => right.score - left.score);
    const currentRoute = state.vup?.currentRoute || state.endingForecast?.likelyEndingType || "UNKNOWN";
    if (currentRoute && currentRoute !== "UNKNOWN" && !entries.some(item => item.route === currentRoute)) {
        entries.push({ route: currentRoute, score: 1 });
    }
    return entries;
}

function routeScoreFor(routeKey) {
    return routeScoreEntries().find(item => item.route === routeKey)?.score || 0;
}

function stageRhythmSummary(briefing = state.stageBriefing) {
    const rhythm = briefing?.stageRhythm;
    if (!rhythm) return "";
    const phaseName = localizedVisibleTextOrFallback(rhythm.phaseName, "");
    const startDay = Number(rhythm.startDay || 0);
    const endDay = Number(rhythm.endDay || 0);
    const reason = localizedVisibleTextOrFallback(rhythm.reason, "");
    const range = startDay && endDay ? `第${startDay}-${endDay}天` : "";
    return [phaseName, range, reportShortSentence(reason, "", 34)].filter(Boolean).join(" · ");
}

function routeTendencySummary(briefing = state.stageBriefing) {
    const tendency = briefing?.routeTendency;
    if (!tendency?.primaryRoute) return "";
    const primary = tendency.primaryRoute;
    const routeName = localizedVisibleTextOrFallback(primary.label, routeLabelFor(primary.routeType || "UNKNOWN"));
    const score = Number(primary.score || 0);
    const rivals = Array.isArray(tendency.competingRoutes)
        ? tendency.competingRoutes.slice(0, 2).map(route => localizedVisibleTextOrFallback(route?.label, routeLabelFor(route?.routeType || "UNKNOWN"))).filter(Boolean)
        : [];
    const reason = localizedVisibleTextOrFallback(tendency.reason, "");
    const scoreLine = score > 0 ? `${score}分` : "证据未定";
    const rivalLine = rivals.length ? `竞争：${rivals.join(" / ")}` : "";
    return [routeName, scoreLine, rivalLine, reportShortSentence(reason, "", 30)].filter(Boolean).join(" · ");
}

function routeVisibilityMilestones(likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN") {
    const scores = routeScoreEntries();
    const maxScore = Math.max(8, ...scores.map(item => item.score), Number(firstWarnEndingRequirement()?.targetValue || 0));
    const primaryActions = primaryRouteActionTypes(likelyEnding)
        .slice(0, 3)
        .map(actionLabelFor)
        .join(" / ");
    const gapLine = endingGapCoachLine(firstWarnEndingRequirement(), { likelyEnding });
    const fallback = [
        {
            route: likelyEnding || "UNKNOWN",
            score: routeScoreFor(likelyEnding) || 0,
            label: routeLabelFor(likelyEnding),
            hint: primaryActions ? `下一手：${primaryActions}` : "先做一个同路线行动",
            tone: "target"
        },
        {
            route: state.vup?.currentRoute || "UNKNOWN",
            score: routeScoreFor(state.vup?.currentRoute) || 0,
            label: routeLabelFor(state.vup?.currentRoute || "UNKNOWN"),
            hint: gapLine,
            tone: "current"
        }
    ];
    const fromScores = scores.slice(0, 4).map((item, index) => ({
        route: item.route,
        score: item.score,
        label: routeLabelFor(item.route),
        hint: index === 0 ? routePlaybookFor(item.route).next : routePlaybookFor(item.route).cost,
        tone: item.route === likelyEnding ? "target" : (item.route === state.vup?.currentRoute ? "current" : "other")
    }));
    const items = (fromScores.length ? fromScores : fallback)
        .filter((item, index, list) => item.route && list.findIndex(other => other.route === item.route) === index)
        .slice(0, 4);
    return {
        maxScore,
        items,
        primaryActions,
        gapLine
    };
}

function renderRouteVisibilityMap(likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN", compact = false) {
    const { maxScore, items, primaryActions, gapLine } = routeVisibilityMilestones(likelyEnding);
    const leading = items[0];
    const routeTitle = routeLabelFor(likelyEnding || leading?.route || "UNKNOWN");
    const body = items.map(item => {
        const width = percent((item.score / Math.max(1, maxScore)) * 100);
        const active = item.route === likelyEnding || item.route === state.vup?.currentRoute;
        return `
            <span class="route-visibility-row ${html(item.tone)} ${active ? "active" : ""}">
                <em>${html(item.label)}</em>
                <i aria-hidden="true"><b style="width:${width}%"></b></i>
                <strong>${html(item.score > 0 ? `${item.score}分` : "未起势")}</strong>
                <small>${html(item.hint)}</small>
            </span>
        `;
    }).join("");
    return `
        <section class="route-visibility-map ${compact ? "compact" : ""}" aria-label="路线可视化">
            <div class="route-visibility-head">
                <div>
                    <span>路线地图</span>
                    <strong>${html(routeTitle)}</strong>
                </div>
                <small>${html(primaryActions || "同路线行动会加厚结局证据")}</small>
            </div>
            <div class="route-visibility-list">${body}</div>
            <p>${html(gapLine || "路线门槛暂稳，继续补同一路线证据。")}</p>
        </section>
    `;
}

function riskCrisisItems() {
    const debts = activeDebts();
    const items = debts
        .sort((left, right) => {
            const dueDelta = debtDaysLeft(left) - debtDaysLeft(right);
            return dueDelta !== 0 ? dueDelta : (Number(right?.severity) || 0) - (Number(left?.severity) || 0);
        })
        .slice(0, 3)
        .map(debt => {
            const tone = debtToneClass(debt);
            const label = debtDisplayLabel(debt);
            const title = firstLocalizedReadableText([
                debt.crisisLevelLabel,
                `${debtDueText(debt)} · ${riskSeverityText(debt.severity)}`
            ], "风险待观察");
            const summary = firstLocalizedReadableText([
                debt.consequencePreview,
                debt.summary,
                debtCounterplayText(debt)
            ], "这笔旧账还在发酵，先留一个低压回合。");
            const action = firstLocalizedReadableText([
                debt.recommendedAction,
                debtCounterplayText(debt)
            ], "明天先看米线工具或低压行动。");
            return {
                tone,
                label,
                title,
                summary: `${debtSourceLine(debt)}。${summary}`,
                action,
                urgent: Boolean(debt.urgent) || debtDaysLeft(debt) <= 1 || Number(debt.severity || 0) >= 3
            };
        });
    if (items.length) return items;

    const reportRisk = firstLocalizedReadableText([
        state.report?.riskHint,
        state.stageBriefing?.riskSnapshot,
        state.endingForecast?.riskLine
    ], "");
    if (reportRisk) {
        return [{
            tone: /高压|债务|风险|回旋|炎上/.test(reportRisk) ? "watch" : "safe",
            label: "风险快照",
            title: /高压|债务|风险|回旋|炎上/.test(reportRisk) ? "明天先降温" : "暂稳",
            summary: reportRisk,
            action: "继续用日报和结局预演校准下一手。",
            urgent: /高压|到期|必须|回旋|炎上/.test(reportRisk)
        }];
    }
    return [];
}

function renderCrisisCards(compact = false) {
    const items = riskCrisisItems();
    if (!items.length) {
        return `
            <section class="crisis-card-stack ${compact ? "compact" : ""} safe" aria-label="危机卡">
                <div class="crisis-card-head">
                    <span>危机卡</span>
                    <strong>今晚暂稳</strong>
                </div>
                <p>没有明显旧账回流，可以把下一手留给路线证据。</p>
            </section>
        `;
    }
    const urgentCount = items.filter(item => item.urgent).length;
    const headerTitle = urgentCount ? "今天先处理" : "旧账在发酵";
    return `
        <section class="crisis-card-stack ${compact ? "compact" : ""} ${urgentCount ? "hot" : "watch"}" aria-label="危机卡">
            <div class="crisis-card-head">
                <span>危机卡</span>
                <strong>${html(headerTitle)}</strong>
            </div>
            <div class="crisis-card-list">
                ${items.map(item => `
                    <span class="crisis-card ${html(item.tone)} ${item.urgent ? "urgent" : ""}">
                        <em>${html(item.label)}</em>
                        <strong>${html(item.title)}</strong>
                        <small>${html(item.summary)}</small>
                        <b>${html(item.action)}</b>
                    </span>
                `).join("")}
            </div>
        </section>
    `;
}

const fullRunRouteMap = [
    { key: "ELECTRONIC_PICKLE", name: "电子榨菜", start: "杂谈复盘 / 粉丝群维护", ending: "ELECTRONIC_PICKLE" },
    { key: "SINGING_IDOL", name: "歌势偶像", start: "练歌 / 歌回 / 投稿", ending: "SINGING_IDOL" },
    { key: "DANCE_MEME", name: "梗舞整活", start: "练舞 / 短视频 / 动作梗", ending: "SLICE_SAINT" },
    { key: "SLICE_SAINT", name: "切片圣体", start: "发布切片 / 练舞 / 短视频", ending: "SLICE_SAINT" },
    { key: "SOCIAL_COLLAB", name: "社交联动", start: "同台互动 / 联动后留粉", ending: "DD_BUS_STOP" },
    { key: "BLACK_RED_MAIN_STAGE", name: "黑红主会场", start: "硬嘴标题 / 抽象企划", ending: "BLACK_RED_MAIN_STAGE" },
    { key: "CYBER_GIRLFRIEND", name: "赛博女友", start: "回应高亮互动 / 陪伴营业", ending: "CYBER_GIRLFRIEND" },
    { key: "DD_BUS_STOP", name: "DD公交站", start: "联动排班 / DD留存", ending: "DD_BUS_STOP" },
    { key: "MAIN_STAGE_KING", name: "主会场之王", start: "黑红控场 / 切片接热度", ending: "MAIN_STAGE_KING" },
    { key: "GLORIOUS_GRADUATION", name: "光荣毕业", start: "稳口碑 / 清旧账 / 粉丝群", ending: "GLORIOUS_GRADUATION" },
    { key: "UNKNOWN", name: "查无此V", start: "长期不定型 / 摸鱼", ending: "UNKNOWN" }
];

function renderFullRouteMap(currentRouteKey = "UNKNOWN") {
    const likelyEnding = state.endingForecast?.likelyEndingType || currentRouteKey || "UNKNOWN";
    const cards = fullRunRouteMap.map(item => {
        const playbook = routePlaybookFor(item.key);
        const active = item.key === currentRouteKey || item.key === likelyEnding || item.ending === likelyEnding;
        const endingLabel = routeLabelFor(item.ending);
        return `
            <span class="full-route-map-card ${active ? "active" : ""}">
                <em>${html(item.name)}</em>
                <strong>${html(playbook.fantasy)}</strong>
                <small>${html(item.start)} → ${html(endingLabel)}</small>
            </span>
        `;
    }).join("");
    return `
        <details class="full-route-map" aria-label="11条完整路线地图">
            <summary>
                <span>11条完整路线地图</span>
                <strong>展开看这局还能往哪里走</strong>
            </summary>
            <div class="full-route-map-grid">${cards}</div>
        </details>
    `;
}

// ============ 路线专属下播选项 ============
const ROUTE_OFFSTREAM_OPTIONS = {
    SINGING_IDOL: [
        { type: 'VOCAL_PRACTICE', name: '🎤 练声', effectPreview: '歌力+1，保护嗓子', recommended: true },
        { type: 'SONG_SELECTION', name: '🎵 选曲', effectPreview: '灵感+1，为下次歌回准备歌单' },
        { type: 'READ_LETTERS', name: '💌 读粉丝信', effectPreview: '口碑+1，真爱粉感动' },
    ],
    SLICE_SAINT: [
        { type: 'CLIP_SCOUTING', name: '🔍 素材挖掘', effectPreview: '素材+1，找直播高光片段', recommended: true },
        { type: 'THUMBNAIL_DESIGN', name: '🎨 封面设计', effectPreview: '传播+1，提高点击率' },
        { type: 'MEME_RESEARCH', name: '📱 刷热门', effectPreview: '灵感+1，看看现在什么火' },
    ],
    ELECTRONIC_PICKLE: [
        { type: 'READ_LETTERS', name: '💌 读粉丝信', effectPreview: '口碑+1，老粉很暖心', recommended: true },
        { type: 'CHAT_ROOM', name: '☕ 陪聊', effectPreview: '灵感+1，和观众闲聊' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷手机', effectPreview: '灵感+1，放松一下' },
    ],
    BLACK_RED_MAIN_STAGE: [
        { type: 'TREND_WATCH', name: '👁️ 舆情监控', effectPreview: '风险-1，提前发现危机', recommended: true },
        { type: 'CRISIS_PR', name: '📢 危机公关', effectPreview: '热度+1，把黑流量变成关注' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷热搜', effectPreview: '灵感+1，看看有什么能蹭的' },
    ],
    SOCIAL_COLLAB: [
        { type: 'DM_MAINTAIN', name: '💬 私信维护', effectPreview: '影响力+1，维护同行关系', recommended: true },
        { type: 'COLLAB_PLAN', name: '🤝 联动排班', effectPreview: 'DD粉+1，安排下次联动' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷同行动态', effectPreview: '灵感+1，看看同行在干嘛' },
    ],
    DANCE_MEME: [
        { type: 'MEME_RESEARCH', name: '🔍 刷梗', effectPreview: '梗热度+1，找新梗灵感', recommended: true },
        { type: 'SHORT_VIDEO_IDEA', name: '💡 短视频构思', effectPreview: '路人粉+1，想下一个爆款' },
        { type: 'ORGANIZE_MATERIALS', name: '📁 整理素材', effectPreview: '素材+1，把舞蹈片段归档' },
    ],
    CYBER_GIRLFRIEND: [
        { type: 'CHAT_ROOM', name: '☕ 陪聊', effectPreview: '口碑+1，和粉丝深夜谈心', recommended: true },
        { type: 'READ_LETTERS', name: '💌 读粉丝信', effectPreview: '口碑+1，认真回复每一条' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷私信', effectPreview: '灵感+1，看看粉丝在聊什么' },
    ],
    DD_BUS_STOP: [
        { type: 'DM_MAINTAIN', name: '💬 私信维护', effectPreview: '影响力+1，维护DD关系', recommended: true },
        { type: 'COLLAB_PLAN', name: '🤝 联动排班', effectPreview: 'DD粉+1，安排明天蹭谁' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷同行动态', effectPreview: '灵感+1，看看谁在开播' },
    ],
    MAIN_STAGE_KING: [
        { type: 'TREND_WATCH', name: '👁️ 舆情监控', effectPreview: '风险-1，关注主会场风向', recommended: true },
        { type: 'VOCAL_PRACTICE', name: '🎤 练声', effectPreview: '歌力+1，保持状态' },
        { type: 'BROWSE_SOCIAL', name: '📱 刷热搜', effectPreview: '灵感+1，看看大势所趋' },
    ],
    GLORIOUS_GRADUATION: [
        { type: 'READ_LETTERS', name: '💌 读粉丝信', effectPreview: '口碑+1，珍惜最后的时光', recommended: true },
        { type: 'ORGANIZE_MATERIALS', name: '📁 整理素材', effectPreview: '素材+1，把回忆归档' },
        { type: 'CHAT_ROOM', name: '☕ 陪聊', effectPreview: '灵感+1，和老粉聊聊' },
    ],
};

// 通用下播选项（所有路线都能选）
const COMMON_OFFSTREAM_OPTIONS = [
    { type: 'BROWSE_SOCIAL', name: '📱 刷手机', effectPreview: '灵感+1，随便看看' },
    { type: 'ORGANIZE_MATERIALS', name: '📁 整理素材', effectPreview: '素材+1，收拾一下' },
];

function routeVisualFor(routeKey) {
    return routeVisualProfiles[routeKey] || routeVisualProfiles.UNKNOWN;
}

function routeKeyForNextRun(routeKey, fallbackRouteKey = "UNKNOWN") {
    const route = String(routeKey || "").trim();
    if (route && route !== "COMPLETE" && Object.prototype.hasOwnProperty.call(routeVisualProfiles, route)) {
        return route;
    }
    const fallback = String(fallbackRouteKey || "").trim();
    if (fallback && fallback !== "COMPLETE" && Object.prototype.hasOwnProperty.call(routeVisualProfiles, fallback)) {
        return fallback;
    }
    return "UNKNOWN";
}

function nextRunMotivationText(options = {}) {
    const endingCollection = options.endingCollection || state.achievementProgress?.endingCollection || {};
    const forecast = options.forecast || state.endingForecast || null;
    const ending = options.ending || state.ending || null;
    const scorecard = options.scorecard || null;
    const atlasTarget = endingCollection.nextTargetType === "COMPLETE" ? "" : endingCollection.nextTargetType;
    const routeKey = routeKeyForNextRun(
        options.routeKey || atlasTarget || forecast?.likelyEndingType || ending?.restartHint?.routeBias,
        ending?.endingType || ending?.routeReview?.currentRoute || state.vup?.currentRoute || "UNKNOWN"
    );
    const routeLabel = routeLabelFor(routeKey);
    const visualTone = localizedVisibleTextOrFallback(routeVisualFor(routeKey)?.tone, routeLabel);
    const gap = (forecast?.requirements || []).find(item => item?.status && item.status !== "PASS");
    if (gap) {
        const progress = endingGapProgressText(gap);
        const progressLine = progress ? `，${progress}` : "";
        return `下一局试${routeLabel}；先${endingGapActionLabel(gap)}：${endingGapSignalLabel(gap)}${progressLine}。`;
    }
    if (scorecard) {
        const weakness = endingPrimaryWeakness(scorecard);
        return `下一局试${routeLabel}；先补${weakness.label}。`;
    }
    const goal = localizedVisibleTextOrFallback(
        options.goal || endingCollection.nextRunGoal || endingCollection.nextTargetHint,
        ""
    );
    if (goal) {
        return `下一局试${routeLabel}；目标：${compactSharePart(goal, "", 32)}。`;
    }
    return `下一局试${routeLabel}；先补一条${visualTone}证据。`;
}

function routeCoverFor(routeKey) {
    return routeVisualFor(routeKey).cover;
}

function endingImageFor(endingType) {
    return endingImages[endingType] || endingImages.UNKNOWN;
}

function endingImageForEnding(ending = state.ending) {
    const route = ending?.endingType || ending?.routeReview?.currentRoute || "UNKNOWN";
    const scorecard = ending?.routeReview?.scorecard || {};
    const overall = Number(scorecard.overall || 0);
    const openDebtCount = Number(scorecard.openDebtCount ?? (ending?.debtRefs || []).length);
    const severeDebtCount = Number(scorecard.severeDebtCount || 0);
    if (endingCostImages[route] && (severeDebtCount > 0 || openDebtCount >= 2 || (overall > 0 && overall < 75))) {
        return endingCostImages[route];
    }
    return endingImageFor(route);
}

function syncRouteTheme(routeKey, enabled) {
    const visual = routeVisualFor(routeKey);
    const phaseBackdrop = phaseBackdrops[state.session?.phase || ""];
    const backdrop = phaseBackdrop || visual.backdrop;
    document.body.dataset.route = enabled ? visual.slug : "pregame";
    document.body.style.setProperty("--route-accent", visual.accent);
    document.body.style.setProperty("--route-accent-2", visual.accent2);
    document.body.style.setProperty("--route-backdrop-image", `url("/gallery/${backdrop}")`);
    document.body.style.setProperty("--route-cover-image", `url("/gallery/${visual.cover}")`);
}

function applyRouteTheme() {
    const route = state.vup?.currentRoute || 'UNKNOWN';
    const theme = ROUTE_THEMES[route];
    if (!theme) {
        document.body.removeAttribute('data-route-theme');
        return;
    }
    document.body.setAttribute('data-route-theme', route);

    // 更新 CSS 变量
    const root = document.documentElement;
    root.style.setProperty('--route-theme-accent', routeVisualProfiles[route]?.accent || theme.accentColor);
    root.style.setProperty('--route-theme-border', theme.cardBorder);
    root.style.setProperty('--route-theme-bg', theme.bgGradient);
}

function debtTypeText(debtType) {
    const debtNames = {
        TITLE_BACKFIRE: "标题党反噬",
        BOOMERANG_CLIP: "回旋镖切片",
        UNICORN_EXPECTATION: "独角兽期待",
        COMMERCIAL_BACKLASH: "商业反噬",
        HARD_MOUTH: "嘴硬留档",
        FAN_SERVICE: "陪伴营业期待",
        BUSINESS_SAFE: "商业安全债"
    };
    return debtNames[debtType] || visibleTextOrFallback(debtType, "未归档旧账");
}

function debtDisplayLabel(debt, fallback = "未归档旧账") {
    return firstLocalizedReadableText([
        debt?.debtLabel,
        debt?.label,
        debt?.title
    ], debtTypeText(debt?.debtType) || fallback);
}

function debtSourceLine(debt) {
    const summary = localizedVisibleTextOrFallback(debt?.summary, "");
    const match = summary.match(/来源：[^。；]+/);
    if (match) return match[0];
    const createDay = Number(debt?.createDay || 0);
    return createDay > 0 ? `来源：第${createDay}天行动记录` : "来源：行动记录待补";
}

function debtConsequenceLine(debt) {
    return localizedVisibleTextOrFallback(
        debt?.consequencePreview,
        `${debtDisplayLabel(debt)}可能回流成正式事件，围观和口碑同时承压。`
    );
}

function debtRecommendedLine(debt) {
    return localizedVisibleTextOrFallback(
        debt?.recommendedAction,
        debtCounterplayText(debt)
    );
}

function riskSeverityText(severity) {
    const level = Number(severity) || 0;
    if (level >= 3) return `米线高压 ${level}级`;
    if (level === 2) return "主会场升温 2级";
    if (level === 1) return "小作文预警 1级";
    return "风险待观察";
}

function statusLabelFor(status) {
    const statusNames = {
        PASS: "已达成",
        WARN: "有风险",
        FAIL: "未达成",
        READY: "可继续",
        LOCKED: "未解锁",
        OPEN: "待处理",
        CLEARED: "已清账",
        ACTIVE: "进行中",
        FORMING: "正在成形",
        DORMANT: "休眠中",
        COMPLETED: "已完成",
        ABANDONED: "已放弃",
        EMPTY: "空档",
        RESERVED: "已预留",
        USED: "已使用",
        SUCCESS: "成功",
        FAILED: "失败"
    };
    return statusNames[status] || localizedVisibleTextOrFallback(status, "观望中");
}

function phaseLabelFor(phase) {
    return getPhaseText(phase);
}

function knownErrorCodeText(code) {
    const raw = String(code || "").trim();
    return errorCodeCopyText(raw);
}

function playerSafeOperationErrorText() {
    return "操作没成，场务已收起后台代号，先刷新当前进度再试。";
}

function errorCodeText(code, statusCode) {
    const codeMessage = knownErrorCodeText(code);
    if (codeMessage) return codeMessage;
    return playerSafeOperationErrorText();
}

function apiErrorMessage(payload, statusCode) {
    const codeMessage = knownErrorCodeText(payload?.code);
    const message = localizedMessageText(payload?.message);
    return codeMessage || message || errorCodeText(payload?.code, statusCode);
}

function demoErrorCodeText(lastError) {
    if (!lastError) return "--";
    return errorCodeText(lastError.code);
}

function demoErrorMessageText(lastError) {
    if (!lastError) return "--";
    return apiErrorMessage(lastError);
}

function demoResultLabel(result) {
    const endingType = String(result?.endingType || "").trim();
    if (endingType) {
        return routeLabelNames[endingType] || "已生成结局";
    }
    return phaseLabelFor(result?.phase);
}

function demoBriefText(status, result) {
    const phaseText = phaseLabelFor(status?.phase || result?.phase);
    const logCount = status?.businessLogCount ?? status?.logCount ?? result?.logCount;
    const endingText = demoResultLabel(result);
    const errorText = status?.lastError ? demoErrorMessageText(status.lastError) : "";

    if (result?.endingReviewId || result?.endingType) {
        return `验证已推进到${phaseText}，结局${endingText}已归档，复盘组可以直接验收路线。`;
    }
    if (errorText && errorText !== "--") {
        return `验证停在${phaseText}，${errorText}`;
    }
    if (status) {
        const logCopy = Number.isFinite(Number(logCount)) ? `，日志 ${logCount} 条` : "";
        return `验证推进到${phaseText}${logCopy}，场务可以继续快进或重置。`;
    }
    return `验证待命，选择路线后可以一键跑满${runMaxDayLabel()}。`;
}

function defenseEvidenceItems() {
    const items = state.configCheck?.defenseEvidence;
    if (Array.isArray(items) && items.length > 0) {
        return items.map(item => ({
            ...item,
            proof: readableEvidenceProof(item.proof)
        }));
    }
    return [
        {
            key: "mvc_layers",
            label: "MVC分层",
            detail: "Controller -> Service -> Mapper -> Model/DTO 分层完整。",
            proof: "业务流程通过 REST 接口进入 Service，再由 Mapper 落库。"
        },
        {
            key: "daily_loop",
            label: `${runMaxDayLabel()}闭环`,
            detail: "行动、日报、结局按状态机推进。",
            proof: "日会话、业务日志、每日报告、结局复盘都会保存证据。"
        }
    ];
}

function readableEvidenceProof(proof) {
    return String(proof || "")
        .replaceAll("day_session", "日会话")
        .replaceAll("business_log", "业务日志")
        .replaceAll("daily_report", "每日报告")
        .replaceAll("ending_review", "结局复盘")
        .replaceAll("api_idempotency_record", "幂等记录");
}

function renderDefenseEvidenceStrip(items = defenseEvidenceItems()) {
    if (!items.length) return "";
    const chips = items.slice(0, 4)
        .map(item => `<span title="${html(item.proof || item.detail)}">${html(item.label || item.key)}</span>`)
        .join('');
    return `<div class="defense-evidence-strip" aria-label="SSM答辩证据速览">${chips}</div>`;
}

function renderDefenseEvidencePanel() {
    const items = defenseEvidenceItems();
    if (!items.length) return "";
    const cards = items.map(item => `
        <article class="defense-evidence-card">
            <span>${html(item.label || item.key)}</span>
            <strong>${html(item.detail || "证据待补齐")}</strong>
            <p>${html(item.proof || "现场可从接口、Service 和数据库表继续追问。")}</p>
        </article>
    `).join('');
    return `
        <section class="defense-evidence-panel" aria-label="SSM证据台">
            <div class="defense-evidence-head">
                <strong>SSM证据台</strong>
                <small>答辩追问路线</small>
            </div>
            <div class="defense-evidence-grid">${cards}</div>
        </section>
    `;
}

function actionLabelFor(actionKey) {
    const actionNames = {
        TRAIN_SONG: "练歌",
        TRAIN_DANCE: "练舞",
        TRAIN_TALK: "杂谈复盘",
        STREAM_PLAN: "直播企划",
        PUBLISH_VIDEO: "发布视频",
        PUBLISH_CLIP: "发布切片",
        FAN_GROUP_MAINTAIN: "粉丝群维护",
        FAN_TOPIC: "粉丝群议题",
        NPC_INTERACT: "同台互动",
        REST: "休息",
        EVENT_CHOICE: "事件选择",
        INTERACTION_CHOICE: "直播现场选择",
        RISK_TOOL: "米线工具"
    };
    const raw = String(actionKey || "").trim();
    if (raw.startsWith("RISK_TOOL")) {
        return "米线工具";
    }
    return actionNames[raw] || visibleTextOrFallback(raw, "未归档行动");
}

function materialLabelFor(material) {
    const materialNames = {
        BOOMERANG_CLIP_CONTEXT: "回旋镖切片上下文",
        UNICORN_EXPECTATION_SCREENSHOT: "独角兽期待截图",
        COMMERCIAL_BACKLASH_RECEIPT: "商业反噬留档",
        TITLE_BACKFIRE_CONTEXT: "标题党反噬上下文"
    };
    const label = String(material?.label || "").trim();
    if (label && !/^[A-Z0-9_]+$/.test(label)) {
        return label;
    }
    const materialId = String(material?.materialId || "").trim();
    return materialNames[materialId] || visibleTextOrFallback(materialId, "未署名事故素材");
}

function memeLabelFor(item) {
    const memeNames = {
        CLIP_SPREAD: "切片传播梗",
        CLIP_SUBMISSION_STOCK: "投稿箱素材"
    };
    const label = String(item?.label || "").trim();
    if (label && !/^[A-Z0-9_]+$/.test(label)) {
        return label;
    }
    const memeSubtype = String(item?.memeSubtype || "").trim();
    return memeNames[memeSubtype] || visibleTextOrFallback(memeSubtype, "未署名梗素材");
}

// 直播企划预览图
const streamPlanArt = {
    SINGING: "v4/stream-plans/previews/singing.png",
    DANCE: "v4/stream-plans/previews/dance.png",
    TALK: "v4/stream-plans/previews/cozy-chat.png",
    GAME: "v4/stream-plans/previews/game.png",
    SHARP_COMMENT: "v4/stream-plans/previews/sharp-commentary.png",
    ENDURANCE: "v4/stream-plans/previews/endurance.png",
    COLLAB: "v4/stream-plans/previews/collaboration.png",
    SC_THANKS: "v4/stream-plans/previews/sc-thanks.png"
};

function streamPlanLabelFor(planType) {
    const planNames = {
        SINGING: "歌回",
        DANCE: "舞回",
        TALK: "杂谈",
        GAME: "游戏回",
        SHARP_COMMENT: "锐评回",
        ENDURANCE: "耐久回",
        COLLAB: "联动回",
        SC_THANKS: "高亮互动回应回"
    };
    return planNames[planType] || visibleTextOrFallback(planType, "直播企划");
}

function streamPlanLabelText(plan) {
    return visibleTextOrFallback(plan?.label, streamPlanLabelFor(plan?.planType));
}

// 渲染直播企划预览
function renderStreamPlanPreview(planType) {
    const image = streamPlanArt[planType] || streamPlanArt.TALK;
    return `
        <div class="stream-plan-preview">
            <img class="stream-plan-art" src="/gallery/${image}" alt="${html(streamPlanLabelFor(planType))}企划预览" loading="lazy">
        </div>
    `;
}

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    initStreamerModeToggle();
    initTutorial();
    initTabs();
    hydrateConfigCheck();
    hydrate();
    document.addEventListener('keydown', handleActionHotkey);
    document.addEventListener('keydown', handleStageChoiceHotkey);
    document.addEventListener('keydown', handleStageAdvanceHotkey);
});


// 配置检查
async function hydrateConfigCheck() {
    try {
        state.configCheck = await api('/api/system/config-check');
        syncTabAvailability();
        if (renderConfigGate()) {
            return;
        }
        // 刷新验证状态
        await refreshDemoStatus();
    } catch (e) {}
}

// 刷新验证状态
async function refreshDemoStatus() {
    if (!state.configCheck?.devTools?.enabled) {
        state.demoStatus = null;
        state.demoResult = null;
        return;
    }
    try {
        state.demoStatus = await api('/api/dev/demo/status');
    } catch (e) {
        state.demoStatus = null;
    }
}

// 配置检查门控
function renderConfigGate() {
    if (state.configCheck?.canPlayP0 === false) {
        document.body.classList.add("config-gate-active");
        const fatalErrors = state.configCheck.fatalErrors || [];
        const errorList = fatalErrors.map(e => `<li>${html(configErrorText(e))}</li>`).join('');

        document.getElementById('authSection').innerHTML = `
            <div class="panel config-fatal-errors">
                <div class="panel-header">
                    <h3 class="panel-title">配置检查失败</h3>
                </div>
                <p>开播配置未达标，场务先别开麦；补齐配置后再开播。</p>
                <ul>${errorList || '<li>配置检查未返回具体错误</li>'}</ul>
            </div>
        `;
        return true;
    }
    document.body.classList.remove("config-gate-active");
    return false;
}

function configErrorText(error) {
    const text = String(error || "").trim();
    if (!text) return "配置检查未返回具体错误。";
    const labels = {
        p0_seed_manifest: "核心种子清单",
        actions: "主行动数据",
        stream_plans: "直播企划数据",
        stream_titles: "直播标题种子",
        title_styles: "标题风格数据",
        risk_debts: "旧账风险数据",
        risk_tools: "米线工具数据",
        ending_templates: "结局模板数据"
    };
    const matched = Object.entries(labels)
        .filter(([key]) => text.includes(key))
        .map(([, label]) => label);
    const uniqueLabels = [...new Set(matched)];
    if (uniqueLabels.length > 0) {
        if (text.includes("核心种子")) {
            return `核心种子数据缺失：${uniqueLabels.join("、")}。`;
        }
        return `配置项未就绪：${uniqueLabels.join("、")}。`;
    }
    if (/[A-Za-z0-9_-]+\.png\b|[\\/]|candidates|gallery/i.test(text)) {
        return "关键图片资源缺失：请补齐图库资源后再开播。";
    }
    return visibleTextOrFallback(text, "配置项未就绪：请检查本地资源和种子数据。");
}

// 验证策略名称
function demoStrategyName(strategy) {
    return {
        steady: "稳健电子榨菜",
        clip: "切片圣体",
        black_red: "黑红主会场",
        social: "社交/DD公交站",
        singing: "歌势偶像",
        cyber_girlfriend: "赛博女友",
        main_stage_king: "主会场之王",
        glorious_graduation: "光荣毕业",
        idle: "摆烂/查无此V",
        defense: "防守降温",
        random: "随机验收轮"
    }[strategy] || visibleTextOrFallback(strategy, "自定义验证");
}

function profileLabelFor(profile) {
    const text = String(profile || "").trim();
    if (text.includes(",")) {
        return text.split(",")
            .map(part => profileLabelFor(part))
            .filter(Boolean)
            .join("、") || "本地环境";
    }
    return {
        dev: "开发环境",
        test: "测试环境",
        manual: "手动验收",
        prod: "正式环境"
    }[text] || visibleTextOrFallback(text, "本地环境");
}

function demoToolsEnabled() {
    return Boolean(state.configCheck?.devTools?.enabled);
}

function demoStrategyOptions(current) {
    const strategies = state.configCheck?.devTools?.availableStrategies || ["steady"];
    return strategies
        .map(s => `<option value="${html(s)}" ${s === current ? 'selected' : ''}>${html(demoStrategyName(s))}</option>`)
        .join('');
}

function demoStrategyValue() {
    return document.getElementById('defenseStrategy')?.value
        || document.getElementById('demoStrategy')?.value
        || 'steady';
}

function syncDemoStrategySelects(value) {
    document.querySelectorAll('#defenseStrategy, #demoStrategy').forEach(select => {
        if (select.value !== value) {
            select.value = value;
        }
    });
}

function liveDrawerAlias(target) {
    const key = String(target || 'host');
    const aliases = {
        mainPanel: 'host',
        platform: 'npcs',
        infoHub: 'host',
        stats: 'host',
        host: 'host',
        streamer: 'host',
        anchor: 'host',
        fans: 'fans',
        audience: 'fans',
        fan: 'fans',
        routes: 'routes',
        route: 'routes',
        combo: 'routes',
        forecast: 'routes',
        report: 'report',
        reports: 'report',
        yesterday: 'report',
        npc: 'npcs',
        npcs: 'npcs',
        platformNpcs: 'npcs',
        atlas: 'atlas',
        achievement: 'atlas',
        achievements: 'atlas',
        unlocks: 'atlas',
        environment: 'environment',
        env: 'environment',
        buzz: 'environment',
        ambient: 'environment',
        leaderboard: 'environment',
        demo: 'environment'
    };
    return aliases[key] || 'host';
}

function normalizeInfoTab(target) {
    const normalized = liveDrawerAlias(target);
    return normalized === 'environment' && target === 'demo' && !demoToolsEnabled() ? 'host' : normalized;
}

const LIVE_DRAWERS = {
    host: { title: '主播', kicker: 'PRIMARY', panels: ['statsSection', 'statChartsSection', 'contextSidebar', 'coachPanel', 'insightDigest', 'stageBriefingPanel'] },
    fans: { title: '粉丝', kicker: 'PRIMARY', panels: ['funAudiencePanel', 'audienceExpectationPanel', 'personaTagPanel', 'fanTopicPanel', 'danmakuPanel'] },
    routes: { title: '路线', kicker: 'PRIMARY', panels: ['timelinePanel', 'routeGalleryPanel', 'endingForecastPanel', 'comboPanel'] },
    report: { title: '昨日结果', kicker: 'PRIMARY', panels: ['reportPanel', 'reportHistoryPanel'] },
    npcs: { title: '其他主播', kicker: 'PRIMARY', panels: ['platformStealBuff', 'npcPanel', 'platformNpcGrid', 'platformInteractPanel'] },
    atlas: { title: '成就', kicker: 'SECONDARY', panels: ['achievementMini', 'unlockAtlasPanel', 'endingAtlasPanel'] },
    environment: { title: '平台/环境', kicker: 'SECONDARY', panels: ['buzzPanel', 'memeLifecyclePanel', 'ambientPanel', 'leaderboardPanel', 'demoPanel'] }
};

function ensureElement(id, tagName = 'div', className = '') {
    let element = document.getElementById(id);
    if (!element) {
        element = document.createElement(tagName);
        element.id = id;
        if (className) element.className = className;
    } else if (className) {
        className.split(/\s+/).filter(Boolean).forEach(name => element.classList.add(name));
    }
    return element;
}

function liveDrawerButton(key, label, kind, icon) {
    return `
        <button class="live-drawer-tab ${kind}" type="button" data-action="open-live-drawer" data-drawer="${key}" title="${label}">
            <span class="live-drawer-tab-icon">${icon}</span>
            <span>${label}</span>
        </button>
    `;
}

function dominantFanGroup() {
    const groups = [
        { label: "真爱粉", value: currentFanValue("trueFans") },
        { label: "乐子人", value: currentFanValue("funFans") },
        { label: "独角兽", value: currentFanValue("unicornFans") },
        { label: "DD", value: currentFanValue("ddFans") }
    ].sort((left, right) => right.value - left.value);
    return groups[0]?.value > 0 ? groups[0] : { label: "还在聚拢", value: 0 };
}

function pressureToneForState(stateText) {
    if (stateText === "失控边缘") return "risk";
    if (stateText === "过载") return "warn";
    if (stateText === "绷紧") return "watch";
    return "neutral";
}

function currentOperationalPressureSnapshot() {
    const actions = Array.isArray(state.actions) ? state.actions : [];
    const pressureAction = actions.find(item => {
        const pressureState = String(item?.pressureState || "").trim();
        return pressureState && pressureState !== "松弛";
    });
    if (!pressureAction) {
        return null;
    }
    const stateText = visibleTextOrFallback(pressureAction.pressureState, "");
    if (!stateText || stateText === "松弛") {
        return null;
    }
    return {
        state: stateText,
        tone: pressureToneForState(stateText),
        lockGroup: visibleTextOrFallback(pressureAction.pressureLockGroupLabel, "当前动作线"),
        replacementLabel: visibleTextOrFallback(pressureAction.pressureReplacementActionLabel, ""),
        hint: localizedVisibleTextOrFallback(pressureAction.pressureHint, ""),
        cooldownLeft: Number(pressureAction.pressureCooldownLeft || 0),
        woundLeft: Number(pressureAction.pressureWoundLeft || 0),
        wounded: Boolean(pressureAction.pressureWounded || Number(pressureAction.pressureWoundLeft || 0) > 0)
    };
}

function operationalBrainSnapshot() {
    const pressure = currentOperationalPressureSnapshot();
    const heat = currentWatchHeat();
    const reputation = currentOpinionValue("reputation");
    const stamina = currentResourceValue("stamina");
    const debts = activeDebts();
    const routeGap = typeof firstWarnEndingRequirement === "function" ? firstWarnEndingRequirement() : null;
    const cooldownLeft = pressure?.cooldownLeft ?? 0;
    const woundLeft = pressure?.woundLeft ?? 0;
    const wounded = Boolean(pressure?.wounded || woundLeft > 0);

    if (pressure) {
        return {
            state: pressure.state,
            tone: pressure.tone,
            title: `${pressure.state} · ${pressure.lockGroup}`,
            value: cooldownLeft > 0 ? `锁${cooldownLeft}天` : (wounded ? `伤${woundLeft || "数"}天` : "在抬头"),
            detail: firstLocalizedReadableText([
                pressure.hint,
                pressure.replacementLabel ? `今天优先用${pressure.replacementLabel}绕开${pressure.lockGroup}，别硬撞。` : ""
            ], "运营脑比粉丝数更早决定你今天做不了什么。"),
            trajectory: cooldownLeft > 0
                ? `重痕迹还会卡住${pressure.lockGroup}${cooldownLeft}天。`
                : wounded
                    ? `带伤恢复还剩${woundLeft || "数"}天，收益和可选项会更紧。`
                    : "压力正在抬头，继续硬冲会留下重痕迹。",
            cooldownLeft,
            woundLeft,
            wounded,
            lockGroup: pressure.lockGroup,
            replacementLabel: pressure.replacementLabel
        };
    }

    const pressureScore = Math.max(
        0,
        Math.min(100, Math.round(heat * 0.38 + Math.max(0, 60 - reputation) * 0.42 + debts.length * 14 + (routeGap ? 10 : 0) + Math.max(0, 3 - stamina) * 8))
    );
    const tone = pressureScore >= 62 ? "watch" : pressureScore >= 38 ? "neutral" : "good";
    const stateText = pressureScore >= 62 ? "慢性上升" : pressureScore >= 38 ? "半可见" : "可控";
    return {
        state: stateText,
        tone,
        title: `运营脑 · ${stateText}`,
        value: `${pressureScore}`,
        detail: pressureScore >= 62
            ? "运营脑比粉丝数更早决定你今天做不了什么。先降噪、还债或补身心。"
            : pressureScore >= 38
                ? "压力半可见：现在还能操作，但连续追热度会把明天变窄。"
                : "运营脑暂时可控，今天可以按路线推进。",
        trajectory: routeGap
            ? `可能走向：先补${endingGapSignalLabel(routeGap)}，否则路线会被旧账和热度挤偏。`
            : "可能走向：维持节奏，别让公开热度盖过身体和关系线。",
        cooldownLeft: 0,
        woundLeft: 0,
        wounded: false,
        lockGroup: "",
        replacementLabel: ""
    };
}

function liveDrawerSummaryChip(label, value, tone = "") {
    return `<span class="live-drawer-summary-chip ${html(tone)}"><em>${html(label)}</em><strong>${html(String(value))}</strong></span>`;
}

function liveDrawerSummaryData(drawerKey) {
    const day = Number(state.session?.day || state.vup?.day || 1);
    const phase = state.session?.phase || (state.vup ? "READY" : "");
    const fans = currentFanValue("fans");
    const stamina = currentResourceValue("stamina");
    const maxStamina = currentResourceValue("maxStamina", 10);
    const reputation = currentOpinionValue("reputation");
    const heat = currentWatchHeat();
    const route = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const routeName = routeLabelFor(route);
    const reportProfile = liveRoomMoodProfile();
    const fanGroup = dominantFanGroup();
    const pressure = currentOperationalPressureSnapshot();
    const firstRequirement = typeof firstWarnEndingRequirement === "function" ? firstWarnEndingRequirement() : null;
    const requirementLabel = firstRequirement && typeof endingGapSignalLabel === "function"
        ? endingGapSignalLabel(firstRequirement)
        : "路线证据";
    const objective = Array.isArray(state.stageBriefing?.objectiveItems)
        ? state.stageBriefing.objectiveItems.find(item => !item?.achieved) || state.stageBriefing.objectiveItems[0]
        : null;
    const npcLine = firstLocalizedReadableText([
        state.chainEvent?.message,
        state.npcSpotlight?.summary,
        state.npcSpotlight?.description,
        state.platformData?.summary
    ], "今天先观察其他主播的动向，再决定是否联动或偷学。");
    const buzzLine = firstLocalizedReadableText([
        state.buzzBriefing?.summary,
        state.buzzBriefing?.description,
        state.memeLifecycle?.summary,
        state.memeLifecycle?.description
    ], "平台风向暂稳，适合按当前路线补证据。");
    const achievement = state.achievementProgress || {};

    const base = {
        host: {
            tone: pressure ? pressure.tone : (heat >= 70 || reputation < 45 ? "watch" : "good"),
            title: pressure
                ? `${pressure.state} · 当前压力在抬头`
                : phase === "READY" ? "今天先选一手行动" : "当前流程正在推进",
            line: pressure
                ? firstLocalizedReadableText([
                    pressure.hint,
                    pressure.replacementLabel ? `当前被${pressure.lockGroup}卡住，先用${pressure.replacementLabel}稳住。` : `当前被${pressure.lockGroup}卡住，先别硬冲。`
                ], "先把压力线压下去，再继续主线。")
                : (objective
                    ? firstLocalizedReadableText([objective.hint, objective.label], "先补阶段委托，再看路线缺口。")
                    : `第${day}天 · ${routeName}路线，先看体力和口碑能不能支撑下一手。`),
            chips: [
                ["体力", `${stamina}/${maxStamina}`, stamina <= 2 ? "risk" : "good"],
                ["口碑", reputation, reputation < 45 ? "risk" : "good"],
                ["围观", heat, heat >= 70 ? "hot" : "neutral"]
            ]
        },
        fans: {
            tone: heat >= 70 ? "hot" : "good",
            title: `${compactCount(fans)} 个粉丝正在形成结构`,
            line: `当前最显眼的是${fanGroup.label}；看粉丝抽屉时优先判断他们想要什么、会为什么回踩。`,
            chips: [
                ["总粉丝", compactCount(fans), "good"],
                [fanGroup.label, compactCount(fanGroup.value), "neutral"],
                ["围观", heat, heat >= 70 ? "hot" : "neutral"]
            ]
        },
        routes: {
            tone: firstRequirement ? "watch" : "good",
            title: `${routeName} 是当前最强路线信号`,
            line: firstRequirement
                ? `下一步补：${requirementLabel}。抽屉里看路线图、结局预测和组合技。`
                : "当前路线门槛暂稳，可以继续堆同一路线证据。",
            chips: [
                ["路线", routeName, "route"],
                ["缺口", firstRequirement ? requirementLabel : "暂稳", firstRequirement ? "watch" : "good"],
                ["第几天", `${day}/${runMaxDay()}`, "neutral"]
            ]
        },
        report: {
            tone: reportProfile.riskText ? "watch" : "good",
            title: reportProfile.headline,
            line: reportProfile.summary,
            chips: reportProfile.chips.map(chip => [chip.label, chip.value, chip.tone])
        },
        npcs: {
            tone: state.chainEvent ? "watch" : "neutral",
            title: state.chainEvent ? "有其他主播递来了事件" : "其他主播正在改写平台势能",
            line: npcLine,
            chips: [
                ["互动", state.chainEvent ? "待处理" : "观察", state.chainEvent ? "watch" : "neutral"],
                ["围观", heat, heat >= 70 ? "hot" : "neutral"],
                ["路线", routeName, "route"]
            ]
        },
        atlas: {
            tone: Number(achievement.unlockedCount || 0) > 0 ? "good" : "neutral",
            title: `成就 ${achievement.unlockedCount ?? 0}/${achievement.totalCount ?? "--"}`,
            line: firstLocalizedReadableText([achievement.nextTitle, achievement.featuredTitle, achievement.title], "完成一轮后，结局图鉴会反过来指导下一局路线。"),
            chips: [
                ["已解锁", achievement.unlockedCount ?? 0, "good"],
                ["总数", achievement.totalCount ?? "--", "neutral"],
                ["周目", achievement.endingCollection?.completedRuns ?? 0, "neutral"]
            ]
        },
        environment: {
            tone: heat >= 70 ? "hot" : "neutral",
            title: heat >= 70 ? "平台正在把你推到更吵的位置" : "平台风向还可以利用",
            line: buzzLine,
            chips: [
                ["围观", heat, heat >= 70 ? "hot" : "neutral"],
                ["梗浓度", currentOpinionValue("memeLevel"), "neutral"],
                ["人气", currentOpinionValue("popularity"), "good"]
            ]
        }
    };
    return base[drawerKey] || base.host;
}

function renderLiveDrawerSummary(drawerKey) {
    if (!state.vup) {
        return `
            <div class="live-drawer-summary neutral">
                <div class="live-drawer-summary-copy">
                    <span>未开播</span>
                    <strong>创建或继续存档后，这里会变成当前抽屉的三行摘要。</strong>
                </div>
            </div>
        `;
    }
    const data = liveDrawerSummaryData(drawerKey);
    const pressure = drawerKey === "host" ? currentOperationalPressureSnapshot() : null;
    const summaryTone = pressure ? pressure.tone : (data.tone || "neutral");
    const chips = (data.chips || []).map(([label, value, tone]) => liveDrawerSummaryChip(label, value, tone)).join("");
    const pressureHtml = pressure ? `
        <div class="live-drawer-pressure ${html(pressure.tone || "neutral")}">
            <span class="live-drawer-pressure-chip state"><em>压力</em><strong>${html(pressure.state)}</strong></span>
            <span class="live-drawer-pressure-chip lock"><em>锁组</em><strong>${html(pressure.lockGroup)}</strong></span>
            <span class="live-drawer-pressure-chip replacement"><em>替代</em><strong>${html(pressure.replacementLabel || "待定")}</strong></span>
        </div>
    ` : "";
    return `
        <div class="live-drawer-summary ${html(summaryTone)}">
            <div class="live-drawer-summary-copy">
                <span>${html(LIVE_DRAWERS[drawerKey]?.title || "摘要")}</span>
                <strong>${html(data.title)}</strong>
                <small>${html(data.line)}</small>
            </div>
            ${pressureHtml}
            <div class="live-drawer-summary-chips">${chips}</div>
        </div>
    `;
}

function syncLiveDrawerSummaries() {
    Object.keys(LIVE_DRAWERS).forEach(key => {
        const summary = document.getElementById(`liveDrawerSummary-${key}`);
        if (summary) summary.innerHTML = renderLiveDrawerSummary(key);
    });
}

function buildLiveDrawerShell() {
    const main = document.getElementById('mainContent');
    const oldMainPanel = document.getElementById('tabMainPanel');
    const oldPlatformPanel = document.getElementById('tabPlatform');
    const oldInfoHubPanel = document.getElementById('tabInfoHub');
    if (!main || !oldMainPanel || document.getElementById('liveStageShell')) return;

    const stageShell = document.createElement('section');
    stageShell.className = 'live-stage-shell tab-panel active';
    stageShell.id = 'liveStageShell';
    stageShell.setAttribute('aria-label', 'Live room stage');

    const stageFrame = document.createElement('div');
    stageFrame.className = 'live-stage-frame';
    stageShell.appendChild(stageFrame);

    const viewport = ensureElement('vupLiveViewport', 'div', 'vup-avatar-section vup-live-window');
    stageFrame.appendChild(viewport);

    const ticker = document.createElement('div');
    ticker.className = 'live-stage-ticker';
    ticker.id = 'liveStageTicker';
    ticker.setAttribute('aria-label', 'Yesterday feedback and live room state');
    stageFrame.appendChild(ticker);

    const stageHud = document.createElement('div');
    stageHud.className = 'live-stage-hud';
    stageFrame.appendChild(stageHud);

    const authSection = ensureElement('authSection', 'div', 'auth-section');
    stageHud.appendChild(authSection);

    const stageControl = document.createElement('div');
    stageControl.className = 'game-panels live-stage-control';
    stageHud.appendChild(stageControl);
    ['actionPanel', 'offStreamPanel', 'endingPanel'].forEach(id => {
        const className = id === 'actionPanel' ? 'panel action-panel' : id === 'offStreamPanel' ? 'panel offstream-panel hidden' : 'panel ending-panel hidden';
        stageControl.appendChild(ensureElement(id, 'div', className));
    });

    const drawer = document.createElement('aside');
    drawer.className = 'live-drawer open';
    drawer.id = 'liveDrawer';
    drawer.setAttribute('aria-label', 'Live room information drawer');
    drawer.innerHTML = `
        <div class="live-drawer-head">
            <div>
                <span class="live-drawer-kicker" id="liveDrawerKicker">PRIMARY</span>
                <h2 id="liveDrawerTitle">主播</h2>
            </div>
            <button class="live-drawer-close" type="button" aria-label="Close drawer" data-action="close-live-drawer">×</button>
        </div>
        <div class="live-drawer-body" id="liveDrawerBody"></div>
    `;
    stageFrame.appendChild(drawer);

    const drawerBody = drawer.querySelector('#liveDrawerBody');
    Object.keys(LIVE_DRAWERS).forEach(key => {
        const section = document.createElement('section');
        section.className = 'live-drawer-panel';
        section.dataset.liveDrawerPanel = key;
        const legacyId = key === 'npcs' ? 'tabPlatform' : key === 'environment' ? 'tabInfoHub' : '';
        if (legacyId) section.id = legacyId;
        const summary = document.createElement('div');
        summary.className = 'live-drawer-summary-slot';
        summary.id = `liveDrawerSummary-${key}`;
        section.appendChild(summary);
        LIVE_DRAWERS[key].panels.forEach(id => {
            const className = id === 'reportPanel' ? 'panel report-panel hidden'
                : id === 'fanTopicPanel' ? 'panel fan-topic-panel'
                : id === 'danmakuPanel' ? 'panel danmaku-panel'
                : id === 'platformStealBuff' ? 'platform-steal-buff'
                : id === 'platformNpcGrid' ? 'platform-npc-grid'
                : id === 'platformInteractPanel' ? 'platform-interact-panel hidden'
                : id === 'achievementMini' ? 'achievement-mini'
                : id === 'coachPanel' ? 'coach-card'
                : id === 'insightDigest' ? 'insight-digest'
                : id === 'statsSection' ? 'vup-info-section'
                : id === 'demoPanel' ? 'panel hidden'
                : 'info-panel';
            section.appendChild(ensureElement(id, 'div', className));
        });
        drawerBody.appendChild(section);
    });

    const dock = document.createElement('nav');
    dock.className = 'live-drawer-dock';
    dock.id = 'liveDrawerDock';
    dock.setAttribute('aria-label', 'Live room information drawers');
    dock.innerHTML = `
        <div class="live-drawer-group primary" aria-label="Primary drawers">
            ${liveDrawerButton('host', '主播', 'primary', '主')}
            ${liveDrawerButton('fans', '粉丝', 'primary', '粉')}
            ${liveDrawerButton('routes', '路线', 'primary', '线')}
            ${liveDrawerButton('report', '昨日结果', 'primary', '昨')}
            ${liveDrawerButton('npcs', '其他主播', 'primary', '竞')}
        </div>
        <div class="live-drawer-group secondary" aria-label="Secondary drawers">
            ${liveDrawerButton('atlas', '成就', 'secondary', '奖')}
            ${liveDrawerButton('environment', '平台/环境', 'secondary', '境')}
        </div>
    `;
    stageFrame.appendChild(dock);

    oldMainPanel.replaceWith(stageShell);
    oldPlatformPanel?.remove();
    oldInfoHubPanel?.remove();
    document.getElementById('secondaryPanels')?.remove();
    document.querySelector('.live-legacy-tabs')?.classList.add('hidden');
    document.body.classList.add('live-drawer-layout');
    bindLiveDrawerDockFallback();
}

let liveDrawerDockFallbackBound = false;

function bindLiveDrawerDockFallback() {
    if (liveDrawerDockFallbackBound) return;
    liveDrawerDockFallbackBound = true;
    document.addEventListener('click', event => {
        const tab = event.target?.closest?.('[data-action="open-live-drawer"]');
        if (tab && document.contains(tab)) {
            event.preventDefault();
            openLiveDrawer(tab.dataset.drawer || tab.dataset.liveDrawer || tab.dataset.tab || 'host');
            return;
        }
        const close = event.target?.closest?.('[data-action="close-live-drawer"]');
        if (close && document.contains(close)) {
            event.preventDefault();
            closeLiveDrawer();
        }
    });
}

function syncDemoTabAvailability() {
    const tab = document.querySelector('.info-tab[data-tab="demo"]');
    if (!tab) return;

    const enabled = demoToolsEnabled();
    tab.hidden = !enabled;
    tab.disabled = !enabled;
    tab.setAttribute('aria-hidden', String(!enabled));
    tab.tabIndex = enabled ? 0 : -1;
    if (!enabled && state.infoTab === 'demo') {
        state.infoTab = 'npc';
    }
}

// 同步Tab可用性：成就面板前5天隐藏
function syncTabAvailability() {
    syncDemoTabAvailability();
    const unlockPanel = document.getElementById('unlockAtlasPanel');
    if (unlockPanel) {
        const day = Number(state.session?.day || state.vup?.day || 0);
        const unlocked = day >= 5;
        const section = unlockPanel.closest('.info-hub-section');
        if (section) section.classList.toggle('hidden', !unlocked);
        const atlasButton = document.querySelector('.live-drawer-tab[data-drawer="atlas"]');
        if (atlasButton) {
            atlasButton.classList.toggle('locked', !unlocked);
            atlasButton.title = unlocked ? '成就' : '成就：第5天后展开';
        }
    }
}

// 渲染验证面板
function renderDemo() {
    const panel = document.getElementById('demoPanel');
    if (!panel) return;

    syncTabAvailability();
    const devTools = state.configCheck?.devTools;
    const showDemo = devTools?.enabled && (state.infoTab === 'demo' || state.infoTab === 'environment');
    panel.classList.toggle("hidden", !showDemo);

    if (!devTools?.enabled) {
        panel.innerHTML = '';
        return;
    }

    const strategies = devTools.availableStrategies || ["steady"];
    const selectedStrategy = demoStrategyValue();
    const current = strategies.includes(selectedStrategy) ? selectedStrategy : strategies[0];
    const demoBrief = demoBriefText(state.demoStatus, state.demoResult);
    const defenseEvidencePanel = renderDefenseEvidencePanel();
    const targetDay = demoTargetDay();
    const targetDayLabel = `${targetDay}天`;
    const demoRunLabel = targetDay === 30 ? "跑满30天" : `跑满${targetDayLabel}`;
    const demoFastLabel = targetDay === 30 ? "补到30天" : `补到${targetDayLabel}`;
    const demoRunAriaAttr = targetDay === 30
        ? 'aria-label="开发验证：跑满30天并生成验收复盘"'
        : `aria-label="开发验证：${demoRunLabel}并生成验收复盘"`;
    const demoFastAriaAttr = targetDay === 30
        ? 'aria-label="开发验证：补到30天"'
        : `aria-label="开发验证：${demoFastLabel}"`;

    // 验证状态
    const demoStatusHtml = state.demoStatus ? `
        <div class="demo-summary demo-status-summary" aria-label="验证状态摘要">
            <div class="demo-pill"><span>天数</span><strong>${state.demoStatus.currentDay}</strong></div>
            <div class="demo-pill"><span>阶段</span><strong>${html(phaseLabelFor(state.demoStatus.phase))}</strong></div>
            <div class="demo-pill"><span>日志</span><strong>${state.demoStatus.businessLogCount ?? state.demoStatus.logCount}</strong></div>
            <div class="demo-pill"><span>错误</span><strong>${html(demoErrorCodeText(state.demoStatus.lastError))}</strong></div>
        </div>
        ${state.demoStatus.lastError ? `<p class="demo-error-line">最后错误：${html(demoErrorMessageText(state.demoStatus.lastError))}</p>` : ''}
    ` : '';

    // 验证结果
    const demoResultHtml = state.demoResult ? `
        <div class="demo-summary demo-result" aria-label="验证结果摘要">
            <div class="demo-pill"><span>日报</span><strong>${state.demoResult.reportCount}</strong></div>
            <div class="demo-pill"><span>日志</span><strong>${state.demoResult.logCount}</strong></div>
            <div class="demo-pill"><span>复盘号</span><strong>${state.demoResult.endingReviewId ? '已存档' : '--'}</strong></div>
            <div class="demo-pill"><span>结局</span><strong>${html(demoResultLabel(state.demoResult))}</strong></div>
        </div>
    ` : '';

    panel.innerHTML = `
        <div class="panel-header">
            <h3 class="panel-title">开发验证</h3>
            <span class="panel-badge">${html(profileLabelFor(devTools.profile))}</span>
        </div>
        <div class="demo-strategy-row">
            <select id="demoStrategy" class="demo-strategy-select" aria-label="开发验证策略" data-change-action="sync-demo-strategy-selects">
                ${demoStrategyOptions(current)}
            </select>
        </div>
        <div class="demo-brief" aria-label="开发验证简报">
            <strong>验证简报</strong>
            <p>${html(demoBrief)}</p>
        </div>
        ${defenseEvidencePanel}
        <div class="demo-controls">
            <button type="button" class="primary demo-control-button demo-control-primary" ${state.busy ? 'disabled' : ''} data-action="demo-run" ${demoRunAriaAttr}>${demoRunLabel}</button>
            <button type="button" class="demo-control-button" ${state.busy ? 'disabled' : ''} data-action="demo-reset" aria-label="开发验证：重置验证轮">重置轮</button>
            <button type="button" class="demo-control-button" ${state.busy ? 'disabled' : ''} data-action="demo-fast-forward" ${demoFastAriaAttr}>${demoFastLabel}</button>
        </div>
        ${demoStatusHtml}
        ${demoResultHtml}
    `;
}

async function demoReset() {
    const strategy = demoStrategyValue();
    await withBusy('重置验证轮', async () => {
        state.demoResult = await api('/api/dev/demo/reset', {
            method: 'POST',
            body: { scenario: strategy }
        });
        await refreshAfterWrite();
    });
}

async function demoRun() {
    const strategy = demoStrategyValue();
    await withBusy('跑验证脚本', async () => {
        state.demoResult = await api('/api/dev/demo/run-script', {
            method: 'POST',
            body: { strategy, targetDay: demoTargetDay(), stopOnError: true }
        });
        await refreshAfterWrite();
    });
}

async function demoResetAndRun() {
    const strategy = demoStrategyValue();
    await withBusy('答辩快跑', async () => {
        await api('/api/dev/demo/reset', {
            method: 'POST',
            body: { scenario: strategy }
        });
        state.demoResult = await api('/api/dev/demo/run-script', {
            method: 'POST',
            body: { strategy, targetDay: demoTargetDay(), stopOnError: true }
        });
        try {
            state.demoStatus = await api('/api/dev/demo/status');
        } catch (e) {
            state.demoStatus = null;
        }
        await refreshAfterWrite();
        openInsightTab('demo');
    });
}

async function demoFastForward() {
    await withBusy('快进验证轮', async () => {
        state.demoResult = await api('/api/dev/demo/fast-forward', {
            method: 'POST',
            body: { targetDay: demoTargetDay() }
        });
        await refreshAfterWrite();
    });
}

// 新手教程
let tutorialReturnFocus = null;

function tutorialFocusableControls() {
    const overlay = document.getElementById('tutorialOverlay');
    if (!overlay || overlay.hasAttribute('hidden')) return [];
    return [...overlay.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')]
        .filter(control => !control.disabled && control.offsetParent !== null);
}

function setGameInert(inert) {
    const game = document.getElementById('gameContainer');
    if (game && 'inert' in game) {
        game.inert = inert;
    }
}

function showTutorial() {
    const overlay = document.getElementById('tutorialOverlay');
    const game = document.getElementById('gameContainer');
    tutorialReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    overlay?.removeAttribute('hidden');
    overlay?.setAttribute('aria-hidden', 'false');
    overlay?.classList.remove('hidden');
    game?.classList.add('visible');
    setGameInert(true);
    requestAnimationFrame(() => document.getElementById('tutorialStart')?.focus());
    setStatus("教程已打开。看完后点“去玩”或“跳过教程”回到当前进度。", "info");
}

function hideTutorial(markSeen = true) {
    const overlay = document.getElementById('tutorialOverlay');
    const game = document.getElementById('gameContainer');
    overlay?.classList.add('hidden');
    overlay?.setAttribute('aria-hidden', 'true');
    overlay?.setAttribute('hidden', 'hidden');
    game?.classList.add('visible');
    setGameInert(false);
    if (markSeen) {
        localStorage.setItem('vup-tutorial-seen', 'true');
    }
    state.tutorialSeen = true;
    if (tutorialReturnFocus && document.contains(tutorialReturnFocus)) {
        tutorialReturnFocus.focus();
    }
    tutorialReturnFocus = null;
    setStatus("按当前主面板继续：登录、创建或今日行动会在首屏出现。", "info");
}

function handleTutorialKeydown(event) {
    const overlay = document.getElementById('tutorialOverlay');
    if (!overlay || overlay.hasAttribute('hidden')) return;

    if (event.key === 'Escape') {
        event.preventDefault();
        hideTutorial(true);
        return;
    }

    if (event.key !== 'Tab') return;

    const controls = tutorialFocusableControls();
    if (controls.length === 0) {
        event.preventDefault();
        return;
    }

    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
    }
}

function initTutorial() {
    const overlay = document.getElementById('tutorialOverlay');
    const startBtn = document.getElementById('tutorialStart');
    const skipBtn = document.getElementById('tutorialSkip');

    startBtn?.addEventListener('click', () => hideTutorial(true));
    skipBtn?.addEventListener('click', () => hideTutorial(true));
    overlay?.addEventListener('keydown', handleTutorialKeydown);

    hideTutorial(false);
    if (!localStorage.getItem('vup-tutorial-seen')) {
        setStatus("右上角 ? 可以查看数值说明；按首屏主按钮推进当天流程。", "info");
    }
}

// 标签页切换
function initTabs() {
    buildLiveDrawerShell();
    syncTabAvailability();
    document.querySelectorAll('.info-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            activateInfoTab(tab.dataset.tab);
        });
    });
    activateInfoTab(state.infoTab);
    initMainTabBarKeyboard();
}

function initMainTabBarKeyboard() {
    const tabBar = document.getElementById('mainTabBar');
    if (!tabBar) return;

    tabBar.addEventListener('keydown', (event) => {
        const tabs = Array.from(tabBar.querySelectorAll('[data-action="switch-main-tab"]'));
        const current = document.activeElement;
        const index = tabs.indexOf(current);
        if (index < 0) return;

        let next = -1;
        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
            next = (index + 1) % tabs.length;
        } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
            next = (index - 1 + tabs.length) % tabs.length;
        } else if (event.key === 'Home') {
            next = 0;
        } else if (event.key === 'End') {
            next = tabs.length - 1;
        }

        if (next >= 0) {
            event.preventDefault();
            tabs[next].focus();
            tabs[next].click();
        }
    });
}

function activateInfoTab(target = 'host') {
    buildLiveDrawerShell();
    syncTabAvailability();
    const drawerKey = normalizeInfoTab(target);
    state.infoTab = drawerKey;

    const drawer = document.getElementById('liveDrawer');
    const title = document.getElementById('liveDrawerTitle');
    const kicker = document.getElementById('liveDrawerKicker');
    const config = LIVE_DRAWERS[drawerKey] || LIVE_DRAWERS.host;

    document.querySelectorAll('.info-tab').forEach(tab => {
        const active = liveDrawerAlias(tab.dataset.tab) === drawerKey;
        tab.classList.toggle('active', active);
        tab.setAttribute('aria-pressed', String(active));
    });
    document.querySelectorAll('.live-drawer-tab').forEach(tab => {
        const active = liveDrawerAlias(tab.dataset.drawer) === drawerKey;
        tab.classList.toggle('active', active);
        tab.setAttribute('aria-pressed', String(active));
    });
    document.querySelectorAll('.live-drawer-panel').forEach(panel => {
        panel.classList.toggle('active', panel.dataset.liveDrawerPanel === drawerKey);
    });
    document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.remove('active'));
    document.getElementById('liveStageShell')?.classList.add('active');

    document.querySelectorAll('.info-panel').forEach(panel => panel.classList.remove('active'));
    activeInfoPanelIds(drawerKey).forEach(id => {
        document.getElementById(id)?.classList.add('active');
    });

    if (drawer) drawer.classList.add('open');
    if (title) title.textContent = config.title;
    if (kicker) kicker.textContent = config.kicker;
    syncLiveDrawerSummaries();

    const demoPanel = document.getElementById('demoPanel');
    if (demoPanel && drawerKey !== 'environment') {
        demoPanel.classList.add('hidden');
    }
    if (drawerKey === 'environment') {
        initLeaderboardData();
    }
    if (drawerKey === 'report') {
        window.setTimeout(() => CHARTS.renderFanLine('fanTrendChart', state.reports), 0);
    }
    renderDemo();
}

function activeInfoPanelIds(target) {
    const drawerKey = liveDrawerAlias(target);
    const phase = state.session?.phase || (state.vup ? "READY" : "");
    const stageFocus = Boolean(state.vup) && STAGE_FOCUS_PHASES.has(phase);
    const stageFocusPanels = {
        host: stageFocus ? ['statsSection', 'coachPanel', 'stageBriefingPanel'] : LIVE_DRAWERS.host.panels,
        npc: stageFocus ? ['npcPanel', 'stageBriefingPanel'] : ['npcPanel'],
        npcs: phase === "READY" ? ['npcPanel'] : (stageFocus ? ['npcPanel', 'stageBriefingPanel'] : LIVE_DRAWERS.npcs.panels),
        environment: stageFocus && phase === "ENDING_READY" ? ['endingForecastPanel', 'buzzPanel', 'ambientPanel'] : LIVE_DRAWERS.environment.panels,
        report: phase === "REPORT_READY" ? ['reportPanel', 'reportHistoryPanel'] : ['reportHistoryPanel']
    };
    if (stageFocusPanels[drawerKey]) {
        return stageFocusPanels[drawerKey];
    }
    const panelIds = LIVE_DRAWERS[drawerKey]?.panels || LIVE_DRAWERS.host.panels;
    if (drawerKey === 'report' && state.session?.phase !== 'REPORT_READY') {
        return panelIds.filter(id => id !== 'reportPanel');
    }
    return panelIds;
}

function openInsightTab(target = 'host', options = {}) {
    const preserveReportDrawer = options.preserveReportDrawer ?? state.session?.phase === 'REPORT_READY';
    if (!preserveReportDrawer && liveDrawerAlias(target) !== 'report') {
        collapseReportDrawer();
    }
    activateInfoTab(target);
}

function pulseFocusTarget(target) {
    if (!target) {
        return false;
    }
    target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
    if (!target.matches('button, [href], input, select, textarea, [tabindex]')) {
        target.setAttribute('tabindex', '-1');
    }
    target.focus({ preventScroll: true });
    target.classList.remove('objective-focus-pulse');
    window.setTimeout(() => target.classList.add('objective-focus-pulse'), 0);
    window.setTimeout(() => target.classList.remove('objective-focus-pulse'), 1600);
    return true;
}

function focusActionCard(actionType) {
    if ((state.session?.phase || 'READY') !== 'READY') {
        openInsightTab('npc');
        return;
    }
    const targetType = actionType || selectPrimaryReadyAction(state.actions, selectQuickActions(state.actions))?.actionType;
    const target = document.querySelector(`[data-action="daily-plan-submit"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="simple-primary-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="simple-alt-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="detail-submit-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="quick-submit-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector('[data-action="daily-plan-submit"]:not(:disabled)')
        || document.querySelector('[data-action="simple-primary-action"]:not(:disabled)')
        || document.querySelector('[data-action="simple-alt-action"]:not(:disabled)')
        || document.querySelector('[data-action="detail-submit-action"][data-recommended="true"]')
        || document.querySelector('[data-action="quick-submit-action"][data-recommended="true"]');
    if (!target) {
        openInsightTab('npc');
        return;
    }
    pulseFocusTarget(target);
}

function focusReadyQuickAction(actionType) {
    if ((state.session?.phase || 'READY') !== 'READY') {
        openInsightTab('npc');
        return;
    }
    const targetType = actionType || selectPrimaryReadyAction(state.actions, selectQuickActions(state.actions))?.actionType;
    const target = document.querySelector(`[data-action="daily-plan-submit"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="simple-primary-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="simple-alt-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="quick-submit-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector(`[data-action="detail-submit-action"][data-action-type="${cssEscapeValue(targetType)}"]`)
        || document.querySelector('[data-action="daily-plan-submit"]:not(:disabled)')
        || document.querySelector('[data-action="simple-primary-action"]:not(:disabled)')
        || document.querySelector('[data-action="simple-alt-action"]:not(:disabled)')
        || document.querySelector('[data-action="quick-submit-action"][data-recommended="true"]')
        || document.querySelector('[data-action="detail-submit-action"][data-recommended="true"]');
    if (!target) {
        openInsightTab('npc');
        return;
    }
    pulseFocusTarget(target);
}

function focusActionResultHud() {
    const target = document.querySelector('.action-result-hud');
    pulseFocusTarget(target);
}

function cssEscapeValue(value) {
    if (window.CSS?.escape) {
        return window.CSS.escape(String(value || ""));
    }
    return String(value || "").replace(/["\\]/g, "\\$&");
}

function openScoreGuideTarget(target) {
    if (target === 'route') {
        focusActionCard();
        return;
    }
    if (target === 'evidence') {
        openInsightTab('buzz');
        window.setTimeout(() => {
            if (pulseFocusTarget(document.querySelector('.ending-forecast-gap-primary'))) {
                return;
            }
            openInsightTab('combo');
            if (pulseFocusTarget(document.querySelector('.combo-next-target'))) {
                return;
            }
            openInsightTab('buzz');
            pulseFocusTarget(document.getElementById('endingForecastPanel'));
        }, 0);
        return;
    }
    if (target === 'risk') {
        openDebtControl();
    }
}

// 接口调用
function renderRestartContractStrip(contract, memory, boundary) {
    if (!contract && !memory) {
        return "";
    }
    const previousScore = Number(contract?.previousScore || 0);
    const nextGrade = localizedVisibleTextOrFallback(contract?.nextGrade, "");
    const nextGradeScore = Number(contract?.nextGradeScore || 0);
    const scoreGap = Number(contract?.scoreGap || Math.max(0, nextGradeScore - previousScore));
    const gradeText = previousScore > 0
        ? `${previousScore}分 -> ${nextGrade || "下一档"}${scoreGap > 0 ? ` 差${scoreGap}` : ""}`
        : (nextGrade ? `首通后冲${nextGrade}档` : "先完成首通");
    const weakness = localizedVisibleTextOrFallback(contract?.primaryWeaknessLabel, "路线专注");
    const stageGoal = localizedVisibleTextOrFallback(contract?.stageOneGoal, memory?.runObjective || memory?.nextGoal || "首周先把复活赛目标打成证据。");
    const boundaryText = localizedVisibleTextOrFallback(boundary || memory?.boundary, "只继承路线提示，不继承旧债。");
    return `
        <div class="restart-contract-strip" aria-label="复活赛升档合约">
            <span><em>升档</em><strong>${html(gradeText)}</strong></span>
            <span><em>短板</em><strong>${html(weakness)}</strong></span>
            <span><em>首周</em><strong>${html(stageGoal)}</strong></span>
            <small>${html(boundaryText)}</small>
        </div>
    `;
}

function renderOpeningAdvice({
    openingAdvice,
    openingStyle,
    openingContractLabel,
    openingPlan,
    openingContractListHtml,
    openingRiskBoundaryHtml,
    restartMemoryHtml
}) {
    const adviceCard = openingAdvice ? `
        <div class="opening-advice-card">
            <div class="opening-advice-head">
                <span class="opening-advice-kicker">${html(openingStyle || '出道风格')}</span>
                <strong>${html(openingContractLabel || '开局建议')}</strong>
            </div>
            <span class="opening-advice-plan">${html(openingPlan || '稳健开播')}</span>
            <p>${html(openingAdvice)}</p>
            ${openingContractListHtml}
            ${openingRiskBoundaryHtml}
        </div>
    ` : "";
    const fallback = restartMemoryHtml ? `
        <div class="opening-advice-fallback" aria-label="复活赛通用开局目标">
            <span>复活赛前三天</span>
            <strong>先补最缺的路线证据</strong>
            <small>先处理上轮最大旧账</small>
            <small>前三天不要同时追三条路线</small>
        </div>
    ` : "";
    return `${adviceCard}${restartMemoryHtml || ""}${fallback}`;
}

function atlasTargetActionType(targetType = state.achievementProgress?.endingCollection?.nextTargetType) {
    const target = String(targetType || "").trim();
    if (!target || target === "COMPLETE" || target === "UNKNOWN") {
        return "";
    }
    const routeTypes = primaryRouteActionTypes(target);
    const available = Array.isArray(state.actions) ? state.actions : [];
    return routeTypes.find(type => available.some(action => action?.actionType === type)) || routeTypes[0] || "";
}

function endingAtlasGuidance(targetType) {
    const target = String(targetType || "UNKNOWN").trim();
    const route = routeLabelFor(target);
    const actionType = atlasTargetActionType(target);
    const action = actionType ? actionLabelFor(actionType) : {
        UNKNOWN: "选一条基础路线",
        ELECTRONIC_PICKLE: "杂谈复盘",
        SINGING_IDOL: "练歌",
        SLICE_SAINT: "发布切片",
        DANCE_MEME: "练舞",
        SOCIAL_COLLAB: "同台互动",
        BLACK_RED_MAIN_STAGE: "直播企划",
        CYBER_GIRLFRIEND: "粉丝群维护",
        DD_BUS_STOP: "同台互动",
        MAIN_STAGE_KING: "直播企划",
        GLORIOUS_GRADUATION: "粉丝群维护"
    }[target] || "连续同路行动";
    const risk = {
        UNKNOWN: "主要风险：路线不定，证据散掉",
        ELECTRONIC_PICKLE: "主要风险：热度低，记忆点不足",
        SINGING_IDOL: "主要风险：只练不播，缺代表事件",
        SLICE_SAINT: "主要风险：同梗疲劳和旧账回旋",
        DANCE_MEME: "主要风险：同梗疲劳和旧账回旋",
        SOCIAL_COLLAB: "主要风险：DD来得快，老粉留存要补",
        DD_BUS_STOP: "主要风险：DD来得快，老粉留存要补",
        BLACK_RED_MAIN_STAGE: "主要风险：旧账会拖低结局体面度",
        MAIN_STAGE_KING: "主要风险：高热度必须同步控旧账",
        CYBER_GIRLFRIEND: "主要风险：陪伴边界和独角兽压力",
        GLORIOUS_GRADUATION: "主要风险：最后一周别冲高危爆点"
    }[target] || "主要风险：证据链不够集中";
    return { route, action, risk, actionType };
}

function openAtlasGoalTarget() {
    const actionType = atlasTargetActionType();
    if (actionType) {
        focusActionCard(actionType);
        return;
    }
    openInsightTab('routes');
}

async function api(path, options = {}) {
    const init = {
        method: options.method || 'GET',
        headers: options.body ? { 'Content-Type': 'application/json' } : {},
        credentials: 'same-origin',
        body: options.body ? JSON.stringify(options.body) : undefined
    };

    let response;
    try {
        response = await fetch(path, init);
    } catch (error) {
        throw new Error("后台掉线了，刷新试试。");
    }

    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new Error(response.ok
            ? "服务返回了无法解析的数据，请刷新页面后重试。"
            : "后台回了异常，刷新试试。");
    }

    if (!response.ok || !payload.success) {
        const error = new Error(apiErrorMessage(payload, response.status));
        error.code = payload.code;
        error.status = response.status;
        throw error;
    }
    return payload.data;
}

async function maybePostOpeningStyleChoice(choice) {
    if (state.openingStyleBackendAvailable === false) return null;

    const body = {
        style: choice.key,
        openingStyle: choice.key,
        route: choice.route,
        routeType: choice.route,
        idempotencyKey: stableIdempotencyKey("opening-style", [state.vup?.id || "NO_VUP", choice.key])
    };
    const candidates = [
        "/api/onboarding/opening-style",
        "/api/vup/opening-style",
        "/api/day/opening-style"
    ];

    for (const path of candidates) {
        try {
            const result = await api(path, { method: "POST", body });
            state.openingStyleBackendAvailable = true;
            clearStableIdempotencyKey("opening-style", [state.vup?.id || "NO_VUP", choice.key]);
            return result || {};
        } catch (error) {
            const status = Number(error?.status || 0);
            if (status === 404 || status === 405 || status === 400) {
                continue;
            }
            throw error;
        }
    }

    state.openingStyleBackendAvailable = false;
    return null;
}

// 运行任务（带错误处理）
async function run(label, task) {
    try {
        const result = await task();
        return result;
    } catch (error) {
        await refreshAfterWriteFailure();
        throw error;
    }
}

function setLocalSaveSlots(saveSlots = []) {
    const slots = Array.isArray(saveSlots) ? saveSlots : [];
    state.saveSlots = slots;
    state.hasSave = slots.some(slot => slot?.occupied);
}

function shouldKeepBootstrapUser(user) {
    return Boolean(user && user.username && user.username !== "local_player");
}

async function applyGameStartPayload(payload) {
    resetGameState();
    state.user = payload?.user || state.user;
    state.vup = payload?.activeRun || null;
    state.session = payload?.daySession || null;
    setLocalSaveSlots(payload?.saveSlots);
    state.actions = [];
    if (!state.vup) return false;

    state.actions = await api('/api/actions');
    await refreshPhaseData();
    return true;
}

async function finishRunEntry(payload) {
    const loaded = await applyGameStartPayload(payload);
    render();
    checkMilestones();
    if (loaded && state.session?.phase === 'READY') triggerDailyDialogue();
    return loaded;
}

async function refreshLocalSaveSlots() {
    try {
        const slots = await api('/api/save-slots');
        setLocalSaveSlots(slots);
    } catch (e) {
        // 存档摘要刷新失败不影响当前局内流程。
    }
}

// 刷新数据
async function hydrate() {
    setBootState("loading", "正在接回你的出道局", "同步存档、角色、今日阶段和行动列表。");
    try {
        const bootstrap = await api('/api/game/bootstrap');
        resetGameState();
        setLocalSaveSlots(bootstrap?.saveSlots);
        state.user = shouldKeepBootstrapUser(bootstrap?.user) ? bootstrap.user : null;

        if (bootstrap?.activeRun) {
            state.user = bootstrap.user || state.user;
            state.vup = bootstrap.activeRun;
            state.session = bootstrap.daySession || null;
            state.actions = await api('/api/actions');
            await refreshPhaseData();
        }

        setBootState("ready");
        render();
        if (typeof initStatCharts === 'function') initStatCharts();
        if (typeof updateStatCharts === 'function') updateStatCharts();
        checkMilestones();
        if (state.session && state.session.phase === 'READY') triggerDailyDialogue();
        return;
    } catch (e) {
        if (bootErrorIsBlocking(e)) {
            setBootState("failed", "没有连上后台", e.message || "请检查服务是否仍在运行，然后重新加载。");
            render();
            return;
        }
    }

    try {
        state.user = await api('/api/auth/current');
    } catch (e) {
        if (bootErrorIsBlocking(e)) {
            setBootState("failed", "没有连上后台", e.message || "请检查服务是否仍在运行，然后重新加载。");
            render();
            return;
        }
        state.user = null;
    }

    try {
        state.vup = await api('/api/vup/current');
        state.session = await api('/api/day/session');
        state.actions = await api('/api/actions');
        await refreshPhaseData();
    } catch (e) {
        if (state.user && bootErrorIsBlocking(e)) {
            setBootState("failed", "进度同步失败", e.message || "今日阶段没有取回，请重新加载。");
            render();
            return;
        }
        resetGameState();
        // 未登录或无VUP
    }
    setBootState("ready");
    render();
    if (typeof initStatCharts === 'function') initStatCharts();
    if (typeof updateStatCharts === 'function') updateStatCharts();
    checkMilestones();
    if (state.session && state.session.phase === 'READY') triggerDailyDialogue();
}

async function refreshPhaseData() {
    if (!state.vup) return;

    try {
        state.reports = await api('/api/reports');
    } catch (e) {}

    try {
        state.npcSpotlight = await api('/api/npc/spotlight');
    } catch (e) {}

    try {
        state.platformData = await api('/api/npc/platform');
    } catch (e) {}

    try {
        state.chainEvent = await api('/api/npc/chain-event');
    } catch (e) {}

    try {
        state.buzzBriefing = await api('/api/buzz/briefing');
    } catch (e) {}

    try {
        state.funAudienceProfile = await api('/api/audience/fun-profile');
    } catch (e) {}

    try {
        state.comboDiscovery = await api('/api/combo/discovery');
    } catch (e) {}

    try {
        state.achievementProgress = await api('/api/achievement/progress');
    } catch (e) {}

    try {
        state.dailyHighlights = await api('/api/highlight/list');
    } catch (e) {
        state.dailyHighlights = null;
    }

    try {
        state.danmaku = await api('/api/danmaku/current');
    } catch (e) {}

    try {
        state.fanTopics = await api('/api/fan-topic/options');
    } catch (e) {}

    try {
        state.endingForecast = await api('/api/ending/forecast');
    } catch (e) {}

    try {
        state.personaTags = await api('/api/persona/tags');
    } catch (e) {}

    try {
        state.memeLifecycle = await api('/api/meme/lifecycle');
    } catch (e) {}

    try {
        state.stageBriefing = await api('/api/stage/briefing');
    } catch (e) {
        state.stageBriefing = null;
    }

    try {
        state.audienceExpectation = await api('/api/audience/expectation');
    } catch (e) {}

    try {
        state.streamPlans = await api('/api/stream/plans');
    } catch (e) {}

    if (state.session?.phase === 'OFF_STREAM_READY') {
        try {
            state.offStreamOptions = await api('/api/offstream/options');
        } catch (e) {
            state.offStreamOptions = [];
        }
    } else {
        state.offStreamOptions = [];
    }

    try {
        state.pendingInteraction = await api('/api/interaction/pending');
    } catch (e) {}

    try {
        state.riskTools = await api('/api/risk-tool/options');
    } catch (e) {}

    if (state.session?.phase === 'NEED_TITLE') {
        state.titles = await api('/api/stream/titles');
    } else {
        state.titles = [];
    }

    if (state.session?.phase === 'NEED_EVENT_CHOICE') {
        try {
            state.pendingEvent = await api('/api/event/pending');
        } catch (e) {}
    } else {
        state.pendingEvent = null;
    }

    if (state.session?.phase === 'NEED_INTERACTION_CHOICE') {
        try {
            state.pendingInteraction = await api('/api/interaction/pending');
        } catch (e) {}
    }

    if (state.session?.phase === 'REPORT_READY' || state.session?.phase === 'ENDING_READY') {
        try {
            state.report = await api('/api/report/today');
        } catch (e) {}
    } else {
        state.report = null;
    }

    if (state.session?.phase === 'ENDING_READY') {
        try {
            state.ending = await api('/api/ending/review');
        } catch (e) {}
    } else {
        state.ending = null;
        state.endingShareCard = null;
        state.restartBiasOverride = null;
    }

    // 2.2 风险预警：危机倒计时
    try {
        state.crisisAlerts = await api('/api/risk-tool/alerts');
    } catch (e) {
        state.crisisAlerts = [];
    }

    // 1.4 对手威胁：竞争对手进度
    try {
        state.rivals = await api('/api/rivals');
    } catch (e) {
        state.rivals = null;
    }

    // 2.3 结局图鉴：已解锁结局集合
    try {
        state.endingAtlas = await api('/api/ending/atlas');
    } catch (e) {
        state.endingAtlas = null;
    }

    // 2.5 回放：结局时拉取本局时间线（需 vupId 参数）
    if (state.session?.phase === 'ENDING_READY' && state.vup?.id) {
        try {
            state.timeline = await api('/api/reports/timeline?vupId=' + encodeURIComponent(state.vup.id));
        } catch (e) {
            state.timeline = null;
        }
    }
}

async function refreshAfterWrite() {
    state.vup = await api('/api/vup/current');
    state.session = await api('/api/day/session');
    state.actions = await api('/api/actions');
    await refreshLocalSaveSlots();
    await refreshPhaseData();
    animateStatChanges();
    if (typeof initStatCharts === 'function') initStatCharts();
    if (typeof updateStatCharts === 'function') updateStatCharts();
}

async function refreshAfterWriteFailure() {
    try {
        state.vup = await api("/api/vup/current");
        state.session = await api("/api/day/session");
        state.actions = await api('/api/actions');
        await refreshLocalSaveSlots();
        await refreshPhaseData();
    } catch (e) {
        // 状态恢复失败
    }
}

// ============ 旅程时间线 ============
function renderTimelinePanel() {
    const panel = document.getElementById('timelinePanel');
    if (!panel || !state.vup) {
        if (panel) { panel.innerHTML = ''; panel.classList.add('hidden'); }
        return;
    }

    const currentDay = state.session?.day || state.vup.day || 1;
    const maxDay = runMaxDay();
    const reports = state.reports || [];
    const reportDays = new Set(reports.map(r => r.day));

    // 生成本局天数的圆点
    let dots = '';
    for (let d = 1; d <= maxDay; d++) {
        let cls = 'tl-dot';
        if (d < currentDay) cls += ' done';
        else if (d === currentDay) cls += ' current';
        else cls += ' future';

        const hasReport = reportDays.has(d);
        const title = d === currentDay ? `第${d}天（今天）` : d < currentDay ? `第${d}天（已完成）` : `第${d}天`;
        dots += '<span class="' + cls + '" title="' + title + '" data-day="' + d + '">' + d + '</span>';
    }

    // 当前阶段描述
    const phase = state.session?.phase || 'READY';
    const phaseLabel = {
        READY: '选择今日行动',
        NEED_TITLE: '选择直播标题',
        ACTION_RESOLVED: '行动已结算',
        NEED_INTERACTION_CHOICE: '处理事件',
        NEED_EVENT_CHOICE: '处理事件',
        REPORT_READY: '查看日报',
        ENDING_READY: '结局复盘'
    }[phase] || '进行中';

    panel.classList.remove('hidden');
    panel.innerHTML = '<div class="timeline-card">'
        + '<div class="timeline-header"><span class="timeline-title">🗺️ 旅程进度</span><span class="timeline-day">第' + currentDay + '天/' + maxDay + '天</span></div>'
        + '<div class="timeline-dots">' + dots + '</div>'
        + '<div class="timeline-legend"><span class="tl-legend done">● 已完成</span><span class="tl-legend current">● 今天</span><span class="tl-legend future">○ 未到</span></div>'
        + '</div>';
}

// ============ 4.3 中央主决策区：按 phase 显隐调度 + 阶段进度条 ============
// phase → stepper 步骤索引（0 行动 / 1 标题 / 2 事件 / 3 日报）
const PHASE_STEP_MAP = {
    READY: 0, ACTION_RESOLVED: 0,
    NEED_TITLE: 1,
    NEED_INTERACTION_CHOICE: 2, NEED_EVENT_CHOICE: 2,
    OFF_STREAM_READY: 2, OFF_STREAM_RESOLVED: 2,
    REPORT_READY: 3,
    ENDING_READY: 3
};

function renderMainDecisionCard() {
    const phase = state.session?.phase || (state.vup ? 'READY' : null);
    updatePhaseStepper(phase);

    // 主决策区阶段提示：当 actionPanel/offStreamPanel/endingPanel 都不显示时，给一个引导卡片
    const hint = document.getElementById('mainDecisionHint');
    if (!hint) return;
    const childVisible = ['actionPanel', 'offStreamPanel', 'endingPanel'].some(id => {
        const el = document.getElementById(id);
        return el && !el.classList.contains('hidden') && el.innerHTML.trim() !== '';
    });
    const hintHtml = mainDecisionHintHtml(phase);
    if (childVisible || !hintHtml) {
        hint.classList.add('hidden');
        hint.innerHTML = '';
    } else {
        hint.innerHTML = hintHtml;
        hint.classList.remove('hidden');
    }
}

function updatePhaseStepper(phase) {
    const activeStep = PHASE_STEP_MAP[phase];
    // 主卡片下方进度条
    const stepper = document.getElementById('phaseStepper');
    if (stepper) {
        const steps = stepper.querySelectorAll('.phase-step');
        steps.forEach((el, i) => {
            const idx = (typeof activeStep === 'number') ? activeStep : 0;
            el.classList.toggle('active', i === idx);
            el.classList.toggle('done', i < idx);
        });
    }
    // 顶栏 dayCycleStepper 同步高亮（data-phase: ACTION/TITLE/EVENT/REPORT）
    const topStepper = document.getElementById('dayCycleStepper');
    if (topStepper) {
        const phaseKey = (typeof activeStep === 'number')
            ? ['ACTION', 'TITLE', 'EVENT', 'REPORT'][activeStep]
            : 'ACTION';
        topStepper.querySelectorAll('.stepper-item').forEach(el => {
            el.classList.toggle('active', el.dataset.phase === phaseKey);
        });
    }
}

function mainDecisionHintHtml(phase) {
    const hints = {
        NEED_TITLE: { icon: '🎬', title: '选择直播标题', desc: '在弹出的标题面板里挑一个，或先在直播间打赏礼物提升热度加成。' },
        ACTION_RESOLVED: { icon: '✅', title: '行动已结算', desc: '正在等待下一阶段，可在直播间继续互动累积热度。' },
        NEED_INTERACTION_CHOICE: { icon: '⚡', title: '处理现场事件', desc: '请在弹出的事件面板中做出选择。' },
        NEED_EVENT_CHOICE: { icon: '🎭', title: '处理事件', desc: '请在弹出的事件面板中做出选择。' },
        OFF_STREAM_READY: { icon: '🏠', title: '下播时间', desc: '点击下播按钮处理场外事务。' },
        OFF_STREAM_RESOLVED: { icon: '🏠', title: '场外已结算', desc: '等待进入日报。' },
        REPORT_READY: { icon: '📋', title: '查看日报', desc: '请打开日报查看今日结算。' },
        ENDING_READY: { icon: '🎬', title: '结局复盘', desc: '本局已结束，查看结局展示与回放。' }
    };
    const h = hints[phase];
    if (!h) return '';
    return '<div class="main-decision-hint-card">'
        + '<div class="hint-icon">' + h.icon + '</div>'
        + '<div class="hint-body"><strong>' + html(h.title) + '</strong>'
        + '<span>' + html(h.desc) + '</span></div>'
        + '</div>';
}

// 渲染函数
function render() {
    renderBootState();
    renderStatus();
    syncShellMode();
    syncBGM();
    syncTabAvailability();
    syncGameplayInfoTab();
    renderHeader();
    syncAudioSettingsPanel();
    syncLiveRoomStage();
    syncLiveDrawerSummaries();
    renderCoach();
    if (state.vup) renderOperationDiagnosis(state.vup, state.session);
    renderAuth();
    renderStats();
    renderActions();
    renderOffStreamPanel();
    renderFanTopics();
    renderDanmaku();
    renderAmbientPanel();
    renderTitles();
    renderEvent();
    renderReport();
    renderEnding();
    renderMainDecisionCard();
    renderNPC();
    renderPlatform();
    renderBuzz();
    renderFunAudienceProfile();
    renderAudienceExpectation();
    renderRouteGallery();
    renderCombo();
    renderPersonaTags();
    renderMemeLifecycle();
    renderEndingForecast();
    renderStageBriefing();
    renderDemo();
    renderReportHistory();
    renderInfoBrief();
    renderInsightDigest();
    renderAchievements();
    renderLeaderboard();
    renderUnlockAtlas();
    renderVisualMood();
    renderTimelinePanel();
    updateVupAvatar();
    positionStatusToast();
    initCollapsiblePanels();
    applyRouteTheme();
    syncDailyFeedbackPrompt();
    setTimeout(initTooltips, 100);
}

const METRIC_HELP_ITEMS = [
    {
        key: "stamina",
        label: "体力",
        icon: "❤",
        short: "行动消耗资源",
        high: "高：今天能做更重的直播、训练或运营行动。",
        low: "低：部分行动会不可选，连续硬撑容易吃风险。",
        change: "改变：休息、低压行动会恢复；直播、训练、投稿、同台互动会消耗。"
    },
    {
        key: "fans",
        label: "粉丝",
        icon: "👥",
        short: "成长和结局核心指标",
        high: "高：更容易进入好结局，也会提高平台排名和影响力。",
        low: "低：出圈慢，结局评分和路线推进会吃亏。",
        change: "改变：直播、投稿、切片、联动、稳定维护会提升；翻车、脱粉回踩会降低。"
    },
    {
        key: "reputation",
        label: "口碑",
        icon: "📊",
        short: "观众信任度",
        high: "高：观众更信任你，低风险行动更稳，结局更体面。",
        low: "低：更容易被黑，旧账更容易爆，商业合作更危险。",
        change: "改变：粉丝群维护、读信、低压直播、危机处理会提升；标题党、连续冲热度、争议互动会降低。"
    },
    {
        key: "watchHeat",
        label: "围观",
        icon: "LIVE",
        short: "直播间热闹程度",
        high: "高：弹幕更多，涨粉更快，也更容易触发舆论事件。",
        low: "低：直播间冷清，传播慢，但压力也小。",
        change: "改变：直播爆点、蹭热度、切片出圈会提升；休息、降温、稳口碑行动会降低。"
    },
    {
        key: "inspiration",
        label: "灵感",
        icon: "✦",
        short: "内容点子",
        high: "高：更容易做标题、企划、投稿和内容准备。",
        low: "低：内容型行动可能卡住，只能先补准备或低压运营。",
        change: "改变：刷动态、聊天、读信、复盘会提升；投稿、标题、企划和制作会消耗。"
    },
    {
        key: "coin",
        label: "运营预算",
        icon: "¥",
        short: "可花资源",
        high: "高：能投制作、处理风险、做运营投入。",
        low: "低：制作和风险处理受限，后期收官会更紧。",
        change: "改变：直播收益、商业反馈、平台顺风会增加；制作、投流、风险工具会消耗。"
    },
    {
        key: "giftIncome",
        label: "礼物收入",
        icon: "🎁",
        short: "直播商业表现",
        high: "高：商业化评分更好，也可能带来榜一压力和商业味。",
        low: "低：说明直播商业反馈弱，但也更少关系压力。",
        change: "改变：高热直播、稳定陪伴、商业回、回应观众会提升；低热或忽视互动会降低。部分收入可转成运营预算。"
    },
    {
        key: "trueFans",
        label: "真爱粉",
        icon: "核",
        short: "稳定支持你的核心观众",
        high: "高：低谷时更稳，口碑托底更强。",
        low: "低：粉丝结构偏虚，热度掉了容易散。",
        change: "改变：陪伴、读信、粉丝群维护、稳定内容会提升；长期忽视老粉或硬冲争议会降低。"
    },
    {
        key: "funFans",
        label: "乐子人",
        icon: "乐",
        short: "传播快但不稳定的观众",
        high: "高：出圈快、切片传播快，但节奏更容易失控。",
        low: "低：传播慢，但舆论压力较小。",
        change: "改变：整活、切片、黑红爆点会提升；降温、稳定内容和低压直播会降低占比。"
    },
    {
        key: "ddFans",
        label: "DD",
        icon: "DD",
        short: "从平台和联动来的流动观众",
        high: "高：扩圈快，但忠诚度低，容易坐一站就走。",
        low: "低：联动扩散弱，但粉丝结构更稳。",
        change: "改变：同台互动、联动排班、平台曝光会提升；少联动或转向陪伴维护会降低占比。"
    },
    {
        key: "unicornFans",
        label: "独角兽",
        icon: "独",
        short: "投入高但要求多的观众",
        high: "高：支持强，但联动、商业和边界问题更敏感。",
        low: "低：关系压力小，但陪伴路线收益会少一些。",
        change: "改变：陪伴营业、私信维护、长期稳定回应会提升；明确边界、减少暧昧营业会降低压力。"
    },
    {
        key: "debt",
        label: "旧账/风险",
        icon: "!",
        short: "过去选择留下的隐患",
        high: "多：未来更容易爆事件、掉口碑、影响结局体面度。",
        low: "少：行动空间更大，收官更安全。",
        change: "改变：危机处理、降温、粉丝群维护、低风险复盘会降低；标题党、连续冲热度、争议互动会增加。"
    }
];

const METRIC_HELP_BY_KEY = Object.fromEntries(METRIC_HELP_ITEMS.map(item => [item.key, item]));

function metricStatus(key, value, extra = {}) {
    const n = Number(value || 0);
    switch (key) {
        case "stamina": {
            const max = Math.max(1, Number(extra.max || 10));
            const ratio = n / max;
            if (ratio >= 0.7) return "状态良好";
            if (ratio >= 0.35) return "还能撑";
            return "该休息";
        }
        case "fans":
            if (n >= 3000) return "平台焦点";
            if (n >= 1000) return "小有名气";
            if (n >= 300) return "开始起势";
            return "新人起步";
        case "reputation":
            if (n >= 70) return "体面稳定";
            if (n >= 45) return "还能挽回";
            return "正在透支";
        case "watchHeat":
            if (n >= 70) return "主会场感";
            if (n >= 35) return "有人围观";
            return "低压";
        case "inspiration":
            if (n >= 5) return "点子充足";
            if (n >= 1) return "可做企划";
            return "需要补";
        case "coin":
            if (n >= 2000) return "预算宽裕";
            if (n >= 500) return "可投入";
            return "偏紧";
        default:
            return "";
    }
}

function metricTooltip(key, value, extra = {}) {
    const item = METRIC_HELP_BY_KEY[key];
    const statusValue = extra.statusValue ?? value;
    const displayValue = extra.displayValue ?? value;
    const status = metricStatus(key, statusValue, extra);
    if (!item) return status || "";
    return [
        `${item.label}`,
        `是什么：${item.short}`,
        status ? `当前：${displayValue}，${status}` : `当前：${displayValue}`,
        `高了怎样：${item.high.replace(/^高[：:]/, "")}`,
        `低了怎样：${item.low.replace(/^低[：:]|^少[：:]|^多[：:]/, "")}`,
        `怎么改变：${item.change.replace(/^改变[：:]/, "")}`
    ].filter(Boolean).join("\n");
}

function renderHeaderStatChip({ key, label, icon, value, status, tooltip }) {
    return `
        <div class="stat-item stat-item-${html(key)}" data-metric-key="${html(key)}" data-metric-tooltip="${html(tooltip)}" title="${html(tooltip)}">
            <span class="stat-icon" aria-hidden="true">${html(icon)}</span>
            <span class="stat-copy">
                <span class="stat-label">${html(label)}</span>
                <span class="stat-value">${html(value)}</span>
                <small>${html(status)}</small>
            </span>
        </div>
    `;
}

function premiumCoreStatusChips(v) {
    const stamina = Number(v.resources?.stamina ?? 0);
    const maxStamina = Number(v.resources?.maxStamina ?? 10);
    const fans = Number(v.fanStructure?.fans ?? v.fans ?? 0);
    const reputation = Number(v.opinion?.reputation ?? v.reputation ?? 0);
    const watchHeat = Number(v.opinion?.watchHeat ?? v.watchHeat ?? 0);
    const trueFans = Number(v.fanStructure?.trueFans ?? v.trueFans ?? 0);
    const funFans = Number(v.fanStructure?.funFans ?? v.funFans ?? 0);
    const unicornFans = Number(v.fanStructure?.unicornFans ?? v.unicornFans ?? 0);
    const ddFans = Number(v.fanStructure?.ddFans ?? v.ddFans ?? 0);
    const moodLine = visibleTextOrFallback(v.moodLine, "");
    const openDebts = activeDebts();
    const brain = operationalBrainSnapshot();
    const relationPressure = Math.min(100, Math.max(0, openDebts.length * 22 + unicornFans * 0.9 + ddFans * 0.7 + Math.max(0, 60 - reputation)));
    const chips = [
        {
            key: "operationBrain",
            label: "运营脑",
            icon: "OPS",
            value: brain.state,
            status: brain.state,
            tooltip: `${brain.title}\n${brain.detail}\n${brain.trajectory}`
        },
        {
            key: "bodyMind",
            label: "身心",
            icon: "HP",
            value: `${stamina}/${maxStamina}`,
            status: moodLine || metricStatus("stamina", stamina, { max: maxStamina }),
            tooltip: metricTooltip("stamina", `${stamina}/${maxStamina}`, { max: maxStamina, statusValue: stamina, displayValue: `${stamina}/${maxStamina}` })
        },
        {
            key: "publicHeat",
            label: "公开热度",
            icon: "LIVE",
            value: watchHeat,
            status: `${metricStatus("watchHeat", watchHeat)} · 粉${compactCount(fans)}`,
            tooltip: `${metricTooltip("watchHeat", watchHeat)}\n粉丝：${fans}，口碑：${reputation}`
        },
        {
            key: "relationshipPressure",
            label: "关系压力",
            icon: "REL",
            value: Math.round(relationPressure),
            status: openDebts.length ? `${openDebts.length}条旧账` : `真爱${compactCount(trueFans)}`,
            tooltip: `粉丝结构：真爱${trueFans}，乐子${funFans}，独角兽${unicornFans}，DD${ddFans}\n旧账：${openDebts.length}条`
        }
    ];
    return chips.map(chip => renderHeaderStatChip(chip)).join('');
}

function headerStatChips(v) {
    return premiumCoreStatusChips(v);
}

function showMetricHelp() {
    hideMetricHelp();
    const overlay = document.createElement("div");
    overlay.id = "metricHelpOverlay";
    overlay.className = "metric-help-overlay";
    overlay.setAttribute("role", "dialog");
    overlay.setAttribute("aria-modal", "true");
    overlay.setAttribute("aria-labelledby", "metricHelpTitle");
    overlay.innerHTML = `
        <div class="metric-help-dialog">
            <div class="metric-help-head">
                <div>
                    <span>数值说明</span>
                    <h2 id="metricHelpTitle">这些数字会怎样影响你的出道局</h2>
                </div>
                <button type="button" class="metric-help-close" data-action="close-metric-help" aria-label="关闭数值说明">×</button>
            </div>
            <div class="metric-help-grid">
                ${METRIC_HELP_ITEMS.map(item => `
                    <article class="metric-help-card">
                        <div class="metric-help-card-title">
                            <span>${html(item.icon)}</span>
                            <strong>${html(item.label)}</strong>
                        </div>
                        <p><b>是什么：</b>${html(item.short)}</p>
                        <ul>
                            <li><b>高了怎样：</b>${html(item.high.replace(/^高[：:]/, ""))}</li>
                            <li><b>低了怎样：</b>${html(item.low.replace(/^低[：:]|^少[：:]|^多[：:]/, ""))}</li>
                            <li><b>怎么改变：</b>${html(item.change.replace(/^改变[：:]/, ""))}</li>
                        </ul>
                    </article>
                `).join('')}
            </div>
        </div>
    `;
    overlay.addEventListener("click", event => {
        if (event.target === overlay) hideMetricHelp();
    });
    document.body.appendChild(overlay);
    overlay.querySelector(".metric-help-close")?.focus({ preventScroll: true });
}

function hideMetricHelp() {
    document.getElementById("metricHelpOverlay")?.remove();
}

window.showMetricHelp = showMetricHelp;
window.hideMetricHelp = hideMetricHelp;

let lastAnnouncedPhase = '';

function renderHeader() {
    const dayBadge = document.getElementById('dayBadge');
    const phaseText = document.getElementById('phaseText');
    const quickStats = document.getElementById('quickStats');
    const userInfo = document.getElementById('userInfo');

    if (state.vup) {
        dayBadge.textContent = formatDayBadge(state.session?.day || state.vup.day);
        const currentPhase = state.session?.phase || 'READY';
        phaseText.textContent = getPhaseText(currentPhase);

        // Announce phase changes to screen readers
        if (currentPhase !== lastAnnouncedPhase) {
            lastAnnouncedPhase = currentPhase;
            const announcement = `阶段变更：${getPhaseText(currentPhase)}`;
            const liveRegion = document.getElementById('appStatus');
            if (liveRegion) {
                liveRegion.textContent = '';
                requestAnimationFrame(() => { liveRegion.textContent = announcement; });
            }
        }

        quickStats.dataset.coreCount = "4";
        quickStats.innerHTML = premiumCoreStatusChips(state.vup);
    } else {
        dayBadge.textContent = "第--天";
        phaseText.textContent = '等待开播';
        lastAnnouncedPhase = '';
        delete quickStats.dataset.coreCount;
        quickStats.innerHTML = '';
    }

    if (state.user) {
        const userDisplayName = state.user.nickname || state.user.username;
        const skipConfirm = isActionConfirmSkipped();
        const activeSlot = activeLocalSlotNumber();
        const saveButtons = state.vup ? `
            <button type="button" class="logout-button" data-action="save-local-slot" ${state.busy ? 'disabled' : ''}>保存</button>
            <button type="button" class="logout-button" data-action="export-local-slot" data-slot-number="${activeSlot}" ${state.busy ? 'disabled' : ''}>导出</button>
            <button type="button" class="logout-button" data-action="import-local-slot" data-slot-number="${activeSlot}" ${state.busy ? 'disabled' : ''}>导入</button>
        ` : "";
        userInfo.innerHTML = `
            <label class="skip-confirm-toggle" title="跳过行动确认步骤">
                <input type="checkbox" ${skipConfirm ? 'checked' : ''} data-change-action="toggle-action-confirm-skip" aria-label="跳过确认">
                <span>跳过确认</span>
            </label>
            <span class="user-name" title="${html(userDisplayName)}">${html(userDisplayName)}</span>
            ${saveButtons}
            <button type="button" class="logout-button" data-action="logout" ${state.busy ? 'disabled' : ''}>退出</button>
        `;
    } else {
        userInfo.innerHTML = '';
    }

    // 1.1 礼物累积徽章 + 1.2 弹幕热度徽章
    const giftBadge = document.getElementById('giftAccumBadge');
    if (giftBadge) {
        const giftCount = (state.giftAccum && state.giftAccum.count) || 0;
        giftBadge.textContent = '🎁 ×' + giftCount;
        giftBadge.classList.toggle('hidden', giftCount === 0);
    }
    const dmHeatBadge = document.getElementById('danmakuHeatBadge');
    if (dmHeatBadge) {
        const heat = (state.danmakuAccum && state.danmakuAccum.heat) || 0;
        dmHeatBadge.textContent = '💬 🔥' + heat;
        dmHeatBadge.classList.toggle('hidden', heat === 0);
    }
}

function getPhaseText(phase) {
    const texts = {
        'READY': '选择今日行动',
        'NEED_TITLE': '选择直播标题',
        'ACTION_RESOLVED': '行动已结算',
        'NEED_INTERACTION_CHOICE': '处理现场事件',
        'NEED_EVENT_CHOICE': '处理正式事件',
        'REPORT_READY': '查看日报',
        'ENDING_READY': '结局复盘',
        'OFF_STREAM_READY': '下播时间'
    };
    return texts[phase] || visibleTextOrFallback(phase, "等待开播");
}

// ============ VUP 说话系统 ============
const ROUTE_PERSONALITY = {
    SINGING_IDOL: {
        day1: '第一天开播，唱了两首歌，虽然紧张但很开心。',
        day2: '昨天唱完嗓子有点紧，但今天又想唱了。',
        day3: '慢慢找到唱歌的节奏了，高音也稳了一点。',
        tired: '嗓子有点累了，但想到还有人想听我唱歌，就觉得值得。',
        fans100: '100个人听过我唱歌了！我要继续努力！',
        fans1000: '千粉了！每一首歌都没有白唱。',
        risk: '最近有点透支嗓子，得注意休息。',
        happy: '今天唱歌的状态特别好！',
        late: '最后几天了，用歌声冲刺到底！',
        mid: '半个月了，唱歌已经成了我的日常。',
        default: '唱歌是我最喜欢的事，希望能唱给更多人听。',
    },
    SLICE_SAINT: {
        day1: '第一天直播，我已经在想怎么切片了。',
        day2: '昨天的直播素材不错，今天好好切一切。',
        day3: '开始摸到切片的门道了，标题很重要。',
        tired: '今天切了5个片子，眼睛都花了。',
        fans100: '100个粉丝！我的切片功不可没！',
        fans1000: '千粉达成！切片就是我的秘密武器。',
        risk: '有个切片标题有点过了，得小心回旋镖。',
        happy: '这个切片肯定能火！',
        late: '最后几天了，多切几个爆款冲一波！',
        mid: '半个月了，切片技术越来越成熟了。',
        default: '好的切片就是最好的安利。',
    },
    ELECTRONIC_PICKLE: {
        day1: '第一天开播，就像和朋友聊天一样。',
        day2: '昨天聊得挺开心，今天继续杂谈。',
        day3: '已经习惯对着镜头说话了，像老朋友一样。',
        tired: '今天聊了好多，但很开心。',
        fans100: '100个人愿意听我唠嗑，好感动。',
        fans1000: '千粉了！都是愿意陪我聊天的朋友。',
        risk: '最近内容有点同质化，得想想新话题。',
        happy: '今天的杂谈回特别顺利！',
        late: '最后几天了，珍惜每一次和大家聊天的机会。',
        mid: '半个月了，和观众聊天已经是最快乐的事了。',
        default: '陪伴就是最长情的告白。',
    },
    BLACK_RED_MAIN_STAGE: {
        day1: '第一天就有人来喷了，没关系，黑红也是红。',
        day2: '争议还在发酵，但热度确实上去了。',
        day3: '开始习惯舆论场了，黑粉也是流量。',
        tired: '今天处理了好多争议，有点累。',
        fans100: '100个粉丝！虽然一半是来骂我的。',
        fans1000: '千粉了！黑红路线果然有效。',
        risk: '舆论压力有点大，得准备危机公关了。',
        happy: '今天的争议反而带来了更多关注！',
        late: '最后几天了，不管风评如何，先把热度拉满。',
        mid: '半个月了，我已经是个合格的争议制造机了。',
        default: '争议就是流量，我深谙此道。',
    },
    SOCIAL_COLLAB: {
        day1: '第一天就约了个联动，开门红！',
        day2: '昨天联动效果不错，今天再约一个。',
        day3: '联动名单越来越长了，人脉就是资源。',
        tired: '今天联动了3场，社交电量耗尽了。',
        fans100: '100个粉丝！都是联动带来的。',
        fans1000: '千粉了！联动网络终于见效了。',
        risk: '有个联动对象出了问题，得保持距离。',
        happy: '今天的联动效果超出预期！',
        late: '最后几天了，多约几场联动冲刺！',
        mid: '半个月了，联动已经成为我的核心打法。',
        default: '多交朋友总没错。',
    },
    DANCE_MEME: {
        day1: '第一天跳了个舞，虽然动作还很生硬。',
        day2: '昨天那个舞步还不够熟练，今天继续练。',
        day3: '开始找到跳舞的感觉了，卡点越来越准。',
        tired: '今天练了一天舞，腿都不是自己的了。',
        fans100: '100个粉丝！他们都是来看我跳舞的。',
        fans1000: '千粉了！舞蹈就是我的流量密码。',
        risk: '同一个梗跳太多遍了，得换新的。',
        happy: '今天的舞蹈视频要火！',
        late: '最后几天了，再跳几个爆款出来！',
        mid: '半个月了，舞蹈已经成了我的标签。',
        default: '下一个爆款舞蹈就是我发明的！',
    },
};

function vupQuote() {
    const route = state.vup?.currentRoute || 'UNKNOWN';
    const personality = ROUTE_PERSONALITY[route];
    const day = state.session?.day || 1;
    const stamina = state.vup?.resources?.stamina || 0;
    const fans = state.vup?.fanStructure?.fans || 0;
    const debts = activeDebts();
    const phase = state.session?.phase || 'READY';

    // 下播阶段
    if (phase === 'OFF_STREAM_READY') {
        const offStreamQuotes = [
            '主行动结束了，下播后做点什么呢？',
            '终于可以歇一会儿了，但还想做点什么...',
            '下播了！今天过得好快。',
            '收工了，接下来做点自己的事吧。',
        ];
        return offStreamQuotes[Math.floor(Math.random() * offStreamQuotes.length)];
    }

    // 路线专属台词优先
    if (personality) {
        if (day === 1) return personality.day1;
        if (day === 2) return personality.day2;
        if (day === 3) return personality.day3;
        if (stamina <= 2) return personality.tired;
        if (fans >= 100 && fans < 200) return personality.fans100;
        if (fans >= 1000) return personality.fans1000;
        if (debts && debts.length >= 3) return personality.risk;
        if (fans >= 500 && day <= 10) return personality.happy;
        if (day >= 25) return personality.late;
        if (day === 15) return personality.mid;
        return personality.default;
    }

    // 通用台词（无路线时的兜底）
    if (day === 1) return "第一天开播，有点紧张，希望有人来看。";
    if (day === 2) return "昨天还不错，今天继续加油！";
    if (day === 3) return "慢慢找到感觉了，希望能越来越好。";
    if (stamina <= 2) return "好累...今天想休息一下。";
    if (debts && debts.length >= 3) return "最近压力有点大，得处理一下。";
    if (fans >= 500 && day <= 10) return "没想到这么快就有人关注我了！";
    if (day === 15) return "半个月了，感觉找到了一点节奏。";
    if (day >= 25) return "最后几天了，拼一把！";
    if (fans >= 1000) return "千粉了！感觉离梦想越来越近。";

    const quotes = [
        "今天也要元气满满地开播！",
        "希望能给大家带来快乐。",
        "加油，我可以的！",
        "直播间的大家，早上好呀~",
        "今天试试新的内容吧。",
    ];
    return quotes[Math.floor(Math.random() * quotes.length)];
}

function vupReaction(actionType, isPositive) {
    const reactions = {
        TRAIN_SONG: { pos: "唱完感觉嗓子打开了！", neg: "今天嗓子有点紧，明天再练。" },
        TRAIN_DANCE: { pos: "这支舞越来越熟练了！", neg: "动作还有点僵硬，得多练。" },
        TRAIN_TALK: { pos: "聊得挺开心的，灵感来了！", neg: "今天话题有点干，下次准备充分点。" },
        STREAM_PLAN: { pos: "直播间好热闹！", neg: "今天观众不多，有点冷清。" },
        PUBLISH_VIDEO: { pos: "这个视频应该能火！", neg: "播放量不太理想，下次换个题材。" },
        PUBLISH_CLIP: { pos: "切片做得不错，应该能传播！", neg: "切片效果一般，标题可能没选好。" },
        FAN_GROUP_MAINTAIN: { pos: "和粉丝们聊得很开心！", neg: "群里有点小摩擦，得注意。" },
        NPC_INTERACT: { pos: "和同行交流收获很大！", neg: "互动效果一般，下次换个方式。" },
        REST: { pos: "充电完毕，明天继续！", neg: "休息了一天，感觉有点愧疚。" },
        // 下播行动反应
        VOCAL_PRACTICE: { pos: "练完声感觉嗓子打开了！", neg: "今天练得有点累，但进步了。" },
        SONG_SELECTION: { pos: "找到了几首好歌！", neg: "歌单还没选好，明天再挑。" },
        CLIP_SCOUTING: { pos: "找到了几个高光片段！", neg: "今天直播没什么亮点可切。" },
        THUMBNAIL_DESIGN: { pos: "封面设计得不错！", neg: "封面效果一般，再改改。" },
        READ_LETTERS: { pos: "粉丝的信好暖心！", neg: "信读完了，有点感动。" },
        TREND_WATCH: { pos: "发现了一个热点！", neg: "今天没什么值得关注的。" },
        CRISIS_PR: { pos: "危机应对方案准备好了！", neg: "黑流量有点难处理。" },
        DM_MAINTAIN: { pos: "同行关系维护得不错！", neg: "私信回得有点慢。" },
        COLLAB_PLAN: { pos: "联动排期安排好了！", neg: "联动时间还没定下来。" },
        MEME_RESEARCH: { pos: "找到了几个新梗！", neg: "今天没什么新梗可学。" },
        SHORT_VIDEO_IDEA: { pos: "想到一个爆款创意！", neg: "创意还在酝酿中。" },
        CHAT_ROOM: { pos: "和粉丝聊得很开心！", neg: "陪聊有点累，但值得。" },
        BROWSE_SOCIAL: { pos: "刷到一些有趣的内容！", neg: "手机刷得有点久。" },
        ORGANIZE_MATERIALS: { pos: "素材整理好了！", neg: "素材有点多，还没整完。" },
    };
    const r = reactions[actionType] || { pos: "今天还不错！", neg: "明天会更好。" };
    return isPositive ? r.pos : r.neg;
}

function routeExpressionImageForResult(result) {
    const route = state.vup?.currentRoute || state.endingForecast?.likelyEndingType || "UNKNOWN";
    const expressions = routeExpressionImages[route];
    if (!expressions) {
        return "";
    }
    const tone = actionResultHudTone(result);
    if (tone === "risk") {
        return expressions.breakdown || expressions.nervous || expressions.soft;
    }
    if (tone === "mixed") {
        return expressions.nervous || expressions.smug || expressions.soft;
    }
    const gained = Number(result?.fanChange || 0) > 0
        || Number(result?.routeScoreChange || 0) > 0
        || Number(result?.popularityChange || 0) > 0;
    return gained ? (expressions.smug || expressions.soft) : expressions.soft;
}

function renderVupReaction(result) {
    const isPositive = Number(result?.fanChange || 0) > 0;
    const text = vupReaction(result?.actionType, isPositive);
    if (!text) {
        return "";
    }
    const image = routeExpressionImageForResult(result);
    const avatar = image
        ? `<img class="vup-reaction-avatar" src="/gallery/${html(image)}" alt="${html(routeLabelFor(state.vup?.currentRoute || "UNKNOWN"))}路线表情" loading="lazy" decoding="async">`
        : '<span class="vup-reaction-mark" aria-hidden="true">💭</span>';
    return `<div class="vup-reaction">${avatar}<span class="vup-reaction-text">💭 ${html(text)}</span></div>`;
}

// ============ 里程碑庆祝系统 ============
const MILESTONE_ROUTE_DESCS = {
    fans100: {
        SINGING_IDOL: '100个人听过你唱歌了！每一首歌都没有白费。',
        SLICE_SAINT: '100个粉丝！你的切片功不可没！',
        ELECTRONIC_PICKLE: '100个人愿意听你唠嗑了，好感动。',
        BLACK_RED_MAIN_STAGE: '100个粉丝！虽然一半是来骂你的，但黑红也是红。',
        SOCIAL_COLLAB: '100个粉丝！都是联动带来的。',
        DANCE_MEME: '100个粉丝！他们都是来看你跳舞的。',
    },
    fans500: {
        SINGING_IDOL: '500个人在听你唱歌了！你的歌声正在被更多人听到。',
        SLICE_SAINT: '500粉丝！切片就是你的秘密武器。',
        ELECTRONIC_PICKLE: '500个人愿意陪你聊天了，这就是最好的陪伴。',
        BLACK_RED_MAIN_STAGE: '500粉丝！争议路线果然有效。',
        SOCIAL_COLLAB: '500粉丝！联动网络终于见效了。',
        DANCE_MEME: '500粉丝！舞蹈就是你的流量密码。',
    },
    fans1000: {
        SINGING_IDOL: '千粉了！每一首歌都没有白唱，你离梦想越来越近了。',
        SLICE_SAINT: '千粉达成！切片就是你的秘密武器。',
        ELECTRONIC_PICKLE: '千粉了！都是愿意陪你聊天的朋友。',
        BLACK_RED_MAIN_STAGE: '千粉了！黑红路线果然有效。',
        SOCIAL_COLLAB: '千粉了！联动网络终于见效了。',
        DANCE_MEME: '千粉了！舞蹈就是你的流量密码。',
    },
    day10: {
        SINGING_IDOL: '第10天了！你的歌声陪伴了大家1/3的旅程。',
        SLICE_SAINT: '第10天！切片库已经初具规模了。',
        ELECTRONIC_PICKLE: '第10天！你和观众们已经成了老朋友。',
        BLACK_RED_MAIN_STAGE: '第10天！争议已经成了你的标签。',
        SOCIAL_COLLAB: '第10天！联动名单越来越长了。',
        DANCE_MEME: '第10天！你的舞蹈已经越来越熟练了。',
    },
    day20: {
        SINGING_IDOL: '第20天了！最后10天，用歌声冲刺到底！',
        SLICE_SAINT: '第20天！最后几个切片要打出爆款！',
        ELECTRONIC_PICKLE: '第20天了！珍惜每一次和大家聊天的机会。',
        BLACK_RED_MAIN_STAGE: '第20天！最后10天，不管风评如何，先把热度拉满。',
        SOCIAL_COLLAB: '第20天！最后的联动冲刺！',
        DANCE_MEME: '第20天！再跳几个爆款出来！',
    },
};

const MILESTONES = [
    { key: 'fans100', label: '🎉 100粉丝达成！', desc: () => MILESTONE_ROUTE_DESCS.fans100?.[state.vup?.currentRoute] || '你的直播间开始有人气了', check: () => (state.vup?.fanStructure?.fans || 0) >= 100 },
    { key: 'fans500', label: '🔥 500粉丝！', desc: () => MILESTONE_ROUTE_DESCS.fans500?.[state.vup?.currentRoute] || '你已经是小有名气的UP主了', check: () => (state.vup?.fanStructure?.fans || 0) >= 500 },
    { key: 'fans1000', label: '👑 千粉达成！', desc: () => MILESTONE_ROUTE_DESCS.fans1000?.[state.vup?.currentRoute] || '平台开始注意到你了', check: () => (state.vup?.fanStructure?.fans || 0) >= 1000 },
    { key: 'day10', label: '📅 第10天！', desc: () => MILESTONE_ROUTE_DESCS.day10?.[state.vup?.currentRoute] || '你已经完成了1/3的挑战', check: () => (state.session?.day || 0) >= 10 },
    { key: 'day20', label: '⚡ 第20天！', desc: () => MILESTONE_ROUTE_DESCS.day20?.[state.vup?.currentRoute] || '最后10天，冲刺阶段！', check: () => (state.session?.day || 0) >= 20 },
    { key: 'routeLocked', label: '📍 路线锁定！', desc: () => '路线：' + routeLabelFor(state.vup?.currentRoute || 'UNKNOWN'), check: () => state.vup?.currentRoute && state.vup.currentRoute !== 'UNKNOWN' },
    { key: 'gradeA', label: '⭐ 评分已达A级！', desc: '保持住就能拿到好结局', check: () => { try { const f = state.endingForecast; return f?.grade === 'A' || f?.grade === 'S'; } catch(e) { return false; } } },
];

const shownMilestones = new Set();

function checkMilestones() {
    if (!state.vup) return;
    for (const m of MILESTONES) {
        if (shownMilestones.has(m.key)) continue;
        try {
            if (m.check()) {
                shownMilestones.add(m.key);
                showMilestonePopup(m);
            }
        } catch(e) {}
    }
}

function showMilestonePopup(milestone) {
    const desc = typeof milestone.desc === 'function' ? milestone.desc() : milestone.desc;
    const popup = document.createElement('div');
    popup.className = 'milestone-popup';
    popup.innerHTML = '<div class="milestone-content"><div class="milestone-icon">' + milestone.label + '</div><div class="milestone-desc">' + html(desc) + '</div></div>';
    document.body.appendChild(popup);
    requestAnimationFrame(() => popup.classList.add('show'));
    setTimeout(() => { popup.classList.remove('show'); setTimeout(() => popup.remove(), 500); }, 3000);
}

let lastCoachPhaseKey = null;
let coachHideTimer = null;

function renderCoach() {
    const panel = document.getElementById('coachPanel');
    if (!panel) return;

    let title = "开一局本地存档";
    let body = "单机版默认直接开始。点“开始第一局”会创建本地档，之后也可以在读取本地存档里继续、导入或重开。";
    let next = "当前高亮卡片：开始第一局";

    if (state.user && !state.vup) {
        title = "创建VUP";
        body = "填好名字后点开始出道；创建成功会进入第1天。";
        next = "当前高亮卡片：创建你的VUP";
    } else if (state.vup) {
        const phase = state.session?.phase || 'READY';
        const phaseText = getPhaseText(phase);
        const copy = {
            READY: ["今天只做一个主决定", "先看推荐卡：它会告诉你得到什么、承担什么、会更像哪条路线。新手前三天照推荐走就能看懂循环。", "先选今日行动"],
            NEED_TITLE: ["给这场直播起标题", "标题不是装饰：稳标题保口碑，狠标题冲围观，也可能留下旧账。", "选一个直播标题"],
            NEED_INTERACTION_CHOICE: ["直播间正在等你回应", "把每个选项当成一次表态：稳住、冲热度，还是接梗，都可能写进今天的日报。", "处理直播现场节奏"],
            NEED_EVENT_CHOICE: ["今天的麻烦来了", "先看选项里的风险预览。单机版不赶时间，想清楚再点。", "处理今日事件"],
            REPORT_READY: ["先看三行，再开下一天", "日报先看粉丝、口碑、围观和风险提示；详情可以以后再展开。", "读完日报，进入下一天"],
            ENDING_READY: ["这局已经成型", "结局页会告诉你为什么走到这里。想重开就用复活赛，直接回到新一局。", "查看结局，开启复活赛"],
            OFF_STREAM_READY: ["下播后补一手", "这是轻量收尾：补路线、降风险，或直接跳过进日报。不会再让你重打一整天。", "选下播动作或跳过"]
        }[phase] || [phaseText, "按当前面板的按钮继续。", ""];
        [title, body, next] = copy;
        const currentDay = Number(state.session?.day || state.vup?.day || 1);
        const openingLine = openingPhaseCoachLine(phase, null, currentDay);
        if (openingLine) {
            title = `第${currentDay}天怎么点`;
            body = openingLine;
            next = {
                READY: "先选今日行动",
                NEED_TITLE: "选一个直播标题",
                NEED_INTERACTION_CHOICE: "处理直播现场节奏",
                NEED_EVENT_CHOICE: "处理今日事件",
                REPORT_READY: "读完日报，进入下一天",
                ENDING_READY: "查看结局，开启复活赛"
            }[phase] || "按当前高亮按钮继续";
        }
    }

    const coachKicker = state.vup && !isFirstRunOpeningTutorialActive(Number(state.session?.day || state.vup?.day || 1))
        ? "下一步"
        : (state.user ? "新手教练" : "单机入口");

    panel.innerHTML = `
        <button class="coach-close" type="button" data-action="toggle-coach-panel" aria-label="收起教练卡" title="收起">×</button>
        <button class="coach-restore" type="button" data-action="toggle-coach-panel" aria-label="展开教练卡">展开教练提示</button>
        <div class="coach-kicker">${html(coachKicker)}</div>
        <div class="coach-main">
            <div>
                <h3>${html(title)}</h3>
                <p>${html(body)}</p>
            </div>
            <span class="coach-phase">${state.session?.day ? formatDayBadge(state.session.day) : "开播前"}</span>
        </div>
        <div class="coach-next">${html(next)}</div>
    `;

    // 阶段变化时弹出提示，5秒后自动收起（像toast一样，不常驻）
    const phaseKey = state.session?.phase || 'pre';
    if (phaseKey !== lastCoachPhaseKey) {
        lastCoachPhaseKey = phaseKey;
        panel.classList.remove('collapsed');
        if (coachHideTimer) clearTimeout(coachHideTimer);
        coachHideTimer = setTimeout(() => {
            panel.classList.add('collapsed');
        }, 5000);
    }
}

function renderAuth() {
    const section = document.getElementById('authSection');
    const busyDisabled = state.busy ? 'disabled' : '';
    const maxDayLabel = runMaxDayLabel();
    const saveSlots = Array.isArray(state.saveSlots) ? state.saveSlots : [];
    const activeSlotNumber = Number(state.vup?.slotNumber || 1);
    const primarySlot = saveSlots.find(slot => Number(slot?.slotNumber) === activeSlotNumber)
        || saveSlots.find(slot => Number(slot?.slotNumber) === 1)
        || saveSlots.find(slot => slot?.occupied)
        || saveSlots[0]
        || null;
    const canContinueLocal = Boolean(primarySlot?.canContinue);
    const saveSummary = localSaveSlotSummary(primarySlot);
    const saveSlotCards = renderLocalSaveSlotCards(saveSlots, busyDisabled);
    if (state.user && state.vup) {
        section.innerHTML = '';
        return;
    }

    if (!state.user) {
        section.innerHTML = `
            <div class="panel guest-landing-panel">
                <div class="guest-start-box">
                    <span class="guest-start-kicker">30天出道局</span>
                    <strong>每天做一个关键运营选择，把新人V推向一种互联网命运。</strong>
                    ${canContinueLocal ? `<button type="button" class="primary guest-start-btn" data-action="continue-local-run" data-slot-number="${Number(primarySlot?.slotNumber || 1)}" ${busyDisabled}>${state.busy ? state.busyLabel : '继续上一局'}</button>` : ''}
                    <button type="button" class="primary guest-start-btn" data-action="quick-start-guest" data-slot-number="${Number(primarySlot?.slotNumber || 1)}" ${busyDisabled}>${state.busy ? state.busyLabel : '开始新局'}</button>
                    <small>本地自动存档；读日报、追路线、30天后生成结局战报。</small>
                    <button type="button" class="ghost atlas-entry-btn" data-action="open-unlock-atlas">🏅 成就图鉴</button>
                    <div class="solo-comfort-strip" aria-label="单机体验说明">
                        <span><strong>少填表</strong><small>点开始就能玩。</small></span>
                        <span><strong>看日报</strong><small>每天知道赚了什么、欠了什么。</small></span>
                        <span><strong>能复活</strong><small>结局后带目标重开。</small></span>
                    </div>
                </div>
                <details class="save-progress-panel">
                    <summary>本地存档 / 设置</summary>
                    <div class="save-progress-body">
                        <div class="home-secondary-grid" aria-label="次级入口">
                            <span><strong>本地存档</strong><small>继续、导入、导出和重开都在这里。</small></span>
                            <span><strong>设置</strong><small>音效、语音和教程按钮在页面右上角。</small></span>
                        </div>
                        ${saveSlotCards}
                        <div class="form-group">
                            <label for="username">存档名</label>
                            <input type="text" id="username" name="username" autocomplete="username" placeholder="输入存档名" value="player_${Date.now() % 10000}">
                        </div>
                        <div class="form-group">
                            <label for="password">存档口令</label>
                            <input type="password" id="password" name="password" autocomplete="current-password" placeholder="输入密码" value="pass1234">
                        </div>
                        <div class="form-group">
                            <label for="nickname">昵称</label>
                            <input type="text" id="nickname" name="nickname" autocomplete="nickname" placeholder="你的昵称" value="复活赛观众">
                        </div>
                        <div class="form-actions">
                            <button type="button" class="primary" data-action="register" ${busyDisabled}>${state.busy ? state.busyLabel : '创建本地存档'}</button>
                            <button type="button" data-action="login" ${busyDisabled}>打开旧档</button>
                        </div>
                    </div>
                </details>
            </div>
        `;
        return;
    }

    if (!state.vup) {
        section.innerHTML = `
            <div class="first-run-empty" role="note">
                <span>首局入口</span>
                <strong>选择一个出道风格，第一天会从今日行动开始。</strong>
                <small>卡组会说明首日打法、适合路线和风险提示。</small>
            </div>
            <div class="panel create-loadout-panel">
                <div class="panel-header">
                    <h3 class="panel-title">开局卡组</h3>
                    <span class="panel-badge">选人设，马上开播</span>
                </div>
                ${renderCreationStylePresets()}
                <div class="create-loadout-fields">
                    <div class="form-group">
                        <label for="vupName">VUP名称</label>
                        <input type="text" id="vupName" name="vupName" autocomplete="off" placeholder="起个响亮的名字" value="露米" data-input-action="sync-creation-style-preview">
                    </div>
                    <div class="form-group">
                        <label for="persona">人设方向</label>
                        <input type="text" id="persona" name="persona" autocomplete="off" placeholder="描述你的人设" value="低压杂谈新人，歌回偶尔上桌" data-input-action="sync-creation-style-preview">
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="primary" data-action="create-vup" ${busyDisabled}>${state.busy ? state.busyLabel : '开始出道 →'}</button>
                </div>
                <details class="create-primer">
                    <summary>可选：查看初始属性</summary>
                    <div class="create-primer-body">
                        <div>
                            <h4>初始属性</h4>
                            <div class="create-primer-grid">
                                <div class="stat-card">
                                    <div class="stat-card-label">歌力</div>
                                    <div class="stat-card-value">10</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">舞力</div>
                                    <div class="stat-card-value">10</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">杂谈</div>
                                    <div class="stat-card-value">12</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">梗力</div>
                                    <div class="stat-card-value">10</div>
                                </div>
                            </div>
                        </div>
                        <div>
                            <h4>粉丝结构预览</h4>
                            <div class="create-primer-grid">
                                <div class="stat-card">
                                    <div class="stat-card-label">总粉丝</div>
                                    <div class="stat-card-value">52</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">真爱粉</div>
                                    <div class="stat-card-value">32</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">乐子人</div>
                                    <div class="stat-card-value">8</div>
                                </div>
                                <div class="stat-card">
                                    <div class="stat-card-label">独角兽</div>
                                    <div class="stat-card-value">5</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </details>
            </div>
        `;
        syncCreationStylePreview();
    }
}

function localSaveSlotSummary(slot) {
    if (!slot?.occupied) {
        return "没有本地存档，直接开始第一局。";
    }
    const name = slot.name || '本地存档';
    const progress = `第${slot.day || 1}天 · ${getPhaseText(slot.phase)}`;
    if (slot.endingTitle) {
        return `${name} · ${progress} · 已结局 · ${slot.endingTitle}`;
    }
    return `${name} · ${progress}`;
}

function formatManualSaveTime(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value).replace("T", " ").slice(0, 19);
    }
    const pad = number => String(number).padStart(2, "0");
    return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function manualSaveStatusText(slot) {
    const day = Number(slot?.day || state.session?.day || state.vup?.day || 1);
    const phase = getPhaseText(slot?.phase || state.session?.phase || "READY");
    const savedAt = formatManualSaveTime(slot?.lastSavedAt) || formatManualSaveTime(new Date().toISOString());
    return `已保存：第${day}天 / ${phase} / ${savedAt}`;
}

function renderLocalSaveSlotCards(slots = [], busyDisabled = '') {
    const normalizedSlots = Array.isArray(slots) && slots.length
        ? slots
        : [1, 2, 3].map(slotNumber => ({ slotNumber, occupied: false }));
    return `
        <div class="local-save-slot-grid" aria-label="本地存档槽">
            ${normalizedSlots.map(slot => {
                const slotNumber = Number(slot?.slotNumber || 1);
                const occupied = Boolean(slot?.occupied);
                const active = Number(state.vup?.slotNumber || 0) === slotNumber;
                const title = occupied ? (slot.name || `存档槽 ${slotNumber}`) : '空槽，可开始新局';
                const progressLine = `第${slot.day || 1}天 · ${getPhaseText(slot.phase)}`;
                const endingLine = slot.endingTitle ? `已结局 · ${slot.endingTitle}` : '';
                const metaLines = occupied
                    ? [progressLine, endingLine].filter(Boolean)
                    : ['不会覆盖其他槽。'];
                return `
                    <div class="local-save-slot-card ${active ? 'active' : ''}" data-slot-number="${slotNumber}">
                        <div>
                            <span>槽 ${slotNumber}${active ? ' · 当前' : ''}</span>
                            <strong>${html(title)}</strong>
                            ${metaLines.map(line => `<small>${html(line)}</small>`).join('')}
                        </div>
                        <div class="local-save-slot-actions">
                            ${occupied
                                ? `<button type="button" data-action="continue-local-run" data-slot-number="${slotNumber}" ${busyDisabled}>继续</button>`
                                : `<button type="button" data-action="quick-start-guest" data-slot-number="${slotNumber}" ${busyDisabled}>开始</button>`}
                            ${occupied ? `<button type="button" data-action="export-local-slot" data-slot-number="${slotNumber}" ${busyDisabled}>导出</button>` : ''}
                            <button type="button" data-action="import-local-slot" data-slot-number="${slotNumber}" ${busyDisabled}>导入</button>
                            ${occupied ? `<button type="button" data-action="rename-local-slot" data-slot-number="${slotNumber}" ${busyDisabled}>重命名存档</button>` : ''}
                            ${occupied ? `<button type="button" data-action="restart-local-slot" data-slot-number="${slotNumber}" ${busyDisabled}>重新开始本槽</button>` : ''}
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

function renderCreationStylePresets() {
    const renderOption = (preset, index) => `
        <label class="create-style-card" title="${html(preset.personaSignal)}" data-action="select-creation-style" data-creation-style="${html(preset.key)}">
            <input type="radio" name="creationStyle" value="${html(preset.key)}" data-change-action="apply-creation-style-preset" data-creation-style="${html(preset.key)}" ${index === 0 ? "checked" : ""}>
            <span class="create-style-card-body">
                <span class="create-style-card-title">${html(preset.label)}</span>
                <span class="create-style-card-badge">${html(preset.badge)}</span>
                <span class="create-style-card-fantasy">${html(preset.fantasy || preset.personaSignal)}</span>
                <span class="create-style-card-facts" aria-label="开局卡组要点">
                    <span><em>适合路线</em><strong>${html(preset.fitRoute)}</strong></span>
                    <span><em>首日建议</em><strong>${html(preset.firstMove)}</strong></span>
                    <span><em>风险提示</em><strong>${html(preset.risk)}</strong></span>
                </span>
                <span class="create-style-card-copy">${html(preset.personaSignal)}</span>
            </span>
        </label>
    `;
    const primaryOptions = creationStylePresets.slice(0, 4).map(renderOption).join("");
    const extraOptions = creationStylePresets.slice(4).map((preset, index) => renderOption(preset, index + 4)).join("");

    return `
        <div class="form-group create-style-group">
            <label>出道风格</label>
            <div class="create-style-grid">${primaryOptions}</div>
            <details class="create-style-more">
                <summary>更多风格</summary>
                <div class="create-style-grid create-style-grid-extra">${extraOptions}</div>
            </details>
            <div class="create-style-preview create-loadout-summary" id="creationStylePreview" data-action="create-loadout-summary" aria-live="polite"></div>
        </div>
    `;
}

function selectedCreationStylePreset() {
    const selectedKey = document.querySelector('input[name="creationStyle"]:checked')?.value;
    return creationStylePresets.find(preset => preset.key === selectedKey) || creationStylePresets[0];
}

function applyCreationStylePreset(key) {
    const preset = creationStylePresets.find(item => item.key === key) || creationStylePresets[0];
    const nameInput = document.getElementById("vupName");
    const personaInput = document.getElementById("persona");
    const selectedInput = [...document.querySelectorAll('input[name="creationStyle"]')]
        .find(input => input.value === preset.key);

    if (selectedInput) selectedInput.checked = true;
    if (nameInput) nameInput.value = preset.name;
    if (personaInput) personaInput.value = preset.persona;
    syncCreationStylePreview();
}

function creationLoadoutSummaryHtml(preset, name, persona) {
    return `
        <div class="create-loadout-summary-head">
            <strong>${html(name)}</strong>
            <span>${html(preset.label)}</span>
        </div>
        <div class="create-loadout-summary-grid">
            <span class="create-loadout-summary-item create-loadout-starter">
                <em>首日打法</em>
                <strong>${html(preset.firstMove || "杂谈暖场 + 粉丝群维护")}</strong>
            </span>
            <span class="create-loadout-summary-item">
                <em>适合路线</em>
                <strong>${html(preset.fitRoute || "稳健电子榨菜")}</strong>
            </span>
            <span class="create-loadout-summary-item">
                <em>风险提示</em>
                <strong>${html(preset.risk || "稳住节奏")}</strong>
            </span>
        </div>
        <p class="create-style-signal">${html(persona)}</p>
    `;
}

function syncCreationStylePreview() {
    const preset = selectedCreationStylePreset();
    const name = document.getElementById("vupName")?.value?.trim() || preset.name;
    const persona = document.getElementById("persona")?.value?.trim() || preset.persona;
    const preview = document.getElementById("creationStylePreview");
    const portrait = document.getElementById("portrait");
    const badge = document.getElementById("portraitBadge");

    if (preview) {
        preview.innerHTML = creationLoadoutSummaryHtml(preset, name, persona);
    }
    if (!state.vup && portrait) {
        portrait.src = `/gallery/${preset.portrait}`;
        portrait.alt = `${preset.label}预设立绘`;
    }
    if (!state.vup && badge) {
        badge.textContent = preset.label;
    }
}

function creationPersonaForSubmit() {
    const preset = selectedCreationStylePreset();
    const persona = document.getElementById("persona")?.value?.trim() || preset.persona;
    return `${persona}｜出道风格：${preset.label}｜${preset.personaSignal}`;
}

function renderStats() {
    const section = document.getElementById('statsSection');
    if (!state.vup) {
        section.innerHTML = '<p style="color: var(--text-muted); font-size: 13px;">创建VUP后查看属性</p>';
        return;
    }

    const v = state.vup;

    // 教程提示
    const tutorialHints = (v.tutorialHints || []).map(hint => `
        <div class="tutorial-hint" data-tutorial="${html(hint.key)}">${html(hint.text)}</div>
    `).join('');

    // 切片素材库存
    const materialStock = v.funProfile?.materialStock ?? 0;

    // 粉丝结构
    const fanStructure = v.fanStructure || {};

    // 平台口味
    const platformTrend = v.platformTrend;
    const platformTrendLabel = platformTrend?.label
        ? visibleTextOrFallback(platformTrend.label, "平台口味")
        : "平台口味";
    const platformTrendDescription = platformTrend?.description
        ? visibleTextOrFallback(platformTrend.description, "平台口味暂时稳定。")
        : "平台口味暂时稳定。";
    const platformTrendHtml = platformTrend ? `
        <div class="state-platform-trend desktop-state-platform-trend" style="margin-top: 12px; padding: 10px; background: var(--bg-secondary); border-radius: 8px;">
            <span style="font-size: 12px; color: var(--neon-purple);">${html(platformTrendLabel)}</span>
            <p style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">${html(platformTrendDescription)}</p>
        </div>
    ` : '';
    const openingAdvice = v.expectations?.openingAdvice;
    const openingPlan = v.expectations?.recommendedPlan;
    const openingStyle = v.expectations?.creationStyle;
    const openingContractLabel = v.expectations?.openingContractLabel;
    const openingRouteGoal = v.expectations?.openingRouteGoal;
    const openingFirstMoves = Array.isArray(v.expectations?.openingFirstMoves)
        ? v.expectations.openingFirstMoves.filter(Boolean)
        : [];
    const openingRiskBoundary = v.expectations?.openingRiskBoundary;
    const restartMemory = v.expectations?.previousEndingMemory;
    const restartGradeContract = v.expectations?.restartGradeContract || restartMemory?.gradeContract || {};
    const openingContractListHtml = openingFirstMoves.length ? `
        <div class="opening-contract-list">
            ${openingRouteGoal ? `<strong class="opening-route-goal">${html(openingRouteGoal)}</strong>` : ''}
            ${openingFirstMoves.map((move, index) => `
                <span class="opening-contract-step">
                    <b>${index + 1}</b>
                    ${html(move)}
                </span>
            `).join('')}
        </div>
    ` : openingRouteGoal ? `<strong class="opening-route-goal">${html(openingRouteGoal)}</strong>` : '';
    const openingRiskBoundaryHtml = openingRiskBoundary
        ? `<small class="opening-risk-boundary">${html(openingRiskBoundary)}</small>`
        : '';
    const restartObjective = restartMemory?.runObjective || v.expectations?.restartRunObjective || restartMemory?.nextGoal || '';
    const restartGradeLine = restartMemory?.gradeContractLine || restartGradeContract.summary || '';
    const restartBoundary = restartMemory?.boundary || v.expectations?.restartBoundary || '';
    const restartContractStrip = renderRestartContractStrip(restartGradeContract, restartMemory, restartBoundary);
    const restartMemoryHtml = restartMemory ? `
        <div class="opening-advice-card restart-memory-card">
            <div class="opening-advice-head">
                <span class="opening-advice-kicker">${html(restartMemory.legacyTag || '复活赛档案')}</span>
                <strong>上轮遗产</strong>
            </div>
            <span class="opening-advice-plan">${html(restartMemory.targetLabel || '复活赛目标')}</span>
            ${restartObjective ? `<strong class="restart-run-objective">${html(restartObjective)}</strong>` : ''}
            ${restartGradeLine ? `<strong class="restart-grade-contract">${html(restartGradeLine)}</strong>` : ''}
            <p>${html(restartMemory.summary || '')}</p>
            ${restartContractStrip}
            <small>${html(restartMemory.nextGoal || '')}</small>
            ${restartBoundary ? `<small class="restart-boundary-note">${html(restartBoundary)}</small>` : ''}
        </div>
    ` : '';
    const openingAdviceHtml = renderOpeningAdvice({
        openingAdvice,
        openingStyle,
        openingContractLabel,
        openingPlan,
        openingContractListHtml,
        openingRiskBoundaryHtml,
        restartMemoryHtml
    });
    const scoreGuideCardHtml = `
        <div class="score-guide-card" aria-label="评分读法">
            <div class="opening-advice-head">
                <span class="opening-advice-kicker">评分读法</span>
                <strong>行动信号会进结局</strong>
            </div>
            <div class="score-guide-grid">
                <button type="button" data-action="open-score-guide-route"><em>路线</em><strong>看推荐行动</strong></button>
                <button type="button" data-action="open-score-guide-evidence"><em>证据</em><strong>看图鉴情报</strong></button>
                <button type="button" data-action="open-score-guide-risk"><em>风险</em><strong>去拆债</strong></button>
            </div>
            <p>行动卡上的评分贡献预告，会在${runMaxDayLabel()}结局里变成总评、升档合约和复活赛目标。</p>
        </div>
    `;

    // Calculate deltas for animation
    const currentFans = v.fanStructure?.fans || 0;
    const currentReputation = v.opinion.reputation || 0;
    const currentPopularity = v.opinion.popularity || 0;
    const currentWatchHeat = v.opinion.watchHeat || 0;
    const currentMemeLevel = v.opinion.memeLevel || 0;
    const currentCommercialLevel = v.opinion.commercialLevel || 0;
    const currentTrueFans = v.fanStructure.trueFans || 0;
    const currentFunFans = v.fanStructure.funFans || 0;
    const currentUnicornFans = v.fanStructure.unicornFans || 0;
    const currentDdFans = v.fanStructure.ddFans || 0;
    const openDebts = activeDebts();
    const brain = operationalBrainSnapshot();
    const operationalBrainCardHtml = brain.wounded ? `
        <div class="operational-brain-card wounded" aria-label="运营脑状态">
            <div class="operational-brain-head">
                <span>运营脑</span>
                <strong>${html(brain.title)}</strong>
            </div>
            <p>${html(brain.detail)}</p>
            <div class="operational-brain-grid">
                <span><em>状态</em><strong>${html(brain.state)}</strong></span>
                <span><em>冷却</em><strong>${html(brain.cooldownLeft > 0 ? `${brain.cooldownLeft}天` : "无")}</strong></span>
                <span><em>恢复</em><strong>${html(brain.woundLeft > 0 ? `带伤恢复 ${brain.woundLeft}天` : "正常")}</strong></span>
            </div>
            <small>${html(brain.cooldownLeft > 0 || brain.wounded ? "重痕迹已经留下，今天要按伤口恢复节奏玩。" : brain.trajectory)}</small>
        </div>
    ` : `
        <div class="operational-brain-card" aria-label="运营脑状态">
            <div class="operational-brain-head">
                <span>运营脑</span>
                <strong>${html(brain.title)}</strong>
            </div>
            <p>${html(brain.detail)}</p>
            <div class="operational-brain-grid">
                <span><em>状态</em><strong>${html(brain.state)}</strong></span>
                <span><em>冷却</em><strong>${html(brain.cooldownLeft > 0 ? `${brain.cooldownLeft}天` : "无")}</strong></span>
                <span><em>恢复</em><strong>${html(brain.woundLeft > 0 ? `带伤恢复 ${brain.woundLeft}天` : "正常")}</strong></span>
            </div>
            <small>${html(brain.cooldownLeft > 0 || brain.wounded ? "重痕迹已经留下，今天要按伤口恢复节奏玩。" : brain.trajectory)}</small>
        </div>
    `;
    const routeKey = state.endingForecast?.likelyEndingType || v.currentRoute || 'UNKNOWN';
    const routeLabel = routeLabelFor(routeKey);
    const routeSignal = routeKey === 'UNKNOWN' ? '先定路线' : '持续加厚';
    const routeNumeric = routeKey === 'UNKNOWN' ? 0 : routeLabel.length;
    const debtSummary = cockpitDebtSummary(openDebts);
    const debtLine = shortDecisionText(debtSummary.line, "暂无旧账");

    function deltaStr(current, prev) {
        if (prev === null) return '';
        const diff = current - prev;
        if (diff === 0) return '';
        return diff > 0 ? `+${diff}` : `${diff}`;
    }

    function deltaClass(current, prev) {
        if (prev === null) return '';
        const diff = current - prev;
        if (diff === 0) return '';
        return diff > 0 ? 'positive' : 'negative';
    }

    function flashClass(current, prev) {
        if (prev === null) return '';
        const diff = current - prev;
        if (diff === 0) return '';
        return diff > 0 ? 'flash-positive' : 'flash-negative';
    }

    const compactStats = [
        { label: '粉丝', value: `${currentFans}`, sub: `核心${currentTrueFans} 乐子${currentFunFans}`, key: 'fans', current: currentFans },
        { label: '口碑', value: currentReputation, sub: currentReputation >= 70 ? '体面稳定' : currentReputation >= 45 ? '还能挽回' : '正在透支', key: 'reputation', current: currentReputation },
        { label: '围观', value: currentWatchHeat, sub: currentWatchHeat >= 70 ? '主会场感' : currentWatchHeat >= 35 ? '有人看热闹' : '低压', key: 'watchHeat', current: currentWatchHeat },
        { label: '旧账', value: openDebts.length, sub: debtLine, key: 'risk', current: openDebts.length },
        { label: '路线', value: routeLabel, sub: routeSignal, key: null, current: routeNumeric }
    ];
    const fullStats = [
        { label: '歌力', value: v.attributes.songPower, key: null },
        { label: '舞力', value: v.attributes.dancePower, key: null },
        { label: '杂谈', value: v.attributes.talkPower, key: null },
        { label: '梗力', value: v.attributes.memePower, key: null },
        { label: '企划', value: v.attributes.planPower, key: null },
        { label: '抗压', value: v.attributes.stressPower, key: null },
        { label: '曝光', value: currentPopularity, key: 'popularity', current: currentPopularity },
        { label: '口碑', value: currentReputation, key: 'reputation', current: currentReputation },
        { label: '围观', value: currentWatchHeat, key: 'watchHeat', current: currentWatchHeat },
        { label: '串味', value: currentMemeLevel, key: 'memeLevel', current: currentMemeLevel },
        { label: '商业化', value: currentCommercialLevel, key: 'commercialLevel', current: currentCommercialLevel },
        { label: '切片素材', value: materialStock, key: 'materialStock', current: materialStock },
        { label: '真爱粉', value: currentTrueFans, key: 'trueFans', current: currentTrueFans },
        { label: '乐子人', value: currentFunFans, key: 'funFans', current: currentFunFans },
        { label: '独角兽', value: currentUnicornFans, key: 'unicornFans', current: currentUnicornFans },
        { label: 'DD', value: currentDdFans, key: 'ddFans', current: currentDdFans }
    ];
    const compactStatCards = compactStats.map(stat => {
        const delta = stat.key ? deltaStr(stat.current, previousStats[stat.key]) : '';
        const deltaCls = stat.key ? deltaClass(stat.current, previousStats[stat.key]) : '';
        const flashCls = stat.key ? flashClass(stat.current, previousStats[stat.key]) : '';
        return `
        <div class="stat-pill ${flashCls}">
            <span>${html(stat.label)}</span>
            <strong>${html(stat.value)}${delta ? `<span class="stat-delta visible ${deltaCls}">${html(delta)}</span>` : ''}</strong>
            ${stat.sub ? `<small class="stat-sub">${html(stat.sub)}</small>` : ''}
        </div>
    `}).join('');
    const fullStatCards = fullStats.map(stat => {
        const delta = stat.key ? deltaStr(stat.current, previousStats[stat.key]) : '';
        const deltaCls = stat.key ? deltaClass(stat.current, previousStats[stat.key]) : '';
        const flashCls = stat.key ? flashClass(stat.current, previousStats[stat.key]) : '';
        return `
        <div class="stat-card ${flashCls}">
            <div class="stat-card-label">${html(stat.label)}</div>
            <div class="stat-card-value">${html(stat.value)}${delta ? `<span class="stat-delta visible ${deltaCls}">${html(delta)}</span>` : ''}</div>
        </div>
    `}).join('');

    section.innerHTML = `
        ${tutorialHints ? `<div class="tutorial-list">${tutorialHints}</div>` : ''}
        ${operationalBrainCardHtml}
        ${renderDebtCountdown()}
        ${state.vup && state.vup.moodLine ? '<div class="vup-mood-bubble"><span class="mood-icon">' + (state.vup.mood === 'HAPPY' ? '😊' : state.vup.mood === 'STRESSED' ? '😰' : state.vup.mood === 'TIRED' ? '😴' : state.vup.mood === 'EXCITED' ? '🤩' : state.vup.mood === 'ENERGIZED' ? '💪' : '😐') + '</span> ' + html(state.vup.moodLine) + '</div>' : ''}
        <div class="desktop-stat-brief" aria-label="核心状态">${compactStatCards}</div>
        ${openingAdviceHtml}
        ${restartMemoryHtml}
        ${scoreGuideCardHtml}
        ${platformTrendHtml}
        <details class="desktop-stat-details">
            <summary>完整属性面板</summary>
            <div class="stat-grid">${fullStatCards}</div>
        </details>
    `;

    // Animate main stat numbers from previous values
    const prevFans = previousStats.fans;
    const prevReputation = previousStats.reputation;
    const prevPopularity = previousStats.popularity;

    function animateStatCard(label, targetValue, prevValue) {
        if (prevValue === null || prevValue === undefined) return;
        const cards = section.querySelectorAll('.stat-card');
        for (const card of cards) {
            if (card.querySelector('.stat-card-label')?.textContent === label) {
                const valueEl = card.querySelector('.stat-card-value');
                if (valueEl) {
                    valueEl.childNodes[0].textContent = prevValue;
                    animateCountUp(valueEl, targetValue);
                }
                break;
            }
        }
    }

    function animateCompactStat(label, targetValue, prevValue) {
        if (prevValue === null || prevValue === undefined) return;
        const pills = section.querySelectorAll('.stat-pill');
        for (const pill of pills) {
            if (pill.querySelector('span')?.textContent === label) {
                const strongEl = pill.querySelector('strong');
                if (strongEl) {
                    strongEl.textContent = prevValue;
                    animateCountUp(strongEl, targetValue);
                }
                break;
            }
        }
    }

    animateCompactStat('粉丝', currentFans, prevFans);
    animateCompactStat('口碑', currentReputation, prevReputation);
    animateCompactStat('围观', currentWatchHeat, previousStats.watchHeat);
    animateCompactStat('旧账', openDebts.length, previousStats.risk);
    animateStatCard('曝光', currentPopularity, prevPopularity);
    animateStatCard('口碑', currentReputation, prevReputation);

    // 属性条增长发光效果：检测变化的属性并触发 glow
    function triggerStatGlow(label, current, prev) {
        if (prev === null || prev === undefined || current === prev) return;
        const cards = section.querySelectorAll('.stat-card');
        for (const card of cards) {
            if (card.querySelector('.stat-card-label')?.textContent === label) {
                ANIM.progressBarGlow(card);
                break;
            }
        }
        const pills = section.querySelectorAll('.stat-pill');
        for (const pill of pills) {
            if (pill.querySelector('span')?.textContent === label) {
                ANIM.progressBarGlow(pill);
                break;
            }
        }
    }
    triggerStatGlow('粉丝', currentFans, prevFans);
    triggerStatGlow('口碑', currentReputation, prevReputation);
    triggerStatGlow('围观', currentWatchHeat, previousStats.watchHeat);
    triggerStatGlow('旧账', openDebts.length, previousStats.risk);
    triggerStatGlow('路线', routeNumeric, previousStats.route);
    triggerStatGlow('曝光', currentPopularity, prevPopularity);

    // Update previous stats for next render
    previousStats.stamina = v.resources?.stamina || 0;
    previousStats.fans = currentFans;
    previousStats.materialStock = materialStock;
    previousStats.reputation = currentReputation;
    previousStats.risk = openDebts.length;
    previousStats.route = routeNumeric;
    previousStats.popularity = currentPopularity;
    previousStats.watchHeat = currentWatchHeat;
    previousStats.memeLevel = currentMemeLevel;
    previousStats.commercialLevel = currentCommercialLevel;
    previousStats.trueFans = currentTrueFans;
    previousStats.funFans = currentFunFans;
    previousStats.unicornFans = currentUnicornFans;
    previousStats.ddFans = currentDdFans;
}

// 网页文本转义
function html(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function jsString(value) {
    return JSON.stringify(String(value ?? ""));
}

function jsAttr(value) {
    return html(jsString(value));
}

function stableIdempotencyKey(scope, parts = []) {
    const normalizedParts = parts
        .map(part => String(part ?? "").trim())
        .filter(Boolean)
        .join(":");
    const cacheKey = normalizedParts ? `${scope}:${normalizedParts}` : scope;
    if (!state.idempotencyKeys[cacheKey]) {
        state.idempotencyKeys[cacheKey] = `${cacheKey}:${Date.now()}`;
    }
    return state.idempotencyKeys[cacheKey];
}

function clearStableIdempotencyKey(scope, parts = []) {
    const normalizedParts = parts
        .map(part => String(part ?? "").trim())
        .filter(Boolean)
        .join(":");
    const cacheKey = normalizedParts ? `${scope}:${normalizedParts}` : scope;
    delete state.idempotencyKeys[cacheKey];
}

function currentRunKeyParts(...parts) {
    return [
        state.vup?.id || state.vup?.name || "NO_VUP",
        state.session?.day || "NO_DAY",
        state.session?.phase || "NO_PHASE",
        ...parts
    ];
}

function clipboardText(value) {
    return encodeURIComponent(String(value ?? ""));
}

function percent(value) {
    const numeric = Number(value);
    if (!Number.isFinite(numeric)) return 0;
    return Math.max(0, Math.min(100, Math.round(numeric)));
}

function isLowHeightDesktopViewport() {
    return window.matchMedia?.('(max-height: 800px)').matches || window.innerHeight <= 800;
}

function actionHotkeyIgnoredTarget(target) {
    if (!(target instanceof Element)) return false;
    return target.closest('input, textarea, select, [contenteditable="true"]') || target.closest('.tutorial-overlay:not([hidden])');
}

function hotkeyDisplayText(key) {
    return key === "Enter" ? "回车" : String(key || "");
}

function handleActionHotkey(event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    if (state.busy  || state.session?.phase !== 'READY') return;
    if (actionHotkeyIgnoredTarget(event.target)) return;

    const key = String(event.key || "");
    if (!/^[1-9]$/.test(key)) return;

    const button = document.querySelector(`.daily-plan-board [data-action-hotkey="${key}"], .simple-decision-card [data-action-hotkey="${key}"], .ready-action-deck [data-action="quick-submit-action"][data-action-hotkey="${key}"]`);
    if (!button || button.disabled) return;

    event.preventDefault();
    const actionName = button.dataset.actionName
        || button.querySelector('strong')?.textContent?.trim()
        || button.querySelector('span')?.textContent?.trim()
        || '今日行动';
    setStatus(`快捷键 ${key}：${actionName}，开始营业。`, "info");
    button.click();
}

function handleStageChoiceHotkey(event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    if (state.busy ) return;
    if (actionHotkeyIgnoredTarget(event.target)) return;

    const key = String(event.key || "");
    if (!/^[1-3]$/.test(key)) return;

    const phase = state.session?.phase || "";
    const selector = phase === 'NEED_TITLE'
        ? `#titlePanel [data-action="choose-title"][data-stage-hotkey="${key}"]`
        : (phase === 'NEED_EVENT_CHOICE' || phase === 'NEED_INTERACTION_CHOICE')
            ? `#eventPanel [data-stage-hotkey="${key}"]`
            : "";
    if (!selector) return;

    const button = document.querySelector(selector);
    if (!button || button.disabled) return;

    event.preventDefault();
    const choiceName = button.querySelector('h4')?.textContent?.trim()
        || button.closest('.event-choice')?.querySelector('.event-choice-title')?.textContent?.trim()
        || '阶段选择';
    setStatus(`快捷键 ${key}：${choiceName}，提交阶段选择。`, "info");
    button.click();
}

function handleStageAdvanceHotkey(event) {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    if (state.busy ) return;
    if (actionHotkeyIgnoredTarget(event.target)) return;

    if (String(event.key || "") !== "Enter") return;

    const phase = state.session?.phase || "";
    const advance = phase === 'REPORT_READY'
        ? { selector: '#reportPanel [data-action="next-day"]', name: "读完日报，切到下一天" }
        : phase === 'ENDING_READY'
            ? { selector: '#endingPanel [data-action="restart"]', name: "复活赛开播" }
            : null;
    if (!advance) return;

    const button = document.querySelector(advance.selector);
    if (!button || button.disabled) return;

    event.preventDefault();
    const advanceName = advance.name;
    const advanceKeyLabel = hotkeyDisplayText("Enter");
    setStatus(`快捷键 ${advanceKeyLabel}：${advanceName}。`, "info");
    button.click();
}

const actionDecisionCopy = {
    TRAIN_SONG: { play: "歌势基本功", risk: "稳粉低压", flow: "等歌回周" },
    TRAIN_DANCE: { play: "舞势铺垫", risk: "起效偏慢", flow: "路线蓄力" },
    TRAIN_TALK: { play: "低压稳米线", risk: "连播会焦虑", flow: "稳住老粉" },
    STREAM_PLAN: { play: "开播爆点", risk: "标题债务", flow: "看标题池" },
    PUBLISH_VIDEO: { play: "投稿铺路线", risk: "消耗灵感", flow: "看推荐口味" },
    PUBLISH_CLIP: { play: "乐子人进场", risk: "回旋镖", flow: "抽象浓度" },
    FAN_GROUP_MAINTAIN: { play: "安抚老粉", risk: "起效偏慢", flow: "清小作文" },
    NPC_INTERACT: { play: "同台扩圈", risk: "独角兽审判", flow: "DD公交站" },
    REST: { play: "回血苟住", risk: "错过热度", flow: "保命回合" }
};

function actionDecisionTags(action) {
    const copy = actionDecisionCopy[action.actionType] || { play: "常规经营", risk: "看文案取舍", flow: "正常推进" };
    const platformLabel = state.vup?.platformTrend?.label;
    const previewText = `${action.effectPreview || ""} ${action.riskPreview || ""}`;
    const trendHit = Boolean(platformLabel && previewText.includes(platformLabel));
    const disabled = Boolean(action.disabledReason);
    const routeLabel = visibleTextOrFallback(action.routeBiasLabel, copy.play);
    const riskLabel = actionRiskLevelText(action.riskLevel, disabled ? "当前卡住" : copy.risk);
    const tempoLabel = visibleTextOrFallback(action.tempoHint, trendHit ? "版本顺风" : copy.flow);

    const tags = [
        { label: "路线", value: routeLabel, tone: "play" },
        { label: "风险", value: riskLabel, tone: disabled ? "blocked" : riskToneForAction(action.riskLevel) },
        { label: "节奏", value: trendHit ? `${tempoLabel}·顺风` : tempoLabel, tone: trendHit ? "trend-hit" : "flow" }
    ];
    if (actionMatchesStageObjective(action?.actionType)) {
        const nextStreak = Number(state.stageBriefing?.objectiveNextStreak || 0);
        tags.push({
            label: "委托",
            value: "今日目标",
            tone: "objective",
            streak: nextStreak
        });
    }
    const debtWindow = actionDebtWindowTag(action);
    if (debtWindow) {
        tags.push(debtWindow);
    }
    const endingGap = actionEndingGapRequirement(action);
    if (actionEndingGapCovered(action)) {
        tags.push({
            label: "缺口",
            value: endingGapActionLabel(endingGap),
            tone: "gap"
        });
    }
    const routeFocus = visibleTextOrFallback(action?.routeFocusHint, "");
    if (routeFocus) {
        const riskTone = routeFocus.includes("风险") || routeFocus.includes("偏移");
        const focusTone = routeFocus.includes("专精") || routeFocus.includes("加固") || routeFocus.includes("成型");
        tags.push({
            label: riskTone ? "转线" : focusTone ? "专精" : "路线",
            value: routeFocus.split("：")[0] || routeFocus,
            tone: riskTone ? "route-risk" : focusTone ? "route-focus" : "route-neutral"
        });
    }
    const comboLabel = actionComboLabelText(action, "");
    if (comboLabel) {
        const comboStatus = comboActionStatus(action);
        tags.push({
            label: comboStatus.tagLabel,
            value: comboLabel,
            tone: comboStatus.tone
        });
    }
    return tags;
}

function actionDebtWindowTag(action) {
    const text = `${action?.effectPreview || ""} ${action?.riskPreview || ""}`;
    if (!/旧账窗口|回流|事件位/.test(text)) {
        return null;
    }
    const defendable = /可处理|压到|延后/.test(text);
    return {
        label: "旧账",
        value: defendable ? "可拆" : "将回流",
        tone: defendable ? "debt-window" : "debt-risk"
    };
}

function orderedActionTags(tags) {
    const priority = {
        gap: 0,
        objective: 1,
        "debt-risk": 2,
        "debt-window": 2,
        combo: 3,
        "combo-risk": 3,
        "route-focus": 4,
        "route-risk": 4,
        "route-neutral": 4,
        "risk-high": 4,
        blocked: 4,
        risk: 5,
        "risk-low": 5,
        play: 6,
        "trend-hit": 7,
        flow: 8
    };
    return tags
        .map((tag, index) => ({ ...tag, index }))
        .sort((left, right) =>
            (priority[left.tone] ?? 50) - (priority[right.tone] ?? 50)
            || left.index - right.index)
        .map(({ index, ...tag }) => tag);
}

function renderActionDecisionTags(action) {
    return orderedActionTags(actionDecisionTags(action)).map(tag => `
        <span class="action-tag ${tag.tone}">
            <span>${html(tag.label)}</span>${html(tag.value)}
            ${tag.tone === "objective" && Number(tag.streak || 0) >= 2 ? `<small>${html(`命中后${Number(tag.streak)}连`)}</small>` : ""}
        </span>
    `).join('');
}

function routeAlignedPrimaryCue(action) {
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const cueByRoute = {
        SINGING_IDOL: {
            TRAIN_SONG: "练歌基本功",
            STREAM_PLAN: "歌回排档",
            PUBLISH_VIDEO: "歌势投稿复盘"
        },
        SLICE_SAINT: {
            PUBLISH_CLIP: "切片供货",
            PUBLISH_VIDEO: "素材投稿",
            STREAM_PLAN: "素材开播"
        },
        DANCE_MEME: {
            TRAIN_DANCE: "舞势练功",
            PUBLISH_CLIP: "舞切扩散",
            PUBLISH_VIDEO: "舞势投稿"
        },
        SOCIAL_COLLAB: {
            NPC_INTERACT: "联动排班",
            FAN_GROUP_MAINTAIN: "老粉接待",
            STREAM_PLAN: "同台预热"
        },
        DD_BUS_STOP: {
            NPC_INTERACT: "DD同台",
            FAN_GROUP_MAINTAIN: "站点调度",
            STREAM_PLAN: "联动开播"
        },
        CYBER_GIRLFRIEND: {
            TRAIN_TALK: "陪伴话术",
            FAN_GROUP_MAINTAIN: "亲密保温",
            NPC_INTERACT: "关系避雷"
        },
        BLACK_RED_MAIN_STAGE: {
            FAN_GROUP_MAINTAIN: "米线稳场",
            TRAIN_TALK: "回应排练",
            PUBLISH_CLIP: "开庭素材"
        },
        MAIN_STAGE_KING: {
            FAN_GROUP_MAINTAIN: "主会场稳粉",
            TRAIN_TALK: "控场练习",
            PUBLISH_CLIP: "议题切片"
        },
        GLORIOUS_GRADUATION: {
            FAN_GROUP_MAINTAIN: "毕业告别",
            REST: "体面收束",
            TRAIN_TALK: "告别排练"
        },
        ELECTRONIC_PICKLE: {
            TRAIN_TALK: "陪饭话术",
            FAN_GROUP_MAINTAIN: "老粉保温",
            STREAM_PLAN: "低压排档"
        }
    };
    return cueByRoute[likelyEnding]?.[action?.actionType] || "";
}

function primaryActionCueText(action) {
    const tags = actionDecisionTags(action);
    const routeCue = routeAlignedPrimaryCue(action);
    const play = routeCue || tags[0]?.value || "常规经营";
    const flow = tags[2]?.value || "正常推进";
    const combo = actionComboShortText(action);
    const reasonTail = (hasMeaningfulReadyRisk() || activeDebts().length > 0)
        ? "先处理旧账风险"
        : (firstWarnEndingRequirement() ? "补路线证据和结局缺口" : (routeCue ? "补路线证据" : "不锁路线"));
    if (combo) {
        return routeCue ? `${combo}，${play}，${flow}，${reasonTail}` : `${combo}，${flow}，${reasonTail}`;
    }
    return `${play}，${flow}，${reasonTail}`;
}

function actionDecisionReason(action, cardState) {
    if (cardState === 'blocked') {
        return actionDisabledText(action, true);
    }
    const tags = actionDecisionTags(action);
    const play = tags[0]?.value || "常规经营";
    const risk = tags[1]?.value || "风险待观察";
    const flow = tags[2]?.value || "正常推进";
    const reason = visibleTextOrFallback(action.recommendedReason, "");
    if (reason) {
        return cardState === 'recommended'
            ? `先点理由：${reason}`
            : `取舍：${reason}`;
    }
    return cardState === 'recommended'
        ? `先点理由：${play}，${flow}`
        : `取舍：${play}，${risk}`;
}

function actionDecisionSummary(action, cardState = "playable") {
    if (!action) {
        return {
            reason: "行动资料同步中，先等列表恢复。",
            benefit: "收获待同步",
            risk: "风险待观察",
            cost: "消耗待同步",
            route: "路线影响待同步"
        };
    }
    const tags = actionDecisionTags(action);
    const routeLabel = tags[0]?.value || visibleTextOrFallback(action.routeBiasLabel, "路线待定");
    const riskLabel = tags[1]?.value || actionRiskLevelText(action.riskLevel);
    const tempoLabel = tags[2]?.value || visibleTextOrFallback(action.tempoHint, "正常推进");
    const routeFocus = visibleTextOrFallback(action.routeFocusHint, "");
    return {
        reason: actionDecisionReason(action, cardState),
        benefit: actionPreviewText(action.effectPreview, "收获待观察"),
        risk: action.riskPreview ? actionPreviewText(action.riskPreview, "风险待观察") : riskLabel,
        cost: actionResourceTradeoffText(action),
        route: routeFocus ? `${routeLabel} · ${routeFocus}` : `${routeLabel} · ${tempoLabel}`
    };
}

function actionTradeoffMetrics(action) {
    const text = `${action?.effectPreview || ""} ${action?.riskPreview || ""} ${action?.recommendedReason || ""} ${action?.routeFocusHint || ""}`;
    const type = action?.actionType || "";
    let payoff = {
        TRAIN_SONG: 48,
        TRAIN_DANCE: 46,
        TRAIN_TALK: 42,
        STREAM_PLAN: 78,
        PUBLISH_VIDEO: 62,
        PUBLISH_CLIP: 76,
        FAN_GROUP_MAINTAIN: 40,
        NPC_INTERACT: 68,
        REST: 24
    }[type] || 45;
    let danger = {
        TRAIN_SONG: 18,
        TRAIN_DANCE: 24,
        TRAIN_TALK: 30,
        STREAM_PLAN: 78,
        PUBLISH_VIDEO: 42,
        PUBLISH_CLIP: 74,
        FAN_GROUP_MAINTAIN: 22,
        NPC_INTERACT: 66,
        REST: 20
    }[type] || 35;
    let pressure = Math.min(100, Number(action?.staminaCost || 0) * 18);
    let commitment = action?.routeBiasType && action.routeBiasType !== "UNKNOWN" ? 56 : 12;

    if (/平台顺风|今日运势|阶段委托|收官押线|组合技|连招/.test(text)) payoff += 12;
    if (/债务|旧账|回旋|独角兽|口碑|压力|风险|开庭|逆风/.test(text)) danger += 14;
    if (new RegExp(`灵感-1|素材-1|${BUDGET_RESOURCE_PATTERN}|预算|消耗`).test(text)) pressure += 16;
    if (/路线专精|路线加固|路线成型|补路线证据|结局门槛/.test(text)) commitment += 18;
    if (/转线风险|路线偏移|双线试探/.test(text)) commitment += 10;
    if (action?.comboKey) payoff += 8;
    if (action?.disabledReason) {
        payoff = Math.max(8, payoff - 24);
        pressure = 100;
    }
    return [
        { key: "payoff", label: "回报", value: clampMetric(payoff), tone: payoff >= danger ? "good" : "neutral" },
        { key: "danger", label: "风险", value: clampMetric(danger), tone: danger >= 70 ? "danger" : danger >= 45 ? "warn" : "safe" },
        { key: "pressure", label: "资源", value: clampMetric(pressure), tone: pressure >= 70 ? "warn" : "neutral" },
        { key: "commitment", label: "定线", value: clampMetric(commitment), tone: commitment >= 70 ? "good" : "neutral" }
    ];
}

function clampMetric(value) {
    return Math.max(0, Math.min(100, Math.round(Number(value) || 0)));
}

function renderActionTradeoffMeter(action) {
    const metrics = actionTradeoffMetrics(action);
    return `
        <div class="action-tradeoff-meter" aria-label="行动取舍仪表">
            ${metrics.map(metric => `
                <span class="action-tradeoff-item ${html(metric.tone)}" style="--meter:${html(metric.value)}%">
                    <em>${html(metric.label)}</em>
                    <i aria-hidden="true"><b></b></i>
                    <strong>${html(metric.value)}</strong>
                </span>
            `).join('')}
        </div>
    `;
}

function actionScoreSignals(action) {
    if (!action) return [];
    const signals = [];
    const text = `${action.effectPreview || ""} ${action.riskPreview || ""} ${action.recommendedReason || ""} ${action.routeFocusHint || ""} ${action.comboLabel || ""} ${action.comboHint || ""}`;
    if (actionEndingGapCovered(action)
            || primaryRouteActionTypes(state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN").includes(action.actionType)) {
        signals.push({ key: "route", label: "路线证据", tone: "route" });
    }
    if (action.comboKey || /连招|组合技/.test(text)) {
        signals.push({ key: "combo", label: "连招证据", tone: "combo" });
    }
    if (actionMatchesStageObjective(action.actionType) || /阶段委托/.test(text)) {
        signals.push({ key: "objective", label: "委托证据", tone: "objective" });
    }
    if (hasMeaningfulReadyRisk() && ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'].includes(action.actionType)) {
        signals.push({ key: "recovery", label: "风险恢复", tone: "recovery" });
    }
    if (/疲劳|重复|连续/.test(text)) {
        signals.push({ key: "fatigue", label: "疲劳风险", tone: "fatigue" });
    }
    if (!signals.length && action.enabled) {
        signals.push({ key: "tempo", label: "经营铺垫", tone: "neutral" });
    }
    return signals.slice(0, 4);
}

function primaryActionScoreSignal(action) {
    return actionScoreSignals(action)[0]?.label || "评分待观察";
}

function renderActionScoreSignals(action) {
    const signals = actionScoreSignals(action);
    if (!signals.length) return "";
    return `
        <div class="action-score-signals" aria-label="结局评分贡献预告">
            ${signals.map(signal => `
                <span class="${html(signal.tone)}">
                    <em>${html(signal.label)}</em>
                </span>
            `).join("")}
        </div>
    `;
}

function actionResourceTradeoffText(action) {
    const stamina = Number(action?.staminaCost || 0);
    const staminaText = stamina > 0 ? `体力-${stamina}` : "体力+";
    const requirementText = String(action?.requirement || "");
    const coinMatch = requirementText.match(new RegExp(`${BUDGET_RESOURCE_PATTERN}>=(\\d+)`));
    const coinText = coinMatch ? ` / 预算-${coinMatch[1]}` : "";
    switch (action?.actionType) {
        case "TRAIN_SONG":
            return `${staminaText} / 歌力+ / 低风险`;
        case "TRAIN_DANCE":
            return `${staminaText} / 舞力+ / 素材苗头`;
        case "TRAIN_TALK":
            return `${staminaText} / 灵感+ / 压旧账`;
        case "STREAM_PLAN":
            return `${staminaText} / 标题分叉 / 可能留债`;
        case "PUBLISH_VIDEO":
            return `${staminaText} / 灵感-1 / 素材+${coinText}`;
        case "PUBLISH_CLIP":
            return `${staminaText} / 素材-1 / 热度+${coinText}`;
        case "FAN_GROUP_MAINTAIN":
            return `${staminaText} / 素材+ / 稳关系`;
        case "NPC_INTERACT":
            return `${staminaText} / DD+ / 关系压力${coinText}`;
        case "REST":
            return "主收获- / 体力+ / 延旧账";
        default:
            return stamina > 0 ? `体力-${stamina}` : "资源待同步";
    }
}

function actionResourceChipText(action) {
    return actionResourceTradeoffText(action)
        .replace(/\s+/g, "")
        .split("/")
        .slice(0, 2)
        .join(" / ");
}

function renderActionDecisionSummary(summary, compact = false) {
    const items = [
        ["推荐理由", summary.reason, "reason"],
        ["收获", summary.benefit, "benefit"],
        ["风险", summary.risk, "risk"],
        ["资源取舍", summary.cost, "cost"],
        ["路线影响", summary.route, "route"]
    ];
    const impactHtml = metricImpactExplanations(summary.benefit).slice(0, 3).map(renderMetricImpactLine).join('');
    return `
        <div class="action-decision-summary ${compact ? "compact" : ""}" aria-label="行动决策解释">
            ${items.map(([label, value, tone]) => `
                <span class="action-decision-item ${tone}">
                    <em>${html(label)}</em>
                    <strong>${html(value)}</strong>
                </span>
            `).join('')}
            ${impactHtml ? `<div class="action-impact-explain">${impactHtml}</div>` : ''}
        </div>
    `;
}

function actionRiskLevelText(level, fallback = "风险待观察") {
    const labels = {
        LOW: "低",
        MEDIUM: "中",
        HIGH: "高"
    };
    return labels[level] ? `${labels[level]}风险` : fallback;
}

function toggleActionDetail(actionType) {
    const card = document.querySelector(`.action-item[data-action-type="${actionType}"]`);
    if (!card) return;
    const wasExpanded = card.classList.contains('expanded');
    // Collapse all other cards first
    document.querySelectorAll('.action-item.expanded').forEach(el => {
        if (el !== card) el.classList.remove('expanded');
    });
    card.classList.toggle('expanded');
}

function riskToneForAction(level) {
    if (level === 'HIGH') return 'risk-high';
    if (level === 'LOW') return 'risk-low';
    return 'risk';
}

function actionSignalText(action, cardState = 'playable') {
    if (!action) {
        return "命中信号：行动资料同步中。";
    }
    if (cardState === 'blocked') {
        if (action.disabledReason === 'INSUFFICIENT_MATERIAL_STOCK') {
            return "命中信号：素材库为0，先补一条可剪素材，下一天再切才会进结局证据。";
        }
        return `命中信号：${actionDisabledText(action, true) || "当前条件不足。"}`;
    }

    const comboHint = actionComboHintText(action, "");
    if (comboHint) {
        return comboHint;
    }

    const endingGap = firstWarnEndingRequirement();
    if (actionCoversEndingGap(action, endingGap)) {
        return `命中信号：当前结局缺口是${endingGapSignalLabel(endingGap)}，这手能${endingGapActionLabel(endingGap)}。`;
    }

    const actionType = action.actionType;
    const trendText = [
        state.stageBriefing?.actionHint,
        state.stageBriefing?.nextTrendHint,
        state.vup?.platformTrend?.label,
        action.effectPreview,
        action.riskPreview
    ].join(" ");
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const likelyTitle = endingForecastFinalTitleText(state.endingForecast, routeLabelFor(likelyEnding));
    const routeFits = {
        UNKNOWN: ['TRAIN_TALK', 'PUBLISH_VIDEO', 'STREAM_PLAN', 'FAN_GROUP_MAINTAIN'],
        ELECTRONIC_PICKLE: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN'],
        SINGING_IDOL: ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO'],
        SLICE_SAINT: ['PUBLISH_VIDEO', 'PUBLISH_CLIP', 'STREAM_PLAN'],
        DANCE_MEME: ['TRAIN_DANCE', 'PUBLISH_CLIP', 'PUBLISH_VIDEO'],
        SOCIAL_COLLAB: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN'],
        DD_BUS_STOP: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'TRAIN_TALK'],
        CYBER_GIRLFRIEND: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'NPC_INTERACT'],
        BLACK_RED_MAIN_STAGE: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'PUBLISH_CLIP'],
        MAIN_STAGE_KING: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'PUBLISH_CLIP'],
        GLORIOUS_GRADUATION: ['FAN_GROUP_MAINTAIN', 'REST', 'TRAIN_TALK']
    };

    if (readyGoalRiskTone() === 'hot' && ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'].includes(actionType)) {
        return "命中信号：风险面板在亮红灯，先稳口碑和旧账。";
    }
    if (/歌回扶持|练歌|歌回/.test(trendText) && ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO'].includes(actionType)) {
        return "命中信号：平台口味偏歌回，今天补歌势证据。";
    }
    if (/抽象出圈|切片|素材|整活/.test(trendText) && ['PUBLISH_VIDEO', 'PUBLISH_CLIP', 'STREAM_PLAN'].includes(actionType)) {
        return "命中信号：平台口味偏素材，优先给切片组供货。";
    }
    if (/商业复审|粉丝群|排班|稳/.test(trendText) && ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'].includes(actionType)) {
        return "命中信号：阶段提示偏稳健，先保住老粉体感。";
    }
    const leadRoute = state.audienceExpectation?.lead?.routeType;
    if (leadRoute && (routeFits[leadRoute] || []).includes(actionType)) {
        return `命中信号：观众期待偏${routeLabelFor(leadRoute)}，继续补同类证据。`;
    }
    if ((routeFits[likelyEnding] || []).includes(actionType)) {
        return `命中信号：结局预演偏${likelyTitle}，这步能补路线证据。`;
    }
    if (actionType === 'PUBLISH_CLIP' && (state.memeLifecycle?.items || []).some(item => ['HEAVY_REPEAT', 'BOOMERANG'].includes(item.stage))) {
        return "命中信号：梗阶段偏疲劳，切片前确认有新素材。";
    }

    return `命中信号：${actionDecisionTags(action)[0]?.value || "常规经营"}，按体力和资源取舍。`;
}

function actionPressureText(action) {
    const stateText = visibleTextOrFallback(action?.pressureState, "");
    if (!stateText || stateText === "松弛") {
        return "";
    }
    const lockGroup = visibleTextOrFallback(action?.pressureLockGroupLabel, "当前动作线");
    const replacementLabel = visibleTextOrFallback(action?.pressureReplacementActionLabel, "");
    return [stateText, lockGroup, replacementLabel ? `替代 ${replacementLabel}` : ""].filter(Boolean).join(" · ");
}

function actionPressureDetail(action) {
    const stateText = visibleTextOrFallback(action?.pressureState, "");
    if (!stateText || stateText === "松弛") {
        return "";
    }
    const lockGroup = visibleTextOrFallback(action?.pressureLockGroupLabel, "当前动作线");
    const replacementLabel = visibleTextOrFallback(action?.pressureReplacementActionLabel, "");
    const hint = localizedVisibleTextOrFallback(action?.pressureHint, "");
    const cooldownLeft = Number(action?.pressureCooldownLeft || 0);
    const woundLeft = Number(action?.pressureWoundLeft || 0);
    const replacementHint = localizedVisibleTextOrFallback(action?.pressureReplacementHint, "");
    const staminaCost = Number(action?.pressureReplacementStaminaCost || 0);
    const inspirationCost = Number(action?.pressureReplacementInspirationCost || 0);
    const reputationCost = Number(action?.pressureReplacementReputationCost || 0);
    const costParts = [];
    if (staminaCost > 0) costParts.push(`体力-${staminaCost}`);
    if (inspirationCost > 0) costParts.push(`灵感-${inspirationCost}`);
    if (reputationCost > 0) costParts.push(`口碑-${reputationCost}`);
    const costText = costParts.length ? costParts.join(" ") : "无代价";
    const locked = Boolean(action?.disabledReason === 'OPERATIONAL_PRESSURE_LOCKED');
    return `
        <div class="action-pressure-summary ${html(pressureToneForState(stateText))}">
            <span class="action-pressure-chip state"><em>压力</em><strong>${html(stateText)}</strong></span>
            <span class="action-pressure-chip lock"><em>锁组</em><strong>${html(lockGroup)}</strong></span>
            <span class="action-pressure-chip replacement"><em>${cooldownLeft > 0 ? "重痕迹" : "替代"}</em><strong>${html(cooldownLeft > 0 ? `${cooldownLeft}天` : replacementLabel || "待定")}</strong></span>
            ${woundLeft > 0 ? `<span class="action-pressure-chip wound"><em>带伤恢复</em><strong>${html(`${woundLeft}天`)}</strong></span>` : ""}
            ${hint ? `<small>${html(hint)}</small>` : ""}
        </div>
        ${locked && replacementLabel ? `
        <div class="action-pressure-alternative">
            <span class="action-pressure-alt-label">替代方案</span>
            <strong>${html(replacementLabel)}</strong>
            ${replacementHint ? `<small>${html(replacementHint)}</small>` : ""}
            <span class="action-pressure-alt-cost">${html(costText)}</span>
        </div>
        ` : ""}
    `;
}

function actionNameText(action) {
    return visibleTextOrFallback(action?.name, actionLabelFor(action?.actionType));
}

function actionComboLabelText(action, fallback = "") {
    return localizedVisibleTextOrFallback(action?.comboLabel, fallback);
}

function actionComboHintText(action, fallback = "") {
    return localizedVisibleTextOrFallback(action?.comboHint, fallback);
}

function actionComboShortText(action) {
    const label = actionComboLabelText(action, "");
    return label ? `连招：${label}` : "";
}

function actionPreviewText(value, fallback) {
    return localizedVisibleTextOrFallback(value, fallback);
}

function stageObjectiveActionTypes() {
    const items = Array.isArray(state.stageBriefing?.objectiveItems)
        ? state.stageBriefing.objectiveItems
        : [];
    const itemTypes = items
        .filter(item => !item?.achieved)
        .map(item => String(item?.recommendedActionType || "").trim())
        .filter(Boolean);
    if (itemTypes.length) {
        return [...new Set(itemTypes)];
    }
    if (items.length) {
        return [];
    }
    const primary = String(state.stageBriefing?.objectiveActionType || "").trim();
    return primary ? [primary] : [];
}

function actionMatchesStageObjective(actionType) {
    const type = String(actionType || "").trim();
    return Boolean(type && stageObjectiveActionTypes().includes(type));
}

function quickActionTypeCandidates() {
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const candidatesByEnding = {
        UNKNOWN: ['TRAIN_TALK', 'PUBLISH_VIDEO', 'STREAM_PLAN', 'FAN_GROUP_MAINTAIN', 'REST'],
        ELECTRONIC_PICKLE: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN', 'PUBLISH_VIDEO', 'REST'],
        SINGING_IDOL: ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO', 'TRAIN_TALK', 'REST'],
        SLICE_SAINT: ['PUBLISH_CLIP', 'PUBLISH_VIDEO', 'STREAM_PLAN', 'TRAIN_TALK', 'REST'],
        DANCE_MEME: ['TRAIN_DANCE', 'PUBLISH_CLIP', 'STREAM_PLAN', 'PUBLISH_VIDEO', 'REST'],
        SOCIAL_COLLAB: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN', 'TRAIN_TALK', 'REST'],
        DD_BUS_STOP: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN', 'TRAIN_TALK', 'REST'],
        CYBER_GIRLFRIEND: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN', 'NPC_INTERACT', 'REST'],
        BLACK_RED_MAIN_STAGE: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'STREAM_PLAN', 'PUBLISH_CLIP', 'REST'],
        MAIN_STAGE_KING: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'PUBLISH_CLIP', 'STREAM_PLAN', 'REST'],
        GLORIOUS_GRADUATION: ['FAN_GROUP_MAINTAIN', 'REST', 'TRAIN_TALK', 'PUBLISH_VIDEO', 'STREAM_PLAN']
    };
    const base = candidatesByEnding[likelyEnding] || candidatesByEnding.UNKNOWN;
    const routeFirstTypes = hasMeaningfulReadyRisk() ? [] : primaryRouteActionTypes(likelyEnding);
    const trendText = [
        state.stageBriefing?.actionHint,
        state.stageBriefing?.nextTrendHint,
        state.vup?.platformTrend?.label
    ].join(" ");
    const trendBoosts = /歌回扶持|练歌|歌回/.test(trendText)
        ? ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO']
        : /抽象出圈|切片|素材|整活/.test(trendText)
            ? ['PUBLISH_VIDEO', 'PUBLISH_CLIP', 'STREAM_PLAN']
            : /商业复审|粉丝群|排班|稳/.test(trendText)
                ? ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST']
                : [];
    const riskBoosts = hasMeaningfulReadyRisk()
        ? ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST']
        : [];
    const endingGapTypes = endingGapActionTypes(firstWarnEndingRequirement(), likelyEnding);
    const backendGapTypes = (state.actions || [])
        .filter(action => actionEndingGapCovered(action))
        .map(action => action.actionType)
        .filter(Boolean);
    const objectiveAction = stageObjectiveActionTypes();
    return [...new Set([...objectiveAction, ...backendGapTypes, ...endingGapTypes, ...riskBoosts, ...routeFirstTypes, ...trendBoosts, ...base, 'PUBLISH_CLIP', 'NPC_INTERACT', 'STREAM_PLAN', 'REST'])];
}

function primaryRouteActionTypes(likelyEnding) {
    const primaryByEnding = {
        SINGING_IDOL: ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO'],
        SLICE_SAINT: ['PUBLISH_CLIP', 'PUBLISH_VIDEO', 'STREAM_PLAN'],
        DANCE_MEME: ['TRAIN_DANCE', 'PUBLISH_CLIP', 'PUBLISH_VIDEO'],
        SOCIAL_COLLAB: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN'],
        DD_BUS_STOP: ['NPC_INTERACT', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN'],
        CYBER_GIRLFRIEND: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'NPC_INTERACT'],
        BLACK_RED_MAIN_STAGE: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'PUBLISH_CLIP'],
        MAIN_STAGE_KING: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'PUBLISH_CLIP'],
        GLORIOUS_GRADUATION: ['FAN_GROUP_MAINTAIN', 'REST', 'TRAIN_TALK'],
        ELECTRONIC_PICKLE: ['TRAIN_TALK', 'FAN_GROUP_MAINTAIN', 'STREAM_PLAN']
    };
    return primaryByEnding[likelyEnding] || [];
}

function endingRequirementKey(requirement) {
    return String(requirement?.key || "").toUpperCase();
}

function endingGapActionTypes(requirement, likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN") {
    if (Array.isArray(requirement?.suggestedActionTypes) && requirement.suggestedActionTypes.length) {
        return requirement.suggestedActionTypes.filter(Boolean);
    }
    const key = endingRequirementKey(requirement);
    const routeTypes = primaryRouteActionTypes(likelyEnding);
    const fanTypes = [...new Set(['STREAM_PLAN', 'PUBLISH_VIDEO', 'PUBLISH_CLIP', ...routeTypes])];
    const defenseTypes = ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'];
    switch (key) {
        case "ROUTE":
        case "RECENT_EVIDENCE":
        case "ROUTE_SPECIFIC":
            return routeTypes.length ? routeTypes : ['PUBLISH_VIDEO', 'STREAM_PLAN', 'TRAIN_TALK'];
        case "FANS":
            return fanTypes;
        case "DEBT_RISK":
            return defenseTypes;
        case "RULE_TRACE": {
            const line = endingForecastRequirementLineText(requirement, "");
            if (/粉丝|基础门槛/.test(line)) {
                return fanTypes;
            }
            if (/债务|未结|高压|风险/.test(line)) {
                return defenseTypes;
            }
            return routeTypes.length ? routeTypes : ['PUBLISH_VIDEO', 'STREAM_PLAN', 'FAN_GROUP_MAINTAIN'];
        }
        default:
            return [];
    }
}

function endingGapActionLabel(requirement) {
    const labels = {
        ROUTE: "补路线",
        RECENT_EVIDENCE: "补近证",
        FANS: "补粉丝",
        DEBT_RISK: "拆风险",
        ROUTE_SPECIFIC: "补专项",
        RULE_TRACE: "补规则"
    };
    return labels[endingRequirementKey(requirement)] || "补短板";
}

function endingGapSignalLabel(requirement) {
    return endingForecastRequirementLabelText(requirement, "结局门槛");
}

function endingGapProgressText(requirement) {
    const current = Number(requirement?.currentValue);
    const target = Number(requirement?.targetValue);
    const missing = Number(requirement?.missingValue);
    if (!Number.isFinite(current) || !Number.isFinite(target) || target <= 0) {
        return "";
    }
    return Number.isFinite(missing) && missing > 0
        ? `还差${Math.round(missing)} · ${Math.round(current)}/${Math.round(target)}`
        : `已达成 · ${Math.round(current)}/${Math.round(target)}`;
}

function endingGapCoachLine(requirement = firstWarnEndingRequirement(), options = {}) {
    if (!requirement) {
        return options.clearText || "结局门槛暂稳，明天继续补同一路线证据。";
    }
    const likelyEnding = options.likelyEnding || state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    const actionType = endingGapActionTypes(requirement, likelyEnding)[0] || "";
    const actionName = actionType ? actionLabelFor(actionType) : endingGapActionLabel(requirement);
    const progress = endingGapProgressText(requirement);
    const progressLine = progress ? `，${progress}` : "";
    return `${endingGapActionLabel(requirement)}：先点${actionName}，${endingGapSignalLabel(requirement)}${progressLine}。`;
}

function renderEndingGapCoachCard(requirement = firstWarnEndingRequirement(), options = {}) {
    const line = endingGapCoachLine(requirement, options);
    const actionType = requirement
        ? endingGapActionTypes(requirement, options.likelyEnding || state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN")[0] || ""
        : "";
    return `
        <div class="ending-gap-coach ${requirement ? "warn" : "clear"}" aria-label="结局差几步">
            <span>${html(requirement ? "结局差距" : "结局状态")}</span>
            <strong>${html(requirement ? endingGapSignalLabel(requirement) : "门槛暂稳")}</strong>
            <small>${html(line)}</small>
            ${actionType ? `<em>下一手：${html(actionLabelFor(actionType))}</em>` : ""}
        </div>
    `;
}

function actionTypeCoversEndingGap(actionType, requirement, likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN") {
    if (!actionType || !requirement) return false;
    return endingGapActionTypes(requirement, likelyEnding).includes(actionType);
}

function actionEndingGapPreview(action) {
    const preview = action?.endingGapPreview;
    return preview?.requirementKey ? preview : null;
}

function actionEndingGapRequirement(action) {
    const preview = actionEndingGapPreview(action);
    if (!preview) return firstWarnEndingRequirement();
    return {
        key: preview.requirementKey,
        label: preview.requirementLabel,
        status: "WARN",
        line: preview.requirementLine,
        currentValue: preview.currentValue,
        targetValue: preview.targetValue,
        missingValue: preview.missingValue,
        targetRoute: preview.targetRoute,
        suggestedActionTypes: preview.suggestedActionTypes || []
    };
}

function actionEndingGapCovered(action) {
    const preview = actionEndingGapPreview(action);
    return preview ? preview.covered === true : actionCoversEndingGap(action, firstWarnEndingRequirement());
}

function actionCoversEndingGap(action, requirement = firstWarnEndingRequirement()) {
    if (!action || !requirement || !action.enabled) return false;
    const preview = actionEndingGapPreview(action);
    if (preview) return preview.covered === true;
    if (action.actionType === 'PUBLISH_CLIP' && action.disabledReason === 'INSUFFICIENT_MATERIAL_STOCK') {
        return false;
    }
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    return actionTypeCoversEndingGap(action.actionType, requirement, likelyEnding);
}

function endingGapSnapshot(actionType) {
    const requirement = firstWarnEndingRequirement();
    if (!requirement) return null;
    return {
        actionType,
        likelyEnding: state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN",
        requirement: {
            key: requirement.key,
            label: endingForecastRequirementLabelText(requirement),
            status: requirement.status,
            line: endingForecastRequirementLineText(requirement),
            currentValue: requirement.currentValue,
            targetValue: requirement.targetValue,
            missingValue: requirement.missingValue,
            targetRoute: requirement.targetRoute,
            suggestedActionTypes: requirement.suggestedActionTypes
        }
    };
}

function endingGapImpactSnapshot(result) {
    const impact = result?.evidenceRef?.endingGapImpact;
    if (!impact?.requirementKey) return null;
    return {
        actionType: impact.actionType || result?.actionType,
        actionRouteType: impact.actionRouteType || "",
        likelyEnding: impact.likelyEndingType || state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN",
        covered: impact.covered === true,
        requirement: {
            key: impact.requirementKey,
            label: impact.requirementLabel,
            status: "WARN",
            line: impact.requirementLine,
            currentValue: impact.currentValue,
            targetValue: impact.targetValue,
            missingValue: impact.missingValue,
            targetRoute: impact.targetRoute,
            suggestedActionTypes: impact.suggestedActionTypes || []
        }
    };
}

function hasMeaningfulReadyRisk() {
    const riskText = [
        state.endingForecast?.riskLine,
        state.stageBriefing?.riskSnapshot,
        ...(state.riskTools || []).map(tool => tool?.enabled ? tool?.effectPreview : "")
    ].join(" ");
    return /未结清债务[1-9]\d*笔|债务[1-9]\d*笔|高压|最高风险|风险等级[1-9]|先拆/.test(riskText)
        && !/暂无未结清债务|不用加班/.test(riskText);
}

function playableActionScore(action, preferredTypes, index) {
    let score = 100 - index;
    if (!action?.enabled) score -= 160;
    if (action?.disabledReason === 'INSUFFICIENT_MATERIAL_STOCK') score -= 30;
    if (action?.comboKey) score += 58;
    if (action?.comboKey) {
        const comboStatus = comboActionStatus(action);
        if (comboStatus.kind === "unlock") {
            score += 16;
        } else if (comboStatus.kind === "replay") {
            score -= 6;
        } else if (comboStatus.kind === "risk") {
            score += hasMeaningfulReadyRisk() ? -24 : 12;
        } else {
            score -= 12;
        }
    }
    const text = [
        action?.effectPreview,
        action?.riskPreview,
        action?.comboLabel,
        action?.comboHint,
        actionDecisionTags(action || {})[0]?.value,
        state.stageBriefing?.actionHint,
        state.endingForecast?.sprintHint
    ].join(" ");
    if (/债务|米线|口碑|降温|稳住|复盘|老粉/.test(text) && ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'].includes(action?.actionType)) {
        score += 26;
    }
    if (/期待锁定|转型摩擦|期待落差/.test(text)) {
        score -= 34;
    }
    const likelyEnding = state.endingForecast?.likelyEndingType || state.vup?.currentRoute || "UNKNOWN";
    if (!hasMeaningfulReadyRisk() && primaryRouteActionTypes(likelyEnding).includes(action?.actionType)) {
        score += 36;
    }
    if (actionEndingGapCovered(action)) {
        score += 46;
    }
    if (/素材|切片|投稿|新料/.test(text) && ['PUBLISH_VIDEO', 'PUBLISH_CLIP'].includes(action?.actionType)) {
        score += 22;
    }
    if (/练歌|歌势|歌回/.test(text) && ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO'].includes(action?.actionType)) {
        score += 18;
    }
    if (/同台|联动|DD/.test(text) && action?.actionType === 'NPC_INTERACT') {
        score += 16;
    }
    if (actionMatchesStageObjective(action?.actionType)) {
        score += 42;
    }
    if (/主推|补路线证据|补上下文|先稳住|先补|今天优先/.test(action?.recommendedReason || "")) {
        score += 28;
    }
    if (/缺切片素材|灵感不足/.test(action?.recommendedReason || "")) {
        score += 20;
    }
    if (/歌势路线|切片路线|同台|主会场|陪伴路线|毕业/.test(action?.recommendedReason || "")) {
        score += 18;
    }
    return score;
}

function selectQuickActions(actions) {
    const preferredTypes = quickActionTypeCandidates();
    const preferred = preferredTypes
        .map(type => actions.find(action => action.actionType === type))
        .filter(Boolean);
    preferred.sort((left, right) =>
        playableActionScore(right, preferredTypes, preferredTypes.indexOf(right.actionType))
        - playableActionScore(left, preferredTypes, preferredTypes.indexOf(left.actionType)));
    const materialBlockedClip = preferred.find(action => action.actionType === 'PUBLISH_CLIP'
        && action.disabledReason === 'INSUFFICIENT_MATERIAL_STOCK');
    const enabledPreferred = preferred.filter(action => action.enabled && action !== materialBlockedClip);
    const disabledPreferred = preferred.filter(action => !action.enabled && action !== materialBlockedClip);
    const quickActions = enabledPreferred
        .slice(0, materialBlockedClip ? 3 : 4);
    if (materialBlockedClip) {
        quickActions.push(materialBlockedClip);
    }
    if (quickActions.length < 4) {
        quickActions.push(...disabledPreferred.slice(0, 4 - quickActions.length));
    }
    return quickActions.slice(0, 4);
}

function selectPrimaryReadyAction(actions, quickActions) {
    const pool = (Array.isArray(actions) ? actions : []).filter(action => action && action.enabled);
    if (!pool.length) {
        return quickActions.find(action => action.enabled)
            || actions.find(action => action.enabled)
            || null;
    }
    const preferredTypes = quickActionTypeCandidates();
    return [...pool]
        .sort((left, right) =>
            playableActionScore(right, preferredTypes, preferredTypes.indexOf(right.actionType))
            - playableActionScore(left, preferredTypes, preferredTypes.indexOf(left.actionType)))
        [0] || null;
}

function quickPrioritySignals(quickActions) {
    const actionTypes = new Set((quickActions || []).map(action => action?.actionType).filter(Boolean));
    const gap = firstWarnEndingRequirement();
    const gapHit = (quickActions || []).some(action => actionEndingGapCovered(action))
        || endingGapActionTypes(gap).some(type => actionTypes.has(type));
    const objectiveTypes = stageObjectiveActionTypes();
    const objectiveHit = objectiveTypes.some(type => actionTypes.has(type));
    const riskHot = hasMeaningfulReadyRisk();
    const trendText = [
        state.stageBriefing?.actionHint,
        state.stageBriefing?.nextTrendHint,
        state.vup?.platformTrend?.label
    ].join(" ");
    const trendSignal = /歌回扶持|练歌|歌回/.test(trendText)
        ? { value: "歌回顺风", types: ['TRAIN_SONG', 'STREAM_PLAN', 'PUBLISH_VIDEO'] }
        : /抽象出圈|切片|素材|整活/.test(trendText)
            ? { value: "素材顺风", types: ['PUBLISH_VIDEO', 'PUBLISH_CLIP', 'STREAM_PLAN'] }
            : /商业复审|粉丝群|排班|稳/.test(trendText)
                ? { value: "稳健顺风", types: ['FAN_GROUP_MAINTAIN', 'TRAIN_TALK', 'REST'] }
                : { value: "常规轮换", types: [] };
    const trendHit = trendSignal.types.some(type => actionTypes.has(type));
    return [
        {
            label: "结局缺口",
            value: gap ? `${endingGapActionLabel(gap)} · ${endingGapProgressText(gap) || "看预演"}` : "门槛暂稳",
            tone: gap ? (gapHit ? "hit" : "warn") : "idle"
        },
        {
            label: "阶段委托",
            value: objectiveTypes.length ? (objectiveHit ? "已排进速切" : "待手动补") : "暂无硬委托",
            tone: objectiveTypes.length ? (objectiveHit ? "hit" : "warn") : "idle"
        },
        {
            label: "风险权重",
            value: riskHot ? "先保旧账" : "可追路线",
            tone: riskHot ? "risk" : "hit"
        },
        {
            label: "平台风向",
            value: trendSignal.value,
            tone: trendSignal.types.length ? (trendHit ? "hit" : "warn") : "idle"
        }
    ];
}

function renderQuickPriorityStrip(quickActions) {
    const signals = quickPrioritySignals(quickActions);
    // 只显示有实际警告的信号，去掉技术标签
    const importantSignals = signals.filter(s => s.tone === 'risk' || s.tone === 'warn');
    if (importantSignals.length === 0) return '';
    return `
        <div class="quick-priority-strip" aria-label="提示">
            ${importantSignals.map(signal => `
                <span class="quick-priority-chip ${html(signal.tone)}">
                    <strong>${html(signal.value)}</strong>
                </span>
            `).join("")}
        </div>
    `;
}

function renderPrimaryActionCue(action) {
    if (!action) {
        return '';
    }
    const cueText = primaryActionCueText(action);
    const actionName = actionNameText(action);
    return `
        <div class="ready-primary-cue" role="note" data-primary-action-type="${html(action.actionType)}" aria-label="${html(`今日主推：${actionName}，${cueText}`)}">
            <span>今日主推</span>
            <strong>${html(actionName)}</strong>
            <small>${html(cueText)}</small>
        </div>
    `;
}

function simplifiedActionPayoff(action) {
    if (!action) return "收获待同步";
    const summary = actionDecisionSummary(action, "recommended");
    return shortDecisionText(summary.benefit, "看日报结算");
}

function simplifiedActionRisk(action) {
    if (!action) return "风险待观察";
    const debts = activeDebts();
    if (debts && debts.length > 0) {
        const urgent = debts.filter(d => debtDaysLeft(d) <= 2);
        if (urgent.length > 0) return '有' + urgent.length + '个风险快到期';
        return '有' + debts.length + '个风险待处理';
    }
    const summary = actionDecisionSummary(action, "recommended");
    return shortDecisionText(summary.risk, "当前安全");
}

function simpleDecisionReasonText(action) {
    if (!action) return "等待行动同步";
    const reason = actionDecisionReason(action, "recommended")
        .replace(/^先点理由：/, "")
        .replace(/^取舍：/, "");
    return shortDecisionText(reason, "按推荐信号推进");
}

function simpleDecisionCostText(action) {
    if (!action) return "消耗待同步";
    const cost = actionResourceChipText(action);
    const risk = actionRiskLevelText(action.riskLevel, "");
    const parts = [
        cost && cost !== "资源待同步" ? cost : "",
        risk
    ].filter(Boolean);
    return shortDecisionText(parts.join(" / "), "消耗待同步");
}

function simpleDecisionNextStepText(action) {
    if (!action) return "等列表加载";
    const type = action.actionType;
    if (type === "STREAM_PLAN") return "点后选标题";
    if (type === "PUBLISH_CLIP" || type === "PUBLISH_VIDEO") return "看效果反馈";
    if (type === "NPC_INTERACT") return "看互动结果";
    if (type === "FAN_GROUP_MAINTAIN" || type === "TRAIN_TALK" || type === "REST") {
        return hasMeaningfulReadyRisk() ? "先处理风险" : "稳步积累";
    }
    if (type === "TRAIN_SONG" || type === "TRAIN_DANCE") return "看技能提升";
    return "做完看日报";
}

function simpleDecisionLoopText(action) {
    if (!action) return "等待行动列表加载。";
    const benefit = simplifiedActionPayoff(action);
    const risk = simplifiedActionRisk(action);
    return `${benefit}。风险：${risk}。`;
}

function simpleDecisionMetricValue(action, key) {
    return actionTradeoffMetrics(action).find(metric => metric.key === key)?.value || 0;
}

function simpleDecisionRouteText(action) {
    if (!action) return "路线待定";
    const summary = actionDecisionSummary(action, "playable");
    return shortDecisionText(summary.route, "路线待定");
}

function simpleDecisionTempoText(action) {
    if (!action) return "节奏待定";
    const combo = actionComboShortText(action);
    const tempo = combo || visibleTextOrFallback(action.tempoHint, "");
    return shortDecisionText(tempo || simpleDecisionNextStepText(action), "做完看日报");
}

function simpleDecisionRiskText(action) {
    if (!action) return "风险待定";
    return shortDecisionText(simplifiedActionRisk(action), "当前安全");
}

function simpleDecisionPreviewText(action) {
    return `路线：${simpleDecisionRouteText(action)} · 风险：${simpleDecisionRiskText(action)} · 节奏：${simpleDecisionTempoText(action)}`;
}

function simpleDecisionPainScore(action) {
    return {
        payoff: simpleDecisionMetricValue(action, "payoff"),
        danger: simpleDecisionMetricValue(action, "danger"),
        pressure: simpleDecisionMetricValue(action, "pressure")
    };
}

function simpleDecisionContrastAction(primaryAction, alternativeActions) {
    const alternatives = (alternativeActions || []).filter(Boolean);
    if (!primaryAction || !alternatives.length) return null;
    const primary = simpleDecisionPainScore(primaryAction);
    const primaryWeight = primary.danger + primary.pressure * 0.35;
    const enabledFirst = alternatives.filter(action => action.enabled);
    const candidates = enabledFirst.length ? enabledFirst : alternatives;
    return candidates
        .map(action => {
            const score = simpleDecisionPainScore(action);
            const weight = score.danger + score.pressure * 0.35;
            const riskGap = Math.abs(weight - primaryWeight);
            const payoffGap = Math.abs(score.payoff - primary.payoff);
            return { action, sort: riskGap * 1.4 + payoffGap };
        })
        .sort((left, right) => right.sort - left.sort)[0]?.action || null;
}

function simpleDecisionVisibleChoices(primaryAction, quickActions) {
    const all = (quickActions || []).filter(Boolean);
    const action = primaryAction || all.find(item => item?.enabled) || null;
    const alternatives = all.filter(item => item && item.actionType !== action?.actionType);
    const enabledAlternatives = alternatives.filter(item => item.enabled);
    const candidates = enabledAlternatives.length ? enabledAlternatives : alternatives;
    const scoreFor = item => simpleDecisionPainScore(item);
    const safe = candidates
        .map(item => {
            const score = scoreFor(item);
            return {
                item,
                score: score.danger + score.pressure * 0.5 - score.payoff * 0.2
            };
        })
        .sort((left, right) => left.score - right.score)[0]?.item || null;
    const risky = candidates
        .filter(item => item.actionType !== safe?.actionType)
        .map(item => {
            const score = scoreFor(item);
            return {
                item,
                score: score.payoff - score.danger * 0.28 - score.pressure * 0.18
            };
        })
        .sort((left, right) => right.score - left.score)[0]?.item || null;
    const fallback = candidates.find(item => item.actionType !== safe?.actionType && item.actionType !== risky?.actionType) || null;
    return {
        action,
        alternatives: [
            { action: safe, role: "safe", label: "稳", title: "安全牌" },
            { action: risky || fallback, role: "risky", label: "搏", title: "高上限" }
        ].filter(choice => choice.action)
    };
}

function simpleDecisionPainModes(primaryAction, contrastAction) {
    if (!primaryAction || !contrastAction) return { primary: "主推", contrast: "备选" };
    const primary = simpleDecisionPainScore(primaryAction);
    const contrast = simpleDecisionPainScore(contrastAction);
    const primaryWeight = primary.danger + primary.pressure * 0.35;
    const contrastWeight = contrast.danger + contrast.pressure * 0.35;
    if (Math.abs(primaryWeight - contrastWeight) >= 6) {
        return primaryWeight > contrastWeight
            ? { primary: "赌", contrast: "稳" }
            : { primary: "稳", contrast: "赌" };
    }
    if (Math.abs(primary.payoff - contrast.payoff) >= 6) {
        return primary.payoff > contrast.payoff
            ? { primary: "赌", contrast: "稳" }
            : { primary: "稳", contrast: "赌" };
    }
    return { primary: "主推", contrast: "另一手" };
}

function simpleDecisionModeFocus(action, mode) {
    if (!action) return "取舍待同步";
    if (mode === "赌") return `押${simplifiedActionPayoff(action)}`;
    if (mode === "稳") return `守${simplifiedActionRisk(action)}`;
    return simpleDecisionCostText(action);
}

function simpleDecisionCounterplayText(primaryAction, alternativeActions) {
    const contrastAction = simpleDecisionContrastAction(primaryAction, alternativeActions);
    if (!contrastAction) return "";
    const modes = simpleDecisionPainModes(primaryAction, contrastAction);
    const primaryName = actionNameText(primaryAction);
    const contrastName = actionNameText(contrastAction);
    return `赌/稳：主推${modes.primary}「${primaryName}」；备选${modes.contrast}「${contrastName}」。`;
}

function simplifiedActionNextStep(action) {
    if (!action) return "等行动列表加载。";
    const type = action.actionType;
    if (type === "STREAM_PLAN") return "点完后选标题，标题决定反馈和风险。";
    if (type === "FAN_GROUP_MAINTAIN" || type === "TRAIN_TALK" || type === "REST") {
        return hasMeaningfulReadyRisk()
            ? "先处理风险，再看日报。"
            : "稳扎稳打，积累口碑和粉丝。";
    }
    if (type === "PUBLISH_CLIP" || type === "PUBLISH_VIDEO") return "提高传播和路线进度。";
    if (type === "NPC_INTERACT") return "扩大影响力，但注意风险。";
    if (type === "TRAIN_SONG" || type === "TRAIN_DANCE") return "提升技能，推进路线。";
    return "做完看日报，再决定下一步。";
}

function shortDecisionText(value, fallback) {
    return digestText(localizedVisibleTextOrFallback(value, fallback), fallback);
}

function isFirstRunOpeningTutorialActive(day = Number(state.session?.day || state.vup?.day || 1)) {
    if (!state.vup || day < 1 || day > 3) return false;
    const expectations = state.vup.expectations || {};
    if (expectations.restartTargetType || expectations.restartRunObjective || expectations.previousEndingMemory) {
        return false;
    }
    const completedRuns = Number(state.achievementProgress?.endingCollection?.completedRuns ?? 0);
    return !Number.isFinite(completedRuns) || completedRuns <= 0;
}

function isFirstRunRouteChoiceActive(day = Number(state.session?.day || state.vup?.day || 1)) {
    return !state.openingStyleChoiceSkipped
        && day === 1
        && isFirstRunOpeningTutorialActive(day)
        && shouldShowOpeningStyleChoices();
}

function primaryActionButtonText(action) {
    if (!action) return "等待行动";
    if (action.actionType === "STREAM_PLAN") return "开始企划";
    return "执行今日行动";
}

function openingPhaseCoachLine(phase, action, day = Number(state.session?.day || state.vup?.day || 1)) {
    if (!isFirstRunOpeningTutorialActive(day) || day < 1 || day > 3) return "";
    const actionName = action ? actionNameText(action) : "推荐行动";
    const phaseKey = phase || state.session?.phase || "READY";
    const dayPlan = {
        1: {
            click: `今天先点推荐行动【${actionName}】，点完看第一张日报`,
            change: "先看粉丝/口碑/围观，其它数值先当后台证据",
            worry: "明天只看日报里的风险和涨粉原因"
        },
        2: {
            click: `今天继续点【${actionName}】或同路线低风险行动`,
            change: "继续同路线会加厚证据，有风险先修复",
            worry: "明天别同时换路线和硬冲热度"
        },
        3: {
            click: `今天点【${actionName}】把路线钉住`,
            change: "结局预演开始有意义，会更偏向当前路线",
            worry: "不要同时换路线和硬冲热度，明天开始补缺口"
        }
    }[day];
    const phaseAction = {
        READY: dayPlan.click,
        NEED_TITLE: "今天选一个标题，想稳就点低风险/安全标题",
        OFF_STREAM_READY: "今天选一个下播小动作，懒得想就跳过",
        NEED_INTERACTION_CHOICE: "今天选一个现场回应，优先看风险预览",
        NEED_EVENT_CHOICE: "今天选一个事件应对，别让旧账滚大",
        REPORT_READY: "今天读完日报后点下一天"
    }[phaseKey] || dayPlan.click;
    return `${phaseAction}；会变：${dayPlan.change}；明天担心：${dayPlan.worry}。`;
}

function renderOpeningDecisionHint(action, day) {
    if (!action || !isFirstRunOpeningTutorialActive(day)) return "";
    const line = openingPhaseCoachLine("READY", action, day);
    return line ? `<p>新手一句话：${html(line)}</p>` : "";
}

function renderNewPlayerGuardrail(action, day) {
    if (!action || day !== 1 || !isFirstRunOpeningTutorialActive(day)) return "";
    const route = simpleDecisionRouteText(action);
    const risk = simpleDecisionRiskText(action);
    const next = simpleDecisionNextStepText(action);
    return `
        <section class="new-player-guardrail" aria-label="新手三步">
            <div class="new-player-guardrail-head">
                <span>首局护栏</span>
                <strong>前三天照这个顺序看就行</strong>
            </div>
            <div class="new-player-guardrail-grid">
                <span><em>1 今天先点推荐行动</em><strong>${html(actionNameText(action))}</strong><small>${html(next)}，点完看第一张日报。</small></span>
                <span><em>2 看变化</em><strong>粉丝 / 口碑 / 围观</strong><small>其它数值先当后台证据。</small></span>
                <span><em>3 明天担心</em><strong>${html(risk)}</strong><small>路线：${html(route)}</small></span>
            </div>
        </section>
    `;
}

function renderRiskStatus() {
    const debts = activeDebts();
    if (!debts || debts.length === 0) {
        return '<div class="risk-status clean"><span>✅ 当前没有风险，可以放心行动</span></div>';
    }
    const urgentDebts = debts.filter(d => debtDaysLeft(d) <= 2);
    const rows = debts.slice(0, 3).map(d => {
        const left = debtDaysLeft(d);
        const urgency = left <= 0 ? 'urgent' : left <= 2 ? 'warning' : 'normal';
        const dueText = left <= 0 ? '⚠️ 今天到期！' : left === 1 ? '⏰ 明天到期' : `📅 ${left}天后到期`;
        const label = firstLocalizedReadableText([d.debtLabel, d.summary, d.consequencePreview], debtDisplayLabel(d, "风险"));
        const counterplay = debtCounterplayText(d);
        return '<div class="risk-row ' + urgency + '"><span>' + html(label) + '</span><span>' + dueText + '</span><span>' + html(counterplay) + '</span></div>';
    }).join('');
    const header = urgentDebts.length > 0 ? '今天先处理' : '旧账在发酵';
    return '<div class="risk-status ' + (urgentDebts.length > 0 ? 'has-urgent' : 'has-risk') + '"><div class="risk-header">⚠️ ' + header + ' · ' + debts.length + '个旧账</div>' + rows + (urgentDebts.length > 0 ? '<div class="risk-urgent-hint">🔥 建议优先处理到期风险！</div>' : '') + '</div>';
}

// ============================================================
// 第二批 - 体验闭环前端 (2.2 风险预警 / 1.4 对手威胁 / 2.3 结局图鉴 / 2.5 回放)
// ============================================================

// 2.2 风险预警面板：渲染 crisisAlerts 列表，daysLeft<=2 红色脉冲
function renderCrisisAlerts() {
    const panel = document.getElementById('crisisAlertPanel');
    if (!panel) return;
    const alerts = Array.isArray(state.crisisAlerts) ? state.crisisAlerts : [];
    if (alerts.length === 0) {
        panel.innerHTML = '<div class="context-sidebar-item empty"><span class="context-sidebar-title">🛡️ 危机预警</span><span class="context-sidebar-empty">暂无即将到期风险</span></div>';
        return;
    }
    const sorted = alerts.slice().sort((a, b) => (a.daysLeft ?? 99) - (b.daysLeft ?? 99));
    const rows = sorted.slice(0, 5).map(a => {
        const days = Number(a.daysLeft ?? 99);
        const urgent = days <= 2;
        const sev = a.severity || (urgent ? 'high' : 'normal');
        const dueText = days <= 0 ? '⚠️ 今天到期' : days === 1 ? '⏰ 明天到期' : `📅 ${days}天后`;
        const cls = urgent ? 'crisis-alert-row urgent' : 'crisis-alert-row';
        const title = visibleTextOrFallback(a.title, '风险');
        const desc = visibleTextOrFallback(a.description, '');
        const type = a.type ? `<span class="crisis-alert-type">${html(a.type)}</span>` : '';
        return '<div class="' + cls + '" data-severity="' + html(sev) + '">'
            + '<div class="crisis-alert-head">' + type + '<strong>' + html(title) + '</strong><span class="crisis-alert-due">' + dueText + '</span></div>'
            + (desc ? '<p class="crisis-alert-desc">' + html(desc) + '</p>' : '')
            + '</div>';
    }).join('');
    panel.innerHTML = '<div class="context-sidebar-item expanded" data-context-item="crisis">'
        + '<button class="context-sidebar-head" type="button" data-action="toggle-context-item" data-context-target="crisis" aria-expanded="true">'
        + '<span class="context-sidebar-title">🛡️ 危机预警 · ' + alerts.length + '</span>'
        + '<span class="context-sidebar-chevron">▾</span></button>'
        + '<div class="context-sidebar-body">' + rows + '</div>'
        + '</div>';
}

// 1.4 对手威胁：水平进度条对比玩家 vs 最强对手
function renderRivalThreat() {
    const panel = document.getElementById('rivalThreatPanel');
    if (!panel) return;
    const rivals = state.rivals;
    const playerFans = Number(state.vup?.fanStructure?.fans ?? state.vup?.fans ?? 0);
    const rivalList = Array.isArray(rivals?.rivals) ? rivals.rivals : (Array.isArray(rivals) ? rivals : []);
    if (rivalList.length === 0 || !state.vup) {
        panel.innerHTML = '<div class="context-sidebar-item empty"><span class="context-sidebar-title">⚔️ 对手威胁</span><span class="context-sidebar-empty">暂无对手数据</span></div>';
        return;
    }
    const topRival = rivalList.slice().sort((a, b) => (b.fans ?? 0) - (a.fans ?? 0))[0];
    const rivalFans = Number(topRival?.fans ?? 0);
    const max = Math.max(playerFans, rivalFans, 1);
    const playerPct = Math.round((playerFans / max) * 100);
    const rivalPct = Math.round((rivalFans / max) * 100);
    const threat = (topRival?.threatLevel || '').toLowerCase();
    const highThreat = threat === 'high' || threat === 'critical' || (rivalFans > playerFans * 1.1);
    const overtaken = !!rivals?.overtaken || rivalFans > playerFans;
    const cls = highThreat ? 'rival-threat-row high' : 'rival-threat-row';
    const rivalName = visibleTextOrFallback(topRival?.name, '最强对手');
    const rivalRoute = topRival?.route ? routeLabelFor(topRival.route) : '';
    const growth = Number(topRival?.growthRate ?? 0);
    const body = '<div class="' + cls + (highThreat ? ' threat-pulse' : '') + '">'
        + '<div class="rival-threat-head"><strong>' + html(rivalName) + '</strong>'
        + (rivalRoute ? '<span class="rival-threat-route">' + html(rivalRoute) + '</span>' : '')
        + (overtaken ? '<span class="rival-threat-flag">⚠️ 已超越你</span>' : '')
        + '</div>'
        + '<div class="rival-threat-bars">'
        + '<div class="rival-threat-bar player"><span class="rival-threat-label">你</span><div class="rival-threat-track"><div class="rival-threat-fill player" style="width:' + playerPct + '%"></div></div><span class="rival-threat-value">' + compactCount(playerFans) + '</span></div>'
        + '<div class="rival-threat-bar rival"><span class="rival-threat-label">对手</span><div class="rival-threat-track"><div class="rival-threat-fill rival" style="width:' + rivalPct + '%"></div></div><span class="rival-threat-value">' + compactCount(rivalFans) + '</span></div>'
        + '</div>'
        + (growth ? '<div class="rival-threat-growth">增长率 +' + html(growth) + '/日</div>' : '')
        + '</div>';
    panel.innerHTML = '<div class="context-sidebar-item expanded" data-context-item="rival">'
        + '<button class="context-sidebar-head" type="button" data-action="toggle-context-item" data-context-target="rival" aria-expanded="true">'
        + '<span class="context-sidebar-title">⚔️ 对手威胁</span>'
        + '<span class="context-sidebar-chevron">▾</span></button>'
        + '<div class="context-sidebar-body">' + body + '</div>'
        + '</div>';
}

// 2.3 结局图鉴：3x3 网格，未解锁灰显，推荐目标高亮
function renderEndingAtlas() {
    const panel = document.getElementById('endingAtlasPanel');
    if (!panel) return;
    const atlas = state.endingAtlas;
    if (!atlas) {
        panel.innerHTML = '<div class="ending-atlas-loading">图鉴加载中...</div>';
        return;
    }
    const items = Array.isArray(atlas.items) ? atlas.items : [];
    const recommended = atlas.recommendedNext || '';
    const unlockedCount = Number(atlas.unlockedCount ?? 0);
    const totalCount = Number(atlas.totalCount ?? (items.length || 9));
    const cells = items.map(item => {
        const unlocked = !!item.unlocked;
        const isRec = recommended && item.endingType === recommended;
        const cls = 'ending-atlas-item' + (unlocked ? '' : ' locked') + (isRec ? ' recommended' : '');
        const title = visibleTextOrFallback(item.title, item.endingType || '结局');
        const hint = visibleTextOrFallback(item.hint, '');
        return '<div class="' + cls + '" data-ending-type="' + html(item.endingType || '') + '">'
            + '<div class="ending-atlas-icon">' + (unlocked ? '✅' : '🔒') + '</div>'
            + '<div class="ending-atlas-title">' + html(title) + '</div>'
            + (hint ? '<div class="ending-atlas-hint">' + html(hint) + '</div>' : '')
            + (isRec ? '<div class="ending-atlas-rec">推荐下一目标</div>' : '')
            + '</div>';
    }).join('');
    panel.innerHTML = '<div class="ending-atlas-header">'
        + '<span class="ending-atlas-kicker">结局图鉴</span>'
        + '<strong>' + unlockedCount + '/' + totalCount + ' 已解锁</strong>'
        + '</div>'
        + '<div class="ending-atlas-grid">' + cells + '</div>'
        + (recommended ? '<div class="ending-atlas-next">推荐下一目标：' + html(recommended) + '</div>' : '');
}

// 2.5 快速回放/复盘：Chart.js 画粉丝增长曲线 + 决策点
let timelineReplayChart = null;

function renderTimelineReplay() {
    const panel = document.getElementById('timelineReplayPanel');
    if (!panel) return;
    const timeline = state.timeline;
    if (!timeline || !Array.isArray(timeline.days) || timeline.days.length === 0) {
        panel.innerHTML = '<div class="timeline-replay-empty">暂无回放数据</div>';
        return;
    }
    const days = timeline.days;
    const labels = days.map(d => '第' + (d.day ?? '?') + '天');
    const fanData = days.map(d => Number(d.fanChange ?? 0));
    const routeData = days.map(d => Number(d.routeScore ?? 0));
    const highlightPoints = days.map(d => d.highlight ? visibleTextOrFallback(d.highlight, '') : '');
    const actionLabels = days.map(d => visibleTextOrFallback(d.action, '') || '');

    panel.classList.remove('hidden');
    panel.innerHTML = '<div class="timeline-replay-card">'
        + '<div class="timeline-replay-head"><strong>📈 本局回放</strong>'
        + '<button class="timeline-replay-close" type="button" data-action="close-timeline-replay" aria-label="关闭回放">×</button></div>'
        + '<div class="timeline-replay-canvas-wrap"><canvas id="timelineReplayChart" height="220"></canvas></div>'
        + '<div class="timeline-replay-events">' + days.map((d, i) => {
            if (!highlightPoints[i] && !actionLabels[i]) return '';
            return '<div class="timeline-replay-event"><span class="timeline-replay-day">第' + html(d.day ?? '?') + '天</span>'
                + (actionLabels[i] ? '<span class="timeline-replay-action">' + html(actionLabels[i]) + '</span>' : '')
                + (highlightPoints[i] ? '<span class="timeline-replay-highlight">' + html(highlightPoints[i]) + '</span>' : '')
                + '</div>';
        }).join('') + '</div>'
        + '</div>';

    const canvas = document.getElementById('timelineReplayChart');
    if (!canvas || typeof window === 'undefined' || !window.Chart) return;
    if (timelineReplayChart) { try { timelineReplayChart.destroy(); } catch (e) {} timelineReplayChart = null; }
    try {
        timelineReplayChart = new window.Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: [
                    {
                        label: '粉丝增量',
                        data: fanData,
                        borderColor: '#fb7299',
                        backgroundColor: 'rgba(251,114,153,0.15)',
                        tension: 0.3,
                        fill: true,
                        pointRadius: days.map((d, i) => d.highlight ? 6 : 3),
                        pointBackgroundColor: days.map(d => d.highlight ? '#ffd700' : '#fb7299')
                    },
                    {
                        label: '路线分',
                        data: routeData,
                        borderColor: '#23ade5',
                        backgroundColor: 'rgba(35,173,229,0.1)',
                        tension: 0.3,
                        fill: false,
                        borderDash: [4, 4]
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { labels: { color: '#e8e8f0' } },
                    tooltip: {
                        callbacks: {
                            afterLabel: function(ctx) {
                                const idx = ctx.dataIndex;
                                const hl = highlightPoints[idx];
                                return hl ? '亮点：' + hl : '';
                            }
                        }
                    }
                },
                scales: {
                    x: { ticks: { color: '#9d9db0' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#9d9db0' }, grid: { color: 'rgba(255,255,255,0.05)' } }
                }
            }
        });
    } catch (e) {
        console.warn('timeline replay chart failed:', e);
    }
}

function closeTimelineReplay() {
    const panel = document.getElementById('timelineReplayPanel');
    if (panel) panel.classList.add('hidden');
    if (timelineReplayChart) { try { timelineReplayChart.destroy(); } catch (e) {} timelineReplayChart = null; }
}

// ============================================================
// 4.4 右侧栏上下文情报：renderContextSidebar 调度5子面板
// ============================================================
function renderContextSidebar() {
    // 5 子面板：crisisAlert / rivalThreat / fortune / comboHint / coachAdvice
    renderCrisisAlerts();
    renderRivalThreat();
    renderFortunePanel();
    renderComboHintPanel();
    renderCoachAdvicePanel();
}

function renderFortunePanel() {
    const panel = document.getElementById('fortunePanel');
    if (!panel) return;
    const v = state.vup;
    if (!v) {
        panel.innerHTML = '<div class="context-sidebar-item empty"><span class="context-sidebar-title">🔮 运势</span><span class="context-sidebar-empty">--</span></div>';
        return;
    }
    const fortune = v.fortune || state.stageBriefing?.fortune || null;
    const label = visibleTextOrFallback(fortune?.label, fortune?.name, '运势平稳');
    const desc = visibleTextOrFallback(fortune?.description, fortune?.effect, '');
    const tier = fortune?.tier || '';
    panel.innerHTML = '<div class="context-sidebar-item expanded" data-context-item="fortune">'
        + '<button class="context-sidebar-head" type="button" data-action="toggle-context-item" data-context-target="fortune" aria-expanded="true">'
        + '<span class="context-sidebar-title">🔮 运势' + (tier ? ' · ' + html(tier) : '') + '</span>'
        + '<span class="context-sidebar-chevron">▾</span></button>'
        + '<div class="context-sidebar-body">'
        + '<div class="fortune-card"><strong>' + html(label) + '</strong>'
        + (desc ? '<p>' + html(desc) + '</p>' : '')
        + '</div>'
        + '</div></div>';
}

function renderComboHintPanel() {
    const panel = document.getElementById('comboHintPanel');
    if (!panel) return;
    const combo = state.comboDiscovery;
    if (!combo) {
        panel.innerHTML = '<div class="context-sidebar-item empty"><span class="context-sidebar-title">🔗 连击提示</span><span class="context-sidebar-empty">暂无可用连击</span></div>';
        return;
    }
    const label = visibleTextOrFallback(combo.label || combo.comboKey, '连击');
    const hint = visibleTextOrFallback(combo.hint || combo.description, '');
    const ready = !!combo.ready;
    panel.innerHTML = '<div class="context-sidebar-item expanded" data-context-item="combo">'
        + '<button class="context-sidebar-head" type="button" data-action="toggle-context-item" data-context-target="combo" aria-expanded="true">'
        + '<span class="context-sidebar-title">🔗 连击提示' + (ready ? ' · 可用' : '') + '</span>'
        + '<span class="context-sidebar-chevron">▾</span></button>'
        + '<div class="context-sidebar-body">'
        + '<div class="combo-hint-card"><strong>' + html(label) + '</strong>'
        + (hint ? '<p>' + html(hint) + '</p>' : '')
        + '</div>'
        + '</div></div>';
}

// 4.4 教练建议精简为1条注入 #coachAdvicePanel（保留原 renderCoach 渲染 #coachPanel 兼容）
function renderCoachAdvicePanel() {
    const panel = document.getElementById('coachAdvicePanel');
    if (!panel) return;
    if (!state.vup) {
        panel.innerHTML = '<div class="context-sidebar-item empty"><span class="context-sidebar-title">🎯 下一步建议</span><span class="context-sidebar-empty">--</span></div>';
        return;
    }
    const phase = state.session?.phase || 'READY';
    const copy = {
        READY: ['今天只做一个主决定', '先看推荐卡：它会告诉你得到什么、承担什么、会更像哪条路线。'],
        NEED_TITLE: ['给这场直播起标题', '稳标题保口碑，狠标题冲围观，也可能留下旧账。'],
        NEED_INTERACTION_CHOICE: ['直播间正在等你回应', '把每个选项当成一次表态。'],
        NEED_EVENT_CHOICE: ['今天的麻烦来了', '先看选项里的风险预览。'],
        REPORT_READY: ['先看三行，再开下一天', '日报先看粉丝、口碑、围观和风险提示。'],
        ENDING_READY: ['这局已经成型', '结局页会告诉你为什么走到这里。'],
        OFF_STREAM_READY: ['下播后补一手', '这是轻量收尾：补路线、降风险，或直接跳过进日报。']
    }[phase] || [getPhaseText(phase), '按当前面板的按钮继续。'];
    const [title, body] = copy;
    panel.innerHTML = '<div class="context-sidebar-item expanded" data-context-item="coach">'
        + '<button class="context-sidebar-head" type="button" data-action="toggle-context-item" data-context-target="coach" aria-expanded="true">'
        + '<span class="context-sidebar-title">🎯 下一步建议</span>'
        + '<span class="context-sidebar-chevron">▾</span></button>'
        + '<div class="context-sidebar-body">'
        + '<div class="coach-advice-card"><strong>' + html(title) + '</strong><p>' + html(body) + '</p></div>'
        + '</div></div>';
}

function toggleContextItem(target) {
    const item = document.querySelector('[data-context-item="' + target + '"]');
    if (!item) return;
    const expanded = item.classList.toggle('expanded');
    const head = item.querySelector('.context-sidebar-head');
    if (head) head.setAttribute('aria-expanded', String(expanded));
    const chevron = item.querySelector('.context-sidebar-chevron');
    if (chevron) chevron.textContent = expanded ? '▾' : '▸';
}

// ============================================================
// 4.5 / 4.6 平台抽屉 / 总览独立页 全屏开关
// ============================================================
function togglePlatformDrawer() {
    // 复用 liveDrawer 的 'npcs' 面板，叠加全屏样式
    if (typeof openLiveDrawer === 'function') {
        openLiveDrawer('npcs');
        const drawer = document.getElementById('liveDrawer');
        if (drawer) drawer.classList.toggle('fullscreen');
    }
}

function toggleInfoHubPage() {
    if (typeof openLiveDrawer === 'function') {
        openLiveDrawer('environment');
        const drawer = document.getElementById('liveDrawer');
        if (drawer) drawer.classList.toggle('fullscreen');
    }
}

// ============================================================
// 5.1 render() 拆分按区渲染
// ============================================================
function renderTopBar() {
    renderHeader();
    syncAudioSettingsPanel();
    syncBGM();
}

function renderMainCard() {
    renderActions();
    renderOffStreamPanel();
    renderFanTopics();
    renderTitles();
    renderEvent();
    renderReport();
    renderEnding();
    renderMainDecisionCard();
}

function renderSidebar() {
    renderCoach();
    renderContextSidebar();
    if (state.vup) renderOperationDiagnosis(state.vup, state.session);
    renderInsightDigest();
    renderAchievements();
    renderUnlockAtlas();
    renderEndingAtlas();
}

function renderLiveLayer() {
    syncLiveRoomStage();
    syncLiveDrawerSummaries();
    renderDanmaku();
    updateVupAvatar();
}

// 按需渲染入口（供 refreshAfterWrite 调用，避免全量重绘）
async function renderOnDemand(scope) {
    const s = scope || 'all';
    if (s === 'all') { render(); return; }
    if (s === 'topBar') { renderTopBar(); return; }
    if (s === 'mainCard') { renderMainCard(); return; }
    if (s === 'sidebar') { renderSidebar(); return; }
    if (s === 'liveLayer') { renderLiveLayer(); return; }
}

// ============================================================
// 4.2 左栏状态聚合：Chart.js 雷达图 + 粉丝饼图 + 路线分迷你条
// ============================================================
let statRadarChart = null;
let statFanPieChart = null;

function initStatCharts() {
    if (typeof window === 'undefined' || !window.Chart) return;
    const radarCanvas = document.getElementById('radarChart');
    const pieCanvas = document.getElementById('fanPieChart');
    if (radarCanvas && !statRadarChart) {
        try {
            statRadarChart = new window.Chart(radarCanvas, {
                type: 'radar',
                data: { labels: ['歌力', '舞力', '杂谈', '梗力', '企划', '抗压'], datasets: [{ label: '能力', data: [0,0,0,0,0,0], backgroundColor: 'rgba(251,114,153,0.15)', borderColor: '#fb7299', pointBackgroundColor: '#fb7299' }] },
                options: { responsive: true, maintainAspectRatio: true, scales: { r: { beginAtZero: true, suggestedMax: 100, ticks: { color: '#9d9db0' }, grid: { color: 'rgba(255,255,255,0.08)' }, angleLines: { color: 'rgba(255,255,255,0.08)' }, pointLabels: { color: '#e8e8f0', font: { size: 11 } } } }, plugins: { legend: { display: false } } }
            });
        } catch (e) { console.warn('stat radar init failed:', e); }
    }
    if (pieCanvas && !statFanPieChart) {
        try {
            statFanPieChart = new window.Chart(pieCanvas, {
                type: 'doughnut',
                data: { labels: ['真爱粉', '乐子人', '独角兽', 'DD'], datasets: [{ data: [1,1,1,1], backgroundColor: ['#fb7299', '#ffd700', '#a855f7', '#23ade5'], borderColor: 'rgba(0,0,0,0.2)', borderWidth: 1 }] },
                options: { responsive: true, maintainAspectRatio: true, plugins: { legend: { position: 'bottom', labels: { color: '#e8e8f0', font: { size: 10 }, boxWidth: 10 } } }, cutout: '55%' }
            });
        } catch (e) { console.warn('stat pie init failed:', e); }
    }
}

function updateStatCharts() {
    if (!state.vup) return;
    const v = state.vup;
    if (statRadarChart) {
        const a = v.attributes || {};
        statRadarChart.data.datasets[0].data = [
            Number(a.songPower ?? 0), Number(a.dancePower ?? 0), Number(a.talkPower ?? 0),
            Number(a.memePower ?? 0), Number(a.planPower ?? 0), Number(a.stressPower ?? 0)
        ];
        try { statRadarChart.update('none'); } catch (e) {}
    }
    if (statFanPieChart) {
        const fs = v.fanStructure || {};
        statFanPieChart.data.datasets[0].data = [
            Math.max(0, Number(fs.trueFans ?? 0)),
            Math.max(0, Number(fs.funFans ?? 0)),
            Math.max(0, Number(fs.unicornFans ?? 0)),
            Math.max(0, Number(fs.ddFans ?? 0))
        ];
        try { statFanPieChart.update('none'); } catch (e) {}
    }
    const mini = document.getElementById('routeScoreMini');
    if (mini) {
        const entries = (typeof routeScoreEntries === 'function') ? routeScoreEntries() : [];
        const bars = entries.slice(0, 6).map(item => {
            const pct = Math.max(2, Math.min(100, Number(item.score ?? 0)));
            return '<div class="route-score-mini-row"><span class="route-score-mini-label">' + html(item.routeLabel || item.route || '') + '</span>'
                + '<div class="route-score-mini-track"><div class="route-score-mini-fill" style="width:' + pct + '%"></div></div>'
                + '<span class="route-score-mini-value">' + html(item.score ?? 0) + '</span></div>';
        }).join('');
        mini.innerHTML = '<div class="route-score-mini-card"><div class="route-score-mini-head">路线分</div>' + bars + '</div>';
    }
}

// 暴露给 window 供 action-registry / app.js 调用
window.renderCrisisAlerts = renderCrisisAlerts;
window.renderRivalThreat = renderRivalThreat;
window.renderEndingAtlas = renderEndingAtlas;
window.renderTimelineReplay = renderTimelineReplay;
window.closeTimelineReplay = closeTimelineReplay;
window.renderContextSidebar = renderContextSidebar;
window.renderFortunePanel = renderFortunePanel;
window.renderComboHintPanel = renderComboHintPanel;
window.renderCoachAdvicePanel = renderCoachAdvicePanel;
window.toggleContextItem = toggleContextItem;
window.togglePlatformDrawer = togglePlatformDrawer;
window.toggleInfoHubPage = toggleInfoHubPage;
window.renderTopBar = renderTopBar;
window.renderMainCard = renderMainCard;
window.renderSidebar = renderSidebar;
window.renderLiveLayer = renderLiveLayer;
window.renderOnDemand = renderOnDemand;
window.initStatCharts = initStatCharts;
window.updateStatCharts = updateStatCharts;
window.renderMainDecisionCard = renderMainDecisionCard;
