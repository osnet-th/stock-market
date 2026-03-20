package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.UserKeywordMapper;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository.UserKeywordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UserKeywordRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class UserKeywordRepositoryImpl implements UserKeywordRepository {

    private final UserKeywordJpaRepository userKeywordJpaRepository;

    @Override
    public UserKeyword save(UserKeyword userKeyword) {
        UserKeywordEntity entity = UserKeywordMapper.toEntity(userKeyword);
        UserKeywordEntity saved = userKeywordJpaRepository.save(entity);
        return UserKeywordMapper.toDomain(saved);
    }

    @Override
    public Optional<UserKeyword> findByUserIdAndKeywordId(Long userId, Long keywordId) {
        return userKeywordJpaRepository.findByUserIdAndKeywordId(userId, keywordId)
                .map(UserKeywordMapper::toDomain);
    }

    @Override
    public List<UserKeyword> findByUserId(Long userId) {
        return userKeywordJpaRepository.findByUserId(userId).stream()
                .map(UserKeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserKeyword> findByUserIdAndActive(Long userId, boolean active) {
        return userKeywordJpaRepository.findByUserIdAndActive(userId, active).stream()
                .map(UserKeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserKeyword> findByKeywordId(Long keywordId) {
        return userKeywordJpaRepository.findByKeywordId(keywordId).stream()
                .map(UserKeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByKeywordId(Long keywordId) {
        return userKeywordJpaRepository.existsByKeywordId(keywordId);
    }

    @Override
    public void delete(UserKeyword userKeyword) {
        UserKeywordEntity entity = UserKeywordMapper.toEntity(userKeyword);
        userKeywordJpaRepository.delete(entity);
    }

    @Override
    public void deleteByUserIdAndKeywordId(Long userId, Long keywordId) {
        userKeywordJpaRepository.deleteByUserIdAndKeywordId(userId, keywordId);
    }
}
