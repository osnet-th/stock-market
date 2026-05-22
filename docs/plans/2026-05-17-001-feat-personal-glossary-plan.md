---
title: "feat: 개인 용어 사전 (Glossary) MVP"
type: feat
status: active
date: 2026-05-17
deepened: 2026-05-17
origin: docs/brainstorms/2026-05-17-glossary-requirements.md
issue: 43
---

# feat: 개인 용어 사전 (Glossary) MVP

## Overview

사용자가 모르는 경제/주식 용어를 본인 사전에 정리하고 카테고리로 묶어 다시 찾아볼 수 있는 신규 도메인 `glossary/` 를 추가한다.

- 백엔드: Spring Boot DDD 4-layer 패키지 신설 (`glossary/{domain, application, infrastructure/persistence, presentation}`)
- 프론트: 사이드바 신규 메뉴 "용어 사전" + 정적 컴포넌트 `glossary.js`
- 보안: 모든 API 인증 필수 + application 계층 `findByIdAndUserId(...).orElseThrow(...)` 강제 + 타사용자 리소스 접근은 404 통일 (`newsjournal/` 컨벤션 답습)

## Problem Frame

이슈 #43 본문은 "경제/주식 등 용어를 정리하는 기능"을 요청. brainstorm에서 **사용자별 개인 사전**(공유 X, 관리자 큐레이션 X)로 좁혔다. 가장 큰 리스크는 외부 진입점/LLM 보조 미포함으로 인한 채택 마찰 — leading indicator로 후속 이터레이션 결정 (see origin: docs/brainstorms/2026-05-17-glossary-requirements.md).

## Requirements Trace

용어 CRUD
- R1. 용어 등록 (용어명 필수, 설명·카테고리 선택; 카테고리 미지정 → null = 미분류) → Unit 2, 3
- R2. 본인 용어 조회/수정/삭제 (PUT 전체 교체) → Unit 3, 4
- R3. 타사용자 접근 차단; application 계층 userId 기반 ownership 검증 → Unit 3, 4

카테고리 관리
- R4. 본인 카테고리 CRUD, `(user_id, name)` unique → Unit 1, 2, 3
- R5. "미분류" = 카테고리 null 가상 표현; 일반 카테고리명으로 "미분류" 생성/리네임 금지 (예약어) → Unit 2, 3
- R6. 일반 카테고리 삭제 시 자식 용어 `category_id` 를 null로 일괄 update + 카테고리 row 삭제를 **단일 트랜잭션**으로 → Unit 3

탐색/검색
- R7. 용어명 LIKE 부분 일치 (대소문자 무시 = PostgreSQL ILIKE) → Unit 2, 3
- R8. 설명 LIKE 부분 일치 (MVP는 LIKE only) → Unit 2, 3
- R9. 카테고리 필터; R7/R8과 AND 조합 → Unit 2, 3
- R10. 등록일순(default 최신)/가나다순 정렬, secondary tie-breaker `id` → Unit 2, 3
- R11. 페이지네이션 (page=0, size=20, max=200) → Unit 2, 4

화면 상태
- R12. Empty state (0건 등록 / 검색 결과 0 / 카테고리 0개) → Unit 5
- R13. 등록/수정 폼에서 카테고리 인라인 생성 (`find-or-create` 의미론) + R4/R14 검증 동일 → Unit 3, 4, 5

보안/입력 검증
- R14. 길이 상한 (category.name=50, term.name=200, term.definition=TEXT + DTO `@Size(max=4000)`); plain text 저장·렌더링 → Unit 1, 4, 5
- R15. R7/R8 검색 입력 LIKE 와일드카드(`%`, `_`, `\`) 이스케이프 + 입력 길이 상한 → Unit 3

## Scope Boundaries

- 사용자 간 공유/공개 사전 미포함 (Entity에 visibility 컬럼 미도입; 향후 마이그레이션으로 확장 가능)
- LLM 자동 설명 생성/제안 미포함
- 챗봇 컨텍스트 주입, 뉴스 화면 연동 등 다른 기능과의 연계 미포함
- 출처(URL)/메모/태그/다국어 미포함
- 자동화 테스트는 명시적 요청 시에만 작성 (CLAUDE.md 정책). 본 plan은 구현 완성도만 다루며 테스트 시나리오는 각 unit에 enumerate 만 한다.

## Context & Research

### Relevant Code and Patterns

원본 worktree 기준: `/Users/app/Documents/subProject/wt-feat-issue-43-glossary/`

- 아키텍처: `ARCHITECTURE.md` §3-§5 — domain은 Spring/JPA 의존 금지, Entity 연관관계 금지 (ID 참조만), application만 `@Transactional`
- 코드 컨벤션: `docs/policies/code-convention.md` — 메서드 5줄/중첩 1단계 기본, guard clause + 조기 반환
- `newsjournal/` 도메인 1차 참조 모델 (Key Decisions에 명시):
  - `newsjournal/domain/model/NewsEventCategory.java` — Lombok `@Getter` + factory `create()` + `assignId()` 패턴
  - `newsjournal/infrastructure/persistence/NewsEventCategoryEntity.java` — `@Table(uniqueConstraints={@UniqueConstraint(...columnNames={"user_id","name"})})`, `@Column(name="user_id", nullable=false)`, `@Column(name="name", nullable=false, length=50)`
  - `newsjournal/application/NewsEventCategoryService.java` — `resolve(userId, name)` find-or-create
  - `newsjournal/application/NewsEventWriteService.java` — `@Transactional` + `findByIdAndUserId(...).orElseThrow(...)` ownership
  - `newsjournal/application/exception/NewsEventNotFoundException.java` — JavaDoc에 "IDOR 방지 위해 403 대신 404" 명시 ⇒ deferred Q "404 vs 403" 자동 해소
  - `newsjournal/presentation/NewsJournalSecurityContext.java` — `Authentication.principal instanceof Long` 가드 + `currentUserId()`
  - `newsjournal/presentation/NewsJournalExceptionHandler.java` — `@RestControllerAdvice(assignableTypes={...})`, 응답 shape `{error, message, timestamp}` + `fieldErrors`
  - `newsjournal/domain/repository/NewsEventListFilter.java` — record + compact constructor 검증 (`page>=0, 1<=size<=200`). **위치 주의**: repository 포트 입력이므로 domain 패키지에 위치 (application/dto 아님)
  - `newsjournal/application/dto/NewsEventListResult.java` — `record(items, totalCount, page, size)`
  - `newsjournal/infrastructure/persistence/NewsEventRepositoryImpl.java` — `PageRequest.of(page, size)`, JPQL `(:x IS NULL OR e.x = :x)` 옵셔널 필터, 안정 정렬 `ORDER BY x DESC, id DESC`
- 길이 컨벤션:
  - 짧은 이름: length=50 (`NewsEventCategoryEntity.name`)
  - 중간 제목: length=200 (`NewsEventEntity.title`)
  - 긴 본문: `columnDefinition="TEXT"` (`NewsEventEntity.what/why/how`) + DTO `@Size(max=4000)` (`CreateNewsEventRequest`)
- 응답 컨벤션: 201 + `Location: /api/.../{id}` + body `Map.of("id", id)`, 갱신/삭제는 204
- 글로벌 fallback: `infrastructure/web/GlobalExceptionHandler` (`DataIntegrityViolationException` → 409)
- 프론트 사이드바 등록 위치 (7곳 동기 변경):
  - `src/main/resources/static/js/app.js:7` `validPages` 추가
  - `src/main/resources/static/js/app.js:14-25` `menus` 배열 항목 추가
  - `src/main/resources/static/js/app.js:36-50` 컴포넌트 spread 추가
  - `src/main/resources/static/js/app.js:78` `partialNames` 추가
  - `src/main/resources/static/js/app.js:224-260` `switch(page)` case (필요 시)
  - `src/main/resources/static/index.html:74-75` `<div data-partial="glossary">` 슬롯
  - `src/main/resources/static/index.html:108` `<script src="/js/components/glossary.js">`
  - `src/main/resources/static/partials/_sidebar.html:36-65, 97-126` 아이콘 SVG 2곳 (모바일+데스크탑)

### Institutional Learnings

`docs/solutions/` 검색 결과:
- `architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md` — Postgres UNIQUE + race 처리. find-or-create 시 race 발생 가능성은 낮으나 인라인 카테고리 생성 동시 클릭 시 대비. SQLState 23505 → GlobalExceptionHandler 409 매핑은 이미 표준.
- `architecture-patterns/spring-data-jpa-custom-impl-fragment-dynamic-sql-2026-05-10.md` — 검색 + 동적 조건 + Pageable 결합이 복잡해지면 `*Custom + *Impl` fragment 패턴. 본 plan은 JPQL 옵셔널 필터 패턴(`:x IS NULL OR e.x = :x`)으로도 충분하나 evolve 시 참조.
- `architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` — `@Transactional` self-invocation 함정. **본 plan에서는 ownership 검증 → bulk update → delete를 모두 동일 `@Service` 메서드 1개에 두므로 self-invocation 위험 없음** (다른 빈 호출 없음).
- 신규 문서화 대상: LIKE escape 정책, ownership 검증 위치 컨벤션은 본 PR 종료 후 `docs/solutions/` 작성 가치 있음 (운영 후속).

## Key Technical Decisions

- **응답 코드 정책: 404 통일**. 타사용자 termId/categoryId 접근 시 403이 아닌 404. `newsjournal/` 의 `NewsEventNotFoundException` JavaDoc 답습. 프론트 카피는 "용어/카테고리를 찾을 수 없거나 접근 권한이 없습니다" 통합 (존재 누설 방지).
- **ownership 검증 위치: application service**. `findByIdAndUserId(id, userId).orElseThrow(...NotFoundException::new)` 패턴을 service 메서드 진입부에 강제. Repository 자체는 unsafe `findById` 도 노출 가능하나, application 외부에서 호출되지 않음. (origin: ownership 검증 강제 위치 deferred)
- **길이 상한 (R14 deferred 해결)**: category.name `length=50` / term.name `length=200` / term.definition `columnDefinition="TEXT"` + Request DTO `@Size(max=4000)`. `newsjournal/` 길이 컨벤션 답습.
- **페이지네이션 (R11 deferred 해결)**: 컨트롤러 기본값 `page=0, size=20`, max `200`. record `GlossaryTermListFilter` compact constructor에서 검증. Spring `Pageable` 을 컨트롤러에 직접 노출하지 않음.
- **정렬 secondary tie-breaker (flow 분석 결과)**: 등록일순 → `created_at DESC, id DESC`, 가나다순 → `name ASC, id ASC`. 동일 용어명 중복 등록 허용 시 안정 정렬 보장.
- **검색·필터·정렬 변경 시 page=1로 일관 리셋**. URL query string에 `q, categoryId, sort, page` 동기화.
- **R7 대소문자 무시 구현: PostgreSQL `LOWER(col) LIKE LOWER(:q)`**. ILIKE도 가능하나 인덱스 호환을 위해 LOWER 채택 (향후 함수형 인덱스 도입 시 호환). MVP에서는 인덱스 추가 없음.
- **R8 LIKE escape (R15)**: 입력의 `\` → `\\`, `%` → `\%`, `_` → `\_` 치환 후 `LIKE ... ESCAPE '\'` 사용. application 계층 helper로 추출.
- **R9 미분류 필터 표현**: 카테고리 필터 쿼리스트링은 `categoryId=<숫자>` 또는 sentinel `categoryId=__uncategorized__`. 후자가 들어오면 application에서 `categoryId IS NULL` 분기. brainstorm에서 가상 표현(null)으로 결정한 것을 그대로 따름.
- **R13 인라인 카테고리 생성 의미론: find-or-create (silent)**. 입력 카테고리명이 trim 후 기존 row와 일치하면 그 카테고리에 매핑, 아니면 신규 생성. 동명 충돌은 사용자에게 에러로 노출하지 않음. 단 "미분류" 예약어는 거부 (R5).
- **"미분류" 예약어 정규화**: 비교는 `name.trim()` 이후 정확 일치 ("미분류"만 차단). "미 분류" 같은 공백 변형은 별도 카테고리로 허용 (over-engineering 회피).
- **카테고리 삭제 단일 트랜잭션**: `GlossaryCategoryService.delete(categoryId, userId)` 한 `@Transactional` 메서드에서 (1) ownership 검증 (2) `@Modifying UPDATE GlossaryTermEntity SET categoryId = null WHERE userId=:u AND categoryId=:c` (3) `categoryJpaRepository.deleteByIdAndUserId(...)` 순서. self-invocation 없음.
- **트랜잭션 실패 응답 (R6 deferred 해결)**: DB 제약 위반은 `DataIntegrityViolationException` → GlobalExceptionHandler 409. 그 외는 500. 프론트는 "잠시 후 다시 시도해주세요" 통합 카피, 자동 재시도 없음.
- **스키마 관리 노선 (P0 해소)**: 본 프로젝트는 Flyway 미사용 + `spring.jpa.hibernate.ddl-auto: update` (dev/prod). 본 plan은 **Entity `@Table(uniqueConstraints=..., indexes=...)` 어노테이션 일임 + SQL 파일은 운영 수동 적용 백업 메모** 노선을 채택. `newsjournal/` 답습. Flyway 도입은 본 plan 범위 밖(별도 이터레이션). 결과로 unique 제약/인덱스는 Entity가 단일 출처, SQL 파일은 참고용.
- **Friendly error mapping (DB 제약 위반)**: `DataIntegrityViolationException`을 그대로 GlobalExceptionHandler 409 매핑에 맡기되, `GlossaryExceptionHandler`에서 cause SQLState `23505` + constraint name `uq_glossary_category_user_name` 매칭 시 `{error:"DUPLICATE_CATEGORY_NAME", message:"이미 같은 이름의 카테고리가 있습니다"}` 로 좁힌 409 반환. 프론트가 카테고리 인라인 폼 inline error로 표시 가능.
- **Cross-service `@Transactional` propagation**: `GlossaryTermService.create()`가 호출하는 `GlossaryCategoryService.resolve()`는 **`Propagation.REQUIRED` (default)** 로 유지. 인라인 카테고리 생성 race로 `DataIntegrityViolationException` 발생 시 외부 트랜잭션(term insert)이 함께 롤백되어 "용어가 저장됐는데 카테고리는 없음" 모순 상태 방지. `REQUIRES_NEW` 사용 금지.
- **404 timing uniformity (적용 범위 명시)**: 단건 조회/수정/삭제(R2, R4 — GET/PUT/DELETE `{id}`)에 한해 ownership 위반과 진짜 미존재의 timing을 동일하게 유지. **단일 쿼리 `WHERE id=? AND user_id=?` (`findByIdAndUserId`)** 만 사용. `findById` + 수동 owner 검증으로 분기하지 않음. **resolve() 인라인 카테고리 생성은 enumeration 대상이 아닌 신규 생성 경로**이므로 본 규칙 적용 외. preview/list 등 보조 endpoint도 마찬가지로 규칙 외이지만 ownership 검증 자체는 동일하게 강제.
- **frontend XSS 방지 규칙**: `glossary.js` 와 `glossary.html` 에서 `x-html` / `innerHTML` 사용 **금지**. 모든 사용자 입력 렌더링은 `x-text` 또는 `textContent` 로만. plain text 저장 정책(R14)의 frontend 측 invariant.
- **SecurityContext layer 위치**: 본 plan은 `GlossarySecurityContext` 를 `presentation/` 에 둠 (`newsjournal/` 답습). 본래 `infrastructure/security/`가 적합하지만 기존 선례와 일관성 우선. 향후 공통 `CurrentUserContext` 로 통합 가능 (known-debt, 문서화).

## Open Questions

### Resolved During Planning

- **404 vs 403**: 404 통일 (newsjournal 컨벤션) — Key Decisions
- **ownership 검증 위치**: application service `findByIdAndUserId(...).orElseThrow(...)` — Key Decisions
- **글자수 상한 수치**: category.name=50, term.name=200, term.definition=TEXT + DTO 4000 — Key Decisions
- **페이지네이션 기본/max**: page=0, size=20, max=200 — Key Decisions
- **트랜잭션 실패 피드백 정책**: 409 / 500 글로벌 매핑 + 통합 카피 — Key Decisions
- **R9 미분류 sentinel 표현**: `__uncategorized__` — Key Decisions
- **정렬 secondary tie-breaker**: `id` — Key Decisions
- **인라인 카테고리 생성 의미론**: find-or-create (silent) — Key Decisions

### Deferred to Implementation

**UX 카피 / 메시지** (1차 안은 Unit 5 Approach에 명시; 아래는 잔여)
- 동일 용어명 중복 등록 시 soft warning 카피 (또는 미노출 — 등록일/설명으로 사용자 자체 구분 위임 옵션도 가능)

**Frontend 인터랙션 / 상태 관리**
- `glossary.js` 컴포넌트 내부 상태 관리 세부 (Alpine.js 스토어 vs 컴포넌트-로컬 state 구조)

**Design Assets**
- 사이드바 아이콘 SVG (book 아이콘 새로 추가 또는 기존 활용)

## Implementation Units

- [ ] **Unit 1: Flyway 마이그레이션 + Domain 모델 + Exception**

**Goal:** glossary 도메인의 데이터베이스 스키마와 Spring/JPA에 독립적인 도메인 모델, 도메인 예외를 정의한다.

**Requirements:** R1, R4, R5, R14

**Dependencies:** 없음 (이 unit이 모든 후속 unit의 선결)

**Files:**
- Create: `src/main/resources/db/migration/glossary_tables_2026_05_18.sql` (운영 수동 적용 백업 메모; prefix `V` 없음 — 기존 인접 파일 컨벤션 답습)
  - `glossary_category(id BIGSERIAL PK, user_id BIGINT NOT NULL, name VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)`
  - `UNIQUE (user_id, name)` `uq_glossary_category_user_name`, 인덱스 `idx_glossary_category_user_id (user_id)`
  - `glossary_term(id BIGSERIAL PK, user_id BIGINT NOT NULL, name VARCHAR(200) NOT NULL, definition TEXT, category_id BIGINT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)`
  - 인덱스: `idx_glossary_term_user_id (user_id)`, `idx_glossary_term_user_category (user_id, category_id)`
  - **FK 미사용** (ID 참조 컨벤션). 카테고리 삭제 시 service 계층이 명시적으로 term.category_id를 null로 update.
  - **권위는 Entity** (ddl-auto: update). 이 파일은 운영 DBA 수동 적용/롤백 백업용.
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/model/GlossaryCategory.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/model/GlossaryTerm.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/exception/GlossaryTermNotFoundException.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/exception/GlossaryCategoryNotFoundException.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/exception/ReservedCategoryNameException.java`

**Approach:**
- 도메인 모델은 Lombok `@Getter` + 정적 factory `create(userId, name, ...)` + `assignId(Long id)` 패턴. Setter 미사용, 비즈니스 메서드(`rename`, `assignCategory`)로 상태 변경 노출.
- `GlossaryCategory.create(userId, name)`는 trim 후 "미분류" 예약어 검사 → `ReservedCategoryNameException` (R5)
- Lombok 외 Spring/JPA import 금지 (domain layer 규칙)
- Exception은 JavaDoc에 "IDOR 방지 위해 404 통일" 명시 — `NewsEventNotFoundException` 답습
- 트랜잭션 인덱스/제약은 Entity 어노테이션이 아닌 SQL에서만 선언

**Patterns to follow:**
- `newsjournal/domain/model/NewsEventCategory.java` (factory + assignId)
- `newsjournal/application/exception/NewsEventNotFoundException.java` (JavaDoc 404 정책)
- Flyway 기존 V 파일: `src/main/resources/db/migration/`에서 마지막 버전 확인 후 다음 번호

**Test scenarios:**
- Happy path: `GlossaryCategory.create(1L, "성장주")` → 정상 객체 반환, userId/name 보존
- Edge case: `GlossaryCategory.create(1L, "  미분류  ")` → trim 후 예약어 매칭, `ReservedCategoryNameException`
- Edge case: `GlossaryCategory.create(1L, "")` → IllegalArgumentException (도메인 모델 단계 가드)
- Happy path: `GlossaryTerm.create(1L, "PER", "주가수익비율", null)` → categoryId=null = 미분류로 생성
- Happy path: `GlossaryTerm.assignCategory(null)` 호출 → categoryId null 변경 성공
- Edge case: `GlossaryCategory.rename("미분류")` → `ReservedCategoryNameException`

**Verification:**
- Flyway 부팅 시 V 파일이 정상 적용되어 PostgreSQL에 두 테이블과 unique 제약, 인덱스가 존재
- domain layer 패키지 어디에도 `org.springframework.*` / `jakarta.persistence.*` import 없음 (grep)

---

- [ ] **Unit 2: Persistence 계층 (Entity / JpaRepository / Adapter / Mapper / Repository 포트)**

**Goal:** 도메인 모델과 PostgreSQL 사이의 JPA 매핑과 포트/어댑터를 구현한다. 검색·필터·정렬·페이지네이션을 위한 JPQL 쿼리도 여기에 둔다.

**Requirements:** R1, R2, R4, R6, R7, R8, R9, R10, R11

**Dependencies:** Unit 1

**Files:**
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/repository/GlossaryCategoryRepository.java` (포트)
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/repository/GlossaryTermRepository.java` (포트)
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryCategoryEntity.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryTermEntity.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryCategoryJpaRepository.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryTermJpaRepository.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryCategoryRepositoryImpl.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryTermRepositoryImpl.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/mapper/GlossaryMapper.java`

**Approach:**
- Entity: `@Entity @Table(name="glossary_category", uniqueConstraints={@UniqueConstraint(name="uq_glossary_category_user_name", columnNames={"user_id","name"})}, indexes={@Index(name="idx_glossary_category_user_id", columnList="user_id")})`. `glossary_term`도 동일 방식으로 인덱스 명시. Column nullable/length 선언. **Entity가 권위, ddl-auto: update가 적용 — newsjournal 답습**.
- Repository 포트는 `application` 이 의존하는 인터페이스만 노출:
  - Category: `save(GlossaryCategory)`, `findByIdAndUserId(Long, Long)`, `findByUserIdAndName(Long, String)`, `findByUserIdOrderByNameAsc(Long)`, `existsByUserIdAndName(Long, String)`, `deleteByIdAndUserId(Long, Long)`
  - Term: `save(GlossaryTerm)`, `findByIdAndUserId(Long, Long)`, `deleteByIdAndUserId(Long, Long)`, `countByUserIdAndCategoryId(Long, Long)`, `findList(GlossaryTermListFilter, Long userId)` → `GlossaryTermListResult`, `reassignCategoryToNull(Long userId, Long categoryId)` → int (R6 cascade용)
- JpaRepository는 Spring Data 인터페이스:
  - Category JpaRepository: `findByIdAndUserId`, `findByUserIdAndNameAndIdNot`(rename 충돌 체크), `existsByUserIdAndName`, `findAllByUserIdOrderByNameAsc`, `deleteByIdAndUserId`
  - Term JpaRepository: `findByIdAndUserId`, `deleteByIdAndUserId`, `countByUserIdAndCategoryId`, `@Modifying(clearAutomatically = true, flushAutomatically = true) @Query("UPDATE GlossaryTermEntity t SET t.categoryId = null, t.updatedAt = CURRENT_TIMESTAMP WHERE t.userId = :u AND t.categoryId = :c") int reassignCategoryToNull(Long u, Long c)`
    - `clearAutomatically=true, flushAutomatically=true` 는 **Hibernate Persistence Context (L1/Session cache) 무효화**용. UPDATE 쿼리 실행 전 pending 변경을 flush + 실행 후 cache 비움 → 같은 트랜잭션에서 동일 term을 `findByIdAndUserId` 호출하면 DB 재조회로 fresh `categoryId == null` 반환. DB 측 result cache와는 무관
    - `updatedAt`은 `@PreUpdate` 가 JPQL UPDATE 쿼리에는 발동하지 않으므로 SET 절에서 명시
  - Term 목록 쿼리는 `RepositoryImpl` 에서 JPQL 동적 빌드 (filter 옵셔널 적용 + LOWER + LIKE ESCAPE + 정렬 secondary). 응답 row 수가 컬렉션 형이고 count 별도 쿼리.
- RepositoryImpl: 포트 구현. Mapper 호출로 Entity ↔ Domain 변환.
- Mapper: `toDomain(entity)`, `toEntity(domain)`, list/result 매핑. **categoryId null 보존**을 round-trip 테스트에서 검증 필요 (institutional learning #2).
- LIKE escape helper는 application 계층에 두지만 (이 unit이 아닌 Unit 3), JPQL 쿼리에서 `LOWER(t.name) LIKE LOWER(:q) ESCAPE '\\'` 형태로 받음.

**Technical design:** *(R6 cascade — 단일 트랜잭션 흐름. 직선적이지만 비자명하여 명시. 직접 구현 코드 아님)*

```
GlossaryCategoryService.delete(categoryId, userId)  [@Transactional]
  ├─ 1. categoryRepo.findByIdAndUserId(categoryId, userId)
  │       └─ orElseThrow(GlossaryCategoryNotFoundException::new)
  ├─ 2. termRepo.reassignCategoryToNull(userId, categoryId)   // @Modifying UPDATE int N
  ├─ 3. categoryRepo.deleteByIdAndUserId(categoryId, userId)
  └─ 트랜잭션 커밋 → 응답 {deletedCategoryId, reassignedCount: N}
```

**Patterns to follow:**
- `newsjournal/infrastructure/persistence/NewsEventCategoryEntity.java` (length=50, user_id nullable=false)
- `newsjournal/infrastructure/persistence/NewsEventRepositoryImpl.java` (옵셔널 필터 + PageRequest)
- `newsjournal/infrastructure/persistence/mapper/NewsEventMapper.java` (toDomain/toEntity 분리)

**Test scenarios:**
- Happy path: `categoryJpaRepo.save(...)` → `findByUserIdAndName(u, "성장주")` 반환
- Edge case: `(user_id, name)` 동일 행 두 번 save → SQL UNIQUE 충돌 → `DataIntegrityViolationException`
- Happy path: 카테고리 A에 속한 term 3건 저장 후 `reassignCategoryToNull(u, A)` → return 3, term.categoryId가 모두 null
- Edge case: 카테고리 A에 속한 term 0건일 때 `reassignCategoryToNull(u, A)` → return 0
- Happy path: 다른 사용자의 categoryId로 `findByIdAndUserId(otherId, myId)` → `Optional.empty()`
- Happy path: term `findList` with filter `q="per"` → LOWER LIKE 매칭, 페이지/총건수 정확
- Edge case: term `findList` with filter `categoryId=null` (미분류) → categoryId IS NULL 분기
- Edge case: term `findList` 정렬 가나다순, 동명 2건 → secondary id ASC로 안정 정렬
- Edge case: term `findList` with `q="100%"` (escape 후) → 리터럴 `%` 포함 row만 매칭, 다른 row 매칭 안 됨 (LIKE ESCAPE 동작 검증)
- Integration: R6 cascade 후 같은 트랜잭션에서 동일 term을 `findByIdAndUserId` → `categoryId == null` (L1 cache 무효화 검증)
- Integration: mapper round-trip — term `categoryId=null` 보존 (`toEntity(toDomain(entity)).categoryId` == null)

**Verification:**
- 두 Entity가 정상 적재되고 (Boot 부팅 OK), Repository 메서드 시그니처가 포트와 일치
- `@Modifying` 쿼리가 트랜잭션 외부에서 호출 시 예외 (Spring 기본 동작) — 확인용 단위 테스트는 선택

---

- [ ] **Unit 3: Application 계층 (CategoryService, TermService, Filter/Command/Result DTO)**

**Goal:** ownership 검증·검색 입력 sanitization·find-or-create·R6 cascade 단일 트랜잭션 등 모든 비즈니스 규칙을 application 서비스에 집중한다.

**Requirements:** R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R13, R15

**Dependencies:** Unit 1, 2

**Files:**
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/GlossaryCategoryService.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/GlossaryTermService.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/CreateGlossaryCategoryCommand.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/UpdateGlossaryCategoryCommand.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/CreateGlossaryTermCommand.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/UpdateGlossaryTermCommand.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/domain/repository/GlossaryTermListFilter.java` (record — repository 포트 입력이므로 domain 패키지. newsjournal 답습)
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/GlossaryTermListResult.java` (record)
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/dto/GlossaryCategoryDeleteResult.java` (record: `deletedCategoryId`, `reassignedCount`)
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/support/LikeEscaper.java` (또는 패키지-private util)

**Approach:**
- 모든 서비스 메서드 진입부에서 ownership 강제: `findByIdAndUserId(id, userId).orElseThrow(...NotFoundException::new)`. Repository의 unsafe `findById`는 호출 금지.
- `GlossaryCategoryService`:
  - `create(CreateGlossaryCategoryCommand, userId)`: name trim → "미분류" 예약어 거부 (`ReservedCategoryNameException`) → unique 충돌 시 `DataIntegrityViolationException` 그대로 throw (글로벌 → 409)
  - `update(id, UpdateGlossaryCategoryCommand, userId)`: ownership 검증 → 예약어 거부 → unique 충돌 throw
  - `delete(id, userId)`: **단일 `@Transactional` 메서드**. ownership 검증 → `termRepo.reassignCategoryToNull(userId, id)` → `categoryRepo.deleteByIdAndUserId(id, userId)` → `GlossaryCategoryDeleteResult` 반환
  - `findAll(userId)`: 본인 카테고리 목록 (가나다순)
  - `resolve(userId, name)` (R13 인라인): trim → 예약어 거부 → find-or-create. `findByUserIdAndName` 후 `.orElseGet(() -> save(create(...)))`. race 시 DataIntegrityViolation 그대로 throw (find로 재해석은 over-engineering, MVP는 그대로 409 노출)
  - `previewDeleteImpact(id, userId)`: ownership 검증 + `termRepo.countByUserIdAndCategoryId(userId, id)` 반환 (R6 확인 다이얼로그 N건)
- `GlossaryTermService`:
  - `create(CreateGlossaryTermCommand, userId)`: categoryId 지정 시 `categoryRepo.findByIdAndUserId(...)` 로 소유권 검증 (외래 카테고리 차단), null이면 미분류로 저장. `categoryName` 인라인 입력이 있으면 (R13) `categoryService.resolve(userId, categoryName)` 호출 후 그 ID 사용.
  - `update(id, UpdateGlossaryTermCommand, userId)`: ownership 검증 → PUT 전체 교체 (필드 모두 갱신, null도 명시적으로 반영)
  - `delete(id, userId)`: ownership 검증 → `deleteByIdAndUserId`
  - `find(id, userId)`: ownership 검증 → 도메인 반환
  - `list(GlossaryTermListFilter, userId)`: filter compact constructor에서 page/size/sort 검증. q는 trim 후 `LikeEscaper.escape(...)` 처리. categoryId sentinel(`__uncategorized__`)이면 null로 변환. `termRepo.findList(filter, userId)` 위임.
- `GlossaryTermListFilter` record: `(String q, String categoryId, GlossaryTermSort sort, int page, int size)` + compact constructor: `page>=0` (0-indexed), `1<=size<=200`, `sort != null` 기본 `REGISTERED_DESC`. `categoryId`는 query string 키와 동일 명명(통일성). sentinel `__uncategorized__`이면 application에서 null로 변환.
- `LikeEscaper`: `\` → `\\`, `%` → `\%`, `_` → `\_` 치환 후 반환. 입력 길이 상한 (예: 100자) — 초과 시 trim. R15 충족.
- 모든 쓰기 메서드에 **`@Transactional` 명시** (특히 `GlossaryTermService.create()`, `update()`, `delete()` — propagation 보장 위해 필수). 읽기에 `@Transactional(readOnly=true)`.
- ⚠ **`@Transactional` self-invocation 회피**: TermService → CategoryService 호출(R13 인라인)은 별도 빈이므로 안전. CategoryService 내부에서 자기 자신 메서드 호출 금지 (모두 외부 진입점만).
- **Propagation 명시**: `categoryService.resolve()` 호출 시 default `Propagation.REQUIRED`. race 시 카테고리 INSERT는 SQL UNIQUE 위반으로 `DataIntegrityViolationException` → outer transaction(term insert) 함께 롤백 → **사용자는 409 `DUPLICATE_CATEGORY_NAME` 응답을 받고 term은 저장되지 않은 상태** (Risks 표의 표현과 동일 의미 — race 시 사용자가 인지해 재시도).
- **Method length 컨벤션 (5줄/10줄)**: 일부 service 메서드는 orchestration 성격으로 5줄을 약간 넘을 수 있음. 분기 깊이가 아닌 순차 호출(ownership 검증 → 비즈니스 로직 → 결과 매핑)이라면 허용. 5줄 초과 시 helper 추출 (예: `resolveCategoryRef(cmd, userId)` — `categoryId` vs `categoryName` 인라인 분기) 또는 메서드 상단 주석으로 사유 명시.

**Patterns to follow:**
- `newsjournal/application/NewsEventCategoryService.java` (resolve)
- `newsjournal/application/NewsEventWriteService.java` (ownership orElseThrow)
- `newsjournal/domain/repository/NewsEventListFilter.java` (record + compact constructor — domain 패키지)

**Test scenarios:**
- Happy path: `categoryService.create(cmd("성장주"), 1L)` → ID 반환
- Edge case: `categoryService.create(cmd("미분류"), 1L)` → `ReservedCategoryNameException`
- Edge case: `categoryService.create(cmd("  성장주  "), 1L)` 후 동명 재호출 → unique 충돌 (`DataIntegrityViolationException`)
- Error path: `categoryService.update(otherUserCategoryId, cmd, myUserId)` → `GlossaryCategoryNotFoundException`
- Integration: `categoryService.delete(catA, userId)`에 catA 소속 term 5건 존재 → 트랜잭션 후 term.categoryId 모두 null, category row 삭제, return `reassignedCount=5`
- Integration: `termService.create(cmd(... categoryName="신규"), userId)` 호출 직후 race로 동일 사용자 동일명 카테고리 미리 존재 → resolve가 DataIntegrityViolation throw → 외부 트랜잭션도 롤백 → term이 저장되지 않음 (REQUIRED propagation 검증)
- Edge case: 타사용자 termId로 `findByIdAndUserId(otherId, myId)` 와 진짜 미존재 termId 모두 → 동일 코드 경로 (single query) → 응답 시간 차이 무의미 (timing uniformity)
- Edge case: `categoryService.previewDeleteImpact(catA, userId)` 후 별도 트랜잭션에서 term 추가 → delete 시 응답 N이 preview와 다를 수 있음 (race; 응답값을 권위로 사용)
- Happy path: `termService.create(cmd("PER", "주가수익", categoryId=null), userId)` → categoryId=null로 저장
- Happy path: `termService.create(cmd("PER", "주가수익", categoryName="가치주"), userId)` → resolve("가치주") 호출, 신규 카테고리 생성 후 그 ID로 term 저장
- Happy path: 동일 사용자 동일 용어명 두 번 등록 → 둘 다 성공 (중복 허용)
- Error path: `termService.create(cmd(... categoryId=otherUserCategoryId), myUserId)` → `GlossaryCategoryNotFoundException` (외래 카테고리 차단)
- Error path: `termService.delete(otherUserTermId, myUserId)` → `GlossaryTermNotFoundException`
- Edge case: `termService.list(filter(q="100%"), userId)` → escape 후 LIKE에서 리터럴 `%` 매칭 (와일드카드 X)
- Edge case: `termService.list(filter(categoryToken="__uncategorized__"), userId)` → categoryId IS NULL 분기
- Happy path: `termService.list(filter(sort=ALPHA_ASC), userId)` 가나다순, 동명 2건 → id ASC secondary

**Verification:**
- 서비스 메서드 외부에서 unsafe Repository 메서드(`findById`)가 호출되지 않음 (grep으로 application 외부 호출 검증)
- 모든 service 쓰기 메서드에 `@Transactional` 적용
- `GlossaryTermListFilter` compact constructor가 잘못된 값(page=-1, size=999)에서 예외

---

- [ ] **Unit 4: Presentation 계층 (Controller, Request/Response DTO, SecurityContext, ExceptionHandler)**

**Goal:** REST API를 노출하고 인증된 사용자 식별, 예외 → HTTP 매핑을 처리한다.

**Requirements:** R1, R2, R3, R4, R6, R11, R12, R13, R14

**Dependencies:** Unit 3

**Files:**
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/GlossarySecurityContext.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/GlossaryTermController.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/GlossaryCategoryController.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/GlossaryExceptionHandler.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/CreateGlossaryTermRequest.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/UpdateGlossaryTermRequest.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/GlossaryTermResponse.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/GlossaryTermListResponse.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/CreateGlossaryCategoryRequest.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/UpdateGlossaryCategoryRequest.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/GlossaryCategoryResponse.java`
- Create: `src/main/java/com/thlee/stock/market/stockmarket/glossary/presentation/dto/GlossaryCategoryDeleteResponse.java`
- Modify: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/config/*` 필요 시 (`/api/glossary/**`가 기존 인증 필터로 보호되는지 확인; 보통 추가 작업 불요)

**Approach:**
- `GlossarySecurityContext.currentUserId()` 정적 메서드 — `Authentication.principal instanceof Long` 가드, 아니면 `InsufficientAuthenticationException`. 컨트롤러는 한 줄로 `Long userId = GlossarySecurityContext.currentUserId();`.
- API 설계:
  - `POST /api/glossary/categories` — 본인 카테고리 생성. 201 + Location + `{id}`
  - `GET /api/glossary/categories` — 본인 카테고리 목록 (가나다순)
  - `PUT /api/glossary/categories/{id}` — 본인 카테고리 이름 수정. 204
  - `DELETE /api/glossary/categories/{id}` — R6 cascade 단일 트랜잭션. 200 + `GlossaryCategoryDeleteResponse{deletedId, reassignedCount}`
  - `GET /api/glossary/categories/{id}/delete-impact` — preview 영향 건수. 200 + `{count}`
  - `POST /api/glossary/terms` — 본인 용어 생성. body에 `categoryId` 또는 `categoryName`(인라인) 둘 다 옵션. 201 + Location + `{id}`
  - `GET /api/glossary/terms` — 본인 용어 목록. query: `q, categoryId, sort, page, size`. categoryId가 `__uncategorized__` 이면 미분류 필터. 200 + `GlossaryTermListResponse{items, totalCount, page, size}`
  - `GET /api/glossary/terms/{id}` — 본인 용어 단건. 200 + `GlossaryTermResponse`
  - `PUT /api/glossary/terms/{id}` — 본인 용어 전체 교체. 204
  - `DELETE /api/glossary/terms/{id}` — 본인 용어 삭제. 204
- Request DTO Bean Validation (필드별 명시):
  - `CreateGlossaryCategoryRequest.name`: `@NotBlank @Size(max=50)`
  - `UpdateGlossaryCategoryRequest.name`: `@NotBlank @Size(max=50)`
  - `CreateGlossaryTermRequest.name`: `@NotBlank @Size(max=200)`
  - `CreateGlossaryTermRequest.definition`: `@Size(max=4000)` (선택)
  - `CreateGlossaryTermRequest.categoryName`: `@Size(max=50)` (선택, 인라인 R13)
  - `CreateGlossaryTermRequest.categoryId`: `Long` (선택)
  - `UpdateGlossaryTermRequest.*`: 위와 동일
  - description은 plain text 그대로 저장 (별도 sanitization 라이브러리 미사용; frontend `x-text` 렌더링 책임)
- `GlossaryExceptionHandler(@RestControllerAdvice(assignableTypes={GlossaryTermController.class, GlossaryCategoryController.class}))`:
  - `GlossaryTermNotFoundException` → 404
  - `GlossaryCategoryNotFoundException` → 404
  - `ReservedCategoryNameException` → 400 + `{error:"RESERVED_CATEGORY_NAME"}`
  - `MethodArgumentNotValidException` → 400 + `fieldErrors`
  - `DataIntegrityViolationException`: cause의 SQLState `23505` + constraint name 매칭 시 `{error:"DUPLICATE_CATEGORY_NAME", message:"이미 같은 이름의 카테고리가 있습니다"}` 으로 좁힌 409 반환. 그 외 23505는 Global Handler 일반 409로 위임.
- 응답 shape `{error, message, timestamp}` (+ 옵션 `fieldErrors`) — newsjournal/GlobalExceptionHandler 답습
- **`/api/glossary/**` 인증 강제 (P1 격상 — 둘 다 의무)**: (a) Spring Security 설정에 명시적인 `requestMatchers("/api/glossary/**").authenticated()` 라인 추가 (`infrastructure/security/config/SecurityConfig` 또는 동등 위치) + (b) MockMvc 테스트로 `GET /api/glossary/terms` 인증 헤더 없이 호출 시 401 단언. 한 쪽만 두면 Security 설정 회귀 시 무방비.

**Patterns to follow:**
- `newsjournal/presentation/NewsJournalSecurityContext.java`
- `newsjournal/presentation/NewsJournalController.java` (201 + Location, 204)
- `newsjournal/presentation/NewsJournalExceptionHandler.java`

**Test scenarios:**
- Happy path: `POST /api/glossary/terms {name:"PER", definition:"..."}` 인증된 user → 201 + Location 헤더 + body `{id}`
- Edge case: `POST /api/glossary/terms` body name 누락 → 400 + fieldErrors에 `name`
- Edge case: `POST /api/glossary/terms` name 201자 → 400 + fieldErrors `name max length`
- Error path: 인증 없음 → 401 (글로벌 보안 필터)
- Error path: 타사용자 termId로 `GET /api/glossary/terms/{id}` → 404 (403 아님)
- Error path: 타사용자 categoryId로 `DELETE /api/glossary/categories/{id}` → 404
- Happy path: `DELETE /api/glossary/categories/{id}` (해당 카테고리에 term 3건 존재) → 200 + `{deletedId, reassignedCount:3}`
- Happy path: `GET /api/glossary/terms?q=per&categoryId=5&sort=ALPHA_ASC&page=0&size=20` → 200 + 응답에 items/totalCount/page/size
- Edge case: `GET /api/glossary/terms?categoryId=__uncategorized__` → categoryId IS NULL 매칭
- Edge case: `GET /api/glossary/terms?size=999` → 400 (filter compact constructor 검증)
- Edge case: `POST /api/glossary/categories {name:"미분류"}` → 400 + `{error:"RESERVED_CATEGORY_NAME"}`
- Edge case: `POST /api/glossary/categories {name:"성장주"}` 두 번 → 두 번째 409 + `{error:"DUPLICATE_CATEGORY_NAME"}` (SQLState 23505 매칭)
- Edge case: `GET /api/glossary/terms?sort=DROP` → 400 + fieldErrors (enum 바인딩 실패)
- Edge case: 인증 헤더 없이 `GET /api/glossary/terms` → 401 (Spring Security)
- Edge case: `POST /api/glossary/terms {categoryName:"<51자 문자열>"}` → 400 + fieldErrors (`@Size(max=50)`)

**Verification:**
- 모든 신규 엔드포인트가 인증 필요 (Spring Security 설정에서 `/api/glossary/**`가 기본 `authenticated()` 룰에 포함되는지 부팅 시 매핑 확인)
- 응답 shape이 newsjournal과 동일 (`{error, message, timestamp}` + 옵션 `fieldErrors`)
- 모든 컨트롤러 메서드가 `GlossarySecurityContext.currentUserId()` 사용 (grep)

---

- [ ] **Unit 5: Frontend (사이드바 메뉴 + glossary 컴포넌트 + 정적 HTML)**

**Goal:** 사용자가 사이드바의 "용어 사전" 메뉴로 진입해서 용어를 등록·검색·필터·삭제하고 카테고리를 관리할 수 있는 정적 UI를 추가한다.

**Requirements:** R1, R2, R4, R6, R7, R8, R9, R10, R11, R12, R13

**Dependencies:** Unit 4 (API 구현 완료 — Postman/curl로 실제 호출 가능한 상태. spec 작성만으로는 부족)

**Files:**
- Modify: `src/main/resources/static/js/app.js`
  - `validPages` 배열에 `'glossary'` 추가
  - `menus` 배열에 `{key:'glossary', label:'용어 사전', icon:'book'}` 추가 (배치는 기존 메뉴 사이 자연스러운 위치)
  - 컴포넌트 spread에 `...GlossaryComponent` 추가
  - `partialNames` 배열에 `'glossary'` 추가
  - `switch(page)`에 case 추가 (필요 시 page 진입 훅; 미사용도 OK)
- Modify: `src/main/resources/static/index.html`
  - `<div data-partial="glossary">` 슬롯 추가 (다른 partial 슬롯 옆)
  - `<script src="/js/components/glossary.js">` 태그 추가 (다른 component 스크립트 옆)
- Modify: `src/main/resources/static/partials/_sidebar.html`
  - `<template x-if="menu.icon==='book'">` 추가 (모바일+데스크탑 SVG 2곳 동기)
- Create: `src/main/resources/static/partials/glossary.html`
  - 좌측: 카테고리 사이드바 (전체 / 미분류 / 사용자 카테고리 목록 + 카테고리 추가 버튼)
  - 우측: 검색바 + 정렬 셀렉트 + 페이지네이션 컨트롤
  - 본문: 용어 카드 리스트 (용어명 / 설명 미리보기 / 카테고리 뱃지 / 등록일 / 수정·삭제 액션)
  - 등록 폼: 모달 또는 슬라이드오버 (용어명/설명/카테고리 select with 인라인 생성)
  - Empty state 3종: 0건 등록 ("첫 용어를 등록하세요" CTA), 검색결과 0건, 카테고리 0개
  - 카테고리 삭제 확인 다이얼로그 (`GET /api/glossary/categories/{id}/delete-impact` 호출 후 영향 건수 표시)
- Create: `src/main/resources/static/js/components/glossary.js`
  - Alpine.js 컴포넌트 `GlossaryComponent()` (다른 components 패턴 답습)
  - 상태: `terms`, `totalCount`, `categories`, `q`, `categoryFilter`, `sort`, `page`, `size`, 폼 상태
  - 액션: `loadCategories`, `loadTerms`, `createTerm` (인라인 카테고리 입력 시 `categoryName` 전송), `updateTerm`, `deleteTerm`, `createCategory`, `renameCategory`, `confirmDeleteCategory` (preview 호출 후 다이얼로그)
  - 검색/필터/정렬 변경 시 page=1 리셋 (Key Decisions)
  - URL query 동기화 (history.replaceState)
  - LIKE escape는 백엔드 책임이므로 프론트는 원문 전송
  - 에러 응답 처리: 404/400/409 각각의 사용자 카피 ("용어/카테고리를 찾을 수 없거나 접근 권한이 없습니다", validation 메시지, "잠시 후 다시 시도해주세요")

**Approach:**
- 기존 컴포넌트(`portfolio.js`, `news.js`, `news-journal.js`, `keyword.js` 등 `static/js/components/*.js`) 패턴 그대로 답습. Alpine.js 객체 리터럴 `const GlossaryComponent = { ... }`, namespaced state, kebab-case 파일명.
- **filename**: `static/js/components/glossary.js`, partial `static/partials/glossary.html` (no underscore prefix; underscore는 공통 layout fragment 전용).
- **XSS 방지 규칙 (Key Decisions)**: 모든 사용자 입력 렌더링은 `x-text` 또는 `textContent` 만 사용. `x-html` / `innerHTML` 금지. 코드 리뷰 체크포인트.

- **Information Architecture (P1 결정)**: 페이지 진입 시 기본 상태 — 카테고리 사이드바 "전체" 선택 + 본문은 등록일 최신순 첫 페이지. 0건 사용자는 본문에 empty state CTA가 primary focus. 카테고리 사이드바는 데스크탑에서 항상 노출, 모바일에서는 collapse(drawer) 모드.
- **카테고리 인라인 생성 UI (P1 결정)**: 카테고리 select를 **검색 가능한 콤보박스(typeahead)**로 구현 — 사용자가 타이핑하면 본인 카테고리 매칭 표시, 일치 항목 선택 시 기존 ID 매핑, 정확 일치 없이 엔터 시 신규 생성. 별도 "+ 새 카테고리" 버튼 모달은 미사용 (마찰 감소). 동명 대소문자 변형 충돌 가능성은 backend `resolve()` find-or-create가 흡수 (trim 후 정확 일치).
- **Empty state 카피 1차안 (P1 결정 — design 단계에서 조정 가능)**:
  - 0건 등록: "첫 용어를 등록해보세요. 모르는 경제·주식 용어를 정리해두면 나중에 카테고리로 묶어 찾아볼 수 있어요." + [용어 등록] CTA
  - 검색 결과 0건: "'{검색어}'에 해당하는 용어가 없어요." + 검색 필드 초기화 링크
  - 카테고리 0개: "아직 분류한 용어가 없어요. 용어 등록 시 새 카테고리를 만들 수 있어요." (사이드바 영역에 표시)
- **카테고리 삭제 다이얼로그 카피 (P1 결정)**: N>0 — "이 카테고리를 삭제하면 속한 용어 N개가 '미분류'로 이동합니다. 계속할까요?" / N=0 — "이 카테고리를 삭제할까요?"
- **반응형 전략 (P1 결정)**: breakpoint 기준 — 데스크탑(≥1024px): 좌측 사이드바(220px) + 본문 2단 / 태블릿(640–1023px): 사이드바 collapse 토글 / 모바일(<640px): 사이드바를 drawer, 본문 전체 폭. 등록/수정 폼은 모든 폼팩터에서 **slideover(우측 슬라이드)** 패턴 — 기존 iPad chat window UX 작업과 일관.
- **Interaction states (P1 결정)** — 비동기 동작별 상태:
  - term list/category 사이드바 로딩: skeleton placeholder(2–3개 row), 200ms 이내 응답 시 깜빡임 회피
  - 폼 제출 in-flight: submit 버튼 disabled + spinner. 응답까지 다른 액션 불가
  - delete-impact preview 로딩: 다이얼로그 내 inline spinner ("확인 중...")
  - 입력 검증: on blur(글자수/필수) + on submit(중복/예약어). on-type 즉시 검증은 미사용
  - 카테고리 인라인 콤보박스: typing 시 250ms debounce 후 매칭 표시
- **페이지네이션 컨트롤**: 이전/다음 버튼 + "M-N / Total건" 표시 (0-indexed). 검색/필터/정렬 변경 시 page=0 리셋.
- **사이드바 아이콘**: 기존 SVG 컬렉션에 `book` 가 없으면 simple 책 아이콘 SVG 추가.
- **에러 응답 처리**: 백엔드 응답의 `error` 코드로 분기 — `DUPLICATE_CATEGORY_NAME` → 카테고리 필드 옆 inline error + 콤보박스 매칭 항목 강조 안내, `RESERVED_CATEGORY_NAME` → 카테고리 필드 inline error, 404 → "용어/카테고리를 찾을 수 없거나 접근 권한이 없습니다" 토스트 + 리스트 새로고침, 401 → 기존 인증 만료 흐름.

**Patterns to follow:**
- `static/js/components/news-journal.js` (사용자 CRUD에 가장 가까움 — 확인됨)
- `static/js/components/portfolio.js` (페이지네이션·필터 패턴)
- `static/partials/_sidebar.html` 기존 메뉴/아이콘 SVG 패턴

**Test scenarios:**
- Test expectation: 수동 브라우저 검증 — 자동화 테스트는 명시적 요청 시에만 (CLAUDE.md 정책). 다만 다음 수동 시나리오 실행:
- Happy path: 신규 사용자 첫 진입 → 0건 empty state CTA → 등록 폼 열기 → 카테고리 인라인 생성 → 저장 → 첫 카드 노출
- Happy path: 용어 등록 후 검색창에 입력 → 부분 일치 결과 → 카테고리 필터 추가 → 결과 좁힘 → 정렬 변경 → page=1 리셋
- Edge case: 검색 결과 0건 → 검색결과 empty state
- Edge case: 카테고리 0개 상태에서 등록 폼 → 카테고리 select가 "미분류" + "+ 새 카테고리" 만 노출
- Edge case: 카테고리 삭제 시 영향 5건 미리 표시 → 확인 → 삭제 → 5건이 미분류로 이동
- Edge case: 동일 용어명 두 번 등록 → 둘 다 목록에 표시 (구분은 등록일 + 설명)
- Error path: 인증 만료 후 액션 → 401 처리 (기존 인증 만료 흐름 답습)
- Error path: 카테고리명 "미분류" 입력 시 백엔드 400 → 폼 inline 에러
- Error path: 네트워크 단절 시 카테고리 삭제 → "잠시 후 다시 시도해주세요" 토스트

**Verification:**
- 사이드바에서 "용어 사전" 클릭 → `glossary.html` 파셜 로드 OK
- 모든 CRUD/검색/필터/정렬/페이지네이션 시나리오가 수동 검증 통과
- 사이드바 SVG 아이콘이 모바일/데스크탑 두 영역 모두 정상 표시

## System-Wide Impact

- **Interaction graph:** `JwtAuthenticationFilter` → `GlossarySecurityContext.currentUserId()` → 컨트롤러 → 서비스 → 포트/어댑터. 외부 다른 도메인과 연계 없음 (Scope Boundary).
- **Error propagation:** Repository UNIQUE 위반 → `DataIntegrityViolationException` (Spring) → `GlobalExceptionHandler` 409. 도메인 NotFound → `GlossaryExceptionHandler` 404. 검증 실패 → 400 + fieldErrors. 인증 실패 → 401 (글로벌 보안 필터).
- **State lifecycle risks:** R6 cascade는 단일 `@Transactional`이므로 partial-write 위험 없음. 단 카테고리 삭제 직전에 다른 클라이언트가 같은 카테고리에 용어 추가 시 영향 건수 N의 race 가능 — 응답값을 권위로 사용 (preview는 best-effort).
- **API surface parity:** `/api/glossary/**` 신규 prefix. 기존 API와 충돌 없음. Spring Security 기본 `authenticated()` 룰이 `/api/**` 전반에 적용되는지 부팅 시 확인 (보통 그러함).
- **Integration coverage:** R6 단일 트랜잭션은 mocking으로 증명 불가 — 실제 DB(테스트 컨테이너 또는 통합 환경)에서 한 번은 손으로 검증 필요. mapper round-trip(특히 categoryId null 보존)도 동일.
- **Unchanged invariants:**
  - 기존 user/, news/, economics/, notification/, chatbot/, newsjournal/, stocknote/, favorite/, portfolio/ 도메인은 본 plan에서 일체 수정 없음 (`newsjournal/`는 참조만, 수정 X)
  - 기존 `JwtAuthenticationFilter`, `GlobalExceptionHandler` 동작은 변경하지 않음 — 신규 도메인이 그 표면을 그대로 사용
  - 기존 Flyway 마이그레이션 파일들은 수정하지 않음 — 다음 버전 V 파일 한 개 신규 추가

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| 카테고리 인라인 생성 동시 클릭 race로 UNIQUE 충돌 (R13) | REQUIRED propagation으로 term insert도 함께 롤백 → 사용자는 409 `DUPLICATE_CATEGORY_NAME` 응답을 받고 term 미저장. 1인 환경에서 빈도 낮음. 사용자가 다시 시도 시 정상 처리. 자동 재시도는 운영 후 데이터로 결정 |
| R6 영향 건수 race | preview는 best-effort, 실제 응답값 권위. 다이얼로그 카피에 "삭제 직전 N건이 미분류로 이동합니다"로 추정 표현 |
| 설명 필드 XSS — 프론트가 innerHTML로 잘못 렌더링 시 self-XSS | `glossary.js`에서 `textContent` 또는 Alpine `x-text` 만 사용. `x-html` 금지 (코드 리뷰 체크포인트) |
| Spring Security 기본 룰이 `/api/glossary/**`를 인증 필수에서 누락 | 부팅 후 매핑 확인 + Postman/curl 으로 인증 헤더 없이 호출 시 401 확인 |
| Entity `@UniqueConstraint`와 SQL 마이그레이션 양쪽에 두어 ddl-auto 충돌 | Entity에는 unique 어노테이션 두지 않고 SQL 단일 출처. Unit 1 V 파일에서만 정의 (institutional learning #1). 코드 리뷰에서 `grep "uniqueConstraints" src/.../glossary/infrastructure/` 결과 비어있음 확인 |
| 도메인 모델에 Spring/JPA import 혼입 | 코드 리뷰에서 `grep -r "org.springframework\|jakarta.persistence" src/.../glossary/domain/` 결과가 비어있는지 확인 |
| frontend `x-html`/`innerHTML` 우회로 self-XSS | Key Decisions의 XSS 방지 규칙 + Unit 5 Approach에 명시. 코드 리뷰에서 `grep "x-html\|innerHTML" src/main/resources/static/js/components/glossary.js src/main/resources/static/partials/glossary.html` 비어있음 확인 |
| `GlossarySecurityContext` 위치가 architecture 권고와 어긋남 (presentation/) | known-debt. `newsjournal/`/기타 도메인과 동일 위치 유지로 일관성 우선. 공통 `CurrentUserContext` 추출은 별도 리팩토링 이터레이션 |
| 자동화 테스트 부재로 회귀 위험 | MVP 한정 수용. 단위 테스트는 사용자 요청 시 우선 추가 (Unit 1·3 시나리오 우선). 단 Unit 4의 인증 강제 검증(`/api/glossary/**` 401)은 MockMvc 테스트로 의무 포함 |

## Documentation / Operational Notes

- 운영 로그에 용어/설명 평문 노출 정책 — 본 plan은 별도 마스킹 미도입 (개인 사전 = self-XSS 수준). 향후 로그 표준 정비 시 재평가.
- **leading indicator 관측 + 잠정 임계값**: 활성 사용자 1인당 평균 등록 건수, 등록 후 7일 내 재조회율. **잠정 임계값(출시 4주 측정, 신규 등록 사용자 cohort)**: 1인당 평균 등록 건수 < 3건 **또는** 7일 내 재조회율 < 20% → 후속 이터레이션(뉴스 인라인 진입점 또는 LLM 자동 정의 보조) 착수. **측정 emission 위치**: `GlossaryTermService.create()` 및 `findList()` 진입부에 `log.info("glossary.event type=create|view userId=... termId=...")` 한 줄 (기존 application log 인덱싱 활용). 정식 metric pipeline은 별도 측정 plan에서.
- DB 스키마 변경: Entity 어노테이션이 권위(ddl-auto: update). production 배포 시 자동 적용. SQL 백업 파일은 운영 DBA 수동 보관용. 롤백은 테이블 신규 추가만이라 별도 plan 불요.
- LIKE escape 정책 / ownership 검증 위치 컨벤션은 본 작업 종료 후 `docs/solutions/` 작성 가치 있음 (운영 후속).

## Sources & References

- **Origin document:** [docs/brainstorms/2026-05-17-glossary-requirements.md](../brainstorms/2026-05-17-glossary-requirements.md)
- 1차 참조 패턴: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/` (전체 4-layer)
- 응답 컨벤션: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java`
- 인증: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/jwt/JwtAuthenticationFilter.java`
- 아키텍처/컨벤션: `ARCHITECTURE.md`, `docs/policies/code-convention.md`, `CLAUDE.md`
- Institutional learnings: `docs/solutions/architecture-patterns/{deferred-unique-constraint-retry-requires-new, spring-data-jpa-custom-impl-fragment-dynamic-sql, external-http-per-item-transaction-isolation}-*.md`
- 이슈: #43
