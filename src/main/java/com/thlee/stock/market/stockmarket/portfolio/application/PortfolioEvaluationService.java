package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 평가금액 계산 서비스
 * 고유 종목 일괄 현재가 조회 → 사용자별 평가 결과 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioEvaluationService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final StockPriceService stockPriceService;

    /**
     * 여러 사용자의 포트폴리오를 일괄 평가
     * 고유 종목에 대해 1회만 현재가를 조회하여 O(distinct stocks) 최적화
     */
    public Map<Long, PortfolioEvaluation> evaluatePortfolios(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        // 1. 전체 대상자 포트폴리오 일괄 조회 (N+1 방지)
        List<PortfolioItem> allItems = portfolioItemRepository.findByUserIdIn(userIds);

         // 2. STOCK 항목에서 고유 종목 추출 + 현재가 1회 조회
        Map<String, StockPriceResponse> priceMap = fetchDistinctStockPrices(allItems);

        // 4. 사용자별 그룹핑 → 평가금액 계산
        Map<Long, List<PortfolioItem>> userItemsMap = allItems.stream()
                .collect(Collectors.groupingBy(PortfolioItem::getUserId));

        Map<Long, PortfolioEvaluation> result = new HashMap<>();
        for (Long userId : userIds) {
            List<PortfolioItem> userItems = userItemsMap.getOrDefault(userId, List.of());
            result.put(userId, buildEvaluation(userId, userItems, priceMap));
        }

        return result;
    }

    private PortfolioEvaluation buildEvaluation(Long userId, List<PortfolioItem> items,
                                                 Map<String, StockPriceResponse> priceMap) {
        List<ItemEvaluation> evaluations = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalEvaluated = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal invested = item.getInvestedAmount();
            BigDecimal evaluated = invested;
            Integer quantity = null;
            String currentPrice = null;
            String changeRate = null;
            String country = null;

            if (item.getAssetType() == AssetType.STOCK && item.getStockDetail() != null) {
                StockDetail detail = item.getStockDetail();
                quantity = detail.getQuantity();
                country = detail.getCountry();
                StockPriceResponse price = priceMap.get(detail.getStockCode());
                if (price != null && price.getCurrentPriceKrw() != null) {
                    currentPrice = price.getCurrentPriceKrw();
                    changeRate = price.getChangeRate();
                    evaluated = new BigDecimal(price.getCurrentPriceKrw())
                            .multiply(BigDecimal.valueOf(detail.getQuantity()));
                }
                // 외화 주식은 investedAmountKrw 사용
                if (detail.getInvestedAmountKrw() != null) {
                    invested = detail.getInvestedAmountKrw();
                }
            }

            totalInvested = totalInvested.add(invested);
            totalEvaluated = totalEvaluated.add(evaluated);

            evaluations.add(new ItemEvaluation(
                    item.getItemName(),
                    item.getAssetType().name(),
                    country,
                    invested,
                    evaluated,
                    quantity,
                    currentPrice,
                    changeRate
            ));
        }

        return new PortfolioEvaluation(userId, evaluations, totalInvested, totalEvaluated);
    }

    /**
     * 전체 포트폴리오에서 고유 종목을 추출하고 1회씩만 현재가 조회
     */
    private Map<String, StockPriceResponse> fetchDistinctStockPrices(List<PortfolioItem> allItems) {
        Map<String, StockPriceResponse> priceMap = new HashMap<>();

        for (PortfolioItem item : allItems) {
            if (item.getAssetType() != AssetType.STOCK || item.getStockDetail() == null) {
                continue;
            }
            StockDetail detail = item.getStockDetail();
            String stockCode = detail.getStockCode();

            if (priceMap.containsKey(stockCode)) {
                continue;
            }

            try {
                MarketType marketType = MarketType.valueOf(detail.getMarket());
                ExchangeCode exchangeCode = ExchangeCode.valueOf(detail.getExchangeCode());
                StockPriceResponse price = stockPriceService.getPrice(stockCode, marketType, exchangeCode);
                priceMap.put(stockCode, price);
            } catch (Exception e) {
                priceMap.put(stockCode, null);
            }
        }

        return priceMap;
    }
}