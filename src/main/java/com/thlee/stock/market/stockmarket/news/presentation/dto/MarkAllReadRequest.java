package com.thlee.stock.market.stockmarket.news.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * `모두 읽음` 요청 (#115) — 현재 화면에 보이는 기사 id 목록.
 */
@Getter
@Setter
public class MarkAllReadRequest {

    @NotEmpty(message = "대상 뉴스를 지정해야 합니다.")
    private List<Long> newsIds;
}
