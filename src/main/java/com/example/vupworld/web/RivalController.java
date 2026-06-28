package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.NpcDtos.LeaderboardDTO;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.fan.RivalProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
public class RivalController {
    private final VupService vupService;
    private final RivalProgressService rivalProgressService;

    public RivalController(VupService vupService, RivalProgressService rivalProgressService) {
        this.vupService = vupService;
        this.rivalProgressService = rivalProgressService;
    }

    @GetMapping("/api/rivals")
    public ApiResponse<?> getRivals(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        var vup = vupService.requireActiveVup(userId);
        return ApiResponse.ok("同行竞争数据读取成功。", rivalProgressService.getRivals(vup));
    }

    @GetMapping("/api/leaderboard")
    public ApiResponse<LeaderboardDTO> leaderboard(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("排行榜读取成功，三榜数据已刷新。", rivalProgressService.leaderboard(userId));
    }
}
