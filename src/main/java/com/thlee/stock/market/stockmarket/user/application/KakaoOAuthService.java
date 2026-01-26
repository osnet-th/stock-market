package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcClaims;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcParser;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoTokenClient;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import org.springframework.stereotype.Service;

/**
 * 카카오 OAuth 로그인 유스케이스 처리
 */
@Service
public class KakaoOAuthService {

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    private final KakaoTokenClient kakaoTokenClient;
    private final OidcParser oidcParser;

    public KakaoOAuthService(
        KakaoTokenClient kakaoTokenClient,
        OidcParser oidcParser
    ) {
        this.kakaoTokenClient = kakaoTokenClient;
        this.oidcParser = oidcParser;
    }

    /**
     * 카카오 인가 코드로 OAuthLoginRequest 생성
     *
     * @param authorizationCode 카카오 인가 코드
     * @return OAuthLoginRequest
     */
    public OAuthLoginRequest loginWithKakao(String authorizationCode) {
        // 1. 카카오 토큰 발급
        KakaoTokenResponse token = kakaoTokenClient.issueToken(authorizationCode);

        // 2. 카카오 사용자 정보 조회
        OidcClaims kakaoUser = oidcParser.parseIdToken(token.openId());

        // 3. OAuthLoginRequest 생성 및 반환
        return new OAuthLoginRequest(
            OAuthProvider.KAKAO,
            KAKAO_ISSUER,
            kakaoUser.subject(),
            kakaoUser.email()
        );
    }
}