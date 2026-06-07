package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * KIS 국내업종 일자별지수 API 응답.
 * tr_id: FHPUP02120000. output2 = 일자별 지수 목록(최신→과거).
 */
@Getter
@NoArgsConstructor
public class KisIndexDailyResponse {

    @JsonProperty("rt_cd")
    private String resultCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output2")
    private List<Map<String, String>> output2;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
