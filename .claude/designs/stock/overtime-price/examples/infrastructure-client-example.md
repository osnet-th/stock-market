# KisStockPriceClient 변경 예시

## 추가되는 상수 및 메서드

```java
// 기존 상수에 추가
private static final String OVERTIME_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-overtime-price";
private static final String OVERTIME_TR_ID = "FHPST02300000";

/**
 * 국내 주식 시간외 단일가 현재가 조회.
 *
 * @param stockCode 종목코드 6자리 (예: 005930)
 */
public KisOvertimePriceOutput getDomesticOvertimePrice(String stockCode) {
    return kisApiClient.get(
        OVERTIME_PRICE_PATH,
        OVERTIME_TR_ID,
        uriBuilder -> uriBuilder
            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
            .queryParam("FID_INPUT_ISCD", stockCode)
            .build(),
        new ParameterizedTypeReference<>() {},
        "국내 시간외 현재가 조회 [" + stockCode + "]"
    );
}
```

요청 파라미터는 정규장 API와 동일. Path와 TR ID만 변경.