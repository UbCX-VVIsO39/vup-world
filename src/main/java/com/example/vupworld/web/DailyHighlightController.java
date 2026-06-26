package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DailyHighlightDtos.HighlightListDTO;
import com.example.vupworld.service.progression.DailyHighlightService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.model.Vup;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/highlight")
public class DailyHighlightController {
    private final DailyHighlightService dailyHighlightService;
    private final VupService vupService;

    public DailyHighlightController(DailyHighlightService dailyHighlightService, VupService vupService) {
        this.dailyHighlightService = dailyHighlightService;
        this.vupService = vupService;
    }

    @GetMapping("/list")
    public ApiResponse<HighlightListDTO> getHighlights(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        return ApiResponse.ok("精彩时刻列表", dailyHighlightService.getHighlights(vup.getId()));
    }
}
