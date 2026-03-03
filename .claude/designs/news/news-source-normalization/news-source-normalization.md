# News 출처 정규화 설계

## 작업 리스트

- [x] News 도메인 모델 변경 (`searchKeyword` → `sourceId`)
- [x] NewsEntity 변경 (`search_keyword` → `source_id`, 복합 인덱스 추가)
- [x] NewsMapper 변경
- [x] NewsRepository(포트) 변경
- [x] NewsJpaRepository 변경 (쿼리 메서드 + native query)
- [x] NewsRepositoryImpl(어댑터) 변경
- [x] NewsSaveRequest DTO 변경
- [x] NewsDto 변경
- [x] NewsSaveService 변경
- [x] NewsQueryService 변경
- [x] KeywordSearchContext 변경
- [x] KeywordNewsBatchServiceImpl 변경
- [x] NewsController 변경 (keyword text → keyword_id 기반 조회)

## 배경

현재 `news` 테이블의 `search_keyword`가 문자열로 저장되어 있어 데이터 증가 시 쿼리 성능 저하가 예상됨.
추후 주식(Stock) 도메인 추가 시 주식명 기반 뉴스 조회/저장도 필요하므로, 다형성 FK 패턴으로 정규화.

## 핵심 결정

- **다형성 FK 패턴**: `purpose`(KEYWORD/STOCK)를 discriminator로, `source_id`를 FK로 사용
  - `purpose = KEYWORD` → `source_id`는 `keyword.id` 참조
  - `purpose = STOCK` → `source_id`는 향후 `stock.id` 참조
- **복합 인덱스**: `(purpose, source_id, published_at DESC)` → 조회 성능 보장
- **API 변경**: 키워드 조회 시 keyword text가 아닌 `keyword_id`(Long) 기반으로 변경

## 테이블 변경

### Before

```
news
├── search_keyword  VARCHAR  (인덱스 없음)
├── purpose         VARCHAR  (KEYWORD, STOCK)
└── ...
```

### After

```
news
├── source_id   BIGINT NOT NULL  (keyword.id 또는 stock.id)
├── purpose     VARCHAR          (discriminator: KEYWORD, STOCK)
├── INDEX idx_news_purpose_source_published (purpose, source_id, published_at DESC)
└── ...
```

## 구현

### News 도메인 모델

위치: `news/domain/model/News.java`

- `searchKeyword` (String) → `sourceId` (Long) 변경
- `validateSearchKeyword` → `validateSourceId` 변경

[예시 코드](./examples/domain-model-example.md)

### NewsEntity

위치: `news/infrastructure/persistence/NewsEntity.java`

- `search_keyword` 컬럼 → `source_id` 컬럼 변경
- `@Table`에 `@Index` 추가: `(purpose, source_id, published_at)`

[예시 코드](./examples/infrastructure-entity-example.md)

### NewsMapper

위치: `news/infrastructure/persistence/mapper/NewsMapper.java`

- `getSearchKeyword()` → `getSourceId()` 변경

### NewsRepository (포트)

위치: `news/domain/repository/NewsRepository.java`

- `findBySearchKeyword(String, int, int)` → `findByPurposeAndSourceId(NewsPurpose, Long, int, int)` 변경

### NewsJpaRepository

위치: `news/infrastructure/persistence/NewsJpaRepository.java`

- `findBySearchKeywordOrderByPublishedAtDesc` → `findByPurposeAndSourceIdOrderByPublishedAtDesc` 변경
- `insertIgnoreDuplicate` native query: `search_keyword` → `source_id` 변경

### NewsRepositoryImpl (어댑터)

위치: `news/infrastructure/persistence/NewsRepositoryImpl.java`

- `findBySearchKeyword` → `findByPurposeAndSourceId` 변경

### DTO 변경

- `NewsSaveRequest`: `searchKeyword` (String) → `sourceId` (Long)
- `NewsDto`: `searchKeyword` (String) → `sourceId` (Long)

### Application 서비스 변경

- `NewsSaveService`: `News.create()` 호출 시 `sourceId` 전달
- `NewsQueryService.getNewsByKeyword()` → `getNewsBySource(NewsPurpose, Long, int, int)` 변경
- `KeywordSearchContext`: `searchKeyword` (String) → `sourceId` (Long)
- `KeywordNewsBatchServiceImpl`: `keyword.getKeyword()` 대신 `keyword.getId()` 전달

[예시 코드](./examples/application-service-example.md)

### NewsController

위치: `news/presentation/NewsController.java`

- `GET /api/news?keyword={text}` → `GET /api/news?keywordId={id}` 변경
- 향후 Stock 추가 시: `GET /api/news?stockId={id}` 추가

[예시 코드](./examples/presentation-example.md)

## 주의사항

- `original_url` 유니크 제약은 유지 (동일 URL 뉴스 중복 방지)
- 기존 데이터 마이그레이션 필요: `search_keyword` 텍스트 → `keyword` 테이블에서 ID 매핑
- Stock 도메인은 이 설계에서 구현하지 않음 (YAGNI), 구조만 준비