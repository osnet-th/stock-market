# stockDetail null 간헐 노출 분석

## 작업 리스트
- [x] 콘솔 에러 발생 위치 확인
- [x] `stockDetail` 생성/조회 경로 추적
- [x] 프론트와 백엔드 원인 후보 분리
- [ ] 수정 설계 문서 작성

## 현재 상태

포트폴리오 화면의 추가 매수 모달에서 Alpine 런타임 에러가 발생한다.

- 콘솔 메시지:
  - `Uncaught TypeError: Cannot read properties of null (reading 'stockDetail')`
  - `[Alpine] '(' + portfolio.purchaseItem.stockDetail.priceCurrency + ')'`
- 사용자 체감:
  - 어떤 경우에는 정상 표시
  - 어떤 경우에는 `stockDetail` 관련 에러 발생

현재 프론트는 주식 항목을 `assetType === 'STOCK'` 기준으로 추가 매수 버튼에 노출한다.

- `src/main/resources/static/index.html:1187`
- `src/main/resources/static/index.html:1286`

추가 매수 모달은 `portfolio.showPurchaseModal` 만으로 열고 닫으며, `purchaseItem` 초기값은 `null` 이다.

- `src/main/resources/static/js/components/portfolio.js:27`
- `src/main/resources/static/index.html:2052`

## 문제점

### 1. 현재 콘솔 에러는 `stockDetail null` 보다 `purchaseItem null` 에 더 직접적으로 연결됨

추가 매수 모달 내부에 다음과 같은 표현식이 있다.

- `src/main/resources/static/index.html:2089`
- `src/main/resources/static/index.html:2091`
- `src/main/resources/static/index.html:2094`
- `src/main/resources/static/index.html:2095`

특히 아래 표현식은 `purchaseItem` 이 없는 시점에도 평가될 수 있다.

```html
x-text="'(' + portfolio.purchaseItem.stockDetail.priceCurrency + ')'"
```

에러 메시지가 `reading 'stockDetail' of null` 인 점을 보면, 이 시점의 실제 null 값은 `portfolio.purchaseItem` 이다.

### 2. 모달이 `x-show` 기반이라 숨겨져 있어도 내부 표현식이 평가될 수 있음

추가 매수 모달은 `x-if` 가 아니라 `x-show` 로 렌더링된다.

- `src/main/resources/static/index.html:2052`

즉 DOM 에서 제거되지 않고 유지되므로, 초기 렌더 시점이나 상태 전환 시점에 `purchaseItem === null` 인 상태로 내부 Alpine 표현식이 먼저 평가될 수 있다.

### 3. `assetType === 'STOCK'` 이지만 `stockDetail` 이 없을 가능성도 별도로 남아 있음

백엔드에서 `stockDetail` 은 원칙적으로 주식 항목에 대해 생성되어야 한다.

- 신규 주식 등록: `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java:57-100`
- 주식 수정: `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java:323-352`
- 추가 매수 재계산: `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java:225-297`
- 조회 매핑: `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java:23-42`
- 응답 DTO 변환: `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/PortfolioItemResponse.java:70-89`

따라서 `GET /api/portfolio/items` 응답에서 `assetType=STOCK` 이면서 `stockDetail=null` 이 실제로 존재한다면, 이는 프론트가 아니라 조회 데이터 자체의 불일치 문제다.

## 원인 분석

## 확정 원인

### 추가 매수 모달의 null-safe 처리 부족

`purchaseItem` 초기값은 `null` 이지만, 모달 내부 일부 표현식은 `purchaseItem` 존재를 보장하지 않은 채 직접 접근한다.

- 상태 초기값: `src/main/resources/static/js/components/portfolio.js:26-30`
- 모달 오픈 시점: `src/main/resources/static/js/components/portfolio.js:922-928`

즉 정상일 때는:

1. 사용자가 추가 매수 버튼 클릭
2. `openPurchaseModal(item)` 호출
3. `purchaseItem` 세팅 완료
4. 모달 내부 표현식 평가

에러가 날 때는:

1. 페이지 초기 렌더 또는 상태 전환
2. `purchaseItem === null`
3. `x-show` 로 유지된 모달 내부 표현식이 먼저 평가
4. `portfolio.purchaseItem.stockDetail...` 접근 시 예외 발생

이 경로는 현재 코드만으로 재현 가능하며, 백엔드 응답과 무관하게 발생할 수 있다.

## 추정 원인

### 일부 조회 데이터가 JOINED 상속 구조와 맞지 않을 가능성

포트폴리오 영속성은 JPA `JOINED` 상속 구조를 사용한다.

- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java:22-23`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockItemEntity.java:9-13`

조회 시 `PortfolioItemMapper.toDomain()` 은 `entity instanceof StockItemEntity` 인 경우에만 `stockDetail` 을 채운다.

- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java:30-42`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java:187-197`

따라서 아래와 같은 데이터가 있으면 `stockDetail` 누락 가능성이 생긴다.

- `portfolio_item.asset_type='STOCK'` 이지만 `stock_detail` 서브 테이블 row 가 없음
- 과거 데이터나 수동 데이터 수정으로 상속 타입과 서브 테이블 데이터가 어긋남
- 조회 시점에 JPA 가 `StockItemEntity` 가 아닌 다른 타입/불완전 타입으로 로딩됨

이 부분은 현재 코드만으로는 확정할 수 없고, 실제 DB 또는 `/api/portfolio/items` 응답 검증이 필요하다.

## 추가 관찰 사항

### 해외 주식 목록 분류 조건이 `stockDetail` 없는 주식도 포함할 수 있음

`getOverseasStocks()` 는 아래 조건을 사용한다.

- `src/main/resources/static/js/components/portfolio.js:303-305`

```javascript
return this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail?.country !== 'KR');
```

`item.stockDetail` 이 `null` 이면 `item.stockDetail?.country` 는 `undefined` 이고, `undefined !== 'KR'` 는 `true` 이다.

즉 `stockDetail` 이 없는 주식 항목도 해외 주식 리스트로 분류될 수 있다. 이것은 근본 원인은 아니지만, 잘못된 데이터를 화면에서 더 쉽게 노출시키는 증폭 요인이다.

## 해결 방안

### 방안 1. 프론트 null-safe 처리 보강 (우선)

추가 매수 모달 전체를 `purchaseItem` 존재 조건으로 보호하거나, 내부 표현식을 모두 optional chaining 기반으로 바꾼다.

예시 대상:

- `src/main/resources/static/index.html:2089-2095`
- `src/main/resources/static/index.html:2052-2118`

장점:

- 현재 콘솔 에러를 즉시 차단 가능
- 백엔드 데이터가 정상이어도 발생하는 프론트 오류를 제거

단점:

- 실제로 `assetType=STOCK` + `stockDetail=null` 데이터가 있다면 근본 원인은 남음

### 방안 2. `/api/portfolio/items` 응답 검증 및 데이터 정합성 점검

아래 케이스를 확인한다.

- `assetType === 'STOCK'` 인데 `stockDetail === null`
- 해당 항목 id 의 `portfolio_item` / `stock_detail` row 존재 여부
- 상속 discriminator 와 서브 테이블 row 일치 여부

장점:

- 간헐적으로 `stockDetail` 이 실제 null 인 데이터 문제를 분리 가능

단점:

- DB 또는 실제 응답 확인이 필요

### 방안 3. 프론트 목록 필터 조건 보정

`getOverseasStocks()` 와 추가 매수 버튼 노출 조건에 `item.stockDetail` 존재 조건을 명시한다.

대상:

- `src/main/resources/static/js/components/portfolio.js:303-305`
- `src/main/resources/static/index.html:1187-1188`
- `src/main/resources/static/index.html:1286-1287`

장점:

- 비정상 데이터가 있어도 UI 진입점을 줄일 수 있음

단점:

- 데이터 정합성 문제 자체는 해결하지 못함

## 영향 범위

- 프론트 템플릿
  - `src/main/resources/static/index.html`
- 프론트 포트폴리오 상태/헬퍼
  - `src/main/resources/static/js/components/portfolio.js`
- 백엔드 조회/응답 경로
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/PortfolioItemResponse.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockItemEntity.java`

## 권장 사항

1. 먼저 프론트의 추가 매수 모달을 `purchaseItem` 기준으로 안전하게 감싼다.
2. 그다음 `/api/portfolio/items` 응답에서 `assetType=STOCK` + `stockDetail=null` 실제 존재 여부를 확인한다.
3. 실제 null 데이터가 확인되면, 별도 설계 문서에서 JPA 상속 데이터 정합성 점검 및 방어 로직을 설계한다.
