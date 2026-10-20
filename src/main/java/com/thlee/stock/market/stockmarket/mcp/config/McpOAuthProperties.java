package com.thlee.stock.market.stockmarket.mcp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 연결 OAuth 설정 (mcp.oauth.*)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.oauth")
public class McpOAuthProperties {

    /** 공개 주소 (예: https://hubth.com). 끝의 '/'는 붙이지 않는다 */
    private String issuer;

    /** RS256 서명 키 (PKCS#8 PEM). 비어 있으면 기동 시 새로 만든다 */
    private String jwkPrivateKey;

    private String jwkKeyId;

    /** MCP 연결용 카카오 로그인 콜백 주소 */
    private String kakaoRedirectUri;

    /** MCP 리소스 식별자 ({issuer}/mcp) */
    public String resourceUri() {
        return issuer + McpPaths.MCP;
    }
}
