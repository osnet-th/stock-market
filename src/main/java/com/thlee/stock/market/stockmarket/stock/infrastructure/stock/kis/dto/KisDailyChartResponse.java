package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KIS 국내주식기간별시세 (FHKST03010100) 응답.
 *
 * <p>output1 (요약) 은 사용하지 않고 output2 (일자별 시세 배열) 만 매핑한다. KisApiResponse 는
 * 단일 output 만 지원하므로 별도 응답 wrapper 로 분리.
 */
@Getter
@NoArgsConstructor
public class KisDailyChartResponse {

    @JsonProperty("rt_cd")
    private String resultCode;

    @JsonProperty("msg_cd")
    private String messageCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output2")
    private List<KisDailyChartItem> items;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}