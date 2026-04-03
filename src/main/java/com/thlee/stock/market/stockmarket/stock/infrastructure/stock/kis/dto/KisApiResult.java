package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * KIS API 연속조회 결과 래퍼.
 * 응답 데이터와 다음 페이지 존재 여부를 함께 전달한다.
 */
@Getter
@RequiredArgsConstructor
public class KisApiResult<T> {

    private final T data;
    private final boolean hasNext;
}