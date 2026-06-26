package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.EndingDtos.EndingForecastDTO;
import com.example.vupworld.dto.EndingDtos.EndingReviewDTO;
import com.example.vupworld.service.ending.EndingForecastService;
import com.example.vupworld.service.ending.EndingService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ending")
public class EndingController {
    private final VupService vupService;
    private final EndingService endingService;
    private final EndingForecastService endingForecastService;

    public EndingController(
            VupService vupService,
            EndingService endingService,
            EndingForecastService endingForecastService
    ) {
        this.vupService = vupService;
        this.endingService = endingService;
        this.endingForecastService = endingForecastService;
    }

    @GetMapping("/forecast")
    public ApiResponse<EndingForecastDTO> forecast(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("结局预演读取成功，毕业照还没冲洗。", endingForecastService.forecast(userId));
    }

    @GetMapping("/review")
    public ApiResponse<EndingReviewDTO> review(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("结局复盘读取成功。", endingService.latestReview(vupService.requireActiveVup(userId)));
    }
}
