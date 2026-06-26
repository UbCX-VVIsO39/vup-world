package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.EventDtos.ChooseEventRequest;
import com.example.vupworld.dto.EventDtos.PendingEventDTO;
import com.example.vupworld.service.event.EventService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/pending")
    public ApiResponse<PendingEventDTO> pending(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("正式事件读取成功，看看今天是哪口锅回旋到账。", eventService.pending(userId));
    }

    @PostMapping("/choose")
    public ApiResponse<DayResultDTO> choose(@Valid @RequestBody ChooseEventRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("正式事件已处理，今天的日报可以写了。", eventService.choose(userId, request));
    }
}
