# 포트폴리오 설계

## 작업 리스트

- [x] AssetType enum 생성 (STOCK, BOND, REAL_ESTATE, FUND, CRYPTO, GOLD, COMMODITY, CASH, OTHER)
- [x] StockSubType, BondSubType, RealEstateSubType, FundSubType enum 생성
- [x] StockDetail, BondDetail, RealEstateDetail, FundDetail 값 객체 생성
- [x] PortfolioItem 도메인 모델 생성 (컴포지션 방식, 공통 필드 + nullable Detail 객체)
- [x] PortfolioItemRepository 포트 인터페이스 정의
- [x] NewsPurpose에 PORTFOLIO 추가
- [x] PortfolioItemEntity JPA 엔티티 생성 (Joined Table 상속 전략)
- [x] StockItemEntity, BondItemEntity 등 자식 엔티티 생성
- [x] PortfolioItemMapper 구현 (타입별 변환)
- [x] PortfolioItemRepositoryImpl 어댑터 구현
- [x] PortfolioService 구현 (항목 CRUD)
- [x] PortfolioAllocationService 구현 (비중 계산)
- [x] PortfolioNewsBatchService 구현 (뉴스 자동 수집)
- [x] PortfolioNewsBatchScheduler 스케줄러 등록
- [x] PortfolioController 구현
- [x] 요청/응답 DTO 생성
- [x] PortfolioController 등록/수정 엔드포인트 자산 유형별 분리
- [x] 타입별 전용 AddRequest DTO 생성 (StockItemAddRequest, BondItemAddRequest, RealEstateItemAddRequest, FundItemAddRequest, GeneralItemAddRequest)
- [x] 타입별 전용 UpdateRequest DTO 생성 (StockItemUpdateRequest, BondItemUpdateRequest, RealEstateItemUpdateRequest, FundItemUpdateRequest, GeneralItemUpdateRequest)
- [x] PortfolioService 타입별 수정 메서드 추가
- [x] 기존 통합 PortfolioItemAddRequest, PortfolioItemUpdateRequest 제거
- [x] StockDetail 필드 변경 (ticker→stockCode, exchange→market, country 추가)
- [x] StockItemEntity 컬럼 변경 (ticker→stock_code, exchange→market, country 추가)
- [x] StockItemAddRequest, StockItemUpdateRequest DTO 필드 반영
- [x] PortfolioItemMapper StockDetail 변환 로직 반영
- [ ] StockDetail 평균단가 자동 계산 방식 변경 (avgBuyPrice 직접 입력 → purchasePrice + quantity로 자동 계산)
- [ ] StockItemAddRequest 변경 (avgBuyPrice/investedAmount 제거, purchasePrice 추가)
- [ ] StockItemUpdateRequest 변경 (avgBuyPrice/investedAmount 제거, purchasePrice 추가)
- [ ] PortfolioItem.createWithStock() 팩토리 메서드 변경 (investedAmount 자동 계산)
- [ ] PortfolioItem.addStockPurchase() 추가 매수 행위 메서드 추가
- [ ] StockItemEntity 컬럼 유지 (avg_buy_price는 계산된 값 저장)
- [ ] PortfolioService.addStockPurchase() 추가 매수 메서드 추가
- [ ] StockPurchaseRequest DTO 생성 (추가 매수 전용)
- [ ] PortfolioController 추가 매수 엔드포인트 추가 (POST /api/portfolio/items/stock/{itemId}/purchase)

## 배경

사용자가 주식, 채권, 부동산 등 다양한 자산에 투자한 내역을 등록하고, 자산 유형별 투자 비중을 퍼센테이지로 확인할 수 있는 포트폴리오 기능이 필요하다. 각 자산 유형은 고유한 상세 정보(주식: 종목코드/배당률, 채권: 만기일/표면금리 등)를 가지며, 항목별로 뉴스 자동 수집을 선택적으로 활성화하여 기존 키워드 뉴스 수집 인프라를 재활용한다.

## 핵심 결정

- **도메인 모델은 컴포지션 방식**: PortfolioItem 하나의 클래스에 AssetType + nullable Detail 값 객체(StockDetail, BondDetail 등)로 구성. 상속 없이 단순한 구조
- **JPA 엔티티는 Joined Table 상속 전략**: DB 확장성 확보를 위해 엔티티 계층에서만 상속 사용. 도메인 ↔ 엔티티 변환은 Mapper가 처리
- **비중 계산은 부모 테이블만 조회**: `portfolio_item` 테이블의 `invested_amount`로 AssetType별 합산 (JOIN 없음)
- **뉴스 수집은 기존 인프라 재활용**: NewsPurpose에 `PORTFOLIO` 추가, sourceId에 portfolioItem.id 저장
- **항목 삭제 시 뉴스 Cascade Delete**: 키워드 삭제와 동일 패턴
- **프론트엔드 친화적 DTO 구조**: 요청/응답 모두 중첩 구조 (공통 필드 + 타입별 detail 객체)
- **주식 평균단가 자동 계산**: 사용자는 매수가(purchasePrice)와 수량(quantity)만 입력. avgBuyPrice와 investedAmount는 시스템이 자동 계산. 추가 매수 시 가중평균으로 누적 계산. 매수 이력은 별도 저장하지 않음 (현재 누적 상태만 유지)

## 구현

### 도메인 모델

#### AssetType (enum)
위치: `portfolio/domain/model/AssetType.java`

| 값 | 설명 |
|---|---|
| STOCK | 주식 |
| BOND | 채권 |
| REAL_ESTATE | 부동산 |
| FUND | 펀드 |
| CRYPTO | 암호화폐 |
| GOLD | 금 |
| COMMODITY | 원자재 |
| CASH | 현금성 자산 |
| OTHER | 기타 |

#### 서브타입 enum

| enum | 위치 | 값 |
|---|---|---|
| StockSubType | `portfolio/domain/model/StockSubType.java` | ETF, INDIVIDUAL |
| BondSubType | `portfolio/domain/model/BondSubType.java` | GOVERNMENT, CORPORATE |
| RealEstateSubType | `portfolio/domain/model/RealEstateSubType.java` | APARTMENT, OFFICETEL, LAND, COMMERCIAL |
| FundSubType | `portfolio/domain/model/FundSubType.java` | EQUITY_FUND, BOND_FUND, MIXED_FUND |

#### PortfolioItem (도메인 모델, 컴포지션 방식)
위치: `portfolio/domain/model/PortfolioItem.java`

**공통 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| userId | Long | 사용자 ID |
| itemName | String | 항목명 |
| assetType | AssetType | 자산 유형 |
| investedAmount | BigDecimal | 투자 금액 |
| newsEnabled | boolean | 뉴스 자동 수집 활성화 여부 |
| region | Region | 뉴스 검색 리전 (기존 Region enum 재사용) |
| memo | String | 메모 (선택) |
| createdAt | LocalDateTime | 생성일 |
| updatedAt | LocalDateTime | 수정일 |

**타입별 Detail 필드 (nullable, 해당 AssetType일 때만 값 존재)**

| 필드 | 타입 | 해당 AssetType |
|---|---|---|
| stockDetail | StockDetail | STOCK |
| bondDetail | BondDetail | BOND |
| realEstateDetail | RealEstateDetail | REAL_ESTATE |
| fundDetail | FundDetail | FUND |

CRYPTO, GOLD, COMMODITY, CASH, OTHER는 공통 필드만 사용 (Detail 없음)

[도메인 모델 예시 코드](./examples/domain-model-example.md)

**팩토리 메서드**
- `PortfolioItem.create(userId, itemName, assetType, investedAmount, region)` — Detail 없는 타입용
- `PortfolioItem.createWithStock(userId, itemName, region, stockDetail)` — 주식 (investedAmount는 stockDetail의 quantity × avgBuyPrice로 자동 계산)
- `PortfolioItem.createWithBond(userId, itemName, investedAmount, region, bondDetail)` — 채권
- `PortfolioItem.createWithRealEstate(userId, itemName, investedAmount, region, realEstateDetail)` — 부동산
- `PortfolioItem.createWithFund(userId, itemName, investedAmount, region, fundDetail)` — 펀드

**행위 메서드**
- `updateAmount(BigDecimal amount)`: 투자 금액 변경
- `enableNews()` / `disableNews()`: 뉴스 수집 토글
- `updateMemo(String memo)`: 메모 수정
- `addStockPurchase(int additionalQuantity, BigDecimal purchasePrice)`: 추가 매수 (가중평균 계산 후 stockDetail, investedAmount 갱신)

**검증 규칙**
- userId, itemName, assetType, region은 필수
- investedAmount > 0 (주식은 자동 계산, 나머지는 직접 입력)
- AssetType과 Detail 객체 정합성 검증 (STOCK인데 bondDetail이 있으면 예외)
- 추가 매수 시 assetType이 STOCK인지, stockDetail이 존재하는지 검증

#### StockDetail (값 객체)
위치: `portfolio/domain/model/StockDetail.java`

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | StockSubType | ETF / 개별주식 |
| stockCode | String | 종목코드 (005930 등) |
| market | String | 상장 시장 (KOSPI, KOSDAQ, NYSE, NASDAQ 등) |
| country | String | 국가 (KR, US, JP 등) |
| quantity | Integer | 보유 수량 (누적) |
| avgBuyPrice | BigDecimal | 평균 매수단가 (자동 계산, 직접 입력 안 함) |
| dividendYield | BigDecimal | 배당률 (%) |

**평균단가 자동 계산 방식 (누적)**
- 최초 등록: `avgBuyPrice = purchasePrice`, `quantity = 입력 수량`
- 추가 매수: `avgBuyPrice = (기존수량 × 기존평균단가 + 추가수량 × 추가매수가) / (기존수량 + 추가수량)`
- `investedAmount = avgBuyPrice × quantity` (부모 필드, 자동 계산)
- 매수 이력은 저장하지 않음 (현재 누적 상태만 유지)

#### BondDetail (값 객체)
위치: `portfolio/domain/model/BondDetail.java`

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | BondSubType | 국채 / 회사채 |
| maturityDate | LocalDate | 만기일 |
| couponRate | BigDecimal | 표면금리 (%) |
| creditRating | String | 신용등급 (AAA, AA+ 등) |

#### RealEstateDetail (값 객체)
위치: `portfolio/domain/model/RealEstateDetail.java`

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | RealEstateSubType | 아파트 / 오피스텔 / 토지 / 상가 |
| address | String | 주소 |
| area | BigDecimal | 면적 (m²) |

#### FundDetail (값 객체)
위치: `portfolio/domain/model/FundDetail.java`

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | FundSubType | 주식형 / 채권형 / 혼합형 |
| managementFee | BigDecimal | 운용보수율 (%) |

#### NewsPurpose 확장
위치: `news/domain/model/NewsPurpose.java`

기존 `KEYWORD`, `STOCK`에 `PORTFOLIO` 추가

### 포트 인터페이스

#### PortfolioItemRepository
위치: `portfolio/domain/repository/PortfolioItemRepository.java`

| 메서드 | 설명 |
|---|---|
| `save(PortfolioItem)` | 저장 (타입별 자식 포함) |
| `findById(Long)` | 단건 조회 (상세 필드 포함) |
| `findByUserId(Long)` | 사용자별 전체 조회 (상세 필드 포함) |
| `findByNewsEnabled(boolean)` | 뉴스 활성화 항목 전체 조회 |
| `existsByUserIdAndItemNameAndAssetType(Long, String, AssetType)` | 중복 확인 |
| `delete(PortfolioItem)` | 삭제 |

### 인프라스트럭처

#### 엔티티 (Joined Table 상속)

**PortfolioItemEntity (부모, abstract)**
위치: `portfolio/infrastructure/persistence/PortfolioItemEntity.java`

- `@Inheritance(strategy = InheritanceType.JOINED)`
- `@DiscriminatorColumn(name = "asset_type")`
- 테이블: `portfolio_item`

| 컬럼 | 타입 | 제약 |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| asset_type | VARCHAR(20) | NOT NULL (discriminator) |
| user_id | BIGINT | NOT NULL, INDEX |
| item_name | VARCHAR(100) | NOT NULL |
| invested_amount | DECIMAL(18,2) | NOT NULL |
| news_enabled | BOOLEAN | NOT NULL, DEFAULT false |
| region | VARCHAR(20) | NOT NULL |
| memo | VARCHAR(500) | NULLABLE |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

인덱스: `idx_portfolio_item_user_id (user_id)`
유니크 제약: `uk_portfolio_item (user_id, item_name, asset_type)`

**StockItemEntity** (테이블: `stock_detail`)
위치: `portfolio/infrastructure/persistence/StockItemEntity.java`

| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK, 부모와 공유) |
| sub_type | VARCHAR(20) |
| stock_code | VARCHAR(20) |
| market | VARCHAR(20) |
| country | VARCHAR(10) |
| quantity | INT |
| avg_buy_price | DECIMAL(18,2) |
| dividend_yield | DECIMAL(5,2) |

**BondItemEntity** (테이블: `bond_detail`)
위치: `portfolio/infrastructure/persistence/BondItemEntity.java`

| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK, 부모와 공유) |
| sub_type | VARCHAR(20) |
| maturity_date | DATE |
| coupon_rate | DECIMAL(5,2) |
| credit_rating | VARCHAR(10) |

**RealEstateItemEntity** (테이블: `real_estate_detail`)
위치: `portfolio/infrastructure/persistence/RealEstateItemEntity.java`

| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK, 부모와 공유) |
| sub_type | VARCHAR(20) |
| address | VARCHAR(200) |
| area | DECIMAL(10,2) |

**FundItemEntity** (테이블: `fund_detail`)
위치: `portfolio/infrastructure/persistence/FundItemEntity.java`

| 컬럼 | 타입 |
|---|---|
| id | BIGINT (PK, 부모와 공유) |
| sub_type | VARCHAR(20) |
| management_fee | DECIMAL(5,2) |

**CryptoItemEntity, GoldItemEntity, CommodityItemEntity, CashItemEntity**
각각 자식 테이블 생성, 현재 추가 컬럼 없음 (discriminator 구분 용도)

[Joined Table 엔티티 예시 코드](./examples/joined-table-entity-example.md)

#### PortfolioItemMapper
위치: `portfolio/infrastructure/persistence/PortfolioItemMapper.java`

**엔티티(상속) → 도메인 모델(컴포지션) 변환**:
- `StockItemEntity` → `PortfolioItem` + `StockDetail`
- `BondItemEntity` → `PortfolioItem` + `BondDetail`
- 고유 필드 없는 엔티티(Crypto, Gold 등) → `PortfolioItem` (Detail null)

**도메인 모델(컴포지션) → 엔티티(상속) 변환**:
- `PortfolioItem.getAssetType()`으로 분기하여 해당 자식 엔티티 생성
- `stockDetail != null` → `StockItemEntity`
- Detail 없음 → AssetType에 따라 `CryptoItemEntity`, `GoldItemEntity` 등

[Mapper 예시 코드](./examples/mapper-example.md)

#### PortfolioItemRepositoryImpl
위치: `portfolio/infrastructure/persistence/PortfolioItemRepositoryImpl.java`

JpaRepository 기반 어댑터. JPA가 상속 전략에 따라 자동으로 타입별 JOIN 처리.

### 애플리케이션 서비스

#### PortfolioService
위치: `portfolio/application/PortfolioService.java`

| 메서드 | 설명 |
|---|---|
| `addItem(userId, request)` | 항목 등록 (중복 검증, 타입별 상세 정보 포함) |
| `getItems(userId)` | 사용자 항목 목록 조회 (상세 정보 포함) |
| `updateItem(userId, itemId, request)` | 항목 수정 (금액 + 상세 정보) |
| `toggleNews(userId, itemId, enabled)` | 뉴스 수집 토글 |
| `deleteItem(userId, itemId)` | 항목 삭제 + 관련 뉴스 Cascade Delete |
| `addStockPurchase(userId, itemId, request)` | 주식 추가 매수 (가중평균 계산, investedAmount 자동 갱신) |

#### PortfolioAllocationService
위치: `portfolio/application/PortfolioAllocationService.java`

| 메서드 | 설명 |
|---|---|
| `getAllocation(userId)` | AssetType별 투자 비중(%) 계산 |

반환 구조: `List<AllocationDto>` (assetType, totalAmount, percentage)
비중 계산은 부모 테이블(`portfolio_item`)만 조회하여 처리 (JOIN 불필요)

#### PortfolioNewsBatchService
위치: `portfolio/application/PortfolioNewsBatchService.java`

키워드 뉴스 배치와 동일한 패턴:
1. `portfolioItemRepository.findByNewsEnabled(true)`로 활성 항목 조회
2. 각 항목의 `itemName`으로 `NewsSearchService` 검색
3. `NewsSaveService`로 저장 (purpose=`PORTFOLIO`, sourceId=`itemId`)

#### 스케줄러
위치: `portfolio/infrastructure/scheduler/PortfolioNewsBatchScheduler.java`

기존 `KeywordNewsBatchScheduler`와 동일한 패턴으로 `PortfolioNewsBatchService` 호출

[서비스 예시 코드](./examples/service-example.md)

### 프레젠테이션

#### PortfolioController
위치: `portfolio/presentation/PortfolioController.java`

**등록 (자산 유형별 진입점 분리)**

| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/portfolio/items/stock` | addStockItem | 주식 등록 |
| `POST /api/portfolio/items/bond` | addBondItem | 채권 등록 |
| `POST /api/portfolio/items/real-estate` | addRealEstateItem | 부동산 등록 |
| `POST /api/portfolio/items/fund` | addFundItem | 펀드 등록 |
| `POST /api/portfolio/items/general` | addGeneralItem | 일반 자산 등록 (CRYPTO, GOLD, COMMODITY, CASH, OTHER) |

**수정 (자산 유형별 진입점 분리)**

| API | 메서드 | 설명 |
|---|---|---|
| `PUT /api/portfolio/items/stock/{itemId}` | updateStockItem | 주식 수정 |
| `PUT /api/portfolio/items/bond/{itemId}` | updateBondItem | 채권 수정 |
| `PUT /api/portfolio/items/real-estate/{itemId}` | updateRealEstateItem | 부동산 수정 |
| `PUT /api/portfolio/items/fund/{itemId}` | updateFundItem | 펀드 수정 |
| `PUT /api/portfolio/items/general/{itemId}` | updateGeneralItem | 일반 자산 수정 |

**추가 매수**

| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/portfolio/items/stock/{itemId}/purchase` | addStockPurchase | 주식 추가 매수 (가중평균 자동 계산) |

**조회/삭제 (공통)**

| API | 메서드 | 설명 |
|---|---|---|
| `GET /api/portfolio/items` | getItems | 항목 목록 조회 (상세 포함) |
| `PATCH /api/portfolio/items/{itemId}/news` | toggleNews | 뉴스 토글 |
| `DELETE /api/portfolio/items/{itemId}` | deleteItem | 항목 삭제 |
| `GET /api/portfolio/allocation` | getAllocation | 자산 비중 조회 |

#### 요청/응답 DTO
위치: `portfolio/presentation/dto/`

**StockItemAddRequest** (주식 등록)
```json
{
  "itemName": "삼성전자",
  "region": "DOMESTIC",
  "memo": "장기 보유",
  "subType": "INDIVIDUAL",
  "stockCode": "005930",
  "market": "KOSPI",
  "country": "KR",
  "quantity": 100,
  "purchasePrice": 50000,
  "dividendYield": 2.5
}
```
- `investedAmount` 제거 (자동 계산: quantity × purchasePrice)
- `avgBuyPrice` → `purchasePrice` (최초 등록 시 avgBuyPrice = purchasePrice)

**BondItemAddRequest** (채권 등록)
```json
{
  "itemName": "국고채 3년",
  "investedAmount": 10000000,
  "region": "DOMESTIC",
  "subType": "GOVERNMENT",
  "maturityDate": "2029-03-01",
  "couponRate": 3.5,
  "creditRating": "AAA"
}
```

**RealEstateItemAddRequest** (부동산 등록)
```json
{
  "itemName": "강남 아파트",
  "investedAmount": 500000000,
  "region": "DOMESTIC",
  "subType": "APARTMENT",
  "address": "서울시 강남구",
  "area": 84.5
}
```

**FundItemAddRequest** (펀드 등록)
```json
{
  "itemName": "미래에셋 인덱스",
  "investedAmount": 3000000,
  "region": "DOMESTIC",
  "subType": "EQUITY_FUND",
  "managementFee": 0.35
}
```

**GeneralItemAddRequest** (일반 자산 등록 — CRYPTO, GOLD, COMMODITY, CASH, OTHER)
```json
{
  "assetType": "GOLD",
  "itemName": "금 현물",
  "investedAmount": 2000000,
  "region": "DOMESTIC",
  "memo": "안전자산"
}
```
- assetType 필수 (CRYPTO, GOLD, COMMODITY, CASH, OTHER 중 하나)

**StockPurchaseRequest** (추가 매수)
```json
{
  "quantity": 50,
  "purchasePrice": 55000
}
```
- 기존 항목에 추가 매수 시 사용
- 가중평균 계산: `newAvg = (기존수량 × 기존평균 + 추가수량 × 추가매수가) / (기존수량 + 추가수량)`
- `investedAmount`, `quantity`, `avgBuyPrice` 모두 자동 갱신

**PortfolioItemResponse** (조회 — 기존과 동일)
```json
{
  "id": 1,
  "assetType": "STOCK",
  "itemName": "삼성전자",
  "investedAmount": 5000000,
  "newsEnabled": false,
  "region": "DOMESTIC",
  "memo": "장기 보유",
  "createdAt": "2026-03-06T10:00:00",
  "updatedAt": "2026-03-06T10:00:00",
  "stockDetail": {
    "subType": "INDIVIDUAL",
    "stockCode": "005930",
    "market": "KOSPI",
    "country": "KR",
    "quantity": 100,
    "avgBuyPrice": 50000,
    "dividendYield": 2.5
  }
}
```
- 해당 타입의 detail 필드만 포함, 나머지 detail 필드는 응답에서 제외 (null 직렬화 안 함)

[Controller & DTO 예시 코드](./examples/controller-dto-example.md)

**StockItemUpdateRequest** (주식 수정) — AddRequest와 동일 구조 (purchasePrice 입력 시 avgBuyPrice 재계산, quantity 변경 시 investedAmount 재계산)
**BondItemUpdateRequest** (채권 수정) — AddRequest와 동일 구조
**RealEstateItemUpdateRequest** (부동산 수정) — AddRequest와 동일 구조
**FundItemUpdateRequest** (펀드 수정) — AddRequest와 동일 구조
**GeneralItemUpdateRequest** (일반 자산 수정) — investedAmount, memo 수정

**PortfolioNewsToggleRequest**: `{ "enabled": true }`

**AllocationResponse** (비중 조회)
```json
[
  { "assetType": "STOCK", "assetTypeName": "주식", "totalAmount": 5000000, "percentage": 33.33 },
  { "assetType": "BOND", "assetTypeName": "채권", "totalAmount": 10000000, "percentage": 66.67 }
]
```

## 주의사항

- 투자 금액은 `BigDecimal`로 처리 (부동소수점 오차 방지)
- 비중 계산 시 소수점 둘째 자리까지 반올림 (RoundingMode.HALF_UP)
- 뉴스 삭제 시 기존 `newsRepository.deleteByPurposeAndSourceId()` 재사용
- PortfolioItem의 userId 검증으로 다른 사용자의 항목 접근 차단
- **도메인 모델(컴포지션) ↔ 엔티티(상속) 변환**: Mapper에서 AssetType 기반 분기 + `instanceof` 체크로 양방향 변환
- **AssetType-Detail 정합성**: PortfolioItem 생성 시 AssetType과 Detail 객체의 타입이 일치하는지 검증 (STOCK인데 bondDetail이 있으면 예외)
- 고유 필드 없는 자식 엔티티(Crypto, Gold 등)도 discriminator 구분을 위해 자식 테이블 필요
- `NewsPurpose.PORTFOLIO` 추가는 news 도메인 변경이므로 최소한으로 제한 (enum 값 추가만)
- **DTO 직렬화**: 응답 시 해당 타입의 detail만 포함, null인 detail 필드는 JSON에서 제외 (`@JsonInclude(NON_NULL)`)
- **주식 평균단가 계산**: BigDecimal 가중평균 계산 시 `RoundingMode.HALF_UP`, 소수점 둘째 자리까지 반올림
- **주식 investedAmount 자동 계산**: 주식 항목의 investedAmount는 항상 `avgBuyPrice × quantity`로 계산. 직접 입력 불가