package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcClaims;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcParser;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoOAuthProperties;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.KakaoTokenClient;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오 OAuth 로그인 유스케이스 처리
 */
@Service
public class KakaoOAuthService {

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";

    private final KakaoTokenClient kakaoTokenClient;
    private final OidcParser oidcParser;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthService(
        KakaoTokenClient kakaoTokenClient,
        OidcParser oidcParser,
        KakaoOAuthProperties properties
    ) {
        this.kakaoTokenClient = kakaoTokenClient;
        this.oidcParser = oidcParser;
        this.properties = properties;
    }

    /**
     * 카카오 로그인(인가) 화면 주소. 화면 로그인(login.html)과 같은 파라미터에 state를 더한다.
     */
    public String authorizeUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
            .queryParam("client_id", properties.getClientId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("state", state)
            .encode()
            .toUriString();
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
        return toLoginRequest(token);
    }

    /**
     * 기본 콜백이 아닌 주소(예: MCP 연결 로그인)로 받은 인가 코드로 OAuthLoginRequest 생성
     */
    public OAuthLoginRequest loginWithKakao(String authorizationCode, String redirectUri) {
        return toLoginRequest(kakaoTokenClient.issueToken(authorizationCode, redirectUri));
    }

    private OAuthLoginRequest toLoginRequest(KakaoTokenResponse token) {

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