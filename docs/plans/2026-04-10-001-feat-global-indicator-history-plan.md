---
title: 글로벌 경제지표 히스토리 로그 적재
type: feat
status: active
date: 2026-04-10
origin: docs/brainstorms/2026-04-09-global-indicator-history-brainstorm.md
---

# 글로벌 경제지표 히스토리 로그 적재

## Overview

국내 경제지표(ECOS)에서 이미 운영 중인 "cycle 변경 감지 기반 히스토리 적재" 패턴을 글로벌 경제지표(TradingEconomics)에도 동일하게 적용한다. 현재 글로벌 지표는 `GlobalIndicatorCacheService`의 Caffeine 캐시로만 노출되고 있어, (국가, 지표)별 값이 시간에 따라 어떻게 변했는지 추적할 수 없는 상태다.

3개 테이블(`global_indicator` history / `global_indicator_latest` / `global_indicator_metadata`)을 신규 추가하고, 전용 배치 스케줄러 `GlobalIndicatorBatchScheduler`가 매일 07:30에 55개 지표를 1.5초 간격으로 순차 수집하여 히스토리로 누적한다. 사용자 API는 기존 캐시 흐름을 그대로 유지한다.

## Problem Statement / Motivation

- **추적 불가**: 글로벌 지표가 언제 값을 갱신했는지, 이전 값이 얼마였는지 DB에 남지 않음. ECOS는 이미 `ecos_indicator` 테이블에 히스토리를 남기고 있어 비대칭이 존재
- **분석 한계**: 챗봇/대시보드에서 "미국 CPI가 지난 분기 대비 어떻게 변했지?" 같은 질의에 답변 불가 (API로는 현재값만 제공)
- **API 의존**: TradingEconomics HTML 스크래핑이 실패하면 이전 값조차 복원할 수 없음. DB에 쌓아두면 캐시 장애 시 최소한의 복구 지점이 생김

## Proposed Solution

**ECOS 패턴 미러링 (브레인스토밍: `docs/brainstorms/2026-04-09-global-indicator-history-brainstorm.md`)**

- 3테이블 구조에 `country_name` 차원만 추가
- (country_name, indicator_type)별 `cycle`(=TradingEconomics `referenceText`) 변경 감지로 중복 최소화
- 별도 스케줄러로 ECOS 배치(07:00)와 시간대 분리 (07:30)
- 55개 지표를 순차 호출하며 요청 간 1.5초 `Thread.sleep` 딜레이
- 개별 지표 실패 시 로그 후 다음 지표로 계속 (부분 성공 허용)
- 사용자 API가 사용하는 `GlobalIndicatorCacheService` Caffeine 캐시는 건드리지 않음

## Technical Considerations

### 아키텍처 준수
- `economics` 도메인 내부에서만 추가/수정 (ARCHITECTURE.md 계층 규칙 유지)
- 포트-어댑터 유지: 기존 `GlobalIndicatorPort` / `TradingEconomicsIndicatorAdapter` 그대로 재사용
- 신규 repository는 domain에 port 인터페이스, infrastructure/persistence에 구현체
- `@Transactional`은 application 계층 (`GlobalIndicatorSaveService`)에서만 사용

### 테이블 생성 전략
- **중요**: 프로젝트에 Flyway 의존성이 **없음** (`build.gradle` 확인). `spring.jpa.hibernate.ddl-auto: update` 기반으로 JPA 엔티티 추가 시 테이블 자동 생성
- 기존 `src/main/resources/db/migration/*.sql`는 수동 실행 스크립트 (Flyway 아님)
- 메타데이터 초기 시드는 `GlobalIndicatorMetadataInitializer`가 앱 시작 시 YAML에서 적재 (ECOS와 동일)

### DB 방언
- **PostgreSQL** 사용 (`application.yml: org.hibernate.dialect.PostgreSQLDialect`, `application-dev.yml: jdbc:postgresql://...`)
- CLAUDE.md의 "MariaDB" 언급은 outdated. 실제 코드/설정은 PostgreSQL

### 로깅
- **중요**: 실제 코드베이스는 `@Slf4j` (Lombok + SLF4J)를 사용. CLAUDE.md의 `BigxLogger` 언급은 outdated. 기존 `EcosIndicatorBatchScheduler`, `EcosIndicatorSaveService` 패턴을 그대로 따라 `@Slf4j` 사용

### 기존 코드 재사용
| 컴포넌트 | 위치 | 재사용 용도 |
|---|---|---|
| `GlobalIndicatorPort.fetchByIndicator(type)` | `economics/domain/service/GlobalIndicatorPort.java:8` | 지표별 HTML 수집 entry point |
| `TradingEconomicsIndicatorAdapter` | `economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorAdapter.java:15` | Port 구현 (HTML → `List<CountryIndicatorSnapshot>`) |
| `CountryIndicatorSnapshot` | `economics/domain/model/CountryIndicatorSnapshot.java:10` | 수집 결과 도메인 모델 (`countryName`, `lastValue`, `previousValue`, `referenceText`) |
| `GlobalEconomicIndicatorType` (enum, 55개) | `economics/domain/model/GlobalEconomicIndicatorType.java:10` | 수집 대상 열거형 |
| `GlobalIndicatorCacheService` | `economics/application/GlobalIndicatorCacheService.java:15` | **건드리지 않음** (사용자 API 흐름 유지) |

### cycle 비교 전략
- `cycle` = `CountryIndicatorSnapshot.referenceText` 그대로 사용 (예: `"Dec 2025"`, `"Q3 2025"`, `"2024"`)
- 원본 문자열 비교만으로 충분 (파싱/정규화 금지 — 브레인스토밍 결정)
- 비교 키: `country_name + "::" + indicator_type.name()`
- `referenceText`가 `null`이거나 공백이면 해당 건 스킵 (ECOS `apiCycle == null` 규칙과 동일)

### 데이터 규모
- 55개 지표 × G20 기준 ~20개국 ≈ **약 1,100건/스냅샷**
- `global_indicator_latest`: 최대 ~1,100 rows 유지 (안정)
- `global_indicator` (history): cycle 변경 시에만 증가. 월/분기 지표가 많아 하루 약 수백 건 이하 INSERT 예상

### 성능 / 속도 제어
- 1.5초 × 55 = 약 82초 배치 소요
- Spring `task.scheduling.pool.size: 5` (이미 application.yml에 설정됨) — ECOS 배치와 동시에 실행되어도 블로킹 없음
- 07:30 실행이므로 ECOS(07:00) 끝난 뒤 시작되도록 분리

## System-Wide Impact

- **Interaction graph**: `@Scheduled GlobalIndicatorBatchScheduler.saveSnapshot()` → `GlobalIndicatorSaveService.fetchAndSave()` → 55회 루프: `GlobalIndicatorPort.fetchByIndicator(type)` → `TradingEconomicsIndicatorAdapter` → `TradingEconomicsHtmlClient.fetch(url)` → `TradingEconomicsTableParser` → `TradingEconomicsValueNormalizer` → 결과를 메모리상 `latestMap`과 비교 후 변경분만 `global_indicator` INSERT, `global_indicator_latest`는 전건 갱신. `GlobalIndicatorCacheService`는 호출 경로에 등장하지 않음 — 격리 보장
- **Error propagation**: 개별 지표의 `TradingEconomicsFetchException` / `TradingEconomicsParseException`을 `GlobalIndicatorSaveService` 루프 내부 try-catch로 포획하고 `log.error` 후 다음 지표로 진행. 실패 지점이 대부분 HTTP 호출(`fetchByIndicator`)이므로 DB 작업 이전에 실패가 결정됨 — 부분 성공이 자연스럽게 보장됨. 스케줄러의 최상위 try-catch는 ECOS 패턴과 동일
- **State lifecycle risks**: 한 지표가 `fetchByIndicator` 단계에서 실패하면 해당 지표만 history/latest 갱신에서 제외된다. 단일 트랜잭션 내부에서 처리되므로 한 지표가 DB 작업 중 예외를 던지면(드물게) 같은 트랜잭션에서 먼저 수행된 다른 지표의 작업까지 함께 롤백될 수 있다 — HTTP 실패가 압도적이라 실전에서는 거의 발생하지 않지만 설계상 위험으로 인지
- **API surface parity**: 사용자 REST API(`GlobalIndicatorController`)는 여전히 `GlobalIndicatorCacheService`를 호출하므로 추가 배치의 존재를 모름. 향후 DB에서 히스토리 조회 API가 필요하면 별도 엔드포인트로 추가

## Acceptance Criteria

### 기능 요구사항
- [ ] `global_indicator`, `global_indicator_latest`, `global_indicator_metadata` 3개 테이블이 JPA 엔티티로 생성되고 `ddl-auto: update`로 자동 반영된다
- [ ] `global_indicator` 복합 인덱스 3개(`(country_name, indicator_type)`, `(snapshot_date)`, `(country_name, indicator_type, cycle, snapshot_date DESC)`)가 `@Index`로 선언된다
- [ ] `global_indicator_latest`는 `(country_name, indicator_type)` 복합 PK를 사용한다
- [ ] `global_indicator_metadata`는 `indicator_type` 단일 PK를 사용한다 (ECOS와 달리 country 차원 없음)
- [ ] 배치가 매일 07:30에 실행되며 cron 설정이 `global.batch.cron` 프로퍼티로 외부화된다 (`@Scheduled(cron = "${global.batch.cron:0 30 7 * * *}")`)
- [ ] 55개 지표를 순차 호출하고, 각 호출 사이에 1.5초 딜레이가 적용된다
- [ ] 히스토리가 비어있으면 초기 시딩으로 수집된 전체 스냅샷을 `global_indicator`와 `global_indicator_latest`에 저장한다
- [ ] 이후 실행에서는 (country_name, indicator_type)별로 cycle(=referenceText)이 변경된 건만 `global_indicator`에 INSERT한다
- [ ] latest 테이블은 cycle 변경 시 기존 dataValue를 `previous_data_value`로 보존한 뒤 새 값으로 갱신한다
- [ ] 개별 지표 수집 실패 시 로그만 남기고 다음 지표로 진행한다 (배치 전체 롤백 금지)
- [ ] `referenceText`가 null 또는 공백인 수집 결과는 저장 대상에서 제외된다
- [ ] 앱 시작 시 `GlobalIndicatorMetadataInitializer`가 `global-indicator-metadata.yml`을 로드하여 `global_indicator_metadata` 테이블이 비어있으면 시드한다
- [ ] `GlobalIndicatorCacheService`와 `GlobalIndicatorController`는 수정되지 않는다 (사용자 API 흐름 격리)

### 품질 요구사항
- [ ] 코드는 기존 ECOS 패턴(`EcosIndicatorSaveService`, `EcosIndicatorBatchScheduler`)과 구조·네이밍이 일관된다
- [ ] Lombok(`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`) 사용, 수동 getter/setter 금지
- [ ] JPA Entity는 protected 기본 생성자 + `@PrePersist` 패턴 유지
- [ ] `EcosIndicatorPort` 등 기존 도메인 인터페이스 시그니처 변경 없음

## Implementation Plan

각 작업은 하나씩 순차 진행한다. CLAUDE.md 규칙에 따라 JPA Entity 생성은 사전 승인이 필요하므로, **본 plan 자체가 Entity 생성 승인 요청**에 해당한다.

### Phase 1 — 도메인 모델 + Repository 인터페이스

- [x] `economics/domain/model/GlobalIndicator.java` — 히스토리 도메인 모델 (필드: `id`, `countryName`, `indicatorType`, `dataValue`, `cycle`, `unit`, `snapshotDate`, `createdAt`). `fromSnapshot(CountryIndicatorSnapshot, LocalDate)` 정적 팩토리 제공
- [x] `economics/domain/model/GlobalIndicatorLatest.java` — 최신값 도메인 모델. `toCompareKey()`, `fromSnapshot()` 정적 팩토리 제공. `EcosIndicatorLatest`와 구조 일치
- [x] `economics/domain/model/GlobalIndicatorMetadata.java` — 메타데이터 도메인 모델 (`indicatorType`, `description`)
- [x] `economics/domain/repository/GlobalIndicatorRepository.java` — `saveAll(List<GlobalIndicator>)`, `existsAny()`
- [x] `economics/domain/repository/GlobalIndicatorLatestRepository.java` — `findAll()`, `saveAll(List<GlobalIndicatorLatest>)`
- [x] `economics/domain/repository/GlobalIndicatorMetadataRepository.java` — `findAll()`, `count()`, `saveAll(List<GlobalIndicatorMetadata>)`

### Phase 2 — JPA 엔티티 + Repository 구현체

- [x] `economics/infrastructure/persistence/GlobalIndicatorEntity.java`
  - `@Table(name = "global_indicator", indexes = { @Index("idx_global_country_indicator" → "country_name, indicator_type"), @Index("idx_global_snapshot_date" → "snapshot_date"), @Index("idx_global_group_snapshot" → "country_name, indicator_type, cycle, snapshot_date DESC") })`
  - `indicator_type` 는 `@Enumerated(EnumType.STRING)` + `GlobalEconomicIndicatorType` 사용
  - `@PrePersist` 로 `createdAt` 자동 설정
- [x] `economics/infrastructure/persistence/GlobalIndicatorLatestEntity.java`
  - 복합 PK: `@IdClass(GlobalIndicatorLatestEntity.LatestId.class)` — `(countryName, indicatorType)`
  - `update(newDataValue, newCycle, newUnit, cycleChanged, updatedAt)` 도메인 메서드로 previous_data_value + unit 보존 (`EcosIndicatorLatestEntity.update` 미러링 + unit 추가)
- [x] `economics/infrastructure/persistence/GlobalIndicatorMetadataEntity.java`
  - 단일 PK: `indicator_type` (`@Enumerated(EnumType.STRING)`)
- [x] `economics/infrastructure/persistence/GlobalIndicatorJpaRepository.java` (Spring Data JPA) — `existsFirstBy()` 노출
- [x] `economics/infrastructure/persistence/GlobalIndicatorLatestJpaRepository.java`
- [x] `economics/infrastructure/persistence/GlobalIndicatorMetadataJpaRepository.java`
- [x] `economics/infrastructure/persistence/mapper/GlobalIndicatorMapper.java`
- [x] `economics/infrastructure/persistence/mapper/GlobalIndicatorLatestMapper.java`
- [x] `economics/infrastructure/persistence/mapper/GlobalIndicatorMetadataMapper.java`
- [x] `economics/infrastructure/persistence/GlobalIndicatorRepositoryImpl.java`
- [x] `economics/infrastructure/persistence/GlobalIndicatorLatestRepositoryImpl.java` — `EcosIndicatorLatestRepositoryImpl.saveAll` 패턴 그대로 (기존 row 존재하면 `update`, 없으면 `save`)
- [x] `economics/infrastructure/persistence/GlobalIndicatorMetadataRepositoryImpl.java`

### Phase 3 — 배치 Save 서비스 + 스케줄러

- [x] `economics/application/GlobalIndicatorSaveService.java`
  - `@Service @Transactional @RequiredArgsConstructor @Slf4j` (클래스 레벨 단일 트랜잭션)
  - 의존성: `GlobalIndicatorPort`, `GlobalIndicatorRepository`, `GlobalIndicatorLatestRepository`
  - **트랜잭션 전략**: 단일 트랜잭션 + 지표별 try-catch. self-invocation 이슈 회피를 위해 `REQUIRES_NEW`나 별도 Writer 클래스를 사용하지 않는다. 실패 지점의 대부분이 HTTP 호출(`fetchByIndicator`)이고 이는 DB 작업 이전에 결정되므로, 단순 try-catch로도 부분 성공이 충분히 보장된다
  - `fetchAndSave()` 흐름:
    1. `historyExists = globalIndicatorRepository.existsAny()` (루프 진입 전 1회)
    2. `latestMap = globalIndicatorLatestRepository.findAll()` 을 `toCompareKey()` 기준 Map으로 변환 (루프 진입 전 1회)
    3. `GlobalEconomicIndicatorType.values()` 순회 루프:
       - 첫 지표 이후에는 `Thread.sleep(1500)` (`InterruptedException` 발생 시 `Thread.currentThread().interrupt()` 호출 후 루프 중단)
       - try 블록 안에서:
         - `globalIndicatorPort.fetchByIndicator(type)` → 수집 실패 시 catch로 이동
         - `referenceText`가 null/blank인 snapshot 필터링
         - `historyExists == false` 이면 초기 시딩 경로 (전체 snapshot → history + latest)
         - `historyExists == true` 이면 `latestMap` 참조하여 cycle 변경분만 history insert, latest는 전건 갱신 (previous_data_value 보존)
         - 저장된 레코드는 `latestMap`에 즉시 반영하여 같은 배치 내 일관성 유지
       - catch `Exception e`: `log.error("글로벌 지표 수집 실패: type={}", type, e);` 후 continue
    4. 루프 종료 후 총 저장 건수 반환
  - 구현 팁: 기존 `EcosIndicatorSaveService.java:42-193` 로직의 "초기 시딩 vs cycle 비교 분기" 구조를 참조하되, "API 1회 전체" 대신 "지표당 1회 호출 + 지표별 try-catch"로 변형. `latestMap`은 지표 타입 전체 키를 가지므로 지표별로 `stream().filter(indicatorType)` 적용
- [x] `economics/infrastructure/scheduler/GlobalIndicatorBatchScheduler.java`
  - `@Component @RequiredArgsConstructor @Slf4j`
  - `@Scheduled(cron = "${global.batch.cron:0 30 7 * * *}")` (매일 07:30)
  - try-catch로 배치 실패 시 로그만 남김 (ECOS와 동일)
  - `application.yml` / `application-dev.yml` 수정 없음 — ECOS와 동일하게 `@Scheduled` 어노테이션 기본값에만 의존 (로컬 배치 비활성화가 필요하면 추후 별도 작업)

### Phase 4 — 메타데이터 YAML + Initializer

- [x] `src/main/resources/global-indicator-metadata.yml` 신규 생성
  - 루트: `global.indicator.metadata.indicators: [...]`
  - 각 항목: `indicator-type` (enum name, 예: `INTEREST_RATE`), `description` (한글 설명)
  - 55개 지표 전체 등록. 초기 description은 `GlobalEconomicIndicatorType.displayName`을 그대로 사용하거나, 가능하면 설명을 수동 작성
- [x] `economics/infrastructure/global/tradingeconomics/config/GlobalIndicatorMetadataInitializer.java`
  - `ApplicationRunner` 구현 (`EcosIndicatorMetadataInitializer` 패턴 그대로)
  - `count() == 0` 이면 YAML 로드 후 `saveAll` 호출
  - `SnakeYaml` 사용 (`Yaml` 클래스) — 이미 ECOS에서 사용 중이라 별도 의존성 추가 불필요

### Phase 5 — 수동 점검

- [x] `./gradlew compileJava` 으로 컴파일 성공 확인
- [ ] 로컬 DB에서 앱 실행하여 3개 테이블이 생성되는지 확인 (hibernate DDL 로그)
- [ ] 메타데이터 시드가 앱 시작 시 주입되는지 확인
- [ ] `global.batch.cron` 을 임시로 단기 cron으로 변경하여 배치가 동작하는지 확인
- [ ] 두 번째 실행 시 cycle 동일한 지표가 history에 중복 INSERT되지 않는지 확인
- [ ] 사용자 API (`GET /api/global-indicators/...`) 가 여전히 캐시 경로로 응답하는지 확인

## Dependencies & Risks

### 의존성
- 신규 라이브러리 없음 (Lombok, Jsoup, SnakeYaml, Spring Data JPA 모두 기존에 포함)
- 기존 `TradingEconomicsIndicatorAdapter`가 정상 동작해야 함 (배치가 호출하는 유일한 외부 경로)

### 위험
- **중복 API 호출**: 배치와 사용자 캐시가 각각 TradingEconomics를 호출하므로 이론상 호출 횟수는 2배. 배치는 07:30에만 실행되고 사용자 캐시는 on-demand이므로 실제 부담은 낮지만, 장기적으로는 배치-캐시 통합 개선을 고려할 수 있음 (본 작업 범위 외)
- **HTML 구조 변경**: TradingEconomics 페이지 구조가 바뀌면 파싱 실패 → `TradingEconomicsParseException` 발생. 부분 성공 로직으로 격리되지만 운영 중 모니터링 필요 (본 작업 범위 외)
- **단일 트랜잭션 경계**: Phase 3 결정(단일 트랜잭션 + try-catch)에 따라, 드물게 HTTP 성공 후 DB 작업 중 예외가 발생하면 같은 트랜잭션의 이전 지표 작업까지 롤백될 수 있음. 실전에서는 HTTP 실패가 압도적이므로 발생 가능성이 낮지만 설계상 인지
- **CLAUDE.md "Entity 생성 사전 승인"**: 본 plan이 승인 요청 역할을 겸함. Phase 2의 JPA 엔티티 3종에 대한 사전 승인이 필요

## Out of Scope

- 글로벌 히스토리 조회 REST API (추후 별도 plan)
- 차트/대시보드 프론트엔드 (추후 별도 plan)
- TradingEconomics 캐시 갱신 통합 (본 배치가 캐시도 업데이트하도록 하는 개선)
- FRED / IMF 등 다른 글로벌 소스 연동
- 테스트 코드 작성 (CLAUDE.md: 명시적 요청 없으면 테스트 작성 안 함)
- 기존 ECOS 코드 수정
- `CLAUDE.md` / `ARCHITECTURE.md` 수정 (MariaDB→PostgreSQL, Java 17→21, BigxLogger→`@Slf4j` 등의 outdated 내용이 있으나 본 plan에서는 건드리지 않음)
- `application.yml` / `application-dev.yml` 수정 (ECOS와 동일하게 `@Scheduled` 기본값만 사용)

## Sources & References

### Origin
- **Brainstorm:** [docs/brainstorms/2026-04-09-global-indicator-history-brainstorm.md](../brainstorms/2026-04-09-global-indicator-history-brainstorm.md) — 핵심 결정: ECOS 3테이블 패턴 미러링, cycle 변경 감지, 별도 스케줄러 07:30, 1.5초 딜레이, 부분 성공 허용, Caffeine 캐시 유지

### Internal References
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/persistence/EcosIndicatorEntity.java:12-76` — history entity 참조
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/persistence/EcosIndicatorLatestEntity.java:13-97` — latest entity (IdClass, update 메서드) 참조
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/persistence/EcosIndicatorMetadataEntity.java:12-69` — metadata entity 참조
- `src/main/java/com/thlee/stock/market/stockmarket/economics/application/EcosIndicatorSaveService.java:29-193` — save 서비스 로직 (초기 시딩 + cycle 비교) 참조
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/scheduler/EcosIndicatorBatchScheduler.java:9-29` — 스케줄러 패턴
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataInitializer.java:22-66` — YAML 로더 패턴
- `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorAdapter.java:15-35` — HTML 수집 어댑터 (재사용)
- `src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/CountryIndicatorSnapshot.java:10-17` — 수집 결과 도메인 모델
- `src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/GlobalEconomicIndicatorType.java:10-60` — 55개 지표 enum
- `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java:15-37` — 건드리지 않을 기존 캐시 서비스
- `src/main/resources/ecos-indicator-metadata.yml` — YAML 포맷 참조
- `ARCHITECTURE.md` — 레이어 규칙 (presentation → application → domain ← infrastructure)
- `CLAUDE.md` — Entity 생성 사전 승인 규칙, YAGNI, 테스트 작성 규칙

### Design Documents (선행 패턴)
- `.claude/designs/economics/ecos-indicator-persistence/ecos-indicator-persistence.md` — ECOS 히스토리 구조 원본 설계
- `.claude/designs/economics/ecos-initial-seeding/ecos-initial-seeding.md` — 초기 시딩 전략
- `.claude/designs/economics/tradingeconomics-html-ingestion/tradingeconomics-html-ingestion.md` — 글로벌 HTML 수집 원본 설계