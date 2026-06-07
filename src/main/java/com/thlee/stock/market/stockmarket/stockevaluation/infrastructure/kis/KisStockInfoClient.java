package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisEstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisIndexDailyResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchStockInfoOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.KisApiClient;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * KIS 국내주식 종목정보(단일 종목) API 호출 클라이언트.
 * 상품기본조회, 종목추정실적, 당사 신용가능종목 조회를 담당한다.
 */
@Component
@RequiredArgsConstructor
public class KisStockInfoClient {

    private static final String SEARCH_INFO_PATH = "/uapi/domestic-stock/v1/quotations/search-info";
    private static final String SEARCH_INFO_TR_ID = "CTPF1604R";
    private static final String PRDT_TYPE_CD_STOCK = "300"; // 주식

    private static final String SEARCH_STOCK_INFO_PATH = "/uapi/domestic-stock/v1/quotations/search-stock-info";
    private static final String SEARCH_STOCK_INFO_TR_ID = "CTPF1002R";

    private static final String ESTIMATE_PERFORM_PATH = "/uapi/domestic-stock/v1/quotations/estimate-perform";
    private static final String ESTIMATE_PERFORM_TR_ID = "HHKST668300C0";

    private static final String CREDIT_PATH = "/uapi/domestic-stock/v1/quotations/credit-by-company";
    private static final String CREDIT_TR_ID = "FHPST04770000";

    private static final String INDEX_DAILY_PATH = "/uapi/domestic-stock/v1/quotations/inquire-index-daily-price";
    private static final String INDEX_DAILY_TR_ID = "FHPUP02120000";

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

    /**
     * 주식기본조회 (요약 카드용 — 업종/상장주식수/자본금/액면가/종가 등).
     *
     * @param stockCode 종목코드 (KRX 6자리)
     * @return 주식 기본정보 (단일 output)
     */
    public KisSearchStockInfoOutput searchStockInfo(String stockCode) {
        return kisApiClient.get(
            SEARCH_STOCK_INFO_PATH,
            SEARCH_STOCK_INFO_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("PDNO", stockCode)
                .queryParam("PRDT_TYPE_CD", PRDT_TYPE_CD_STOCK)
                .build(),
            new ParameterizedTypeReference<>() {},
            "주식기본조회 [" + stockCode + "]"
        );
    }

    /**
     * 종목추정실적 조회 (output1~4 다중).
     *
     * @param stockCode 종목코드 (KRX 6자리)
     */
    public KisEstimatePerformResponse estimatePerform(String stockCode) {
        KisEstimatePerformResponse response = kisApiClient.getRaw(
            ESTIMATE_PERFORM_PATH,
            ESTIMATE_PERFORM_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("SHT_CD", stockCode)
                .build(),
            new ParameterizedTypeReference<>() {},
            "종목추정실적 조회 [" + stockCode + "]"
        );
        if (response == null || !response.isSuccess()) {
            throw new KisApiException("종목추정실적 조회 실패");
        }
        return response;
    }

    /**
     * 당사 신용가능종목 조회 (신용주문 가능 종목 전체 목록).
     * 단일 종목 조회 API가 없어 전체 목록을 받아 호출자가 종목코드 포함 여부로 판정한다.
     *
     * @return 신용주문 가능 종목 목록 (stck_shrn_iscd, hts_kor_isnm, crdt_rate)
     */
    public List<Map<String, String>> creditEligibleList() {
        return kisApiClient.get(
            CREDIT_PATH,
            CREDIT_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("fid_rank_sort_cls_code", "0")   // 0: 코드순
                .queryParam("fid_slct_yn", "0")              // 0: 신용주문가능
                .queryParam("fid_input_iscd", "0000")        // 0000: 전체
                .queryParam("fid_cond_scr_div_code", "20477")
                .queryParam("fid_cond_mrkt_div_code", "J")
                .build(),
            new ParameterizedTypeReference<>() {},
            "신용가능종목 조회"
        );
    }

    /**
     * 국내업종 일자별지수 조회.
     *
     * @param indexCode 지수업종 코드 (4자리, 예: 0013 전기,전자 / 0001 KOSPI종합)
     * @param fromDate  기준일자 (YYYYMMDD) — 이 일자부터 과거로 최대 100건
     * @return output2(일자별 지수 목록, 최신→과거)
     */
    public List<Map<String, String>> industryIndexDaily(String indexCode, String fromDate) {
        KisIndexDailyResponse response = kisApiClient.getRaw(
            INDEX_DAILY_PATH,
            INDEX_DAILY_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("FID_PERIOD_DIV_CODE", "D")
                .queryParam("FID_COND_MRKT_DIV_CODE", "U")  // U: 업종
                .queryParam("FID_INPUT_ISCD", indexCode)
                .queryParam("FID_INPUT_DATE_1", fromDate)
                .build(),
            new ParameterizedTypeReference<>() {},
            "업종 일자별지수 조회 [" + indexCode + "]"
        );
        if (response == null || !response.isSuccess()) {
            throw new KisApiException("업종 지수 조회 실패");
        }
        return response.getOutput2();
    }
}
