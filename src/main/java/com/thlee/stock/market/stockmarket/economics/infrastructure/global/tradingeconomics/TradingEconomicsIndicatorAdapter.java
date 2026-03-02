package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.ParsedTable;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TradingEconomicsIndicatorAdapter implements GlobalIndicatorPort {

    private final TradingEconomicsIndicatorRegistry registry;
    private final TradingEconomicsHtmlClient htmlClient;
    private final TradingEconomicsTableParser tableParser;
    private final TradingEconomicsValueNormalizer normalizer;

    @Override
    public List<CountryIndicatorSnapshot> fetchByIndicator(GlobalEconomicIndicatorType indicatorType) {
        String url = registry.getUrl(indicatorType);

        // 1. HTML 수집
        Document document = htmlClient.fetch(url);

        // 2. 테이블 파싱 (헤더 기반 동적 매핑)
        ParsedTable parsedTable = tableParser.parse(document);

        // 3. 값 정규화 → 도메인 모델 변환
        return normalizer.normalize(indicatorType, parsedTable.rows());
    }
}