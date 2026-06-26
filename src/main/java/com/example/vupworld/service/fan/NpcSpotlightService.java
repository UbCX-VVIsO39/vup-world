package com.example.vupworld.service.fan;

import com.example.vupworld.domain.NpcType;
import com.example.vupworld.service.core.VupService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.dto.NpcDtos.NpcSpotlightDTO;
import com.example.vupworld.model.Vup;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * NPC聚光灯系统。按当前路线/粉丝结构挑选一个"同台NPC"画像推给前端。
 *
 * profile.npcKey 统一使用 {@link NpcType} 枚举 key，与 NpcRelationshipService /
 * NpcEventChainService 共享同一套 NPC 标识，避免三套定义各说各话。
 *
 * 路线 -> NPC 的映射与 NpcEventChainService 保持一致：
 *   SINGING_IDOL    -> SENN_LIN
 *   SLICE_SAINT     -> MIKU_QI
 *   BLACK_RED_MAIN_STAGE -> XIAO_YU
 *   SOCIAL_COLLAB   -> DA_HAO
 *   DANCE_MEME      -> YUE_YA
 *   (CYBER_GIRLFRIEND 走房管画像，对应 BILI_MIKO)
 * 两个特殊画像（商单经纪/平台观察员）映射到剩余的 TUAN_ZI / MI_SHE。
 */
@Service
public class NpcSpotlightService {
    private static final Map<String, NpcProfile> ROUTE_PROFILES = Map.of(
            "SINGING_IDOL", new NpcProfile(
                    NpcType.SENN_LIN.key(),
                    "海声前辈",
                    "歌回导师",
                    "v4/npcs/portraits/singing-mentor.png",
                    "前辈在隔壁棚试麦，录播组已经把高音段落标了三种颜色。",
                    "歌回路线别只卷音高，稳住老粉和DD的听感，比标题组硬蹭热搜更划算。",
                    "歌力和口碑会影响歌回标题兑现。"
            ),
            "DANCE_MEME", new NpcProfile(
                    NpcType.YUE_YA.key(),
                    "节拍教练",
                    "舞蹈教练",
                    "v4/npcs/portraits/dance-coach.png",
                    "练舞房地板刚擦完，切片组已经等一个抽象失误当封面。",
                    "舞蹈短挑战要给DD一个能复读的动作点，也要给老粉留一点正常舞台。",
                    "舞力、整活力和短视频素材会影响舞蹈路线。"
            ),
            "SLICE_SAINT", new NpcProfile(
                    NpcType.MIKU_QI.key(),
                    "夜班剪辑",
                    "切片组编辑",
                    "v4/npcs/portraits/clipper-editor.png",
                    "剪辑台亮到凌晨，标题组把回旋镖和名场面分成了两个文件夹。",
                    "切片路线要喂素材，但别让DD只记住标题，不记得主播本人。",
                    "素材库存和标题债务会影响切片路线。"
            ),
            "BLACK_RED_MAIN_STAGE", new NpcProfile(
                    NpcType.XIAO_YU.key(),
                    "隔壁锐评员",
                    "同行评论员",
                    "v4/npcs/portraits/rival-commentator.png",
                    "对方直播间正在品你今天的措辞，论坛楼已经开始截半句话。",
                    "黑红热度能带DD上车，但米线太薄时老粉和品牌都会先撤。",
                    "围观热度、口碑和争议债务会影响主会场风险。"
            ),
            "CYBER_GIRLFRIEND", new NpcProfile(
                    NpcType.BILI_MIKO.key(),
                    "值班房管",
                    "粉丝群房管",
                    "v4/npcs/portraits/fan-moderator.png",
                    "房管盯着榜一和小作文两边，群公告已经改了三版语气。",
                    "独角兽期待高时别突然端水，DD联动要提前给老粉一个台阶。",
                    "独角兽占比和粉丝群气压会影响陪伴路线。"
            ),
            "SOCIAL_COLLAB", new NpcProfile(
                    NpcType.DA_HAO.key(),
                    "联动搭子",
                    "联动主播",
                    "v4/npcs/portraits/collab-streamer.png",
                    "对方已经发了联动预告，DD像公交站一样开始换乘。",
                    "社交流可以吃DD流量，但每天都蹭会让老粉和独角兽同时开始写小作文。",
                    "同台互动次数和DD占比会影响社交联动路线。"
            )
    );

    private static final NpcProfile SPONSOR_MANAGER = new NpcProfile(
            NpcType.TUAN_ZI.key(),
            "商单经纪",
            "赞助经理",
            "v4/npcs/portraits/sponsor-manager.png",
            "赞助表格刚发来，品牌方正在问今天的直播间能不能少一点抽象。",
            "商业味上桌时要给DD节目效果，也要让老粉看到你不是只会念口播。",
            "商业化和口碑会影响商业反噬风险。"
    );
    private static final NpcProfile PLATFORM_ANALYST = new NpcProfile(
            NpcType.MI_SHE.key(),
            "平台观察员",
            "平台分析师",
            "v4/npcs/portraits/platform-analyst.png",
            "算法面板安静得可疑，录播组说这种低压周最适合攒基本盘。",
            "今天先读风向：DD会追热闹，老粉看兑现，标题组别只会加感叹号。",
            "平台口味、当前阶段和粉丝结构会影响明日选择。"
    );

    private final VupService vupService;

    public NpcSpotlightService(VupService vupService) {
        this.vupService = vupService;
    }

    public NpcSpotlightDTO spotlight(Long userId) {
        Vup vup = vupService.requireActiveVup(userId);
        NpcProfile profile = selectProfile(vup);
        return new NpcSpotlightDTO(
                profile.npcKey(),
                profile.displayName(),
                profile.role(),
                profile.image(),
                profile.moodLine(),
                profile.advice(),
                profile.relevance()
        );
    }

    private NpcProfile selectProfile(Vup vup) {
        if (vup.getCommercialLevel() >= 35) {
            return SPONSOR_MANAGER;
        }
        if (vup.getUnicornFans() >= vup.getTrueFans() && vup.getUnicornFans() >= vup.getDdFans()) {
            return ROUTE_PROFILES.get("CYBER_GIRLFRIEND");
        }
        if (vup.getDdFans() >= 60 || "SOCIAL_COLLAB".equals(vup.getCurrentRoute())) {
            return ROUTE_PROFILES.get("SOCIAL_COLLAB");
        }
        if (vup.getWatchHeat() >= 65 || vup.getMemeLevel() >= 55) {
            return ROUTE_PROFILES.get("BLACK_RED_MAIN_STAGE");
        }
        return ROUTE_PROFILES.getOrDefault(vup.getCurrentRoute(), PLATFORM_ANALYST);
    }

    private record NpcProfile(
            String npcKey,
            String displayName,
            String role,
            String image,
            String moodLine,
            String advice,
            String relevance
    ) {
    }
}
