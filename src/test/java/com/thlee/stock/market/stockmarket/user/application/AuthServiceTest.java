package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.TokenRefreshRequest;
import com.thlee.stock.market.stockmarket.user.application.dto.TokenRefreshResponse;
import com.thlee.stock.market.stockmarket.user.domain.model.*;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jwtTokenProvider, userRepository);
    }

    @Test
    @DisplayName("유효한 refresh token으로 토큰 갱신 성공")
    void refreshToken_WithValidToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;
        User user = User.createSigning();

        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(refreshToken)).willReturn(false);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(userId, UserRole.SIGNING_USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(userId))
                .willReturn("new-refresh-token");

        // When
        TokenRefreshResponse response = authService.refreshToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("유효하지 않은 refresh token으로 토큰 갱신 실패")
    void refreshToken_WithInvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(invalidToken);

        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은 토큰");
    }

    @Test
    @DisplayName("만료된 refresh token으로 토큰 갱신 실패")
    void refreshToken_WithExpiredToken_ThrowsException() {
        // Given
        String expiredToken = "expired-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(expiredToken);

        given(jwtTokenProvider.validateToken(expiredToken)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(expiredToken)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("만료된 토큰");
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 refresh token으로 토큰 갱신 실패")
    void refreshToken_WithNonExistentUser_ThrowsException() {
        // Given
        String refreshToken = "valid-refresh-token";
        Long userId = 999L;
        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isTokenExpired(refreshToken)).willReturn(false);
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}