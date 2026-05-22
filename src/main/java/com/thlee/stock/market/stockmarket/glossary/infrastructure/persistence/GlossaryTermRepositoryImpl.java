package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermListFilter;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermRepository;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermSort;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.mapper.GlossaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GlossaryTerm 포트 어댑터.
 *
 * <p>정렬 매핑은 본 어댑터의 책임. {@link GlossaryTermSort} → JPA {@link Sort} 변환.
 * 모든 정렬에 secondary tie-breaker {@code id} 를 동반해 안정 정렬 보장.
 */
@Repository
@RequiredArgsConstructor
public class GlossaryTermRepositoryImpl implements GlossaryTermRepository {

    private final GlossaryTermJpaRepository jpaRepository;

    @Override
    public GlossaryTerm save(GlossaryTerm term) {
        GlossaryTermEntity entity = GlossaryMapper.toEntity(term);
        GlossaryTermEntity saved = jpaRepository.save(entity);
        if (term.getId() == null) {
            term.assignId(saved.getId());
        }
        return GlossaryMapper.toDomain(saved);
    }

    @Override
    public Optional<GlossaryTerm> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(GlossaryMapper::toDomain);
    }

    @Override
    public List<GlossaryTerm> findList(Long userId, GlossaryTermListFilter filter) {
        Pageable pageable = PageRequest.of(filter.page(), filter.size(), toSort(filter.sort()));
        return jpaRepository.findList(
                        userId,
                        filter.q(),
                        filter.definitionQ(),
                        filter.categoryId(),
                        filter.uncategorizedOnly(),
                        pageable)
                .stream()
                .map(GlossaryMapper::toDomain)
                .toList();
    }

    @Override
    public long countList(Long userId, GlossaryTermListFilter filter) {
        return jpaRepository.countList(
                userId,
                filter.q(),
                filter.definitionQ(),
                filter.categoryId(),
                filter.uncategorizedOnly()
        );
    }

    @Override
    public int deleteByIdAndUserId(Long id, Long userId) {
        return jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public long countByUserIdAndCategoryId(Long userId, Long categoryId) {
        return jpaRepository.countByUserIdAndCategoryId(userId, categoryId);
    }

    @Override
    public int reassignCategoryToNull(Long userId, Long categoryId) {
        return jpaRepository.reassignCategoryToNull(userId, categoryId);
    }

    private static Sort toSort(GlossaryTermSort sort) {
        return switch (sort) {
            case REGISTERED_DESC -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case REGISTERED_ASC -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case ALPHA_ASC -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
        };
    }
}