# 포트폴리오 프론트엔드 설계

## 작업 리스트

- [x] api.js에 Portfolio API, Stock Search API 메서드 추가
- [x] app.js에 portfolio 상태 및 메서드 추가
- [ ] js/components/portfolio.js 컴포넌트 파일 생성
- [x] index.html 사이드바 메뉴에 '포트폴리오' 추가
- [x] index.html 대시보드 홈에 포트폴리오 요약 카드 추가
- [x] index.html 포트폴리오 메인 화면 (비중 차트 + 카테고리별 섹션) 구현
- [x] index.html 자산 추가 모달 구현 (타입 선택 → 타입별 폼)
- [x] index.html 주식 추가 시 종목 검색 + 선택 UI 구현
- [x] index.html 자산 수정 모달 구현
- [x] index.html 자산 삭제 + 뉴스 토글 기능 구현

## 배경

포트폴리오 백엔드 API가 완성되어 이를 프론트엔드에서 호출하여 사용자에게 보여주는 화면이 필요하다. 자산 유형별로 섹션을 나눠서 한 화면에서 전체 투자 현황과 비중을 확인할 수 있어야 하고, 주식 추가 시에는 StockController의 검색 API를 활용하여 종목을 검색/선택 후 정보를 자동 입력하는 UX가 필요하다.

## 핵심 결정

- **기존 패턴 준수**: Alpine.js + Tailwind CSS + Fetch API (빌드 도구 없음)
- **비중 시각화**: CSS 기반 수평 바 차트 (라이브러리 의존 없음)
- **카테고리별 섹션**: AssetType별로 접이식(collapse) 섹션으로 구분
- **주식 추가 플로우**: 종목 검색 → 드롭다운 선택 → 선택한 종목 정보 자동 채움
- **타입별 모달 폼**: 자산 타입 선택 시 해당 타입의 상세 필드만 동적 표시

## 구현

### API 확장

위치: `src/main/resources/static/js/api.js`

| 메서드 | 설명 |
|---|---|
| `searchStocks(name)` | `GET /api/stocks/search?name={name}` — 종목 검색 |
| `getPortfolioItems(userId)` | `GET /api/portfolio/items?userId={userId}` — 항목 목록 조회 |
| `getPortfolioAllocation(userId)` | `GET /api/portfolio/allocation?userId={userId}` — 비중 조회 |
| `addStockItem(userId, body)` | `POST /api/portfolio/items/stock?userId={userId}` |
| `addBondItem(userId, body)` | `POST /api/portfolio/items/bond?userId={userId}` |
| `addRealEstateItem(userId, body)` | `POST /api/portfolio/items/real-estate?userId={userId}` |
| `addFundItem(userId, body)` | `POST /api/portfolio/items/fund?userId={userId}` |
| `addGeneralItem(userId, body)` | `POST /api/portfolio/items/general?userId={userId}` |
| `updateStockItem(userId, itemId, body)` | `PUT /api/portfolio/items/stock/{itemId}?userId={userId}` |
| `updateBondItem(userId, itemId, body)` | `PUT /api/portfolio/items/bond/{itemId}?userId={userId}` |
| `updateRealEstateItem(userId, itemId, body)` | `PUT /api/portfolio/items/real-estate/{itemId}?userId={userId}` |
| `updateFundItem(userId, itemId, body)` | `PUT /api/portfolio/items/fund/{itemId}?userId={userId}` |
| `updateGeneralItem(userId, itemId, body)` | `PUT /api/portfolio/items/general/{itemId}?userId={userId}` |
| `deletePortfolioItem(userId, itemId)` | `DELETE /api/portfolio/items/{itemId}?userId={userId}` |
| `togglePortfolioNews(userId, itemId, enabled)` | `PATCH /api/portfolio/items/{itemId}/news?userId={userId}` |

[API 예시 코드](./examples/api-example.md)

### 상태 관리

위치: `src/main/resources/static/js/app.js`

**portfolio 상태 객체**

| 필드 | 타입 | 설명 |
|---|---|---|
| items | Array | 포트폴리오 항목 목록 |
| allocation | Array | 비중 데이터 (assetType, assetTypeName, totalAmount, percentage) |
| loading | boolean | 로딩 상태 |
| showAddModal | boolean | 추가 모달 표시 |
| showEditModal | boolean | 수정 모달 표시 |
| editingItem | Object/null | 수정 중인 항목 |
| selectedAssetType | String | 추가 모달에서 선택한 자산 타입 |
| addForm | Object | 추가 폼 데이터 |
| editForm | Object | 수정 폼 데이터 |
| stockSearch | Object | 주식 검색 상태 (query, results, loading, selected) |
| expandedSections | Object | 카테고리별 접이식 섹션 열림 상태 |

**메서드**

| 메서드 | 설명 |
|---|---|
| `loadPortfolio()` | 항목 목록 + 비중 동시 조회 |
| `openAddModal()` | 추가 모달 열기 (폼 초기화) |
| `selectAssetType(type)` | 자산 타입 선택 시 폼 전환 |
| `searchStock()` | 주식 종목 검색 (debounce 300ms) |
| `selectStock(stock)` | 검색 결과에서 종목 선택 → 폼 자동 채움 |
| `submitAddItem()` | 타입별 분기하여 API 호출 |
| `openEditModal(item)` | 수정 모달 열기 (기존 데이터 채움) |
| `submitEditItem()` | 타입별 분기하여 수정 API 호출 |
| `deleteItem(itemId)` | 삭제 확인 후 API 호출 |
| `toggleNews(item)` | 뉴스 수집 토글 |
| `toggleSection(assetType)` | 카테고리 섹션 접기/펼치기 |
| `getItemsByType(type)` | 특정 타입의 항목만 필터 |
| `getTotalInvested()` | 전체 투자 금액 합산 |

### 사이드바 메뉴 추가

위치: `src/main/resources/static/js/app.js` → menus 배열

```
{ key: 'portfolio', label: '포트폴리오', icon: 'portfolio' }
```

navigateTo에 `portfolio` case 추가 → `loadPortfolio()` 호출

### 대시보드 홈 요약 카드

위치: `src/main/resources/static/index.html` → home 섹션

기존 3개 카드 그리드를 4개로 변경 (grid-cols-4)

| 카드 내용 | 설명 |
|---|---|
| 총 투자 금액 | `Format.number(homeSummary.portfolioTotalAmount)` 원 |
| 자산 항목 수 | `homeSummary.portfolioItemCount` 개 |
| 클릭 시 | `navigateTo('portfolio')` |

homeSummary에 `portfolioTotalAmount`, `portfolioItemCount` 필드 추가

### 포트폴리오 메인 화면

위치: `src/main/resources/static/index.html`

**레이아웃 구조**

```
┌─────────────────────────────────────────────────┐
│ 포트폴리오              [+ 자산 추가] 버튼       │
├─────────────────────────────────────────────────┤
│ 요약 카드 (3열)                                  │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│ │ 총 투자  │ │ 자산 수  │ │ 뉴스활성 │          │
│ │ ₩15,000  │ │ 8개      │ │ 3개      │          │
│ └──────────┘ └──────────┘ └──────────┘          │
├─────────────────────────────────────────────────┤
│ 자산 비중 (수평 바 차트)                         │
│ 주식  ███████████████████ 33.3% (₩5,000,000)    │
│ 채권  ████████████████████████████ 66.7%         │
├─────────────────────────────────────────────────┤
│ ▼ 주식 (2건)                                     │
│ ┌───────────────────────────────────────────┐    │
│ │ 삼성전자 | ₩5,000,000 | KRX:005930       │    │
│ │ [뉴스 ON/OFF] [수정] [삭제]               │    │
│ ├───────────────────────────────────────────┤    │
│ │ TIGER S&P500 | ₩3,000,000 | ETF          │    │
│ │ [뉴스 ON/OFF] [수정] [삭제]               │    │
│ └───────────────────────────────────────────┘    │
│ ▼ 채권 (1건)                                     │
│ ┌───────────────────────────────────────────┐    │
│ │ 국고채 3년 | ₩10,000,000 | 만기 2029-03  │    │
│ └───────────────────────────────────────────┘    │
│ ▶ 금 (0건)  ← 접혀 있음                         │
└─────────────────────────────────────────────────┘
```

[메인 화면 예시 코드](./examples/main-view-example.md)

### 자산 추가 모달

위치: `src/main/resources/static/index.html`

**플로우**

```
1. [+ 자산 추가] 클릭 → 모달 열림
2. 자산 타입 선택 버튼 (주식/채권/부동산/펀드/일반)
3. 선택한 타입에 맞는 폼 필드 표시
4. [등록] 클릭 → API 호출 → 목록 갱신
```

**타입별 폼 필드**

| 타입 | 공통 필드 | 타입 전용 필드 |
|---|---|---|
| 주식 | itemName(자동), investedAmount, region, memo | subType, ticker(자동), exchange(자동), quantity, avgBuyPrice, dividendYield |
| 채권 | itemName, investedAmount, region, memo | subType, maturityDate, couponRate, creditRating |
| 부동산 | itemName, investedAmount, region, memo | subType, address, area |
| 펀드 | itemName, investedAmount, region, memo | subType, managementFee |
| 일반 | assetType(선택), itemName, investedAmount, region, memo | 없음 |

**주식 종목 검색 UX**

```
┌──────────────────────────────────────────┐
│ 종목 검색                                 │
│ ┌──────────────────────────────────────┐  │
│ │ 삼성  [검색 중...]                    │  │ ← 입력 시 debounce 300ms 후 자동 검색
│ └──────────────────────────────────────┘  │
│ ┌──────────────────────────────────────┐  │
│ │ 삼성전자 (005930) - KOSPI            │  │ ← 클릭 시 선택
│ │ 삼성SDI (006400) - KOSPI             │  │
│ │ 삼성전기 (009150) - KOSPI            │  │
│ │ 삼성물산 (028260) - KOSPI            │  │
│ └──────────────────────────────────────┘  │
│                                           │
│ 선택된 종목: 삼성전자 (005930)             │ ← 선택 후 표시
│                                           │
│ 항목명: [삼성전자     ] ← 자동 입력       │
│ 종목코드: [005930     ] ← 자동 입력       │
│ 거래소: [KOSPI       ] ← 자동 입력        │
│ ...                                       │
└──────────────────────────────────────────┘
```

- `GET /api/stocks/search?name={query}` 호출
- 응답: `[{ stockCode, stockName, marketType, corpName }]`
- 선택 시: itemName ← stockName, ticker ← stockCode, exchange ← marketType

[추가 모달 예시 코드](./examples/add-modal-example.md)

### 수정 모달

위치: `src/main/resources/static/index.html`

- 추가 모달과 유사한 구조, 자산 타입 선택 불가 (기존 타입 유지)
- 기존 데이터로 폼 필드 채움
- 주식 수정 시에도 종목 재검색 가능
- region은 수정 불가 (API에서 region 미지원)

[수정 모달 예시 코드](./examples/edit-modal-example.md)

### 자산 타입 표시 매핑

| AssetType | 한글명 | 색상 (badge) |
|---|---|---|
| STOCK | 주식 | blue |
| BOND | 채권 | green |
| REAL_ESTATE | 부동산 | yellow |
| FUND | 펀드 | purple |
| CRYPTO | 암호화폐 | orange |
| GOLD | 금 | amber |
| COMMODITY | 원자재 | red |
| CASH | 현금 | gray |
| OTHER | 기타 | slate |

### 비중 바 차트

CSS 기반 수평 바 차트:
- 각 자산 타입별 한 줄
- 바 너비 = percentage% (Tailwind inline style)
- 바 색상 = 타입별 지정 색상
- 우측에 percentage% + 금액 표시

### 카테고리별 접이식 섹션

- AssetType 순서로 섹션 표시 (항목이 있는 타입만)
- 섹션 헤더: 타입 뱃지 + 항목 수 + 접기/펼치기 토글
- 기본: 항목이 있는 섹션은 펼침, 없는 섹션은 접힘
- 각 항목 카드: 항목명, 투자 금액, 타입별 핵심 정보, 액션 버튼

### 항목 카드 표시 정보

| 타입 | 핵심 표시 정보 |
|---|---|
| STOCK | ticker, exchange, subType(ETF/개별), quantity주 |
| BOND | subType(국채/회사채), 만기일, 금리 |
| REAL_ESTATE | subType, 주소 |
| FUND | subType, 운용보수 |
| 일반 | memo |

### 파일 구조

```
src/main/resources/static/
├── js/
│   ├── api.js                  ← Portfolio/Stock API 메서드 추가
│   ├── app.js                  ← portfolio 상태 + 메뉴 + navigateTo 추가
│   └── components/
│       └── portfolio.js        ← (선택) 복잡한 로직 분리 시
└── index.html                  ← 포트폴리오 화면 + 모달 추가
```

## 주의사항

- 주식 검색 시 debounce 300ms 적용하여 과도한 API 호출 방지
- 투자 금액 표시 시 `Format.number()` 사용하여 천단위 구분
- 모달 외부 클릭 시 닫힘 (`@click.outside`) 기존 패턴 유지
- 자산 삭제 시 `confirm()` 확인 다이얼로그 필수
- StockController 응답의 `marketType`(KOSPI/KOSDAQ)을 exchange로 매핑
- 일반 자산(CRYPTO, GOLD, COMMODITY, CASH, OTHER)은 GeneralItemAddRequest 사용, assetType 필드 필수
- 빈 포트폴리오 상태에서도 안내 메시지 + 추가 버튼 표시