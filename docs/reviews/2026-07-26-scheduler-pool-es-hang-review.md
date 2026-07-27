# ES 클라이언트 행 스케줄러 풀 고갈 수정 Review 기록

**Date:** 2026-07-26
**Issue:** #96
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md
**대상:** 커밋 dbe06bb 코드 변경 4파일
**방식:** ce:review 미설치(원격 세션) → compound-engineering review_agents 4종(code-simplicity-reviewer, security-sentinel, performance-oracle, architecture-strategist) 병렬 서브에이전트 대체 수행 (#93 선례)

## Findings (심각도순, 에이전트 교차 취합·중복 제거)

### 중간

- **[M1] KIS ZIP 다운로드 body 구간 타임아웃 공백** — `KisMasterFileClient.java:118-123`. JDK `HttpRequest.timeout`은 응답 헤더 수신까지만 적용(보안 에이전트가 JDK 21 `MultiExchange` 바이트코드로 검증 — 헤더 도착 시 타이머 해제). 수 MB ZIP body 스트리밍 중 스톨하면 여전히 무기한 블로킹 — 본 수정이 제거하려던 행 클래스가 잔존. 이 경로는 스케줄러 스레드(23h 갱신)뿐 아니라 **캐시 미스 시 사용자 요청 스레드**(`KisStockMasterCache.getAllStocks`)에서도 실행됨. 3개 에이전트 공통 지적. 반면 DART는 Spring `JdkClientHttpRequest$TimeoutHandler`가 body 읽기까지 커버함이 검증됨(안전). → `sendAsync(...).get(DOWNLOAD_TIMEOUT)` 방식으로 총 시간 상한 부여 권고.
- **[M2] flushPending 예외 누출 시 주기 flush 조용한 영구 취소** — `LogBatchBuffer.java:89-90,136-147`. `scheduleWithFixedDelay`는 태스크가 예외를 던지면 주기 실행을 영구 취소하고 예외를 삼킨다. 기존 `@Scheduled`는 Spring error handler가 로깅 후 계속 보장했으므로 에러 처리 의미의 회귀. Exception 레벨 누출 경로는 없음이 라인 추적으로 검증됐으나(`flush()` 내부 try-catch 완결) Error(OOM·LinkageError)는 새어나감 — 실패 양상이 정확히 "조용한 영구 정지"로 본 수정이 제거하려는 장애 계열과 동일. → flushPending 최상위 catch(Throwable) 방어 권고.
- **[M3] 동일 계열 잔존 경로 미문서화** — `LogIndexScheduler`(03:00/23:55 cron)가 여전히 공용 풀에서 같은 ES 전송로(`Rest5Client`/`BasicFuture.get()`)를 동기 호출, 뉴스 배치 경로(`NewsSaveService` → `NewsElasticsearchIndexer.save`)도 동일. reactor 사망 시 cron당 최대 1스레드 잠식 가능(유계, 풀 10 상향으로 완화되나 plan의 "cron은 보호됨" 결론은 부분적으로만 참). 코드 수정은 범위 확대이므로 → 잔여 위험 문서 명기 + 후속 이슈 검토.
- **[M4] AUDIT 로그 드롭 관측 한계** — 하드캡 드롭이 도메인(AUDIT/ERROR/BUSINESS) 구분 없이 공용 `log.ingestion.dropped` 카운터에 합산(rejection·매핑 실패·bulk 실패와도 합산). ES 장애 중 감사 로그 유실량을 사후 판별 불가 — 코드베이스가 감사 시그널 보존을 명시 요구(`AdminGuardInterceptor` 등)하는 것과 긴장. → 카운터 도메인 태그/분리는 범위 확대 — 태형님 판단 필요.

### 낮음

- **[L1] drainLoop 인터럽트 소거 가능성** — `LogBatchBuffer.java:185,196,246`. `shutdownNow()`의 1회 인터럽트가 `flush()` 실행 중 예외로 표면화되면 `catch (Exception)`이 삼키면서 인터럽트 상태가 소거되어 루프가 재개될 수 있음. daemon 스레드라 앱 종료는 지연되지 않음(영향: 잔류 스레드 1개). work 문서의 "인터럽트로 스레드 회수 유효" 서술은 과장 → 문서 정정 + catch에서 인터럽트 복원 권고.
- **[L2] enqueue 하드캡 분기 컨벤션** — `LogBatchBuffer.java:93-118`. 신규 3단계 중첩 + 메서드 길이 초과(code-convention 기준). lock 경계를 깨지 않는 private helper(`dropIfHardCapReached()`) 추출 가능.
- **[L3] DART read 15s의 corpCode ZIP 회귀 리스크** — 같은 클라이언트를 쓰는 `DartApiClient.downloadCorpCodes()`(수 MB ZIP)에도 15s가 일괄 적용 — 느린 회선에서 기존 성공 케이스가 타임아웃될 수 있음. 23h 주기 재시도로 복구 가능. → 값 상향/분리 검토.
- **[L4] 셧다운 후 late enqueue 무계수 소멸** — `requestFlush`의 RejectedExecutionException 경로에서 드롭 카운터 미증가. 셧다운 윈도우 한정이라 영향 미미.

### nit

- **[N1]** drain 타임아웃 WARN "잔여 N건"이 행 중 in-flight 배치(최대 1,000건) 미포함 — 유실 과소보고.
- **[N2]** `application.yml` 주석 "스케줄 작업 11개" — periodicFlush 제거로 실제 @Scheduled는 10개.
- **[N3]** `ApplicationLog log` 파라미터가 @Slf4j `log` 가림 → `LogBatchBuffer.log` 정규화 접근 (기존 flush()와 일관된 기존 패턴).
- **[N4]** KIS 타임아웃 하드코딩 상수 vs 같은 도메인 `KisRestClientConfig`의 KisProperties 방식 이원화 (plan의 "리팩토링 금지, 타임아웃만" 원칙상 수용).
- **[N5]** `estimateBytes`가 char 수 기반이라 한글 payload의 하드캡 실효 크기 과소평가 (기존 로직, 본 커밋은 캡 배수만 신설. LogSanitizer 16KB 상한으로 유한).

### 검증 통과 (문제 없음 확인)

- 동시성: flushRequested CAS 순서(reset→swapOut)로 요청 유실 창 없음, lock 내 ES 호출 없음(데드락 불가), executor 큐 유계(주기 1 + 요청 1), dropWarned 에피소드 전이 정확
- 보안: DART TLS(SSLContext.getDefault) 유지, 타임아웃 예외 메시지에 API 키 노출 없음(Spring이 쿼리스트링 절단 — 바이트코드 검증), WARN 로그 민감정보 없음
- 단순성: 죽은 코드·YAGNI 위반 없음, 신규 멤버 전부 실사용
- 범위: diff가 plan 변경 파일 표·구현 단계와 정확히 일치, 숨은 동작 변경 없음, 로그 적재 의미 보존

## Open Questions / Assumptions

- `ELASTICSEARCH_URIS`에 basic-auth 자격증명 포함 형태로 운영 배포되는가? 그렇다면 bulk 실패 WARN의 `e.getMessage()` 자격증명 노출 여부 별도 확인 필요.
- 하드캡 도달 시 drop-newest(현행, 오래된 로그 보존) vs drop-oldest(최신 행위 보존) — 감사 관점 논의 대상.
- M3 잔존 경로(LogIndexScheduler·뉴스 ES)의 코드 수정은 후속 이슈로 분리할 것인가.
- 컨텍스트 종료 시 async 리스너 풀과 본 버퍼의 파괴 순서는 변경 전과 동일하게 미보장 (의미 보존, 기존 리스크).

## Change Summary

커밋 dbe06bb는 LogBatchBuffer flush를 전용 단일 daemon 스레드로 격리하고 하드캡 드롭 + 셧다운 유한 대기를 도입, KIS/DART 타임아웃과 풀 상향을 더해 #96의 핵심 목표(공용 풀·호출자 스레드 비잠식)를 달성했다. 격리 구조의 동시성 설계는 4개 에이전트 교차 검증에서 건전함이 확인됐다. 남은 실질 이슈는 KIS body 구간 타임아웃 공백(M1), flushPending Error 방어 공백(M2), 잔존 ES 경로 문서화(M3), AUDIT 드롭 관측 한계(M4)다.

## 반영 내역

태형님 "권장안대로 반영해" (2026-07-26) — M1·M2·L1·L2·N2 코드 반영 + M3 문서 명기. M4(카운터 도메인 분리)·L3(DART 타임아웃 값 조정)은 보류(범위 확대 — 후속 논의), L4·N1·N3·N4·N5는 미반영 수용.

- M1: `KisMasterFileClient.downloadZip` — `send` → `sendAsync` + `get(120s, SECONDS)`로 본문 포함 총 시간 상한. 타임아웃 시 future cancel 후 IOException 변환, ExecutionException은 IOException unwrap 또는 래핑 (기존 호출자 예외 처리 흐름 유지)
- M2: `LogBatchBuffer.flushPending` 전체를 catch(Throwable)로 방어 — 주기 태스크 영구 취소 방지, WARN 로깅 후 주기 유지
- L1: `flush()`의 bulk catch에서 인터럽트 유래 예외(`causedByInterrupt` cause 체인 검사) 감지 시 인터럽트 복원 — drainLoop 종료 가드 실효화. work 문서의 과장 서술 정정
- L2: enqueue 하드캡 분기를 `dropIfHardCapReached()` private helper로 추출 (lock 경계 불변, 중첩 완화)
- N2: application.yml 주석 "11개" → "@Scheduled 작업 10개" 정정
- M3: work 문서 특이사항에 잔존 ES 행 경로(LogIndexScheduler·뉴스 배치) 명기 — 후속 이슈 후보
