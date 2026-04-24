package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteLockedException;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * stocknote 도메인 컨트롤러 전용 예외 매핑.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {
        StockNoteController.class,
        StockNoteVerificationController.class,
        StockNoteAnalyticsController.class,
        StockNoteCustomTagController.class
})
public class StockNoteExceptionHandler {

    @ExceptionHandler(StockNoteNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StockNoteNotFoundException e) {
        log.debug("stocknote not found: {}", e.getMessage());
        return ResponseEntity.status(404).body(Map.of("error", "NOT_FOUND"));
    }

    @ExceptionHandler(StockNoteLockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(StockNoteLockedException e) {
        return ResponseEntity.status(409).body(Map.of("error", StockNoteLockedException.ERROR_CODE));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }
}