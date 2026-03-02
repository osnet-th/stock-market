# ECOS 경제지표 히스토리 저장 설계

- 작성일: 2026-02-22

---

## 작업 리스트

- [ ] `KeyStatIndicator` 수정 (toCompareKey() 추가)
- [ ] `EcosKeyStatResult` 수정 (validIndicators() 추가)
- [ ] `EcosIndicator` 도메인 모델 구현 (히스토리 저장용)
- [ ] `EcosIndicatorRepository` 도메인 레포지토리 인터페이스 구현
- [ ] `EcosIndicatorEntity` JPA Entity 구현
- [ ] `EcosIndicatorJpaRepository` 구현
- [ ] `EcosIndicatorRepositoryImpl` 구현 (어댑터)
- [ ] `EcosIndicatorMapper` 구현 (Entity <-> Domain 변환)
- [ ] `EcosIndicatorLatest` 도메인 모델 구현 (최신값 비교용)
- [ ] `EcosIndicatorLatestRepository` 도메인 레포지토리 인터페이스 구현
- [ ] `EcosIndicatorLatestEntity` JPA Entity 구현
- [ ] `EcosIndicatorLatestJpaRepository` 구현
- [ ] `EcosIndicatorLatestRepositoryImpl` 구현 (어댑터)
- [ ] `EcosIndicatorLatestMapper` 구현 (Entity <-> Domain 변환)
- [ ] `EcosIndicatorSaveService` 구현 (application 계층, 캐시 적재 + DB 저장 통합)
- [ ] `EcosIndicatorBatchScheduler` 구현 (배치 스케줄러)
- [ ] `EcosIndicatorService` 수정 (캐시 확인 → fallback API 조회)
- [ ] `EcosCacheConfig` 수정 (TTL 25h)
- [ ] `EcosIndicatorWarmupListener` 구현 (앱 시작 시 warm-up)

---

## 배경

현재 ECOS 100대 경제지표는 API 호출 → Caffeine 캐시(24h) 구조로만 제공. 지표 값의 변화를 추적할 수 없고, API 장애 시 데이터 제공 불가. 날짜별 히스토리를 DB에 쌓아 시계열 추적 및 안정적 데이터 제공 필요.

---

## 핵심 결정

### 캐시 + DB 저장 통합 구조

기존 구조는 조회(EcosIndicatorService)와 저장(EcosIndicatorSaveService)이 각각 별도로 API를 호출하여 중복 발생. 이를 단일 흐름으로 통합.

**기존 구조 (문제)**:
```
조회 요청 → EcosIndicatorService → @Cacheable → API 호출 (카테고리별 miss 시마다 호출, 최악 15회)
배치(1일) → EcosIndicatorSaveService → API 호출 → DB 저장
```

**변경 구조**:
```
앱 시작 (Event) → SaveService.fetchAndSave() → API 1회 조회 → 15개 카테고리별 캐시 적재 + DB 저장
배치 (1일 1회) → SaveService.fetchAndSave() → API 1회 조회 → 15개 카테고리별 캐시 갱신 + DB 저장
조회 요청     → IndicatorService → 캐시 hit → 반환
                                 → 캐시 miss → API 직접 조회 (배치 실패 fallback)
```

### 2-테이블 구조: 히스토리 + 최신값

| 테이블 | 역할 | 데이터량 |
|---|---|---|
| `ecos_indicator` | 히스토리 (변경 이력 누적) | 계속 증가 |
| `ecos_indicator_latest` | 최신 스냅샷 (현재 값) | 고정 101건 |

### 변경 감지 최적화 (N+1 제거)

**기존 설계 (문제)**: 지표별 `findLatestDataValue()` × 101회 = N+1 쿼리

**변경**: `ecos_indicator_latest` 테이블 전체 1회 조회 → Java Map으로 변환 → 메모리에서 비교

```
1. ecos_indicator_latest 전체 조회 (1회, 101건) → Map<className+keystatName, cycle>
2. API 호출 (1회, 101건)
3. Java에서 Map 비교 (cycle 값 = 통계 기준 시점) → 변경분 추출
4. 변경분 → ecos_indicator에 INSERT (히스토리)
5. ecos_indicator_latest 전체 갱신 (UPSERT)
```

### 변경 감지 기반 히스토리 저장

- 지표별 갱신 주기가 다름 (일/월/분기) → 매일 무조건 저장하면 중복 데이터 발생
- **저장 로직**: latest 테이블의 `cycle`(통계 기준 시점)과 API 값 비교 → 시점이 다를 때만 히스토리 INSERT, 같으면 스킵
- `cycle`은 통계 기준 시점 (예: `20260220`, `202512`, `2025Q3`, `2024`)
- **cycle null 방어**: API 응답의 `cycle`이 null인 지표는 데이터 미제공 상태로 판단하여 히스토리 저장 스킵
- `snapshot_date`는 값이 변경된 시점의 수집일

---

## 기존 코드 영향도 분석

### 수정 대상 파일

| 파일 | 위치 | 변경 내용 |
|---|---|---|
| `EcosIndicatorService` | `economics/application/EcosIndicatorService.java` | `@Cacheable` 제거 → CacheManager 직접 조회 + 캐시 miss 시 API fallback |
| `EcosCacheConfig` | `economics/infrastructure/korea/ecos/config/EcosCacheConfig.java` | TTL 24h → 25h (배치 주기 안전망) |

### 신규 파일

| 파일 | 위치 | 설명 |
|---|---|---|
| `EcosIndicatorWarmupListener` | `economics/infrastructure/scheduler/EcosIndicatorWarmupListener.java` | `ApplicationReadyEvent` 리스너, 앱 시작 시 `SaveService.fetchAndSave()` 호출 |
| `EcosIndicatorLatest` | `economics/domain/model/EcosIndicatorLatest.java` | 최신값 비교용 도메인 모델 |
| `EcosIndicatorLatestRepository` | `economics/domain/repository/EcosIndicatorLatestRepository.java` | 최신값 레포지토리 인터페이스 |
| `EcosIndicatorLatestEntity` | `economics/infrastructure/persistence/EcosIndicatorLatestEntity.java` | 최신값 JPA Entity |
| `EcosIndicatorLatestJpaRepository` | `economics/infrastructure/persistence/EcosIndicatorLatestJpaRepository.java` | Spring Data JPA |
| `EcosIndicatorLatestRepositoryImpl` | `economics/infrastructure/persistence/EcosIndicatorLatestRepositoryImpl.java` | 어댑터 |
| `EcosIndicatorLatestMapper` | `economics/infrastructure/persistence/mapper/EcosIndicatorLatestMapper.java` | Entity <-> Domain 변환 |

### 영향 없는 파일 (변경 불필요)

| 파일 | 이유 |
|---|---|
| `EcosIndicatorController` | `EcosIndicatorService.getIndicatorsByCategory()` 시그니처 동일, 변경 없음 |
| `EcosIndicatorPort` | 인터페이스 변경 없음 |
| `EcosIndicatorAdapter` | Port 구현체 변경 없음 |
| `EcosIndicatorCategory` | Enum 변경 없음 |
| `KeyStatIndicator` | 도메인 모델 변경 없음 |
| `IndicatorResponse` / `CategoryResponse` | DTO 변경 없음 |

---

### EcosIndicatorService 수정 상세

현재 코드:
```java
@Cacheable(
    cacheManager = "ecosCacheManager",
    cacheNames = EcosCacheConfig.ECOS_INDICATOR_CACHE,
    key = "#category.name()"
)
public List<KeyStatIndicator> getIndicatorsByCategory(EcosIndicatorCategory category) {
    EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics(); // 매번 전체 API 호출
    return result.indicators().stream()
        .filter(indicator -> category.contains(indicator.className()))
        .toList();
}
```

**문제점**:
- 카테고리별 캐시 miss 시마다 ECOS API 전체 조회 호출 (최악 15회)
- `@Cacheable`이 메서드 단위로 동작하여, 1회 API 호출 결과를 여러 캐시 키에 분배 불가

**변경 방향**:
- `@Cacheable` 제거, `CacheManager`를 직접 주입받아 캐시 조회
- 캐시 hit → 즉시 반환
- 캐시 miss → API 1회 호출 → 해당 카테고리만 필터링하여 반환 (배치 실패 fallback)
- 캐시 적재는 `SaveService.fetchAndSave()`가 담당 (warm-up / 배치)

[예시 코드](examples/EcosIndicatorService-modified-example.md)

### EcosCacheConfig 수정 상세

현재 코드:
```java
cacheManager.setCaffeine(Caffeine.newBuilder()
    .expireAfterWrite(24, TimeUnit.HOURS)
    .maximumSize(15));
```

**변경**: `expireAfterWrite(25, TimeUnit.HOURS)` — 배치 주기(24h)보다 약간 길게 설정하여 배치 갱신 전 캐시 만료 방지

[예시 코드](examples/EcosCacheConfig-modified-example.md)

---

## 테이블 구조

### ecos_indicator (히스토리)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT (PK, AUTO) | 자동 증가 ID |
| class_name | VARCHAR(100) | ECOS CLASS_NAME (예: 시장금리) |
| keystat_name | VARCHAR(200) | 지표명 (예: CD유통수익률(91일)) |
| data_value | VARCHAR(50) | 지표 값 |
| cycle | VARCHAR(20) | 통계 기준 시점 (예: 20260220, 202512, 2025Q3) |
| unit_name | VARCHAR(50) | 단위 (%, 원, 억원 등) |
| snapshot_date | DATE | 데이터 수집 기준일 |
| created_at | DATETIME | 레코드 생성 시각 |

**인덱스**: `(class_name, keystat_name)`, `(snapshot_date)`

### ecos_indicator_latest (최신값, 변경 감지 비교용)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| class_name | VARCHAR(100) (PK) | ECOS CLASS_NAME |
| keystat_name | VARCHAR(200) (PK) | 지표명 |
| cycle | VARCHAR(20) | 최신 통계 기준 시점 (변경 감지 비교용) |
| updated_at | DATETIME | 마지막 갱신 시각 |

**복합 PK**: `(class_name, keystat_name)` — UPSERT 기준

---

## 구현

### KeyStatIndicator 수정

위치: `economics/domain/model/KeyStatIndicator.java`

- `toCompareKey()` 추가 — `className + "::" + keystatName` 비교 키 생성

[예시 코드](examples/KeyStatIndicator-modified-example.md)

### EcosKeyStatResult 수정

위치: `economics/domain/model/EcosKeyStatResult.java`

- `validIndicators()` 추가 — 유효 카테고리 매핑 지표만 필터링

[예시 코드](examples/EcosKeyStatResult-modified-example.md)

### EcosIndicator 도메인 모델

위치: `economics/domain/model/EcosIndicator.java`

- 기존 `KeyStatIndicator`와 별도로 히스토리 저장용 도메인 모델
- `id`, `snapshotDate`, `createdAt` 필드 추가

[예시 코드](examples/EcosIndicator-example.md)

### EcosIndicatorRepository

위치: `economics/domain/repository/EcosIndicatorRepository.java`

- `findLatestDataValue()` 제거 (latest 테이블로 대체)

[예시 코드](examples/EcosIndicatorRepository-example.md)

### EcosIndicatorEntity

위치: `economics/infrastructure/persistence/EcosIndicatorEntity.java`

[예시 코드](examples/EcosIndicatorEntity-example.md)

### EcosIndicatorJpaRepository

위치: `economics/infrastructure/persistence/EcosIndicatorJpaRepository.java`

- `findLatestDataValue()` 제거 (latest 테이블로 대체)

[예시 코드](examples/EcosIndicatorJpaRepository-example.md)

### EcosIndicatorRepositoryImpl

위치: `economics/infrastructure/persistence/EcosIndicatorRepositoryImpl.java`

[예시 코드](examples/EcosIndicatorRepositoryImpl-example.md)

### EcosIndicatorMapper

위치: `economics/infrastructure/persistence/mapper/EcosIndicatorMapper.java`

[예시 코드](examples/EcosIndicatorMapper-example.md)

### EcosIndicatorLatest 도메인 모델

위치: `economics/domain/model/EcosIndicatorLatest.java`

- 101건 고정, 최신 `cycle`(통계 기준 시점) 비교 전용 도메인 모델
- `className + keystatName`으로 식별

[예시 코드](examples/EcosIndicatorLatest-example.md)

### EcosIndicatorLatestRepository

위치: `economics/domain/repository/EcosIndicatorLatestRepository.java`

[예시 코드](examples/EcosIndicatorLatestRepository-example.md)

### EcosIndicatorLatestEntity

위치: `economics/infrastructure/persistence/EcosIndicatorLatestEntity.java`

[예시 코드](examples/EcosIndicatorLatestEntity-example.md)

### EcosIndicatorLatestJpaRepository

위치: `economics/infrastructure/persistence/EcosIndicatorLatestJpaRepository.java`

[예시 코드](examples/EcosIndicatorLatestJpaRepository-example.md)

### EcosIndicatorLatestRepositoryImpl

위치: `economics/infrastructure/persistence/EcosIndicatorLatestRepositoryImpl.java`

[예시 코드](examples/EcosIndicatorLatestRepositoryImpl-example.md)

### EcosIndicatorLatestMapper

위치: `economics/infrastructure/persistence/mapper/EcosIndicatorLatestMapper.java`

[예시 코드](examples/EcosIndicatorLatestMapper-example.md)

### EcosIndicatorSaveService

위치: `economics/application/EcosIndicatorSaveService.java`

- API 1회 조회 → latest 전체 조회 (1회) → Java 비교 → 변경분 히스토리 INSERT + 캐시 적재 + latest UPSERT

[예시 코드](examples/EcosIndicatorSaveService-example.md)

### EcosIndicatorBatchScheduler

위치: `economics/infrastructure/scheduler/EcosIndicatorBatchScheduler.java`

- 매일 1회 실행 (cron 설정)
- `EcosIndicatorSaveService.fetchAndSave()` 호출

[예시 코드](examples/EcosIndicatorBatchScheduler-example.md)

### EcosIndicatorWarmupListener

위치: `economics/infrastructure/scheduler/EcosIndicatorWarmupListener.java`

- `ApplicationReadyEvent` 리스닝, 앱 시작 완료 후 `SaveService.fetchAndSave()` 호출
- 앱 시작 시 캐시 warm-up + 초기 DB 저장

[예시 코드](examples/EcosIndicatorWarmupListener-example.md)

---

## 주의사항

- `KeyStatIndicator`(기존)와 `EcosIndicator`(신규)는 목적이 다름: 전자는 API 응답용, 후자는 히스토리 저장용
- `EcosIndicatorCategory.fromClassName()`으로 카테고리 매핑 시, 매핑되지 않는 className은 로그 후 스킵
- 배치 저장 시 `saveAll` 배치 처리 (batch_size=1000 설정 활용)
- warm-up 실패 시 앱 기동은 정상 진행 (캐시 miss 시 fallback으로 대응)
- `ecos_indicator_latest`는 첫 배치 실행 시 101건 INSERT, 이후 UPSERT로 갱신