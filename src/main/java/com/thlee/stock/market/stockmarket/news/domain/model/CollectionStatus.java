package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 키워드 수집 시도 결과 (#115).
 *
 * <p>새 기사가 0건이어도 외부 API 호출이 정상이면 {@link #SUCCESS} 다 —
 * "수집은 됐지만 새 기사가 없었다"와 "수집이 실패했다"를 구분해야
 * 목업의 `연속 실패` 뱃지가 의미를 갖는다.
 */
public enum CollectionStatus {
    SUCCESS,
    FAILURE
}
