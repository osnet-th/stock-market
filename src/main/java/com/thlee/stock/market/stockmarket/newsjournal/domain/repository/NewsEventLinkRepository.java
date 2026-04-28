package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 사건 관련 기사 링크 Repository 포트.
 *
 * <p>갱신은 {@link #replaceAll} 정책으로 처리되며, 사건 삭제 시
 * {@link #deleteByEventId} 로 자식을 명시적으로 제거한다 (Entity 연관관계 미사용).
 */
public interface NewsEventLinkRepository {

    /** 단건 사건 상세용. {@code displayOrder} 오름차순. */
    List<NewsEventLink> findByEventId(Long eventId);

    /** 목록 뷰 N+1 회피용 일괄 조회. 결과 키는 eventId, 값은 displayOrder 정렬 리스트. */
    Map<Long, List<NewsEventLink>> findAllByEventIds(Collection<Long> eventIds);

    /**
     * 사건의 자식 링크를 새 리스트로 교체한다.
     * <p>{@code deleteByEventId} 후 일괄 저장하는 replace-all 정책. 동일 트랜잭션 안에서 호출 필수.
     */
    void replaceAll(Long eventId, List<NewsEventLink> links);

    /** 사건 삭제 cascade 용. */
    void deleteByEventId(Long eventId);
}