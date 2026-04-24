package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;

import java.util.List;
import java.util.Optional;

/**
 * 사용자별 자유 태그 마스터 Repository 포트.
 *
 * <p>기록 생성 시 자유 태그가 존재하지 않으면 {@link #save} 로 생성하고, 존재하면
 * {@link #incrementUsage} 로 원자적으로 사용 횟수를 증가시킨다.
 */
public interface StockNoteCustomTagRepository {

    StockNoteCustomTag save(StockNoteCustomTag tag);

    Optional<StockNoteCustomTag> findByUserIdAndTagValue(Long userId, String tagValue);

    /**
     * 원자적 사용 횟수 증가.
     *
     * @return 업데이트된 행 수 (0 이면 해당 태그가 아직 없음 → caller 가 save 로 분기)
     */
    int incrementUsage(Long userId, String tagValue);

    /** 접두어 기반 자동완성. 사용자 스코프 내에서 usage_count DESC, tag_value ASC. */
    List<StockNoteCustomTag> findTopByPrefix(Long userId, String prefix, int limit);

    long countByUserId(Long userId);
}