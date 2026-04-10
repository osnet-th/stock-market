---
date: 2026-04-09
topic: 글로벌 경제지표 히스토리 로그 적재
status: brainstormed
---

# 글로벌 경제지표 히스토리 로그 적재

## What We're Building

태형님이 국내 경제지표(ECOS)에서 이미 구현된 히스토리 로그 패턴을 글로벌 경제지표(TradingEconomics)에도 동일하게 적용하려고 합니다. 현재 글로벌 지표는 캐시 전용(Caffeine, TTL 24h)으로만 관리되고 있어 **시간에 따른 변화 추이를 추적할 수 없는 상태**입니다.

이 작업을 통해:
- 55개 글로벌 지표 × 전체 국가 조합의 변화 이력을 DB에 누적
- (국가, 지표)별 cycle 변경 감지로 중복 최소화
- 기존 ECOS 패턴과 일관된 구조로 유지보수 용이성 확보

## Why This Approach

**ECOS 패턴 미러링(방안 A)**을 선택한 이유:

1. **일관성**: 기존 `ecos_indicator`, `ecos_indicator_latest`, `ecos_indicator_metadata` 3테이블 패턴을 그대로 따르므로 팀 내 멘탈 모델 공유
2. **검증된 구조**: 이미 프로덕션에서 안정적으로 동작 중인 패턴
3. **독립성**: ECOS 코드를 건드리지 않고 독립적으로 추가 가능 (변경 범위 작음)
4. **확장성**: country_name 칼럼 추가만으로 글로벌 특성(국가 차원) 수용 가능

## Key Decisions

### 1. 대상 범위
- **전체 적재**: 55개 지표 × TradingEconomics가 제공하는 전체 국가
- latest 테이블 규모 예상: 약 2,000~2,750 rows (성능 이슈 없음)

### 2. 변경 감지 방식
- **(국가, 지표)별 cycle 변경 감지**
- `global_indicator_latest`의 cycle(= TradingEconomics의 Reference 필드)과 비교
- cycle이 변경된 건만 `global_indicator` history 테이블에 INSERT
- ECOS와 동일한 전략 (중복 최소화)

### 3. 테이블 구조 (3테이블 패턴 + country_name)

**`global_indicator` (history)**
| 칼럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | auto |
| country_name | VARCHAR(100) | 국가명 |
| indicator_type | VARCHAR(100) | GlobalEconomicIndicatorType enum |
| data_value | VARCHAR(50) | 값 |
| cycle | VARCHAR(50) | Reference 필드 |
| unit | VARCHAR(50) | 단위 |
| snapshot_date | DATE | 수집일 |
| created_at | DATETIME | auto |

인덱스 (ECOS와 동일 전략):
- `idx_global_country_indicator` on (country_name, indicator_type)
- `idx_global_snapshot_date` on (snapshot_date)
- `idx_global_group_snapshot` on (country_name, indicator_type, cycle, snapshot_date DESC)

**`global_indicator_latest`**
| 칼럼 | 타입 | 설명 |
|------|------|------|
| country_name | VARCHAR(100) PK | 국가명 |
| indicator_type | VARCHAR(100) PK | 지표 enum |
| data_value | VARCHAR(50) | 현재 값 |
| previous_data_value | VARCHAR(50) | 이전 값 (cycle 변경 시에만 갱신) |
| cycle | VARCHAR(50) | 현재 Reference |
| unit | VARCHAR(50) | 단위 |
| updated_at | DATETIME | 최종 갱신 시각 |

- 복합 PK: (country_name, indicator_type)
- cycle 비교용 최신값 테이블 (ECOS `ecos_indicator_latest`와 동일 패턴 + country_name)
- **previous_data_value는 이 테이블에만 존재** (ECOS 패턴 일치)

**`global_indicator_metadata`**
| 칼럼 | 타입 | 설명 |
|------|------|------|
| indicator_type | VARCHAR(100) PK | 지표 enum |
| description | VARCHAR(500) | 한글 설명 |

- ECOS metadata와 동일 패턴 (단, country는 지표별 공통 설명이므로 indicator_type 단일 PK)

### 4. 배치 스케줄러
- **별도 스케줄러**: `GlobalIndicatorBatchScheduler` 신규 구성
- ECOS와 독립 실행 (시간대 분리로 부하 분산)
- cron 설정은 별도 property로 관리 (`global.batch.cron`)

### 5. 속도 제어 (Rate Limiting)
- **요청 간 딜레이**: 1.5초
- 55개 지표 순차 호출 → 약 1분 30초 소요 예상
- TradingEconomics 차단 위험 최소화

### 6. 기존 코드 재사용
다음 기존 컴포넌트는 그대로 활용:
- `TradingEconomicsHtmlClient`
- `TradingEconomicsTableParser`
- `TradingEconomicsValueNormalizer`
- `TradingEconomicsIndicatorRegistry`
- `GlobalEconomicIndicatorType` enum (55개)
- `CountryIndicatorSnapshot` 도메인 모델

신규 구현 필요:
- JPA 엔티티 3종 (history/latest/metadata)
- Repository 인터페이스 + 구현체
- `GlobalIndicatorSaveService` (배치 저장 로직)
- `GlobalIndicatorBatchScheduler`
- Flyway 마이그레이션 SQL
- YAML 메타데이터 시드 파일 + Initializer

## Resolved Questions

브레인스토밍 과정에서 아래 사항이 모두 확정되었습니다:

1. **cycle 저장 방식** → **원본 문자열 그대로 저장**
   - TradingEconomics의 Reference 문자열("Dec 2025", "Q3 2025", "2024" 등)을 그대로 VARCHAR 칼럼에 저장
   - 변경 감지는 문자열 비교만으로 충분 (ECOS와 동일 전략)

2. **초기 시딩 전략** → **첫 실행 시 전체 스냅샷 저장**
   - history가 비어있으면 최초 1회 전체를 history + latest에 저장
   - 이후부터는 cycle 변경 감지 로직 적용

3. **metadata 테이블 생성 여부** → **ECOS와 동일하게 테이블 생성**
   - `global_indicator_metadata` 테이블 생성 + YAML 시드 파일
   - ECOS 패턴과 일관성 유지

4. **실패 처리 정책** → **부분 성공 허용**
   - 개별 지표 실패 시 로그만 남기고 다음 지표로 진행
   - 성공한 건만 history에 저장
   - 전체 롤백하지 않음

5. **country_name 정규화** → **원본 문자열만 저장**
   - TradingEconomicsValueNormalizer가 이미 정규화 수행 중
   - ISO country code 병행 저장하지 않음 (단순화)

6. **배치 실행 시간대** → **매일 07:30**
   - ECOS(07:00) 직후 실행
   - 사용자 출근 전 데이터 준비 완료
   - cron: `"0 30 7 * * *"`

7. **기존 캐시와의 관계** → **캡처만 DB에 적재**
   - 사용자 API는 기존 Caffeine 캐시 유지 (변경 없음)
   - 배치는 TradingEconomics에서 수집한 데이터를 DB에 쌓는 역할만 수행
   - 변경 범위 최소화

## Next Steps

모든 핵심 결정이 확정되었으므로 `/ce:plan` 단계로 진행할 수 있습니다. 상세 구현 계획에서 다룰 항목:
- Flyway 마이그레이션 SQL 작성 (3개 테이블 + 인덱스)
- JPA 엔티티/리포지토리 구현
- `GlobalIndicatorSaveService` 배치 로직
- `GlobalIndicatorBatchScheduler` 구성
- YAML 메타데이터 시드 파일 + Initializer