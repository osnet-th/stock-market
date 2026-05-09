package com.thlee.stock.market.stockmarket.realestate.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceFactory;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 부동산 일배치 스케줄러 — 매일 06:00 KST.
 * <p>
 * region × adapter × supportedCategories 모든 조합을 순회하며 captureExecutor를 호출한다.
 * 자체는 {@code @Transactional} 없음, try-catch만으로 항목 단위 격리.
 */
@Component
@Slf4j
public class RealEstateMarketBatchScheduler {

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final long ITEM_TIMEOUT_SECONDS = 60;

    private final RealEstateRegionRepository regionRepository;
    private final RealEstateMarketSourceFactory sourceFactory;
    private final RealEstateMarketCaptureExecutor captureExecutor;
    private final ThreadPoolTaskExecutor fetchExecutor;

    public RealEstateMarketBatchScheduler(
            RealEstateRegionRepository regionRepository,
            RealEstateMarketSourceFactory sourceFactory,
            RealEstateMarketCaptureExecutor captureExecutor,
            @Qualifier(RealEstateMarketAsyncConfig.FETCH_EXECUTOR_BEAN) ThreadPoolTaskExecutor fetchExecutor) {
        this.regionRepository = regionRepository;
        this.sourceFactory = sourceFactory;
        this.captureExecutor = captureExecutor;
        this.fetchExecutor = fetchExecutor;
    }

    @Scheduled(cron = "${realestate.batch.cron:0 0 6 * * *}", zone = "Asia/Seoul")
    public void runDailyBatch() {
        try (var ctx = LoggingContext.forScheduler("realestate-market-batch")) {
            log.info("[realestate.batch] start");
            long startNs = System.nanoTime();

            FetchWindow window = FetchWindow.lastNDays(LocalDate.now(), DEFAULT_WINDOW_DAYS);
            List<RealEstateRegion> regions = regionRepository.findAllSigunguOnly();
            List<RealEstateMarketSourceAdapter> adapters = sourceFactory.getAll();

            int success = 0;
            int failure = 0;
            int totalSaved = 0;
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (RealEstateRegion region : regions) {
                RegionCode regionCode = RegionCode.of(region.getRegionCode());
                for (RealEstateMarketSourceAdapter adapter : adapters) {
                    if (!adapter.supportsRegion(regionCode)) {
                        continue;  // 지역 특화 출처(서울/경기) 헛 submit 사전 차단
                    }
                    for (RealEstateMarketCategory category : adapter.supportedCategories()) {
                        futures.add(submit(adapter, regionCode, category, window));
                    }
                }
            }

            for (CompletableFuture<Integer> future : futures) {
                try {
                    int saved = future.get(ITEM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    totalSaved += saved;
                    success++;
                } catch (TimeoutException e) {
                    log.warn("[realestate.batch] item timeout after {}s", ITEM_TIMEOUT_SECONDS);
                    future.cancel(true);
                    failure++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure++;
                } catch (ExecutionException e) {
                    log.warn("[realestate.batch] item failed", e.getCause());
                    failure++;
                }
            }

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("[realestate.batch] completed: success={}, failure={}, savedRows={}, elapsedMs={}",
                    success, failure, totalSaved, elapsedMs);
        } catch (Exception e) {
            log.error("[realestate.batch] unexpected failure", e);
        }
    }

    private CompletableFuture<Integer> submit(RealEstateMarketSourceAdapter adapter,
                                               RegionCode region,
                                               RealEstateMarketCategory category,
                                               FetchWindow window) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return captureExecutor.captureOne(adapter, region, category, window);
                } catch (IllegalStateException e) {
                    log.debug("[realestate.batch] skip (api-key not configured): source={}",
                            adapter.supportedSource());
                    return 0;
                }
            }, fetchExecutor);
        } catch (RejectedExecutionException e) {
            // CallerRunsPolicy 가 1차 방어선이지만, executor shutdown 등 거부 시 항목 단위 격리 (다른 항목 진행).
            log.warn("[realestate.batch] submit rejected: source={}, region={}, category={}",
                    adapter.supportedSource(), region, category);
            return CompletableFuture.completedFuture(-1);
        }
    }
}