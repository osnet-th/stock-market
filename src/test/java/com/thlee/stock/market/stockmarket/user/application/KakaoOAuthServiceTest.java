package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcClaims;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcParser;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoTokenClient;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoTokenIssueFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthServiceTest {

    private KakaoTokenClient kakaoTokenClient;
    private OidcParser oidcParser;
    private KakaoOAuthService kakaoOAuthService;

    @BeforeEach
    void setUp() {
        kakaoTokenClient = mock(KakaoTokenClient.class);
        oidcParser = mock(OidcParser.class);
        kakaoOAuthService = new KakaoOAuthService(kakaoTokenClient, oidcParser);
    }

    @Test
    @DisplayName("인가 코드로 카카오 로그인 성공")
    void loginWithKakao_success() {
        // Given
        String authorizationCode = "test-code-12345";
        String accessToken = "kakao-access-token";
        String idToken = "kakao-id-token";
        String subject = "123456789";
        String email = "test@kakao.com";

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
            accessToken,
            "bearer",
            "refresh-token",
            idToken,
            3600L,
            86400L
        );

        OidcClaims oidcClaims = new OidcClaims(
            "https://kauth.kakao.com",
            subject,
            email
        );

        given(kakaoTokenClient.issueToken(authorizationCode)).willReturn(tokenResponse);
        given(oidcParser.parseIdToken(idToken)).willReturn(oidcClaims);

        // When
        OAuthLoginRequest request = kakaoOAuthService.loginWithKakao(authorizationCode);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(request.issuer()).isEqualTo("https://kauth.kakao.com");
        assertThat(request.subject()).isEqualTo(subject);
        assertThat(request.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("카카오 토큰 발급 실패 시 예외 전파")
    void loginWithKakao_tokenIssueFailed() {
        // Given
        String authorizationCode = "invalid-code";
        given(kakaoTokenClient.issueToken(authorizationCode))
            .willThrow(new KakaoTokenIssueFailed("토큰 발급 실패"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.loginWithKakao(authorizationCode))
            .isInstanceOf(KakaoTokenIssueFailed.class)
            .hasMessageContaining("토큰 발급 실패");
    }

    @Test
    @DisplayName("카카오 ID Token 파싱 실패 시 예외 전파")
    void loginWithKakao_idTokenParseFailed() {
        // Given
        String authorizationCode = "test-code";
        String accessToken = "kakao-access-token";
        String idToken = "invalid-id-token";

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
            accessToken,
            "bearer",
            "refresh-token",
            idToken,
            3600L,
            86400L
        );

        given(kakaoTokenClient.issueToken(authorizationCode)).willReturn(tokenResponse);
        given(oidcParser.parseIdToken(idToken))
            .willThrow(new IllegalArgumentException("id_token 파싱 실패"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.loginWithKakao(authorizationCode))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id_token 파싱 실패");
    }
}