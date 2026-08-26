package com.thlee.stock.market.stockmarket.realestate.infrastructure;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Source/카테고리 기반 어댑터 라우팅 구현체.
 * <p>
 * 생성자 주입 시점에 Spring이 모든 RealEstateMarketSourceAdapter 구현체를 List로 모아주며,
 * 이를 supportedSource로 grouping해 source당 N개의 어댑터(예: MOLIT 매매/전월세 분리)를 다룬다.
 */
@Component
public class RealEstateMarketSourceFactoryImpl implements RealEstateMarketSourceFactory {

    private final List<RealEstateMarketSourceAdapter> all;
    private final Map<RealEstateMarketSource, List<RealEstateMarketSourceAdapter>> bySource;

    public RealEstateMarketSourceFactoryImpl(List<RealEstateMarketSourceAdapter> adapters) {
        this.all = List.copyOf(adapters);
        this.bySource = adapters.stream()
                .collect(Collectors.groupingBy(RealEstateMarketSourceAdapter::supportedSource));
    }

    @Override
    public List<RealEstateMarketSourceAdapter> getAdapters(RealEstateMarketSource source) {
        return bySource.getOrDefault(source, Collections.emptyList());
    }

    @Override
    public List<RealEstateMarketSourceAdapter> getAdaptersByCategory(RealEstateMarketCategory category) {
        return all.stream()
                .filter(adapter -> adapter.supportedCategories().contains(category))
                .toList();
    }

    @Override
    public List<RealEstateMarketSourceAdapter> getAll() {
        return all;
    }
}
