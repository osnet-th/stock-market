package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteCustomTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper.StockNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockNoteCustomTagRepositoryImpl implements StockNoteCustomTagRepository {

    private final StockNoteCustomTagJpaRepository jpaRepository;

    @Override
    public StockNoteCustomTag save(StockNoteCustomTag tag) {
        StockNoteCustomTagEntity saved = jpaRepository.save(StockNoteMapper.toEntity(tag));
        if (tag.getId() == null) {
            tag.assignId(saved.getId());
        }
        return StockNoteMapper.toDomain(saved);
    }

    @Override
    public Optional<StockNoteCustomTag> findByUserIdAndTagValue(Long userId, String tagValue) {
        return jpaRepository.findByUserIdAndTagValue(userId, tagValue).map(StockNoteMapper::toDomain);
    }

    @Override
    public int incrementUsage(Long userId, String tagValue) {
        return jpaRepository.incrementUsage(userId, tagValue);
    }

    @Override
    public List<StockNoteCustomTag> findTopByPrefix(Long userId, String prefix, int limit) {
        return jpaRepository.findTopByPrefix(userId, prefix, PageRequest.of(0, limit)).stream()
                .map(StockNoteMapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }
}