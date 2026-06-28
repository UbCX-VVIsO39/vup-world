package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.RiskToolDtos.CrisisAlertDTO;
import com.example.vupworld.dto.RiskToolDtos.RiskToolOptionDTO;
import com.example.vupworld.dto.RiskToolDtos.RiskToolResultDTO;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import com.example.vupworld.service.core.VupService;
import com.example.vupworld.service.risk.DebtService;
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
    private final DebtService debtService;
    private final VupService vupService;

    public RiskToolController(RiskToolService riskToolService, DebtService debtService, VupService vupService) {
        this.riskToolService = riskToolService;
        this.debtService = debtService;
        this.vupService = vupService;
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

    @GetMapping("/alerts")
    public ApiResponse<List<CrisisAlertDTO>> alerts(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        Long vupId = vupService.requireActiveVup(userId).getId();
        return ApiResponse.ok("危机倒计时读取成功，先看红色再排明天。", debtService.crisisAlerts(vupId));
    }
}
