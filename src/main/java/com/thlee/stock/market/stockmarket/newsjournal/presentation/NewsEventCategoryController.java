package com.thlee.stock.market.stockmarket.newsjournal.presentation;

import com.thlee.stock.market.stockmarket.newsjournal.application.NewsEventCategoryService;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.CategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 뉴스 저널 사건 분류 REST API.
 *
 * <p>탭 / 자동완성용으로 사용자 카테고리 목록을 노출한다.
 * 카테고리 명시 등록은 별도 엔드포인트 없이 사건 생성/수정 시 서비스가 자동 등록한다.
 */
@RestController
@RequestMapping("/api/news-journal/categories")
@RequiredArgsConstructor
public class NewsEventCategoryController {

    private final NewsEventCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> findList() {
        Long userId = NewsJournalSecurityContext.currentUserId();
        List<CategoryDto> response = categoryService.findByUserId(userId).stream()
                .map(CategoryDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}