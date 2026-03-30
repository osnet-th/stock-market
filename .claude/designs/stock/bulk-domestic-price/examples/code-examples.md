# 코드 예시

## 1. KisDomesticMultiPriceOutput (새 DTO)

```java
@Getter
@NoArgsConstructor
public class KisDomesticMultiPriceOutput {
    @JsonProperty("inter_shrn_iscd")
    private String stockCode;           // 종목코드

    @JsonProperty("inter_kor_isnm")
    private String stockName;           // 종목명

    @JsonProperty("inter2_prpr")
    private String currentPrice;        // 현재가

    @JsonProperty("inter2_prdy_clpr")
    private String previousClose;       // 전일 종가

    @JsonProperty("inter2_prdy_vrss")
    private String change;              // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String changeSign;          // 전일 대비 부호

    @JsonProperty("prdy_ctrt")
    private String changeRate;          // 전일 대비율

    @JsonProperty("acml_vol")
    private String volume;              // 누적 거래량

    @JsonProperty("acml_tr_pbmn")
    private String tradingAmount;       // 누적 거래대금

    @JsonProperty("inter2_hgpr")
    private String highPrice;           // 고가

    @JsonProperty("inter2_lwpr")
    private String lowPrice;            // 저가

    @JsonProperty("inter2_oprc")
    private String openPrice;           // 시가
}
```

## 2. KisStockPriceClient.getDomesticMultiPrice()

```java
private static final String DOMESTIC_MULTI_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/intstock-multprice";
private static final String DOMESTIC_MULTI_TR_ID = "FHKST11300006";

public List<KisDomesticMultiPriceOutput> getDomesticMultiPrice(List<String> stockCodes) {
    return kisApiClient.get(
        DOMESTIC_MULTI_PRICE_PATH,
        DOMESTIC_MULTI_TR_ID,
        uriBuilder -> {
            for (int i = 0; i < stockCodes.size() && i < 30; i++) {
                int idx = i + 1;
                uriBuilder.queryParam("FID_COND_MRKT_DIV_CODE_" + idx, "J");
                uriBuilder.queryParam("FID_INPUT_ISCD_" + idx, stockCodes.get(i));
            }
            return uriBuilder.build();
        },
        new ParameterizedTypeReference<>() {},
        "국내 멀티종목 현재가 조회 [" + stockCodes.size() + "종목]"
    );
}
```

참고: 응답이 `output` 배열(List)이므로, `KisApiResponse`의 제네릭 타입이 `List<KisDomesticMultiPriceOutput>`이어야 함. 기존 `KisApiResponse<T>`에서 `output` 필드가 단건 객체인데, 멀티종목은 배열로 내려옴. `KisApiClient.get()`의 반환 타입 처리 필요.

## 3. KisStockPriceAdapter.getDomesticPrices()

```java
public Map<String, StockPrice> getDomesticPrices(List<String> stockCodes) {
    Map<String, StockPrice> result = new LinkedHashMap<>();
    // 30개씩 분할
    for (int i = 0; i < stockCodes.size(); i += 30) {
        List<String> batch = stockCodes.subList(i, Math.min(i + 30, stockCodes.size()));
        List<KisDomesticMultiPriceOutput> outputs = priceClient.getDomesticMultiPrice(batch);
        for (KisDomesticMultiPriceOutput output : outputs) {
            StockPrice price = KisStockPriceMapper.fromDomesticMulti(output);
            result.put(output.getStockCode(), price);
            // 개별 캐시에도 저장
            putCache(output.getStockCode(), ExchangeCode.KRX, price);
        }
    }
    return result;
}
```

## 4. StockPriceService.getPrices() 변경

```java
public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
    Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

    // 국내/해외 분리
    List<BulkStockPriceRequest.StockPriceItem> domesticStocks = new ArrayList<>();
    List<BulkStockPriceRequest.StockPriceItem> overseasStocks = new ArrayList<>();
    for (BulkStockPriceRequest.StockPriceItem item : stocks) {
        if (item.getMarketType().isDomestic()) {
            domesticStocks.add(item);
        } else {
            overseasStocks.add(item);
        }
    }

    // 국내: 멀티종목 일괄 조회
    if (!domesticStocks.isEmpty()) {
        List<String> stockCodes = domesticStocks.stream()
            .map(BulkStockPriceRequest.StockPriceItem::getStockCode)
            .toList();
        Map<String, StockPrice> domesticPrices = stockPricePort.getDomesticPrices(stockCodes);
        BigDecimal krwRate = exchangeRatePort.getRate("KRW"); // 1.0
        for (var item : domesticStocks) {
            StockPrice price = domesticPrices.get(item.getStockCode());
            if (price != null) {
                prices.put(item.getStockCode(), StockPriceResponse.from(price, "KRW", krwRate));
            } else {
                prices.put(item.getStockCode(), null);
            }
        }
    }

    // 해외: 기존 개별 조회
    for (var item : overseasStocks) {
        try {
            StockPriceResponse response = getPrice(
                item.getStockCode(), item.getMarketType(), item.getExchangeCode());
            prices.put(item.getStockCode(), response);
        } catch (Exception e) {
            prices.put(item.getStockCode(), null);
        }
    }

    return new BulkStockPriceResponse(prices);
}
```

## 5. KIS 멀티종목 API 응답 필드 매핑 (전체)

| 영문 키 | 한글명 | 비고 |
|---------|--------|------|
| `inter_shrn_iscd` | 종목코드 | 사용 |
| `inter_kor_isnm` | 종목명 | 사용 |
| `inter2_prpr` | 현재가 | 사용 |
| `inter2_prdy_clpr` | 전일 종가 | 사용 |
| `inter2_prdy_vrss` | 전일 대비 | 사용 |
| `prdy_vrss_sign` | 전일 대비 부호 | 사용 |
| `prdy_ctrt` | 전일 대비율 | 사용 |
| `acml_vol` | 누적 거래량 | 사용 |
| `acml_tr_pbmn` | 누적 거래대금 | 사용 |
| `inter2_hgpr` | 고가 | 사용 |
| `inter2_lwpr` | 저가 | 사용 |
| `inter2_oprc` | 시가 | 사용 |
| `inter2_sdpr` | 기준가 | 미사용 |
| `inter2_mxpr` | 상한가 | 미사용 |
| `inter2_llam` | 하한가 | 미사용 |
| `inter2_askp` | 매도호가 | 미사용 |
| `inter2_bidp` | 매수호가 | 미사용 |
| `seln_rsqn` | 매도 잔량 | 미사용 |
| `shnu_rsqn` | 매수 잔량 | 미사용 |
| `total_askp_rsqn` | 총 매도호가 잔량 | 미사용 |
| `total_bidp_rsqn` | 총 매수호가 잔량 | 미사용 |
| `kospi_kosdaq_cls_name` | 코스피/코스닥 구분 | 미사용 |
| `mrkt_trtm_cls_name` | 시장 조치 구분 | 미사용 |
| `hour_cls_code` | 시간 구분 코드 | 미사용 |
| `oprc_vrss_hgpr_rate` | 시가 대비 최고가 비율 | 미사용 |
| `intr_antc_cntg_vrss` | 예상 체결 대비 | 미사용 |
| `intr_antc_cntg_vrss_sign` | 예상 체결 대비 부호 | 미사용 |
| `intr_antc_cntg_prdy_ctrt` | 예상 체결 전일 대비율 | 미사용 |
| `intr_antc_vol` | 예상 거래량 | 미사용 |