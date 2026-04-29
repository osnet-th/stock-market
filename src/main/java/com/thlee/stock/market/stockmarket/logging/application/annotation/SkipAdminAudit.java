package com.thlee.stock.market.stockmarket.logging.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code AdminGuardInterceptor} 의 {@code ADMIN_LOG_ACCESS} meta-audit 발행을 면제하는 표식.
 *
 * <p>대시보드 요약과 같이 admin 사용자의 home 진입마다 자동 호출되는 endpoint 에 부착해
 * audit 신호/잡음 비를 보존한다. 면제는 응답 {@code status < 400} 에 한정되며, 4xx/5xx 응답은
 * 본 어노테이션이 있어도 audit 발행을 유지한다(SOC2/ISO 27001 시그널 보존).
 *
 * <p>인가({@code preHandle}) 자체는 모든 admin path 에 동일하게 적용된다 — 본 어노테이션은
 * 권한 면제가 아니라 <b>audit 면제</b> 만 제어한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipAdminAudit {
}
