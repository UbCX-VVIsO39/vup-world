package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.SystemDtos.ConfigCheckDTO;
import com.example.vupworld.service.SystemConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemConfigService systemConfigService;

    public SystemController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 前端在 DOMContentLoaded 时（登录前）调用此接口做配置门控，
     * 因此允许匿名访问，但未登录时只返回基础通过/失败信息，
     * 不暴露答辩证据、开发工具等内部细节。
     */
    @GetMapping("/config-check")
    public ApiResponse<ConfigCheckDTO> configCheck(HttpSession session) {
        ConfigCheckDTO full = systemConfigService.checkP0();
        Object userId = session.getAttribute(SessionSupport.USER_ID);
        if (userId == null) {
            ConfigCheckDTO stripped = new ConfigCheckDTO(
                    full.status(),
                    full.checkedAt(),
                    full.categories(),
                    full.fatalErrors(),
                    full.warnings(),
                    full.defenseEvidence(),
                    null,
                    full.canPlayP0()
            );
            return ApiResponse.ok("配置检查完成，可以开始营业。", stripped);
        }
        return ApiResponse.ok("配置检查完成，可以开始营业。", full);
    }
}
