package com.thlee.stock.market.stockmarket.realestate.domain.service;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;

import java.util.Set;

/**
 * 부동산 시장 데이터 출처 어댑터 포트.
 * <p>
 * 8개 출처(MOLIT, REB, HUB, KOSIS, SUBSCRIPTION_HOME, HUG, SEOUL_OPEN_DATA, GG_DATA_DREAM)는
 * 각각 본 인터페이스를 구현하고 {@link #supportedSource()}로 자기 source를 선언한다.
 * <p>
 * 인증키 미설정이나 외부 호출 실패는 예외 또는 {@link FetchResult#failure(String)}로 표현하며,
 * 스케줄러가 항목 단위로 격리해 다음 항목 처리에 영향을 주지 않는다.
 */
public interface RealEstateMarketSourceAdapter {

    RealEstateMarketSource supportedSource();

    /**
     * 해당 출처가 지원하는 카테고리 집합.
     */
    Set<RealEstateMarketCategory> supportedCategories();

    /**
     * 한 (region, category) 단위 fetch.
     */
    FetchResult fetch(RegionCode region,
                      RealEstateMarketCategory category,
                      FetchWindow window);
}