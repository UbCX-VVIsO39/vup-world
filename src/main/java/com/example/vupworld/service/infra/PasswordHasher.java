package com.example.vupworld.service.infra;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 使用 BCrypt 对密码进行加盐哈希。
     * username 参数仅为保持接口兼容，BCrypt 内部已自动生成随机盐，不再需要 username 参与哈希。
     */
    public String hash(String username, String password) {
        return encoder.encode(password);
    }

    /**
     * 校验明文密码是否与已存储的 BCrypt 哈希匹配。
     */
    public boolean matches(String rawPassword, String storedHash) {
        return storedHash != null && encoder.matches(rawPassword, storedHash);
    }
}
