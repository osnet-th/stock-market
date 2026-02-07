package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.KeywordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * KeywordRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class KeywordRepositoryImpl implements KeywordRepository {

    private final KeywordJpaRepository keywordJpaRepository;

    @Override
    public Keyword save(Keyword keyword) {
        KeywordEntity entity = KeywordMapper.toEntity(keyword);
        KeywordEntity savedEntity = keywordJpaRepository.save(entity);
        return KeywordMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Keyword> findById(Long id) {
        return keywordJpaRepository.findById(id)
                .map(KeywordMapper::toDomain);
    }

    @Override
    public List<Keyword> findByUserId(Long userId) {
        return keywordJpaRepository.findByUserId(userId)
                .stream()
                .map(KeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Keyword> findByUserIdAndActive(Long userId, boolean active) {
        return keywordJpaRepository.findByUserIdAndActive(userId, active)
                .stream()
                .map(KeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Keyword> findByActive(boolean active) {
        return keywordJpaRepository.findByActive(active)
                .stream()
                .map(KeywordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Keyword keyword) {
        KeywordEntity entity = KeywordMapper.toEntity(keyword);
        keywordJpaRepository.delete(entity);
    }
}