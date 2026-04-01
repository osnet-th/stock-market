# KisStockPriceMapper 변경 예시

## 추가되는 메서드

```java
public static StockPrice fromOvertime(KisOvertimePriceOutput output,
                                      String stockCode,
                                      MarketType marketType,
                                      ExchangeCode exchangeCode) {
    return new StockPrice(
        stockCode,
        output.getCurrentPrice(),       // ovtm_untp_prpr
        output.getPreviousClose(),      // ovtm_untp_sdpr (기준가)
        output.getChange(),             // ovtm_untp_prdy_vrss
        output.getChangeSign(),         // ovtm_untp_prdy_vrss_sign
        output.getChangeRate(),         // ovtm_untp_prdy_ctrt
        output.getVolume(),             // ovtm_untp_vol
        output.getTradingAmount(),      // ovtm_untp_tr_pbmn
        output.getHighPrice(),          // ovtm_untp_hgpr
        output.getLowPrice(),           // ovtm_untp_lwpr
        output.getOpenPrice(),          // ovtm_untp_oprc
        marketType,
        exchangeCode
    );
}
```

시간외 응답 필드(`ovtm_untp_*`)를 기존 `StockPrice` 도메인 모델과 동일한 구조로 매핑한다.