package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EcosKeyStatResponse {

    @JsonProperty("KeyStatisticList")
    private KeyStatisticList keyStatisticList;

    @Getter
    @Setter
    public static class KeyStatisticList {
        @JsonProperty("list_total_count")
        private int listTotalCount;

        @JsonProperty("row_count")
        private int rowCount;

        private List<EcosKeyStatRow> row;
    }
}