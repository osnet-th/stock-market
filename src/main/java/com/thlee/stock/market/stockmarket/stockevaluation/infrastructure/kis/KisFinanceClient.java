package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.FinanceStatementType;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.KisApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * KIS 국내주식 재무 API 호출 클라이언트.
 * 재무 7종(대차대조표/손익계산서/각종 비율)은 파라미터·응답 구조가 동일하므로 단일 메서드로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class KisFinanceClient {

    private static final String MARKET_DIV_STOCK = "J"; // 주식

    private final KisApiClient kisApiClient;

    /**
     * 재무 항목 조회.
     *
     * @param type      재무 항목(경로/tr_id 보유)
     * @param stockCode 종목코드 (KRX 6자리)
     * @param divCls    조회 단위 코드 (0:년, 1:분기)
     * @return 기간별 레코드 목록 (단일 output)
     */
    public List<Map<String, String>> fetch(FinanceStatementType type, String stockCode, String divCls) {
        return kisApiClient.get(
            type.getPath(),
            type.getTrId(),
            uriBuilder -> uriBuilder
                .queryParam("FID_DIV_CLS_CODE", divCls)
                .queryParam("FID_COND_MRKT_DIV_CODE", MARKET_DIV_STOCK)
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build(),
            new ParameterizedTypeReference<>() {},
            type.name() + " 조회 [" + stockCode + "]"
        );
    }
}
