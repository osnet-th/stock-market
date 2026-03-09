# KIS 범용 API 클라이언트 분리 설계

## 작업 리스트

- [x] KisApiClient 작성 (범용 KIS HTTP 클라이언트)
- [x] KisStockPriceClient에서 공통 로직 제거, KisApiClient 위임으로 변경

## 배경

현재 `KisStockPriceClient`에 KIS 공통 헤더 세팅, 응답 검증, RestClient 호출 로직이 현재가 조회 전용으로 결합되어 있다. 향후 다른 KIS API(주문, 잔고 조회 등) 추가 시 동일한 공통 로직을 중복 작성해야 한다.

## 핵심 결정

- **KisApiClient**: KIS API 공통 관심사(인증 헤더, 응답 검증, base URL)를 캡슐화한 범용 HTTP 클라이언트
- **KisStockPriceClient**: `KisApiClient`를 주입받아 현재가 전용 파라미터(path, tr_id, query params)만 조립
- **응답 검증 범용화**: `KisPriceApiResponse` → `KisApiResponse`로 이름 변경하여 범용 wrapper로 사용

## 구현

### KisApiClient (범용 클라이언트)

위치: `stock/infrastructure/stock/kis/KisApiClient.java`

KIS API 공통 로직을 담당하는 범용 HTTP 클라이언트. 인증 헤더 세팅, 응답 검증, GET 요청 수행.

[예시 코드](./examples/kis-api-client-example.md)

### KisStockPriceClient 리팩토링

기존 코드에서 공통 로직을 제거하고, `KisApiClient`에 위임.

[예시 코드](./examples/kis-stock-price-client-example.md)