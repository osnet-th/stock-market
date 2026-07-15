package com.thlee.stock.market.stockmarket.companyreport.presentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증 principal(Long userId) 안전 추출 헬퍼 (newsjournal 패턴)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompanyReportSecurityContext {

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new InsufficientAuthenticationException("로그인이 필요합니다.");
    }
}
