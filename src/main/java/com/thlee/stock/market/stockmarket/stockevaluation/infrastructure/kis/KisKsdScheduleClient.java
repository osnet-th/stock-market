package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisKsdResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.KisApiClient;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * KIS 예탁원정보(일정) API 호출 클라이언트.
 * 12종 모두 기간(F_DT/T_DT) 단위 조회 + SHT_CD(종목 필터) 구조가 동일하므로 단일 메서드로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class KisKsdScheduleClient {

    private final KisApiClient kisApiClient;

    /**
     * 예탁원 일정 조회.
     *
     * @param type      일정 종류(경로/tr_id/추가 파라미터 보유)
     * @param stockCode 종목코드 (SHT_CD 필터)
     * @param fromDate  조회 시작일 (YYYYMMDD)
     * @param toDate    조회 종료일 (YYYYMMDD)
     * @return 일정 목록 (output1)
     */
    public List<Map<String, String>> fetch(KsdScheduleType type, String stockCode, String fromDate, String toDate) {
        KisKsdResponse response = kisApiClient.getRaw(
            type.getPath(),
            type.getTrId(),
            uriBuilder -> buildUri(uriBuilder, type, stockCode, fromDate, toDate),
            new ParameterizedTypeReference<>() {},
            type.name() + " 조회 [" + stockCode + "]"
        );
        if (response == null || !response.isSuccess()) {
            throw new KisApiException(type.name() + " 조회 실패");
        }
        return response.getOutput1();
    }

    private URI buildUri(UriBuilder uriBuilder, KsdScheduleType type,
                         String stockCode, String fromDate, String toDate) {
        uriBuilder
            .queryParam("CTS", "")
            .queryParam("F_DT", fromDate)
            .queryParam("T_DT", toDate)
            .queryParam("SHT_CD", stockCode);
        type.getExtraParams().forEach(uriBuilder::queryParam);
        return uriBuilder.build();
    }
}
