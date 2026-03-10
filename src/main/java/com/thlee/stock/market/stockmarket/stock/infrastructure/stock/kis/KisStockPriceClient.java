package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * KIS 주식 현재가 조회 클라이언트.
 * KisApiClient에 현재가 전용 파라미터를 조립하여 위임한다.
 */
@Component
@RequiredArgsConstructor
public class KisStockPriceClient {

    private static final String DOMESTIC_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String OVERSEAS_PRICE_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String DOMESTIC_TR_ID = "FHKST01010100";
    private static final String OVERSEAS_TR_ID = "HHDFS00000300";

    private final KisApiClient kisApiClient;

    /**
     * 국내 주식/ETF 현재가 조회.
     *
     * @param stockCode 종목코드 6자리 (예: 005930)
     */
    public KisDomesticPriceOutput getDomesticPrice(String stockCode) {
        return kisApiClient.get(
            DOMESTIC_PRICE_PATH,
            DOMESTIC_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build(),
            new ParameterizedTypeReference<>() {},
            "국내 현재가 조회 [" + stockCode + "]"
        );
    }

    /**
     * 해외 주식/ETF 현재가 조회.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드
     */
    public KisOverseasPriceOutput getOverseasPrice(String stockCode, ExchangeCode exchangeCode) {
        return kisApiClient.get(
            OVERSEAS_PRICE_PATH,
            OVERSEAS_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("AUTH", "")
                .queryParam("EXCD", exchangeCode.name())
                .queryParam("SYMB", stockCode)
                .build(),
            new ParameterizedTypeReference<>() {},
            "해외 현재가 조회 [" + exchangeCode.name() + ":" + stockCode + "]"
        );
    }
}