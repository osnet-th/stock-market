package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoOAuthClient;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoUserResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoTokenIssueFailed;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoUserInfoFetchFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthServiceTest {

    private KakaoOAuthClient kakaoOAuthClient;
    private KakaoOAuthService kakaoOAuthService;

    @BeforeEach
    void setUp() {
        kakaoOAuthClient = mock(KakaoOAuthClient.class);
        kakaoOAuthService = new KakaoOAuthService(kakaoOAuthClient);
    }

    @Test
    @DisplayName("인가 코드로 카카오 로그인 성공")
    void loginWithKakao_success() {
        // Given
        String authorizationCode = "test-code-12345";
        String accessToken = "kakao-access-token";
        Long kakaoId = 123456789L;
        String email = "test@kakao.com";
        String nickname = "테스트유저";

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
            accessToken,
            "bearer",
            "refresh-token",
            3600L,
            86400L
        );

        KakaoUserResponse.Profile profile = new KakaoUserResponse.Profile(nickname);
        KakaoUserResponse.KakaoAccount kakaoAccount = new KakaoUserResponse.KakaoAccount(email, profile);
        KakaoUserResponse userResponse = new KakaoUserResponse(kakaoId, kakaoAccount);

        given(kakaoOAuthClient.issueToken(authorizationCode)).willReturn(tokenResponse);
        given(kakaoOAuthClient.getUserInfo(accessToken)).willReturn(userResponse);

        // When
        OAuthLoginRequest request = kakaoOAuthService.loginWithKakao(authorizationCode);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(request.issuer()).isEqualTo("https://kauth.kakao.com");
        assertThat(request.subject()).isEqualTo("123456789");
        assertThat(request.email()).isEqualTo("test@kakao.com");
    }

    @Test
    @DisplayName("카카오 토큰 발급 실패 시 예외 전파")
    void loginWithKakao_tokenIssueFailed() {
        // Given
        String authorizationCode = "invalid-code";
        given(kakaoOAuthClient.issueToken(authorizationCode))
            .willThrow(new KakaoTokenIssueFailed("토큰 발급 실패"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.loginWithKakao(authorizationCode))
            .isInstanceOf(KakaoTokenIssueFailed.class)
            .hasMessageContaining("토큰 발급 실패");
    }

    @Test
    @DisplayName("카카오 사용자 정보 조회 실패 시 예외 전파")
    void loginWithKakao_userInfoFetchFailed() {
        // Given
        String authorizationCode = "test-code";
        String accessToken = "invalid-token";

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
            accessToken,
            "bearer",
            "refresh-token",
            3600L,
            86400L
        );

        given(kakaoOAuthClient.issueToken(authorizationCode)).willReturn(tokenResponse);
        given(kakaoOAuthClient.getUserInfo(accessToken))
            .willThrow(new KakaoUserInfoFetchFailed("사용자 정보 조회 실패"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.loginWithKakao(authorizationCode))
            .isInstanceOf(KakaoUserInfoFetchFailed.class)
            .hasMessageContaining("사용자 정보 조회 실패");
    }
}