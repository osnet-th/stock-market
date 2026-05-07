package com.thlee.stock.market.stockmarket.realestate.domain.model;

/**
 * 부동산 시장 데이터 출처 (8종).
 * <p>
 * 같은 region×category에 복수 출처 데이터가 들어올 수 있어
 * latest unique key 및 compareKey에 source가 포함된다.
 */
public enum RealEstateMarketSource {

    MOLIT("국토교통부 실거래가"),
    REB("한국부동산원 R-ONE"),
    HUB("건축HUB"),
    KOSIS("KOSIS"),
    SUBSCRIPTION_HOME("청약홈"),
    HUG("주택도시보증공사"),
    SEOUL_OPEN_DATA("서울 열린데이터광장"),
    GG_DATA_DREAM("경기도 데이터드림");

    private final String label;

    RealEstateMarketSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
