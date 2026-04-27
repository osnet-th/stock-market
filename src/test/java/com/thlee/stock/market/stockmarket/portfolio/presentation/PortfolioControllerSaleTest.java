package com.thlee.stock.market.stockmarket.portfolio.presentation;

import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AddStockSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleContextResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.UpdateSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import com.thlee.stock.market.stockmarket.portfolio.presentation.dto.StockSaleHistoryUpdateRequest;
import com.thlee.stock.market.stockmarket.portfolio.presentation.dto.StockSaleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioController — 매도 엔드포인트")
class PortfolioControllerSaleTest {

    @Mock PortfolioService portfolioService;
    @Mock PortfolioAllocationService portfolioAllocationService;

    @InjectMocks PortfolioController controller;

    private static final Long USER_ID = 1L;
    private static final Long STOCK_ITEM_ID = 100L;
    private static final Long HISTORY_ID = 300L;
    private static final LocalDate TODAY = LocalDate.now();

    private StockSaleHistoryResponse sampleResponse() {
        StockSaleHistory history = StockSaleHistory.create(
                STOCK_ITEM_ID, 3, BigDecimal.valueOf(70_000), BigDecimal.valueOf(80_000),
                "KRW", BigDecimal.ONE, BigDecimal.valueOf(10_000_000),
                SaleReason.TARGET_PRICE_REACHED, "익절", "005930", "삼성전자",
                false, TODAY, TODAY
        );
        return StockSaleHistoryResponse.from(history);
    }

    private StockSaleRequest fillSaleRequest(int quantity, BigDecimal salePrice) {
        StockSaleRequest req = new StockSaleRequest();
        ReflectionTestUtils.setField(req, "quantity", quantity);
        ReflectionTestUtils.setField(req, "salePrice", salePrice);
        ReflectionTestUtils.setField(req, "soldAt", TODAY);
        ReflectionTestUtils.setField(req, "reason", SaleReason.TARGET_PRICE_REACHED);
        ReflectionTestUtils.setField(req, "memo", "익절");
        return req;
    }

    @Test
    @DisplayName("POST /sale → 200 + StockSaleHistoryResponse")
    void addStockSale_returns200() {
        StockSaleHistoryResponse expected = sampleResponse();
        given(portfolioService.addStockSale(eq(USER_ID), eq(STOCK_ITEM_ID), org.mockito.ArgumentMatchers.any(AddStockSaleParam.class)))
                .willReturn(expected);

        ResponseEntity<StockSaleHistoryResponse> resp = controller.addStockSale(
                USER_ID, USER_ID, STOCK_ITEM_ID, fillSaleRequest(3, BigDecimal.valueOf(80_000)));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("POST /sale: Request → AddStockSaleParam 변환 정확")
    void addStockSale_paramMapping() {
        StockSaleRequest req = new StockSaleRequest();
        ReflectionTestUtils.setField(req, "quantity", 5);
        ReflectionTestUtils.setField(req, "salePrice", BigDecimal.valueOf(180));
        ReflectionTestUtils.setField(req, "soldAt", TODAY);
        ReflectionTestUtils.setField(req, "reason", SaleReason.REBALANCING);
        ReflectionTestUtils.setField(req, "memo", "리밸런싱");
        ReflectionTestUtils.setField(req, "fxRate", BigDecimal.valueOf(1_400));
        ReflectionTestUtils.setField(req, "depositCashItemId", 200L);

        ArgumentCaptor<AddStockSaleParam> captor = ArgumentCaptor.forClass(AddStockSaleParam.class);
        given(portfolioService.addStockSale(eq(USER_ID), eq(STOCK_ITEM_ID), captor.capture()))
                .willReturn(sampleResponse());

        controller.addStockSale(USER_ID, USER_ID, STOCK_ITEM_ID, req);

        AddStockSaleParam captured = captor.getValue();
        assertThat(captured.quantity()).isEqualTo(5);
        assertThat(captured.salePrice()).isEqualByComparingTo(BigDecimal.valueOf(180));
        assertThat(captured.reason()).isEqualTo(SaleReason.REBALANCING);
        assertThat(captured.memo()).isEqualTo("리밸런싱");
        assertThat(captured.fxRate()).isEqualByComparingTo(BigDecimal.valueOf(1_400));
        assertThat(captured.depositCashItemId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("GET /sale-context → 200 + StockSaleContextResponse")
    void getSaleContext_returns200() {
        StockSaleContextResponse expected = new StockSaleContextResponse(
                BigDecimal.valueOf(80_000), BigDecimal.valueOf(80_000), "KRW",
                BigDecimal.ONE, BigDecimal.valueOf(10_000_000));
        given(portfolioService.getSaleContext(USER_ID, STOCK_ITEM_ID)).willReturn(expected);

        ResponseEntity<StockSaleContextResponse> resp = controller.getSaleContext(USER_ID, USER_ID, STOCK_ITEM_ID);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("GET /sales (item) → 200 + List")
    void getSaleHistories_returns200() {
        List<StockSaleHistoryResponse> expected = List.of(sampleResponse());
        given(portfolioService.getSaleHistories(USER_ID, STOCK_ITEM_ID)).willReturn(expected);

        ResponseEntity<List<StockSaleHistoryResponse>> resp =
                controller.getSaleHistories(USER_ID, USER_ID, STOCK_ITEM_ID);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("PUT /sales/{historyId} → 200")
    void updateSaleHistory_returns200() {
        StockSaleHistoryUpdateRequest req = new StockSaleHistoryUpdateRequest();
        ReflectionTestUtils.setField(req, "quantity", 2);
        ReflectionTestUtils.setField(req, "salePrice", BigDecimal.valueOf(85_000));
        ReflectionTestUtils.setField(req, "reason", SaleReason.STOP_LOSS);
        ReflectionTestUtils.setField(req, "memo", "수정");

        ArgumentCaptor<UpdateSaleParam> captor = ArgumentCaptor.forClass(UpdateSaleParam.class);
        given(portfolioService.updateSaleHistory(eq(USER_ID), eq(STOCK_ITEM_ID), eq(HISTORY_ID), captor.capture()))
                .willReturn(sampleResponse());

        ResponseEntity<StockSaleHistoryResponse> resp =
                controller.updateSaleHistory(USER_ID, USER_ID, STOCK_ITEM_ID, HISTORY_ID, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        UpdateSaleParam captured = captor.getValue();
        assertThat(captured.quantity()).isEqualTo(2);
        assertThat(captured.salePrice()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
        assertThat(captured.reason()).isEqualTo(SaleReason.STOP_LOSS);
        assertThat(captured.memo()).isEqualTo("수정");
    }

    @Test
    @DisplayName("DELETE /sales/{historyId} → 204")
    void deleteSaleHistory_returns204() {
        ResponseEntity<Void> resp = controller.deleteSaleHistory(USER_ID, USER_ID, STOCK_ITEM_ID, HISTORY_ID);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("GET /sales → 200 + 사용자 전체 매도 이력")
    void getAllUserSaleHistories_returns200() {
        List<StockSaleHistoryResponse> expected = List.of(sampleResponse());
        given(portfolioService.getAllUserSaleHistories(USER_ID)).willReturn(expected);

        ResponseEntity<List<StockSaleHistoryResponse>> resp =
                controller.getAllUserSaleHistories(USER_ID, USER_ID);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }
}