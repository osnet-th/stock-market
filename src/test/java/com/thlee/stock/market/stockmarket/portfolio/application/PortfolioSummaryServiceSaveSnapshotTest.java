package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioSnapshot;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioSnapshotRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioSummaryService — 평가 결과로 스냅샷 저장")
class PortfolioSummaryServiceSaveSnapshotTest {

    private static final Long USER_ID = 1L;

    @Mock
    PortfolioEvaluationService portfolioEvaluationService;

    @Mock
    PortfolioIncomeService portfolioIncomeService;

    @Mock
    PortfolioSnapshotRepository snapshotRepository;

    @Mock
    PortfolioItemRepository portfolioItemRepository;

    @Mock
    StockPurchaseHistoryRepository purchaseHistoryRepository;

    @Mock
    StockPriceService stockPriceService;

    @InjectMocks
    PortfolioSummaryService service;

    @Test
    @DisplayName("B1 당일 스냅샷이 없으면 평가액·원금으로 새로 저장한다")
    void createsSnapshotWhenAbsent() {
        given(snapshotRepository.findByUserIdAndSnapshotDate(eq(USER_ID), any()))
                .willReturn(Optional.empty());
        given(snapshotRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.saveSnapshot(USER_ID, evaluation("1000", "800"));

        PortfolioSnapshot saved = captureSaved();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getSnapshotDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getTotalEvaluated()).isEqualByComparingTo("1000");
        assertThat(saved.getTotalInvested()).isEqualByComparingTo("800");
    }

    @Test
    @DisplayName("B2 당일 스냅샷이 있으면 id를 유지한 채 금액만 갱신한다")
    void refreshesSnapshotWhenPresent() {
        PortfolioSnapshot existing = new PortfolioSnapshot(99L, USER_ID, LocalDate.now(),
                new BigDecimal("500"), new BigDecimal("400"), LocalDateTime.now());
        given(snapshotRepository.findByUserIdAndSnapshotDate(eq(USER_ID), any()))
                .willReturn(Optional.of(existing));
        given(snapshotRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.saveSnapshot(USER_ID, evaluation("1000", "800"));

        PortfolioSnapshot saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getTotalEvaluated()).isEqualByComparingTo("1000");
        assertThat(saved.getTotalInvested()).isEqualByComparingTo("800");
    }

    @Test
    @DisplayName("B3 평가 결과의 사용자와 저장 대상 사용자가 다르면 저장하지 않는다")
    void rejectsMismatchedUser() {
        PortfolioEvaluation otherUsers = new PortfolioEvaluation(2L, List.of(),
                new BigDecimal("800"), new BigDecimal("1000"));

        assertThatThrownBy(() -> service.saveSnapshot(USER_ID, otherUsers))
                .isInstanceOf(IllegalArgumentException.class);
        verify(snapshotRepository, never()).save(any());
    }

    private PortfolioSnapshot captureSaved() {
        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        return captor.getValue();
    }

    private PortfolioEvaluation evaluation(String evaluated, String invested) {
        return new PortfolioEvaluation(USER_ID, List.of(),
                new BigDecimal(invested), new BigDecimal(evaluated));
    }
}
