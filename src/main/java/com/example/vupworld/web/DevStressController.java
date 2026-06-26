package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.StressTestDtos.StressRunRequest;
import com.example.vupworld.dto.StressTestDtos.StressRunResult;
import com.example.vupworld.service.dev.DevStressService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/stress")
@Profile("!prod & (dev | test)")
public class DevStressController {
    private final DevStressService devStressService;

    public DevStressController(DevStressService devStressService) {
        this.devStressService = devStressService;
    }

    @PostMapping("/run")
    public ApiResponse<StressRunResult> run(@RequestBody(required = false) StressRunRequest request) {
        return ApiResponse.ok("压测摘要已生成，数值组可以开始看分布。", devStressService.run(request));
    }
}
