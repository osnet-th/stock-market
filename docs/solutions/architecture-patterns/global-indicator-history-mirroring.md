---
title: ECOS 히스토리 패턴을 글로벌 지표로 미러링하기
category: architecture-patterns
date: 2026-04-10
tags: [economics, tradingeconomics, history, batch, cycle-detection, jpa, scheduler]
modules: [economics]
complexity: medium
---

# ECOS 히스토리 패턴을 글로벌 지표로 미러링하기

## 문제

글로벌 경제지표(TradingEconomics)가 Caffeine 캐시 전용으로만 운영되어, (국가, 지표)별 값의 시간 추이를 추적할 수 없었다. 국내(ECOS)는 이미 3테이블 히스토리 패턴(`ecos_indicator`, `ecos_indicator_latest`, `ecos_indicator_metadata`)으로 운영 중이었으므로, 동일 패턴을 글로벌에 적용하되 `country_name` 차원을 추가해야 했다.

## 핵심 결정과 근거

### 1. 3테이블 패턴 미러링 + country_name 추가

ECOS의 검증된 구조를 그대로 따름. `global_indicator`(history), `global_indicator_latest`(변경 감지), `global_indicator_metadata`(설명). 글로벌은 국가 차원이 필요하므로 latest의 복합 PK가 `(country_name, indicator_type)`로 확장됨.

### 2. cycle 변경 감지 (referenceText 문자열 비교)

TradingEconomics의 `referenceText`("Dec 2025", "Q3 2025" 등)를 원본 문자열 그대로 cycle 칼럼에 저장. 파싱/정규화 없이 문자열 비교만으로 변경 감지 충분. ECOS도 동일 전략.

### 3. 단일 트랜잭션 + 지표별 try-catch

`@Transactional(propagation = REQUIRES_NEW)` + self-invocation 시 Spring AOP 프록시를 타지 않아 적용 안 되는 문제 발견 → 단일 트랜잭션으로 단순화. 실패 지점이 대부분 HTTP 호출(`fetchByIndicator`)이므로 DB 작업 전에 결정됨 → 단순 try-catch로도 부분 성공 자연 보장.

### 4. latestMap 루프 전 1회 조회 + 즉시 갱신

ECOS의 N+1 제거 패턴 따름: `findAll()` 1회 → `Map<compareKey, Latest>` 변환 → 메모리에서 비교. 각 지표 처리 후 Map에 즉시 반영하여 배치 내 일관성 유지.

## 구현 구조 (22개 신규 파일)

```
economics/
├── domain/model/
│   ├── GlobalIndicator.java              # history 도메인 모델 (fromSnapshot 팩토리)
│   ├── GlobalIndicatorLatest.java        # latest 도메인 모델 (toCompareKey, fromSnapshot)
│   └── GlobalIndicatorMetadata.java      # metadata 도메인 모델
├── domain/repository/
│   ├── GlobalIndicatorRepository.java    # saveAll, existsAny
│   ├── GlobalIndicatorLatestRepository.java  # findAll, saveAll
│   └── GlobalIndicatorMetadataRepository.java
├── application/
│   └── GlobalIndicatorSaveService.java   # 핵심 배치 로직
├── infrastructure/
│   ├── persistence/
│   │   ├── GlobalIndicatorEntity.java        # @Index 3종, @PrePersist
│   │   ├── GlobalIndicatorLatestEntity.java  # @IdClass 복합PK, update()
│   │   ├── GlobalIndicatorMetadataEntity.java # 단일PK (enum)
│   │   ├── Global*JpaRepository.java         # Spring Data JPA 3종
│   │   ├── Global*RepositoryImpl.java        # 어댑터 3종
│   │   └── mapper/Global*Mapper.java         # Entity↔Domain 3종
│   ├── scheduler/
│   │   └── GlobalIndicatorBatchScheduler.java  # @Scheduled 07:30
│   └── global/tradingeconomics/config/
│       └── GlobalIndicatorMetadataInitializer.java  # YAML 시드
└── (resources)
    └── global-indicator-metadata.yml     # 31개 지표 description
```

## 재사용한 기존 컴포넌트 (수정 없음)

- `GlobalIndicatorPort` / `TradingEconomicsIndicatorAdapter` — HTML 수집 포트-어댑터
- `CountryIndicatorSnapshot` — 수집 결과 도메인 모델
- `GlobalEconomicIndicatorType` enum (31개 지표)
- `GlobalIndicatorCacheService` — 사용자 API 캐시 흐름 (건드리지 않음)

## 주의사항

1. **self-invocation + @Transactional**: 같은 클래스 내부 메서드 호출은 Spring AOP 프록시를 타지 않음. `REQUIRES_NEW`가 필요하면 반드시 별도 @Component로 분리해야 함
2. **Flyway 없음**: 이 프로젝트는 `ddl-auto: update`로 JPA 엔티티에서 테이블 자동 생성. Flyway 마이그레이션 불필요
3. **PostgreSQL**: CLAUDE.md에 MariaDB로 명시되어 있으나 실제는 PostgreSQL. 네이티브 SQL 작성 시 PostgreSQL 문법 사용
4. **@Slf4j**: CLAUDE.md의 BigxLogger 규칙은 outdated. 실제 코드베이스는 Lombok `@Slf4j` 사용
5. **복합 PK에 enum 사용 시**: `@IdClass` 내부에서 enum 타입 필드의 `equals`는 `==` 비교로 충분 (Java enum은 싱글턴)
6. **초기 시딩 분기**: `existsAny()`를 루프 진입 전 1회만 확인. 루프 안에서 호출하면 첫 지표 저장 후 결과가 바뀌어 의도치 않은 동작 발생
7. **latest UPSERT 패턴**: `findById` → 존재하면 Entity의 `update()` 호출 (dirty checking으로 자동 flush), 없으면 `save()`. 이 패턴은 `EcosIndicatorLatestRepositoryImpl`에서 검증됨

## 관련 문서

- **브레인스토밍**: `docs/brainstorms/2026-04-09-global-indicator-history-brainstorm.md`
- **구현 계획**: `docs/plans/2026-04-10-001-feat-global-indicator-history-plan.md`
- **ECOS 히스토리 설계**: `.claude/designs/economics/ecos-indicator-persistence/ecos-indicator-persistence.md`
- **ECOS 초기 시딩 설계**: `.claude/designs/economics/ecos-initial-seeding/ecos-initial-seeding.md`
- **TradingEconomics HTML 수집**: `.claude/designs/economics/tradingeconomics-html-ingestion/tradingeconomics-html-ingestion.md`
- **시계열 차트 중복 제거**: `docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md`