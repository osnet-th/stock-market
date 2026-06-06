package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * KIS 종목추정실적 API 응답 (output1~4 다중).
 * tr_id: HHKST668300C0
 *
 * <p>응답 필드 라벨이 KIS 측 자동생성 placeholder라 신뢰 불가하므로, 각 output은 원시 키-값으로 보관한다.
 */
@Getter
@NoArgsConstructor
public class KisEstimatePerformResponse {

    @JsonProperty("rt_cd")
    private String resultCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output1")
    private List<Map<String, String>> output1;

    @JsonProperty("output2")
    private List<Map<String, String>> output2;

    @JsonProperty("output3")
    private List<Map<String, String>> output3;

    @JsonProperty("output4")
    private List<Map<String, String>> output4;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
