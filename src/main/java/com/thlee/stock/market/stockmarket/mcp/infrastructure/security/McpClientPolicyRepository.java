package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

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
 * </ul>
 */
public class McpClientPolicyRepository implements RegisteredClientRepository {

    static final String CLAUDE_CALLBACK = "https://claude.ai/api/mcp/auth_callback";
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private static final TokenSettings TOKEN_SETTINGS = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofHours(1))
            .refreshTokenTimeToLive(Duration.ofDays(30))
            .reuseRefreshTokens(false)
            .build();

    private final RegisteredClientRepository delegate;

    public McpClientPolicyRepository(RegisteredClientRepository delegate) {
        this.delegate = delegate;
    }

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
        return applyPolicy(delegate.findById(id));
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return applyPolicy(delegate.findByClientId(clientId));
    }

    /** 허용 리다이렉트가 하나도 없으면 null(없는 클라이언트로 취급) */
    static RegisteredClient applyPolicy(RegisteredClient client) {
        if (client == null || client.getRedirectUris().stream().noneMatch(McpClientPolicyRepository::isAllowedRedirect)) {
            return null;
        }
        return RegisteredClient.from(client)
                .redirectUris(uris -> uris.removeIf(uri -> !isAllowedRedirect(uri)))
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
