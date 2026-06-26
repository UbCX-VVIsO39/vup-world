package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.AuthDtos.LoginRequest;
import com.example.vupworld.dto.AuthDtos.RegisterRequest;
import com.example.vupworld.dto.AuthDtos.UserDTO;
import com.example.vupworld.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.example.vupworld.web.SessionSupport.USER_ID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功，皮套还没动，账号先站稳。", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserDTO> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        UserDTO user = authService.login(request);
        session.setAttribute(USER_ID, user.id());
        return ApiResponse.ok("登录成功，主播进场。", user);
    }

    @GetMapping("/current")
    public ApiResponse<UserDTO> current(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("登录状态读取成功。", authService.getUser(userId));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok("已退出登录。", null);
    }

    @PostMapping("/guest")
    public ApiResponse<?> guestLogin(HttpSession session) {
        UserDTO user = authService.guestRegister(session);
        return ApiResponse.ok("游客模式已开启，注册账号可保存进度。", Map.of(
            "user", user,
            "isGuest", true,
            "message", "游客模式已开启，注册账号可保存进度"
        ));
    }
}
