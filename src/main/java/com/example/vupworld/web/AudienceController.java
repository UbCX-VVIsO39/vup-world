package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.AudienceDtos.AudienceExpectationBoardDTO;
import com.example.vupworld.dto.AudienceDtos.FunAudienceProfileDTO;
import com.example.vupworld.service.fan.AudienceExpectationService;
import com.example.vupworld.service.fan.FunAudienceProfileService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audience")
public class AudienceController {
    private final FunAudienceProfileService funAudienceProfileService;
    private final AudienceExpectationService audienceExpectationService;

    public AudienceController(
            FunAudienceProfileService funAudienceProfileService,
            AudienceExpectationService audienceExpectationService
    ) {
        this.funAudienceProfileService = funAudienceProfileService;
        this.audienceExpectationService = audienceExpectationService;
    }

    @GetMapping("/fun-profile")
    public ApiResponse<FunAudienceProfileDTO> funProfile(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("乐子人画像读取成功，录播组正在对轴。", funAudienceProfileService.profile(userId));
    }

    @GetMapping("/expectation")
    public ApiResponse<AudienceExpectationBoardDTO> expectation(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("观众期待读取成功，老粉和DD正在校准标签。", audienceExpectationService.board(userId));
    }
}
