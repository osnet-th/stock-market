package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoOAuthClient;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoUserResponse;
import org.springframework.stereotype.Service;

/**
 * 카카오 OAuth 로그인 유스케이스 처리
 */
@Service
public class KakaoOAuthService {

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    private final KakaoOAuthClient kakaoOAuthClient;

    public KakaoOAuthService(
        KakaoOAuthClient kakaoOAuthClient
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
    }

    /**
     * 카카오 인가 코드로 OAuthLoginRequest 생성
     *
     * @param authorizationCode 카카오 인가 코드
     * @return OAuthLoginRequest
     */
    public OAuthLoginRequest loginWithKakao(String authorizationCode) {
        // 1. 카카오 토큰 발급
        KakaoTokenResponse token = kakaoOAuthClient.issueToken(authorizationCode);

        // 2. 카카오 사용자 정보 조회
        KakaoUserResponse kakaoUser = kakaoOAuthClient.getUserInfo(token.accessToken());

        // 3. OAuthLoginRequest 생성 및 반환
        return new OAuthLoginRequest(
            OAuthProvider.KAKAO,
            KAKAO_ISSUER,
            kakaoUser.id().toString(),
            kakaoUser.getEmail()
        );
    }
}