package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteVerificationService;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.UpsertVerificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사후 검증 REST API. 이 컨트롤러에 의해 본문 잠금 상태가 전이된다
 * (PUT 으로 잠금 발효, DELETE 로 해제).
 */
@RestController
@RequestMapping("/api/stock-notes/{id}/verification")
@RequiredArgsConstructor
public class StockNoteVerificationController {

    private final StockNoteVerificationService verificationService;

    @PutMapping
    public ResponseEntity<Void> upsert(@PathVariable Long id,
                                       @Valid @RequestBody UpsertVerificationRequest request) {
        verificationService.upsert(request.toCommand(id, StockNoteSecurityContext.currentUserId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        verificationService.delete(id, StockNoteSecurityContext.currentUserId());
        return ResponseEntity.noContent().build();
    }
}