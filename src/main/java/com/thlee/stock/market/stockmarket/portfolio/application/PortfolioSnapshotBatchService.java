package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.stock.domain.service.MarketCalendarPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.StockPriceCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 자산 스냅샷 일 1회 저장 배치.
 *
 * <p>트랜잭션을 걸지 않는다. 저장은 {@link PortfolioSummaryService#saveSnapshot} 이 사용자별로
 * 맡으며, 배치 전체를 한 트랜잭션으로 묶으면 한 사용자의 실패가 전체를 롤백시킨다.</p>
 */
@Slf4j
@Service
public class PortfolioSnapshotBatchService {

    private final MarketCalendarPort marketCalendarPort;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioEvaluationService portfolioEvaluationService;
    private final PortfolioSummaryService portfolioSummaryService;
    private final CacheManager stockPriceCacheManager;

    public PortfolioSnapshotBatchService(MarketCalendarPort marketCalendarPort,
                                         PortfolioItemRepository portfolioItemRepository,
                                         PortfolioEvaluationService portfolioEvaluationService,
                                         PortfolioSummaryService portfolioSummaryService,
                                         @Qualifier("stockPriceCacheManager") CacheManager stockPriceCacheManager) {
        this.marketCalendarPort = marketCalendarPort;
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioEvaluationService = portfolioEvaluationService;
        this.portfolioSummaryService = portfolioSummaryService;
        this.stockPriceCacheManager = stockPriceCacheManager;
    }

    /**
     * 개장일이면 보유 항목이 있는 사용자 전원의 오늘자 스냅샷을 저장한다.
     *
     * <p>평가는 대상 전원을 한 번에 수행해 고유 종목 시세 조회를 인원과 무관하게 1회로 고정하고,
     * 저장은 사용자별로 격리한다.</p>
     *
     * @return 저장에 성공한 사용자 수
     */
    public int saveDailySnapshots() {
        LocalDate today = LocalDate.now();
        if (!isMarketOpen(today)) {
            log.info("휴장일이라 자산 스냅샷을 저장하지 않습니다: date={}", today);
            return 0;
        }

        List<Long> userIds = portfolioItemRepository.findUserIdsWithActiveItems();
        if (userIds.isEmpty()) {
            log.info("보유 항목이 있는 사용자가 없어 자산 스냅샷을 저장하지 않습니다.");
            return 0;
        }

        evictStockPriceCache();
        Map<Long, PortfolioEvaluation> evaluations =
                portfolioEvaluationService.evaluatePortfolios(userIds);

        int savedCount = 0;
        for (Long userId : userIds) {
            PortfolioEvaluation evaluation = evaluations.get(userId);
            if (evaluation == null) {
                log.warn("평가 결과가 없어 저장을 건너뜁니다: userId={}", userId);
                continue;
            }
            if (hasNoUsableStockPrice(evaluation)) {
                log.error("주식 시세를 한 건도 받지 못해 저장을 건너뜁니다: userId={}", userId);
                continue;
            }
            try {
                portfolioSummaryService.saveSnapshot(userId, evaluation);
                savedCount++;
            } catch (Exception e) {
                log.error("자산 스냅샷 저장 실패: userId={}", userId, e);
            }
        }

        log.info("자산 스냅샷 저장 완료: 성공={}/전체={}", savedCount, userIds.size());
        return savedCount;
    }

    /**
     * 개장일 여부를 판별한다. 조회 자체가 실패하면 경고만 남기고 개장일로 간주해 저장을 진행한다.
     *
     * <p>조회 실패로 그날 기록이 영구히 비는 것보다, 휴장일에 전일과 같은 값이 한 건 더 남는 쪽의
     * 피해가 작다. 지나간 날짜의 시세는 되살릴 수 없다.</p>
     */
    private boolean isMarketOpen(LocalDate date) {
        try {
            return marketCalendarPort.isOpen(date);
        } catch (Exception e) {
            log.warn("개장일 조회에 실패해 자산 스냅샷 저장을 그대로 진행합니다: date={}", date, e);
            return true;
        }
    }

    /**
     * 시세 캐시를 비우고 평가를 시작한다.
     *
     * <p>시세 캐시는 쓰기 후 30분간 유효하다. 10분 앞서 도는 마감 알림 배치나 그 사이 사용자 화면
     * 조회가 캐시를 채워두면, 이 배치는 KIS 를 다시 호출하지 않고 장중 가격을 그대로 저장한다.
     * 그러면 스냅샷마다 기준 시점이 달라져 자산 추이 비교가 무의미해진다. 캐시를 비워 이 배치가
     * 항상 마감 직후 가격을 받도록 한다.</p>
     */
    private void evictStockPriceCache() {
        Cache cache = stockPriceCacheManager.getCache(StockPriceCacheConfig.STOCK_PRICE_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 보유한 주식 항목이 있는데 유효한 현재가가 하나도 없는 상태인지 판정한다.
     *
     * <p>시세 조회가 전부 실패하면 평가액이 원금과 같아진다. 수익률 0% 는 그럴싸한 값이라 사람 눈으로
     * 구분되지 않고, 저장하면 같은 날 기존 기록까지 덮어쓴다. 지나간 날짜는 되살릴 수 없으므로
     * 잘못된 값을 남기는 것보다 그날을 비우는 편이 낫다.</p>
     */
    private boolean hasNoUsableStockPrice(PortfolioEvaluation evaluation) {
        List<ItemEvaluation> stockItems = evaluation.getItems().stream()
                .filter(item -> AssetType.STOCK.name().equals(item.getAssetType()))
                .toList();
        return !stockItems.isEmpty()
                && stockItems.stream().allMatch(item -> item.getCurrentPrice() == null);
    }
}
