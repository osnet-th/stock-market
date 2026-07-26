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

- ~~JDK `HttpRequest.timeout`의 본문 미커버 한계~~ → **리뷰 M1 반영으로 해소**: KIS ZIP 다운로드는 `sendAsync` + 유한 `get(120s)`으로 본문 수신 포함 총 시간 상한을 걸었다. DART는 Spring `JdkClientHttpRequest`의 TimeoutHandler가 본문 읽기까지 커버함이 리뷰에서 검증됨.
- `flushExecutor.shutdownNow()` 인터럽트는 `flush()`의 `catch (Exception)`에 삼켜져 소거될 수 있어(리뷰 L1) 인터럽트 유래 예외 감지 시 복원하도록 보강. flush 스레드는 daemon이라 어떤 경우에도 앱 종료를 막지 않는다.
- **잔존 ES 행 경로 (리뷰 M3, 본 이슈 범위 밖 — 후속 이슈 후보)**: `LogIndexScheduler`(03:00 cleanup / 23:55 precreate cron)와 뉴스 배치(`NewsSaveService` → `NewsElasticsearchIndexer.save`)는 여전히 공용 스케줄러 풀에서 동일 ES 전송로(`Rest5Client`/`BasicFuture.get()`)를 동기 호출한다. reactor 사망 시 cron당 최대 1스레드 잠식 가능(누적되지 않고 유계, 풀 10 상향으로 완화). 핵심 잠식원(5초 주기 + 임계 flush)은 격리됐으므로 전면 정지 재발 가능성은 크게 낮아졌으나, "cron 완전 보호"는 이 경로 해소 후에 성립한다.

## 미검증 항목 (validation 단계 과제)

- reactor 사망 상태 재현 불가 — 격리 로직은 리뷰로 검증, 운영 배포 후 다음 배치 사이클(07:30 UTC = KST 16:30)에 cron 재개로 최종 확인
- ES 정상 상태에서의 로그 적재 동등성(주기·트리거) 실기동 확인
