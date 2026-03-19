package com.thlee.stock.market.stockmarket.infrastructure.web;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsParseException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.exception.EcosApiException;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.NewsApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception.DartApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
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
     * 외부 API 예외 (DART, KIS, ECOS, News, DataGoKr, TradingEconomics)
     */
    @ExceptionHandler({
            DartApiException.class,
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