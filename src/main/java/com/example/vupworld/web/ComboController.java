package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.ComboDtos.ComboDiscoveryDTO;
import com.example.vupworld.service.progression.ComboDiscoveryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/combo")
public class ComboController {
    private final ComboDiscoveryService comboDiscoveryService;

    public ComboController(ComboDiscoveryService comboDiscoveryService) {
        this.comboDiscoveryService = comboDiscoveryService;
    }

    @GetMapping("/discovery")
    public ApiResponse<ComboDiscoveryDTO> discovery(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("组合技发现板读取成功，行动历史已对齐。", comboDiscoveryService.discovery(userId));
    }
}
