package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.UserKeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserKeywordJpaRepository extends JpaRepository<UserKeywordEntity, Long> {

    Optional<UserKeywordEntity> findByUserIdAndKeywordId(Long userId, Long keywordId);

    List<UserKeywordEntity> findByUserId(Long userId);

    List<UserKeywordEntity> findByUserIdAndActive(Long userId, boolean active);

    List<UserKeywordEntity> findByKeywordId(Long keywordId);

    boolean existsByKeywordId(Long keywordId);

    void deleteByUserIdAndKeywordId(Long userId, Long keywordId);
}
