package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.thlee.stock.market.stockmarket.mcp.infrastructure.config.McpPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * MCP·OAuth 경로의 요청 주소를 공개 주소(issuer) 기준으로 바꾼다.
 * <p>
 * 터널→Nginx 구간이 http라 요청만 보면 http 주소가 나온다. 메타데이터·WWW-Authenticate·리다이렉트에 쓰는
 * 주소가 https 공개 주소가 되도록 scheme·host·port를 issuer 값으로 덮는다. 다른 경로는 건드리지 않는다.
 */
public class McpPublicOriginFilter extends OncePerRequestFilter {

    private final String scheme;
    private final String host;
    private final int port;

    public McpPublicOriginFilter(String issuer) {
        URI uri = URI.create(issuer);
        this.scheme = uri.getScheme();
        this.host = uri.getHost();
        this.port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !McpPaths.isMcpRelated(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new PublicOriginRequest(request), response);
    }

    private final class PublicOriginRequest extends HttpServletRequestWrapper {

        private PublicOriginRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public String getServerName() {
            return host;
        }

        @Override
        public int getServerPort() {
            return port;
        }

        @Override
        public boolean isSecure() {
            return "https".equals(scheme);
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer(scheme).append("://").append(host);
            if (!isDefaultPort()) {
                url.append(':').append(port);
            }
            return url.append(getRequestURI());
        }

        private boolean isDefaultPort() {
            return ("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80);
        }
    }
}
