package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.DevDemoDtos.DemoFastForwardRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoResetResult;
import com.example.vupworld.dto.DevDemoDtos.DemoRunScriptRequest;
import com.example.vupworld.dto.DevDemoDtos.DemoRunScriptResult;
import com.example.vupworld.dto.DevDemoDtos.DemoStatusResult;
import com.example.vupworld.service.dev.DevDemoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.vupworld.web.SessionSupport.USER_ID;

@RestController
@RequestMapping("/api/dev/demo")
@Profile("!prod & (dev | test | manual)")
public class DevDemoController {
    private final DevDemoService devDemoService;

    public DevDemoController(DevDemoService devDemoService) {
        this.devDemoService = devDemoService;
    }

    @PostMapping("/reset")
    public ApiResponse<DemoResetResult> reset(@RequestBody DemoResetRequest request, HttpSession session) {
        DemoResetResult result = devDemoService.reset(request);
        session.setAttribute(USER_ID, result.userId());
        return ApiResponse.ok("验证轮次已重置，标题组已待命。", result);
    }

    @PostMapping("/run-script")
    public ApiResponse<DemoRunScriptResult> runScript(@RequestBody DemoRunScriptRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("验证脚本已跑完，录播组证据链已生成。", devDemoService.runScript(userId, request));
    }

    @PostMapping("/fast-forward")
    public ApiResponse<DemoRunScriptResult> fastForward(@RequestBody(required = false) DemoFastForwardRequest request, HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("验证快进完成，可以直接看第30天复盘。", devDemoService.fastForward(userId, request));
    }

    @GetMapping("/status")
    public ApiResponse<DemoStatusResult> status(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("验证状态读取成功，验收台不用靠占卜。", devDemoService.status(userId));
    }
}
