# 뉴스 시스템 키워드 구독 모델 리팩토링

## 배경

현재 뉴스 시스템은 `purpose`(KEYWORD/PORTFOLIO)로 뉴스 출처를 구분하고, `news` 테이블에 소유권 정보(`user_id`, `purpose`, `source_id`)가 직접 포함되어 있다. 이로 인해:

1. **동일 키워드를 등록한 다른 사용자가 이미 수집된 뉴스를 볼 수 없음** — `source_id`가 사용자별 keywordId에 종속
2. **뉴스 저장 시 URL 중복으로 IGNORE되면 매핑도 생성되지 않음**
3. **키워드/포트폴리오 뉴스가 별도 수집 로직**으로 분리되어 있어 불필요한 복잡성

## 핵심 결정

- `keyword`를 사용자 소유가 아닌 **공유 리소스**로 변경
- `user_keyword` 테이블로 **사용자-키워드 구독 관계**(N:M) 관리
- `news` 테이블에서 소유권 정보 제거, `keyword_id`만 참조
- 포트폴리오 뉴스 ON = 해당 종목명으로 `keyword` + `user_keyword` 생성
- `purpose` 구분 제거, 수집 스케줄러 1개로 통합

## 변경 후 구조

```
keyword (공유)
  id=1, keyword="삼성전자", region=DOMESTIC, active=true

user_keyword (구독 관계 N:M)
  user_id=A, keyword_id=1, active=true    ← A가 직접 등록
  user_id=B, keyword_id=1, active=true    ← B가 포트폴리오에서 뉴스 ON

news (순수 뉴스 데이터)
  id=100, keyword_id=1, url, title, content, publishedAt, region
```

### 조회 흐름

```
user → user_keyword(active=true) → keyword → news
```

### 수집 흐름

```
KeywordNewsBatchScheduler → active=true인 keyword 조회 → 뉴스 검색 → news 저장 (keyword_id 기반)
```

### 삭제 흐름

```
user_keyword 삭제 → 해당 keyword 구독자 0명 → keyword + news 일괄 삭제
```

---

## 작업 리스트

### Phase 1: 신규 테이블 및 도메인 모델 생성

- [x] 1-1. `UserKeyword` 도메인 모델 생성
  - 파일: `news/domain/model/UserKeyword.java`
  - 예시: [user-keyword-domain-example.md](examples/user-keyword-domain-example.md)

- [x] 1-2. `UserKeywordEntity` JPA Entity 생성
  - 파일: `news/infrastructure/persistence/UserKeywordEntity.java`
  - 테이블: `user_keyword` (user_id, keyword_id, active, created_at, updated_at)
  - UNIQUE: (user_id, keyword_id)
  - 예시: [user-keyword-entity-example.md](examples/user-keyword-entity-example.md)

- [x] 1-3. `UserKeywordRepository` 도메인 인터페이스 + JPA Repository + Impl + Mapper 생성
  - 파일: `news/domain/repository/UserKeywordRepository.java`
  - 파일: `news/infrastructure/persistence/UserKeywordJpaRepository.java`
  - 파일: `news/infrastructure/persistence/UserKeywordRepositoryImpl.java`
  - 파일: `news/infrastructure/persistence/mapper/UserKeywordMapper.java`
  - 예시: [user-keyword-repository-example.md](examples/user-keyword-repository-example.md)

### Phase 2: Keyword 공유 모델로 변경

- [x] 2-1. `Keyword` 도메인 모델에서 `userId`, `active` 제거
  - 파일: `news/domain/model/Keyword.java`
  - `create(keyword, region)` — userId 파라미터 제거, active 필드 제거 (user_keyword.active로 이관)
  - `activate()`, `deactivate()` 메서드 제거
  - 예시: [keyword-domain-example.md](examples/keyword-domain-example.md)

- [x] 2-2. `KeywordEntity`에서 `user_id`, `active` 컬럼 제거, `(keyword, region)` UNIQUE 추가
  - 파일: `news/infrastructure/persistence/KeywordEntity.java`

- [x] 2-3. `KeywordRepository` 인터페이스 변경
  - 파일: `news/domain/repository/KeywordRepository.java`
  - 삭제: `findByUserId()`, `findByUserIdAndActive()`, `existsByUserIdAndKeyword()`
  - 추가: `findByKeywordAndRegion()`, `existsByKeywordAndRegion()`

- [x] 2-4. `KeywordJpaRepository`, `KeywordRepositoryImpl` 변경
  - 파일: `news/infrastructure/persistence/KeywordJpaRepository.java`
  - 파일: `news/infrastructure/persistence/KeywordRepositoryImpl.java`

- [x] 2-5. `KeywordService` / `KeywordServiceImpl` 변경
  - 파일: `news/application/KeywordService.java`
  - 파일: `news/application/KeywordServiceImpl.java`
  - `registerKeyword()`: 기존 키워드 재사용 + `UserKeyword` 생성
  - `deleteKeyword()`: `UserKeyword` 삭제 → 구독자 0명이면 keyword + news 삭제
  - 삭제: `getKeywordsByUserId()`, `getActiveKeywordsByUserId()`
  - 추가: `getKeywordsByUser(userId)` — user_keyword JOIN으로 조회
  - 예시: [keyword-service-example.md](examples/keyword-service-example.md)

- [x] 2-6. `RegisterKeywordRequest`에서 `userId` 제거
  - 파일: `news/application/dto/RegisterKeywordRequest.java`

- [x] 2-7. `KeywordController` 변경
  - 파일: `news/presentation/KeywordController.java`
  - GET: userId 기반 → user_keyword 기반 조회
  - POST: registerKeyword 시 userId는 user_keyword에만 사용

### Phase 3: News 테이블 소유권 분리

- [x] 3-1. `News` 도메인 모델에서 `userId`, `purpose`, `sourceId` 제거, `keywordId` 추가
  - 파일: `news/domain/model/News.java`
  - 예시: [news-domain-example.md](examples/news-domain-example.md)

- [x] 3-2. `NewsEntity`에서 컬럼 변경
  - 파일: `news/infrastructure/persistence/NewsEntity.java`
  - 제거: `user_id`, `purpose`, `source_id`
  - 추가: `keyword_id`
  - 인덱스: `idx_news_keyword_published (keyword_id, published_at DESC)`

- [x] 3-3. `NewsRepository` 인터페이스 변경
  - 파일: `news/domain/repository/NewsRepository.java`
  - 삭제: `findByPurpose()`, `findByPurposeAndSourceId()`, `deleteByPurposeAndSourceId()`
  - 추가: `findByKeywordId()`, `deleteByKeywordId()`

- [x] 3-4. `NewsJpaRepository`, `NewsRepositoryImpl` 변경
  - 파일: `news/infrastructure/persistence/NewsJpaRepository.java`
  - 파일: `news/infrastructure/persistence/NewsRepositoryImpl.java`
  - `insertIgnoreDuplicate` 네이티브 쿼리에서 `user_id`, `purpose`, `source_id` 제거, `keyword_id` 추가

- [x] 3-5. `NewsMapper` 변경
  - 파일: `news/infrastructure/persistence/mapper/NewsMapper.java`

- [x] 3-6. `NewsDto`에서 `purpose`, `sourceId` 제거, `keywordId` 추가
  - 파일: `news/application/dto/NewsDto.java`

- [x] 3-7. `NewsSaveRequest`에서 `userId`, `sourceId` 제거, `keywordId` 추가
  - 파일: `news/application/dto/NewsSaveRequest.java`

### Phase 4: 수집/조회 서비스 변경

- [x] 4-1. `NewsSaveService` 변경 — `purpose` 파라미터 제거
  - 파일: `news/application/NewsSaveService.java`
  - `save(request)`, `saveBatch(requests)` — purpose 파라미터 제거

- [x] 4-2. `NewsQueryService` 변경 — keyword 기반 조회
  - 파일: `news/application/NewsQueryService.java`
  - 삭제: `getByPurpose()`
  - 변경: `getNewsBySource()` → `getNewsByKeywordId(keywordId, page, size)`

- [x] 4-3. `KeywordNewsBatchServiceImpl` 변경
  - 파일: `news/application/KeywordNewsBatchServiceImpl.java`
  - `collectByKeyword()`: userId 제거, keywordId 기반 저장
  - `collectBySource()`: purpose 제거
  - `KeywordSearchContext`(또는 `KeywordSearchContext.java`): userId 제거

- [x] 4-4. `NewsController` 변경
  - 파일: `news/presentation/NewsController.java`
  - GET: `purpose` + `sourceId` → `keywordId`
  - POST: `purpose`, `userId` 제거

- [x] 4-5. `NewsCollectRequest` 변경
  - 파일: `news/presentation/dto/NewsCollectRequest.java`
  - `purpose`, `userId` 필드 제거

### Phase 5: 포트폴리오 뉴스 통합

- [x] 5-1. `PortfolioService.toggleNews()` 변경 — 키워드 연동
  - 파일: `portfolio/application/PortfolioService.java`
  - 뉴스 ON: 종목명으로 Keyword 조회/생성 + UserKeyword 생성
  - 뉴스 OFF: UserKeyword 비활성화
  - 예시: [portfolio-service-example.md](examples/portfolio-service-example.md)

- [x] 5-2. `PortfolioNewsBatchService` 삭제
  - 파일: `portfolio/application/PortfolioNewsBatchService.java`

- [x] 5-3. `PortfolioNewsBatchScheduler` 삭제
  - 파일: `portfolio/infrastructure/scheduler/PortfolioNewsBatchScheduler.java`

- [x] 5-4. `PortfolioNewsContext` 삭제
  - 파일: `portfolio/application/PortfolioNewsContext.java`

### Phase 6: 불필요 코드 삭제

- [x] 6-1. `NewsSource` 관련 전체 삭제 (6개 파일)
  - `news/domain/model/NewsSource.java`
  - `news/domain/repository/NewsSourceRepository.java`
  - `news/infrastructure/persistence/NewsSourceEntity.java`
  - `news/infrastructure/persistence/NewsSourceJpaRepository.java`
  - `news/infrastructure/persistence/NewsSourceRepositoryImpl.java`
  - `news/infrastructure/persistence/mapper/NewsSourceMapper.java`

- [x] 6-2. `NewsCleanupService` 삭제
  - 파일: `news/application/NewsCleanupService.java`

- [x] 6-3. `NewsPurpose` enum 삭제
  - 파일: `news/domain/model/NewsPurpose.java`

### Phase 7: Flyway 마이그레이션

- [x] 7-1. 마이그레이션 스크립트 작성
  - 파일: `src/main/resources/db/migration/V{번호}__news_keyword_subscription_refactoring.sql`
  - 예시: [migration-example.md](examples/migration-example.md)

### Phase 8: 프론트엔드 변경

- [x] 8-1. `api.js` 변경 — API 호출 파라미터 변경
  - `getKeywords()`: userId 제거
  - `registerKeyword()`: userId 제거
  - `getNewsByKeyword()`: purpose 제거, keywordId 사용
  - `getNewsByPortfolioItem()`: purpose 제거, keywordId 사용
  - `collectNewsByKeyword()`: userId, purpose 제거
  - `collectNewsByPortfolioItem()`: 삭제 (키워드 수집으로 통합)

- [x] 8-2. `app.js` 변경 — 뉴스 조회/수집 로직 변경
  - `loadKeywords()`: userId 제거
  - `addKeyword()`: userId 제거
  - `collectNews()`: userId 제거
  - 포트폴리오 뉴스 조회: user_keyword 기반으로 변경

- [x] 8-3. `index.html` 변경 — 필요 시 UI 조정

## 주의사항

- 마이그레이션 시 기존 데이터 보존 필요: keyword 테이블의 user_id를 user_keyword로 이관 후 제거
- `news` 테이블의 기존 `source_id`를 `keyword_id`로 변환하는 데이터 마이그레이션 포함
- `insertIgnoreDuplicate`의 `ON CONFLICT` 구문은 MariaDB에서는 `ON DUPLICATE KEY UPDATE` 사용 확인 필요