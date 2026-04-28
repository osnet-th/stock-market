# 뉴스 기록 페이지 (News Journal) MVP 설계

> 원천: [docs/brainstorms/2026-04-28-news-journal-brainstorm.md](../../../../docs/brainstorms/2026-04-28-news-journal-brainstorm.md) · 관련 이슈: #33

## 작업 리스트

### 백엔드 — 도메인 / 영속화
- [x] `EventCategory` enum 생성 (`GOOD`, `BAD`, `NEUTRAL`)
- [x] `NewsEvent` 도메인 모델 + 정적 팩토리 `create()` + `updateBody()`
- [x] `NewsEventLink` 도메인 모델 (제목 + URL)
- [x] 애플리케이션 예외 (`NewsEventNotFoundException`) — 위치 보정: `application/exception/` (stocknote 컨벤션 일치)
- [x] `NewsEventEntity` JPA Entity (인덱스 포함)
- [x] `NewsEventLinkEntity` JPA Entity
- [x] `NewsEventJpaRepository`, `NewsEventLinkJpaRepository` (Spring Data)
- [x] `NewsEventRepository`, `NewsEventLinkRepository` 도메인 포트 (+ `NewsEventListFilter` record)
- [x] `NewsEventRepositoryImpl`, `NewsEventLinkRepositoryImpl` 어댑터
- [x] `NewsEventMapper` (단일 파일에 본체+자식 통합 — stocknote 컨벤션 일치)

### 백엔드 — 애플리케이션
- [x] `application/dto`: `CreateNewsEventCommand`, `UpdateNewsEventCommand`, `NewsEventLinkCommand`, `NewsEventDetailResult`, `NewsEventListItemResult`, `NewsEventListResult` — Result는 도메인 모델 임베드(stocknote 컨벤션). 평탄화는 presentation Response에서 처리.
- [x] `NewsEventWriteService` (`create`, `update`, `delete`)
- [x] `NewsEventReadService` (`findById`, `findList`)

### 백엔드 — 표현
- [x] `presentation/dto`: `CreateNewsEventRequest`, `UpdateNewsEventRequest`, `NewsEventLinkDto`, `NewsEventDetailResponse`, `NewsEventListResponse`
- [x] `NewsJournalSecurityContext` (인증 사용자 추출)
- [x] `NewsJournalController` (5개 엔드포인트)
- [x] `NewsJournalExceptionHandler`

### 프론트엔드
- [x] `static/index.html` 섹션 컨테이너 추가 (메뉴 항목은 menus 배열로 자동 렌더; 다음 작업에서 배열에 등록)
- [x] `static/js/app.js` `menus` 배열에 `news-journal` 등록 (+ `validPages`, index.html 사이드바 `journal` 아이콘 SVG)
- [x] `static/js/api.js` 5개 메서드 추가 (`createNewsEvent`, `getNewsEvents`, `getNewsEvent`, `updateNewsEvent`, `deleteNewsEvent`)
- [x] `static/js/components/news-journal.js` 신규 컴포넌트 (state + methods 골격)
  - app.js 스프레드 + index.html `<script>` 등록 완료
  - HTML 마크업(타임라인 + 모달)은 다음 작업으로 분리
- [x] `index.html` 본문 마크업 (세로 타임라인 + 생성/수정 모달 + 필터 바 + 페이지네이션)

### 보류 (2차, 본 설계 범위 외)
- 결과 사후 추가 필드 (`resultDate`, `resultText`)
- 학습 메모 필드 (`unknownTerms`, `questions`, `investmentInsights`)
- 키워드 단위 그룹 / 텍스트 검색 / 강화된 필터

---

## 배경

이슈 #33은 사용자가 뉴스를 직접 정리하여 일자별 사건(WHAT/WHY/HOW)으로 기록하고 타임라인으로 회고할 수 있는 개인용 저널 기능을 요구합니다. 이번 설계는 1차 MVP — 사건 CRUD + 세로 타임라인 — 만 구현합니다. 결과 추가, 학습 메모, 고급 검색은 사용 경험을 본 뒤 2차로 분리합니다.

## 핵심 결정

- **도메인 분리**: `news` / `overseasnews`와 결합하지 않음. 새 도메인 `newsjournal` 신규. URL은 외부 자유 입력만 지원하며 `NewsEntity` ID 참조 안 함.
- **모델 단위**: "사건(NewsEvent)" 1엔티티가 중심, URL은 자식 엔티티 `NewsEventLink`로 분리. `stock-purchase-history` 패턴과 일치 (Entity 연관관계 없이 ID 참조).
- **카테고리**: 3종 enum `GOOD/BAD/NEUTRAL` 고정. 강도/세분류는 도입하지 않음.
- **자식 갱신 전략**: 사건 수정 시 링크는 replace-all (기존 링크 전체 삭제 후 새 리스트 저장). 단순·결정적, diff 로직 불필요.
- **DB 마이그레이션**: 본 프로젝트는 `ddl-auto: update`를 사용하므로 Flyway/SQL 파일 불필요. JPA Entity 정의만으로 테이블 자동 생성. 인덱스는 `@Table(indexes=...)`로 선언.
- **소유권 / 인증**: `Long userId` 필드 보유, JWT principal에서 직접 추출 (`stocknote.SecurityContext` 패턴 복제). 다른 사용자 사건 접근 시 404 통일.
- **미래 일자 금지**: stocknote와 동일하게 `noteDate.isAfter(today)` 검증. `today` 파라미터 주입으로 테스트 가능성 확보.
- **타임라인 시각화**: Chart.js 미사용. Tailwind 세로 마크업(좌측 수직선 + 카테고리별 도트 색상 + 카드).

## 구현

### 도메인 모델

위치
- `newsjournal/domain/model/NewsEvent.java`
- `newsjournal/domain/model/NewsEventLink.java`
- `newsjournal/domain/model/EventCategory.java`
- `newsjournal/application/exception/NewsEventNotFoundException.java`

`NewsEvent` 필드:
| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | 저장 후 `assignId` 주입 |
| userId | Long | 소유자, 불변 |
| title | String | NotBlank, max 200 |
| occurredDate | LocalDate | 불변, 미래 일자 금지 |
| category | EventCategory | NotNull |
| what | String (TEXT) | nullable, max 4000 |
| why | String (TEXT) | nullable, max 4000 |
| how | String (TEXT) | nullable, max 4000 |
| createdAt / updatedAt | LocalDateTime | 도메인이 직접 관리 |

`NewsEvent` 메서드:
- `create(Long userId, String title, LocalDate occurredDate, LocalDate today, EventCategory category, String what, String why, String how)` — 정적 팩토리, 미래 일자/길이/필수값 검증.
- `updateBody(String title, LocalDate occurredDate, LocalDate today, EventCategory category, String what, String why, String how)` — 동일 검증, `updatedAt` 갱신.
- `assignId(Long id)` — 저장 후 ID 주입, 이미 설정 시 `IllegalStateException`.

`NewsEventLink` 필드: `id`, `eventId`, `title`(NotBlank, max 200), `url`(NotBlank, max 2000), `displayOrder`(int).
- `create(Long eventId, String title, String url, int displayOrder)` 팩토리.
- URL 형식 검증은 길이만 — http/https 필수 강제는 하지 않음 (사용 편의).

`EventCategory`: `GOOD`, `BAD`, `NEUTRAL` 3종.

> 본 도메인 모델에는 Spring/JPA 의존이 없어야 합니다 (ARCHITECTURE.md 4.domain).

[예시 코드](./examples/domain-model-example.md) — 후속 작업

### 영속화 (Entity / Repository / Mapper)

위치
- `newsjournal/infrastructure/persistence/NewsEventEntity.java`
- `newsjournal/infrastructure/persistence/NewsEventLinkEntity.java`
- `newsjournal/infrastructure/persistence/NewsEventJpaRepository.java`
- `newsjournal/infrastructure/persistence/NewsEventLinkJpaRepository.java`
- `newsjournal/infrastructure/persistence/NewsEventRepositoryImpl.java`
- `newsjournal/infrastructure/persistence/NewsEventLinkRepositoryImpl.java`
- `newsjournal/infrastructure/persistence/mapper/NewsEventMapper.java`
- `newsjournal/infrastructure/persistence/mapper/NewsEventLinkMapper.java`
- 포트: `newsjournal/domain/repository/NewsEventRepository.java`, `NewsEventLinkRepository.java`

`NewsEventEntity` 테이블 `news_event`:
- 인덱스: `idx_news_event_user_date (user_id, occurred_date DESC)` — 타임라인 기본 정렬.
- 보조 인덱스: `idx_news_event_user_category (user_id, category, occurred_date DESC)` — 카테고리 필터.
- 컬럼: id, user_id, title(varchar 200), occurred_date, category(varchar 20, ENUM STRING), what/why/how(TEXT), created_at, updated_at.

`NewsEventLinkEntity` 테이블 `news_event_link`:
- 인덱스: `idx_news_event_link_event (event_id, display_order)`.
- 컬럼: id, event_id, title(varchar 200), url(varchar 2000), display_order(int).
- FK 제약은 두지 않음 (CLAUDE.md Entity 연관관계 금지). 정합성은 application 트랜잭션에서 보장.

도메인 포트 인터페이스:

`NewsEventListFilter` (record, `domain/repository/`): `category`(nullable), `fromDate`(nullable), `toDate`(nullable), `page`(>=0), `size`(1~200) — stocknote 패턴 일치.

`NewsEventRepository`
- `NewsEvent save(NewsEvent event)` — 신규/수정 모두 처리 (id 유무로 분기).
- `Optional<NewsEvent> findByIdAndUserId(Long id, Long userId)` — 소유권 함께 검사.
- `List<NewsEvent> findList(Long userId, NewsEventListFilter filter)` — 사용자/카테고리/기간/페이지 적용.
- `long countList(Long userId, NewsEventListFilter filter)` — 페이지 합계용.
- `void deleteByIdAndUserId(Long id, Long userId)` — 안전 삭제.

`NewsEventLinkRepository`
- `List<NewsEventLink> findByEventId(Long eventId)` — `displayOrder` ASC.
- `Map<Long, List<NewsEventLink>> findAllByEventIds(Collection<Long> eventIds)` — 목록 뷰 N+1 회피.
- `void replaceAll(Long eventId, List<NewsEventLink> links)` — `deleteByEventId` 후 새 리스트 일괄 저장.
- `void deleteByEventId(Long eventId)` — 사건 삭제 시 호출.

[예시 코드](./examples/infrastructure-example.md) — 후속 작업

### 애플리케이션 서비스

위치
- `newsjournal/application/NewsEventWriteService.java`
- `newsjournal/application/NewsEventReadService.java`
- `newsjournal/application/dto/*.java`

DTO (application 계층):
- `CreateNewsEventCommand(Long userId, String title, LocalDate occurredDate, EventCategory category, String what, String why, String how, List<NewsEventLinkCommand> links)`
- `UpdateNewsEventCommand(Long id, Long userId, String title, LocalDate occurredDate, EventCategory category, String what, String why, String how, List<NewsEventLinkCommand> links)`
- `NewsEventLinkCommand(String title, String url)` — 신규 자식, displayOrder는 서비스에서 0-based로 부여.
- `NewsEventListFilter(EventCategory category, LocalDate from, LocalDate to, int page, int size)` — 모든 필드 nullable except page/size.
- `NewsEventDetailResult`, `NewsEventListResult`, `NewsEventLinkResult` — 조회 결과 (Domain Model을 그대로 노출하지 않기 위한 read-only DTO).

`NewsEventWriteService` (`@Service`, `@Transactional`):
- `Long create(CreateNewsEventCommand cmd)`:
  1. `NewsEvent.create(...)` 도메인 생성 (today = `LocalDate.now()` 기본값, 테스트는 Clock 주입 가능).
  2. `eventRepository.save(event)` → eventId 회수.
  3. links → `NewsEventLink.create(eventId, ...)` 변환 → `linkRepository.replaceAll(eventId, links)`.
  4. eventId 반환.
- `void update(UpdateNewsEventCommand cmd)`:
  1. `findByIdAndUserId(id, userId)` → 없으면 `NewsEventNotFoundException`.
  2. `event.updateBody(...)` → `eventRepository.save(event)`.
  3. `linkRepository.replaceAll(eventId, newLinks)`.
- `void delete(Long id, Long userId)`:
  1. 소유권 검증 (`findByIdAndUserId`).
  2. `linkRepository.deleteByEventId(id)` → `eventRepository.deleteByIdAndUserId(id, userId)`.

`NewsEventReadService` (`@Service`, `@Transactional(readOnly = true)`):
- `NewsEventDetailResult findById(Long id, Long userId)`: 사건 + 링크 조회, 조합 결과 반환. 미스매치/없음 → `NewsEventNotFoundException`.
- `NewsEventListResult findList(Long userId, NewsEventListFilter filter)`: 페이지 결과 반환. 각 사건의 링크는 일괄 조회 후 매핑 (N+1 회피 — `IN(eventIds)`).

[예시 코드](./examples/application-service-example.md) — 후속 작업

### 표현 / REST API

위치
- `newsjournal/presentation/NewsJournalController.java`
- `newsjournal/presentation/NewsJournalSecurityContext.java`
- `newsjournal/presentation/NewsJournalExceptionHandler.java`
- `newsjournal/presentation/dto/*.java`

Base path: `/api/news-journal`

| Method | Path | 설명 | 응답 |
|---|---|---|---|
| POST | `/events` | 사건 생성 | 201 + `Location` + `{"id": ...}` |
| GET | `/events` | 목록 (타임라인용) | 200 + `NewsEventListResponse` |
| GET | `/events/{id}` | 상세 | 200 + `NewsEventDetailResponse` |
| PUT | `/events/{id}` | 수정 | 204 |
| DELETE | `/events/{id}` | 삭제 | 204 |

목록 쿼리 파라미터: `from`, `to`, `category`, `page=0`, `size=20` — 모두 옵셔널 (페이지/사이즈 제외).

DTO:
- `CreateNewsEventRequest` / `UpdateNewsEventRequest`: `title`, `occurredDate`, `category`, `what`, `why`, `how`, `links: List<NewsEventLinkDto>`. Bean Validation: `@NotBlank`, `@Size(max=...)`, `@NotNull`. `toCommand(userId)` 메서드 보유.
- `NewsEventLinkDto`: `title`, `url` — 입출력 공용.
- `NewsEventDetailResponse` / `NewsEventListResponse`: `from(...)` 정적 팩토리.

`NewsJournalSecurityContext`: stocknote의 `StockNoteSecurityContext`와 동일 패턴 (Authentication principal이 `Long`인지 검사, 아니면 `InsufficientAuthenticationException`).

`NewsJournalExceptionHandler` (`@RestControllerAdvice(basePackageClasses = NewsJournalController.class)`):
- `NewsEventNotFoundException` → 404
- `InsufficientAuthenticationException` → 401
- `IllegalArgumentException`, `MethodArgumentNotValidException` → 400 (메시지 포함)

[예시 코드](./examples/presentation-example.md) — 후속 작업

### 프론트엔드

위치
- `static/index.html`
- `static/js/app.js`
- `static/js/api.js`
- `static/js/components/news-journal.js` (신규)

`index.html`
- `<aside>`/메뉴 영역에 `news-journal` 항목 추가 (아이콘 선택은 기존 메뉴와 통일).
- `<main>`에 `<section x-show="currentMenu === 'news-journal'" x-data="newsJournalComponent()">` 영역 추가.

`app.js`
- `menus` 배열에 `{ key: 'news-journal', label: '뉴스 기록', icon: ... }` 추가.

`api.js`
- `createNewsEvent(payload)`, `getNewsEvents(params)`, `getNewsEvent(id)`, `updateNewsEvent(id, payload)`, `deleteNewsEvent(id)`.

`news-journal.js`
- `state`: `events` (목록), `filter` (category/from/to/page/size), `detail` (상세 모달용), `form` (생성/수정 폼), `mode` (`'create' | 'edit' | 'view' | null`).
- `init()`에서 `loadEvents()` 호출.
- 생성/수정 폼 검증 (제목 필수, 미래일자 차단).
- 링크 추가/삭제: 폼 안에서 동적 행 추가, displayOrder는 배열 인덱스.
- 타임라인 마크업: 좌측 수직 라인 + 일자 그룹 도트 + 카드. 카테고리별 색상 (호재 emerald, 악재 rose, 중립 slate).
- 페이지네이션: 단순 prev/next + page 인디케이터.

[예시 코드](./examples/frontend-example.md) — 후속 작업

## 주의사항

- **N+1 회피**: 목록 조회 시 사건 페이지를 가져온 뒤 `IN (eventIds)`로 링크를 일괄 조회하여 메모리에서 그룹핑한다. 트랜잭션 안에서 처리 (`open-in-view: false`).
- **자식 replace-all 정책**: update 트랜잭션에서 `linkRepository.deleteByEventId(eventId)` → `saveAll(newLinks)`. 동일 트랜잭션이므로 외부에서 빈 상태 노출 위험 없음.
- **소유권 미스매치 = 404**: 권한 부족도 일관되게 404로 응답하여 ID enumeration 방지 (stocknote security 리뷰 권고와 동일).
- **userId 출처 단일화**: 컨트롤러에서 `NewsJournalSecurityContext.currentUserId()`만 사용. 쿼리스트링/바디로 받지 않음.
- **Entity 연관관계 금지**: `NewsEventLinkEntity`에 `@ManyToOne` 등 사용 금지, `event_id` 컬럼만. CLAUDE.md 규칙.
- **테스트 가능성**: `NewsEvent.create / updateBody`에 `LocalDate today`를 외부 주입하여 미래 일자 검증을 결정적으로 단위테스트 가능하게 한다. 테스트 코드 작성은 본 설계 범위 외 (CLAUDE.md: 명시 요청 시).
- **2차 확장 여지**: 결과 추가/학습 메모는 컬럼 추가만으로 가능 (`ddl-auto: update`). API는 이번 응답 스키마에 옵셔널 필드를 추가하는 형태로 무중단 확장.

## 작업 순서

1. **백엔드 도메인 + 영속화** 일괄 → 컴파일/기동 확인 (DB 테이블 자동 생성 검증)
2. **애플리케이션 서비스** → 단위 동작 점검 (수동 호출)
3. **표현 계층 + ExceptionHandler** → curl/HTTPie로 5개 엔드포인트 검증
4. **프론트엔드** → 메뉴 진입, 생성/조회/수정/삭제 골든패스 + 타임라인 시각 확인
5. (선택) examples 코드 파일 작성 — 본문 안 링크 위치에 후속 보강