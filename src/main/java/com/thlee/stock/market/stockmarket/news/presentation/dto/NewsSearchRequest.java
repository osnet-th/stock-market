package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchField;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchSort;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스 전문 검색 요청 DTO
 *
 * <p>#115 에서 검색어가 <b>선택</b>으로 바뀌었다. 통합 화면의 기본 상태가
 * "저장된 전체 뉴스"(검색어 없음)이고, 그때는 DB 최신순 조회로 처리한다.
 * 이전에는 {@code @NotBlank} 라 빈 검색어가 400 이었다.
 */
@Getter
@Setter
public class NewsSearchRequest {

    @Size(max = 200, message = "검색어는 200자 이내여야 합니다.")
    private String query;

    /**
     * 검색을 좁힐 키워드 (다중). 비어 있으면 사용자의 전체 구독 키워드로 해석한다.
     * 컨트롤러가 내 구독인지 검증한다 — 검증 없이 믿으면 남의 키워드 뉴스를 열람할 수 있다.
     */
    private List<Long> keywordIds;

    private LocalDate startDate;
    private LocalDate endDate;
    private Region region;

    /** 검색 대상 필드. 기본 제목+본문. */
    private NewsSearchField field = NewsSearchField.TITLE_CONTENT;

    /** 정렬. 기본 최신순 — 검색어가 없으면 관련도가 무의미해 최신순으로 강제된다. */
    private NewsSearchSort sort = NewsSearchSort.LATEST;

    /** 목업 필터 바의 `안 읽은 것만` 토글. */
    private boolean unreadOnly = false;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}
