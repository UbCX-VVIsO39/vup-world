package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DevDemoDtos.DemoResetRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetResult;
import com.example.vupworld.service.dev.DevDemoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.vupworld.web.SessionSupport.USER_ID;

@RestController
@RequestMapping("/api/friend/demo")
@Profile("!prod & (manual | test)")
public class FriendDemoController {
    private final DevDemoService devDemoService;

    public FriendDemoController(DevDemoService devDemoService) {
        this.devDemoService = devDemoService;
    }

    @PostMapping("/quick-start")
    public ApiResponse<DemoResetResult> quickStart(
            @RequestBody(required = false) DemoResetRequest request,
            HttpSession session
    ) {
        DemoResetResult result = devDemoService.reset(request);
        session.setAttribute(USER_ID, result.userId());
        return ApiResponse.ok("Friend demo quick start is ready.", result);
    }
}
