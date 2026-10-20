package com.thlee.stock.market.stockmarket.mcp.presentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * MCP 도구 호출자의 userId. access token의 sub(=인증 이름)에 userId가 들어 있다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpUserContext {

    public static Long currentUserId() {
        return userIdOf(SecurityContextHolder.getContext().getAuthentication());
    }

    static Long userIdOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new AuthenticationCredentialsNotFoundException("MCP 연결 인증이 필요합니다.");
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AuthenticationCredentialsNotFoundException("MCP 토큰의 사용자 정보가 올바르지 않습니다.");
        }
    }
}
