package com.thlee.stock.market.stockmarket.glossary.presentation;

import com.thlee.stock.market.stockmarket.glossary.application.exception.GlossaryCategoryNotFoundException;
import com.thlee.stock.market.stockmarket.glossary.application.exception.GlossaryTermNotFoundException;
import com.thlee.stock.market.stockmarket.glossary.domain.exception.ReservedCategoryNameException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * glossary 컨트롤러 전용 예외 매핑.
 *
 * <p>응답 shape 는 newsjournal / GlobalExceptionHandler 와 동일 — {@code {error, message, timestamp}}.
 * {@code @Valid} 검증 실패는 추가로 {@code fieldErrors} 를 포함한다.
 *
 * <p>{@link DataIntegrityViolationException} 중 카테고리 unique 제약 위반은
 * {@code DUPLICATE_CATEGORY_NAME} 으로 좁힌 409 응답. 그 외 무결성 위반은 Global Handler 의 일반 409 로 위임 (재 throw).
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {
        GlossaryTermController.class,
        GlossaryCategoryController.class
})
public class GlossaryExceptionHandler {

    private static final String UNIQUE_CATEGORY_CONSTRAINT = "uq_glossary_category_user_name";
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

    @ExceptionHandler({GlossaryTermNotFoundException.class, GlossaryCategoryNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        log.debug("glossary not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "용어/카테고리를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @ExceptionHandler(ReservedCategoryNameException.class)
    public ResponseEntity<Map<String, Object>> handleReserved(ReservedCategoryNameException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "RESERVED_CATEGORY_NAME",
                "예약된 카테고리명입니다. 다른 이름을 사용해 주세요.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        if (isCategoryUniqueViolation(e)) {
            log.debug("glossary duplicate category name: {}", e.getMessage());
            return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_NAME",
                    "이미 같은 이름의 카테고리가 있습니다.");
        }
        throw e;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(InsufficientAuthenticationException e) {
        log.debug("glossary insufficient authentication: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Map<String, Object> body = baseBody("VALIDATION_FAILED", "요청 본문 검증에 실패했습니다.");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        log.debug("glossary payload not readable: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("glossary param type mismatch: name={}, value={}", e.getName(), e.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "잘못된 요청 파라미터 값: " + e.getName());
    }

    private static boolean isCategoryUniqueViolation(DataIntegrityViolationException e) {
        String message = rootMessage(e);
        if (message == null) {
            return false;
        }
        return message.contains(UNIQUE_CATEGORY_CONSTRAINT) || message.contains(SQLSTATE_UNIQUE_VIOLATION);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private static ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(baseBody(error, message));
    }

    private static Map<String, Object> baseBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
