# Elasticsearch 기반 애플리케이션 로깅 시스템 브레인스토밍

**작성일**: 2026-04-20
**작성자**: 태형
**상태**: Brainstorming

---

## What We're Building

뉴스 검색용으로 도입된 Elasticsearch 인스턴스를 활용해, 선별적 애플리케이션 로그(에러/예외, API 감사, 비즈니스 이벤트)를 구조화된 문서 형태로 적재하고 운영자 페이지에서 조회/검색하는 로깅 시스템을 도입한다.

**핵심 요구사항**
- 기존 ES 인스턴스 공유 (별도 인덱스로 분리)
- 운영자 페이지(Alpine.js + Tailwind) 직접 구현 — Kibana 미사용
- 선별적 로깅: API 요청 감사 + WARN/ERROR 예외 + 명시적 비즈니스 이벤트
- 30일 보관
- 기존 log.info/debug 콘솔 로깅은 유지

**범위 밖 (YAGNI)**
- 모든 레벨 로그의 ES 적재 (DEBUG/INFO 콘솔은 기존 유지)
- 복잡한 시각화 대시보드 (Kibana 수준) / 로그 기반 알람
- 멀티 서비스/분산 트레이싱 (`requestId`는 남기되 Trace/Span은 미도입)

---

## Why This Approach

**접근법 A — 코드 레벨 통합 (Spring AOP + 커스텀 로거 서비스)** 선택.

| 대안 | 채택 안 한 이유 |
|------|----------------|
| B. Logback ES Appender | "선별적" 요구와 부적합. 레벨 필터만으론 비즈니스 이벤트 구분 불가. Appender 라이브러리 유지보수 약함. 구조화 제한. |
| C. Filebeat Sidecar | 단일 서버에서 3GB 제한 ES가 있는 개인 프로젝트 규모엔 오버엔지니어링. 추가 컨테이너 메모리 부담. |

**A가 맞는 이유**
- 기존 Spring Data Elasticsearch 패턴(`NewsDocument`, `NewsElasticsearchRepository`)과 일관성 있게 확장 가능
- 추가 인프라 제로 — 현재 리소스 제약과 맞음
- 선별적·구조화 로깅에 정확히 부합
- 운영자 페이지에서 ES Repository를 그대로 조회할 수 있어 구현 단순

---

## Key Decisions

### 1. 로깅 대상을 3가지 도메인으로 분류

| 도메인 | 인덱스 | 수집 방식 | 예시 |
|--------|--------|-----------|------|
| **API 감사 (audit)** | `app-audit-YYYY.MM` | `@LogAudit` AOP (Controller 메서드) | URI, method, userId, status, durationMs |
| **에러/예외 (error)** | `app-error-YYYY.MM` | `@RestControllerAdvice` 전역 핸들러 + 선택적 AOP | exceptionClass, message, stacktrace(요약), context |
| **비즈니스 이벤트 (business)** | `app-business-YYYY.MM` | `DomainEventLogger` 서비스 명시 호출 | eventType, userId, payload (포트폴리오 변경, 챗봇 질의 등) |

### 2. 비동기 적재로 요청 스레드 블로킹 회피

- `@Async` + `ApplicationEventPublisher`로 로깅 이벤트 발행
- ES 장애 시 앱 기능에 영향 없도록 fallback: 예외 삼키고 로컬 WARN 로그만 남김
- 배치 처리는 YAGNI — 단일 이벤트 단위로 적재 시작

### 3. 월 단위 인덱스 + 스케줄러 기반 30일 경과 삭제

- 인덱스명 패턴: `app-{domain}-2026.04`
- 매일 새벽 정리 스케줄러: 30일 경과 인덱스 DELETE
- ILM(Index Lifecycle Management) 미사용 — 단순 스케줄러로 충분

### 4. 공통 로그 문서 구조

모든 도메인 문서는 공통 필드 + 도메인 전용 `payload` 구조.

- 공통: `timestamp`, `domain`, `userId`, `requestId`
- `payload`: 도메인별 자유 필드 (audit/error/business 각각)
- 인덱스명은 문서 저장 시점에 결정되는 동적 라우팅 전략

### 5. RequestId 전파

- `RequestIdFilter`를 `JwtAuthenticationFilter` 앞에 등록
- UUID 생성 → MDC 주입 → 응답 헤더 `X-Request-Id` 노출
- 모든 로그 문서에 `requestId` 필드로 포함되어 동일 요청 흐름 추적 가능

### 6. 운영자 페이지 UI

- 경로: `/admin/logs` (기존 운영자 페이지 흐름에 맞춤)
- 접근 제어: 관리자 userId 화이트리스트(설정값) 기반 커스텀 체크
- 필터 (풀옵션): 도메인, 기간, 키워드, userId, status, exceptionClass
- payload 상세 모달 + **도메인별 일간 카운트 차트**(간단 Bar/Line, 복잡 시각화 제외)
- 상단 ES 디스크/인덱스 사용률 배지 (80% 초과 시 경고 색상)
- ES query DSL 서버에서 조립, Alpine.js 페이징

### 7. Truncation 정책

- 문자열 필드 4KB 상한, 문서 합계 64KB 상한 → `...[truncated]` 표기
- 스택트레이스 최상단 20줄(설정 가능)만 유지
- 유틸 클래스로 일관 적용 (상세는 plan 단계)

---

## Open Questions

*(모두 해결됨)*

---

## Resolved Questions

- 인증/인가 정책 → 관리자 userId 화이트리스트 (Decision 6)
- RequestId 생성 위치 → `RequestIdFilter` 도입 (Decision 5)
- 스택트레이스 저장 정책 → 최상단 20줄 (Decision 7)
- payload 크기 상한 → 필드 4KB / 문서 64KB (Decision 7)
- ES 디스크 모니터링 → 운영자 페이지 배지만, 알람 없음 (Decision 6)
- 초기 대시보드 범위 → 풀옵션 필터 + 간단 차트 (Decision 6)