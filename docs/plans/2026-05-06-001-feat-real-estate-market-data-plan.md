---
title: "feat: Add Real Estate Market Data Dashboard"
type: feat
status: active
date: 2026-05-06
origin: docs/brainstorms/2026-05-06-real-estate-market-data-requirements.md
issue: 41
branch: feat/issue-41-real-estate-market-data
---

# feat: Add Real Estate Market Data Dashboard

## Overview

부동산 시장 데이터를 수치 중심으로 제공하는 신규 도메인(`realestate`)을 추가한다. 11개 탭(매매 거래/가격, 전월세, 공식 통계, 공급, 미분양, 청약, 분양 리스크, 지역 특화, 지역 비교, 데이터 출처) 모두 MVP에 포함하며, MVP 커버리지는 서울특별시 + 경기도 시군구로 한정한다. 외부 공공 API 8개 출처(국토교통부, 한국부동산원 R-ONE, 건축HUB, KOSIS, 청약홈, HUG, 서울 열린데이터광장, 경기도 데이터드림)를 매일 새벽 6시 KST 일배치로 수집하며, 모든 화면은 출처/기준일을 명시하고 판단 라벨을 노출하지 않는다.

## Problem Frame

실수요 주택 매수/임차인이 특정 지역(서울/경기)의 매매·전월세·공급·청약·분양 리스크 흐름을 한 화면에서 수치 기반으로 비교하고 싶지만, 현재 stock-market 서비스에는 부동산 보유 자산 등록(`portfolio.RealEstateItem`)만 있고 시장 흐름 조회는 없다. (see origin: docs/brainstorms/2026-05-06-real-estate-market-data-requirements.md)

## Requirements Trace

origin 문서의 R1–R17을 그대로 이행한다.

- R1–R3: 11개 탭 + 필터 + 요약 카드 → Unit 5(API), Unit 7(프론트)
- R4–R6: 서울/경기 우선, 비교 지역 → Unit 1(지역코드), Unit 5(비교 가공)
- R7–R9: 일배치 새벽 6시 + 기준일 표시 → Unit 4(스케줄러), Unit 8(메타 노출)
- R10–R12: 로그인 필수 + 관심지역 → Unit 6(realestate 도메인 자체 region 즐겨찾기 entity)
- R13–R17: 평균/중위, 해제 거래, 전세가율 추정, 판단 라벨 금지, 누락 시 출처 노출 → Unit 5(가공), Unit 8(UX 검증)

## Scope Boundaries

origin 문서의 Scope Boundaries 그대로 유지:

- 매수/매도/관망 추천, 등급 판단, 신호 라벨, 저평가/고평가 판단 미제공
- 챗봇 컨텍스트 주입 미포함 (별도 후속 과제)
- `portfolio.RealEstateItem` 자동 연계 미포함
- 시장 데이터 알림/구독, 매물·단지 검색, 실거래 알림 미포함
- 모바일 앱 미포함 (기존 웹 반응형 정책 따름)

## Context & Research

### Relevant Code and Patterns

- **economics 도메인 (1:1 미러링 모델)**
  - 패키지: `economics/{presentation,application,domain,infrastructure}` — 신규 `realestate/`도 동일 골격
  - 외부 API 어댑터: `economics/infrastructure/korea/ecos/{EcosApiClient, EcosIndicatorAdapter, config/EcosProperties, config/EcosCacheConfig, exception/EcosApiException}`
  - 다중 출처 Registry 패턴: `economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorRegistry.java` (URL/지표 매핑) — 30+ 부동산 API 라우팅 참고
  - 스케줄러+Warmup: `economics/infrastructure/scheduler/{EcosIndicatorBatchScheduler, EcosIndicatorWarmupListener, GlobalIndicatorBatchScheduler}` — `@Scheduled` cron, `@EventListener(ApplicationReadyEvent.class)`, 메소드는 트랜잭션 없음, 내부 SaveService에 `@Transactional` 위임
  - 3-table 패턴: `EcosIndicatorEntity`(history) + `EcosIndicatorLatestEntity`(upsert/변경 감지) + `EcosIndicatorMetadataEntity`(yml 시딩)
  - 메타데이터 시딩: `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataInitializer.java`(ApplicationRunner) + `src/main/resources/ecos-indicator-metadata.yml`
  - JpaRepository: native `@Query`로 `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY snapshot_date DESC)` window function 활용 (PostgreSQL)
  - Mapper: `infrastructure/persistence/mapper/*Mapper` POJO `@Component`, MapStruct 미사용
- **Region/Source 기반 Factory 패턴**: `news/infrastructure/NewsSearchPortFactoryImpl.java` (생성자 `List<Port>` 자동 주입 + `groupingBy(supportedRegion)`)
- **Async/Cache 인프라**: `stocknote/infrastructure/cache/StocknoteCacheConfig.java`(named CacheManager), `stocknote/infrastructure/async/StocknoteAsyncConfig.java`(전용 ThreadPoolExecutor + MdcTaskDecorator)
- **Favorite 도메인**: `favorite/{application/FavoriteIndicatorService, domain/model/FavoriteIndicatorSourceType, infrastructure/persistence/UserFavoriteIndicatorEntity}` — `sourceType` enum 확장으로 신규 source 추가
- **공통 RestClient**: `news/infrastructure/config/RestClientConfig.java` (단일 `RestClient` bean, 모든 도메인 공유)
- **API 키 주입 패턴**: `application.yml`에 `${ENV_VAR}` 치환, `@ConfigurationProperties(prefix=...)` POJO
- **포트폴리오 RealEstate 충돌 회피**: `portfolio/{infrastructure/persistence/RealEstateItemEntity, domain/model/RealEstateDetail, enums/RealEstateSubType}` 점유 중 → 신규 도메인 클래스 prefix는 `RealEstateMarket*` 또는 `Housing*`
- **프론트엔드**: `static/index.html`(partial 슬롯만), `static/js/utils/partial-loader.js`, `static/js/components/{ecos.js, portfolio.js}`, `static/partials/ecos.html`(카테고리 탭 + Chart.js) — 부동산 partial/component 동일 패턴

### Institutional Learnings

- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` — 30+ API 호출 시 핵심. 스케줄러는 `@Transactional` 제거, `*CaptureExecutor` 별도 빈으로 외부 호출 + per-item 트랜잭션 격리. `REQUIRES_NEW`/self-invocation 금지.
- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md` — 3-table 패턴 + `latestMap` 1회 조회 후 메모리 비교. `referenceText/cycle`은 원본 문자열 그대로 저장(파싱 없이 변경 감지). 부동산은 `compareKey = regionCode::category::source::indicatorCode::referenceText`로 일반화 — **source 포함 필수**(같은 region×category에 복수 출처가 들어오는 경우 출처별 row를 별개로 다뤄야 함).
- `docs/solutions/performance-issues/parallel-external-fetch-resilience-2026-04-23.md` — 벽시계 timeout(`.get(timeout)` + future Map), bounded `ThreadPoolExecutor` + `ArrayBlockingQueue` + `AbortPolicy`, daemon=false + `@PreDestroy` `awaitTermination(5s)`, Caffeine 빈 응답 차등 TTL(60s).
- `docs/plans/2026-04-15-002-feat-favorite-indicator-dashboard-plan.md` — `favorite`는 독립 최상위 도메인 유지, source enum 확장으로 부동산 합류, userId는 JWT SecurityContext 추출(@RequestParam 금지), cross-domain enrich는 application service 주입.
- `docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md` — `ddl-auto: update`는 NOT NULL silent skip. **신규 테이블이라 직접 영향은 없음**, 후속 컬럼 추가 시 주의.
- 한국 공공 API(data.go.kr/KOSIS/R-ONE) 인증키/포맷/EUC-KR/공공포털 에러 응답 학습은 저장소에 **없음** — 구현 후 `docs/solutions`에 신규 학습 적재 후보.

### External References

생략. 로컬 economics/global 패턴이 한국 공공 API 어댑터에 충분히 직접적인 reference.

## Key Technical Decisions

- **신규 도메인 분리**: portfolio의 보유 자산 도메인과 책임 명확 분리. 패키지명 `realestate`, 클래스 prefix `RealEstateMarket*`. (Approval Gate: 신규 패키지 + Entity)
- **출처별 Adapter Factory + 카테고리 Registry 혼합**: 8개 출처는 `RealEstateMarketSourceAdapter` 인터페이스 구현체로 분리(news 패턴), 출처 내부의 N개 지표는 Registry 매핑(global/tradingeconomics 패턴). 30+ API라도 출처가 8개로 묶이므로 라우팅이 단순해짐.
- **Per-item transaction 격리**: 스케줄러 → `RealEstateMarketBatchService`(`@Transactional` 없음, try-catch 흐름) → `RealEstateMarketCaptureExecutor`(별도 빈, 메소드별 `@Transactional`) → Adapter. 외부 빈 호출이라 Spring AOP 프록시 동작.
- **3-table 패턴 (그룹별)**: `real_estate_market_indicator`(history) + `real_estate_market_latest`(upsert/변경 감지) + `real_estate_market_metadata`(yml 시딩). 데이터 카테고리는 enum `RealEstateMarketCategory`(TRADE / PRICE_INDEX / RENT / SUPPLY / UNSOLD / SUBSCRIPTION / GUARANTEE_RISK / SEOUL_LOCAL / GYEONGGI_LOCAL)로 단일 테이블에 통합.
- **Caffeine named CacheManager**: `realEstateCacheManager` bean, `@Primary` 미사용(ecos 점유). TTL 25h(일배치 + 1시간 여유), 빈 응답은 `Expiry` 60s.
- **일배치 스케줄링**: `@Scheduled(cron="0 0 6 * * *")` KST. ECOS(0 0 7) / Global(0 30 7)과 시간대 분리.
- **관심지역(region 즐겨찾기)은 realestate 도메인 자체 entity로 분리**: `favorite` 도메인은 "지표 즐겨찾기" 의미가 명확하므로 enum 확장 대신 신규 `UserFavoriteRealEstateRegion` entity를 realestate 도메인에 둔다. 즐겨찾기 단위는 region(시도+시군구+선택적 읍면동)만 — 주택유형/면적 같은 필터 상태는 저장하지 않는다.
- **Warmup은 외부 API 호출 없음**: 부팅 시 외부 8개 출처에 region×category 폭증 호출이 발생하지 않도록, `WarmupListener`는 DB latest를 메모리 캐시로 hot-load만 수행. 외부 fetch는 일배치(06:00 KST)에서만.
- **백엔드 집계**: 평균/중위/변화율/전세가율 추정은 모두 백엔드 application 계층에서 계산 후 응답(이슈 #35 정신과 정합). 프론트는 차트 렌더링만.
- **지역코드**: 행정안전부 `시군구 코드(5자리)` + 읍면동 `법정동 코드(8자리)` 사용. 메타 시딩 yml에 서울 25개 자치구 + 경기도 31개 시군 + 주요 동 코드 포함.
- **데이터 누락 정책**: 어댑터 실패 시 history INSERT skip, latest 미갱신, 응답에 `lastUpdatedAt`만 노출하고 카드/차트는 빈 상태 + 출처/사유/다음 갱신 예상 시각.
- **PostgreSQL window function**: `ROW_NUMBER() OVER PARTITION BY` 활용해 최신 시계열 추출 (economics와 동일).

## Open Questions

### Resolved During Planning

- **단일 Adapter+Registry vs 출처별 Adapter+Factory**: 출처가 8개로 묶이므로 출처별 Adapter + 카테고리별 Registry 혼합 채택.
- **도메인 명칭/충돌**: `realestate` 패키지 + `RealEstateMarket*` prefix.
- **데이터 모델 통합 vs 분리**: 단일 history 테이블 + `RealEstateMarketCategory` enum 분류로 통합. 카테고리별 컬럼은 JSON `payload` 컬럼(jsonb) 한 개로 흡수.
- **백엔드 집계 vs 프론트 집계**: 백엔드 집계 채택.
- **Caffeine 충돌**: `realEstateCacheManager` named bean.
- **CaptureExecutor 분리**: per-item 트랜잭션 격리 학습 적용.

### Deferred to Implementation

- **외부 API 인증키 발급 절차**: data.go.kr(국토부, 청약홈, HUG), R-ONE, KOSIS, 건축HUB, 서울/경기 포털 각각 발급 후 `application-local.yml` / 운영 환경 변수에 주입. 발급 실패 시 해당 출처는 skip하고 plan 일정 별도 협의.
- **공공포털 응답 포맷 처리**: XML/JSON 혼재, `<resultCode>00`/`00`/`SUCCESS` 등 표준 부재. 출처별 어댑터 구현 시 결정.
- **트래픽 제한**: 출처별 일일 호출 한도 구현 시 측정. 초과 시 일배치를 출처별 시간대 분산하거나, 시군구별 N건씩 배치 분할.
- **KOSIS 항목 코드 매핑**: `orgId=116`, `tblId=DT_MLTM_2082` 외 미분양 통계표의 `prdSe`, `itmId`, `objL1` 코드는 인증키 발급 후 KOSIS Open API 명세에서 확인.
- **R-ONE 통계 코드**: 매매/전세/월세가격지수, 실거래가격지수, 거래호수의 통계표 ID는 R-ONE 개발가이드 확인.
- **건축HUB 시군구 단위 조회 가능 여부**: 시군구 인허가/사용승인 일자 조회 가능한 항목 코드 확인.
- **메서드/SQL 디테일**: 컨벤션상 메소드 5~10줄 한도 + guard clause. 정확한 메소드 분리는 구현 시.
- **테스트 작성 여부**: CLAUDE.md 정책상 명시 요청 시에만 작성. plan은 테스트 가능한 구조 유지로 충분, 실제 테스트는 구현 시 협의.
- **면적 단위 ㎡ vs 평 토글**: UX 디테일은 프론트 구현 시 결정.
- **데이터 기준일/갱신시각 노출 위치**: 카드별 vs 탭 헤더 vs 둘 다는 프론트 구현 시 결정.

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance for review, not implementation specification. The implementing agent should treat it as context, not code to reproduce.*

### 패키지 골격

```
realestate/
├── presentation/
│   ├── RealEstateMarketController.java        # GET /api/realestate/market/...
│   ├── RealEstateRegionController.java        # 지역 코드/즐겨찾기 진입
│   └── dto/
├── application/
│   ├── RealEstateMarketQueryService           # 11개 탭별 조회 유스케이스
│   ├── RealEstateMarketSaveService            # @Transactional 단위, 어댑터 호출 결과 저장
│   ├── RealEstateMarketAggregator             # 평균/중위/변화율/전세가율 백엔드 집계
│   ├── RealEstateRegionService                # 시군구/동 메타 조회
│   └── dto/                                   # *Result, *Command
├── domain/
│   ├── model/
│   │   ├── RealEstateMarketIndicator          # history 도메인 모델
│   │   ├── RealEstateMarketLatest             # latest 도메인 모델
│   │   ├── RealEstateMarketMetadata
│   │   ├── RealEstateMarketCategory (enum)    # TRADE/PRICE_INDEX/RENT/SUPPLY/UNSOLD/SUBSCRIPTION/GUARANTEE_RISK/SEOUL_LOCAL/GYEONGGI_LOCAL
│   │   ├── RealEstateMarketSource (enum)      # MOLIT/REB/HUB/KOSIS/SUBSCRIPTION_HOME/HUG/SEOUL_OPEN_DATA/GG_DATA_DREAM
│   │   ├── RegionCode (VO 시군구 5자리)
│   │   └── EmdCode (VO 법정동 8자리)
│   ├── repository/                            # 포트 인터페이스
│   └── service/
│       └── RealEstateMarketSourceAdapter      # (포트) fetchAll(RegionCode, period) -> List<RealEstateMarketIndicator>
└── infrastructure/
    ├── source/                                # 출처별 어댑터 8종 (각 폴더에 Client + Adapter + Registry + DTO + Exception + Properties)
    │   ├── molit/                             # 국토교통부 실거래가
    │   ├── reb/                               # R-ONE
    │   ├── hub/                               # 건축HUB
    │   ├── kosis/                             # KOSIS
    │   ├── subscriptionhome/                  # 청약홈
    │   ├── hug/                               # HUG
    │   ├── seoulopendata/                     # 서울 열린데이터광장
    │   └── ggdatadream/                       # 경기도 데이터드림
    ├── scheduler/
    │   ├── RealEstateMarketBatchScheduler     # @Scheduled(cron="0 0 6 * * *") KST
    │   ├── RealEstateMarketWarmupListener     # @EventListener(ApplicationReadyEvent.class)
    │   └── RealEstateMarketCaptureExecutor    # 별도 빈, 메소드별 @Transactional, per-item 격리
    ├── persistence/
    │   ├── RealEstateMarketIndicatorEntity    # history (snapshot_date 인덱스)
    │   ├── RealEstateMarketLatestEntity       # (region_code, category, source, indicator_code) Unique
    │   ├── RealEstateMarketMetadataEntity
    │   ├── *JpaRepository                     # native window function
    │   ├── *RepositoryImpl
    │   └── mapper/
    └── config/
        ├── RealEstateMarketProperties         # @ConfigurationProperties("realestate")
        ├── RealEstateMarketCacheConfig        # named CacheManager
        ├── RealEstateMarketAsyncConfig        # bounded ThreadPoolExecutor + MdcTaskDecorator
        └── RealEstateMarketMetadataInitializer  # ApplicationRunner, yml 시딩
```

### 일배치 흐름 (per-item 격리)

```mermaid
flowchart TB
  S["@Scheduled 06:00 KST"] --> B[RealEstateMarketBatchScheduler]
  B --> R[지역 목록 로드<br/>서울 25 + 경기 31]
  R --> L[지역×카테고리 루프]
  L --> CE[RealEstateMarketCaptureExecutor.captureOne]
  CE -->|@Transactional REQUIRED| SA[SourceAdapter.fetch]
  SA --> M[Latest 비교 + 변경 시 history INSERT]
  CE -->|예외| EX[로그 + 다음 항목 진행]
  L --> N[다음 지역×카테고리]
  L --> END[완료]
```

### 데이터 모델 (단일 history 테이블 + payload jsonb)

```text
real_estate_market_indicator
  id BIGSERIAL PK
  region_code VARCHAR(8)  NOT NULL
  category    VARCHAR(32) NOT NULL  -- RealEstateMarketCategory
  indicator_code VARCHAR(128) NOT NULL  -- 카테고리 내 고유 식별 (예: APT_TRADE_VOLUME, R-ONE_PRICE_INDEX_SALE)
  reference_text VARCHAR(64)         -- 기준월/주기 원본 문자열 (예: 2026-04, 2026Q1)
  value          NUMERIC(18,4)        -- 단일 수치
  payload        JSONB                -- 카테고리별 구조화 데이터 (단지/면적별 등)
  source         VARCHAR(32) NOT NULL -- RealEstateMarketSource
  source_url     VARCHAR(255)
  snapshot_date  TIMESTAMP NOT NULL
  cycle          VARCHAR(16)          -- M/Q/Y 등
  IDX (region_code, category, snapshot_date DESC)

real_estate_market_latest
  ... + (region_code, category, source, indicator_code) UNIQUE  -- upsert 키
  -- 같은 (region, category)에 복수 출처가 들어올 수 있어 source 포함 필수
  -- 예: T2 매매가격(국토부 실거래 + R-ONE 지수), T3 전월세(국토부 + R-ONE),
  --     T6 미분양(KOSIS + 국토부 통계누리)

real_estate_market_metadata
  category VARCHAR(32), source VARCHAR(32), indicator_code VARCHAR(128)
  display_name, description, unit, default_visible
  PK (category, source, indicator_code)
  -- source까지 포함해야 같은 indicator_code가 출처별로 메타를 가질 수 있음
```

## Implementation Units

- [ ] **Unit 1: realestate 도메인 골격 + 데이터 모델 + 메타데이터 yml**

**Goal:** 신규 도메인 패키지 + Entity 3종 + Repository 포트/어댑터 + 메타데이터 yml 시딩까지 — 후속 어댑터/스케줄러가 의존하는 골격.

**Requirements:** R3, R4, R8, R9, R13–R16

**Dependencies:** None

**Files:**
- Create:
  - `src/main/java/com/thlee/stock/market/stockmarket/realestate/domain/model/{RealEstateMarketIndicator,RealEstateMarketLatest,RealEstateMarketMetadata,RealEstateMarketCategory,RealEstateMarketSource,RegionCode,EmdCode}.java`
  - `.../realestate/domain/repository/{RealEstateMarketIndicatorRepository,RealEstateMarketLatestRepository,RealEstateMarketMetadataRepository,RealEstateRegionRepository}.java`
  - `.../realestate/infrastructure/persistence/{RealEstateMarketIndicatorEntity,RealEstateMarketLatestEntity,RealEstateMarketMetadataEntity,RealEstateRegionEntity}.java`
  - `.../realestate/infrastructure/persistence/{*JpaRepository, *RepositoryImpl, mapper/*Mapper}.java`
  - `.../realestate/infrastructure/config/RealEstateMarketMetadataInitializer.java`
  - `src/main/resources/realestate-region-codes.yml` (또는 `.json`/SQL — 행정안전부 법정동 기준, 서울 25구 + 경기 31시군 + **모든 읍면동 전체**)
  - `src/main/resources/realestate-indicator-metadata.yml` (카테고리 × 출처 × 지표 메타)
- Modify:
  - `ARCHITECTURE.md` — 도메인 표에 `RealEstate` 한 행 추가 (Approval Gate)

**Approach:**
- Entity는 ID 기반 참조만(`region_code` VARCHAR로 join 없이 보유). Lombok `@Getter @Builder`. CLAUDE.md 정책상 manual getter 금지.
- `payload` 컬럼은 `@JdbcTypeCode(SqlTypes.JSON)` Hibernate 6 jsonb 매핑. PostgreSQL `JSONB`.
- `RealEstateMarketLatestEntity`는 `(region_code, category, source, indicator_code)` Unique. **source 포함 필수** — 같은 region×category에 복수 출처가 들어오는 경우(T2 매매가격: 국토부 실거래 + R-ONE 지수, T3 전월세: 국토부 + R-ONE, T6 미분양: KOSIS + 국토부 통계누리)가 있어 source 빠지면 덮어써짐. upsert는 `merge` + `@Version` 미사용(Optimistic 미적용, 일배치 단일 thread per item).
- `RealEstateMarketMetadataEntity`도 PK에 source 포함: `(category, source, indicator_code)`.
- `RealEstateMarketCategory` enum 9종 + `RealEstateMarketSource` enum 8종.
- `RegionCode`/`EmdCode`는 VO. `RegionCode.SEOUL_PREFIX = "11"`, `RegionCode.GG_PREFIX = "41"` 헬퍼.
- 지역 코드 시딩은 `realestate-region-codes.yml`(또는 `.json`/SQL resource)로 분리: 행정안전부 법정동 코드 기준 **서울 25개 자치구 + 모든 읍면동 + 경기 31개 시군 + 모든 읍면동 전체**. brainstorm R12("시도+시군구+읍면동(선택) 단위")를 충족하기 위함. `ApplicationRunner`가 `region count() == 0`일 때 1회 시딩.
- 지표 메타 yml(`realestate-indicator-metadata.yml`)은 카테고리×출처×지표 단위로 분리.
- JpaRepository에 `ROW_NUMBER() OVER (PARTITION BY region_code, category, source, indicator_code ORDER BY snapshot_date DESC)` window function 활용 native query 추가.

**Patterns to follow:**
- `economics/infrastructure/persistence/EcosIndicatorEntity.java` (history)
- `economics/infrastructure/persistence/EcosIndicatorLatestEntity.java` (latest)
- `economics/infrastructure/persistence/EcosIndicatorJpaRepository.java` (window function native @Query)
- `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataInitializer.java`
- `src/main/resources/ecos-indicator-metadata.yml` (yml 구조)

**Test scenarios:**
- Test expectation: none — 명시 요청 없으므로 테스트 미작성. 구조는 테스트 가능하도록 유지(Mapper/Repo가 순수 POJO에 의존, 외부 의존 없음).

**Verification:**
- 애플리케이션 부팅 시 `RealEstateMarketMetadataInitializer`가 1회 yml을 읽어 region/metadata 테이블에 시딩되며, 재부팅 시 추가 INSERT 없음.
- 서울 25개 자치구 + 경기 31개 시군 + 두 시도의 모든 읍면동(법정동) 코드가 region 테이블에 존재 (R12 충족).

---

- [ ] **Unit 2: 외부 API 공통 인프라 (Properties + per-item Executor + Cache + Adapter 포트)**

**Goal:** 8개 출처가 공유할 공통 인프라(Properties, RestClient timeout 설정, bounded executor, named CacheManager, source adapter 포트) 마련.

**Requirements:** R7 (일배치), R17 (누락 표시), R8 (출처 메타)

**Dependencies:** Unit 1 (포트 정의 위치)

**Files:**
- Create:
  - `.../realestate/domain/service/RealEstateMarketSourceAdapter.java` (포트 인터페이스: `RealEstateMarketSource supportedSource()`, `List<RealEstateMarketIndicator> fetch(RegionCode, period)`)
  - `.../realestate/domain/service/RealEstateMarketSourceFactory.java` (포트)
  - `.../realestate/infrastructure/RealEstateMarketSourceFactoryImpl.java` (생성자에서 `List<RealEstateMarketSourceAdapter>` 자동 주입 → `groupingBy(supportedSource)`)
  - `.../realestate/infrastructure/config/RealEstateMarketProperties.java` (`@ConfigurationProperties("realestate")`, source별 sub-properties)
  - `.../realestate/infrastructure/config/RealEstateMarketCacheConfig.java` (named bean `realEstateCacheManager`, Caffeine, expireAfterWrite=25h, 빈 응답 차등 TTL=60s via `Expiry`)
  - `.../realestate/infrastructure/config/RealEstateMarketAsyncConfig.java` (bounded `ThreadPoolExecutor`, `ArrayBlockingQueue`, `AbortPolicy`, daemon=false, `@PreDestroy` `awaitTermination(5s)`, MdcTaskDecorator)
  - `.../realestate/infrastructure/exception/RealEstateMarketApiException.java`
- Modify:
  - `src/main/resources/application.yml` — `realestate:` block 추가 (8개 source의 base-url/timeout/api-key 환경변수 placeholder)

**Approach:**
- Adapter 포트는 단일 메소드 `fetch(RegionCode region, RealEstateMarketCategory category, FetchWindow window)` 반환 `List<RealEstateMarketIndicator>` 또는 부분 실패 표시 `FetchResult`(success indicators + failed reason).
- Factory는 `news/infrastructure/NewsSearchPortFactoryImpl.java` 패턴 그대로(생성자 List 주입 + groupingBy).
- Properties는 source별 sub class: `MolitProperties`, `RebProperties`, `KosisProperties` 등 — 각 `api-key`, `base-url`, `connect-timeout`, `read-timeout`, `daily-quota`(optional).
- `RestClient`는 기존 `news/infrastructure/config/RestClientConfig.java` bean 공유. timeout은 source 단위로 override 필요 시 source 폴더 내에 `*ClientConfig`로 별도 RestClient 빌드.
- Async executor는 `core=4, max=8, queue=200, AbortPolicy`. Per-item 호출용 — N×지역×카테고리 폭증 방지.
- Cache는 빈 응답 60s, 정상 응답 25h(다음 새벽 6시 직후 만료).

**Patterns to follow:**
- `news/infrastructure/NewsSearchPortFactoryImpl.java`
- `news/infrastructure/config/RestClientConfig.java`
- `economics/infrastructure/korea/ecos/config/{EcosProperties.java, EcosCacheConfig.java}`
- `stocknote/infrastructure/async/StocknoteAsyncConfig.java`
- `economics/infrastructure/global/tradingeconomics/config/GlobalIndicatorCacheConfig.java` (`Expiry`로 차등 TTL — 빈 응답 60s)

**Test scenarios:**
- Test expectation: none — 인프라 골격, 명시 요청 없음. 외부 API와의 통합은 Unit 3 어댑터에서 각각 검증.

**Verification:**
- `realestate` 시작 시 named cache manager가 `@Primary` ECOS와 충돌 없이 동시 등록.
- bounded executor 스레드 풀이 daemon=false로 등록되며, 앱 셧다운 시 `@PreDestroy`로 graceful 종료(5초 timeout).

---

- [ ] **Unit 3: 출처별 어댑터 8종 (per-source Adapter + 카테고리 Registry)**

**Goal:** 8개 출처에 대해 동일 패턴의 Source Adapter 구현. 각 출처 내부에서 카테고리/지표는 Registry로 매핑.

**Requirements:** R3 (11개 탭 데이터), R4–R5 (서울/경기 우선)

**Dependencies:** Unit 2 (포트 + 인프라)

**Files:**
- Create (각 source 폴더 내):
  - `.../realestate/infrastructure/source/molit/{MolitClient, MolitTradeAdapter, MolitRentAdapter, MolitIndicatorRegistry, dto/*, exception/MolitApiException}.java`
  - `.../realestate/infrastructure/source/reb/{RebClient, RebPriceIndexAdapter, RebTransactionVolumeAdapter, RebIndicatorRegistry, dto/*, exception/RebApiException}.java`
  - `.../realestate/infrastructure/source/hub/{HubClient, HubPermitAdapter, HubIndicatorRegistry, dto/*, exception/*}.java`
  - `.../realestate/infrastructure/source/kosis/{KosisClient, KosisUnsoldAdapter, KosisIndicatorRegistry, dto/*, exception/*}.java`
  - `.../realestate/infrastructure/source/subscriptionhome/{SubscriptionHomeClient, SubscriptionAdapter, SubscriptionRegistry, dto/*, exception/*}.java`
  - `.../realestate/infrastructure/source/hug/{HugClient, HugRiskAdapter, HugRegistry, dto/*, exception/*}.java`
  - `.../realestate/infrastructure/source/seoulopendata/{SeoulOpenDataClient, SeoulOpenDataAdapter, SeoulRegistry, dto/*, exception/*}.java`
  - `.../realestate/infrastructure/source/ggdatadream/{GgDataDreamClient, GgDataDreamAdapter, GgRegistry, dto/*, exception/*}.java`

**Approach:**
- 각 Adapter는 `RealEstateMarketSourceAdapter` 구현. `supportedSource()`로 자기 source enum 반환.
- 한 출처 안의 여러 카테고리는 동일 Adapter 내 분기(`switch (category)`) 또는 카테고리별 Adapter 분리(국토부 실거래는 매매/전월세 별 API라 분리 권장).
- Registry는 `Map<RealEstateMarketCategory, IndicatorMapping>`(URL/지표 코드/응답 필드 경로). `economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorRegistry.java` 패턴.
- DTO는 외부 응답 그대로 매핑 후 도메인 모델로 변환. 문자열은 trim/normalize 최소화 — `referenceText`는 원본 그대로 보존(global-indicator-history-mirroring 학습).
- 응답 포맷 차이(XML vs JSON) 흡수: client 레벨에서 RestClient `accept(MediaType.APPLICATION_JSON)` 우선, XML만 지원하는 출처는 Jackson XML 모듈 또는 Jsoup. data.go.kr 일부 API는 `_type=json` 쿼리 파라미터.
- 에러 응답 (`<resultCode>00`/공공포털 표준) 검증 후 `RealEstateMarketApiException`으로 변환.
- 인증키 미설정 시 어댑터는 `@ConditionalOnProperty`로 비활성화하지 말고, 부팅은 진행하되 `fetch()`에서 `IllegalStateException` 던져 스케줄러가 skip하고 로그.

**Patterns to follow:**
- `economics/infrastructure/korea/ecos/EcosApiClient.java` (RestClient + 예외 변환)
- `economics/infrastructure/korea/ecos/EcosIndicatorAdapter.java` (DTO → 도메인 변환)
- `economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorRegistry.java` (Registry)

**Execution note:** 출처 8종은 각자 commit 단위로 분리해서 점진 진행 권장. 한 출처씩 인증키 발급 + 어댑터 구현 + 일배치 dry-run 검증 후 다음 출처 진행.

**Test scenarios:**
- Test expectation: none — 명시 요청 없음. 구현 시 fakeWebServer 기반 통합 테스트는 implementer 판단.
- 단, 응답 파서/Registry 매핑은 단위 테스트가 손쉽게 가능한 구조로 분리.

**Verification:**
- 각 어댑터가 인증키 주입 시 1개 이상의 실제 응답을 받아 도메인 모델로 변환 가능.
- 인증키 미주입/요청 실패 시 `RealEstateMarketApiException` 또는 `IllegalStateException` 발생, 스케줄러 정상 진행.

---

- [ ] **Unit 4: 일배치 스케줄러 + Warmup + Save 서비스 (per-item transaction 격리)**

**Goal:** 매일 06:00 KST 일배치로 모든 출처를 호출, per-item 트랜잭션 격리로 부분 실패 흡수. Warmup은 **외부 API 호출 없이** DB latest를 메모리 캐시로만 hot-load (재기동 시 외부 쿼터 소모/부팅 지연 회피).

**Requirements:** R7, R8, R9, R17

**Dependencies:** Unit 1 (Repository), Unit 2 (Factory + Cache + Executor), Unit 3 (Adapters)

**Files:**
- Create:
  - `.../realestate/infrastructure/scheduler/RealEstateMarketBatchScheduler.java` (`@Scheduled(cron="0 0 6 * * *", zone="Asia/Seoul")`)
  - `.../realestate/infrastructure/scheduler/RealEstateMarketWarmupListener.java` (`@EventListener(ApplicationReadyEvent.class)`)
  - `.../realestate/infrastructure/scheduler/RealEstateMarketCaptureExecutor.java` (별도 빈, 메소드 단위 `@Transactional` REQUIRED)
  - `.../realestate/application/RealEstateMarketSaveService.java` (`@Transactional`, latest 1회 조회 + 메모리 비교 + 변경분만 history INSERT)

**Approach:**
- Scheduler는 `@Transactional` 없음, try-catch만. Region × Category 모든 조합을 순회.
- 각 항목은 `captureExecutor.captureOne(region, category, source)` 호출 — **외부 빈 호출이라야 Spring AOP 프록시가 트랜잭션 래핑** (학습: `external-http-per-item-transaction-isolation-2026-04-26`).
- CaptureExecutor.captureOne 내부에서 SourceAdapter.fetch → SaveService.upsert 호출. 예외 발생 시 그 항목만 롤백, 다음 항목 진행.
- SaveService는 `latestRepository.findAllByRegionAndCategoryAndSource(regionCode, category, source)`로 **source 단위까지 좁혀** 1회 조회 → `Map<compareKey, latest>` → 어댑터 결과와 비교 → 변경분만 history INSERT + latest update. **compareKey는 `regionCode::category::source::indicatorCode::referenceText`** — source 포함 필수. 같은 region×category에 복수 출처가 들어오는 경우 출처별로 독립 비교/저장. (학습: `global-indicator-history-mirroring`)
- **WarmupListener는 외부 API를 호출하지 않는다.** `ApplicationReadyEvent` 1회에서 `RealEstateMarketLatestRepository`를 통해 DB의 latest 전체(또는 인기 region/category subset)를 조회하여 Caffeine 캐시에 put 하는 in-process hot-load만 수행. 외부 fetch는 일배치(06:00)에서만 발생. 부팅마다 8개 출처 × 56시군 × 9카테고리의 외부 호출이 발생하지 않도록 명시적 분리.
- 일배치는 Async executor를 사용해 region×category 호출을 N병렬로(최대 8 thread, queue 200). 벽시계 timeout 적용(`.get(60, SECONDS)`).
- 데이터 누락 시 Latest는 미갱신, response 메타에는 마지막 성공 `snapshot_date`만 노출.

**Patterns to follow:**
- `economics/infrastructure/scheduler/EcosIndicatorBatchScheduler.java`
- `economics/infrastructure/scheduler/EcosIndicatorWarmupListener.java`
- `economics/application/EcosIndicatorSaveService.java` (`@Transactional` 위치, latestMap 1회 조회)
- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` (CaptureExecutor 분리)
- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md` (3-table mirroring)

**Test scenarios:**
- Test expectation: none — 통합 테스트는 인증키 의존이라 implementer 판단. 구조는 SaveService를 단위 테스트 가능하도록 SourceAdapter는 mockable한 포트로 유지.

**Verification:**
- 임의 출처 1개 인증키만 활성 상태에서 06:00 트리거 시, 활성 출처는 history INSERT + latest update, 비활성 출처는 로그 후 skip, 앱은 정상.
- 한 항목의 외부 호출 실패가 다른 항목 처리에 영향 없음.
- Warmup listener는 외부 호출 0건 (DB read only). 부팅 시간 영향 미미.
- 부팅 시 외부 API 쿼터가 차감되지 않음 (관찰: 8개 출처 트래픽 카운터 변화 없음).

---

- [ ] **Unit 5: REST API + Application 유스케이스 (11개 탭 + 지역 비교 + 출처 메타)**

**Goal:** 11개 탭에 필요한 모든 수치/시계열/비교 데이터를 백엔드에서 집계하여 응답하는 API.

**Requirements:** R1, R2, R3, R6, R13, R14, R15, R16

**Dependencies:** Unit 1 (Repo), Unit 4 (Save 결과를 읽기만)

**Files:**
- Create:
  - `.../realestate/presentation/RealEstateMarketController.java` (`/api/realestate/market/...`)
  - `.../realestate/presentation/RealEstateRegionController.java` (`/api/realestate/regions`)
  - `.../realestate/presentation/dto/{SummaryResponse, TabResponse, ComparisonResponse, RegionResponse, SourceMetaResponse, FetchWindowQuery}.java`
  - `.../realestate/application/RealEstateMarketQueryService.java`
  - `.../realestate/application/RealEstateMarketAggregator.java` (평균/중위/변화율/전세가율 추정)
  - `.../realestate/application/RealEstateRegionService.java`

**Approach:**
- Controller는 GET 전용. 탭별 endpoint 분리: `/summary`, `/tabs/trade`, `/tabs/price`, `/tabs/rent`, `/tabs/official-stats`, `/tabs/supply`, `/tabs/unsold`, `/tabs/subscription`, `/tabs/risk`, `/tabs/local`, `/tabs/comparison`, `/tabs/sources`.
- 모든 endpoint에 `region_code`, `period`(1m/3m/6m/1y), `housing_type`(APT/OPISTL/...), `area`(optional) 쿼리 파라미터.
- Aggregator는 history에서 N월 시계열 조회 후 평균/중위/변화율 계산. **백엔드 집계** (이슈 #35 정신 정합).
- 전세가율은 매매 평균 + 전세 평균을 같은 region/period/area로 매핑 후 `rent_deposit / sale_price` 비율 — 추정값임을 응답에 `estimated: true`로 명시.
- 해제 거래는 국토부 실거래 상세 API(`canceled` 필드)에서 추출, 응답에 분리 필드.
- 지역 비교(T10)는 선택 region + 비교 region N개에 대해 동일 지표를 한 응답에 담음.
- 출처 메타(T11)는 metadata + 각 source의 마지막 성공 snapshot_date 반환.
- 모든 응답은 `referenceText`, `snapshotDate`, `source`, `sourceUrl`을 항목마다 포함. **판단 라벨 절대 미노출** — DTO에서 `signal`, `grade`, `recommendation` 필드 자체 금지.
- 누락 시 응답은 빈 컬렉션 + `lastUpdatedAt: null` + `nextScheduledAt`.
- `@Transactional(readOnly=true)`.

**Patterns to follow:**
- `economics/presentation/EcosIndicatorController.java`
- `economics/application/EcosIndicatorService.java` (캐시 + 트랜잭션 패턴)
- `stocknote/application/StockNoteDashboardService.java` (Caffeine TTL 30m 캐시 적용)

**Test scenarios:**
- Test expectation: none — implementer 판단. Aggregator는 순수 함수형으로 분리해 단위 테스트 가능 구조로 유지.

**Verification:**
- 각 탭 endpoint가 한 region/period 조건으로 200 응답 + `referenceText`/`snapshotDate`/`source` 메타 포함.
- 데이터 부재 시 4xx 아닌 200 + 빈 페이로드 + 메타.
- 응답 어디에도 판단/추천/등급 표현 없음.

---

- [ ] **Unit 6: 관심지역(region 즐겨찾기) — realestate 도메인 자체 entity로 분리**

**Goal:** 사용자별 부동산 관심지역 등록/해제. 기존 `favorite` 도메인은 "지표 즐겨찾기"(ECOS/GLOBAL indicator) 의미가 명확하므로 **건드리지 않고**, realestate 도메인 자체에 region 즐겨찾기 entity를 신설한다. 즐겨찾기 단위는 brainstorm R12에 따라 시도+시군구+읍면동(선택). **주택유형/면적 등 필터 상태는 즐겨찾기에 포함하지 않음** — 칩은 "지역"이지 "지역+필터 상태"가 아님.

**Requirements:** R10, R11, R12

**Dependencies:** Unit 1 (region 메타), Unit 5 (region 조회 API)

**Files:**
- Create:
  - `.../realestate/domain/model/UserFavoriteRealEstateRegion.java` (도메인 모델)
  - `.../realestate/domain/repository/UserFavoriteRealEstateRegionRepository.java` (포트)
  - `.../realestate/infrastructure/persistence/UserFavoriteRealEstateRegionEntity.java` (Entity)
  - `.../realestate/infrastructure/persistence/UserFavoriteRealEstateRegionJpaRepository.java`
  - `.../realestate/infrastructure/persistence/UserFavoriteRealEstateRegionRepositoryImpl.java`
  - `.../realestate/infrastructure/persistence/mapper/UserFavoriteRealEstateRegionMapper.java`
  - `.../realestate/application/RealEstateFavoriteRegionService.java`
  - `.../realestate/presentation/RealEstateFavoriteRegionController.java` (`POST/DELETE/GET /api/realestate/favorites/regions`)
  - `.../realestate/presentation/dto/{FavoriteRegionRequest, FavoriteRegionResponse}.java`
- **변경 없음 (의도적)**: `favorite` 도메인은 수정하지 않는다. `FavoriteIndicatorSourceType` enum은 그대로 ECOS/GLOBAL 유지. 의미 도메인을 흐리지 않기 위해 별도 도메인에서 처리.

**Approach:**
- Entity 스키마: `user_favorite_real_estate_region(id, user_id BIGINT NOT NULL, sido_code VARCHAR(2) NOT NULL, sigungu_code VARCHAR(5) NOT NULL, emd_code VARCHAR(8) NOT NULL, display_order INT, created_at)`.
- **emd_code는 NULL 대신 sentinel 빈 문자열(`""`)로 정규화** — 시군구 단위 즐겨찾기는 `emd_code = ""`, 읍면동 단위는 8자리 법정동 코드. 표현식 기반 unique(`COALESCE(emd_code,'')`)는 JPA `@UniqueConstraint`로 선언 불가 + 프로젝트 스키마 관리 방식이 `ddl-auto: update`라 수동 DDL이 승인 범위로 들어옴 — 이를 회피하기 위해 컬럼 자체를 NOT NULL DEFAULT `""`로 정규화.
- Unique: `@UniqueConstraint(columnNames={"user_id","sido_code","sigungu_code","emd_code"})` — 표준 JPA 표현으로 `ddl-auto: update`가 그대로 처리.
- 도메인 모델 관점에서는 `Optional<EmdCode>`로 wrapping하여 빈 문자열 sentinel을 외부에 노출하지 않음(infrastructure mapper에서 `""` ↔ `Optional.empty()` 양방향 변환).
- 즐겨찾기 식별은 region 코드 자체. `housing_type`/`area` 같은 필터 상태는 저장하지 않는다.
- userId는 JWT SecurityContext 추출. `@RequestParam` 금지.
- POST 등록(중복은 idempotent). DELETE 해제. GET은 사용자 region 목록 + region 메타(시군구명/읍면동명) join 응답.
- Region 메타 enrich는 application service가 `RealEstateRegionService`(Unit 5) 주입.
- Approval Gate: 신규 Entity 생성. `favorite` 도메인 Entity/enum 변경은 없음.

**Patterns to follow:**
- 신규 entity이므로 favorite 도메인 entity 구조만 참고: `favorite/infrastructure/persistence/UserFavoriteIndicatorEntity.java` (Unique 제약 + userId BIGINT + JWT 추출 패턴)
- `economics/infrastructure/persistence/EcosIndicatorRepositoryImpl.java` (Repository 분리 패턴)

**Test scenarios:**
- Test expectation: none — implementer 판단.

**Verification:**
- 로그인 사용자가 시군구 단위 또는 읍면동 단위로 region 즐겨찾기 등록/해제 가능.
- 등록된 관심지역이 대시보드 진입 시 칩으로 노출 (지역명만 표기, 필터 상태 미포함).
- 같은 region을 두 번 등록해도 idempotent (중복 row 없음).
- favorite 도메인의 ECOS/GLOBAL 즐겨찾기 동작은 변동 없음.

---

- [ ] **Unit 7: 프론트엔드 partial + component + 11개 탭 차트**

**Goal:** `index.html`에 부동산 partial 슬롯 추가, `partials/realestate.html` + `js/components/realestate.js`로 11개 탭 + 필터 + 관심지역 칩 렌더링.

**Requirements:** R1, R2, R3, R11, R13, R14, R15, R16, R17

**Dependencies:** Unit 5 (API), Unit 6 (관심지역 API)

**Files:**
- Create:
  - `src/main/resources/static/partials/realestate.html` (필터 영역 + 요약 카드 + 11개 탭 + 관심지역 칩 + 출처/기준일 표시)
  - `src/main/resources/static/js/components/realestate.js` (Alpine `x-data` mixin, Chart.js 인스턴스 관리)
- Modify:
  - `src/main/resources/static/index.html` — `<div data-partial="realestate">` 슬롯 + script 등록 + 사이드바 메뉴 항목
  - `src/main/resources/static/js/app.js` — `RealEstateComponent` 통합
  - `src/main/resources/static/js/api.js` — `getRealEstate*` 메소드 추가

**Approach:**
- partial-loader.js의 name allow-list regex 통과 이름(`realestate`).
- 카테고리 탭 UI는 `partials/ecos.html`의 `<template x-for>` + `tab-active` 패턴 그대로.
- Chart.js 인스턴스는 `_chartInstances = new Map()`으로 관리(ecos.js 참고). 탭 전환 시 destroy + recreate 또는 update.
- 각 카드/차트에 `data-source` `data-reference-text` `data-snapshot-date` 속성 표시. 디자인은 카드 모서리에 작은 메타 라벨.
- 판단/등급/신호 라벨 절대 사용 금지. 컴포넌트 코드에 `signal`/`grade`/`recommend` 키워드 사용 금지(코드 리뷰 체크포인트).
- 면적 단위는 우선 ㎡로 노출 + 평으로 토글 버튼 (UX 디테일은 구현 시 결정 OK).
- 관심지역 칩은 상단 sticky.
- 응답 누락 시 빈 영역에 출처/사유/다음 갱신 예상 시각 표시(R17).

**Patterns to follow:**
- `static/partials/ecos.html` (카테고리 탭 + Chart.js 차트 + 메타)
- `static/js/components/ecos.js` (`_chartInstances` 패턴, Alpine `x-data` mixin)
- `static/js/utils/partial-loader.js` (name 규약)

**Test scenarios:**
- Test expectation: none — 프론트 정적 자원, 명시 요청 없음. 시각 검증은 implementer가 브라우저로.

**Verification:**
- 로그인 사용자가 사이드바에서 부동산 메뉴 진입 시 partial이 lazy-load되며 11개 탭 모두 데이터 표시.
- 데이터 부재 영역에 빈 상태 + 출처/사유/다음 갱신 시각.
- 차트/카드 어디에도 판단/등급/신호 문구 없음.

---

- [ ] **Unit 8: 데이터 출처 탭 + 운영/문서/스모크 검증**

**Goal:** T11 데이터 출처 탭의 메타 정보 노출 + 모든 카드/차트에 출처/기준일이 빠짐없이 표기되는지 운영 검증 + 신규 도메인 학습 적재.

**Requirements:** R8, R9, R16, R17 + 성공 기준 (서울/경기 주요 지역 11개 탭 정상 노출)

**Dependencies:** Unit 5, Unit 7

**Files:**
- Create:
  - `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-XX-XX.md` (한국 공공 Open API 인증/포맷/EUC-KR/공공포털 에러 응답 학습 적재)
- Modify:
  - `static/partials/realestate.html` — T11 데이터 출처 탭 본체
  - `ARCHITECTURE.md` — RealEstate 도메인 행 보강(이미 Unit 1에서 추가, 이 단계에서 최종 정리)

**Approach:**
- T11 출처 탭은 `/tabs/sources` 응답을 표 형태로 렌더링. 출처별 `last_success_at`, `next_scheduled_at`, `total_indicators`, `coverage_regions` 노출.
- 운영 스모크: 서울 25구 + 경기 31시군 중 임의 5개 sample을 manual 검증 — 모든 탭 카드/차트에 출처/기준일 표시 여부.
- 학습 문서는 구현 후 마지막에 적재 (인증키 발급 절차, 출처별 응답 quirk, EUC-KR 처리, 공공포털 에러 코드 매핑).

**Patterns to follow:**
- 기존 `docs/solutions/architecture-patterns/*.md` 형식

**Test scenarios:**
- Test expectation: none — 운영/문서 단계.

**Verification:**
- T11 탭이 8개 출처 모두에 대해 `last_success_at`/`next_scheduled_at` 표시.
- 운영 스모크 5개 sample 모두 11개 탭 정상 노출 + 출처/기준일 표기.
- 학습 문서 1편 작성.

## System-Wide Impact

- **Interaction graph**: 신규 도메인 단독 구성. `favorite` 도메인은 변경하지 않음(region 즐겨찾기는 realestate 도메인 자체 entity). 사이드바 메뉴 + partial-loader 추가. 그 외 기존 도메인 영향 없음.
- **Error propagation**: 어댑터 실패 → CaptureExecutor가 항목 롤백 → Scheduler 로그 후 다음 항목. 사용자 응답은 빈 페이로드 + 메타. 외부 의존 실패가 다른 도메인에 전파되지 않음.
- **State lifecycle risks**: latest 테이블 unique 키는 `(region_code, category, source, indicator_code)`로 source 포함 — 같은 region×category에 복수 출처가 들어올 때 덮어쓰기 방지. 단일 thread per item 일배치 + per-source row 분리로 race 없음.
- **API surface parity**: 신규 `/api/realestate/*` 엔드포인트만 추가. 기존 API 변경 없음.
- **Integration coverage**: 외부 8개 출처는 인증키 발급 후 dry-run으로 검증. 단위 테스트로는 응답 매핑/Aggregator 로직만 가능, 실제 통합은 운영 스모크.
- **Unchanged invariants**: economics/news/portfolio/stocknote/chatbot 도메인 기존 동작 그대로. `RealEstateItemEntity`(portfolio) 의미와 책임 변동 없음.

## Risks & Dependencies

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| 외부 API 인증키 발급 지연 | High | High | 출처별 어댑터 분리 + `@ConditionalOnProperty` 회피(부팅은 진행). 발급 완료된 출처부터 incremental rollout. |
| 공공 API 트래픽 한도 초과 | Med | Med | bounded executor + 일배치 + 빈 응답 차등 TTL. 한도 도달 시 출처별 시간대 분산(06:00 → 06:10 → 06:20...). |
| 응답 포맷 변동 (XML→JSON, 필드 변경) | Med | Med | DTO 매핑은 출처별 어댑터 안에 격리. 변동 감지는 어댑터 단위 예외 + 로그. |
| KOSIS/R-ONE 통계표 코드 매핑 오류 | Med | Med | 메타 yml 시딩 + 메타 검증 스모크. 잘못된 매핑은 빈 응답 또는 명백한 0값으로 빠르게 감지. |
| Region 즐겨찾기 단위 혼동(시군구/읍면동) | Low | Med | Unique 키에 `COALESCE(emd_code,'')` 포함. UI 칩은 항상 region 메타 join으로 시군구+(있으면)읍면동 표기. |
| 부팅 시 외부 API 쿼터 소모 | Low | High | Warmup은 외부 호출 0건(DB latest hot-load만). 일배치 외 외부 호출 경로 없음. 코드 리뷰 + 출처별 트래픽 모니터링. |
| latest 덮어쓰기(같은 region×category 복수 출처) | Med | High | Unique 키에 `source` 포함. 출처별로 독립 row 유지. Mapping 메타는 source별 분리. |
| 판단 라벨 코드 누락 노출 | Med | Med | DTO 명시 금지 필드 리스트 + 코드 리뷰 체크포인트 + 통합 테스트 시 응답 grep. |
| Caffeine bean 충돌 | Low | High | named bean(`realEstateCacheManager`) + `@Primary` 회피. 부팅 시 충돌 시 즉시 실패. |
| Entity 연관관계 금지 정책 위반 | Low | High | Entity는 ID 컬럼만. 코드 리뷰. (CLAUDE.md 정책) |

## Dependencies / Prerequisites

- 외부 인증키 발급: data.go.kr(국토부, 청약홈, HUG), R-ONE, KOSIS, 건축HUB, 서울 열린데이터광장, 경기도 데이터드림 → 운영 환경 변수 또는 `application-local.yml`.
- PostgreSQL JSONB 지원 (Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`).
- 기존 RestClient/스케줄러/Caffeine 인프라가 안정 상태 (favorite 도메인은 변경하지 않으므로 의존 없음).

## Phased Delivery

### Phase 1: 백엔드 골격 + 첫 출처 (1~2주)
- Unit 1, Unit 2, Unit 4 + Unit 3의 1개 출처(국토부 매매 실거래 우선) → 일배치 동작 확인 + dry-run.

### Phase 2: 나머지 출처 + API + 관심지역 (2~3주)
- Unit 3 나머지 7개 출처 + Unit 5(API) + Unit 6(realestate 도메인 region 즐겨찾기 entity 추가) — Approval Gate 통과 후.

### Phase 3: 프론트 + 출처 탭 + 운영 검증 + 학습 적재
- Unit 7(프론트), Unit 8(T11 + 스모크 + 학습 문서).

## Documentation Plan

- `ARCHITECTURE.md` — 도메인 표 RealEstate 행 추가 (Unit 1).
- `docs/policies/code-convention.md` — 변경 없음 (기존 정책 그대로 적용).
- `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-XX-XX.md` — Unit 8에서 작성.
- 운영 README — 외부 인증키 환경변수 목록 (별도 운영 노트).

## Operational / Rollout Notes

- 일배치 시간대: 06:00 KST 고정. ECOS(07:00)/Global(07:30)과 분리.
- 인증키 미설정 시에도 부팅은 정상 진행. `@ConditionalOnProperty`로 어댑터 자체를 비활성화하지 않고, 일배치 `fetch()` 시점에서 `IllegalStateException`을 던져 스케줄러가 해당 항목만 skip하고 로그를 남긴다(Unit 3 Approach와 동일). 다른 도메인 영향 없음.
- 모니터링: 기존 application logging Elasticsearch 인덱스에 `domain=realestate` 라벨로 적재(별도 인프라 추가 없음).
- Rollback: 신규 도메인이라 feature flag 미사용. 문제 발생 시 사이드바 메뉴 항목 hide만으로 사용자 노출 차단 가능.

## Sources & References

- **Origin document**: [docs/brainstorms/2026-05-06-real-estate-market-data-requirements.md](../brainstorms/2026-05-06-real-estate-market-data-requirements.md)
- **Architecture**: [ARCHITECTURE.md](../../ARCHITECTURE.md)
- **Conventions**: [docs/policies/code-convention.md](../policies/code-convention.md), [docs/policies/git-worktree.md](../policies/git-worktree.md)
- **Reference patterns** (economics 1:1 미러):
  - `economics/infrastructure/korea/ecos/{EcosApiClient, EcosIndicatorAdapter, config/EcosProperties, config/EcosCacheConfig}`
  - `economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorRegistry.java`
  - `economics/infrastructure/scheduler/{EcosIndicatorBatchScheduler, EcosIndicatorWarmupListener}`
  - `economics/application/EcosIndicatorSaveService.java`
  - `economics/infrastructure/persistence/{EcosIndicatorEntity, EcosIndicatorJpaRepository, mapper/EcosIndicatorMapper}`
- **Reference patterns** (factory):
  - `news/infrastructure/NewsSearchPortFactoryImpl.java`
  - `news/infrastructure/config/RestClientConfig.java`
- **Reference patterns** (async + cache):
  - `stocknote/infrastructure/async/StocknoteAsyncConfig.java`
  - `stocknote/infrastructure/cache/StocknoteCacheConfig.java`
- **Reference patterns** (favorite):
  - `favorite/{application/FavoriteIndicatorService, domain/model/FavoriteIndicatorSourceType, infrastructure/persistence/UserFavoriteIndicatorEntity}`
- **Frontend patterns**:
  - `static/partials/ecos.html`, `static/js/components/ecos.js`, `static/js/utils/partial-loader.js`
- **Institutional learnings**:
  - `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`
  - `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md`
  - `docs/solutions/performance-issues/parallel-external-fetch-resilience-2026-04-23.md`
  - `docs/plans/2026-04-15-002-feat-favorite-indicator-dashboard-plan.md`
- **Issue**: #41
