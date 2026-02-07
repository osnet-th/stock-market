# 키워드 국내/해외 분류 추가 수정 범위 분석

작성일: 2026-02-07

## 현재 구조 분석

### Keyword 도메인 모델
위치: `src/main/java/com/thlee/stock/market/stockmarket/news/domain/model/Keyword.java:8-88`

**현재 필드:**
- `Long id`
- `String keyword`
- `Long userId`
- `boolean active`
- `LocalDateTime createdAt`

**주요 메서드:**
- `create(String keyword, Long userId)`: 키워드 생성
- `activate()`: 키워드 활성화
- `deactivate()`: 키워드 비활성화

### KeywordEntity (JPA Entity)
위치: `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/persistence/KeywordEntity.java:11-87`

**현재 필드:**
- `Long id`
- `String keyword`
- `Long userId`
- `boolean active`
- `LocalDateTime createdAt`

### 뉴스 검색 API 현황

**국내 뉴스:**
- Naver API 사용

**해외 뉴스:**
- GNews API
- NewsAPI
- The Guardian API

### NewsSearchService 동작 방식
위치: `src/main/java/com/thlee/stock/market/stockmarket/news/application/NewsSearchService.java:14-41`

- 여러 `NewsSearchPort` 구현체를 순회하며 검색 수행
- 키워드만 전달, 현재 국내/해외 구분 없음
- 첫 번째 성공한 포트의 결과 반환

## 수정 범위 분석

### 1. Domain Layer 수정

#### 1.1 KeywordRegion Enum 생성 (신규)
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/domain/model/KeywordRegion.java`
- 내용:
  ```
  DOMESTIC("국내")
  INTERNATIONAL("해외")
  ```

#### 1.2 Keyword 도메인 모델 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/domain/model/Keyword.java:8-88`
- 추가 필드: `KeywordRegion region`
- 수정 생성자:
  - `Keyword(String keyword, Long userId, boolean active, LocalDateTime createdAt)` → region 파라미터 추가
  - `Keyword(Long id, String keyword, Long userId, boolean active, LocalDateTime createdAt)` → region 파라미터 추가
- 수정 메서드:
  - `create(String keyword, Long userId)` → region 파라미터 추가
  - `validateRegion(KeywordRegion region)` 추가 (검증 메서드)
- 추가 메서드:
  - `getRegion()` getter


### 2. Infrastructure Layer 수정

#### 2.1 KeywordEntity 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/persistence/KeywordEntity.java:11-87`
- 추가 필드:
  ```
  @Enumerated(EnumType.STRING)
  @Column(name = "region", nullable = false, length = 20)
  private KeywordRegion region
  ```
- 수정 생성자: region 파라미터 추가
- 추가 getter/setter:
  - `getRegion()`
  - `setRegion(KeywordRegion region)`

#### 2.2 KeywordMapper 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/persistence/mapper/KeywordMapper.java:9-35`
- `toEntity()`: region 필드 매핑 추가
- `toDomain()`: region 필드 매핑 추가


### 3. Application Layer 수정

#### 3.1 RegisterKeywordRequest DTO 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/application/dto/RegisterKeywordRequest.java`
- 추가 필드: `KeywordRegion region`
- 추가 getter: `getRegion()`

#### 3.2 KeywordService 인터페이스 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/application/KeywordService.java`
- 수정 메서드 시그니처:
  - `registerKeyword(String keyword, Long userId)` → `registerKeyword(RegisterKeywordRequest request)`로 변경
  - DTO를 통해 keyword, userId, region을 한 번에 전달

#### 3.3 KeywordServiceImpl 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/application/KeywordServiceImpl.java:17-85`
- `registerKeyword()` 메서드: RegisterKeywordRequest에서 keyword, userId, region 추출하여 전달

### 4. Presentation Layer 수정

#### 4.1 KeywordController 수정
- 경로: `src/main/java/com/thlee/stock/market/stockmarket/news/presentation/KeywordController.java:18-93`
- `registerKeyword()` 메서드: request에서 region 추출 및 전달
- `getKeywords()` 메서드: region 필터링 옵션 추가 (선택적)

### 6. Test Layer 수정

#### 6.1 KeywordTest 수정
- 경로: `src/test/java/com/thlee/stock/market/stockmarket/news/domain/model/KeywordTest.java:8-80`
- 모든 테스트 케이스에 region 파라미터 추가
- region 검증 테스트 추가:
  - `region이_null이면_예외_발생()`

#### 6.2 KeywordServiceTest 수정
- 경로: `src/test/java/com/thlee/stock/market/stockmarket/news/application/KeywordServiceTest.java:21-125`
- 모든 테스트 케이스에 region 파라미터 추가

#### 6.3 KeywordNewsBatchServiceImplTest 수정
- 경로: `src/test/java/com/thlee/stock/market/stockmarket/news/application/KeywordNewsBatchServiceImplTest.java:21-118`
- Keyword 생성 시 region 파라미터 추가

## 수정 순서 (TDD 기준)

### Phase 1: Domain Layer (TDD)
1. KeywordRegion Enum 생성
2. KeywordTest 수정 (region 관련 테스트 추가)
3. Keyword 도메인 모델 수정 (테스트 통과시키기)

### Phase 2: Infrastructure Layer
1. Database Schema 수정
2. KeywordEntity 수정
3. KeywordMapper 수정

### Phase 3: Application Layer (TDD)
1. RegisterKeywordRequest DTO 수정
2. KeywordServiceTest 수정 (region 관련 테스트 추가)
3. KeywordService 인터페이스 수정
4. KeywordServiceImpl 수정 (테스트 통과시키기)
5. KeywordNewsBatchServiceImplTest 수정

### Phase 4: Presentation Layer
1. KeywordController 수정

### Phase 5: NewsSearchService 개선 (선택적)
1. region에 따른 NewsSearchPort 선택 로직 추가

## 영향도 분석

### 높은 영향도 (필수 수정)
- Keyword 도메인 모델
- KeywordEntity
- KeywordMapper
- KeywordService
- KeywordController
- 모든 테스트 코드

### 중간 영향도 (선택적 수정)
- NewsSearchService (region별 API 선택 로직)
- KeywordRepository (region별 조회 메서드)

### 낮은 영향도
- KeywordNewsBatchServiceImpl (기존 로직 유지 가능)

## 주의 사항

1. **TDD 원칙 준수**
   - Domain Layer는 반드시 테스트 우선 작성
   - Infrastructure Layer는 TDD 적용하지 않음

2. **변경 범위 제한**
   - 요청받은 기능(국내/해외 분류)만 추가
   - 불필요한 리팩토링 금지
   - 기존 동작 변경 금지

3. **데이터베이스 마이그레이션**
   - 기존 데이터에 대한 DEFAULT 값 설정 필요
   - 운영 환경 적용 시 데이터 무결성 검증 필요

4. **하위 호환성**
   - 기존 키워드는 DOMESTIC으로 기본 설정
   - API 변경 시 클라이언트 영향도 고려
