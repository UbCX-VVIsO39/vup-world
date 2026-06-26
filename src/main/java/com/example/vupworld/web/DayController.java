package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.DayDtos.NextDayRequest;
import com.example.vupworld.service.core.DayService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/day")
public class DayController {
    private final VupService vupService;
    private final DayService dayService;

    public DayController(VupService vupService, DayService dayService) {
        this.vupService = vupService;
        this.dayService = dayService;
    }

    @GetMapping("/session")
    public ApiResponse<DaySessionDTO> session(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("当前日状态读取成功。", vupService.currentSession(userId));
    }

    @PostMapping("/next")
    public ApiResponse<DaySessionDTO> next(@Valid @RequestBody NextDayRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("进入下一天，标题组重新待机。", dayService.nextDay(userId, request));
    }
}
