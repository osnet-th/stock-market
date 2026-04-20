---
title: Elasticsearch 기반 애플리케이션 로깅 시스템 도입
type: feat
status: active
date: 2026-04-20
origin: docs/brainstorms/2026-04-20-elasticsearch-application-logging-brainstorm.md
---

# Elasticsearch 기반 애플리케이션 로깅 시스템 도입

## Enhancement Summary

**Deepened on**: 2026-04-20 (ultrathink + 3 research + 4 review agents + 3 re-review agents)

### Key Design Decisions (최종)

1. **Pointcut = `@within(@RestController)` + GlobalExceptionHandler 이중 수집** — `@LogAudit`만으로는 에러 누락
2. **`LogBatchBuffer`를 Phase 2로 전진** — 단건 적재는 공유 ES(2GB heap) 포화 위험. 5s/500/5MB flush
3. **Spring 6.1+ `ContextPropagatingTaskDecorator`** — 커스텀 MDC 전파 코드 불필요
4. **ES 8.x API**: `_index_template` (legacy `_template` deprecated), `BulkIngester<T>` 또는 `ElasticsearchOperations.bulkIndex()`
5. **단일 `ApplicationLogEvent` + `ApplicationLogDocument`** + 도메인 enum 구분 (3종 클래스 중복 제거)
6. **트랜잭션 분리**: Business만 `@TransactionalEventListener(AFTER_COMMIT)`, Audit/Error는 즉시 publish
7. **보안 최소 방어선 유지**: fail-close 화이트리스트, `SecurityContextHolder` only, CRLF 치환, 마스킹 5종, `destructive_requires_name`
8. **Data Integrity Policy** 추가: timestamp 규약, UTC 단일 기준, best-effort at-most-once, GDPR 대응 (챗봇 원문 금지)
9. **크기 상한 하향**: 필드 4KB→2KB, 문서 64KB→16KB
10. **네이밍 일관성**: `ApplicationLogEvent`, `ApplicationLogDocument`, `LogSearchResponse`, `LogDailyCountResponse`, `LogMonthlyIndexNameResolver`
11. **cron 외부화**: `scheduler.logging.{cleanup,precreate}.cron` — 기존 패턴과 일관

### Deferred to Future Considerations (개인 프로젝트 YAGNI)

- Bucket4j rate limit (관리자 2명+ 확장 시)
- ES/JVM `HealthIndicator` (오케스트레이터 도입 시) — 운영자 페이지 배지로 대체
- Meta-audit 별도 sync 경로 (팀 감사 요건 도입 시)
- Credential 타입 파라미터 기동 스캔 (팀 확대 시)
- Cloudflare `CF-Connecting-IP` 파싱 (clientIp 로깅 결정 시)
- ArchUnit 자동 레이어 검증 (팀 협업 시)
- CSP strict 헤더 (관리자 페이지 별도 도메인 운영 시)

### Anti-patterns to Avoid (리서치/리뷰 결과)

- Controller에 `final` 키워드 (CGLIB 프록시 실패)
- `this.method()` self-invocation (AOP 우회)
- `@Async` 리스너에서 try-catch 없이 던지기 (silent drop)
- `DateFormat.epoch_millis` + Instant (Spring Data ES 직렬화 버그)
- Wildcard index delete (`app-audit-*`) — exact name만
- `CallerRunsPolicy` rejection (요청 스레드에서 ES 쓰기 발생, p99 spike)
- Alpine.js `x-html` (XSS)
- userId를 header/param에서 읽기 (Authentication principal만)
- 공용 `applicationTaskExecutor` 사용 (가상 스레드 충돌) — 반드시 `@Async("logIndexerExecutor")`

---

## Overview

뉴스 검색용으로 도입된 Elasticsearch 인스턴스를 재활용하여, 선별적 애플리케이션 로그(API 감사 / 에러·예외 / 비즈니스 이벤트)를 구조화된 문서로 적재하고 운영자 페이지에서 조회·검색·집계할 수 있도록 한다.

접근은 **코드 레벨 통합(Approach A)** — Spring AOP + 커스텀 로거 서비스 + `@Async` + Spring Data Elasticsearch를 사용한다 (see brainstorm: `docs/brainstorms/2026-04-20-elasticsearch-application-logging-brainstorm.md`).

## Problem Statement

현재 프로젝트의 관측 수단은 Spring Boot 기본 콘솔 로깅(stdout)뿐이다. 이로 인해:

1. **사후 조사 불가**: 어제 발생한 500 에러, 어떤 사용자가 어떤 엔드포인트를 호출했는지 흐름 복원 불가
2. **비즈니스 이벤트 추적 공백**: 포트폴리오 변경, 챗봇 질의 등 도메인 이벤트가 휘발됨
3. **성능 병목 식별 불가**: API 응답시간 추이, 느린 요청 탐지 부재
4. **운영자 접근성 부재**: 콘솔 로그는 배포 서버 로그인 없이 조회 불가, 필터/검색 불편

동시에 Elasticsearch가 이미 뉴스 검색용으로 운영되고 있어, 신규 인프라 없이 로깅 백엔드로 재활용 가능하다.

## Proposed Solution

3개 도메인 인덱스를 월 단위로 롤링 생성하고, 요청 흐름을 `requestId`로 연결한다.

| 도메인 | 인덱스 | 수집 수단 |
|--------|--------|-----------|
| API 감사 (audit) | `app-audit-YYYY.MM` | `@LogAudit` AOP (Controller 메서드 `@Around`) |
| 에러/예외 (error) | `app-error-YYYY.MM` | `GlobalExceptionHandler` 확장 + AOP `@AfterThrowing` |
| 비즈니스 이벤트 (business) | `app-business-YYYY.MM` | `DomainEventLogger` 서비스 명시 호출 |

- **공통 필드**: `timestamp`, `domain`, `userId`, `requestId`, 도메인별 `payload`
- **비동기 적재**: `@Async` 전용 TaskExecutor + `ApplicationEventPublisher` + MDC 전파 `TaskDecorator`
- **보관 정책**: 30일, 매일 03:00 스케줄러로 31일 이전 월 인덱스 DELETE (ILM 미사용)
- **운영자 UI**: `index.html` 내부 `x-if` 섹션 + `js/components/admin-logs.js`, 관리자 userId 화이트리스트 가드

## Data Integrity Policy (필수 사전 규약)

1. **Timestamp 출처**: AOP `@Around` 진입 시 또는 `publishEvent` 직전에 `Instant.now()` 로 **한 번** 확정. 리스너/indexer는 절대 재할당 금지
2. **UTC 단일 기준**: Document에 저장되는 `timestamp`는 UTC. **월별 인덱스 라우팅도 UTC 연월** 기반 (`toInstant().atZone(UTC).format("YYYY.MM")`). UI 표시만 `Asia/Seoul` 변환 — 월 경계 경계조건 불일치 회피
3. **Best-effort logging 정책**: 로그 적재는 at-most-once. partial bulk 실패는 drop 카운터만 증가, 재시도 없음. 로그 유실이 비즈니스 실패를 유발하지 않음
4. **Graceful shutdown**: `@PreDestroy` 훅에서 `LogBatchBuffer.flushRemaining()` + 최대 10초 대기. 그 안에 flush 실패 시 유실 감수
5. **Sanitize 실패 방어**: `LogSanitizer.sanitize()` 자체가 throw하면 원본 payload 대신 `{"_sanitize_failed": true, "domain": "...", "requestId": "..."}` 최소 문서 저장 (완전 유실 금지)
6. **개인정보 정책 (GDPR/PIPA 대응)**:
   - 챗봇 비즈니스 이벤트 payload: **프롬프트/응답 원문 기록 금지**. 메타데이터만 (길이, 토큰수, 모델명, 소요시간)
   - 포트폴리오 변경 payload: 금액/종목코드는 기록, 개인 메모는 기록 금지
   - GDPR erasure 요청 시: `_update_by_query`로 해당 userId 문서의 `userId` → `null`, payload 주요 필드 해시화 (error/meta-audit 인덱스는 legitimate interest로 30일 유지)

## Technical Approach

### Architecture

```
┌──────────────────────────────────────────────────────────────┐
│ Request Lifecycle                                             │
│                                                                │
│  Client ──► RequestIdFilter ──► JwtAuthenticationFilter       │
│              │ UUID → MDC                                      │
│              │ 응답 헤더 X-Request-Id                          │
│              ▼                                                 │
│         @LogAudit (Controller) ─┐                              │
│              │                   │                             │
│              ▼                   │ AuditEvent                  │
│          [Business Code]         │ ErrorEvent                  │
│              │                   │ BusinessEvent               │
│              ▼                   │                             │
│      (optional)                  │ ApplicationEventPublisher   │
│      DomainEventLogger ──────────┤                             │
│              │                   ▼                             │
│              │            Spring Async Listener                │
│              │            (logIndexerExecutor,                 │
│              │             MDC copy via TaskDecorator)         │
│              │                   │                             │
│              ▼                   ▼                             │
│     GlobalExceptionHandler   LogIndexPort                      │
│              │                   │ BulkProcessor               │
│              └───► ErrorEvent ─► ElasticsearchOperations       │
│                                   │                            │
│                                   ▼                            │
│                              Elasticsearch                     │
│                       (app-audit-YYYY.MM 등)                  │
└──────────────────────────────────────────────────────────────┘
```

### Package Structure (신규 top-level 도메인 `logging`, 심화 검토 후 간소화)

```
src/main/java/com/thlee/stock/market/stockmarket/logging/
├── presentation/
│   ├── AdminLogController.java              # /api/admin/logs/**
│   └── dto/
│       ├── LogSearchRequest.java
│       ├── LogSearchResponse.java           # news 패턴 일치 (Document 누출 안 함)
│       └── LogDailyCountResponse.java       # 도메인 prefix 일관
├── application/
│   ├── annotation/
│   │   └── LogAudit.java                    # @LogAudit 유지 (명확성 우선)
│   ├── event/
│   │   └── ApplicationLogEvent.java         # ApplicationLog + Event (이벤트 버스 충돌 회피)
│   ├── DomainEventLogger.java               # application 계층에서만 호출
│   └── LogSearchService.java                # Caffeine 60s 집계 캐시
├── domain/
│   ├── model/
│   │   ├── ApplicationLog.java              # 불변 record
│   │   └── LogDomain.java                   # enum
│   └── service/
│       ├── LogIndexPort.java                # news 패턴 일치 (domain/service/)
│       └── LogSanitizer.java                # truncation + stacktrace + 마스킹 통합
└── infrastructure/
    ├── aspect/
    │   └── ApplicationLoggingAspect.java
    ├── async/
    │   ├── LogAsyncConfig.java              # @EnableAsync + logIndexerExecutor + AsyncUncaughtExceptionHandler
    │   └── LogEventListener.java            # @Async @EventListener + @TransactionalEventListener(AFTER_COMMIT)
    ├── elasticsearch/
    │   ├── config/LoggingElasticsearchConfig.java
    │   ├── document/ApplicationLogDocument.java  # 단일 Document
    │   ├── mapper/LogDocumentMapper.java     # 도메인↔문서 (news 패턴)
    │   ├── LogElasticsearchIndexer.java
    │   ├── LogElasticsearchSearcher.java
    │   ├── LogBatchBuffer.java               # 5s/500/5MB flush
    │   └── LogMonthlyIndexNameResolver.java  # 도메인 prefix 일관
    ├── scheduler/
    │   └── LogIndexScheduler.java           # cleanup + precreate, cron은 application.yml
    ├── filter/
    │   ├── RequestIdFilter.java             # @Component (기존 filter 와이어링 패턴 일치)
    │   └── AdminGuardInterceptor.java
    └── config/
        ├── LoggingProperties.java           # app.logging.* (truncation/queue)
        └── AdminProperties.java             # app.logging.admin.user-ids (별도)

# 기존 파일 수정 (신규 파일 X)
src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/
└── GlobalExceptionHandler.java              # ApplicationEventPublisher 주입 + publishEvent 한 줄 추가
```

변경점 근거:
- **단일 `LogEvent` + `LogDocument`**: 3종 클래스 중복 코드 ~120 LOC 제거. 도메인은 enum 필드로 구분
- **Sanitizer 통합**: Truncator + StackTrace + Masker가 모두 문자열 변환 — 한 클래스 3 메서드로
- **Scheduler 통합**: cleanup과 pre-create가 동일 `ElasticsearchOperations` 의존 → 한 클래스 2 `@Scheduled`
- **`LogIndexPort`를 `domain/service/`**: 기존 `NewsIndexPort`와 일관 (arch W2)
- **`@LogAudit`을 `application/annotation/`**: 공개 API 장식자는 application 적합 (arch S3)
- **`LoggingProperties` 단일화**: admin user-ids / 큐 크기 / truncation 상한 / stacktrace 라인 등 전부 포함

### Domain 모델 (불변 + Getter Lombok)

```java
// ApplicationLog.java (도메인 모델 — 단일 record, payload는 polymorphic)
public record ApplicationLog(
    Instant timestamp,                  // ISO 8601 full datetime (LocalDate 함정 회피)
    LogDomain domain,
    Long userId,                        // nullable (anonymous, scheduler)
    String requestId,
    Map<String, Object> payload,        // 도메인별 자유 필드
    boolean truncated,
    Integer originalSize                // truncated=true일 때만 세팅
) {}
```

**ES Document 직렬화 규칙**:
- `@Field(type = FieldType.Date, format = DateFormat.date_time)` — ISO 8601 full
- `DateFormat.epoch_millis` **절대 사용 금지** (Spring Data ES issue #2318: Instant 직렬화 시 문자열로 저장되어 ES 숫자 타입과 미스매치)
- 과거 함정 참고: `docs/solutions/integration-issues/elasticsearch-localdate-serialization-mismatch-2026-04-20.md`

### `@LogAudit` 애노테이션 및 AOP (Critical C1 — Pointcut 전략 수정)

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAudit {
    boolean includeBody() default false;              // 기본 off (PII 보호)
    String[] maskFields() default {};                 // 특정 필드 추가 마스킹
}
```

**Pointcut 전략 (심화 결정)**:
- **Audit 대상 = 모든 `@RestController`**: `execution(* com.thlee..presentation..*Controller.*(..))` 또는 `@within(@RestController)`. 새 Controller 추가 시 자동 적용.
- **Opt-out**: `@LogAudit(includeBody=true)` 같은 세부 제어는 메서드 레벨 애노테이션. 미적용 시 기본값 (body 제외).
- **Error 수집은 이중 경로**:
  1. AOP `@AfterThrowing` — 모든 Controller에 자동 적용
  2. `GlobalExceptionHandler`에도 `ApplicationEventPublisher.publishEvent(LogEvent)` 삽입 — AOP가 어떤 이유로 누락했거나, Filter 체인에서 발생한 예외도 커버
  - 중복 발행은 `requestId` 기준 최근 N초 내 중복 제거 (간단한 Caffeine `Cache<String, Boolean>` TTL 5s) 또는 문서 ID를 `{requestId}-{exceptionClass}`로 강제해 ES 레벨에서 중복 허용

**Spring AOP 함정 회피 (리서치 결과)**:
- **Controller에 `final` 금지** — CGLIB proxy는 final 클래스/메서드를 감쌀 수 없음
- **Self-invocation 금지** — Controller 내부 `this.method()` 호출은 AOP를 타지 않음. 반드시 주입된 빈 경유
- `@Around` 안에서 예외 catch 후 **재던짐 필수** — Controller 응답이 정상 흐름으로 돌아와야 `GlobalExceptionHandler`가 동작

### 이벤트 리스너 (단일 LogEvent, 트랜잭션 경계 분리)

```java
@Component
@RequiredArgsConstructor
public class LogEventListener {
    private final LogIndexPort logIndexPort;
    private final LogSanitizer sanitizer;

    // Audit/Error — 즉시 처리. 트랜잭션 롤백 여부와 무관하게 "요청이 있었다"는 사실 기록
    @Async("logIndexerExecutor")
    @EventListener
    public void onImmediate(LogEvent event) {
        if (event.domain() == LogDomain.BUSINESS) return;  // below handler가 처리
        safeSave(event);
    }

    // Business — 트랜잭션 커밋 후에만. 롤백 시 오팬 로그 방지
    @Async("logIndexerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAfterCommit(LogEvent event) {
        if (event.domain() != LogDomain.BUSINESS) return;
        safeSave(event);
    }

    private void safeSave(LogEvent event) {
        try {
            logIndexPort.save(sanitizer.sanitize(event.toApplicationLog()));
        } catch (Exception e) {
            droppedCounter.increment();
            log.warn("Log indexing failed, dropped. requestId={}", event.requestId(), e);
        }
    }
}
```

**중요 설계 규칙 (리서치 반영)**:
- `@Async` 리스너 예외는 **조용히 사라짐** → `AsyncUncaughtExceptionHandler` + 내부 try/catch 둘 다 필수
- AOP Aspect에서 `@Async` 메서드를 직접 호출 금지 — `ApplicationEventPublisher` 경유 (self-invocation 함정, see solution: `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md`)
- **Rejection policy는 `DiscardOldestPolicy`** — `CallerRunsPolicy`는 요청 스레드가 ES에 직접 기록하게 만들어 p99 spike를 유발 (audit 용도 critical anti-pattern)
- TaskExecutor: `core=2, max=4, queue=2000` (공유 ES heap 여유 확보)

### MDC 전파 — Spring Framework 6.1+ 표준 사용 (리서치 반영)

Spring Framework 6.1부터 제공되는 **`ContextPropagatingTaskDecorator`** 를 사용한다 (Micrometer ContextPropagation 기반). 커스텀 TaskDecorator를 작성할 필요 없음:

```java
@Configuration
@EnableAsync
public class LogAsyncConfig implements AsyncConfigurer {
    @Bean("logIndexerExecutor")
    public ThreadPoolTaskExecutor logIndexerExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("log-indexer-");
        executor.setRejectedExecutionHandler((r, ex) -> droppedCounter.increment());
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());  // MDC + tracing + security 전부
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Uncaught async exception in {}", method.getName(), ex);
    }
}
```

**Spring Boot 4 가상 스레드 주의사항 (리서치 결과)**:
- `spring.threads.virtual.enabled=true` 설정 시 auto-configured `applicationTaskExecutor`가 `SimpleAsyncTaskExecutor`(가상 스레드)로 전환되어 **`queueCapacity`/`rejectedExecutionHandler`가 무시됨**
- 로그 적재는 backpressure 제어가 필수이므로 **반드시 `@Async("logIndexerExecutor")`로 명시 지정** (공용 executor 사용 금지)

### RequestIdFilter

`OncePerRequestFilter` 상속. `JwtAuthenticationFilter` 앞에 등록:

```java
// ProdSecurityConfig / DevSecurityConfig 공통 수정
.addFilterBefore(new RequestIdFilter(), JwtAuthenticationFilter.class)
```

스케줄러/배치 유입은 requestId가 없으므로 `scheduled-{jobName}-{uuid}` 형식으로 수동 세팅하는 유틸 제공 (`LoggingContext.forScheduler(String)`).

### 관리자 식별 (화이트리스트 + 보안 강화)

```yaml
app:
  logging:
    admin:
      user-ids: ${ADMIN_USER_IDS:}  # 쉼표 구분 Long 리스트
```

```java
@ConfigurationProperties(prefix = "app.logging.admin")
@Validated
public record AdminProperties(
    @NotNull Set<@Positive Long> userIds
) {
    public AdminProperties {
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }
}
```

**보안 강화 결정 (리서치 반영)**:
1. **config bind 실패 시 앱 기동 실패** — 잘못된 포맷(`"1,abc,3"`) 입력 시 Spring Validation이 `BindException`을 던져 기동 중단
2. **빈 리스트 = 전면 차단 (fail-close)** — Interceptor에서 `userIds.isEmpty()` → 403
3. **userId는 오직 `SecurityContextHolder.getAuthentication().getPrincipal()`에서만 읽음** — `X-User-Id`, 쿼리 파라미터 등 기타 소스 금지
4. **Anonymous/null Authentication은 거부** — `/admin/**` 경로는 항상 인증된 userId를 전제

```java
@Component
@RequiredArgsConstructor
public class AdminGuardInterceptor implements HandlerInterceptor {
    private final AdminProperties props;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof Long userId) || !props.userIds().contains(userId)) {
            res.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }
}
```

- `HandlerInterceptor`를 `/api/admin/**`에 등록 (`WebMvcConfigurer.addInterceptors`)
- `ProdSecurityConfig`는 인증 필수(`.authenticated()`) 유지, 인가는 Interceptor에서 처리
- **Cloudflare Tunnel 환경**: `CF-Connecting-IP`만 신뢰할 client IP. `X-Forwarded-For`는 tunnel 내부에서 forgeable → `ForwardedHeaderFilter` + trusted-proxies에 Cloudflare CIDR 등록 필요 (또는 CF 헤더만 명시적 사용)

### ES 인덱스 설정 (Composable Index Template — ES 8.x 표준)

**legacy `_template` API는 ES 8.x에서 deprecated.** `_index_template`을 사용한다.

```bash
PUT _index_template/app-log
{
  "index_patterns": ["app-audit-*", "app-error-*", "app-business-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "refresh_interval": "30s"
    },
    "mappings": { ... }   // 공통 필드 + dynamic_templates로 payload.* 처리
  },
  "priority": 100
}
```

- 뉴스 인덱스 대비 쓰기량 ↑ 예상 → **`refresh_interval=30s`** 로 색인 비용 완화
- **단일 노드 → `number_of_replicas=0`** (데이터 유실 감수, PostgreSQL 원본이 있는 이벤트는 재인덱싱 가능)
- `action.destructive_requires_name: true` 클러스터 설정 추가 — **wildcard delete 사고 방지**
- `action.auto_create_index: true` (기본값) + `_index_template` → 존재하지 않는 월 인덱스로 write 시 템플릿 규칙으로 자동 생성

**월 경계 전략**:
- `MonthlyIndexNameResolver`는 항상 현재 `YearMonth.now(ZoneId.of("Asia/Seoul"))` 기반 이름 생성 (`app-audit-2026.04`)
- 매월 말 23:55 pre-create 스케줄러: `indexOps(IndexCoordinates.of(next)).create()` — 이미 존재하면 noop
- 실패 안전망: write 시점에 `exists()` 체크하여 없으면 즉시 `create()` → `save()`

**BulkIngester 사용 (ES 8.x — 리서치 결과)**:
- 기존 `BulkProcessor`는 ES 8.x Java API Client에 없음 → `co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester<T>` 사용
- Spring Data ES 6.x에는 래퍼가 없으므로 두 선택지 중 하나:
  - (A) `ElasticsearchOperations.bulkIndex(List<IndexQuery>, IndexCoordinates)` + 앱 레벨 버퍼 (간단)
  - (B) `ElasticsearchClient`를 별도 빈으로 꺼내 `BulkIngester`를 직접 사용 (세밀 제어)
- **추천: (A)**. `LogBatchBuffer` 클래스에서 5초/500건/5MB 중 하나 충족 시 flush
- `ElasticsearchOperations.indexOps(...).delete()` — 정확한 인덱스 이름만 전달 (wildcard 금지)

### Truncation & Masking (`LogSanitizer` 단일 클래스로 통합)

**크기 상한** (리서치 권고: 64KB는 과도 → 16KB):
- 문자열 필드 **2KB** (기존 4KB → 하향)
- 문서 전체 합계 **16KB** (기존 64KB → 하향). 챗봇 전문은 S3/파일 분리 저장 또는 요약만 기록
- 스택트레이스 **최상단 20줄**
- 초과 시 `originalSize` 기록 + `truncated=true` 플래그
- **마스킹을 truncation보다 먼저 적용** (잘린 시크릿이 부분 노출되는 것 방지)

**마스킹 규칙 (ReDoS-safe, case-insensitive, URL-decoded view 기준)**:

```java
public final class LogSanitizer {
    // ReDoS 방어: 입력 상한 8KB
    private static final int MAX_MASK_INPUT = 8192;

    // Pattern은 모두 static final — 컴파일 재사용
    private static final Pattern CRLF = Pattern.compile("[\\r\\n]");

    private record Rule(Pattern pattern, String replacement) {}

    // Possessive quantifier + bounded — catastrophic backtracking 회피
    private static final List<Rule> MASK_RULES = List.of(
        new Rule(Pattern.compile("(?i)Bearer\\s++[A-Za-z0-9\\-._~+/]{10,500}+=*+"), "Bearer ***"),
        new Rule(Pattern.compile("(?i)(authorization|cookie|set-cookie)\\s*+[:=]\\s*+\\S{1,500}+"), "$1: ***"),
        new Rule(Pattern.compile("\\b(sk|pk)_(live|test)_[A-Za-z0-9]{10,64}+"), "$1_$2_***"),
        new Rule(Pattern.compile("(?i)\"(password|passwd|token|api[_-]?key|secret|access[_-]?token|refresh[_-]?token|client[_-]?secret)\"\\s*+:\\s*+\"[^\"]{1,500}+\""), "\"$1\":\"***\""),
        new Rule(Pattern.compile("(?i)([?&](?:api[_-]?key|access[_-]?token|password|token)=)[^&\\s]{1,500}+"), "$1***"),
        new Rule(Pattern.compile("\\b\\d{6}+[-]\\d{7}+\\b"), "******-*******"),      // 주민번호
        new Rule(Pattern.compile("\\b01[016789][-\\s]?+\\d{3,4}+[-\\s]?+\\d{4}+\\b"), "010-****-****")
    );
}
```

**마스킹 적용 범위 확장 (보안 리뷰 반영)**:
- Exception message + 각 stack frame message 전체에도 적용 (`DataAccessException`의 JDBC URL에 `password=...` 포함 빈번)
- `@LogAudit(includeBody=true)` 사용 시: 시작 시 `LoginRequest`, `*Credential*`, `*Password*` 타입 파라미터 포함 메서드는 **컴파일/기동 스캔으로 차단**
- **CRLF 치환** (log injection 방지) — 사용자 입력이 들어올 수 있는 모든 문자열 필드

### 운영자 페이지 UI

경로: `/#/admin/logs` (기존 x-if 섹션 라우팅 방식). 관리자 userId가 아니면 서버 응답 403 → 프론트는 "접근 권한 없음" 메시지.

**구성 요소** (`js/components/admin-logs.js`):

| 영역 | 기능 |
|------|------|
| 상단 배지 | ES 디스크 사용률, 월별 인덱스 용량 (80% 초과 빨강) |
| 필터바 | 도메인(tab) · 기간(date range) · 키워드(300ms debounce) · userId · status · exceptionClass |
| 집계 차트 | Chart.js bar, 일간 카운트. 막대 클릭 시 해당 일자로 기간 필터 auto-set |
| 결과 목록 | 가상 스크롤 100건 단위, 행 클릭 시 payload 모달 |
| Payload 모달 | 2KB 프리뷰 + "원문 다운로드(JSON)" 버튼 |

**API**:
- `GET /api/admin/logs/{domain}?from=&to=&userId=&q=&status=&exceptionClass=&page=&size=`
  - `{domain}` 은 enum 화이트리스트(`audit|error|business`)로 검증, 직접 인덱스명에 concat 금지
  - 서버 강제: `size ≤ 100`, `to - from ≤ 90일`, ES `timeout=5s`
- `GET /api/admin/logs/{domain}/aggregations?from=&to=`
  - `calendar_interval` 자동 선택: ≤31d → day, ≤90d → week
  - `LogSearchService`에서 Caffeine 60s TTL 캐시 적용 (대시보드 새로고침에 의한 ES 연타 방지)
- `GET /api/admin/logs/disk-usage`
  - `_cluster/stats` + `_cat/indices?bytes=b`
  - 에러 응답은 ES 스택트레이스 미노출
- `GET /api/admin/logs/{domain}/download?from=&to=&...`
  - **서버 생성 filename**: `logs-{domain}-{yyyyMMddHHmmss}.json`. `Content-Disposition`에 사용자 입력 금지 (CRLF injection 방지)
  - `Content-Type: application/json`, `X-Content-Type-Options: nosniff`
  - 다운로드 행위 자체도 meta-audit 기록

**Meta-audit (SOC2 CC6.1 / ISO 27001 A.12.4.3 / NIST SP 800-53 AU-9(4) 패턴)**:
- `AdminGuardInterceptor.afterCompletion`에서 `DomainEventLogger.logAdminAccess(...)` 호출
- 전용 인덱스 `app-meta-audit-YYYY.MM` — **`DiscardOldestPolicy`를 우회하는 별도 소형 sync 경로** (메타 감사 유실 방지. 하드한 성능 요건 없음)
- Rate limit (Bucket4j): 관리자당 집계 쿼리 ≤ 1 req/5s

### Implementation Phases (심화 리뷰 반영 후 재배치)

#### Phase 1: Foundation (기반 인프라)

**목표**: 기동 가능한 스켈레톤 완성 + 단건 로그 1건 적재 확인.

- [x] `spring-boot-starter-aop` 의존성 추가 (`build.gradle`)
- [x] `logging/` 패키지 스켈레톤 (simplified 구조)
- [x] `RequestIdFilter` 구현 + Prod/Dev SecurityConfig 둘 다에 `addFilterBefore(..., JwtAuthenticationFilter.class)`
- [x] `@EnableAsync` + `LogAsyncConfig` — `logIndexerExecutor` + **`ContextPropagatingTaskDecorator`** + `AsyncUncaughtExceptionHandler`
- [x] `ApplicationLog` record (single domain model)
- [x] `LogDomain` enum
- [x] `LogIndexPort` (in `domain/service/`, news 패턴 일치)
- [x] `LogDocument` (`@Field(type=Date, format=date_time)`, `DateFormat.epoch_millis` 절대 사용 금지)
- [x] `MonthlyIndexNameResolver` (`YearMonth.now(Asia/Seoul)` 기반)
- [x] `LogElasticsearchIndexer.save(ApplicationLog)` — 단건 `bulkIndex()` 호출 1건
- [x] `LoggingElasticsearchConfig` — `@EnableElasticsearchRepositories(basePackages=...)` 또는 news config 공유
- [x] ES `_index_template` 등록 (startup에서 `IndicesClient.putIndexTemplate`) — 공유 설정 JSON 포함
- [x] 클러스터 설정 `action.destructive_requires_name: true`
- [ ] 단위 테스트: `MonthlyIndexNameResolver`, `LogSanitizer` (masker 규칙별 + CRLF + 입력 상한)
- [ ] (선택) `ArchUnit` 테스트 — ARCHITECTURE.md 레이어 규칙 고정

#### Phase 2: Capture + Bulk (3개 도메인 적재 + 배치 쓰기)

**목표**: 실제 요청 흐름에서 3개 도메인 로그가 자동/수동 적재되고 **배치로 묶여 쓰여짐** (Phase 3 지연 시 perf bomb 확정).

- [x] `@LogAudit` 애노테이션 (`application/annotation/`)
- [x] `ApplicationLoggingAspect` — `@Around(@within(@RestController))` + `@AfterThrowing` (C1 수정: pointcut 확장)
- [x] `LogEvent` record + `LogSanitizer` 통합 클래스
- [x] `LogEventListener` — 즉시 리스너(audit/error) + `@TransactionalEventListener(AFTER_COMMIT)` 리스너(business)
- [x] `DomainEventLogger` 서비스 (application 계층만 호출 허용, 규칙 주석 명시)
- [x] `GlobalExceptionHandler`에 `ApplicationEventPublisher.publishEvent(errorLogEvent)` 삽입 — AOP 누락분 이중 수집
- [x] **`LogBatchBuffer` — 5초/500건/5MB flush** (ES 8.x `BulkIngester` 또는 Spring Data ES `bulkIndex()` 버퍼링)
- [x] Controller에 `@LogAudit` 점진 적용 (운영자 API부터 → 전체 확산) — Aspect pointcut 이 `@within(@RestController)` 로 자동 적용
- [ ] 챗봇 응답 저장, 포트폴리오 생성/수정, 사용자 회원가입 등 핵심 비즈니스 이벤트 `DomainEventLogger` 호출 추가
- [ ] Integration test: 단일 요청 → 동일 requestId 3개 인덱스 적재 확인 (testcontainers)
- [ ] `/actuator/metrics/log.ingestion.dropped` Micrometer counter 노출

#### Phase 3: Retention & Reliability (장기 운영성) — 개인 프로젝트 규모

**목표**: 30일 운영해도 디스크/로그 유실 없음.

- [ ] `LogIndexScheduler` 통합 클래스 — cleanup + precreate
- [ ] 크론은 `application.yml`의 `scheduler.logging.cleanup.cron`, `scheduler.logging.precreate.cron`로 externalize (기존 `KeywordNewsBatchScheduler` 패턴과 일치)
- [ ] 현재 월 제외 로직 (삭제 대상 필터링), exact index name만 DELETE
- [ ] ES 장애 복원력: indexer 예외 catch → 로컬 WARN + drop 카운터
- [ ] **Graceful shutdown drain**: `@PreDestroy`에서 `LogBatchBuffer.flushRemaining()` + 10초 대기
- [ ] 스케줄러 컨텍스트: `LoggingContext.forScheduler("keyword-news-batch")` 등 기존 스케줄러에 적용

**연기 (Future Considerations로 이동)**:
- ~~`/actuator/health` JVM heap degraded 연동~~ — 개인 프로젝트엔 health를 감시하는 오케스트레이터 없음. 운영자 페이지 배지로 대체
- ~~ES 디스크 `HealthIndicator`~~ — 운영자 페이지 상단 배지만으로 가시성 확보

#### Phase 4: Admin UI (운영자 페이지) — 엔터프라이즈 항목 연기

**목표**: 브라우저로 로그 조회/집계/다운로드. **개인 단일 관리자(태형님)** 전제.

- [ ] `AdminProperties` + `@Validated` + `@ConfigurationProperties` — bind 실패 시 기동 실패
- [ ] `AdminGuardInterceptor` (userId from `Authentication` principal only) + `WebMvcConfigurer` 등록
- [ ] Meta-audit — **기존 async 경로 재사용** (개인 프로젝트이므로 별도 sync 분리는 YAGNI)
- [ ] `LogElasticsearchSearcher` (NativeQuery — news 패턴 재사용), `search_after` 페이징
- [ ] `LogSearchService` + Caffeine 60s 집계 캐시
- [ ] `AdminLogController` — 검색 / 집계 / 디스크 / 다운로드 통합
- [ ] 다운로드 filename 서버 생성 (`logs-{domain}-{yyyyMMddHHmmss}.json`), `X-Content-Type-Options: nosniff`
- [ ] `index.html`에 `/#/admin/logs` x-if 섹션 + Alpine.js `x-text` 사용 (`x-html` 금지)
- [ ] `js/components/admin-logs.js` — 필터(300ms debounce) / 목록 / 차트 / 모달 / 디스크 배지
- [ ] 403 응답 처리: "관리자 권한 필요" 화면

**연기 (Future Considerations로 이동)**:
- ~~Bucket4j rate limit~~ — 단일 관리자이므로 자기 자신에 대한 DoS 방어는 의미 희석
- ~~CSP strict 헤더~~ — 관리자 페이지만 strict 적용 복잡도 대비 이득 낮음
- ~~Credential 타입 파라미터 기동 스캔~~ — 마스킹 + `includeBody=false` 기본값으로 대체, 스캔은 향후 코드베이스 커졌을 때 추가

## Alternative Approaches Considered

(see brainstorm: `docs/brainstorms/2026-04-20-elasticsearch-application-logging-brainstorm.md#why-this-approach`)

| 대안 | 채택 안 한 이유 |
|------|----------------|
| Logback ES Appender | "선별적 로깅" 요구와 부적합. 레벨 필터만으로는 비즈니스 이벤트 구분 불가. 구조화 약함. Appender 라이브러리 유지보수 취약 |
| Filebeat Sidecar | 단일 서버 3GB ES 제약 하 컨테이너 추가가 과도. 개인 프로젝트 규모에 오버엔지니어링 |

## System-Wide Impact

### Interaction Graph

```
HTTP Request
 └─► RequestIdFilter (NEW)          → MDC["requestId"] = UUID, 응답헤더 세팅
     └─► JwtAuthenticationFilter    → SecurityContext 주입
         └─► Spring MVC DispatcherServlet
             └─► HandlerInterceptor (AdminGuardInterceptor - /admin/**만)
                 └─► Controller method
                     └─► @LogAudit Aspect @Around (NEW)
                         ├─► 메서드 호출 (정상)
                         │   └─► Application/Domain 코드
                         │       └─► (optional) DomainEventLogger.log(...) 
                         │           └─► ApplicationEventPublisher.publishEvent(BusinessLogEvent)
                         │               └─► [비동기] LogEventListener @Async
                         │                   └─► LogIndexPort.save() → ES
                         └─► @Around finally block
                             └─► ApplicationEventPublisher.publishEvent(AuditLogEvent)
                                 └─► [비동기] LogEventListener → ES
                     └─► @AfterThrowing (예외 시)
                         └─► publishEvent(ErrorLogEvent)
                             └─► [비동기] LogEventListener → ES
             └─► GlobalExceptionHandler → 응답 생성 (로깅은 AOP가 이미 수행)
```

### Error & Failure Propagation

| 레이어 | 예외 클래스 | 처리 위치 | 로깅 방식 |
|--------|------------|----------|-----------|
| Controller | `UserDomainException`, `*ApiException` | `GlobalExceptionHandler` (응답) + AOP `@AfterThrowing` (로깅) | ErrorLogEvent 1건 |
| Async Listener | ES 장애 (`NoNodeAvailableException` 등) | `LogEventListener` 내 try-catch | 로컬 WARN + dropped 카운터 ↑ |
| Scheduler | 인덱스 삭제 실패 | Scheduler 내부 catch + WARN | ES 에러 자체는 기록 안 함 (재귀 방지) |
| RequestIdFilter | 이론상 실패 없음 | — | — |
| AOP Aspect | Aspect 자체 예외 | Aspect 내 try-catch, 원본 메서드 결과 불변 | 로컬 ERROR만 |

**핵심 원칙**: **로깅 실패가 본 기능 실패를 유발해서는 안 된다.** 모든 Aspect/Listener는 `try { ... } catch (Exception e) { log.warn(...) }` 패턴 준수.

### State Lifecycle Risks

- **월 경계 race**: `2026-04-30 23:59:59`에 시작된 요청이 `2026-05-01 00:00:00` 이후 로그 publish → 인덱스 이름은 5월로 결정. pre-create가 없으면 쓰기 실패. **대응**: `MonthlyIndexPrecreateScheduler` + `IndicesClient.exists` 존재 확인 후 write
- **30일 경계 삭제 race**: 03:00 삭제와 동일 인덱스에 대한 쓰기 충돌 가능성. 삭제 대상은 "월"이고 현재 월은 항상 제외이므로 실질적 충돌 없음 (31일차 쓰기 가능)
- **큐 포화 시 유실**: `DiscardOldestPolicy` 하에서 로그 유실 발생. 드롭 카운터로 가시화. 로컬 파일 fallback은 YAGNI (이번 범위 제외)

### API Surface Parity

- **변경 영향 없음**: 기존 public API 시그니처·동작 무변경
- **신규 public API**: `/api/admin/logs/*` (관리자 전용)
- **신규 응답 헤더**: 모든 응답에 `X-Request-Id` 추가

### Integration Test Scenarios

1. **단일 요청 로그 일관성**: 동일 요청 내 audit + error 문서의 `requestId`가 동일한지
2. **비동기 MDC 전파**: @Async 스레드에서 기록된 로그에도 `requestId`가 보존되는지
3. **ES 다운 상황**: 테스트 컨테이너 ES 중단 → API 요청 정상 200, dropped 카운터 +1
4. **월 경계 인덱스**: 시스템 시계 조작 → 00:00:00 직후 요청이 새 월 인덱스에 쓰여지는지
5. **관리자 화이트리스트**: 비관리자 userId로 `/api/admin/logs` 호출 시 403, meta-audit 기록 안 됨. 관리자 호출 시 200 + meta-audit 1건

## Acceptance Criteria

### Functional Requirements

- [ ] `RequestIdFilter`가 `JwtAuthenticationFilter` 앞단에서 UUID를 MDC + 응답 헤더 `X-Request-Id`에 세팅
- [ ] `@LogAudit`가 적용된 Controller 메서드 호출 시 `app-audit-YYYY.MM` 인덱스에 1건 생성
- [ ] 예외 발생 시 `app-error-YYYY.MM` 인덱스에 1건 생성, 스택트레이스 ≤ 20줄
- [ ] `DomainEventLogger.log(...)` 호출 시 `app-business-YYYY.MM` 인덱스에 1건 생성
- [ ] 문자열 필드 4KB 초과 시 잘리고 `truncated=true` + `originalSize` 기록
- [ ] 문서 전체 64KB 초과 시 동일 처리
- [ ] `scheduler` 실행에서는 `requestId = "scheduled-{jobName}-{uuid}"` 형식으로 기록
- [ ] 매일 03:00에 30일 경과 월 인덱스 DELETE
- [ ] 매월 말 23:55에 다음달 인덱스 pre-create
- [ ] `/api/admin/logs/{domain}` 검색 API 페이징/필터 정상 동작
- [ ] `/api/admin/logs/{domain}/aggregations` date_histogram 집계 반환
- [ ] `/api/admin/logs/disk-usage` ES 디스크 사용률 반환
- [ ] 관리자 userId 화이트리스트 외 사용자 접근 시 403
- [ ] 화이트리스트 빈 리스트 → 전면 차단 (fail-close)
- [ ] 관리자의 `/admin/logs` 접근 자체가 `adminAction=true` meta-audit 1건 생성
- [ ] 운영자 페이지 필터: 도메인/기간/키워드(300ms debounce)/userId/status/exceptionClass
- [ ] 차트 막대 클릭 시 해당 일자로 기간 필터 자동 적용
- [ ] Payload 모달 2KB 프리뷰 + 원문 JSON 다운로드

### Non-Functional Requirements (개인 프로젝트 규모에 맞춰 재조정)

**성능**
- [ ] 로그 적재 비동기화로 Controller 응답시간 영향 < 1ms (간단 마이크로벤치)
- [ ] Phase 2 종료 시점에 `LogBatchBuffer` 5s/500/5MB 기준 동작 확인 (단건 적재 금지)
- [ ] 큐 포화 시 `DiscardOldestPolicy` 적용, 드롭 카운터가 `/actuator/metrics/log.ingestion.dropped`에 노출
- [ ] 관리자 페이지 집계 쿼리는 Caffeine 60s 캐시 적용 후 반복 요청이 ES로 전파되지 않음
- [ ] 관리자 집계 쿼리 window clamp `to - from ≤ 90일` + ES `timeout=5s`

**크기 / 데이터 위생**
- [ ] 문자열 필드 **2KB** 초과 시 `truncated=true` + `originalSize` 기록
- [ ] 문서 전체 **16KB** 초과 시 동일 처리
- [ ] 스택트레이스 ≤ 20줄
- [ ] **Timestamp 규약**: AOP `@Around` 진입 시점 / `publishEvent` 직전에 `Instant.now()` 로 고정. 리스너에서 재할당 금지
- [ ] **인덱스 라우팅은 문서 `timestamp`의 UTC 연월 기반** (KST 기반 명명과 UTC 저장의 경계 불일치 회피)

**보안 / 마스킹 (최소 방어선)**
- [ ] config bind 실패(`ADMIN_USER_IDS="1,abc"`) → app 기동 실패
- [ ] 화이트리스트 빈 리스트 / null principal / AnonymousAuthenticationToken → 403
- [ ] `AdminGuardInterceptor`는 userId를 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`에서만 읽음
- [ ] 마스킹 단위 테스트: 대표 5종 통과 — `Bearer ...`, `Authorization: ...`, `"password":"..."` (case-insensitive), `?token=...`, 주민번호
- [ ] 마스킹이 truncation보다 먼저 실행
- [ ] Exception message + stack frame message에도 마스킹 적용
- [ ] `@LogAudit(includeBody)` 기본값 = `false`
- [ ] 다운로드 endpoint: filename 서버 생성, `Content-Disposition`에 사용자 입력 주입 불가
- [ ] Alpine.js 렌더링: payload는 `x-text` 또는 `<pre>`, `x-html` 사용 없음
- [ ] CRLF 치환 적용
- [ ] `action.destructive_requires_name: true` 클러스터 설정 적용

**운영성**
- [ ] `ADMIN_USER_IDS` 환경변수 누락 → fail-close (403)
- [ ] ES 디스크 사용률 > 85% 시 운영자 페이지 상단 배지 경고 표시
- [ ] **Graceful shutdown**: `@PreDestroy`에서 `LogBatchBuffer.flushRemaining()` + 최대 10초 대기

### Quality Gates

- [ ] Aspect/Listener/Truncator/Masker 단위 테스트 ≥ 80% 라인 커버
- [ ] `ARCHITECTURE.md` 레이어 규칙(DTO/Entity/Domain 경계, 의존성 방향) 위반 없음
- [ ] Lombok 규칙 준수 (수동 getter/setter 없음)
- [ ] `CLAUDE.md` Entity 규칙 준수 (ID 기반 참조, 연관관계 금지)

## Success Metrics

- **운영 가시성**: 배포 1주 후, 운영자 페이지에서 에러 건수·사용자별 활동·응답시간 p95를 **별도 도구 없이 조회 가능**
- **MTTR 단축**: 에러 재현 리포트 받았을 때, requestId 또는 userId + 시간대로 5분 내 관련 로그 풀 세트 확보
- **적재 안정성**: 30일 운영 후 드롭 카운터 < 일일 로그량의 0.1%

## Dependencies & Prerequisites

### 런타임 의존성
- Elasticsearch 8.x (기존 뉴스 검색용 인스턴스 공유)
- `spring-boot-starter-aop` (신규)
- 기존: `spring-boot-starter-data-elasticsearch`, Spring Security, Lombok

### 설정 의존성
- `ADMIN_USER_IDS` 환경변수 (`.env`)
- `application.yml`에 `spring.task.execution.*`, `app.admin.*`, `app.logging.*` 추가

### 선행 작업
- 없음. 현재 상태에서 바로 착수 가능.

## Risk Analysis & Mitigation

| 위험 | 영향도 | 완화 |
|------|-------|------|
| ES 쓰기 부하로 뉴스 검색 지연 | 중 | `refresh_interval=30s`, `replicas=0`, **BulkIngester/bulkIndex 버퍼 Phase 2부터** (5s/500/5MB). 단일 insert는 600 req/s에서 노드 포화 |
| AOP 도입 첫 시도 → 프록시 이슈 | 중 | Controller에 `final` 금지 규칙, self-invocation 단위 테스트. `@Async`는 별도 빈 분리 |
| @Async 리스너 예외 silent drop | 고 | `AsyncUncaughtExceptionHandler` 필수. 리스너 내부 try/catch 이중 방어. 누락 시 ES 장애 시 audit 전체 유실 |
| MDC 비동기 전파 실수 | 중 | Spring 6.1+ `ContextPropagatingTaskDecorator` 사용 (커스텀 불필요). integration test로 requestId 일관성 검증 |
| LocalDateTime/epoch_millis 직렬화 함정 | 고 | `Instant` + `@Field(format=date_time)` 고정. `epoch_millis` 금지 (Spring Data ES #2318) |
| Spring Boot 4 가상 스레드 + queue/reject policy 무시 | 고 | `@Async("logIndexerExecutor")` 명시 지정, 공용 executor 금지 |
| 큐 포화 시 로그 유실 | 중 | `DiscardOldestPolicy` + Micrometer 드롭 카운터. Meta-audit는 별도 sync 경로로 분리 |
| 민감정보 로그 노출 | 고 | `LogSanitizer` 확장 규칙(authorization/cookie/api_key/token 키) + `@LogAudit(includeBody=false)` 기본 + credential 타입 파라미터 startup 스캔 차단 |
| 화이트리스트 fail-open | 고 | @Validated bind 실패 → 기동 실패. Anonymous/null Authentication 403. 빈 리스트 전면 차단 test |
| 화이트리스트 userId forgery | 고 | `SecurityContextHolder` 만 신뢰, header/param 금지 |
| Cloudflare Tunnel에서 X-Forwarded-For 위조 | 중 | `CF-Connecting-IP`만 사용 또는 trusted-proxies 설정 |
| 월 경계 인덱스 미생성 | 중 | `_index_template` + `auto_create_index` + pre-create 스케줄러 + `exists` 방어망 |
| Wildcard delete 사고 | 고 | Exact index name만 전달. `action.destructive_requires_name: true` |
| 관리자 UI XSS (payload 렌더) | 고 | Alpine.js `x-text` 강제, `x-html` 금지. payload는 `<pre>` 내 JSON. CSP + nosniff |
| Admin 집계 쿼리 DoS | 중 | window clamp 90d, ES `timeout=5s`, per-admin rate limit 1/5s, Caffeine 60s 캐시 |
| 2GB ES heap 공유로 GC 급증 | 중 | JVM old-gen 75% 알림, bulk flush 간격 고정, 인덱스 replicas=0 |
| `destructive_requires_name` 미설정 상태 존재 | 고 | Phase 1에서 클러스터 설정 즉시 적용, integration test 포함 |
| 30일 경계 race (삭제 중 write) | 저 | 현재 월은 절대 삭제 대상 미포함 (월 단위 인덱스라 사실상 충돌 없음) |
| 관리자 DoS로 audit 유실 → 공격 흔적 은폐 | 중 | Meta-audit을 main 큐와 분리, endpoint rate limit |

## Future Considerations

- **알림 연동**: 에러율 임계치 초과 시 Gmail 알림 (기존 `notification` 모듈 활용)
- **분산 트레이싱**: `requestId`만 사용 → 추후 Trace/Span ID로 확장. OpenTelemetry 도입
- **롤업 인덱스**: 차트 집계가 느려지면 `app-rollup-daily` 사전 집계 인덱스 추가
- **Circuit Breaker**: ES 장애 장기화 시 Resilience4j 도입
- **DB 기반 관리자 테이블**: runtime 권한 변경이 필요해지면 `users.role` + Caffeine 캐싱
- **Bucket4j rate limit**: 관리자가 2명+로 확장되면 적용
- **Credential 타입 파라미터 기동 스캔**: 팀 확대 시 부주의 방어 장치
- **JVM/ES HealthIndicator**: 오케스트레이터(k8s/ECS) 도입 시
- **Cloudflare `CF-Connecting-IP` 전용 파싱**: `clientIp`를 로그 payload에 포함하기로 결정되는 시점에 추가
- **ArchUnit 레이어 테스트**: 팀 협업 시 자동 규칙 고정
- **Snapshot 백업**: 감사 법적 요건 도입 시 S3 snapshot 전략
- **CSP strict 헤더**: 관리자 페이지 별도 도메인 운영 시

## Documentation Plan

- [ ] `CLAUDE.md` 프로젝트 개요의 도메인 목록에 `logging` 추가
- [ ] `ARCHITECTURE.md` 필요 시 신규 도메인의 예외적 배치(infrastructure 횡단) 근거 추가
- [ ] `docs/solutions/`에 구현 중 부딪힌 이슈 기록 (예: AOP 자기호출, MDC 전파)
- [ ] `README` 또는 별도 운영 문서: 관리자 화이트리스트 설정법, 인덱스 수동 관리 명령

## Sources & References

### Origin

- **Brainstorm**: [docs/brainstorms/2026-04-20-elasticsearch-application-logging-brainstorm.md](../brainstorms/2026-04-20-elasticsearch-application-logging-brainstorm.md)
  - 카테고리 3분할 (audit/error/business)
  - 접근법 A (코드 레벨 통합) 채택 근거
  - 관리자 userId 화이트리스트, RequestIdFilter, truncation 정책, 풀옵션 UI 결정

### Internal References

- 기존 ES 통합 표본:
  - `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/document/NewsDocument.java:16`
  - `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/config/NewsElasticsearchConfig.java:14`
  - `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/NewsElasticsearchIndexer.java:23`
  - `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/NewsElasticsearchSearcher.java:43`
- Security/JWT: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/config/ProdSecurityConfig.java:56`, `.../jwt/JwtAuthenticationFilter.java:19`
- 전역 예외 처리: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java:25`
- 기존 스케줄러 패턴: `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/scheduler/KeywordNewsBatchScheduler.java:13`
- Frontend: `src/main/resources/static/index.html`, `src/main/resources/static/js/components/news-search.js`
- 이전 플랜: [docs/plans/2026-04-12-001-feat-elasticsearch-news-search-plan.md](2026-04-12-001-feat-elasticsearch-news-search-plan.md)

### Institutional Learnings (반드시 회피)

- **LocalDateTime 직렬화 함정**: [docs/solutions/integration-issues/elasticsearch-localdate-serialization-mismatch-2026-04-20.md](../solutions/integration-issues/elasticsearch-localdate-serialization-mismatch-2026-04-20.md) — 타임스탬프는 `Instant` + ISO 8601 full
- **AOP self-invocation 함정**: [docs/solutions/architecture-patterns/global-indicator-history-mirroring.md](../solutions/architecture-patterns/global-indicator-history-mirroring.md) — @Async는 별도 Bean으로 분리 필수
- **배치 쓰기 성능**: [docs/solutions/architecture-patterns/deposit-history-n-plus-one-batch-pattern.md](../solutions/architecture-patterns/deposit-history-n-plus-one-batch-pattern.md) — 단건 적재 ≠ 배치, BulkIngester/bulkIndex 필수
- **집계 쿼리 비용**: [docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md](../solutions/architecture-patterns/ecos-timeseries-chart-visualization.md) — date_histogram 집계 + 복합 인덱스 설계

### External References (심화 리서치 결과)

**Spring Framework / Spring Boot**
- Spring Framework Task Execution — https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- ContextPropagatingTaskDecorator Javadoc — https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/task/support/ContextPropagatingTaskDecorator.html
- Spring Boot Virtual Threads Task Execution — https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html
- Transaction-bound Events — https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
- Spring Proxying Mechanisms (CGLIB vs JDK) — https://docs.spring.io/spring-framework/reference/core/aop/proxying.html

**Elasticsearch 8.x**
- Date math index names — https://www.elastic.co/guide/en/elasticsearch/reference/current/date-math-index-names.html
- Bulk API (Java API Client) — https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/indexing-bulk.html
- Size your shards — https://www.elastic.co/docs/deploy-manage/production-guidance/optimize-performance/size-shards
- Fix watermark errors — https://www.elastic.co/guide/en/elasticsearch/reference/8.19/fix-watermark-errors.html
- Spring Data ES `epoch_millis` string serialization bug — https://github.com/spring-projects/spring-data-elasticsearch/issues/2318
- Index template date-math alias limitation — https://github.com/elastic/elasticsearch/issues/75651

**Security / Privacy**
- OWASP Logging Cheat Sheet — https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html
- OWASP Top 10:2025 A09 — https://owasp.org/Top10/2025/A09_2025-Security_Logging_and_Alerting_Failures/
- CERT IDS03-J Do not log unsanitized input — https://wiki.sei.cmu.edu/confluence/display/java/IDS03-J.+Do+not+log+unsanitized+user+input
- CWE-117 Log injection — https://www.veracode.com/security/java/cwe-117/
- NIST SP 800-53 AU family — https://csrc.nist.gov/publications/detail/sp/800-53/rev-5/final
- Preventing ReDoS — https://www.regular-expressions.info/redos.html

### Conventions

- `CLAUDE.md` — Entity 연관관계 금지, Lombok 규칙, 분석/설계 문서 프로세스
- `ARCHITECTURE.md` — 레이어 규칙, DTO/Entity/Domain 경계, 트랜잭션 경계 (이번 기회에 `logging/` 도메인 항목 추가 필요)