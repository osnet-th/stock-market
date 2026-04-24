package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockNoteVerificationJpaRepository extends JpaRepository<StockNoteVerificationEntity, Long> {

    Optional<StockNoteVerificationEntity> findByNoteId(Long noteId);

    boolean existsByNoteId(Long noteId);

    List<StockNoteVerificationEntity> findByNoteIdIn(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);
}