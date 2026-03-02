package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EcosKeyStatRow {

    @JsonProperty("CLASS_NAME")
    private String className;

    @JsonProperty("KEYSTAT_NAME")
    private String keystatName;

    @JsonProperty("DATA_VALUE")
    private String dataValue;

    @JsonProperty("CYCLE")
    private String cycle;

    @JsonProperty("UNIT_NAME")
    private String unitName;
}