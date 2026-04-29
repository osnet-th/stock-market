package com.thlee.stock.market.stockmarket.newsjournal.application;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사건 분류 유스케이스.
 *
 * <p>{@link #resolve} 는 사건 생성/수정 시 사용자가 입력한 분류명을
 * find-or-create 로 처리한다. 매칭은 trim 후 정확 일치(case-sensitive).
 * 동시 INSERT race 는 unique {@code (user_id, name)} 제약이 안전망으로 동작하며,
 * 1인 환경에서 발현 빈도가 0 에 가까워 별도 retry 는 두지 않는다 (예외는 호출자에게 전파).
 */
@Service
@RequiredArgsConstructor
public class NewsEventCategoryService {

    private final NewsEventCategoryRepository categoryRepository;

    /** 사용자 카테고리 목록 (탭 / 자동완성용). 이름 오름차순. */
    @Transactional(readOnly = true)
    public List<NewsEventCategory> findByUserId(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId);
    }

    /**
     * 사용자 분류명에 해당하는 카테고리를 찾거나 즉시 생성한다.
     */
    @Transactional
    public NewsEventCategory resolve(Long userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("category 는 필수입니다.");
        }
        String trimmed = name.trim();
        return categoryRepository.findByUserIdAndName(userId, trimmed)
                .orElseGet(() -> categoryRepository.save(NewsEventCategory.create(userId, trimmed)));
    }
}