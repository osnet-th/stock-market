package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * 등록된 클라이언트(CIMD·DCR)에 앱의 연결 정책을 덧씌운다.
 * <ul>
 *   <li>토큰: access token 1시간, refresh token 30일, refresh token은 쓸 때마다 교체</li>
 *   <li>PKCE 필수, 승인 확인은 카카오 로그인 화면에서 한다(인가 서버 기본 승인 화면은 쓰지 않는다)</li>
 *   <li>리다이렉트: claude.ai 콜백과 loopback(Claude Code)만 남긴다</li>
 *   <li>클라이언트 인증: 서버가 외부 키 주소(jwks_uri)를 받아 오게 되는 private_key_jwt·client_secret_jwt는 뺀다
 *       (Claude는 PKCE 공개 클라이언트 none을 쓴다)</li>
 *   <li>CIMD(URL 형태 client_id): Claude 도메인의 https 문서만 조회한다(서버가 임의 주소로 요청을 보내지 않게)</li>
 *   <li>CIMD 클라이언트는 id가 문서 URL이고 id 조회는 캐시만 본다. 캐시가 만료돼도 토큰 조회·refresh가 되도록
 *       id 조회가 비면 문서 URL로 다시 찾는다</li>
 * </ul>
 */
@RequiredArgsConstructor
public class McpClientPolicyRepository implements RegisteredClientRepository {

    static final String CLAUDE_CALLBACK = "https://claude.ai/api/mcp/auth_callback";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");
    /** CIMD 문서를 받아 올 수 있는 도메인(하위 도메인 포함) */
    private static final Set<String> CIMD_ALLOWED_DOMAINS = Set.of("claude.ai", "claude.com", "anthropic.com");
    private static final Set<ClientAuthenticationMethod> JWT_AUTH_METHODS = Set.of(
            ClientAuthenticationMethod.PRIVATE_KEY_JWT, ClientAuthenticationMethod.CLIENT_SECRET_JWT);
    private static final String HTTPS = "https";
    private static final String URL_PREFIX = "http";

    private static final TokenSettings TOKEN_SETTINGS = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofHours(1))
            .refreshTokenTimeToLive(Duration.ofDays(30))
            .reuseRefreshTokens(false)
            .build();

    private final RegisteredClientRepository delegate;

    @Override
    public void save(RegisteredClient registeredClient) {
        RegisteredClient applied = applyPolicy(registeredClient);
        if (applied == null) {
            throw new IllegalArgumentException("허용하지 않는 리다이렉트 주소입니다: " + registeredClient.getRedirectUris());
        }
        delegate.save(applied);
    }

    @Override
    public RegisteredClient findById(String id) {
        if (!isAllowedClientId(id)) {
            return null;
        }
        RegisteredClient client = delegate.findById(id);
        if (client == null && isUrl(id)) {
            client = delegate.findByClientId(id);
        }
        return applyPolicy(client);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (!isAllowedClientId(clientId)) {
            return null;
        }
        return applyPolicy(delegate.findByClientId(clientId));
    }

    /** URL이 아닌 id(DCR)는 그대로 둔다. URL이면 Claude 도메인의 https만 허용한다 */
    static boolean isAllowedClientId(String clientId) {
        if (clientId == null || !isUrl(clientId)) {
            return true;
        }
        try {
            URI uri = URI.create(clientId);
            String host = uri.getHost();
            return HTTPS.equals(uri.getScheme()) && host != null && uri.getUserInfo() == null
                    && CIMD_ALLOWED_DOMAINS.stream().anyMatch(d -> host.equals(d) || host.endsWith("." + d));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isUrl(String id) {
        return id.regionMatches(true, 0, URL_PREFIX, 0, URL_PREFIX.length());
    }

    /** 허용 리다이렉트가 하나도 없으면 null(없는 클라이언트로 취급) */
    static RegisteredClient applyPolicy(RegisteredClient client) {
        if (client == null || client.getRedirectUris().stream().noneMatch(McpClientPolicyRepository::isAllowedRedirect)) {
            return null;
        }
        return RegisteredClient.from(client)
                .redirectUris(uris -> uris.removeIf(uri -> !isAllowedRedirect(uri)))
                .clientAuthenticationMethods(methods -> methods.removeAll(JWT_AUTH_METHODS))
                .tokenSettings(TOKEN_SETTINGS)
                .clientSettings(ClientSettings.withSettings(client.getClientSettings().getSettings())
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
    }

    static boolean isAllowedRedirect(String redirectUri) {
        if (CLAUDE_CALLBACK.equals(redirectUri)) {
            return true;
        }
        try {
            URI uri = URI.create(redirectUri);
            return "http".equals(uri.getScheme()) && uri.getHost() != null && LOOPBACK_HOSTS.contains(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
