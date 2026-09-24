package com.thlee.stock.market.stockmarket.glossary.application;

import com.thlee.stock.market.stockmarket.glossary.application.dto.CreateGlossaryTermCommand;
import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryTermListResult;
import com.thlee.stock.market.stockmarket.glossary.application.dto.UpdateGlossaryTermCommand;
import com.thlee.stock.market.stockmarket.glossary.application.exception.GlossaryCategoryNotFoundException;
import com.thlee.stock.market.stockmarket.glossary.application.exception.GlossaryTermNotFoundException;
import com.thlee.stock.market.stockmarket.glossary.application.support.LikeEscaper;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryCategoryRepository;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermListFilter;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermRepository;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermSort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 용어 유스케이스 (CRUD + 검색/필터/정렬/페이지네이션).
 *
 * <p>ownership 은 단일 쿼리 {@code findByIdAndUserId(...).orElseThrow(...)} 로 강제 (404 통일).
 * 외부 카테고리 차단(외래 categoryId 사용 시도)도 동일 패턴으로.
 */
@Service
@RequiredArgsConstructor
public class GlossaryTermService {

    private final GlossaryTermRepository termRepository;
    private final GlossaryCategoryRepository categoryRepository;
    private final GlossaryCategoryService categoryService;

    @Transactional(readOnly = true)
    public GlossaryTerm find(Long userId, Long termId) {
        return loadOwned(userId, termId);
    }

    @Transactional(readOnly = true)
    public GlossaryTermListResult list(Long userId, String rawQ, String rawDefinitionQ,
                                       Long categoryId, boolean uncategorizedOnly,
                                       GlossaryTermSort sort, int page, int size) {
        GlossaryTermListFilter filter = new GlossaryTermListFilter(
                LikeEscaper.toContainsPattern(rawQ),
                LikeEscaper.toContainsPattern(rawDefinitionQ),
                uncategorizedOnly ? null : categoryId,
                uncategorizedOnly,
                sort != null ? sort : GlossaryTermSort.REGISTERED_DESC,
                page,
                size
        );
        List<GlossaryTerm> items = termRepository.findList(userId, filter);
        long total = termRepository.countList(userId, filter);
        return new GlossaryTermListResult(items, total, page, size);
    }

    @Transactional
    public GlossaryTerm create(Long userId, CreateGlossaryTermCommand cmd) {
        Long resolvedCategoryId = resolveCategoryRef(userId, cmd.categoryId(), cmd.categoryName());
        List<Long> relatedIds = resolveRelatedRefs(userId, null, cmd.relatedTermIds());
        GlossaryTerm term = GlossaryTerm.create(userId, cmd.name(), cmd.content(), resolvedCategoryId, relatedIds);
        return termRepository.save(term);
    }

    @Transactional
    public GlossaryTerm update(Long userId, Long termId, UpdateGlossaryTermCommand cmd) {
        GlossaryTerm term = loadOwned(userId, termId);
        Long resolvedCategoryId = resolveCategoryRef(userId, cmd.categoryId(), cmd.categoryName());
        List<Long> relatedIds = resolveRelatedRefs(userId, termId, cmd.relatedTermIds());
        term.replace(cmd.name(), cmd.content(), resolvedCategoryId, relatedIds);
        return termRepository.save(term);
    }

    @Transactional
    public void delete(Long userId, Long termId) {
        loadOwned(userId, termId);
        termRepository.deleteByIdAndUserId(termId, userId);
    }

    /**
     * categoryId / categoryName 입력을 단일 categoryId 로 정규화.
     * categoryId 우선, 없으면 categoryName 인라인 resolve, 둘 다 없으면 null (미분류).
     */
    private Long resolveCategoryRef(Long userId, Long categoryId, String categoryName) {
        if (categoryId != null) {
            categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new GlossaryCategoryNotFoundException(categoryId));
            return categoryId;
        }
        if (categoryName != null && !categoryName.isBlank()) {
            GlossaryCategory resolved = categoryService.resolve(userId, categoryName);
            return resolved.getId();
        }
        return null;
    }

    /**
     * 함께 볼 용어 참조 정규화 — null/자기 참조/중복 제거 후 본인 소유 & 실존 id 만 통과.
     * 비소유/미존재 id 는 조용히 제거한다 (편집 중 다른 탭에서 삭제된 용어 허용 —
     * 목업의 dangling 필터와 동일 방침). 타 사용자 id 도 동일하게 제거되므로 존재 여부가
     * 새어 나가지 않는다. 입력 순서는 보존한다.
     */
    private List<Long> resolveRelatedRefs(Long userId, Long selfId, List<Long> candidateIds) {
        List<Long> requested = sanitizeRelatedInput(selfId, candidateIds);
        if (requested.isEmpty()) {
            return List.of();
        }
        Set<Long> owned = Set.copyOf(termRepository.findOwnedIds(userId, requested));
        return requested.stream().filter(owned::contains).toList();
    }

    private List<Long> sanitizeRelatedInput(Long selfId, List<Long> candidateIds) {
        if (candidateIds == null) {
            return List.of();
        }
        return candidateIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(selfId))
                .distinct()
                .toList();
    }

    private GlossaryTerm loadOwned(Long userId, Long termId) {
        return termRepository.findByIdAndUserId(termId, userId)
                .orElseThrow(() -> new GlossaryTermNotFoundException(termId));
    }
}