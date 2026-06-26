package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DevDemoDtos.DemoFastForwardRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoRunScriptRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/demo")
@Profile("prod | (!dev & !test & !manual)")
public class DevDemoDisabledController {

    @PostMapping("/reset")
    public ApiResponse<Object> reset(@RequestBody DemoResetRequest request) {
        return disabled();
    }

    @PostMapping("/run-script")
    public ApiResponse<Object> runScript(@RequestBody DemoRunScriptRequest request) {
        return disabled();
    }

    @PostMapping("/fast-forward")
    public ApiResponse<Object> fastForward(@RequestBody(required = false) DemoFastForwardRequest request) {
        return disabled();
    }

    @GetMapping("/status")
    public ApiResponse<Object> status() {
        return disabled();
    }

    private ApiResponse<Object> disabled() {
        return ApiResponse.error("DEV_TOOL_DISABLED", "开发验证工具只允许在 dev/test/manual 环境使用，正式环境别让脚本代打。", null);
    }
}
