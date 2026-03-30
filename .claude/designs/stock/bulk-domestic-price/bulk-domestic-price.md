# 국내 주식 멀티종목 시세 일괄조회

## 배경

현재 `StockPriceService.getPrices()`가 종목마다 개별 KIS API를 순차 호출하여, 국내 주식 N개 = N번 HTTP 요청 발생. KIS API의 멀티종목 시세조회(`intstock-multprice`)를 활용하면 최대 30종목을 1회 호출로 처리 가능.

## 핵심 결정

- 국내 주식만 멀티종목 API 적용 (해외 주식은 기존 개별 조회 유지)
- 30종목 초과 시 30개씩 분할 요청
- 기존 캐시 전략 유지 (캐시 히트된 종목은 API 호출에서 제외)

## 변경 파일

1. **`KisStockPriceClient.java`** - `getDomesticMultiPrice()` 메서드 추가
2. **`KisStockPriceAdapter.java`** - `getDomesticPrices()` 벌크 메서드 추가
3. **`StockPricePort.java`** - `getDomesticPrices()` 인터페이스 메서드 추가
4. **`StockPriceService.java`** - `getPrices()` 로직 변경: 국내 주식을 묶어서 일괄 조회
5. **새 DTO** - `KisDomesticMultiPriceOutput.java` (멀티종목 응답 매핑)

## 구현 상세

### KIS 멀티종목 API 스펙

- 엔드포인트: `/uapi/domestic-stock/v1/quotations/intstock-multprice`
- TR_ID: `FHKST11300006`
- 파라미터: `FID_COND_MRKT_DIV_CODE_1~30` + `FID_INPUT_ISCD_1~30`
- 최대 30종목/요청
- 응답: output 배열에 종목별 시세 데이터

### StockPriceService.getPrices() 변경 로직

```
1. 요청 목록에서 국내/해외 주식 분리
2. 국내 주식:
   a. 캐시 히트 종목 분리 (개별 캐시 키로 조회)
   b. 캐시 미스 종목만 30개씩 묶어서 멀티종목 API 호출
   c. 응답을 개별 StockPrice로 분해 → 캐시 저장
3. 해외 주식: 기존 개별 조회 유지
4. 결과 합산하여 반환
```

## 예시 코드

[코드 예시](examples/code-examples.md) 참조 - DTO, Client, Adapter, Service 변경 코드 + KIS API 응답 필드 전체 매핑

## 작업 리스트

- [x] KisDomesticMultiPriceOutput DTO 생성
- [x] KisStockPriceClient에 getDomesticMultiPrice() 추가
- [x] StockPricePort에 getDomesticPrices() 메서드 추가
- [x] KisStockPriceAdapter에 getDomesticPrices() 구현 (캐시 히트 분리 + 30개 분할 + 폴백)
- [x] StockPriceService.getPrices() 로직 변경 (국내/해외 분리)

## 주의사항

- 기존 단건 조회 API(`getPrice()`)는 그대로 유지
- 캐시 키 형식 기존과 동일 유지 (`stockCode_exchangeCode`)
- 국내 주식 환율은 항상 1.0 (KRW)