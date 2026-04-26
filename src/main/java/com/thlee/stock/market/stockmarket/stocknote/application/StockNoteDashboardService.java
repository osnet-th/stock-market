package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteDashboardRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.cache.StocknoteCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 KPI 집계 — 화면 단위 5개 쿼리를 {@link StockNoteDashboardRepository} 어댑터에 위임하고
 * 결과만 조립한다 (ARCHITECTURE.md 포트-어댑터 정합).
 *
 * <p>캐시: stocknoteCacheManager/stocknoteDashboard, TTL 30분 (심화 E/Caffeine 리서치).
 * CUD 시 {@code @CacheEvict} 로 무효화 — WriteService/VerificationService 에서 처리.
 */
@Service
@RequiredArgsConstructor
public class StockNoteDashboardService {

    private static final int TOP_TAG_COMBO_LIMIT = 5;

    private final StockNoteDashboardRepository dashboardRepository;

    @Cacheable(
            cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager",
            key = "#userId",
            sync = true
    )
    @Transactional(readOnly = true)
    public DashboardResult getDashboard(Long userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastOfMonth = firstOfMonth.plusMonths(1).minusDays(1);

        long thisMonthCount = dashboardRepository.countByUserAndNoteDateBetween(userId, firstOfMonth, lastOfMonth);
        DashboardResult.HitRate hitRate = dashboardRepository.aggregateHitRate(userId);
        long pendingVerificationCount = dashboardRepository.countPendingVerification(userId);
        Map<String, Long> characterDistribution = dashboardRepository.aggregateCharacterDistribution(userId);
        List<DashboardResult.TagComboEntry> topCombos =
                dashboardRepository.aggregateTopTagCombos(userId, TOP_TAG_COMBO_LIMIT);

        return new DashboardResult(
                thisMonthCount,
                hitRate.total(),
                pendingVerificationCount,
                hitRate,
                characterDistribution,
                topCombos
        );
    }
}