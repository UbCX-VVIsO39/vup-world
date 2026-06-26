package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DanmakuDtos.DanmakuListDTO;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.DanmakuService;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/danmaku")
public class DanmakuController {
    private final DanmakuService danmakuService;
    private final VupService vupService;
    private final DayFlowService dayFlowService;

    public DanmakuController(DanmakuService danmakuService, VupService vupService, DayFlowService dayFlowService) {
        this.danmakuService = danmakuService;
        this.vupService = vupService;
        this.dayFlowService = dayFlowService;
    }

    @GetMapping("/current")
    public ApiResponse<DanmakuListDTO> getCurrentDanmaku(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        DaySession daySession = dayFlowService.requireCurrentSession(vup);
        return ApiResponse.ok("弹幕已加载", danmakuService.generateDanmakuReadOnly(vup, null, null, daySession));
    }
}
