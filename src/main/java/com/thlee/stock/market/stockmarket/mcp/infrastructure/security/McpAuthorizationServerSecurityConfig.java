package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.thlee.stock.market.stockmarket.mcp.infrastructure.config.McpOAuthProperties;
import com.thlee.stock.market.stockmarket.mcp.infrastructure.config.McpPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.DelegatingRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdUrlValidator;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.DefaultClientIdMetadataDocumentResolver;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.DefaultClientMetadataValidator;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.List;

import static org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer;

/**
 * MCP 연결용 OAuth 인가 서버. 로그인은 카카오(/mcp-auth/login)로 하고, 발급 토큰의 sub는 userId다.
 * 클라이언트 등록은 CIMD(claude.ai 기본)와 동적 등록(DCR)을 받는다. 등록 클라이언트·인가·승인은 DB에 저장한다.
 * 이 체인만 로그인 유지용 세션을 쓴다. 기존 화면·REST 체인은 건드리지 않는다.
 */
@Configuration
public class McpAuthorizationServerSecurityConfig {

    public static final int ORDER = 2;

    @Bean
    @Order(ORDER)
    public SecurityFilterChain mcpAuthorizationServerSecurityFilterChain(
            HttpSecurity http, McpOAuthProperties properties) throws Exception {
        http
                .securityMatcher(McpPaths.AUTHORIZATION_SERVER)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(McpPaths.MCP_AUTH + "/**", McpPaths.KAKAO_CALLBACK).permitAll())
                .with(mcpAuthorizationServer(), mcp -> mcp
                        .dynamicClientRegistration(true)
                        .cimd(true)
                        // PKCE 공개 클라이언트(claude.ai CIMD)가 쓰는 "none"을 메타데이터에 알린다
                        .authorizationServer(server -> server.authorizationServerMetadataEndpoint(metadata -> metadata
                                .authorizationServerMetadataCustomizer(builder -> builder
                                        .tokenEndpointAuthenticationMethod("none")))))
                // 톰캣이 상대 경로 리다이렉트를 내부 주소(http)로 바꾸지 않게 공개 주소로 보낸다
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint(properties.getIssuer() + McpPaths.MCP_AUTH + "/login")));
        return http.build();
    }

    /**
     * 카카오 로그인 연결 화면(/mcp-auth/**, /oauth/kakao/mcp). 인가 서버 설정이 자기 엔드포인트로 경로를 좁혀도
     * 이 경로가 기존 체인으로 넘어가지 않게 따로 받는다. 로그인 결과는 인가 서버 체인과 같은 세션에 저장한다.
     */
    @Bean
    @Order(ORDER + 1)
    public SecurityFilterChain mcpLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(McpPaths.MCP_AUTH + "/**", McpPaths.KAKAO_CALLBACK)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public AuthorizationServerSettings mcpAuthorizationServerSettings(McpOAuthProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    /** 등록 클라이언트: DB(DCR 저장) + CIMD(문서 URL로 조회). 연결 정책을 덧씌운다 */
    @Bean
    public RegisteredClientRepository mcpRegisteredClientRepository(JdbcOperations jdbcOperations) {
        JdbcRegisteredClientRepository jdbc = new JdbcRegisteredClientRepository(jdbcOperations);
        ClientIdMetadataDocumentRegisteredClientRepository cimd = new ClientIdMetadataDocumentRegisteredClientRepository();
        cimd.setMetadataDocumentResolver(new DefaultClientIdMetadataDocumentResolver());
        cimd.setMetadataValidator(new DefaultClientMetadataValidator());
        cimd.setClientIdUrlValidator(new ClientIdUrlValidator());
        return new McpClientPolicyRepository(new DelegatingRegisteredClientRepository(List.of(cimd, jdbc), jdbc));
    }

    @Bean
    public OAuth2AuthorizationService mcpAuthorizationService(
            JdbcOperations jdbcOperations, RegisteredClientRepository mcpRegisteredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, mcpRegisteredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService mcpAuthorizationConsentService(
            JdbcOperations jdbcOperations, RegisteredClientRepository mcpRegisteredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, mcpRegisteredClientRepository);
    }

    /**
     * access token의 aud를 MCP 리소스(/mcp)로 고정한다. 클라이언트가 resource 파라미터를 보내지 않아도
     * 리소스 서버의 aud 검증을 통과하게 하기 위해서다. sub는 로그인 시 넣은 userId 그대로다.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> mcpAudienceTokenCustomizer(McpOAuthProperties properties) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getClaims().audience(List.of(properties.resourceUri()));
            }
        };
    }
}
