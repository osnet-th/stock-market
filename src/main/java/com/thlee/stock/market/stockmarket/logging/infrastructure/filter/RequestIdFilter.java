package com.thlee.stock.market.stockmarket.logging.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 전체에 걸쳐 추적 가능한 {@code requestId} 를 확정하는 필터.
 *
 * <ul>
 *   <li>클라이언트가 {@code X-Request-Id} 를 보냈으면 값 검증 후 재사용, 아니면 UUID 생성</li>
 *   <li>MDC({@code requestId}) 에 주입 — AOP/Listener 가 로그 이벤트에 전파</li>
 *   <li>응답 헤더 {@code X-Request-Id} 로도 노출 (디버깅 편의)</li>
 *   <li>{@link com.thlee.stock.market.stockmarket.infrastructure.security.jwt.JwtAuthenticationFilter} 앞단에 등록</li>
 * </ul>
 *
 * 스케줄러/비동기 진입점은 별도 컨텍스트 유틸({@code LoggingContext.forScheduler}) 로 requestId 를 세팅한다.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // 간단한 sanity check: 영숫자/하이픈/언더스코어만 허용, 길이 128자 이하
    private static final int MAX_INCOMING_LENGTH = 128;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER_NAME);
        if (incoming != null && isSafe(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isSafe(String value) {
        if (value.isEmpty() || value.length() > MAX_INCOMING_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '-' || c == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}