package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockQuantity;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartApiResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartStockTotqyItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DartFinancialAdapter — 주식총수 현황 변환")
class DartFinancialAdapterTest {

    @Mock
    DartApiClient dartApiClient;

    @Mock
    DartCorpCodeCache corpCodeCache;

    @InjectMocks
    DartFinancialAdapter adapter;

    @Test
    @DisplayName("F1 주식총수 응답의 결산기준일(stlm_dt)이 도메인 모델까지 전달된다")
    void settlementDateIsCarried() throws Exception {
        given(corpCodeCache.getCorpCode("005930")).willReturn("00126380");
        given(dartApiClient.fetchStockTotalQuantity("00126380", "2026", "11012")).willReturn(response("""
                {"status":"000","message":"정상","list":[
                  {"se":"합계","distb_stock_co":"6,566,563,106","stlm_dt":"2026-06-30"}]}
                """));

        List<StockQuantity> quantities = adapter.getStockQuantities("005930", "2026", "11012");

        assertThat(quantities).singleElement().satisfies(q -> {
            assertThat(q.category()).isEqualTo("합계");
            assertThat(q.settlementDate()).isEqualTo("2026-06-30");
        });
    }

    private DartApiResponse<DartStockTotqyItem> response(String json) throws Exception {
        return new ObjectMapper().readValue(json, new TypeReference<>() {});
    }
}
