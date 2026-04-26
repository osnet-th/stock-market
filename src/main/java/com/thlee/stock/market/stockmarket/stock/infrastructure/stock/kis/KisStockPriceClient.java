package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDailyChartResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticMultiPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOvertimePriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 주식 현재가 조회 클라이언트.
 * KisApiClient에 현재가 전용 파라미터를 조립하여 위임한다.
 */
@Component
@RequiredArgsConstructor
public class KisStockPriceClient {

    private static final String DOMESTIC_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String DOMESTIC_MULTI_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/intstock-multprice";
    private static final String OVERSEAS_PRICE_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String DOMESTIC_DAILY_CHART_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String DOMESTIC_TR_ID = "FHKST01010100";
    private static final String DOMESTIC_MULTI_TR_ID = "FHKST11300006";
    private static final String OVERTIME_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-overtime-price";
    private static final String OVERTIME_TR_ID = "FHPST02300000";
    private static final String OVERSEAS_TR_ID = "HHDFS00000300";
    private static final String DOMESTIC_DAILY_CHART_TR_ID = "FHKST03010100";
    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

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
     * 국내 주식 시간외 단일가 현재가 조회.
     *
     * @param stockCode 종목코드 6자리 (예: 005930)
     */
    public KisOvertimePriceOutput getDomesticOvertimePrice(String stockCode) {
        return kisApiClient.get(
            OVERTIME_PRICE_PATH,
            OVERTIME_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build(),
            new ParameterizedTypeReference<>() {},
            "국내 시간외 현재가 조회 [" + stockCode + "]"
        );
    }

    /**
     * 국내 주식 멀티종목 시세 일괄조회. 최대 30종목.
     *
     * @param stockCodes 종목코드 목록 (최대 30개)
     */
    public List<KisDomesticMultiPriceOutput> getDomesticMultiPrice(List<String> stockCodes) {
        return kisApiClient.get(
            DOMESTIC_MULTI_PRICE_PATH,
            DOMESTIC_MULTI_TR_ID,
            uriBuilder -> {
                for (int i = 0; i < stockCodes.size() && i < 30; i++) {
                    int idx = i + 1;
                    uriBuilder.queryParam("FID_COND_MRKT_DIV_CODE_" + idx, "J");
                    uriBuilder.queryParam("FID_INPUT_ISCD_" + idx, stockCodes.get(i));
                }
                return uriBuilder.build();
            },
            new ParameterizedTypeReference<>() {},
            "국내 멀티종목 현재가 조회 [" + stockCodes.size() + "종목]"
        );
    }

    /**
     * 국내 주식 일봉(국내주식기간별시세) 조회. 한 번 호출로 최대 약 100 영업일치 반환.
     *
     * @param stockCode 종목코드 6자리
     * @param from      시작일 (포함)
     * @param to        종료일 (포함)
     */
    public KisDailyChartResponse getDomesticDailyChart(String stockCode, LocalDate from, LocalDate to) {
        KisDailyChartResponse response = kisApiClient.getRaw(
            DOMESTIC_DAILY_CHART_PATH,
            DOMESTIC_DAILY_CHART_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_INPUT_DATE_1", from.format(KIS_DATE_FORMAT))
                .queryParam("FID_INPUT_DATE_2", to.format(KIS_DATE_FORMAT))
                .queryParam("FID_PERIOD_DIV_CODE", "D")
                .queryParam("FID_ORG_ADJ_PRC", "0")
                .build(),
            new ParameterizedTypeReference<>() {},
            "국내 일봉 조회 [" + stockCode + " " + from + "~" + to + "]"
        );
        if (!response.isSuccess()) {
            throw new KisApiException("국내 일봉 조회 실패 [" + stockCode + "]: " + response.getMessage());
        }
        return response;
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