package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.BuzzDtos.BuzzBriefingDTO;
import com.example.vupworld.service.content.BuzzBriefingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buzz")
public class BuzzController {
    private final BuzzBriefingService buzzBriefingService;

    public BuzzController(BuzzBriefingService buzzBriefingService) {
        this.buzzBriefingService = buzzBriefingService;
    }

    @GetMapping("/briefing")
    public ApiResponse<BuzzBriefingDTO> briefing(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("热搜风向读取成功，楼友还在刷新。", buzzBriefingService.briefing(userId));
    }
}
