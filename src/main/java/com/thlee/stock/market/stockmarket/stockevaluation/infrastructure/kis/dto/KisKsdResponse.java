package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * KIS 예탁원정보(일정) API 공통 응답.
 * 12종 모두 단일 output1(일정 목록) 구조.
 */
@Getter
@NoArgsConstructor
public class KisKsdResponse {

    @JsonProperty("rt_cd")
    private String resultCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output1")
    private List<Map<String, String>> output1;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
