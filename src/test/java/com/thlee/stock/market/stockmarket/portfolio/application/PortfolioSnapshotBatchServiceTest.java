package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.StockPriceCacheConfig;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.stock.domain.service.MarketCalendarPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioSnapshotBatchService — 자산 스냅샷 일 1회 저장 배치")
class PortfolioSnapshotBatchServiceTest {

    @Mock
    MarketCalendarPort marketCalendarPort;

    @Mock
    PortfolioItemRepository portfolioItemRepository;

    @Mock
    PortfolioEvaluationService portfolioEvaluationService;

    @Mock
    PortfolioSummaryService portfolioSummaryService;

    @Mock
    CacheManager stockPriceCacheManager;

    @InjectMocks
    PortfolioSnapshotBatchService service;

    @Test
    @DisplayName("A1 개장일이면 보유 사용자 전원의 스냅샷을 평가 결과대로 저장한다")
    void savesSnapshotsForAllHoldingUsers() {
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L, 2L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L, 2L))).willReturn(Map.of(
                1L, evaluation(1L, "1000", "800"),
                2L, evaluation(2L, "2000", "1500")));

        int saved = service.saveDailySnapshots();

        assertThat(saved).isEqualTo(2);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PortfolioEvaluation> evaluationCaptor =
                ArgumentCaptor.forClass(PortfolioEvaluation.class);
        verify(portfolioSummaryService, times(2))
                .saveSnapshot(userIdCaptor.capture(), evaluationCaptor.capture());

        assertThat(userIdCaptor.getAllValues()).containsExactly(1L, 2L);
        assertThat(evaluationCaptor.getAllValues())
                .extracting(PortfolioEvaluation::getTotalEvaluated)
                .containsExactly(new BigDecimal("1000"), new BigDecimal("2000"));
        assertThat(evaluationCaptor.getAllValues())
                .extracting(PortfolioEvaluation::getTotalInvested)
                .containsExactly(new BigDecimal("800"), new BigDecimal("1500"));
    }

    @Test
    @DisplayName("A2 휴장일이면 평가도 저장도 하지 않는다")
    void skipsEverythingOnMarketClosedDay() {
        given(marketCalendarPort.isOpen(any())).willReturn(false);

        int saved = service.saveDailySnapshots();

        assertThat(saved).isZero();
        verify(portfolioEvaluationService, never()).evaluatePortfolios(any());
        verify(portfolioSummaryService, never()).saveSnapshot(any(), any());
    }

    @Test
    @DisplayName("A3 개장일 조회가 실패하면 저장을 그대로 진행한다")
    void proceedsWhenMarketCalendarLookupFails() {
        given(marketCalendarPort.isOpen(any())).willThrow(new RuntimeException("개장일 조회 실패"));
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L)))
                .willReturn(Map.of(1L, evaluation(1L, "1000", "800")));

        int saved = service.saveDailySnapshots();

        assertThat(saved).isEqualTo(1);
        verify(portfolioSummaryService).saveSnapshot(eq(1L), any());
    }

    @Test
    @DisplayName("A4 보유 사용자가 없으면 평가도 저장도 하지 않는다")
    void skipsWhenNoHoldingUser() {
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of());

        int saved = service.saveDailySnapshots();

        assertThat(saved).isZero();
        verify(portfolioEvaluationService, never()).evaluatePortfolios(any());
        verify(portfolioSummaryService, never()).saveSnapshot(any(), any());
    }

    @Test
    @DisplayName("A5 한 사용자 저장이 실패해도 나머지 사용자는 저장된다")
    void isolatesFailureOfSingleUser() {
        PortfolioEvaluation first = evaluation(1L, "1000", "800");
        PortfolioEvaluation second = evaluation(2L, "2000", "1500");
        PortfolioEvaluation third = evaluation(3L, "3000", "2500");
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L, 2L, 3L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L, 2L, 3L)))
                .willReturn(Map.of(1L, first, 2L, second, 3L, third));
        // 2번 사용자만 실패시키고 1·3번은 기본 동작으로 둔다. strict stubs 는 스텁되지 않은
        // 인자로의 호출 자체를 막으므로 여기서는 lenient 로 등록한다.
        lenient().when(portfolioSummaryService.saveSnapshot(2L, second))
                .thenThrow(new RuntimeException("저장 실패"));

        int saved = service.saveDailySnapshots();

        assertThat(saved).isEqualTo(2);
        verify(portfolioSummaryService).saveSnapshot(1L, first);
        verify(portfolioSummaryService).saveSnapshot(3L, third);
    }

    @Test
    @DisplayName("A6 주식 시세를 한 건도 받지 못한 사용자는 저장하지 않는다")
    void skipsUserWhenNoStockPriceAvailable() {
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L)))
                .willReturn(Map.of(1L, evaluationWith(1L, stockItem(null))));

        int saved = service.saveDailySnapshots();

        assertThat(saved).isZero();
        verify(portfolioSummaryService, never()).saveSnapshot(any(), any());
    }

    @Test
    @DisplayName("A7 보유 항목에 주식이 없으면 시세와 무관하게 저장한다")
    void savesUserWithoutStockItems() {
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L)))
                .willReturn(Map.of(1L, evaluationWith(1L, cashItem())));

        int saved = service.saveDailySnapshots();

        assertThat(saved).isEqualTo(1);
        verify(portfolioSummaryService).saveSnapshot(eq(1L), any());
    }

    @Test
    @DisplayName("A8 평가 전에 시세 캐시를 비워 마감 직후 가격을 받는다")
    void evictsStockPriceCacheBeforeEvaluation() {
        Cache cache = mock(Cache.class);
        given(stockPriceCacheManager.getCache(StockPriceCacheConfig.STOCK_PRICE_CACHE)).willReturn(cache);
        given(marketCalendarPort.isOpen(any())).willReturn(true);
        given(portfolioItemRepository.findUserIdsWithActiveItems()).willReturn(List.of(1L));
        given(portfolioEvaluationService.evaluatePortfolios(List.of(1L)))
                .willReturn(Map.of(1L, evaluation(1L, "1000", "800")));

        service.saveDailySnapshots();

        InOrder inOrder = inOrder(cache, portfolioEvaluationService);
        inOrder.verify(cache).clear();
        inOrder.verify(portfolioEvaluationService).evaluatePortfolios(List.of(1L));
    }

    private PortfolioEvaluation evaluation(Long userId, String evaluated, String invested) {
        return new PortfolioEvaluation(userId, List.of(),
                new BigDecimal(invested), new BigDecimal(evaluated));
    }

    private PortfolioEvaluation evaluationWith(Long userId, ItemEvaluation... items) {
        return new PortfolioEvaluation(userId, List.of(items),
                new BigDecimal("800"), new BigDecimal("800"));
    }

    private ItemEvaluation stockItem(String currentPrice) {
        return new ItemEvaluation(10L, "삼성전자", "STOCK", "KR",
                new BigDecimal("800"), new BigDecimal("800"), 10, currentPrice, null);
    }

    private ItemEvaluation cashItem() {
        return new ItemEvaluation(11L, "예금", "CASH", null,
                new BigDecimal("800"), new BigDecimal("800"), null, null, null);
    }
}
