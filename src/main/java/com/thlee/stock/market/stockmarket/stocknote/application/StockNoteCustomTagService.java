package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteCustomTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 자유 태그 자동완성 지원.
 */
@Service
@RequiredArgsConstructor
public class StockNoteCustomTagService {

    private static final int MAX_AUTOCOMPLETE_LIMIT = 20;

    private final StockNoteCustomTagRepository customTagRepository;

    @Transactional(readOnly = true)
    public List<StockNoteCustomTag> autocomplete(Long userId, String prefix, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_AUTOCOMPLETE_LIMIT));
        if (prefix == null || prefix.isBlank()) {
            return customTagRepository.findTopByPrefix(userId, "", safeLimit);
        }
        String normalized = StockNoteTag.normalizeCustom(prefix);
        return customTagRepository.findTopByPrefix(userId, normalized, safeLimit);
    }
}