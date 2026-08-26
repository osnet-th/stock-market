package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NewsEventKeywordJpaRepository extends JpaRepository<NewsEventKeywordEntity, Long> {

    List<NewsEventKeywordEntity> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    /** 목록 조회 시 N+1 회피용 일괄 조회. eventId 그룹 내 displayOrder 순서로 정렬. */
    List<NewsEventKeywordEntity> findByEventIdInOrderByEventIdAscDisplayOrderAsc(Collection<Long> eventIds);

    /**
     * 사용자 전체 사건의 키워드 평탄화 행 (화면 통계용).
     * 연관관계 없이 {@code eventId = e.id} 조인으로 userId 스코프를 강제한다.
     */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRow(
                k.eventId, e.occurredDate, k.keyword
            )
            FROM NewsEventKeywordEntity k, NewsEventEntity e
            WHERE k.eventId = e.id
              AND e.userId = :userId
            ORDER BY e.occurredDate DESC, e.id DESC, k.displayOrder ASC
            """)
    List<NewsEventKeywordRow> findRowsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM NewsEventKeywordEntity k WHERE k.eventId = :eventId")
    int deleteByEventId(@Param("eventId") Long eventId);
}
