package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.FanTopicDtos.ChooseFanTopicRequest;
import com.example.vupworld.dto.FanTopicDtos.FanTopicOptionDTO;
import com.example.vupworld.dto.FanTopicDtos.FanTopicResultDTO;
import com.example.vupworld.service.fan.FanTopicService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fan-topic")
public class FanTopicController {
    private final FanTopicService fanTopicService;

    public FanTopicController(FanTopicService fanTopicService) {
        this.fanTopicService = fanTopicService;
    }

    @GetMapping("/options")
    public ApiResponse<List<FanTopicOptionDTO>> options(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("粉丝群议题读取成功，群友开始递小作文草稿。", fanTopicService.options(userId));
    }

    @PostMapping("/choose")
    public ApiResponse<FanTopicResultDTO> choose(@Valid @RequestBody ChooseFanTopicRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("粉丝群议题已处理，群友暂时收了小作文。", fanTopicService.choose(userId, request));
    }
}
