# 키워드 뉴스 배치 Region 지정 분석

작성일: 2026-02-07

## 현재 구조 분석

### 문제점

배치 실행 시 `Keyword` 도메인에 `region` 정보가 있지만, 뉴스 검색 API 호출 시 전달되지 않음

### 현재 호출 흐름

```
KeywordNewsBatchScheduler (스케줄러)
  └─> KeywordNewsBatchServiceImpl.executeKeywordNewsBatch()
      ├─> keywordService.getAllActiveKeywords() → List<Keyword> (region 정보 포함)
      └─> newsSearchService.search(keyword.getKeyword()) ← region 정보 누락
          └─> NewsSearchPort.search(keyword, fromDateTime) ← region 정보 없음
```

### 관련 코드 위치

1. **KeywordNewsBatchServiceImpl** (`src/main/java/.../news/application/`)
   - Line 37: `newsSearchService.search(keyword.getKeyword())`
   - region 정보를 전달하지 않음

2. **NewsSearchService** (`src/main/java/.../news/application/`)
   - Line 23: `search(String keyword)` 메서드
   - region 파라미터 없음

3. **NewsSearchPort** (`src/main/java/.../news/domain/service/`)
   - Line 11: `search(String keyword, LocalDateTime fromDateTime)`
   - region 파라미터 없음

4. **Keyword** (`src/main/java/.../news/domain/model/`)
   - region 필드 존재 (`KeywordRegion` enum: DOMESTIC, INTERNATIONAL)

## 변경 방안

### 옵션 1: 메서드 시그니처에 region 추가 (권장)

**장점**
- 명시적이고 타입 안전
- 호출 지점에서 어떤 region으로 검색하는지 명확

**변경 대상**
1. `NewsSearchPort.search()` 시그니처 변경
2. `NewsSearchService.search()` 시그니처 변경
3. `KeywordNewsBatchServiceImpl` 호출부 수정
4. NewsSearchPort 구현체들 수정

**변경 코드**

```java
// NewsSearchPort
List<NewsSearchResult> search(String keyword, KeywordRegion region, LocalDateTime fromDateTime);

// NewsSearchService
public List<NewsResultDto> search(String keyword, KeywordRegion region) {
    // ...
    List<NewsSearchResult> results = port.search(keyword, region, fromDateTime);
}

// KeywordNewsBatchServiceImpl
for (Keyword keyword : activeKeywords) {
    List<NewsResultDto> searchResults = newsSearchService.search(
        keyword.getKeyword(),
        keyword.getRegion()
    );
}
```

### 옵션 2: 검색 요청 객체로 캡슐화

**장점**
- 향후 검색 조건 추가 시 확장 용이
- 파라미터 증가 문제 해결

**단점**
- DTO 클래스 추가 필요
- 간단한 케이스에는 과도할 수 있음

**변경 코드**

```java
// NewsSearchRequest DTO
public class NewsSearchRequest {
    private String keyword;
    private KeywordRegion region;
    private LocalDateTime fromDateTime;
}

// NewsSearchPort
List<NewsSearchResult> search(NewsSearchRequest request);
```

### 옵션 3: Keyword 도메인 객체 직접 전달

**장점**
- 변경 최소화

**단점**
- application → domain 의존성 역전
- NewsSearchPort가 domain 모델에 의존하게 됨 (DDD 포트 패턴 위반)

## 권장 사항

**옵션 1 (메서드 시그니처 변경)** 선택 권장

이유:
- 현재 파라미터가 적어 추가 부담 낮음
- 명시적이고 이해하기 쉬움
- DDD 계층 원칙 유지

## 영향 범위

### 수정 필요 파일

1. `NewsSearchPort.java` - 인터페이스 시그니처
2. `NewsSearchService.java` - 메서드 시그니처 및 호출부
3. `KeywordNewsBatchServiceImpl.java` - 호출부에서 region 전달
4. NewsSearchPort 구현체들 (아직 구현 안 됨)
5. `NewsSearchServiceTest.java` - 테스트 코드 수정

### 테스트 영향

- NewsSearchService 단위 테스트 수정 필요
- KeywordNewsBatchServiceImpl 테스트 수정 필요

## 구현 순서

1. NewsSearchPort 인터페이스에 region 파라미터 추가
2. NewsSearchService에 region 파라미터 추가 및 port 호출부 수정
3. KeywordNewsBatchServiceImpl에서 keyword.getRegion() 전달
4. 단위 테스트 수정
5. NewsSearchPort 구현체 작성 시 region 기반 분기 로직 구현