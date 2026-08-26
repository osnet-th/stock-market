package com.thlee.stock.market.stockmarket.realestate.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.realestate.application.RealEstateMarketSaveService;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부동산 일배치 per-item 트랜잭션 격리 빈.
 * <p>
 * 학습: docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md
 * Scheduler가 외부 빈으로 호출해야 Spring AOP 프록시가 트랜잭션을 래핑한다 (self-invocation 금지).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealEstateMarketCaptureExecutor {

    private final RealEstateMarketSaveService saveService;

    /**
     * 한 (region, category, adapter) 단위 fetch + save. 예외는 항목 단위로 격리되며
     * 호출자(Scheduler)가 다음 항목 처리에 영향을 주지 않도록 catch한다.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int captureOne(RealEstateMarketSourceAdapter adapter,
                          RegionCode region,
                          RealEstateMarketCategory category,
                          FetchWindow window) {
        RealEstateMarketSource source = adapter.supportedSource();
        FetchResult result = adapter.fetch(region, category, window);
        if (result.isFailure()) {
            log.warn("[realestate.capture] fetch failure: source={}, region={}, category={}, reason={}",
                    source, region, category, result.failureReason());
            return 0;
        }
        return saveService.upsert(region.value(), category, source, result.indicators());
    }
}
