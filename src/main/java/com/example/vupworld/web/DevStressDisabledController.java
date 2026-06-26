package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.StressTestDtos.StressRunRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/stress")
@Profile("prod | (!dev & !test)")
public class DevStressDisabledController {

    @PostMapping("/run")
    public ApiResponse<Object> run(@RequestBody(required = false) StressRunRequest request) {
        return ApiResponse.error("DEV_TOOL_DISABLED", "压测工具只允许在 dev/test 环境使用，正式环境别让脚本代打。", null);
    }
}
