package com.thlee.stock.market.stockmarket.infrastructure.web;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsParseException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.exception.EcosApiException;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartStatusCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception.DartApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import com.thlee.stock.market.stockmarket.user.domain.exception.UserDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 검증 실패 (잘못된 파라미터, 도메인 규칙 위반)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    /**
     * 도메인 예외
     */
    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<Map<String, Object>> handleUserDomain(UserDomainException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "DOMAIN_ERROR", e.getMessage());
    }

    /**
     * DART API 예외 (상태 코드별 HTTP 매핑)
     */
    @ExceptionHandler(DartApiException.class)
    public ResponseEntity<Map<String, Object>> handleDartApi(DartApiException e) {
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
        return buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
    }

    /**
     * 기타 미처리 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", error,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}