package com.thlee.stock.market.stockmarket.infrastructure.web;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsParseException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.exception.EcosApiException;
import com.thlee.stock.market.stockmarket.logging.application.event.ApplicationLogEvent;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.filter.RequestIdFilter;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartStatusCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception.DartApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import com.thlee.stock.market.stockmarket.user.domain.exception.UserDomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String UNKNOWN_REQUEST_ID = "unknown";

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 비즈니스 검증 실패 (잘못된 파라미터, 도메인 규칙 위반)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        publishError(e);
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /**
     * 데이터 무결성 위반 (UNIQUE 제약 충돌 등 동시성 이슈)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        publishError(e);
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT",
                "요청이 동시성 충돌로 처리되지 않았습니다. 다시 시도해주세요.");
    }

    /**
     * 도메인 예외
     */
    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<Map<String, Object>> handleUserDomain(UserDomainException e) {
        publishError(e);
        return buildResponse(HttpStatus.BAD_REQUEST, "DOMAIN_ERROR", e.getMessage());
    }

    /**
     * DART API 예외 (상태 코드별 HTTP 매핑)
     */
    @ExceptionHandler(DartApiException.class)
    public ResponseEntity<Map<String, Object>> handleDartApi(DartApiException e) {
        publishError(e);
        DartStatusCode statusCode = e.getStatusCode();

        if (statusCode == null) {
            return buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
        }

        return switch (statusCode) {
            case RATE_LIMIT_EXCEEDED ->
                    buildResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", e.getMessage());
            case INVALID_FIELD ->
                    buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
            case SYSTEM_MAINTENANCE ->
                    buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", e.getMessage());
            default ->
                    buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
        };
    }

    /**
     * SEC EDGAR API 예외 (에러 타입별 HTTP 매핑)
     */
    @ExceptionHandler(SecApiException.class)
    public ResponseEntity<Map<String, Object>> handleSecApi(SecApiException e) {
        publishError(e);
        return switch (e.getErrorType()) {
            case CIK_NOT_FOUND, COMPANY_NOT_FOUND ->
                    buildResponse(HttpStatus.NOT_FOUND, "SEC_NOT_FOUND", e.getMessage());
            case RATE_LIMIT_EXCEEDED ->
                    buildResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", e.getMessage());
            case API_ERROR ->
                    buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
            default ->
                    buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
        };
    }

    /**
     * 외부 API 예외 (KIS, ECOS, News, DataGoKr, TradingEconomics)
     */
    @ExceptionHandler({
            KisApiException.class,
            EcosApiException.class,
            NewsApiException.class,
            DataGoKrApiException.class,
            TradingEconomicsFetchException.class,
            TradingEconomicsParseException.class
    })
    public ResponseEntity<Map<String, Object>> handleExternalApi(RuntimeException e) {
        publishError(e);
        return buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
    }

    /**
     * 기타 미처리 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("unhandled exception: {}", e.toString(), e);
        publishError(e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", error,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * AOP 누락 경로 대비 이중 수집 — 예외를 ERROR 이벤트로 발행.
     * ES 문서 ID 는 {requestId}-{exceptionClass} 로 강제되어 AOP 적재분과 자연 dedup.
     * 로깅 실패가 본 기능을 막지 않도록 모든 예외 삼킴.
     */
    private void publishError(Exception e) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("exceptionClass", e.getClass().getName());
            payload.put("message", String.valueOf(e.getMessage()));
            payload.put("source", "GlobalExceptionHandler");
            payload.put("stackTrace", renderStack(e));

            eventPublisher.publishEvent(new ApplicationLogEvent(
                    LogDomain.ERROR,
                    Instant.now(),
                    currentUserId(),
                    currentRequestId(),
                    payload
            ));
        } catch (Exception publishingFailure) {
            log.warn("ERROR 이벤트 발행 실패: {}", publishingFailure.getMessage());
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getPrincipal() instanceof Long id ? id : null;
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
        return sb.toString();
    }
}