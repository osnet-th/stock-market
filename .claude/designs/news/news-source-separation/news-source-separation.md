# News-NewsSource 테이블 분리 설계

## 작업 리스트

- [x] NewsSource 도메인 모델 생성
- [x] NewsSourceEntity JPA Entity 생성
- [x] NewsSourceRepository (도메인 인터페이스) 생성
- [x] NewsSourceJpaRepository + NewsSourceRepositoryImpl 생성
- [x] NewsSourceMapper 생성
- [x] News 도메인 모델에서 userId, purpose, sourceId 필드 제거 (region 유지)
- [x] NewsEntity에서 userId, purpose, sourceId 컬럼 제거 + unique 제약조건 유지 (region 유지)
- [x] NewsJpaRepository 수정 (insertIgnoreDuplicate SQL 변경, 쿼리 메서드 변경)
- [x] NewsRepositoryImpl 수정 (insertIgnoreDuplicate → 뉴스 ID 반환 방식 변경)
- [x] NewsRepository 도메인 인터페이스 수정
- [x] NewsMapper 수정
- [x] NewsSaveService 수정 (뉴스 저장 + NewsSource 매핑 분리)
- [x] NewsQueryService 수정 (findExistingUrls → 사용자별 중복 검사로 변경)
- [x] KeywordNewsBatchServiceImpl 수정 (filterNewNews 로직 변경)
- [x] NewsController + NewsQueryService 조회 로직 수정 (NewsSource 기반 조회)
- [x] NewsDto 수정 (NewsSource 정보 포함)
- [x] NewsSaveRequest 수정
- [x] KeywordServiceImpl.deleteKeyword() 삭제 로직 수정
- [x] PortfolioService.deleteItem() 삭제 로직 수정

## 배경

현재 `news` 테이블의 `original_url`에 글로벌 UNIQUE 제약조건이 걸려 있어, 동일 URL의 뉴스는 시스템 전체에서 1건만 저장된다. 사용자 A가 "삼성전자" 키워드로 먼저 수집하면, 사용자 B의 동일 키워드 수집에서 같은 URL이 모두 제외되는 문제가 있다.

뉴스 콘텐츠(news)와 수집 출처 정보(news_source)를 분리하여, 뉴스는 URL 기준으로 유일하게 저장하되, "누가 어떤 목적으로 수집했는가"는 별도 테이블로 관리한다.

## 핵심 결정

- **news 테이블**: 순수 뉴스 콘텐츠만 보관 (original_url UNIQUE 유지)
- **news_source 테이블**: 사용자-뉴스 매핑 관계 관리 (user_id, purpose, source_id, region)
- **중복 기준 변경**: URL 전역 중복 → `(news_id, user_id, purpose, source_id)` 복합 유니크
- **저장 흐름**: 뉴스 INSERT OR IGNORE → 뉴스 ID 조회 → NewsSource INSERT OR IGNORE
- **삭제 흐름**: NewsSource만 삭제 (news 콘텐츠는 보존, 추후 배치로 orphan 정리 가능)

## 테이블 구조 변경

### 변경 후 news 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 생성 |
| original_url | VARCHAR(500) UNIQUE | 뉴스 원본 URL |
| title | VARCHAR(500) NOT NULL | 제목 |
| content | TEXT | 본문 |
| published_at | DATETIME NOT NULL | 발행일 |
| created_at | DATETIME NOT NULL | 저장일 |
| region | VARCHAR(50) | API 검색 출처 (DOMESTIC / INTERNATIONAL) |

제거 컬럼: `user_id`, `purpose`, `source_id`
제거 인덱스: `idx_news_purpose_source_published`

### 신규 news_source 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 생성 |
| news_id | BIGINT NOT NULL | news.id 참조 |
| user_id | BIGINT NOT NULL | 수집한 사용자 ID |
| purpose | VARCHAR(100) NOT NULL | KEYWORD / STOCK / PORTFOLIO |
| source_id | BIGINT NOT NULL | keyword.id 또는 portfolioItem.id |
| created_at | DATETIME NOT NULL | 매핑 생성일 |

유니크 제약: `(news_id, user_id, purpose, source_id)`
인덱스: `(user_id, purpose, source_id, created_at DESC)` — 사용자별 뉴스 조회용

## 구현

### NewsSource 도메인 모델
위치: `news/domain/model/NewsSource.java`

[예시 코드](./examples/domain-model-example.md)

### NewsSourceEntity
위치: `news/infrastructure/persistence/NewsSourceEntity.java`

[예시 코드](./examples/infrastructure-entity-example.md)

### NewsSourceRepository + 구현체
위치:
- `news/domain/repository/NewsSourceRepository.java`
- `news/infrastructure/persistence/NewsSourceJpaRepository.java`
- `news/infrastructure/persistence/NewsSourceRepositoryImpl.java`
- `news/infrastructure/persistence/mapper/NewsSourceMapper.java`

[예시 코드](./examples/infrastructure-repository-example.md)

### News 도메인 모델 변경
위치: `news/domain/model/News.java`

- `userId`, `purpose`, `sourceId`, `region` 필드 제거
- `News.create()` 파라미터에서 해당 필드 제거
- validate 메서드에서 해당 필드 검증 제거

[예시 코드](./examples/domain-model-example.md)

### NewsEntity 변경
위치: `news/infrastructure/persistence/NewsEntity.java`

- `userId`, `purpose`, `sourceId`, `region` 컬럼 제거
- 인덱스 `idx_news_purpose_source_published` 제거
- `original_url` UNIQUE 제약조건 유지

[예시 코드](./examples/infrastructure-entity-example.md)

### NewsRepository + NewsJpaRepository 변경
위치:
- `news/domain/repository/NewsRepository.java`
- `news/infrastructure/persistence/NewsJpaRepository.java`
- `news/infrastructure/persistence/NewsRepositoryImpl.java`

주요 변경:
- `insertIgnoreDuplicate`: userId/purpose/sourceId/region 파라미터 제거
- `findByPurpose`, `findByPurposeAndSourceId`, `deleteByPurposeAndSourceId` 제거 (NewsSourceRepository로 이동)
- `insertIgnoreDuplicateAndReturnId` 추가: INSERT 후 ID 반환 (ON CONFLICT 시 기존 ID 조회)

[예시 코드](./examples/infrastructure-repository-example.md)

### NewsSaveService 변경
위치: `news/application/NewsSaveService.java`

저장 흐름 변경:
1. News INSERT OR IGNORE (URL 기준 중복 무시)
2. news ID 조회 (findByOriginalUrl)
3. NewsSource INSERT OR IGNORE (매핑 중복 무시)

[예시 코드](./examples/application-service-example.md)

### NewsQueryService 변경
위치: `news/application/NewsQueryService.java`

- `findExistingUrls(urls)` → `findExistingUrlsByUser(urls, userId)` 변경
  - news_source 테이블에서 해당 사용자가 이미 수집한 URL만 조회

[예시 코드](./examples/application-service-example.md)

### KeywordNewsBatchServiceImpl 변경
위치: `news/application/KeywordNewsBatchServiceImpl.java`

- `filterNewNews()`: 사용자별 중복 필터링으로 변경
- `NewsSaveRequest`에서 userId, purpose, sourceId, region은 그대로 전달 (NewsSaveService에서 news/news_source 분리 저장)

[예시 코드](./examples/application-service-example.md)

### NewsController + 조회 로직 변경
위치: `news/presentation/NewsController.java`

- 조회 시 NewsSource 기반으로 news 조인 조회
- `GET /api/news?purpose=KEYWORD&sourceId=10` → NewsSource에서 해당 조건의 newsId 목록 → News 조회

[예시 코드](./examples/application-service-example.md)

### 삭제 로직 변경

- `KeywordServiceImpl.deleteKeyword()`: `newsRepository.deleteByPurposeAndSourceId()` → `newsSourceRepository.deleteByPurposeAndSourceId()` 변경
- `PortfolioService.deleteItem()`: 동일하게 변경
- news 콘텐츠는 삭제하지 않음 (다른 사용자가 참조할 수 있으므로)

## 주의사항

- **마이그레이션 순서**: news_source 테이블 생성 → 기존 news 데이터에서 news_source 데이터 마이그레이션 → news 테이블 컬럼 제거. Flyway 마이그레이션이 없으므로 DDL을 `schema.sql` 또는 JPA auto-ddl로 관리
- **기존 데이터**: 현재 news 테이블의 userId, purpose, sourceId, region 데이터를 news_source로 이관 필요
- **Orphan News 관리**: NewsSource가 모두 삭제된 news 레코드는 즉시 삭제하지 않음. 필요 시 배치로 정리
- **조회 성능**: news_source 기반 조회 시 news와 JOIN 필요. 인덱스 `(user_id, purpose, source_id, created_at DESC)` 로 커버