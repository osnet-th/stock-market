---
title: ES 클라이언트 행으로 인한 스케줄러 풀 고갈 수정
type: fix
status: active
date: 2026-07-26
issue: https://github.com/osnet-th/stock-market/issues/96
origin: docs/brainstorms/2026-07-26-scheduler-pool-es-hang-brainstorm.md
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md
---

# ES 클라이언트 행으로 인한 스케줄러 풀 고갈 수정 (#96)

## Overview

ES 클라이언트의 I/O reactor 사망 시 `BasicFuture.get()` 무기한 대기가 공용 스케줄러 풀(5스레드)을 잠식해 cron 배치 11개 전체가 정지하는 문제를 수정한다. 핵심은 로그 flush의 **실행 격리**(전용 단일 스레드)와 **버퍼 드롭 정책**이며, 같은 계열의 무한 대기 위험(KIS/DART HTTP 타임아웃 미설정)을 함께 제거한다.

원인 진단·근거는 brainstorm 문서 참조. reactor 사망 자체의 1차 원인 수정과 ES 클라이언트 자가 복구는 범위 밖(Open Question).

## 변경 파일

| 파일 | 변경 |
|---|---|
| `logging/infrastructure/elasticsearch/LogBatchBuffer.java` | flush 실행을 전용 단일 스레드 executor로 격리 + 버퍼 하드캡 드롭 정책 + shutdown drain 유한 대기 |
| `stock/infrastructure/stock/kis/KisMasterFileClient.java` | ZIP 다운로드 HttpClient connect/request 타임아웃 부여 |
| `stock/infrastructure/stock/dart/config/DartRestClientConfig.java` | connect/read 타임아웃 부여 |
| `src/main/resources/application.yml` | `spring.task.scheduling.pool.size` 5 → 10 |

백엔드 API·Entity·DB 변경 없음. 로그 적재 의미(best-effort at-most-once, 5초/500건/5MB 트리거, 월별 인덱스 그룹핑)는 불변.

## Implementation Steps

### 1. LogBatchBuffer flush 격리 (핵심)

- [ ] `@Scheduled(fixedDelay=5s) periodicFlush` 제거 → `@PostConstruct`에서 전용 `ScheduledExecutorService`(단일 daemon 스레드, 스레드명 `log-flush`) 생성 후 같은 주기로 자체 스케줄
- [ ] `enqueue()`의 임계 초과 시 호출자 스레드 직접 `flush()` 제거 → 전용 executor에 flush 작업 제출 (중복 제출 방지 플래그 — 이미 제출/실행 중이면 skip)
- [ ] 버퍼 하드캡 도입: flush 스레드가 행 상태로 버퍼가 하드캡(기존 임계의 2배: 1,000건 또는 10MB) 초과 시 신규 로그 드롭 + `logIngestionDroppedCounter` 계수 + 최초 1회 WARN (반복 WARN 방지)
- [ ] `drainOnShutdown`(@PreDestroy): 전용 executor에 drain 제출 후 기존 `SHUTDOWN_DRAIN_TIMEOUT`(10s) 내 유한 대기 → 타임아웃 시 `shutdownNow()` + 유실 WARN (flush 스레드 행 시 앱 종료가 지연되지 않도록)
- [ ] ES 정상 상태에서 기존과 동등 동작 확인 (주기·트리거·인덱스 그룹핑·카운터)

### 2. 스케줄 작업 무한 대기 위생

- [ ] `KisMasterFileClient.downloadZip`: `HttpClient.newBuilder().connectTimeout(10s)` + `HttpRequest.newBuilder().timeout(120s)` (ZIP 대용량 감안)
- [ ] `DartRestClientConfig`: `HttpClient.newBuilder().connectTimeout(3s)` + `JdkClientHttpRequestFactory.setReadTimeout(15s)` (SEC 설정과 동일 수준)

### 3. 스케줄러 풀 상향 (보조)

- [ ] `application.yml`: `spring.task.scheduling.pool.size: 10`

## Technical Considerations

- **동시성**: 기존 `synchronized(lock)` 버퍼 보호는 유지. 변경되는 것은 "누가 flush를 실행하는가"뿐 — 전용 스레드 1개로 고정되므로 flush 동시 실행 없음.
- **전용 스레드 행 시 동작**: 주기 flush 정지 → 버퍼가 하드캡까지 참 → 신규 드롭. reactor 회복 시 자연 재개. 비즈니스 스케줄러·요청 스레드는 어떤 경우에도 영향 없음 (이 격리가 본 수정의 목적).
- **@Scheduled 제거 이유**: 공용 `ThreadPoolTaskScheduler`를 경유하는 한 풀 잠식 경로가 남는다. 로깅 인프라는 자체 스레드를 소유하는 것이 계층상도 자연스러움 (`LogAsyncConfig`가 이미 로깅 전용 async executor를 소유하는 패턴과 일관).
- **KIS 타임아웃 값**: 마스터파일 ZIP은 수 MB — request 전체 타임아웃 120s는 정상 다운로드에 여유, 행 방지에 충분.
- **리팩토링 금지 원칙**: KisMasterFileClient의 per-call 클라이언트 생성 구조 등 기존 구조는 유지, 타임아웃만 부여.

## Validation

- `./gradlew compileJava` 통과
- LogBatchBuffer 동작 확인은 로컬 기동 + ES 컨테이너로 수동 확인 가능 범위에서 수행, 불가 항목은 validation 문서에 미검증으로 명시
- reactor 사망 재현은 불가 — 격리 로직은 코드 리뷰로 검증, 운영 배포 후 다음 배치 사이클(07:30 UTC)에 cron 재개 여부로 최종 확인
- 운영 확인 절차(배포 후): `docker logs hubth-app | grep "배치 저장 시작"` 에 ECOS·글로벌 로그 등장 + `SELECT max(created_at) FROM global_indicator` 갱신

## Risks

- LogBatchBuffer 실행 모델 변경으로 인한 회귀 가능성 — 트리거 의미 불변을 리뷰에서 중점 확인
- 풀 상향(5→10)은 메모리 영향 미미 (유휴 스레드 5개 추가)
- reactor 사망 1차 원인은 미해결로 남음 — 재발 시 로그 유실만 발생(드롭 카운터로 관측 가능), cron은 보호됨

## Out of Scope

- reactor 사망 1차 원인 조사·수정, ES 클라이언트 자가 복구 (후속)
- #51 글로벌 히스토리 결함 (본 이슈 완료 후 별도 진행)
- 로깅 파이프라인 구조 변경
