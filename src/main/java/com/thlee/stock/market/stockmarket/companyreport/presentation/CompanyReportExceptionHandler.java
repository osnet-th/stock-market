package com.thlee.stock.market.stockmarket.companyreport.presentation;

import com.thlee.stock.market.stockmarket.companyreport.domain.exception.CompanyReportNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * companyreport 도메인 전용 예외 → HTTP Status 매핑
 */
@RestControllerAdvice(assignableTypes = CompanyReportController.class)
public class CompanyReportExceptionHandler {

    @ExceptionHandler(CompanyReportNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(CompanyReportNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(InsufficientAuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
                        (left, right) -> left));
        return ResponseEntity.badRequest().body(Map.of("message", "입력값이 올바르지 않습니다.", "fieldErrors", fieldErrors));
    }
}
