package com.example.vupworld.service;

import com.example.vupworld.service.infra.PasswordHasher;

import com.example.vupworld.common.GameException;
import com.example.vupworld.dto.AuthDtos.LoginRequest;
import com.example.vupworld.dto.AuthDtos.RegisterRequest;
import com.example.vupworld.dto.AuthDtos.UserDTO;
import com.example.vupworld.mapper.UserAccountMapper;
import com.example.vupworld.model.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String LOCAL_PLAYER_USERNAME = "local_player";
    private static final String LOCAL_PLAYER_PASSWORD = "local-player-local-save";

    private final UserAccountMapper userAccountMapper;
    private final PasswordHasher passwordHasher;

    public AuthService(UserAccountMapper userAccountMapper, PasswordHasher passwordHasher) {
        this.userAccountMapper = userAccountMapper;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        String username = requireText(request.username(), "用户名不能为空");
        String password = requireText(request.password(), "密码不能为空");
        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? username
                : request.nickname().trim();

        if (userAccountMapper.findByUsername(username) != null) {
            log.warn("Registration failed: username already exists: {}", username);
            throw new GameException("USERNAME_EXISTS", "这个账号已经出道过了，换个马甲吧。");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(username, password));
        user.setNickname(nickname);
        user.setCoin(0);
        user.setRestartCount(0);
        userAccountMapper.insert(user);
        log.info("User registered: id={}, username={}", user.getId(), username);
        return toDto(user);
    }

    public UserDTO login(LoginRequest request) {
        String username = requireText(request.username(), "用户名不能为空");
        String password = requireText(request.password(), "密码不能为空");
        UserAccount user = userAccountMapper.findByUsername(username);
        if (user == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            log.warn("Login failed for username: {}", username);
            throw new GameException("UNAUTHORIZED", "账号或密码不对，主播还没进场。");
        }
        log.info("User logged in: id={}, username={}", user.getId(), username);
        return toDto(user);
    }

    public UserDTO getUser(Long userId) {
        UserAccount user = userAccountMapper.findById(userId);
        if (user == null) {
            throw new GameException("UNAUTHORIZED", "登录状态失效，请重新登录。");
        }
        return toDto(user);
    }

    public UserDTO localPlayerIfExists() {
        UserAccount user = userAccountMapper.findByUsername(LOCAL_PLAYER_USERNAME);
        return user == null ? null : toDto(user);
    }

    public void bindSession(jakarta.servlet.http.HttpSession session, UserDTO user) {
        if (session != null && user != null) {
            session.setAttribute("USER_ID", user.id());
        }
    }

    @Transactional
    public UserDTO localPlayer(jakarta.servlet.http.HttpSession session) {
        UserAccount user = userAccountMapper.findByUsername(LOCAL_PLAYER_USERNAME);
        if (user == null) {
            user = new UserAccount();
            user.setUsername(LOCAL_PLAYER_USERNAME);
            user.setPasswordHash(passwordHasher.hash(LOCAL_PLAYER_USERNAME, LOCAL_PLAYER_PASSWORD));
            user.setNickname("本地玩家");
            user.setCoin(0);
            user.setRestartCount(0);
            userAccountMapper.insert(user);
            log.info("Local single-player account created: id={}", user.getId());
        }
        session.setAttribute("USER_ID", user.getId());
        return toDto(user);
    }

    private UserDTO toDto(UserAccount user) {
        return new UserDTO(user.getId(), user.getUsername(), user.getNickname(), user.getRestartCount());
    }

    /**
     * 游客快速注册，自动生成用户名和密码
     */
    public UserDTO guestRegister(jakarta.servlet.http.HttpSession session) {
        String guestName = "guest_" + System.currentTimeMillis() % 100000;
        String guestPass = "guest" + System.currentTimeMillis() % 10000;
        RegisterRequest req = new RegisterRequest(guestName, guestPass, "游客" + guestName.substring(6));
        UserDTO user = register(req);
        session.setAttribute("USER_ID", user.id());
        return user;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new GameException("CONFIG_FIELD_INVALID", message);
        }
        return value.trim();
    }
}
