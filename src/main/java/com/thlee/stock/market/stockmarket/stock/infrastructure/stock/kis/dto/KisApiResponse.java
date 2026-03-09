package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS API 공통 응답 wrapper.
 * 제네릭으로 output 타입을 받아 국내/해외 응답을 통합 처리.
 */
@Getter
@NoArgsConstructor
public class KisApiResponse<T> {

    @JsonProperty("rt_cd")
    private String resultCode;     // 성공: "0"

    @JsonProperty("msg_cd")
    private String messageCode;

    @JsonProperty("msg1")
    private String message;

    private T output;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}