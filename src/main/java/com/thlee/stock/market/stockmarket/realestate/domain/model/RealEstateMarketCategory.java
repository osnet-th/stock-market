package com.thlee.stock.market.stockmarket.realestate.domain.model;

/**
 * 부동산 시장 데이터 카테고리.
 * <p>
 * 화면의 11개 탭과 매핑되며, 단일 history 테이블을 카테고리 enum으로 분류한다.
 */
public enum RealEstateMarketCategory {

    TRADE("매매 거래"),
    PRICE_INDEX("매매 가격"),
    RENT("전월세"),
    OFFICIAL_STATS("공식 통계"),
    SUPPLY("공급"),
    UNSOLD("미분양"),
    SUBSCRIPTION("청약"),
    GUARANTEE_RISK("분양 리스크"),
    SEOUL_LOCAL("서울 지역 특화"),
    GYEONGGI_LOCAL("경기 지역 특화");

    private final String label;

    RealEstateMarketCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
