# 뉴스 사건 주제 분류(NewsEventCategory) 도입 + EventCategory → EventImpact 리네임 설계

> 1차 MVP 후속 — 사건의 주제 분류 차원을 추가하고, 동시에 기존 시장영향 enum 의 의미를 명확히 하기 위해 명명 정리.
> 관련: `.claude/designs/newsjournal/news-journal/news-journal.md` (1차 MVP)

## 작업 리스트

### Phase A — 명명 정리 (EventCategory → EventImpact 일괄 리네임)
- [x] `domain/model/EventCategory.java` → `EventImpact.java` 리네임 (enum 상수 `GOOD/BAD/NEUTRAL` 유지)
- [x] 도메인 `NewsEvent.category` 필드 → `impact` (`getCategory` → `getImpact`, `create / updateBody` 시그니처 갱신)
- [x] 영속성 `NewsEventEntity.category` 필드 → `impact`, `@Column(name = "impact")`
- [x] 영속성 `@Index` 이름 `idx_news_event_user_category` → `idx_news_event_user_impact`
- [x] `NewsEventMapper` 본체 매핑 갱신
- [x] 도메인 포트 `NewsEventListFilter.category` → `impact`
- [x] `NewsEventJpaRepository.findList / countList` JPQL 의 `e.category` → `e.impact`
- [x] application DTO: `CreateNewsEventCommand.category` → `impact`, `UpdateNewsEventCommand.category` → `impact`
- [x] presentation Request/Response: `CreateNewsEventRequest.category` → `impact`, `UpdateNewsEventRequest.category` → `impact`, `NewsEventDetailResponse.category` → `impact`, `NewsEventListResponse.ItemDto.category` → `impact`
- [x] `NewsJournalController.findList` 쿼리 파라미터 `category` → `impact` (타입은 enum 그대로)
- [x] 프론트 `news-journal.js`: `form.category` → `form.impact`, `filter.category` → `filter.impact`, `newsJournalCategoryLabel/BadgeClass/DotClass` → `newsJournalImpactLabel/BadgeClass/DotClass`, payload 필드 `category` → `impact`
- [x] 프론트 `index.html`: 모든 `category` 바인딩/필터/배지 → `impact` 로 텍스트·바인딩 일괄 갱신 (단, "카테고리" 라벨은 유지하지 않고 "시장영향" 등 의미 명확한 텍스트로 교체)
- [x] 운영 DDL: `ALTER TABLE news_event RENAME COLUMN category TO impact;` + `ALTER INDEX idx_news_event_user_category RENAME TO idx_news_event_user_impact;` (db/migration/news_event_impact_rename.sql 동봉)
- [x] 컴파일 + 부팅 회귀 검증 (compileJava BUILD SUCCESSFUL — 부팅 회귀는 운영 DDL 선적용 후 검증 필요)

### Phase B — NewsEventCategory 신규 도입

#### B-1. 백엔드 — 도메인 / 포트
- [x] `NewsEventCategory` 도메인 모델 + 정적 팩토리 `create(userId, name)` (name trim, 길이 ≤ 50)
- [x] `NewsEventCategoryRepository` 포트 — `findByUserIdAndName`, `findByUserIdOrderByNameAsc`, `findByIdAndUserId`, `save`

#### B-2. 백엔드 — 영속화
- [x] `NewsEventCategoryEntity` (테이블 `news_event_category`, unique `(user_id, name)`)이
- [x] `NewsEventCategoryJpaRepository` (Spring Data 파생 메서드 + `Optional` 반환)
- [x] `NewsEventCategoryRepositoryImpl` 어댑터
- [x] `NewsEventMapper` 에 `toEntity(NewsEventCategory)` / `toDomain(NewsEventCategoryEntity)` 추가

#### B-3. 백엔드 — 기존 사건 모델 변경 (사전 승인 영역)
- [x] `NewsEvent` 도메인에 `Long categoryId` 필드 + 검증 (`create / updateBody` 시그니처에 categoryId 포함)
- [x] `NewsEventEntity` 에 `category_id BIGINT` 컬럼 추가 (FK 미사용, ID 참조)
- [x] `NewsEventEntity` 신규 인덱스 `@Index(name = "idx_news_event_user_category_date", columnList = "user_id, category_id, occurred_date DESC, id DESC")` 추가
- [x] `NewsEventMapper` 본체 매핑에 categoryId 반영
- [x] `NewsEventListFilter` 에 `Long categoryId` 추가 (nullable)
- [x] `NewsEventJpaRepository.findList / countList` JPQL 에 `:categoryId IS NULL OR e.categoryId = :categoryId` 절 추가

#### B-4. 백엔드 — 애플리케이션
- [x] `CreateNewsEventCommand` / `UpdateNewsEventCommand` 에 `String categoryName` 필드 추가
- [x] `NewsEventCategoryService` 신규 — `resolve(userId, name)` (find or create), `findByUserId(userId)`, race 방어용 `DataIntegrityViolationException` retry 1회
- [x] `NewsEventWriteService.create / update` — `categoryService.resolve(userId, cmd.categoryName())` → categoryId 주입
- [x] `NewsEventReadService.findById / findList` — 결과에 `NewsEventCategory` 동봉 (단건 단일 조회, 목록 IN-batch)
- [x] Result record 확장: `NewsEventDetailResult.category`, `NewsEventListItemResult.category` (도메인 모델 임베드)

#### B-5. 백엔드 — 표현
- [x] Request DTO: `CreateNewsEventRequest.category` (필드명 그대로) — `@NotBlank @Size(max = 50) String category` (Phase A에서 시장영향이 `impact`로 빠지므로 `category` 가용. 의미는 *주제 분류명*)
- [x] Update Request 동일
- [x] Response: `CategoryDto(Long id, String name)` 신규 + `NewsEventDetailResponse.category: CategoryDto`, `NewsEventListResponse.ItemDto.category: CategoryDto`
- [x] `NewsJournalController.findList` 신규 쿼리 파라미터 `?categoryId=` (Long, nullable)
- [x] 신규 엔드포인트 `GET /api/news-journal/categories` → `[{id, name}]` (별도 컨트롤러 `NewsEventCategoryController` 분리)

#### B-6. 데이터 마이그레이션 / 부팅
- [x] `NewsJournalBootstrap` (`ApplicationRunner`) — 사용자별 "기타" 카테고리 idempotent 생성, 기존 `news_event.category_id IS NULL` 행을 "기타" 로 backfill
- [x] `db/migration/news_event_category_not_null.sql` — 운영 DB 수동 적용용 (NOT NULL 강화. `news_event_category` 테이블 / unique / 인덱스 자체는 ddl-auto 가 생성하므로 SQL 미포함)

#### B-7. 프론트엔드
- [x] `static/js/api.js` — `getNewsEventCategories()`
- [x] `static/js/components/news-journal.js`
  - state 확장: `categories: []`, `activeCategoryId: null`, `form.category: ''`
  - 메서드: `newsJournalLoadCategories()`, `newsJournalSelectCategoryTab(id)`
  - `newsJournalLoad()` 의 params 에 `categoryId` 추가
  - 폼 검증: `form.category` 필수 + 길이 50
  - 저장 응답 후 카테고리 목록 갱신 (새 분류가 자동 등록되었을 가능성)
- [x] `index.html` — news-journal 섹션
  - 헤더 아래 가로 스크롤 탭 행 ("전체" + 사용자 카테고리 chips), 활성 탭 강조
  - 사건 모달 폼: 분류명 input + `<datalist id="newsJournalCategoryOptions">` 자동완성
  - 타임라인 카드: 카테고리 이름 배지(중립 톤) + 시장영향 배지(impact) 병기

### 보류 (본 설계 범위 외)
- 카테고리 이름 변경 / 삭제 / 정렬 / 색상
- 미사용 카테고리 정리 도구
- 다중 사용자 환경의 글로벌(공용) 카테고리

---

## 배경

(1) 1차 MVP 의 사건은 단일 타임라인에 시간 역순으로 누적되어 주제(경제/부동산/IT 등)별 회고가 어렵다.
(2) 기존 enum `EventCategory`(GOOD/BAD/NEUTRAL)는 *시장 영향* 차원이지만 명칭이 "category" 라 *주제 분류*와 의미 혼동 위험. 신규 분류 차원 도입과 동시에 `EventImpact` 로 리네임해 의미를 명확히 한다.

## 핵심 결정

- **명명 통일**: 시장영향 enum `EventCategory` → `EventImpact`. DB 컬럼 / API 파라미터 / 응답 필드 / 프론트 모두 `category` → `impact` 일괄 리네임. 운영 DB 컬럼은 `ALTER TABLE … RENAME COLUMN` 으로 정리.
- **신규 분류**: `NewsEventCategory` 엔티티 신설 (테이블 `news_event_category`). 사건은 `news_event.category_id` 로 1:N 참조. Entity 연관관계 미사용 (CLAUDE.md 규칙).
- **소유권 / 매칭**: 사용자별 카테고리. unique `(user_id, name)`. 매칭은 `name.trim()` 후 정확 일치 (case-sensitive).
- **자동 등록**: 사건 입력 시 사용자가 분류 *이름* 입력 → 서비스가 `findByUserIdAndName` → 없으면 즉시 INSERT 후 ID 회수. race 시 `DataIntegrityViolationException` 캐치 후 1회 재조회.
- **필수 입력**: `category_id NOT NULL` (운영 DB 는 backfill 후 수동 ALTER).
- **마이그레이션**: `ddl-auto: update` 의 NOT NULL 제약 변경 한계 회피 — 컬럼은 nullable 추가, 부팅 backfill 후 운영 수동 ALTER.
- **UI**: 상단 가로 탭 (전체 + 사용자 카테고리). 폼은 datalist 자동완성. 카드는 카테고리 배지 + 시장영향(impact) 배지 병기.
- **API 호환성**: 신규 파라미터 `?categoryId=` (Long), 기존 `?category=GOOD/BAD/NEUTRAL` 은 `?impact=` 로 리네임. 응답 필드도 `impact` (시장영향) + `category: {id, name}` (주제) 분리.
- **단계화**: Phase A (리네임) → Phase B (신규 도입) 순. 각 Phase 끝에 컴파일 + 부팅 회귀 검증.

## 데이터 모델

### `news_event_category` 신규 테이블

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT (PK) | IDENTITY |
| user_id | BIGINT | NOT NULL |
| name | VARCHAR(50) | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |

- unique: `(user_id, name)` — 자동 등록 시 중복 방지

### `news_event` 변경

- 컬럼 리네임: `category` → `impact` (Phase A, ALTER TABLE … RENAME COLUMN)
- 컬럼 신규: `category_id BIGINT` (Phase B, 1차 nullable → 운영 backfill 후 NOT NULL)
- 인덱스 리네임: `idx_news_event_user_category` → `idx_news_event_user_impact` (Phase A)
- 인덱스 신규: `idx_news_event_user_category_date (user_id, category_id, occurred_date DESC, id DESC)` (Phase B)

## 구현 위치

### Phase A 영향 파일

`category` → `impact` 리네임 일괄:
- `newsjournal/domain/model/EventCategory.java` → `EventImpact.java`
- `newsjournal/domain/model/NewsEvent.java` (필드/검증/팩토리/업데이트 시그니처)
- `newsjournal/infrastructure/persistence/NewsEventEntity.java` (컬럼/인덱스)
- `newsjournal/infrastructure/persistence/NewsEventJpaRepository.java` (JPQL)
- `newsjournal/infrastructure/persistence/mapper/NewsEventMapper.java`
- `newsjournal/domain/repository/NewsEventListFilter.java`
- `newsjournal/application/dto/CreateNewsEventCommand.java`, `UpdateNewsEventCommand.java`
- `newsjournal/presentation/dto/CreateNewsEventRequest.java`, `UpdateNewsEventRequest.java`, `NewsEventDetailResponse.java`, `NewsEventListResponse.java`
- `newsjournal/presentation/NewsJournalController.java`
- `static/js/components/news-journal.js`
- `static/index.html` (news-journal 섹션 + 모달 + 타임라인 카드)
- `db/migration/news_event_impact_rename.sql` (운영 적용용)

[Phase A 예시 코드](./examples/phase-a-rename-example.md) — 후속 작업

### Phase B 영향 파일

신규 추가:
- `newsjournal/domain/model/NewsEventCategory.java`
- `newsjournal/domain/repository/NewsEventCategoryRepository.java`
- `newsjournal/infrastructure/persistence/NewsEventCategoryEntity.java`
- `newsjournal/infrastructure/persistence/NewsEventCategoryJpaRepository.java`
- `newsjournal/infrastructure/persistence/NewsEventCategoryRepositoryImpl.java`
- `newsjournal/application/NewsEventCategoryService.java`
- `newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java`
- `newsjournal/presentation/dto/CategoryDto.java` (또는 NewsEventDetailResponse 안 record)
- `db/migration/news_event_category_not_null.sql` (운영 NOT NULL + 인덱스)

기존 변경:
- `NewsEvent.java` / `NewsEventEntity.java` (categoryId 필드/컬럼/인덱스 추가)
- `NewsEventMapper.java` (본체 매핑 갱신 + 카테고리 매핑 추가)
- `NewsEventListFilter.java` (categoryId 추가)
- `NewsEventJpaRepository.java` (JPQL 절 추가)
- `NewsEventWriteService.java` / `NewsEventReadService.java` (resolve + 결과 동봉)
- `CreateNewsEventCommand.java` / `UpdateNewsEventCommand.java` (categoryName 추가)
- `CreateNewsEventRequest.java` / `UpdateNewsEventRequest.java` (category 필드 추가)
- `NewsEventDetailResponse.java` / `NewsEventListResponse.java` (category 필드 추가)
- `NewsJournalController.java` (categoryId 파라미터 + 카테고리 목록 엔드포인트)
- `news-journal.js` / `index.html` / `api.js` (탭 + 폼 datalist + 배지)

[Phase B 백엔드 예시](./examples/phase-b-backend-example.md), [Phase B 프론트엔드 예시](./examples/phase-b-frontend-example.md) — 후속 작업

## 주의사항

- **컬럼 rename + 자바 배포 순서**: Phase A 의 자바 코드는 `@Column(name = "impact")` 를 가짐. 운영 배포 직전에 `ALTER TABLE news_event RENAME COLUMN category TO impact;` 를 먼저 적용하지 않으면, ddl-auto 가 신규 컬럼 `impact` 를 NULL 로 추가하고 기존 `category` 를 그대로 둬 데이터 분리 사고. **DDL → 자바 배포** 순서 필수.
- **인덱스 이름 동기화**: `@Index(name = ...)` 는 ddl-auto 가 변경된 이름을 자동 적용하지 않음. 운영 DB 에서 `ALTER INDEX … RENAME TO …` 수동 적용.
- **Phase A 와 Phase B 의 단계 분리 권장**: 한 PR 로 묶으면 영향 파일 30+, 리뷰/롤백 단위 큼. 본 설계는 단일 설계지만 *커밋/배포* 단위는 Phase A 마침 → 검증 → Phase B 로 분할 권장.
- **자동 등록 race**: 1인 환경이라 실위험 낮음. `DataIntegrityViolationException` 캐치 + 1회 재조회로 충분. 대규모 사용자 시 `INSERT … ON CONFLICT DO NOTHING` 또는 `ON CONFLICT (user_id, name) DO UPDATE SET name = EXCLUDED.name RETURNING id` 패턴 검토.
- **신규 사용자 흐름**: 카테고리 0개 사용자가 첫 사건 등록 시 datalist 비어있음 → 자동 등록 흐름이 정상 동작해야 함. ApplicationRunner 의 "기타" 자동 생성은 *기존* 사용자 backfill 용이며, 신규 사용자는 첫 입력으로 카테고리가 만들어진다.
- **응답 shape 변경**: `category` (시장영향 enum) → `impact` 로 키 변경 + `category: {id, name}` 신설. 단일 프론트라 동시 배포로 호환. 외부 API 클라이언트 없음 확인됨.
- **운영 DDL 누락 시 증상**: Phase A 적용 후 ALTER COLUMN RENAME 누락 시 부팅은 성공하나 `impact` 컬럼이 NULL 로 추가되고 기존 데이터는 `category` 컬럼에 잔류 — 모든 조회가 NULL 반환. 부팅 직후 quick check 권장.
- **prod NOT NULL 제약**: Phase B 의 `category_id NOT NULL` 은 부팅 backfill 후 운영자가 수동 ALTER 적용. 자바 코드 자체는 `nullable = false` 를 선언하지 않거나(컬럼 정의에서) 또는 ApplicationRunner 보장 후 다음 배포에서 `nullable = false` 로 강화 — 단계 분리.

## 작업 순서 권장

1. **Phase A 일괄** — 자바 + API + 프론트 + 운영 DDL → 컴파일 + 부팅 검증 → 커밋/배포
2. **Phase A 회귀 통과 확인** (사건 생성/조회/수정/삭제 + impact 필터 정상)
3. **Phase B-1 ~ B-2** — NewsEventCategory 신규 모델/포트/영속화
4. **Phase B-3** — NewsEvent 에 categoryId 추가 (사전 승인 필수, Entity 변경)
5. **Phase B-4 ~ B-5** — 애플리케이션 + 표현 (자동 등록, 응답 동봉, 카테고리 목록 엔드포인트)
6. **Phase B-6** — 부팅 backfill + 운영 NOT NULL DDL
7. **Phase B-7** — 프론트엔드 (탭, datalist, 배지)
8. **회귀 검증** — 신규 사건 + 기존 사건 + 신규/기존 사용자 모두 정상 노출