package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper.StockNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockNoteVerificationRepositoryImpl implements StockNoteVerificationRepository {

    private final StockNoteVerificationJpaRepository jpaRepository;

    @Override
    public StockNoteVerification save(StockNoteVerification verification) {
        StockNoteVerificationEntity saved = jpaRepository.save(StockNoteMapper.toEntity(verification));
        if (verification.getId() == null) {
            verification.assignId(saved.getId());
        }
        return StockNoteMapper.toDomain(saved);
    }

    @Override
    public Optional<StockNoteVerification> findByNoteId(Long noteId) {
        return jpaRepository.findByNoteId(noteId).map(StockNoteMapper::toDomain);
    }

    @Override
    public boolean existsByNoteId(Long noteId) {
        return jpaRepository.existsByNoteId(noteId);
    }

    @Override
    public Map<Long, StockNoteVerification> findAllByNoteIds(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, StockNoteVerification> map = new LinkedHashMap<>();
        for (StockNoteVerificationEntity e : jpaRepository.findByNoteIdIn(noteIds)) {
            map.put(e.getNoteId(), StockNoteMapper.toDomain(e));
        }
        return map;
    }

    @Override
    public void deleteByNoteId(Long noteId) {
        jpaRepository.deleteByNoteId(noteId);
    }
}