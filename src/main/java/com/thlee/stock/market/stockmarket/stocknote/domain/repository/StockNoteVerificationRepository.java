package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 사후 검증 Repository 포트.
 *
 * <p>검증 존재 여부가 본문 잠금 판정을 대신하므로 {@link #existsByNoteId} 가 잠금 검사의
 * 기준이 된다.
 */
public interface StockNoteVerificationRepository {

    StockNoteVerification save(StockNoteVerification verification);

    Optional<StockNoteVerification> findByNoteId(Long noteId);

    boolean existsByNoteId(Long noteId);

    /** 다건 기록의 검증을 한 번에 조회 (N+1 회피). 검증 없는 noteId 는 결과 맵에 존재하지 않음. */
    Map<Long, StockNoteVerification> findAllByNoteIds(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);
}