package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteCustomTagService;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.CustomTagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자유 태그 자동완성 API.
 */
@RestController
@RequestMapping("/api/stock-notes/custom-tags")
@RequiredArgsConstructor
public class StockNoteCustomTagController {

    private final StockNoteCustomTagService customTagService;

    @GetMapping
    public ResponseEntity<CustomTagResponse> autocomplete(
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(CustomTagResponse.from(
                customTagService.autocomplete(StockNoteSecurityContext.currentUserId(), prefix, limit)));
    }
}