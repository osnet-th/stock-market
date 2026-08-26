package com.thlee.stock.market.stockmarket.realestate.domain.service;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;

import java.util.List;

/**
 * Source 기반 어댑터 라우팅 포트.
 */
public interface RealEstateMarketSourceFactory {

    /**
     * 단일 source의 어댑터를 반환. 없으면 빈 Optional이 아닌 빈 List 사용 후 호출자가 skip.
     */
    List<RealEstateMarketSourceAdapter> getAdapters(RealEstateMarketSource source);

    /**
     * 카테고리를 지원하는 모든 어댑터(여러 출처) 반환.
     */
    List<RealEstateMarketSourceAdapter> getAdaptersByCategory(RealEstateMarketCategory category);

    List<RealEstateMarketSourceAdapter> getAll();
}