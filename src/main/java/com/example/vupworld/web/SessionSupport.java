package com.example.vupworld.web;

import com.example.vupworld.common.GameException;
import jakarta.servlet.http.HttpSession;

public final class SessionSupport {
    public static final String USER_ID = "USER_ID";

    private SessionSupport() {
    }

    public static Long requireUserId(HttpSession session) {
        Object value = session.getAttribute(USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        throw new GameException("UNAUTHORIZED", "请先登录。");
    }
}
