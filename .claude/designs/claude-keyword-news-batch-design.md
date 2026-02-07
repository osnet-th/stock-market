# 키워드 뉴스 배치 조회 설계

작성일: 2026-02-07
수정일: 2026-02-07

## 작업 리스트

### Application Layer
- [완료] `NewsQueryService`에 `findExistingUrls` 메서드 추가
- [완료] `KeywordNewsBatchService` 인터페이스 생성
- [완료] `KeywordNewsBatchServiceImpl` 구현
- [완료] `KeywordNewsBatchServiceImpl` 테스트 작성

### Infrastructure Layer
- [완료] `KeywordNewsBatchScheduler` 구현

### 설정
- [완료] 스케줄링 활성화 설정 확인 (`@EnableScheduling`)

## 개요

키워드 기반으로 뉴스를 주기적으로 조회하고 저장하는 배치 기능 설계

## 패키지 구조

```
news/
├── application/
│   ├── KeywordNewsBatchService.java (interface)
│   └── KeywordNewsBatchServiceImpl.java
└── infrastructure/
    └── scheduler/
        └── KeywordNewsBatchScheduler.java
```

## 클래스 설계

### 1. Application Layer

#### KeywordNewsBatchService (Interface)

```java
package com.thlee.stock.market.stockmarket.news.application;

public interface KeywordNewsBatchService {
    /**
     * 활성화된 키워드 기반으로 뉴스를 조회하고 저장
     * @return 저장된 신규 뉴스 건수
     */
    int executeKeywordNewsBatch();
}
```

#### KeywordNewsBatchServiceImpl

**책임**
- 활성 키워드 조회
- 키워드별 뉴스 검색
- 중복 필터링 (신규 뉴스만 추출)
- 배치 저장
- 알림 발송

**의존성**
- `KeywordService`: 활성 키워드 조회
- `NewsSearchService`: 키워드 기반 뉴스 검색
- `NewsSaveService`: 뉴스 배치 저장
- `NewsQueryService`: 중복 확인용 (URL 기반 기존 뉴스 존재 여부)
- `Notification`: 알림 발송 (현재 미구현, 인터페이스만 호출)

**주요 메서드**

```java
package com.thlee.stock.market.stockmarket.news.application;

@Service
@RequiredArgsConstructor
public class KeywordNewsBatchServiceImpl implements KeywordNewsBatchService {

    private final KeywordService keywordService;
    private final NewsSearchService newsSearchService;
    private final NewsSaveService newsSaveService;
    private final NewsQueryService newsQueryService;
    private final Notification notification;

    @Override
    public int executeKeywordNewsBatch() {
        // 1. 활성 키워드 조회
        // 2. 키워드별 뉴스 검색
        // 3. 중복 필터링
        // 4. 배치 저장
        // 5. 알림 발송
        // return 저장된 건수
    }

    /**
     * 신규 뉴스만 필터링 (중복 제거)
     * @param searchedNews 검색된 뉴스 목록
     * @return 신규 뉴스 목록
     */
    private List<News> filterNewNews(List<News> searchedNews) {
        // 기존 저장된 뉴스와 비교하여 신규만 반환
    }
}
```

### 2. Infrastructure Layer

#### KeywordNewsBatchScheduler

**책임**
- 스케줄링만 담당 (1시간 주기)
- KeywordNewsBatchService 인터페이스 호출

**구현**

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.scheduler;

@Component
@RequiredArgsConstructor
public class KeywordNewsBatchScheduler {

    private final KeywordNewsBatchService keywordNewsBatchService;

    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각
    public void scheduleKeywordNewsBatch() {
        keywordNewsBatchService.executeKeywordNewsBatch();
    }
}
```

## 처리 흐름

```
KeywordNewsBatchScheduler (매 시간 실행)
    ↓
KeywordNewsBatchService.executeKeywordNewsBatch()
    ↓
1. KeywordService.getActiveKeywords() → List<Keyword>
    ↓
2. for each keyword:
   NewsSearchService.searchByKeyword(keyword) → List<News>
    ↓
3. filterNewNews(allSearchedNews)
   ├─ NewsQueryService.findExistingUrls() → 기존 URL 목록
   └─ Stream filter → List<News> (신규 뉴스만)
    ↓
4. NewsSaveService.saveBatch(newNewsList)
    ↓
5. Notification.send(저장 건수 정보)
```

## 중복 필터링 전략

### 채택 방안: NewsQueryService 활용 (CQRS)

**설계**
- `NewsQueryService`에 URL 기반 중복 확인 메서드 추가
- Application Service는 Query/Command 서비스를 조율

**장점**
- 책임 분리 명확 (Query: 조회, Command: 저장)
- CQRS 패턴 적용
- 재사용 가능 (다른 배치에서도 활용)

**NewsQueryService 추가 메서드**

```java
package com.thlee.stock.market.stockmarket.news.application;

public interface NewsQueryService {
    // 기존 메서드들...

    /**
     * 여러 URL 중 이미 존재하는 것 필터링
     * @param originalLinks 확인할 URL 목록
     * @return 이미 존재하는 URL 목록
     */
    List<String> findExistingUrls(List<String> originalLinks);
}
```

**KeywordNewsBatchServiceImpl 필터링 로직**

```java
private List<News> filterNewNews(List<News> searchedNews) {
    List<String> urls = searchedNews.stream()
        .map(News::getOriginalLink)
        .toList();

    List<String> existingUrls = newsQueryService.findExistingUrls(urls);

    return searchedNews.stream()
        .filter(news -> !existingUrls.contains(news.getOriginalLink()))
        .toList();
}
```

## 알림 처리

현재 `Notification` 인터페이스만 정의되어 있고 구현체는 미존재

```java
// 예상 인터페이스
public interface Notification {
    void send(String message);
}
```

실제 구현은 추후 Notification 패키지에서 진행

## 예외 처리

- 키워드 조회 실패: 로그 기록 후 종료
- 뉴스 검색 실패: 해당 키워드 스킵하고 다음 키워드 진행
- 저장 실패: 로그 기록 및 알림 (운영자에게 통지)
- 알림 발송 실패: 로그 기록만 (배치 작업은 성공으로 간주)

## 트랜잭션 경계

`@Transactional`은 `executeKeywordNewsBatch()` 메서드에 적용하지 않음
- 이유: 배치 작업이 길어질 수 있고, 외부 API 호출 포함
- 저장은 `NewsSaveService.saveBatch()`에서 트랜잭션 처리

## 성능 고려사항

- 키워드가 많을 경우 순차 처리 시 시간 소요
- 추후 필요 시 병렬 처리(CompletableFuture) 고려
- 페이징 처리가 필요한 경우 NewsSearchService에서 처리