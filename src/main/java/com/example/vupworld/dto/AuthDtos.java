package com.example.vupworld.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空") @Size(min = 2, max = 64, message = "用户名长度2-64") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 4, max = 128, message = "密码长度4-128") String password,
            @NotBlank(message = "昵称不能为空") @Size(min = 1, max = 64, message = "昵称长度1-64") String nickname
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record UserDTO(Long id, String username, String nickname, int restartCount) {
    }
}
