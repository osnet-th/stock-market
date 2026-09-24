package com.thlee.stock.market.stockmarket.news.application.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 키워드 벌크 처리 요청 (#115) — 선택한 키워드 일괄 중단·삭제.
 *
 * <p>목업 레일 선택 바의 `중단` · `삭제` 버튼에 대응한다.
 * 일괄 활성화(재개)는 목업에 버튼이 없어 범위에서 제외했다.
 */
@Getter
@Setter
public class BulkKeywordRequest {

    @NotEmpty(message = "대상 키워드를 선택해야 합니다.")
    private List<Long> keywordIds;
}
