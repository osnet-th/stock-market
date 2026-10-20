package com.thlee.stock.market.stockmarket.mcp.presentation;

import com.thlee.stock.market.stockmarket.mcp.config.McpOAuthProperties;
import com.thlee.stock.market.stockmarket.mcp.config.McpPaths;
import com.thlee.stock.market.stockmarket.user.application.KakaoOAuthService;
import com.thlee.stock.market.stockmarket.user.application.OAuthLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MCP 연결 로그인. 인가 서버가 로그인이 필요할 때 보내는 화면이다.
 * <ol>
 *   <li>/mcp-auth/login: 어떤 클라이언트가 연결을 요청하는지 보여 주고 카카오 로그인으로 보낸다(연결 승인 확인)</li>
 *   <li>/oauth/kakao/mcp: 카카오 인가 코드를 교환하고, 가입을 마친 활성 사용자만 로그인시킨 뒤 원래 인가 요청으로 돌아간다.
 *       계정이 없거나 가입 미완료면 사용자를 만들지 않고 가입 안내를 보인다</li>
 * </ol>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class McpLoginController {

    static final String STATE_ATTRIBUTE = McpLoginController.class.getName() + ".STATE";
    private static final String USER_AUTHORITY = "ROLE_USER";

    private final KakaoOAuthService kakaoOAuthService;
    private final OAuthLoginService oauthLoginService;
    private final RegisteredClientRepository mcpRegisteredClientRepository;
    private final McpOAuthProperties properties;

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping(value = McpPaths.MCP_AUTH + "/login", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> login(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) {
            return page("연결 요청 없음", "<p>Claude 등에서 stock-market 연결을 시작한 뒤 다시 시도해 주세요.</p>");
        }
        String state = UUID.randomUUID().toString();
        request.getSession(true).setAttribute(STATE_ATTRIBUTE, state);
        String kakaoUrl = kakaoOAuthService.authorizeUrl(properties.getKakaoRedirectUri(), state);
        String clientName = HtmlUtils.htmlEscape(clientName(saved));
        return page("stock-market 연결",
                "<p><strong>" + clientName + "</strong>이(가) stock-market 데이터 조회를 요청합니다.</p>"
                        + "<p>연결하면 기업 리포트, S-RIM 적정주가, 포트폴리오, 배당, 경제지표, 뉴스를 조회할 수 있습니다. "
                        + "조회만 하고 데이터를 바꾸지 않습니다.</p>"
                        + "<p><a class=\"btn\" href=\"" + HtmlUtils.htmlEscape(kakaoUrl) + "\">카카오로 로그인하고 연결 허용</a></p>");
    }

    @GetMapping(value = McpPaths.KAKAO_CALLBACK, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> kakaoCallback(@RequestParam(required = false) String code,
                                                @RequestParam(required = false) String state,
                                                HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute(STATE_ATTRIBUTE);
        if (code == null || state == null || !state.equals(expected)) {
            return page("연결 실패", "<p>로그인 요청이 올바르지 않거나 만료되었습니다. Claude에서 연결을 다시 시작해 주세요.</p>");
        }
        session.removeAttribute(STATE_ATTRIBUTE);

        Optional<Long> userId;
        try {
            userId = oauthLoginService.findActiveUserId(
                    kakaoOAuthService.loginWithKakao(code, properties.getKakaoRedirectUri()));
        } catch (RuntimeException e) {
            log.warn("MCP 연결 카카오 로그인 실패: {}", e.getMessage());
            return page("연결 실패", "<p>카카오 로그인에 실패했습니다. Claude에서 연결을 다시 시작해 주세요.</p>");
        }
        if (userId.isEmpty()) {
            return page("가입이 필요합니다",
                    "<p>stock-market에 가입한 계정만 연결할 수 있습니다. 앱에서 카카오 로그인과 가입을 마친 뒤 다시 연결해 주세요.</p>");
        }

        signIn(userId.get(), request, response);
        SavedRequest saved = requestCache.getRequest(request, response);
        String target = saved != null ? saved.getRedirectUrl() : properties.getIssuer();
        return ResponseEntity.status(302).header("Location", target).build();
    }

    /** 세션 고정을 막고 principal=userId로 로그인시킨다. 인가 코드를 발급하면 이 세션은 끝난다 */
    private void signIn(Long userId, HttpServletRequest request, HttpServletResponse response) {
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                String.valueOf(userId), null, List.of(new SimpleGrantedAuthority(USER_AUTHORITY))));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private String clientName(SavedRequest saved) {
        String[] clientIds = saved.getParameterValues("client_id");
        if (clientIds == null || clientIds.length == 0) {
            return "알 수 없는 클라이언트";
        }
        try {
            RegisteredClient client = mcpRegisteredClientRepository.findByClientId(clientIds[0]);
            return client != null ? client.getClientName() : clientIds[0];
        } catch (RuntimeException e) {
            log.warn("MCP 연결 클라이언트 조회 실패: {}", e.getMessage());
            return clientIds[0];
        }
    }

    private static ResponseEntity<String> page(String title, String body) {
        String html = "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + title + "</title><style>"
                + "body{font-family:system-ui,sans-serif;max-width:480px;margin:48px auto;padding:0 16px;color:#1f2937}"
                + ".btn{display:inline-block;background:#FEE500;color:#191919;padding:12px 18px;border-radius:8px;"
                + "text-decoration:none;font-weight:600}</style></head><body><h2>" + title + "</h2>" + body
                + "</body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
