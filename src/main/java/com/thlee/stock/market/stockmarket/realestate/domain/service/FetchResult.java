package com.thlee.stock.market.stockmarket.realestate.domain.service;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;

import java.util.List;

/**
 * 어댑터 fetch 결과. 부분 실패는 indicators가 빈 리스트로 표현되며
 * failureReason이 채워진다. 일배치 스케줄러는 failureReason을 로그하고 다음 항목으로 진행한다.
 */
public record FetchResult(List<RealEstateMarketIndicator> indicators,
                          String failureReason) {

    public static FetchResult success(List<RealEstateMarketIndicator> indicators) {
        return new FetchResult(indicators, null);
    }

    public static FetchResult failure(String reason) {
        return new FetchResult(List.of(), reason);
    }

    public boolean isFailure() {
        return failureReason != null;
    }
}