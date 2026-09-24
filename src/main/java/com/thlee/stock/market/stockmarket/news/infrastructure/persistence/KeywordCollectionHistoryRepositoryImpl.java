package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.KeywordCollectionHistory;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionHistoryRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionSummary;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.KeywordCollectionHistoryMapper;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository.KeywordCollectionHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * KeywordCollectionHistoryRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class KeywordCollectionHistoryRepositoryImpl implements KeywordCollectionHistoryRepository {

    private final KeywordCollectionHistoryJpaRepository jpaRepository;

    @Override
    public KeywordCollectionHistory save(KeywordCollectionHistory history) {
        KeywordCollectionHistoryEntity saved =
                jpaRepository.save(KeywordCollectionHistoryMapper.toEntity(history));
        return KeywordCollectionHistoryMapper.toDomain(saved);
    }

    @Override
    public List<KeywordCollectionSummary> summarizeByKeywordIds(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }
        // 네이티브 집계라 컬럼 타입이 드라이버에 따라 갈린다 — Timestamp/Number 로 받아 변환한다
        return jpaRepository.summarizeByKeywordIds(keywordIds).stream()
                .map(row -> new KeywordCollectionSummary(
                        toLong(row[0]),
                        toLocalDateTime(row[1]),
                        toLocalDateTime(row[2]),
                        row[3] == null ? 0L : ((Number) row[3]).longValue()
                ))
                .toList();
    }

    @Override
    public Optional<LocalDateTime> findLastAttemptAt() {
        return Optional.ofNullable(jpaRepository.findLastAttemptAt());
    }

    @Override
    public int deleteOlderThan(LocalDateTime threshold) {
        return jpaRepository.deleteByAttemptedAtBefore(threshold);
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
