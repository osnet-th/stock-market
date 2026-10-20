package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;
import com.thlee.stock.market.stockmarket.user.domain.model.UserStatus;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpTokenCustomizerTest {

    private static final String RESOURCE = "https://hubth.test/mcp";

    @Test
    @DisplayName("T1: 가입을 마친 활성 사용자면 access token의 aud를 MCP 리소스로 고정한다")
    void activeUser() {
        // given
        UserRepository users = mock(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user(7L, UserRole.USER, UserStatus.ACTIVE)));
        JwtEncodingContext context = context("7");

        // when
        new McpTokenCustomizer(users, RESOURCE).customize(context);

        // then
        assertThat(context.getClaims().build().getAudience()).containsExactly(RESOURCE);
    }

    @Test
    @DisplayName("T2: 탈퇴·가입 미완료·없는 사용자·숫자가 아닌 sub면 토큰을 발급하지 않는다(invalid_grant)")
    void inactiveUser() {
        // given
        UserRepository users = mock(UserRepository.class);
        when(users.findById(8L)).thenReturn(Optional.of(user(8L, UserRole.SIGNING_USER, UserStatus.ACTIVE)));
        when(users.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.USER, UserStatus.DELETED)));
        when(users.findById(10L)).thenReturn(Optional.empty());
        McpTokenCustomizer customizer = new McpTokenCustomizer(users, RESOURCE);

        // when / then
        for (String sub : List.of("8", "9", "10", "abc")) {
            assertThatThrownBy(() -> customizer.customize(context(sub)))
                    .isInstanceOf(OAuth2AuthenticationException.class)
                    .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                            .isEqualTo("invalid_grant"));
        }
    }

    private static JwtEncodingContext context(String sub) {
        return JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder().subject(sub))
                .principal(UsernamePasswordAuthenticationToken.authenticated(sub, null, List.of()))
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
    }

    private static User user(Long id, UserRole role, UserStatus status) {
        return new User(id, "홍길동", null, null, List.of(1L), status, role, false, LocalDateTime.now(), null);
    }
}
