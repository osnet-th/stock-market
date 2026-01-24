package com.thlee.stock.market.stockmarket.user.presentation;

import com.thlee.stock.market.stockmarket.user.application.OAuthLoginService;
import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping("/oauth")
public class AuthController {

    private final OAuthLoginService oauthLoginService;

    public AuthController(OAuthLoginService oauthLoginService) {
        this.oauthLoginService = oauthLoginService;
    }

    /**
     * 카카오 OAuth 콜백 엔드포인트
     *
     * @param code 카카오 인가 코드
     * @return OAuthLoginResponse
     */
    @GetMapping("/kakao")
    public ResponseEntity<OAuthLoginResponse> kakaoCallback(@RequestParam String code) {
        OAuthLoginResponse response = oauthLoginService.loginWithKakao(code);
        return ResponseEntity.ok(response);
    }
}