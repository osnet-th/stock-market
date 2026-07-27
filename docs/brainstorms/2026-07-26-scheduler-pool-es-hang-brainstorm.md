# ES 클라이언트 행(hang)으로 인한 스케줄러 풀 고갈 - Brainstorm

**Date:** 2026-07-26
**Status:** Decided (태형님 "진행해" — Issue A 우선 진행 승인, 2026-07-26. 세부 설계 결정은 plan 게이트에서 확정)
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md

## 배경

글로벌 경제지표 그래프에 히스토리가 안 보인다는 문제(별도 Issue B)를 추적하는 과정에서 시스템 전체 장애를 발견했다.

### 진단 경위 (2026-07-26, 운영 서버 확인)

1. 운영 DB `global_indicator`: 620건 전부 **2026-04-11 07:30:00~56 사이 생성** — 배치가 평생 1회만 커밋. 이후 3개월 반 동안 0건.
2. `docker logs` 최근 72시간: **ECOS(07:00)·글로벌(07:30) 배치 로그 전무** — cron 배치 전체가 실행되지 않음.
3. 스레드 덤프(kill -3): 스케줄러 풀 5개 중 **scheduling-3, scheduling-5가 `ElasticsearchClient.bulk` → `Rest5Client.performRequest` → `BasicFuture.get()`에서 영구 블로킹** (기동 5시간 만에 2개 소모).
4. 앱 WARN 로그: `ES 오늘 장애 건수 조회 실패: err=I/O reactor has been shut down` — 앱 내 ES 클라이언트의 비동기 I/O reactor가 죽어 있음.
5. ES 서버 컨테이너는 5주 무중단·로그 정상 — **reactor 사망은 앱(클라이언트) 측 문제**. 앱 로그 첫머리에 `IOReactorWorker.run` 스택 흔적 확인.

### 장애 메커니즘

- `NewsElasticsearchConfig`에 connect 3s / socket 5s 타임아웃이 설정돼 있으나, **이 타임아웃을 집행하는 주체가 I/O reactor 자신**이다. reactor가 죽으면 in-flight 요청의 future는 완료도 실패도 타임아웃도 되지 않고, `Rest5Client.performRequest`의 `BasicFuture.get()`(무기한 대기)이 **영원히** 블로킹된다.
- 이 블로킹이 스케줄러 풀(`task.scheduling.pool.size: 5`)의 스레드를 잠식하는 경로가 2개:
  - `LogBatchBuffer.periodicFlush` — `@Scheduled(fixedDelay=5s)`로 공용 스케줄러 풀에서 실행
  - `LogBatchBuffer.enqueue` — 버퍼 500건/5MB 도달 시 **호출자 스레드에서 직접 flush** → 로그를 남기는 스케줄 작업(cron)의 스레드가 직접 ES에 붙었다가 잠식
- 풀 5개가 모두 잠식되면 **@Scheduled 작업 11개 전부**(ECOS·글로벌·뉴스·부동산·포트폴리오 알림·로그 flush·SEC/DART/KIS 캐시 갱신) 조용히 영구 정지한다. 로그도 안 남는다.
- ECOS만 데이터가 쌓여 보인 이유: `EcosIndicatorWarmupListener`(ApplicationReadyEvent, 스케줄러 풀 무관)가 재시작마다 적재해서. 글로벌은 warmup이 없어 04-11에 동결.

### 파급

- 글로벌 지표 히스토리 동결(Issue B의 직접 원인), ECOS cron 배치 미실행, 뉴스/부동산 배치 미실행, ES 로그 적재 중단, 최근 업데이트/알림 기능 무력화.
- 재시작하면 풀이 리셋되어 한동안 정상 → reactor가 다시 죽으면 재발하는 간헐 패턴.

## What We're Building

reactor가 다시 죽더라도 **비즈니스 스케줄러가 잠식되지 않고, 로그 적재는 best-effort로 유실 처리**되도록 앱을 방어한다.

1. **로그 flush를 공용 스케줄러 풀에서 격리** — `LogBatchBuffer`가 전용 단일 스레드 executor(daemon)를 소유하고, 주기 flush와 버퍼 초과 flush 모두 그 스레드에서만 실행한다. 호출자 스레드(cron·async 리스너)는 어떤 경우에도 ES에 직접 붙지 않는다.
2. **flush 스레드 행 상태에서의 버퍼 보호** — flush 스레드가 잠긴 상태에서 버퍼가 계속 차면 신규 로그를 드롭하고 기존 드롭 카운터(`logIngestionDroppedCounter`)로 계수한다 (best-effort at-most-once 원칙 유지).
3. **스케줄 작업의 무한 대기 위생 보강** — 같은 계열의 잠재 위험 제거:
   - `KisMasterFileClient.downloadZip`: `HttpClient.newHttpClient()` + `.timeout()` 없는 요청 → connect/request 타임아웃 부여 (23시간마다 스케줄러 스레드에서 ZIP 다운로드)
   - `DartRestClientConfig`: connect/read 타임아웃 미설정 → 부여
4. **스케줄러 풀 크기 상향** — 5 → 10 (작업 11개 대비 여유 확보, 잠식 내성은 1·2가 담당)

## 검토한 대안 (채택 안 함)

- **ES 호출을 future.get(timeout)으로 감싸 강제 타임아웃** — 동기 `ElasticsearchOperations` 호출을 별도 스레드로 넘겨 대기 시간만 제한하는 방식. 타임아웃돼도 실제 스레드는 여전히 잠식된 채 남고(인터럽트로 안 풀림), 스레드 핸드오프가 격리(1안)와 중복된다. 격리 + 드롭 정책이 더 단순하고 충분.
- **reactor 사망 감지 시 ES 클라이언트 재생성(자가 복구)** — 효과는 있으나 Spring Data ES 클라이언트 빈의 런타임 교체는 복잡도가 높다. 격리가 되면 로그 적재 외 영향이 없으므로 후속 과제로 미룬다 (복구는 앱 재시작).
- **스케줄러 풀만 크게 늘려 버티기** — 잠식 속도만 늦출 뿐 근본 해결이 아님. 보조 수단으로만 채택(4안).

## Open Questions

- reactor가 죽는 1차 원인 (앱 로그의 `IOReactorWorker` 예외 스택 확보 시 후속 조사 — 본 수정과 독립, 수정 후에는 죽어도 로그 유실만 발생)
- ES 로깅 자가 복구(클라이언트 재생성)는 후속 과제 후보

## Edge Cases

- 앱 종료 시(`drainOnShutdown`): flush 스레드가 행이면 drain도 못 하므로, 전용 executor에 제출 후 **유한 대기**로 변경 — 타임아웃 시 유실 감수(기존 원칙과 동일).
- flush 스레드가 행에서 회복(reactor 복구)되면 자연스럽게 재개 — 별도 상태 관리 없음.
- 전용 스레드 도입 후에도 ES 정상 시 동작은 기존과 동등해야 함 (5초 주기, 500건/5MB 트리거).

## 범위 밖 (하지 않음)

- 글로벌 경제지표 히스토리 수정 (기존 open 이슈 #51로 진행: 지표 단위 트랜잭션, warmup 부재, 정렬 버그. #25로 추가된 10개 지표는 시딩 이후 추가된 것으로 배치 재개 시 자동 시딩 예상 — 별도 수정 불필요 가능성 높음, #51에서 확인)
- reactor 사망 1차 원인 수정 (원인 미상 — Open Question)
- ES 클라이언트 자가 복구
- 로깅 파이프라인 구조 변경 (버퍼/인덱스 전략 등 기존 유지)
