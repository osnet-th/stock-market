package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DartApiResponse<T> {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("list")
    private List<T> list;

    public boolean isSuccess() {
        return "000".equals(status);
    }
}