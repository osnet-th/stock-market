package com.thlee.stock.market.stockmarket.mcp.presentation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpUserContextTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("U1: 토큰 sub가 숫자면 그 값을 userId로 쓴다")
    void numericSubject() {
        // given
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("42", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Long userId = McpUserContext.currentUserId();

        // then
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("U2: 인증이 없으면 인증 오류다")
    void noAuthentication() {
        assertThatThrownBy(McpUserContext::currentUserId)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("U2: sub가 숫자가 아니면 인증 오류다")
    void nonNumericSubject() {
        // given
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("kakao-user", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when / then
        assertThatThrownBy(McpUserContext::currentUserId)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
