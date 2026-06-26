package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.AchievementDtos.AchievementProgressDTO;
import com.example.vupworld.dto.MoodDtos.PermanentUnlockDTO;
import com.example.vupworld.service.progression.AchievementService;
import com.example.vupworld.service.progression.PermanentUnlockService;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.model.Vup;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievement")
public class AchievementController {
    private final AchievementService achievementService;
    private final VupService vupService;
    private final PermanentUnlockService permanentUnlockService;

    public AchievementController(
            AchievementService achievementService,
            VupService vupService,
            PermanentUnlockService permanentUnlockService
    ) {
        this.achievementService = achievementService;
        this.vupService = vupService;
        this.permanentUnlockService = permanentUnlockService;
    }

    @GetMapping("/progress")
    public ApiResponse<AchievementProgressDTO> getProgress(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Vup vup = vupService.requireActiveVup(userId);
        return ApiResponse.ok("成就进度已获取", achievementService.getAchievementProgress(userId, vup));
    }

    @GetMapping("/unlocks")
    public ApiResponse<List<PermanentUnlockDTO>> getUnlocks(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("永久解锁列表已获取", permanentUnlockService.getUnlocks(userId));
    }
}
