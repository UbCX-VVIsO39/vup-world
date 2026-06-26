package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ActionDtos.ChooseTitleRequest;
import com.example.vupworld.dto.ActionDtos.DayResultDTO;
import com.example.vupworld.dto.ActionDtos.RerollTitleRequest;
import com.example.vupworld.dto.ActionDtos.StreamPlanOptionDTO;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.dto.ActionDtos.TitleRerollResultDTO;
import com.example.vupworld.service.TitleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private final TitleService titleService;

    public StreamController(TitleService titleService) {
        this.titleService = titleService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<StreamPlanOptionDTO>> plans(HttpSession session) {
        SessionSupport.requireUserId(session);
        return ApiResponse.ok("直播企划列表读取成功。", titleService.streamPlans());
    }

    @GetMapping("/titles")
    public ApiResponse<List<TitleOptionDTO>> titles(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("标题候选读取成功。", titleService.savedTitleCandidates(userId));
    }

    @PostMapping("/titles/reroll")
    public ApiResponse<TitleRerollResultDTO> rerollTitles(@Valid @RequestBody RerollTitleRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("标题换了一批，灵感-1。", titleService.rerollTitles(userId, request));
    }

    @PostMapping("/title/choose")
    public ApiResponse<DayResultDTO> chooseTitle(@Valid @RequestBody ChooseTitleRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("标题确认成功，今日直播开始结算。", titleService.chooseTitle(userId, request));
    }
}
