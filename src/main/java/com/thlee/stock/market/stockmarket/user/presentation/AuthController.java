package com.thlee.stock.market.stockmarket.user.presentation;

import com.thlee.stock.market.stockmarket.user.application.OAuthLoginService;
import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginResponse;
import com.thlee.stock.market.stockmarket.user.application.dto.SignupCompleteRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping
public class AuthController {

    private final OAuthLoginService oauthLoginService;

    public AuthController(OAuthLoginService oauthLoginService) {
        this.oauthLoginService = oauthLoginService;
    }

    /**
     * 카카오 OAuth 콜백 엔드포인트
     * 로그인 처리 후 토큰 정보를 쿼리 파라미터로 담아 대시보드로 리다이렉트
     *
     * @param code 카카오 인가 코드
     */
    @GetMapping("/oauth/kakao")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code) {
        OAuthLoginResponse response = oauthLoginService.loginWithKakao(code);

        String redirectUrl = "/?token=" + response.accessToken()
                + "&userId=" + response.userId()
                + "&role=" + response.role();

        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    /**
     * 소셜 로그인 가입 완료 엔드포인트
     * @param request 가입 완료 요청 데이터
     * @return 204 No Content
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> completeSignup(
            @RequestBody SignupCompleteRequest request
    ) {
        oauthLoginService.completeSignup(request);
        return ResponseEntity.noContent().build();
    }
}
