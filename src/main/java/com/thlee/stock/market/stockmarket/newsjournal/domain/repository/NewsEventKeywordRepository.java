package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventKeyword;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 사건 키워드 Repository 포트.
 *
 * <p>갱신은 {@link #replaceAll} 정책으로 처리되며, 사건 삭제 시
 * {@link #deleteByEventId} 로 자식을 명시적으로 제거한다 (Entity 연관관계 미사용).
 */
public interface NewsEventKeywordRepository {

    /** 단건 사건 상세용. {@code displayOrder} 오름차순. */
    List<NewsEventKeyword> findByEventId(Long eventId);

    /** 목록 뷰 N+1 회피용 일괄 조회. 결과 키는 eventId, 값은 displayOrder 정렬 리스트. */
    Map<Long, List<NewsEventKeyword>> findAllByEventIds(Collection<Long> eventIds);

    /**
     * 사용자 전체 사건의 키워드 평탄화 행 (화면 통계용).
     * 정렬은 {@code occurredDate DESC, eventId DESC, displayOrder ASC} — 최근 사용 계산에 사용.
     */
    List<NewsEventKeywordRow> findRowsByUserId(Long userId);

    /**
     * 사건의 자식 키워드를 새 리스트로 교체한다.
     * <p>{@code deleteByEventId} 후 일괄 저장하는 replace-all 정책. 동일 트랜잭션 안에서 호출 필수.
     */
    void replaceAll(Long eventId, List<NewsEventKeyword> keywords);

    /** 사건 삭제 cascade 용. */
    void deleteByEventId(Long eventId);
}
