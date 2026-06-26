package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.MoodDtos.MoodResult;
import com.example.vupworld.dto.MoodDtos.StrategyAdviceDTO;
import com.example.vupworld.dto.VupDtos.CreateVupRequest;
import com.example.vupworld.dto.VupDtos.VupStateDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.MoodService;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.progression.StrategyAdviceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vup")
public class VupController {
    private final VupService vupService;
    private final MoodService moodService;
    private final StrategyAdviceService strategyAdviceService;
    private final DayFlowService dayFlowService;

    public VupController(
            VupService vupService,
            MoodService moodService,
            StrategyAdviceService strategyAdviceService,
            DayFlowService dayFlowService
    ) {
        this.vupService = vupService;
        this.moodService = moodService;
        this.strategyAdviceService = strategyAdviceService;
        this.dayFlowService = dayFlowService;
    }

    @PostMapping("/create")
    public ApiResponse<VupStateDTO> create(@Valid @RequestBody CreateVupRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("VUP创建成功或已继续当前进度。", vupService.createOrResumeVup(userId, request));
    }

    @GetMapping("/current")
    public ApiResponse<VupStateDTO> current(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("当前VUP状态读取成功。", vupService.currentState(userId));
    }

    @GetMapping("/mood")
    public ApiResponse<MoodResult> getMood(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        DaySession daySession = dayFlowService.requireCurrentSession(vup);
        return ApiResponse.ok("心情信息读取成功。", moodService.getMood(vup, daySession));
    }

    @GetMapping("/advice")
    public ApiResponse<List<StrategyAdviceDTO>> getAdvice(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        DaySession daySession = dayFlowService.requireCurrentSession(vup);
        return ApiResponse.ok("策略建议读取成功。", strategyAdviceService.getAdvice(vup, daySession));
    }
}
