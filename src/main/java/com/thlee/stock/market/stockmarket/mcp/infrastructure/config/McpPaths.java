package com.thlee.stock.market.stockmarket.mcp.infrastructure.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MCP 연결에 쓰는 경로. 보안 체인과 공개 주소 필터가 같은 목록을 쓴다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpPaths {

    public static final String MCP = "/mcp";
    public static final String PROTECTED_RESOURCE_METADATA = "/.well-known/oauth-protected-resource";
    public static final String AUTHORIZATION_SERVER_METADATA = "/.well-known/oauth-authorization-server";
    public static final String OPENID_CONFIGURATION = "/.well-known/openid-configuration";
    public static final String OAUTH2 = "/oauth2";
    public static final String MCP_AUTH = "/mcp-auth";
    public static final String KAKAO_CALLBACK = "/oauth/kakao/mcp";

    /** 리소스 서버 체인이 받는 경로 */
    public static final String[] RESOURCE_SERVER = {MCP, MCP + "/**", PROTECTED_RESOURCE_METADATA + "/**"};

    /** 인가 서버 체인이 받는 경로 */
    public static final String[] AUTHORIZATION_SERVER = {
            OAUTH2 + "/**", AUTHORIZATION_SERVER_METADATA + "/**", OPENID_CONFIGURATION + "/**",
            MCP_AUTH + "/**", KAKAO_CALLBACK};

    /** 공개 주소 기준으로 요청 주소를 바꿀 경로 접두어 */
    public static boolean isMcpRelated(String path) {
        return path.equals(MCP) || path.startsWith(MCP + "/")
                || path.startsWith(PROTECTED_RESOURCE_METADATA)
                || path.startsWith(AUTHORIZATION_SERVER_METADATA)
                || path.startsWith(OPENID_CONFIGURATION)
                || path.startsWith(OAUTH2 + "/")
                || path.startsWith(MCP_AUTH + "/")
                || path.equals(KAKAO_CALLBACK);
    }
}
