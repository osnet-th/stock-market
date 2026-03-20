package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;

import java.util.List;
import java.util.Optional;

public interface UserKeywordRepository {

    UserKeyword save(UserKeyword userKeyword);

    Optional<UserKeyword> findByUserIdAndKeywordId(Long userId, Long keywordId);

    List<UserKeyword> findByUserId(Long userId);

    List<UserKeyword> findByUserIdAndActive(Long userId, boolean active);

    List<UserKeyword> findByKeywordId(Long keywordId);

    boolean existsByKeywordId(Long keywordId);

    void delete(UserKeyword userKeyword);

    void deleteByUserIdAndKeywordId(Long userId, Long keywordId);
}
