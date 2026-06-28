package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ReportDtos.DailyReportDTO;
import com.example.vupworld.dto.ReportDtos.TimelineDTO;
import com.example.vupworld.service.report.ReportService;
import com.example.vupworld.service.core.VupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/api/reports/timeline")
    public ApiResponse<TimelineDTO> timeline(@RequestParam Long vupId, HttpSession session) {
        SessionSupport.requireUserId(session);
        return ApiResponse.ok("本局时间线回放读取成功，30天关键决策点已就绪。", reportService.timeline(vupId));
    }
}
