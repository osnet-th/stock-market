package com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis;

import com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto.KisBreakingNewsOutput;
import com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto.KisNewsOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.KisApiClient;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * KIS 해외뉴스 API 호출 클라이언트.
 * 해외속보(제목)와 해외뉴스종합(제목) API를 호출한다.
 */
@Component
@RequiredArgsConstructor
public class KisOverseasNewsClient {

    private static final String BREAKING_NEWS_PATH = "/uapi/overseas-price/v1/quotations/brknews-title";
    private static final String BREAKING_NEWS_TR_ID = "FHKST01011801";

    private static final String COMPREHENSIVE_NEWS_PATH = "/uapi/overseas-price/v1/quotations/news-title";
    private static final String COMPREHENSIVE_NEWS_TR_ID = "HHPSTH60100C1";

    private final KisApiClient kisApiClient;

    /**
     * 해외속보(제목) 조회.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드 (예: NAS)
     * @return 속보 목록 (최대 100건, 페이지네이션 없음)
     */
    public List<KisBreakingNewsOutput> getBreakingNews(String stockCode, String exchangeCode) {
        return kisApiClient.get(
            BREAKING_NEWS_PATH,
            BREAKING_NEWS_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("FID_NEWS_OFER_ENTP_CODE", "0")
                .queryParam("FID_COND_SCR_DIV_CODE", "11801")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_TITL_CNTT", "")
                .build(),
            new ParameterizedTypeReference<>() {},
            "해외속보 조회 [" + exchangeCode + ":" + stockCode + "]"
        );
    }

    /**
     * 해외뉴스종합(제목) 조회.
     * 연속조회를 지원하며, hasNext가 true이면 마지막 항목의 dataDate/dataTime으로 다음 페이지를 요청한다.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드 (예: NAS)
     * @param countryCode  국가코드 (예: US)
     * @param dataDt       연속조회 기준 일자 (최초: "")
     * @param dataTm       연속조회 기준 시간 (최초: "")
     * @return 뉴스 목록 + 다음 페이지 존재 여부
     */
    public KisApiResult<List<KisNewsOutput>> getComprehensiveNews(String stockCode,
                                                                   String exchangeCode,
                                                                   String countryCode,
                                                                   String dataDt,
                                                                   String dataTm) {
        String trCont = dataDt.isEmpty() ? "" : "N";

        return kisApiClient.getWithContinuation(
            COMPREHENSIVE_NEWS_PATH,
            COMPREHENSIVE_NEWS_TR_ID,
            trCont,
            uriBuilder -> uriBuilder
                .queryParam("NATION_CD", countryCode)
                .queryParam("EXCHANGE_CD", exchangeCode)
                .queryParam("SYMB", stockCode)
                .queryParam("DATA_DT", dataDt)
                .queryParam("DATA_TM", dataTm)
                .queryParam("CTS", "")
                .build(),
            new ParameterizedTypeReference<>() {},
            "해외뉴스종합 조회 [" + exchangeCode + ":" + stockCode + "]"
        );
    }
}