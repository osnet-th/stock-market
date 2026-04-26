package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StockNoteTagJpaRepository extends JpaRepository<StockNoteTagEntity, Long> {

    List<StockNoteTagEntity> findByNoteId(Long noteId);

    List<StockNoteTagEntity> findByNoteIdIn(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);
}