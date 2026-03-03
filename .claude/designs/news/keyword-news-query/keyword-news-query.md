# 키워드 기반 뉴스 조회 설계

## 작업 리스트

- [x] PageResult<T> 공통 페이징 래퍼 생성 (`common` 패키지)
- [x] NewsRepository에 `findBySearchKeyword(keyword, page, size)` 메서드 추가
- [x] NewsJpaRepository에 JPA 쿼리 메서드 추가
- [x] NewsRepositoryImpl에 어댑터 구현
- [x] NewsQueryService에 키워드 조회 메서드 추가
- [x] NewsQueryResponse (페이징 응답 DTO) 생성
- [x] NewsController 생성 (키워드 뉴스 조회 API)

## 배경

키워드 기반으로 뉴스를 수집/저장하고 있지만, 저장된 뉴스를 키워드로 조회하는 API가 없음.

## 핵심 결정

- **페이징**: Spring Data `Pageable` + `Page<T>` 활용 (offset 기반)
- **정렬**: `publishedAt` 최신순 기본 정렬
- **Controller 분리**: 기존 `KeywordController`는 키워드 관리 전용이므로, 뉴스 조회는 `NewsController`로 분리
- **API 경로**: `GET /api/news?keyword={keyword}&page=0&size=20`

## 구현

### PageResult (공통)
위치: `common/response/PageResult.java`
- 순수 Java 페이징 래퍼 (Spring 의존성 없음)
- `content`, `page`, `size`, `totalElements`, `totalPages` 포함
- 모든 도메인에서 재사용 가능

### NewsRepository (domain 포트)
위치: `news/domain/repository/NewsRepository.java`
- `PageResult<News> findBySearchKeyword(String keyword, int page, int size)` 추가

### NewsJpaRepository (infrastructure)
위치: `news/infrastructure/persistence/NewsJpaRepository.java`
- `Page<NewsEntity> findBySearchKeywordOrderByPublishedAtDesc(String searchKeyword, Pageable pageable)` 추가

### NewsRepositoryImpl (infrastructure 어댑터)
위치: `news/infrastructure/persistence/NewsRepositoryImpl.java`
- `findBySearchKeyword` 구현: Entity → Domain 변환 + Page 매핑

### NewsQueryService (application)
위치: `news/application/NewsQueryService.java`
- `PageResult<NewsDto> getNewsByKeyword(String keyword, int page, int size)` 추가

### NewsQueryResponse (presentation DTO)
위치: `news/presentation/dto/NewsQueryResponse.java`
- 페이징 메타데이터 포함 응답 DTO

[예시 코드](./examples/domain-infrastructure-example.md)
[예시 코드](./examples/application-presentation-example.md)

### NewsController (presentation)
위치: `news/presentation/NewsController.java`
- `GET /api/news?keyword={keyword}&page=0&size=20`

## 주의사항

- `searchKeyword`는 정확히 일치하는 키워드로 검색 (LIKE 아님) — 배치 저장 시 키워드 단위로 저장되므로
- domain 레이어는 순수 Java만 허용 → `PageResult<T>` 커스텀 래퍼를 domain에 생성하여 `Page` 대체
- `Pageable`은 application/infrastructure 레이어에서만 사용, domain 포트에는 `int page, int size`로 전달