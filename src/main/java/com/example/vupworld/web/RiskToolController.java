package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.RiskToolDtos.RiskToolOptionDTO;
import com.example.vupworld.dto.RiskToolDtos.RiskToolResultDTO;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import com.example.vupworld.service.risk.RiskToolService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/risk-tool")
public class RiskToolController {
    private final RiskToolService riskToolService;

    public RiskToolController(RiskToolService riskToolService) {
        this.riskToolService = riskToolService;
    }

    @GetMapping("/options")
    public ApiResponse<List<RiskToolOptionDTO>> options(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("米线工具箱读取成功，先看成本再保边界。", riskToolService.options(userId));
    }

    @PostMapping("/use")
    public ApiResponse<RiskToolResultDTO> use(@Valid @RequestBody UseRiskToolRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("米线工具使用成功，楼友先放半个法槌。", riskToolService.useTool(userId, request));
    }
}
