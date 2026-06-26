package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.DayPhase;
import com.example.vupworld.dto.MoodDtos.NpcInteractionResult;
import com.example.vupworld.dto.MoodDtos.StealLearnResult;
import com.example.vupworld.dto.NpcDtos.NpcRelationshipDTO;
import com.example.vupworld.dto.NpcDtos.NpcSpotlightDTO;
import com.example.vupworld.dto.NpcDtos.PlatformNPCDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.fan.NpcEventChainService;
import com.example.vupworld.service.fan.NpcRelationshipService;
import com.example.vupworld.service.fan.NpcSpotlightService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/npc")
public class NpcController {
    private final NpcSpotlightService npcSpotlightService;
    private final NpcRelationshipService npcRelationshipService;
    private final NpcEventChainService npcEventChainService;
    private final VupService vupService;
    private final DayFlowService dayFlowService;

    public NpcController(
            NpcSpotlightService npcSpotlightService,
            NpcRelationshipService npcRelationshipService,
            NpcEventChainService npcEventChainService,
            VupService vupService,
            DayFlowService dayFlowService
    ) {
        this.npcSpotlightService = npcSpotlightService;
        this.npcRelationshipService = npcRelationshipService;
        this.npcEventChainService = npcEventChainService;
        this.vupService = vupService;
        this.dayFlowService = dayFlowService;
    }

    @GetMapping("/spotlight")
    public ApiResponse<NpcSpotlightDTO> spotlight(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("同台情报台读取成功，同行生态轻轻飘过。", npcSpotlightService.spotlight(userId));
    }

    @GetMapping("/relationships")
    public ApiResponse<List<NpcRelationshipDTO>> getRelationships(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        return ApiResponse.ok("NPC亲密度列表已获取。", npcRelationshipService.getAllAffinity(vup));
    }

    /** 查询当前路线是否有可触发的NPC事件链事件 */
    @GetMapping("/chain-event")
    public ApiResponse<?> getChainEvent(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        var event = npcEventChainService.checkForChainEvent(vup, null);
        if (event.isPresent()) {
            var e = event.get();
            return ApiResponse.ok("发现NPC事件链事件。", Map.of(
                "hasEvent", true,
                "eventKey", e.eventKey,
                "npcKey", npcEventChainService.getNpcForCurrentRoute(vup),
                "title", e.title,
                "description", e.description,
                "effectKey", e.effectKey,
                "effectValue", e.effectValue
            ));
        }
        return ApiResponse.ok("暂无NPC事件链事件。", Map.of("hasEvent", false));
    }

    /** 标记NPC事件链事件已解决 */
    @PostMapping("/chain-event/resolve")
    public ApiResponse<?> resolveChainEvent(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        String npcKey = body.get("npcKey");
        String eventKey = body.get("eventKey");
        String effect = npcEventChainService.markResolved(vup, npcKey, eventKey);
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("resolved", true);
        if (effect != null && !effect.isEmpty()) {
            data.put("effect", effect);
        }
        return ApiResponse.ok("事件已解决。", data);
    }

    /** 平台：获取所有主播列表及互动状态 */
    @GetMapping("/platform")
    public ApiResponse<?> platform(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        List<NpcRelationshipDTO> relationships = npcRelationshipService.getAllAffinity(vup);
        List<PlatformNPCDTO> platformNpcs = new ArrayList<>();
        for (NpcRelationshipDTO rel : relationships) {
            boolean canSteal = npcRelationshipService.canStealLearn(vup, rel.key());
            platformNpcs.add(new PlatformNPCDTO(
                    rel.key(), rel.name(), rel.affinity(), rel.label(),
                    canSteal, canSteal
            ));
        }
        String stealBuff = npcRelationshipService.getActiveStealBuff(vup);
        return ApiResponse.ok("平台主播列表已获取。", Map.of(
                "npcs", platformNpcs,
                "stealBuff", stealBuff != null ? stealBuff : ""
        ));
    }

    /** 平台：发送弹幕或申请联动 */
    @PostMapping("/interact")
    public ApiResponse<NpcInteractionResult> interact(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        // 平台互动消耗今日主行动：先校验阶段与已选行动
        DaySession daySession = dayFlowService.requireCurrentSession(vup);
        if (!DayPhase.READY.name().equals(daySession.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的主行动已经用过了，平台互动算一次主行动。");
        }
        if (daySession.getSelectedAction() != null) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的主行动已经用过了，平台互动算一次主行动。");
        }
        String npcKey = body.get("npcKey");
        String interactionType = body.get("interactionType");
        NpcInteractionResult result = npcRelationshipService.interact(vup, npcKey, interactionType);
        // 互动成功，标记主行动已消耗并推进到场外阶段
        daySession.setSelectedAction("NPC_INTERACT");
        daySession.setPhase(DayPhase.OFF_STREAM_READY.name());
        dayFlowService.commitSessionAfterAction(daySession);
        return ApiResponse.ok("互动完成。", result);
    }

    /** 平台：偷学 */
    @PostMapping("/steal-learn")
    public ApiResponse<StealLearnResult> stealLearn(@RequestBody Map<String, String> body, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        // 平台互动消耗今日主行动：先校验阶段与已选行动
        DaySession daySession = dayFlowService.requireCurrentSession(vup);
        if (!DayPhase.READY.name().equals(daySession.getPhase())) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的主行动已经用过了，平台互动算一次主行动。");
        }
        if (daySession.getSelectedAction() != null) {
            throw new GameException("PHASE_NOT_ALLOWED", "今天的主行动已经用过了，平台互动算一次主行动。");
        }
        String npcKey = body.get("npcKey");
        StealLearnResult result = npcRelationshipService.stealLearn(vup, npcKey);
        // 偷学成功，标记主行动已消耗并推进到场外阶段
        daySession.setSelectedAction("NPC_INTERACT");
        daySession.setPhase(DayPhase.OFF_STREAM_READY.name());
        dayFlowService.commitSessionAfterAction(daySession);
        return ApiResponse.ok("偷学完成。", result);
    }
}
