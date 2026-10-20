package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인가 코드를 클라이언트로 돌려보내면서 로그인 세션을 끝낸다(1회용 로그인).
 * 세션이 남아 있으면 다른 사람이 만든 인가 주소를 열었을 때 확인 화면 없이 코드가 나가므로,
 * 연결할 때마다 연결 확인 화면과 카카오 로그인을 다시 거치게 한다.
 * 리다이렉트 형식은 인가 서버 기본 응답과 같다(code, state).
 */
public class McpOneTimeAuthorizationResponseHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthorizationCodeRequestAuthenticationToken result =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(result.getRedirectUri())
                .queryParam(OAuth2ParameterNames.CODE, result.getAuthorizationCode().getTokenValue());
        if (StringUtils.hasText(result.getState())) {
            uri.queryParam(OAuth2ParameterNames.STATE, UriUtils.encode(result.getState(), StandardCharsets.UTF_8));
        }
        endLoginSession(request);
        redirectStrategy.sendRedirect(request, response, uri.build(true).toUriString());
    }

    private static void endLoginSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
