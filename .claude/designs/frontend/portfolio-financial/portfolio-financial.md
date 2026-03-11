# 포트폴리오 재무정보 조회 화면 설계

## 작업 리스트

### 백엔드
- [x] ReportCode enum 생성 (`stock/domain/model`)
- [x] IndexClassCode enum 생성 (`stock/domain/model`)
- [x] 옵션 조회 API 추가 (`GET /api/stocks/financial/options`) — 보고서코드, 지표분류코드 목록 반환
- [x] StockFinancialController의 reportCode/indexClassCode 파라미터를 enum 타입으로 변경

### 프론트엔드
- [x] api.js에 옵션 조회 + 재무정보 API 메서드 추가
- [x] app.js에 재무정보 상태 및 메서드 추가 (옵션은 API로 조회)
- [x] 주식 항목 카드에 [재무상세] 버튼 추가
- [ ] ~~재무 상세 패널 구현 (연도/보고서 선택 + 재무요약, 투자지표, 배당정보)~~ → 메뉴 선택 방식으로 변경
- [x] format.js에 compactNumber 유틸 추가
- [ ] ~~재무지표 로딩/에러 상태 처리~~ → 메뉴 선택 방식에 맞게 변경

### 프론트엔드 (v2 — 메뉴 선택 방식)
- [x] api.js에 누락된 API 메서드 추가 (전체재무제표, 소송, 사모/공모 자금사용)
- [x] app.js 상태/메서드를 메뉴 선택 방식으로 변경
- [x] 재무 상세 패널을 메뉴 목록 + 결과 영역으로 변경
- [x] 로딩/에러 상태 처리

## 배경

포트폴리오에 등록된 주식 항목에 대해 DART 재무정보(StockFinancialController)를 조회하여 투자 판단에 필요한 핵심 지표를 포트폴리오 화면에서 바로 확인할 수 있도록 한다. 보고서코드, 지표분류코드 등 DART 도메인 값은 백엔드 enum으로 관리하고 옵션 조회 API를 통해 프론트에 제공한다.

## 핵심 결정

- **보고서코드/지표분류코드를 백엔드 enum으로 관리** — 프론트에서 DART 코드값을 하드코딩하지 않음
- **옵션 조회 API 신규 추가** — `GET /api/stocks/financial/options`로 사용 가능한 보고서코드, 지표분류코드 목록 제공
- **기존 Controller 파라미터를 enum 타입으로 변경** — String → enum, 잘못된 코드 요청 시 400 에러
- **목록 화면은 변경 없음** — [재무상세] 버튼만 추가
- **상세 패널에서 연도/보고서/지표분류 선택 가능**
- **기본값**: 직전 연도 + 사업보고서(ANNUAL)

## 구현

### ReportCode enum

위치: `stock/domain/model/ReportCode.java`

| enum 값 | DART 코드 | 라벨 |
|---|---|---|
| `ANNUAL` | `11011` | 사업보고서 |
| `SEMI_ANNUAL` | `11012` | 반기보고서 |
| `Q1` | `11013` | 1분기보고서 |
| `Q3` | `11014` | 3분기보고서 |

- 필드: `code` (DART API에 전달하는 실제 코드), `label` (한글 표시명)
- 기존 ExchangeCode, MarketType과 동일한 패턴

### IndexClassCode enum

위치: `stock/domain/model/IndexClassCode.java`

| enum 값 | DART 코드 | 라벨 |
|---|---|---|
| `PROFITABILITY` | `M210000` | 수익성지표 |
| `STABILITY` | `M220000` | 안정성지표 |
| `GROWTH` | `M230000` | 성장성지표 |
| `ACTIVITY` | `M240000` | 활동성지표 |

- 필드: `code`, `label`

[enum 예시 코드](./examples/enum-example.md)

### 옵션 조회 API

위치: `StockFinancialController`

**`GET /api/stocks/financial/options`**

응답:
```json
{
  "reportCodes": [
    { "code": "ANNUAL", "dartCode": "11011", "label": "사업보고서" },
    { "code": "SEMI_ANNUAL", "dartCode": "11012", "label": "반기보고서" },
    { "code": "Q1", "dartCode": "11013", "label": "1분기보고서" },
    { "code": "Q3", "dartCode": "11014", "label": "3분기보고서" }
  ],
  "indexClassCodes": [
    { "code": "PROFITABILITY", "dartCode": "M210000", "label": "수익성지표" },
    { "code": "STABILITY", "dartCode": "M220000", "label": "안정성지표" },
    { "code": "GROWTH", "dartCode": "M230000", "label": "성장성지표" },
    { "code": "ACTIVITY", "dartCode": "M240000", "label": "활동성지표" }
  ]
}
```

- 프론트는 이 API를 한번 호출하여 드롭다운 옵션을 구성
- 프론트에서 재무정보 요청 시 enum name(예: `ANNUAL`, `PROFITABILITY`)을 파라미터로 전달

[옵션 API 예시 코드](./examples/options-api-example.md)

### 기존 Controller 파라미터 변경

기존 `String reportCode` → `ReportCode reportCode`, `String indexClassCode` → `IndexClassCode indexClassCode`로 변경.

**변경 전**: `@RequestParam String reportCode`
**변경 후**: `@RequestParam ReportCode reportCode`

- Spring이 자동으로 enum name 매핑 (예: `?reportCode=ANNUAL` → `ReportCode.ANNUAL`)
- 잘못된 값 요청 시 400 Bad Request 자동 응답
- Service, Port, Adapter 내부에서는 `reportCode.getCode()`로 DART 실제 코드를 사용

[Controller 변경 예시 코드](./examples/controller-change-example.md)

### 사용 API (단일회사, 상세 패널에서 메뉴 선택 시 개별 호출)

| 메뉴 key | 라벨 | 엔드포인트 | 추가 파라미터 |
|---|---|---|---|
| `accounts` | 재무계정 | `GET /{stockCode}/financial/accounts` | year, reportCode |
| `indices` | 재무지표 | `GET /{stockCode}/financial/indices` | year, reportCode, indexClassCode |
| `full-statements` | 전체재무제표 | `GET /{stockCode}/financial/full-statements` | year, reportCode, fsDiv |
| `stock-quantities` | 주식수량 | `GET /{stockCode}/financial/stock-quantities` | year, reportCode |
| `dividends` | 배당정보 | `GET /{stockCode}/financial/dividends` | year, reportCode |
| `lawsuits` | 소송현황 | `GET /{stockCode}/financial/lawsuits` | startDate, endDate |
| `private-fund` | 사모자금사용 | `GET /{stockCode}/financial/private-fund-usages` | year, reportCode |
| `public-fund` | 공모자금사용 | `GET /{stockCode}/financial/public-fund-usages` | year, reportCode |

- reportCode 파라미터: enum name 전달 (예: `ANNUAL`, `Q1`)
- indexClassCode 파라미터: enum name 전달 (예: `PROFITABILITY`, `STABILITY`)
- fsDiv: `OFS`(개별), `CFS`(연결)
- lawsuits는 reportCode 대신 startDate/endDate 사용

### 프론트엔드 API 확장

위치: `src/main/resources/static/js/api.js`

| 메서드 | 설명 | 상태 |
|---|---|---|
| `getFinancialOptions()` | 옵션 조회 (보고서코드, 지표분류코드) | 구현완료 |
| `getFinancialAccounts(stockCode, year, reportCode)` | 단일회사 재무계정 | 구현완료 |
| `getFinancialIndices(stockCode, year, reportCode, indexClassCode)` | 단일회사 재무지표 | 구현완료 |
| `getFinancialDividends(stockCode, year, reportCode)` | 배당정보 | 구현완료 |
| `getFinancialStockQuantities(stockCode, year, reportCode)` | 주식수량 | 구현완료 |
| `getFullFinancialStatements(stockCode, year, reportCode, fsDiv)` | 전체재무제표 | **추가 필요** |
| `getLawsuits(stockCode, startDate, endDate)` | 소송현황 | **추가 필요** |
| `getPrivateFundUsages(stockCode, year, reportCode)` | 사모자금사용 | **추가 필요** |
| `getPublicFundUsages(stockCode, year, reportCode)` | 공모자금사용 | **추가 필요** |

[프론트 API 예시 코드](./examples/api-example.md)

### 상태 관리 확장

위치: `src/main/resources/static/js/app.js`

**financial 상태** (portfolio 객체 내부에 추가)

| 필드 | 타입 | 설명 |
|---|---|---|
| `financialOptions` | `Object/null` | API에서 조회한 옵션 (reportCodes, indexClassCodes) |
| `financialResult` | `Array/null` | 선택한 메뉴의 API 응답 결과 |
| `financialLoading` | `boolean` | 재무정보 로딩 상태 |
| `selectedStockItem` | `Object/null` | 상세 보기 중인 주식 항목 |
| `financialYear` | `String` | 선택된 조회 연도 (기본: 직전 연도) |
| `financialReportCode` | `String` | 선택된 보고서 코드 (기본: `ANNUAL`) |
| `selectedFinancialMenu` | `String/null` | 현재 선택된 메뉴 key (예: `accounts`, `indices`) |
| `financialIndexClass` | `String` | 재무지표 메뉴 선택 시 지표분류코드 (기본: `PROFITABILITY`) |
| `financialFsDiv` | `String` | 전체재무제표 메뉴 선택 시 재무제표구분 (기본: `CFS`) |

**메뉴 목록** (프론트에서 정적 정의)

```javascript
financialMenus: [
    { key: 'accounts', label: '재무계정', params: ['year', 'reportCode'] },
    { key: 'indices', label: '재무지표', params: ['year', 'reportCode', 'indexClassCode'] },
    { key: 'full-statements', label: '전체재무제표', params: ['year', 'reportCode', 'fsDiv'] },
    { key: 'stock-quantities', label: '주식수량', params: ['year', 'reportCode'] },
    { key: 'dividends', label: '배당정보', params: ['year', 'reportCode'] },
    { key: 'lawsuits', label: '소송현황', params: ['startDate', 'endDate'] },
    { key: 'private-fund', label: '사모자금사용', params: ['year', 'reportCode'] },
    { key: 'public-fund', label: '공모자금사용', params: ['year', 'reportCode'] }
]
```

**추가/변경 메서드**

| 메서드 | 설명 |
|---|---|
| `loadFinancialOptions()` | 옵션 API 호출 → financialOptions 저장 (최초 1회) |
| `openStockDetail(item)` | 주식 항목 클릭 → 옵션 로드 + 메뉴 목록 표시 (API 호출 없음) |
| `closeStockDetail()` | 상세 패널 닫기 |
| `selectFinancialMenu(menuKey)` | 메뉴 선택 → 해당 API 1개만 호출 |
| `loadSelectedFinancial()` | 현재 선택된 메뉴 + 파라미터로 API 호출 |
| `onFinancialFilterChange()` | 연도/보고서 등 파라미터 변경 → loadSelectedFinancial() 재호출 |
| `getYearOptions()` | 최근 6개년 옵션 배열 반환 (현재 연도 포함) |

### 데이터 흐름

```
주식 항목에서 [재무상세] 클릭 → openStockDetail(item)
  ① loadFinancialOptions() — 최초 1회만 호출, 이후 캐시 사용
  ② 패널 표시: 보유 정보 + 메뉴 목록 버튼 (API 호출 없음)

메뉴 버튼 클릭 (예: "재무계정") → selectFinancialMenu('accounts')
  ③ selectedFinancialMenu = 'accounts'
  ④ loadSelectedFinancial()
     → getFinancialAccounts(stockCode, year, reportCode) 1개만 호출
     → financialResult에 결과 저장

파라미터 변경 (연도/보고서/지표분류 드롭다운) → onFinancialFilterChange()
  ⑤ loadSelectedFinancial() 재호출 (변경된 파라미터로)

다른 메뉴 선택 → selectFinancialMenu('indices')
  ⑥ 기존 결과 초기화 + 새 API 호출
```

[프론트 화면 예시 코드](./examples/portfolio-financial-view-example.md)

### 목록 화면 레이아웃 변경

기존 주식 항목 카드에 [재무상세] 버튼만 추가:

```
┌───────────────────────────────────────────────────────────┐
│ ▼ 주식 (2건)                                              │
│ ┌─────────────────────────────────────────────────────┐   │
│ │ 삼성전자 (005930) | 100주 | 평단가 70,000           │   │
│ │ [재무상세▸] [뉴스 ON/OFF] [수정] [삭제]             │   │
│ ├─────────────────────────────────────────────────────┤   │
│ │ 카카오 (035720) | 50주 | 평단가 45,000              │   │
│ │ [재무상세▸] [뉴스 ON/OFF] [수정] [삭제]             │   │
│ └─────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────┘
```

- [재무상세] 버튼: 국내 주식(`country === 'KR'`)에만 표시

### 상세 패널 레이아웃

```
┌─────────────────────────────────────────────────────────────┐
│ 삼성전자 (005930) 재무 상세                        [닫기 ✕] │
├─────────────────────────────────────────────────────────────┤
│ 수량: 100주 | 평단가: ₩70,000 | 투자금: ₩7,000,000         │
├─────────────────────────────────────────────────────────────┤
│ [재무계정] [재무지표] [전체재무제표] [주식수량]               │
│ [배당정보] [소송현황] [사모자금사용] [공모자금사용]           │
├─────────────────────────────────────────────────────────────┤
│ ← 메뉴 선택 시 아래에 파라미터 + 결과 표시                   │
├─────────────────────────────────────────────────────────────┤
│ 연도: [2025 ▾]  보고서: [사업보고서 ▾]  지표: [수익성 ▾]    │
│ (파라미터는 선택한 메뉴에 필요한 것만 표시)                   │
├─────────────────────────────────────────────────────────────┤
│                     [조회 결과 영역]                          │
│  재무계정 선택 시: 테이블 (계정명, 당기금액, 전기금액)        │
│  재무지표 선택 시: 테이블 (지표명, 지표값)                    │
│  배당정보 선택 시: 주당배당금, 배당률, 예상배당수익            │
│  소송현황 선택 시: 테이블 (소송 목록)                         │
│  ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

- 패널 열릴 때: 보유 정보 + 메뉴 버튼 목록만 표시, API 호출 없음
- 메뉴 버튼 클릭 시: 해당 API에 필요한 파라미터 드롭다운 표시 + API 1개 호출
- 선택된 메뉴 버튼은 하이라이트 처리
- 파라미터 변경 시 자동 재조회
- 다른 메뉴 선택 시 기존 결과 초기화 + 새 API 호출

### 메뉴별 파라미터

| 메뉴 | 필요 파라미터 |
|---|---|
| 재무계정, 주식수량, 배당정보, 사모자금, 공모자금 | 연도 + 보고서 |
| 재무지표 | 연도 + 보고서 + 지표분류코드 |
| 전체재무제표 | 연도 + 보고서 + 재무제표구분(개별/연결) |
| 소송현황 | 시작일 + 종료일 (연도/보고서 불필요) |

### 에러 처리

- API 실패 시: 결과 영역에 "조회에 실패했습니다" 메시지 표시
- 결과가 빈 배열일 때: "데이터 없음" 표시
- 주식이 아닌 자산(채권, 부동산 등): [재무상세] 버튼 미표시
- 해외 주식: [재무상세] 버튼 미표시 (DART API는 국내 전용)
- 잘못된 enum 값 요청: Spring이 400 Bad Request 자동 반환

## 주의사항

- 기존 Controller의 `String reportCode` → `ReportCode reportCode` 변경 시, Service/Port/Adapter에서 `reportCode.getCode()`로 DART 실제 코드 추출 필요
- financialOptions는 최초 1회만 API 호출, 이후 메모리 캐시
- 예상 배당수익 계산은 프론트에서 처리 (주당 배당금 × 보유 수량)
- 해외 주식은 DART API 대상이 아니므로 `country === 'KR'`인 항목만 [재무상세] 버튼 표시
- 메뉴 선택 전까지 API 호출하지 않음 (불필요한 DART API 호출 최소화)