package com.thlee.stock.market.stockmarket.glossary.application;

import com.thlee.stock.market.stockmarket.glossary.application.dto.CreateGlossaryCategoryCommand;
import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryCategoryDeleteResult;
import com.thlee.stock.market.stockmarket.glossary.application.dto.UpdateGlossaryCategoryCommand;
import com.thlee.stock.market.stockmarket.glossary.application.exception.GlossaryCategoryNotFoundException;
import com.thlee.stock.market.stockmarket.glossary.domain.exception.ReservedCategoryNameException;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryCategoryRepository;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 유스케이스 (CRUD + R6 cascade).
 *
 * <p>모든 메서드 진입부에서 {@code findByIdAndUserId(...).orElseThrow(...)} 패턴으로
 * ownership 을 강제한다 (IDOR 방지, 404 통일).
 *
 * <p>{@link #resolve} 는 R13 인라인 카테고리 생성을 위한 find-or-create 패턴.
 * race 시 unique 제약 위반 → {@code DataIntegrityViolationException} 그대로 전파(409).
 */
@Service
@RequiredArgsConstructor
public class GlossaryCategoryService {

    private final GlossaryCategoryRepository categoryRepository;
    private final GlossaryTermRepository termRepository;

    @Transactional(readOnly = true)
    public List<GlossaryCategory> findAll(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId);
    }

    @Transactional
    public GlossaryCategory create(Long userId, CreateGlossaryCategoryCommand cmd) {
        GlossaryCategory category = GlossaryCategory.create(userId, cmd.name());
        return categoryRepository.save(category);
    }

    @Transactional
    public GlossaryCategory update(Long userId, Long categoryId, UpdateGlossaryCategoryCommand cmd) {
        GlossaryCategory category = loadOwned(userId, categoryId);
        category.rename(cmd.name());
        return categoryRepository.save(category);
    }

    /**
     * R6: 카테고리 삭제 + 자식 용어를 미분류(null)로 일괄 이동.
     * 단일 트랜잭션으로 원자성을 보장한다.
     */
    @Transactional
    public GlossaryCategoryDeleteResult delete(Long userId, Long categoryId) {
        loadOwned(userId, categoryId);
        int reassigned = termRepository.reassignCategoryToNull(userId, categoryId);
        categoryRepository.deleteByIdAndUserId(categoryId, userId);
        return new GlossaryCategoryDeleteResult(categoryId, reassigned);
    }

    /** R6 확인 다이얼로그에 표시할 영향받는 용어 건수 (best-effort, 실제 삭제 응답이 권위). */
    @Transactional(readOnly = true)
    public long previewDeleteImpact(Long userId, Long categoryId) {
        loadOwned(userId, categoryId);
        return termRepository.countByUserIdAndCategoryId(userId, categoryId);
    }

    /**
     * R13 인라인 카테고리 생성. trim 후 정확 일치하는 카테고리가 있으면 그것, 없으면 신규 생성.
     * "미분류" 예약어는 거부.
     */
    @Transactional
    public GlossaryCategory resolve(Long userId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("categoryName 는 필수입니다.");
        }
        String trimmed = categoryName.trim();
        if (GlossaryCategory.isReservedName(trimmed)) {
            throw new ReservedCategoryNameException(trimmed);
        }
        return categoryRepository.findByUserIdAndName(userId, trimmed)
                .orElseGet(() -> categoryRepository.save(GlossaryCategory.create(userId, trimmed)));
    }

    private GlossaryCategory loadOwned(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new GlossaryCategoryNotFoundException(categoryId));
    }
}