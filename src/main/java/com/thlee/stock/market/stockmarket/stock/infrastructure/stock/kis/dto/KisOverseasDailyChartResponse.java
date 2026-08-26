package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KIS 해외주식 기간별시세 (HHDFS76240000) 응답.
 *
 * <p>호출당 기준일(BYMD) 이전 최대 100봉을 반환한다. output1 (요약) 은 사용하지 않고
 * output2 (일자별 시세 배열) 만 매핑한다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KisOverseasDailyChartResponse {

    @JsonProperty("rt_cd")
    private String resultCode;

    @JsonProperty("msg_cd")
    private String messageCode;

    @JsonProperty("msg1")
    private String message;

    @JsonProperty("output2")
    private List<KisOverseasDailyChartItem> items;

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KisOverseasDailyChartItem {

        /** 일자 (yyyyMMdd, 현지 기준) */
        @JsonProperty("xymd")
        private String businessDate;

        @JsonProperty("open")
        private String openPrice;

        @JsonProperty("high")
        private String highPrice;

        @JsonProperty("low")
        private String lowPrice;

        @JsonProperty("clos")
        private String closePrice;

        @JsonProperty("tvol")
        private String volume;
    }
}
