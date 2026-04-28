package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NewsEventLinkJpaRepository extends JpaRepository<NewsEventLinkEntity, Long> {

    List<NewsEventLinkEntity> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    /** 목록 조회 시 N+1 회피용 일괄 조회. eventId 그룹 내 displayOrder 순서로 정렬. */
    List<NewsEventLinkEntity> findByEventIdInOrderByEventIdAscDisplayOrderAsc(Collection<Long> eventIds);

    @Modifying
    @Query("DELETE FROM NewsEventLinkEntity l WHERE l.eventId = :eventId")
    int deleteByEventId(@Param("eventId") Long eventId);
}