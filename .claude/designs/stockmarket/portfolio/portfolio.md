# 포트폴리오 설계

## 작업 리스트

- [ ] AssetType enum 생성 (STOCK, BOND, REAL_ESTATE, FUND, CRYPTO, GOLD, COMMODITY, CASH, OTHER)
- [ ] StockSubType, BondSubType, RealEstateSubType, FundSubType enum 생성
- [ ] PortfolioItem 도메인 모델 생성 (공통 베이스)
- [ ] StockItem, BondItem, RealEstateItem, FundItem, CryptoItem, GoldItem, CommodityItem, CashItem 도메인 모델 생성
- [ ] PortfolioItemRepository 포트 인터페이스 정의
- [ ] NewsPurpose에 PORTFOLIO 추가
- [ ] PortfolioItemEntity JPA 엔티티 생성 (Joined Table 상속 전략)
- [ ] StockItemEntity, BondItemEntity 등 자식 엔티티 생성
- [ ] PortfolioItemMapper 구현 (타입별 변환)
- [ ] PortfolioItemRepositoryImpl 어댑터 구현
- [ ] PortfolioService 구현 (항목 CRUD)
- [ ] PortfolioAllocationService 구현 (비중 계산)
- [ ] PortfolioNewsBatchService 구현 (뉴스 자동 수집)
- [ ] PortfolioNewsBatchScheduler 스케줄러 등록
- [ ] PortfolioController 구현
- [ ] 요청/응답 DTO 생성

## 배경

사용자가 주식, 채권, 부동산 등 다양한 자산에 투자한 내역을 등록하고, 자산 유형별 투자 비중을 퍼센테이지로 확인할 수 있는 포트폴리오 기능이 필요하다. 각 자산 유형은 고유한 상세 정보(주식: 종목코드/배당률, 채권: 만기일/표면금리 등)를 가지며, 항목별로 뉴스 자동 수집을 선택적으로 활성화하여 기존 키워드 뉴스 수집 인프라를 재활용한다.

## 핵심 결정

- **Joined Table 상속 전략**: 공통 필드는 `portfolio_item` 부모 테이블, 유형별 고유 필드는 자식 테이블(`stock_detail`, `bond_detail` 등)로 분리. 새 자산 유형 추가 시 자식 테이블만 추가하면 되어 확장에 유리
- **비중 계산은 부모 테이블만 조회**: `portfolio_item` 테이블의 `invested_amount`로 AssetType별 합산 (JOIN 없음)
- **뉴스 수집은 기존 인프라 재활용**: NewsPurpose에 `PORTFOLIO` 추가, sourceId에 portfolioItem.id 저장
- **항목 삭제 시 뉴스 Cascade Delete**: 키워드 삭제와 동일 패턴

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

#### PortfolioItem (공통 베이스 도메인 모델)
위치: `portfolio/domain/model/PortfolioItem.java`

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

공통 행위:
- `updateAmount(BigDecimal amount)`: 투자 금액 변경
- `enableNews()` / `disableNews()`: 뉴스 수집 토글
- `updateMemo(String memo)`: 메모 수정

#### StockItem (주식)
위치: `portfolio/domain/model/StockItem.java` (extends PortfolioItem)

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | StockSubType | ETF / 개별주식 |
| ticker | String | 종목코드 (005930 등) |
| exchange | String | 거래소 (KRX, NYSE 등) |
| quantity | Integer | 보유 수량 |
| avgBuyPrice | BigDecimal | 평균 매수단가 |
| dividendYield | BigDecimal | 배당률 (%) |

팩토리: `StockItem.create(userId, itemName, investedAmount, region, subType, ticker, exchange, quantity, avgBuyPrice)`

#### BondItem (채권)
위치: `portfolio/domain/model/BondItem.java` (extends PortfolioItem)

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | BondSubType | 국채 / 회사채 |
| maturityDate | LocalDate | 만기일 |
| couponRate | BigDecimal | 표면금리 (%) |
| creditRating | String | 신용등급 (AAA, AA+ 등) |

팩토리: `BondItem.create(userId, itemName, investedAmount, region, subType, maturityDate, couponRate, creditRating)`

#### RealEstateItem (부동산)
위치: `portfolio/domain/model/RealEstateItem.java` (extends PortfolioItem)

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | RealEstateSubType | 아파트 / 오피스텔 / 토지 / 상가 |
| address | String | 주소 |
| area | BigDecimal | 면적 (m2) |

팩토리: `RealEstateItem.create(userId, itemName, investedAmount, region, subType, address, area)`

#### FundItem (펀드)
위치: `portfolio/domain/model/FundItem.java` (extends PortfolioItem)

| 필드 | 타입 | 설명 |
|---|---|---|
| subType | FundSubType | 주식형 / 채권형 / 혼합형 |
| managementFee | BigDecimal | 운용보수율 (%) |

팩토리: `FundItem.create(userId, itemName, investedAmount, region, subType, managementFee)`

#### CryptoItem, GoldItem, CommodityItem, CashItem
위치: `portfolio/domain/model/` 각각

현재 공통 필드 외 추가 고유 필드 없음. 추후 필요 시 확장.

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
| ticker | VARCHAR(20) |
| exchange | VARCHAR(20) |
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

타입별 Entity <-> Domain Model 변환. `instanceof` 체크로 자식 타입별 매핑 처리.

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

### 프레젠테이션

#### PortfolioController
위치: `portfolio/presentation/PortfolioController.java`

| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/portfolio/items` | addItem | 항목 등록 (타입별 상세 포함) |
| `GET /api/portfolio/items` | getItems | 항목 목록 조회 (상세 포함) |
| `PUT /api/portfolio/items/{itemId}` | updateItem | 항목 수정 |
| `PATCH /api/portfolio/items/{itemId}/news` | toggleNews | 뉴스 토글 |
| `DELETE /api/portfolio/items/{itemId}` | deleteItem | 항목 삭제 |
| `GET /api/portfolio/allocation` | getAllocation | 자산 비중 조회 |

#### 요청/응답 DTO
위치: `portfolio/presentation/dto/`

- `PortfolioItemAddRequest`: assetType + 공통 필드 + 타입별 상세 필드 (Optional)
- `PortfolioItemResponse`: 공통 필드 + 타입별 상세 필드
- `PortfolioItemUpdateRequest`: 공통 필드 + 타입별 상세 필드
- `PortfolioNewsToggleRequest`: enabled
- `AllocationResponse`: assetType, assetTypeName, totalAmount, percentage

## 주의사항

- 투자 금액은 `BigDecimal`로 처리 (부동소수점 오차 방지)
- 비중 계산 시 소수점 둘째 자리까지 반올림 (RoundingMode.HALF_UP)
- 뉴스 삭제 시 기존 `newsRepository.deleteByPurposeAndSourceId()` 재사용
- PortfolioItem의 userId 검증으로 다른 사용자의 항목 접근 차단
- Mapper에서 `instanceof` 체크로 타입별 변환 처리
- 고유 필드 없는 자식 엔티티(Crypto, Gold 등)도 discriminator 구분을 위해 자식 테이블 필요
- `NewsPurpose.PORTFOLIO` 추가는 news 도메인 변경이므로 최소한으로 제한 (enum 값 추가만)