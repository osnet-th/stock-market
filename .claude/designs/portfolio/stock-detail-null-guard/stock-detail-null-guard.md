# stockDetail null guard 설계

## 작업 리스트

- [x] 추가 매수 모달의 렌더 조건을 `purchaseItem` 기준으로 보호
- [x] 추가 매수 모달 내부 `stockDetail` 참조를 null-safe 형태로 정리
- [x] 추가 매수 버튼 노출 조건을 `stockDetail` 존재 기준으로 제한
- [x] 해외 주식 목록 필터에서 `stockDetail` 없는 주식 제외
- [x] 포트폴리오 로드 시 `assetType=STOCK` + `stockDetail=null` 이상 데이터를 감지하는 경고 로직 추가

## 배경

포트폴리오 화면의 추가 매수 모달에서 Alpine 런타임 에러가 발생한다. 분석 결과 현재 에러의 직접 원인은 `purchaseItem === null` 상태에서 모달 내부 표현식이 평가되는 프론트 null-safe 처리 부족이며, 별도로 `assetType=STOCK` 인데 `stockDetail` 이 없는 데이터가 존재할 가능성도 남아 있다.

관련 분석 문서:

- [분석 문서](../../analyzes/portfolio/stock-detail-null-intermittent/stock-detail-null-intermittent.md)

## 핵심 결정

- **프론트 방어 우선**: 현재 콘솔 에러는 프론트만으로 재현 가능하므로 먼저 UI 렌더 안전성을 확보한다.
- **데이터 이상은 감지하되 자동 보정하지 않음**: `stockDetail` 누락 데이터가 있더라도 임의 복원 로직은 넣지 않고, 화면 진입 차단과 경고 로그만 추가한다.
- **버튼 진입점도 함께 차단**: 모달 내부만 막지 않고, `stockDetail` 없는 주식은 추가 매수/해외 주식 흐름에 들어가지 않도록 필터를 좁힌다.
- **백엔드 API 스펙은 유지**: 이번 설계에서는 응답 구조나 도메인 모델 변경 없이 프론트 보호와 이상 감지만 수행한다.

## 구현

### 추가 매수 모달 보호

위치:

- `src/main/resources/static/index.html`
- `src/main/resources/static/js/components/portfolio.js`

현재 추가 매수 모달은 `x-show="portfolio.showPurchaseModal"` 로만 제어되고 있어, `purchaseItem` 초기값이 `null` 인 상태에서도 내부 표현식이 평가될 수 있다.

이번 변경에서는 다음 원칙을 적용한다.

- 모달 컨텐츠는 `portfolio.showPurchaseModal && portfolio.purchaseItem` 조건이 충족될 때만 렌더
- `stockDetail` 기반 표시는 `portfolio.purchaseItem?.stockDetail` 조건으로 한 번 더 보호
- 모달 닫기 동작을 공통 메서드로 분리하여 `showPurchaseModal`, `purchaseItem`, `purchaseForm`, `editingHistory` 를 함께 초기화

[예시 코드](./examples/frontend-example.md)

### 추가 매수 버튼 노출 조건 조정

위치:

- `src/main/resources/static/index.html`

현재는 `item.assetType === 'STOCK'` 만으로 추가 매수 버튼이 노출된다. 이 조건을 아래처럼 강화한다.

- `item.assetType === 'STOCK' && item.stockDetail`

이 변경으로 비정상 주식 데이터가 있더라도 추가 매수 모달 진입 자체를 막는다.

### 모달 내부 표현식 정리

위치:

- `src/main/resources/static/index.html`

현재 `x-show` 와 `x-text` 가 서로 다른 안전성 수준으로 작성되어 있다. 예를 들어 `x-show` 는 optional chaining 을 쓰지만, `x-text` 는 직접 접근한다.

정리 원칙:

- 같은 표현식에서 `purchaseItem` 과 `stockDetail` 을 반복 직접 접근하지 않음
- 표시용 텍스트는 `portfolio.purchaseItem?.stockDetail?.priceCurrency` 기준으로 계산
- `step`, `placeholder` 도 동일 기준으로 맞춤
- 예상 결과 미리보기는 `purchaseItem` 과 `stockDetail` 이 모두 있는 경우에만 렌더

### 해외 주식 필터 보정

위치:

- `src/main/resources/static/js/components/portfolio.js`

현재 `getOverseasStocks()` 는 `item.stockDetail?.country !== 'KR'` 조건을 사용하여 `stockDetail` 이 없는 주식도 해외 주식으로 분류할 수 있다.

변경 후 기준:

- `item.assetType === 'STOCK' && item.stockDetail && item.stockDetail.country !== 'KR'`

국내 주식 필터와 동일한 수준으로 `stockDetail` 존재를 먼저 보장한다.

### 이상 데이터 감지 로그

위치:

- `src/main/resources/static/js/components/portfolio.js`

`loadPortfolio()` 이후 아래 조건의 항목을 검사한다.

- `item.assetType === 'STOCK' && !item.stockDetail`

이 경우:

- 화면은 크래시하지 않음
- `console.warn` 로 item id, itemName, assetType 을 남김

목적은 실제 API 응답에 이상 데이터가 섞여 있는지 빠르게 식별하는 것이다. 이번 단계에서는 사용자 alert 나 자동 복구는 넣지 않는다.

[예시 코드](./examples/frontend-example.md)

## 주의사항

- `x-if` 로 DOM 생명주기가 바뀌면 기존 모달 내부 입력 상태가 닫힐 때 초기화된다. 현재 추가 매수 모달은 닫힐 때 상태를 유지할 이유가 없으므로 허용한다.
- `assetType=STOCK` 이면서 `stockDetail=null` 인 응답이 실제 존재하더라도, 이번 설계는 프론트 방어가 목적이며 DB 정합성 수정은 별도 설계로 분리한다.
- 추가 매수 버튼을 숨기더라도 수정 화면 등 다른 주식 관련 진입점이 `stockDetail` 을 직접 참조하는지 함께 점검해야 한다.

## 영향 범위

- `src/main/resources/static/index.html`
- `src/main/resources/static/js/components/portfolio.js`

## 권장 사항

이번 작업은 프론트 null guard 와 진입 조건 정리까지만 수행한다. 이후에도 `console.warn` 이 계속 발생하면, 다음 단계에서 `portfolio_item` 와 `stock_detail` 의 JOINED 상속 데이터 정합성을 점검하는 별도 설계 문서를 추가한다.
