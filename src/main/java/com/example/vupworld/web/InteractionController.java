package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.InteractionDtos.ChooseInteractionRequest;
import com.example.vupworld.dto.InteractionDtos.PendingInteractionDTO;
import com.example.vupworld.service.event.InteractionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {
    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/pending")
    public ApiResponse<PendingInteractionDTO> pending(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("直播现场读取成功，看看弹幕今天递了什么话筒。", interactionService.pending(userId));
    }

    @PostMapping("/choose")
    public ApiResponse<DayResultDTO> choose(@Valid @RequestBody ChooseInteractionRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("直播现场已稳住，日报组开始写小作文。", interactionService.choose(userId, request));
    }
}
