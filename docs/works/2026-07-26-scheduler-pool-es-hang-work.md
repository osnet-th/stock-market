# ES 클라이언트 행 스케줄러 풀 고갈 수정 Work 기록

**Date:** 2026-07-26
**Issue:** #96
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md
**Plan:** docs/plans/2026-07-26-002-fix-scheduler-pool-es-hang-plan.md

## 변경 파일

- `src/main/java/.../logging/infrastructure/elasticsearch/LogBatchBuffer.java`
  - 공용 스케줄러 풀의 `@Scheduled periodicFlush` 제거 → `@PostConstruct`에서 전용 단일 daemon 스레드(`log-flush`) `ScheduledExecutorService` 생성, 같은 주기(5s fixedDelay)로 `flushPending` 자체 스케줄
  - `enqueue()`: 임계(500건/5MB) 초과 시 호출자 스레드 직접 flush 제거 → `requestFlush()`로 전용 스레드에 제출 (AtomicBoolean `flushRequested`로 중복 제출 방지, 셧다운 중 RejectedExecutionException은 drain에 위임)
  - 버퍼 하드캡(1,000건/10MB) 도입: flush 스레드 행 상태에서 초과 시 신규 로그 드롭 + `logIngestionDroppedCounter` 계수 + 에피소드당 1회 WARN (`dropWarned`는 lock 보호, swapOut 시 리셋)
  - `drainOnShutdown`: drain 루프를 전용 스레드에 제출 후 `Future.get(10s)` 유한 대기 → 타임아웃/인터럽트/실패 시 유실 감수 WARN, finally에서 `shutdownNow()` (행 상태여도 앱 종료 지연 없음)
  - flush 의미 불변: 5s/500건/5MB 트리거, 월별 인덱스 그룹핑, best-effort at-most-once, 실패 시 카운터+WARN
- `src/main/java/.../stock/infrastructure/stock/kis/KisMasterFileClient.java`
  - `downloadZip`: `HttpClient.newHttpClient()` → `connectTimeout(10s)` builder + 요청 `timeout(120s)` (마스터파일 ZIP 대용량 감안)
- `src/main/java/.../stock/infrastructure/stock/dart/config/DartRestClientConfig.java`
  - `connectTimeout(3s)` + `JdkClientHttpRequestFactory.setReadTimeout(15s)` — SecRestClientConfig와 동일 수준
- `src/main/resources/application.yml`
  - `spring.task.scheduling.pool.size` 5 → 10 (주석으로 #96 근거 명기)

## API·Entity·DB 변경

없음 (plan 준수).

## work 단계 자체 검증

- `./gradlew compileJava` 통과 (deprecation 노트 1건은 기존 무관 파일 `RealEstateRestClientConfig`)

## 특이사항 / 알려진 한계

- JDK `HttpClient`의 `HttpRequest.timeout`은 응답(헤더) 수신까지의 타임아웃이라 **본문 스트리밍 중 지연(tarpit)은 커버하지 못함**. KIS ZIP 다운로드의 잔여 리스크이나, 본 수정의 본체(LogBatchBuffer 격리)와 무관하고 발생 빈도가 낮아 수용. 근본 방어는 격리 원칙(행이 나도 해당 작업 1개만 영향).
- `flushExecutor.shutdownNow()`의 인터럽트는 행 중인 `BasicFuture.get()`(condition await)을 깨울 수 있어 셧다운 시 스레드 회수에도 유효.

## 미검증 항목 (validation 단계 과제)

- reactor 사망 상태 재현 불가 — 격리 로직은 리뷰로 검증, 운영 배포 후 다음 배치 사이클(07:30 UTC = KST 16:30)에 cron 재개로 최종 확인
- ES 정상 상태에서의 로그 적재 동등성(주기·트리거) 실기동 확인
