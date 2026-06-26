package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.RebirthDtos.RestartRequest;
import com.example.vupworld.dto.RebirthDtos.RestartResultDTO;
import com.example.vupworld.service.ending.RebirthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rebirth")
public class RebirthController {
    private final RebirthService rebirthService;

    public RebirthController(RebirthService rebirthService) {
        this.rebirthService = rebirthService;
    }

    @PostMapping("/restart")
    public ApiResponse<RestartResultDTO> restart(@Valid @RequestBody RestartRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("复活赛开场，第1天重新待机。", rebirthService.restart(userId, request));
    }
}
