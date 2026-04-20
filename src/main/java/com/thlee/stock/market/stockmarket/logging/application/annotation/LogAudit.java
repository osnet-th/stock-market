package com.thlee.stock.market.stockmarket.logging.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 감사(audit) 기록 옵션 제어용 장식자.
 *
 * 기본적으로 {@code ApplicationLoggingAspect} 는 모든 {@code @RestController} 메서드를 대상으로
 * 감사 이벤트를 남기므로, 특별한 제어가 필요 없으면 이 애노테이션을 붙일 필요가 없다.
 *
 * 이 애노테이션은 아래 두 가지 목적으로만 사용한다:
 * <ul>
 *   <li>{@link #includeBody()} — 요청 바디를 audit payload 에 포함할지 (기본 off, PII 보호)</li>
 *   <li>{@link #maskFields()} — 특정 필드를 추가로 마스킹</li>
 * </ul>
 *
 * 향후 {@code @LogAudit(disabled=true)} 로 opt-out 하는 용도도 열어둔다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAudit {

    boolean includeBody() default false;

    String[] maskFields() default {};
}