package com.thlee.stock.market.stockmarket.logging.infrastructure.aspect;

import com.thlee.stock.market.stockmarket.logging.application.event.ApplicationLogEvent;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 모든 {@code @RestController} 메서드 호출을 자동 감사한다.
 *
 * <ul>
 *   <li>정상/예외 모두 AUDIT 이벤트 1건 발행 (status, durationMs 포함)</li>
 *   <li>예외 발생 시 ERROR 이벤트 추가 발행 — GlobalExceptionHandler 의 응답 생성 흐름은 그대로 유지하기 위해
 *       예외는 AOP 에서 catch 후 재던지기</li>
 *   <li>AOP self-invocation 함정 회피: 리스너 호출은 항상 {@link ApplicationEventPublisher} 경유</li>
 * </ul>
 *
 * 민감정보는 여기서 마스킹하지 않는다 — 적재 직전 {@code LogSanitizer} 가 일괄 처리.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApplicationLoggingAspect {

    private static final String UNKNOWN_REQUEST_ID = "unknown";

    private final ApplicationEventPublisher publisher;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Around("restController()")
    public Object auditRestController(ProceedingJoinPoint pjp) throws Throwable {
        Instant startAt = Instant.now();
        long startNanos = System.nanoTime();
        Throwable failure = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            try {
                long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                publishAudit(pjp, startAt, durationMs, result, failure);
                if (failure != null) {
                    publishError(pjp, startAt, failure);
                }
            } catch (Exception publishingFailure) {
                // 로깅 자체 실패가 본 기능을 막아서는 안 됨
                log.warn("Audit/Error 이벤트 발행 실패: {}", publishingFailure.getMessage());
            }
        }
    }

    private void publishAudit(ProceedingJoinPoint pjp,
                              Instant startAt,
                              long durationMs,
                              Object result,
                              Throwable failure) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", currentHttpMethod());
        payload.put("uri", currentUri());
        payload.put("handler", handlerName(pjp));
        payload.put("status", resolveStatus(result, failure));
        payload.put("durationMs", durationMs);

        publisher.publishEvent(new ApplicationLogEvent(
                LogDomain.AUDIT,
                startAt,
                currentUserId(),
                currentRequestId(),
                payload
        ));
    }

    private void publishError(ProceedingJoinPoint pjp, Instant startAt, Throwable t) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exceptionClass", t.getClass().getName());
        payload.put("message", String.valueOf(t.getMessage()));
        payload.put("handler", handlerName(pjp));
        payload.put("uri", currentUri());
        payload.put("method", currentHttpMethod());
        payload.put("stackTrace", renderStack(t));

        publisher.publishEvent(new ApplicationLogEvent(
                LogDomain.ERROR,
                startAt,
                currentUserId(),
                currentRequestId(),
                payload
        ));
    }

    private int resolveStatus(Object result, Throwable failure) {
        if (failure != null) {
            return 500;
        }
        if (result instanceof ResponseEntity<?> entity) {
            HttpStatusCode code = entity.getStatusCode();
            return code.value();
        }
        return 200;
    }

    private String handlerName(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String currentUri() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getRequestURI() : null;
    }

    private String currentHttpMethod() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getMethod() : null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal instanceof Long id ? id : null;
    }

    private String currentRequestId() {
        String id = MDC.get(RequestIdFilter.MDC_KEY);
        return id != null ? id : UNKNOWN_REQUEST_ID;
    }

    private String renderStack(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("\tat ").append(el).append('\n');
        }
        // sanitizer 가 20줄 상한 적용
        return sb.toString();
    }
}
