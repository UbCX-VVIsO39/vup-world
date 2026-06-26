package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DevDemoDtos.DemoResetRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friend/demo")
@Profile("prod | (!manual & !test)")
public class FriendDemoDisabledController {

    @PostMapping("/quick-start")
    public ApiResponse<Object> quickStart(@RequestBody(required = false) DemoResetRequest request) {
        return ApiResponse.error(
                "DEV_TOOL_DISABLED",
                "Friend demo quick start is only available in manual/test profiles.",
                null
        );
    }
}
