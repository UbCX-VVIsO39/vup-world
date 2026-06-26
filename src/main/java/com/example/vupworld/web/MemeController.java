package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.MemeDtos.MemeLifecycleDTO;
import com.example.vupworld.service.content.MemeLifecycleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meme")
public class MemeController {
    private final MemeLifecycleService memeLifecycleService;

    public MemeController(MemeLifecycleService memeLifecycleService) {
        this.memeLifecycleService = memeLifecycleService;
    }

    @GetMapping("/lifecycle")
    public ApiResponse<MemeLifecycleDTO> lifecycle(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("梗生命周期读取成功，切片组已经排好时间轴。", memeLifecycleService.lifecycle(userId));
    }
}
