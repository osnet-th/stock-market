package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 파생지표 도메인 예외 → HTTP 매핑. 응답 본문은 안정된 error/message만 노출(내부 정보 비노출).
 */
@RestControllerAdvice(assignableTypes = UserDerivedIndicatorController.class)
public class DerivedIndicatorExceptionHandler {

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(InsufficientAuthenticationException e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ExceptionHandler({DerivedIndicatorNotFoundException.class, UnknownPresetException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidDerivedFormulaException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFormula(InvalidDerivedFormulaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_FORMULA",
                "reason", e.getReason().name(),
                "message", "수식이 올바르지 않습니다.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(DerivedIndicatorLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimit(DerivedIndicatorLimitExceededException e) {
        return build(HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED", e.getMessage());
    }

    @ExceptionHandler(DuplicateDerivedIndicatorNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateDerivedIndicatorNameException e) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_NAME", e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "error", error,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
