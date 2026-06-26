package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ReportDtos.DailyReportDTO;
import com.example.vupworld.service.report.ReportService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final VupService vupService;
    private final ReportService reportService;

    public ReportController(VupService vupService, ReportService reportService) {
        this.vupService = vupService;
        this.reportService = reportService;
    }

    @GetMapping("/today")
    public ApiResponse<DailyReportDTO> today(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("今日日报读取成功。", reportService.todayReport(vupService.requireActiveVup(userId)));
    }
}
