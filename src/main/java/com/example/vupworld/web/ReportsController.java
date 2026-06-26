package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ReportDtos.DailyReportDTO;
import com.example.vupworld.service.report.ReportService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReportsController {
    private final VupService vupService;
    private final ReportService reportService;

    public ReportsController(VupService vupService, ReportService reportService) {
        this.vupService = vupService;
        this.reportService = reportService;
    }

    @GetMapping("/api/reports")
    public ApiResponse<List<DailyReportDTO>> reports(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("历史日报读取成功，录播组有证据了。", reportService.historyReports(vupService.requireActiveVup(userId)));
    }
}
