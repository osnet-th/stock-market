package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.KisApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * KIS 국내주식 종목정보(단일 종목) API 호출 클라이언트.
 * 상품기본조회 등 종목 단위 조회를 담당한다.
 */
@Component
@RequiredArgsConstructor
public class KisStockInfoClient {

    private static final String SEARCH_INFO_PATH = "/uapi/domestic-stock/v1/quotations/search-info";
    private static final String SEARCH_INFO_TR_ID = "CTPF1604R";
    private static final String PRDT_TYPE_CD_STOCK = "300"; // 주식

    private final KisApiClient kisApiClient;

    /**
     * 상품기본조회.
     *
     * @param stockCode 종목코드 (KRX 6자리, 예: 005930)
     * @return 상품 기본정보 (단일 output)
     */
    public KisSearchInfoOutput searchInfo(String stockCode) {
        return kisApiClient.get(
            SEARCH_INFO_PATH,
            SEARCH_INFO_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("PDNO", stockCode)
                .queryParam("PRDT_TYPE_CD", PRDT_TYPE_CD_STOCK)
                .build(),
            new ParameterizedTypeReference<>() {},
            "상품기본조회 [" + stockCode + "]"
        );
    }
}
