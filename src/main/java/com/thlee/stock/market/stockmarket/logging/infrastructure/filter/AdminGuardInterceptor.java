package com.thlee.stock.market.stockmarket.logging.infrastructure.filter;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.logging.infrastructure.config.AdminProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 운영자 전용 경로의 인가 가드.
 *
 * <ul>
 *   <li>userId 는 오직 {@link SecurityContextHolder} 의 Authentication principal 에서만 읽는다
 *       (header/param 금지 — userId forgery 방지)</li>
 *   <li>Anonymous 또는 null Authentication → 401</li>
 *   <li>화이트리스트 미포함 → 403</li>
 *   <li>화이트리스트 빈 리스트 → 전면 차단 (fail-close)</li>
 *   <li>인가 통과된 요청은 {@code afterCompletion} 에서 meta-audit 1건 기록
 *       ({@code ADMIN_LOG_ACCESS} business event, SOC2 CC6.1 / ISO 27001 A.12.4.3 패턴)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminGuardInterceptor implements HandlerInterceptor {

    private final AdminProperties adminProperties;
    private final DomainEventLogger domainEventLogger;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Long userId) || !adminProperties.userIds().contains(userId)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }

    /**
     * {@code preHandle} 이 {@code true} 를 반환했을 때만 Spring 이 호출한다 —
     * 즉 인가 통과된 관리자 접근만 meta-audit 기록 (실패 접근은 AUDIT 도메인의 AOP 가 이미 수집).
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (auth != null && auth.getPrincipal() instanceof Long id) ? id : null;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("adminAction", true);
            payload.put("method", request.getMethod());
            payload.put("uri", request.getRequestURI());
            payload.put("status", response.getStatus());
            if (ex != null) {
                payload.put("errorClass", ex.getClass().getSimpleName());
            }
            domainEventLogger.logBusiness("ADMIN_LOG_ACCESS", userId, payload);
        } catch (Exception publishFailure) {
            // 감사 실패가 응답 반환을 막아서는 안 됨
            log.warn("ADMIN_LOG_ACCESS meta-audit 발행 실패: {}", publishFailure.getMessage());
        }
    }
}