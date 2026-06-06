package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisEstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
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

    private static final String ESTIMATE_PERFORM_PATH = "/uapi/domestic-stock/v1/quotations/estimate-perform";
    private static final String ESTIMATE_PERFORM_TR_ID = "HHKST668300C0";

    private static final String CREDIT_PATH = "/uapi/domestic-stock/v1/quotations/credit-by-company";
    private static final String CREDIT_TR_ID = "FHPST04770000";

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
}
