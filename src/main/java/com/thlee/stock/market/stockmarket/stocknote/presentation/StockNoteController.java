package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteReadService;
import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteSnapshotService;
import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteWriteService;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteDetailResult;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteListResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.RiseCharacter;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteListFilter;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.CreateStockNoteRequest;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.StockNoteDetailResponse;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.StockNoteListResponse;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.UpdateStockNoteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

/**
 * 주식 기록 REST API.
 *
 * <p>모든 엔드포인트는 {@link StockNoteSecurityContext#StockNoteSecurityContext.currentUserId()} 로 안전하게 추출한 userId 로 스코프된다.
 * 권한이 없거나 존재하지 않는 기록 접근은 404 로 통일 (security 리뷰 권고).
 */
@RestController
@RequestMapping("/api/stock-notes")
@RequiredArgsConstructor
public class StockNoteController {

    private final StockNoteWriteService writeService;
    private final StockNoteReadService readService;
    private final StockNoteSnapshotService snapshotService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateStockNoteRequest request) {
        Long userId = StockNoteSecurityContext.currentUserId();
        Long noteId = writeService.create(request.toCommand(userId));
        URI location = UriComponentsBuilder.fromPath("/api/stock-notes/{id}").buildAndExpand(noteId).toUri();
        return ResponseEntity.created(location).body(Map.of("id", noteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockNoteDetailResponse> findById(@PathVariable Long id) {
        StockNoteDetailResult result = readService.findById(id, StockNoteSecurityContext.currentUserId());
        return ResponseEntity.ok(StockNoteDetailResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<StockNoteListResponse> findList(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) NoteDirection direction,
            @RequestParam(required = false) RiseCharacter character,
            @RequestParam(required = false) JudgmentResult judgmentResult,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        StockNoteListFilter filter = new StockNoteListFilter(
                stockCode, from, to, direction, character, judgmentResult, page, size);
        StockNoteListResult result = readService.findList(StockNoteSecurityContext.currentUserId(), filter);
        return ResponseEntity.ok(StockNoteListResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateStockNoteRequest request) {
        writeService.update(request.toCommand(id, StockNoteSecurityContext.currentUserId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        writeService.delete(id, StockNoteSecurityContext.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/snapshots/{type}/retry")
    public ResponseEntity<Void> retrySnapshot(@PathVariable Long id,
                                              @PathVariable SnapshotType type) {
        snapshotService.manualRetry(id, type, StockNoteSecurityContext.currentUserId());
        return ResponseEntity.noContent().build();
    }

}