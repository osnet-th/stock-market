# Infrastructure DTO 예시

## KisPriceApiResponse (공통 API 응답 wrapper)

```java
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
public class KisPriceApiResponse<T> {

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
```

## KisDomesticPriceOutput (국내 현재가 응답)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 국내 주식 현재가 시세 API 응답 output.
 * tr_id: FHKST01010100
 */
@Getter
@NoArgsConstructor
public class KisDomesticPriceOutput {

    @JsonProperty("stck_prpr")
    private String currentPrice;       // 현재가

    @JsonProperty("stck_oprc")
    private String openPrice;          // 시가

    @JsonProperty("stck_hgpr")
    private String highPrice;          // 고가

    @JsonProperty("stck_lwpr")
    private String lowPrice;           // 저가

    @JsonProperty("stck_sdpr")
    private String previousClose;      // 전일 종가 (기준가)

    @JsonProperty("prdy_vrss")
    private String change;             // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String changeSign;         // 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)

    @JsonProperty("prdy_ctrt")
    private String changeRate;         // 전일 대비율

    @JsonProperty("acml_vol")
    private String volume;             // 누적 거래량

    @JsonProperty("acml_tr_pbmn")
    private String tradingAmount;      // 누적 거래대금

    @JsonProperty("w52_hgpr")
    private String week52High;         // 52주 최고가

    @JsonProperty("w52_lwpr")
    private String week52Low;          // 52주 최저가
}
```

## KisOverseasPriceOutput (해외 현재가 응답)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 해외 주식 현재체결가 API 응답 output.
 * tr_id: HHDFS00000300
 */
@Getter
@NoArgsConstructor
public class KisOverseasPriceOutput {

    @JsonProperty("last")
    private String currentPrice;       // 현재가

    @JsonProperty("base")
    private String previousClose;      // 전일 종가

    @JsonProperty("diff")
    private String change;             // 전일 대비

    @JsonProperty("sign")
    private String changeSign;         // 대비 부호

    @JsonProperty("rate")
    private String changeRate;         // 등락률

    @JsonProperty("tvol")
    private String volume;             // 거래량

    @JsonProperty("tamt")
    private String tradingAmount;      // 거래대금

    @JsonProperty("ordy")
    private String tradeable;          // 매수가능여부
}
```