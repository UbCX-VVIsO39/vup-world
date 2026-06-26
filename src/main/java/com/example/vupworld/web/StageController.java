package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.StageDtos.StageBriefingDTO;
import com.example.vupworld.service.progression.StageBriefingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stage")
public class StageController {
    private final StageBriefingService stageBriefingService;

    public StageController(StageBriefingService stageBriefingService) {
        this.stageBriefingService = stageBriefingService;
    }

    @GetMapping("/briefing")
    public ApiResponse<StageBriefingDTO> briefing(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("阶段复盘台读取成功，日报组提前把风向板擦干净了。", stageBriefingService.briefing(userId));
    }
}
