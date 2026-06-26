package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.service.progression.CareerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
public class CareerController {
    private final CareerService careerService;

    public CareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping("/api/career/progress")
    public ApiResponse<?> getCareerProgress(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("查询成功", careerService.getProgress(userId));
    }

    @GetMapping("/api/career/inheritance")
    public ApiResponse<?> getCareerInheritance(HttpSession session,
                                               @RequestParam(defaultValue = "0") int completedRuns,
                                               @RequestParam(required = false) String lastEnding) {
        SessionSupport.requireUserId(session);
        return ApiResponse.ok("查询成功", careerService.calculateInheritance(completedRuns, lastEnding));
    }
}
