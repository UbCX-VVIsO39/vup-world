package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ActionDtos.ActionOptionDTO;
import com.example.vupworld.dto.ActionDtos.CancelActionRequest;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.ActionDtos.OffStreamOptionDTO;
import com.example.vupworld.dto.ActionDtos.SubmitActionRequest;
import com.example.vupworld.dto.ActionDtos.SubmitOffStreamRequest;
import com.example.vupworld.dto.ActionDtos.SubmitScheduleRequest;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.service.ActionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ActionController {
    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping("/api/actions")
    public ApiResponse<List<ActionOptionDTO>> actions(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("今日行动列表读取成功。", actionService.listActions(userId));
    }

    @PostMapping("/api/day/action")
    public ApiResponse<DayResultDTO> submitAction(@Valid @RequestBody SubmitActionRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("今日日动已结算，日报施工完成。", actionService.submitAction(userId, request));
    }

    @PostMapping("/api/day/schedule")
    public ApiResponse<DayResultDTO> submitSchedule(@Valid @RequestBody SubmitScheduleRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("今日排班已结算，日报施工完成。", actionService.submitSchedule(userId, request));
    }

    @PostMapping("/api/day/action/cancel")
    public ApiResponse<DaySessionDTO> cancelAction(@Valid @RequestBody CancelActionRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("直播企划已取消，今天还能稳一手别的。", actionService.cancelPendingAction(userId, request));
    }

    @GetMapping("/api/offstream/options")
    public ApiResponse<List<OffStreamOptionDTO>> getOffStreamOptions(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("下播行动列表读取成功。", actionService.getOffStreamOptions(userId));
    }

    @PostMapping("/api/offstream/action")
    public ApiResponse<DayResultDTO> submitOffStreamAction(@Valid @RequestBody SubmitOffStreamRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("下播行动已结算。", actionService.submitOffStreamAction(userId, request.offStreamType()));
    }

    @PostMapping("/api/offstream/skip")
    public ApiResponse<DayResultDTO> skipOffStream(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("已跳过下播行动。", actionService.skipOffStream(userId));
    }
}
