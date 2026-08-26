---
title: 글로벌 경제지표 히스토리 적재 보강
type: fix
status: active
date: 2026-08-02
issue: https://github.com/osnet-th/stock-market/issues/51
origin: docs/brainstorms/2026-08-02-global-indicator-history-hardening-brainstorm.md
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md
---

# 글로벌 경제지표 히스토리 적재 보강 (#51)

## Overview

#96(스케줄러 정지) 해결 후 남은 글로벌 히스토리 파이프라인 결함 4건을 일괄 보강한다: 지표 단위 독립 트랜잭션(부분 성공 실제 보장), 그래프 시간순 정렬, 조건부 catch-up(+수집 시각 컬럼), 1포인트 표시.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `application/GlobalIndicatorSaveService.java` | 클래스 @Transactional 제거, 수집(트랜잭션 밖)→writer 위임, 성공/실패 카운트 요약 로그 |
| `application/GlobalIndicatorSnapshotWriter.java` | **신규** — 지표 단위 @Transactional 저장 (initialSeed/saveChanged/isCycleChanged 이동) |
| `infrastructure/scheduler/GlobalIndicatorWarmupListener.java` | **신규** — 시작 시 조건부 catch-up (20h, 전용 daemon 스레드, LoggingContext) |
| `domain/model/GlobalIndicatorLatest.java` | `lastCollectedAt` 필드 추가 (fromSnapshot 에서 now 스탬프) |
| `infrastructure/persistence/GlobalIndicatorLatestEntity.java` | `last_collected_at` nullable 컬럼 + 생성자/update 파라미터 (**Entity 수정 — 게이트 항목**) |
| `infrastructure/persistence/mapper/GlobalIndicatorLatestMapper.java` | 필드 매핑 추가 |
| `domain/repository/GlobalIndicatorLatestRepository.java` + `Impl` + `JpaRepository` | `findMaxLastCollectedAt()` 추가, saveAll 에서 스탬프 반영 |
| `infrastructure/persistence/GlobalIndicatorJpaRepository.java` | 히스토리 조회 `ORDER BY cycle`(사전순) → `snapshot_date, id`(시간순) |
| `static/js/components/global.js` | 1포인트 렌더링 허용 + 단일 포인트 시 pointRadius 3 |
| `static/partials/global.html` | 차트/데이터 부족 분기 2포인트 → 1포인트 기준 |

공개 API 시그니처·응답 형식 변경 없음 (히스토리 응답 구조 동일, 정렬 순서만 시간순으로 교정).

## Implementation Steps

- [x] `GlobalIndicatorLatest` 도메인에 `lastCollectedAt` 추가, `fromSnapshot` now 스탬프
- [x] `GlobalIndicatorLatestEntity` nullable `last_collected_at` 컬럼 + update 반영 (ddl-auto=update 로 자동 생성)
- [x] 매퍼·RepositoryImpl.saveAll·도메인 리포지토리·JpaRepository(`select max`) 반영
- [x] `GlobalIndicatorSnapshotWriter` 신규 — 지표 단위 @Transactional, 기존 저장 로직 무변경 이동
- [x] `GlobalIndicatorSaveService` 재구성 — @Transactional 제거, fetch→writer, 요약 로그
- [x] `GlobalIndicatorWarmupListener` 신규 — max(last_collected_at) null 또는 20h+ 경과 시에만 catch-up, 전용 daemon 스레드
- [x] 히스토리 조회 정렬 수정 (`snapshot_date, id`)
- [x] 프론트 1포인트 표시 (global.js / global.html)

## Technical Considerations

- **트랜잭션 경계**: 자기 호출 프록시 우회를 피하려 별도 빈으로 분리. HTTP 수집이 트랜잭션 밖으로 나가면서 DB 커넥션 점유도 지표당 저장 순간으로 최소화 (#96 계열 위험 추가 감소)
- **historyExists 1회 판정 유지**: 지표 단위 커밋이어도 시딩 런에서 의미 동일 (각 지표는 자기 데이터만 시딩)
- **latestMap 일관성**: writer 가 커밋 성공 경로에서만 latestMap 갱신 — 실패 지표는 다음 실행에서 재시도됨
- **정렬 근거**: cycle 은 국가별로 겹치지 않고 순차 등장 → rn=1 대표 행의 snapshot_date 오름차순이 시간순과 일치. 동일 날짜 다중 cycle(이론상)만 id 로 안정화
- **catch-up 임계 20h**: 하루 1회 배치(24h 간격)보다 짧고, 같은 날 재배포(수 시간 간격)보다 길어 중복 수집 방지
- **Entity 수정**: nullable 컬럼 추가만 — 기존 행·코드 경로와 완전 호환, 데이터 마이그레이션 불필요

## Validation

- `./gradlew compileJava` + `node --check global.js`
- 지표 단위 트랜잭션·catch-up 은 운영 환경 재현 불가 — 코드 리뷰 검증, 배포 후 로그("catch-up 시작/불필요", 요약 로그)로 확인
- 정렬·1포인트는 배포 후 그래프 탭 육안 확인 (시딩 상태에서 점 표시, 데이터 누적 후 시간순)

## Risks

- Entity 컬럼 추가는 ddl-auto=update 환경에서 자동 반영 — 실패 시 앱 기동 로그로 즉시 드러남
- catch-up 과 정규 배치 동시 실행 창(시작 직후 배치 시각) — cycle dedup 조회가 흡수, 무해

## Out of Scope

- 놓친 중간 cycle 소급, ECOS 동일 패턴 적용, 수집 주기 변경
