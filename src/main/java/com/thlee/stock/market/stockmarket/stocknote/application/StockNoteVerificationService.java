package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.UpsertVerificationCommand;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.cache.StocknoteCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사후 검증 upsert/delete.
 *
 * <p>검증 존재 여부가 본문 잠금 판정이므로, upsert 는 잠금을 발효시키고 delete 는 해제한다.
 * 노트 접근은 {@code findByIdAndUserId} 로 IDOR 방어 (security 리뷰).
 */
@Service
@RequiredArgsConstructor
public class StockNoteVerificationService {

    private final StockNoteRepository noteRepository;
    private final StockNoteVerificationRepository verificationRepository;

    @Transactional
    @CacheEvict(cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager", key = "#cmd.userId")
    public void upsert(UpsertVerificationCommand cmd) {
        noteRepository.findByIdAndUserId(cmd.noteId(), cmd.userId())
                .orElseThrow(() -> new StockNoteNotFoundException(cmd.noteId()));
        Optional<StockNoteVerification> existing = verificationRepository.findByNoteId(cmd.noteId());
        StockNoteVerification verification = existing
                .map(v -> {
                    v.update(cmd.judgmentResult(), cmd.verificationNote());
                    return v;
                })
                .orElseGet(() -> StockNoteVerification.create(
                        cmd.noteId(), cmd.judgmentResult(), cmd.verificationNote()));
        verificationRepository.save(verification);
    }

    @Transactional
    @CacheEvict(cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager", key = "#userId")
    public void delete(Long noteId, Long userId) {
        noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        verificationRepository.deleteByNoteId(noteId);
    }
}