# Phase 3: 뉴스 조회 (Week 5-6)

## 작업 리스트

### 3.1 뉴스 도메인 모델 (TDD)
1. [ ] News 도메인 모델 테스트 작성
2. [ ] News 도메인 모델 최소 구현
3. [ ] 뉴스 검색 도메인 서비스 테스트 작성
4. [ ] 뉴스 검색 도메인 서비스 최소 구현

### 3.2 뉴스 API 클라이언트 (인프라 계층)
1. [ ] 딥서치 뉴스 API 클라이언트 구현
2. [ ] NewsAPI.org 클라이언트 구현
3. [ ] 국제방송교류재단 API 클라이언트 구현

### 3.3 뉴스 API 폴백 전략 (TDD)
1. [ ] 뉴스 API 폴백 서비스 테스트 작성
2. [ ] 뉴스 API 폴백 서비스 최소 구현

### 3.4 뉴스 조회 및 저장 유스케이스 (TDD)
1. [ ] 키워드로 뉴스 조회 유스케이스 테스트 작성
2. [ ] 키워드로 뉴스 조회 유스케이스 최소 구현
3. [ ] 주식명으로 뉴스 조회 유스케이스 테스트 작성
4. [ ] 주식명으로 뉴스 조회 유스케이스 최소 구현
5. [ ] 뉴스 즐겨찾기 저장 유스케이스 테스트 작성 (MANUAL_STOCK)
6. [ ] 뉴스 즐겨찾기 저장 유스케이스 최소 구현
7. [ ] 저장된 뉴스 조회/삭제 유스케이스 테스트 작성
8. [ ] 저장된 뉴스 조회/삭제 유스케이스 최소 구현

### 3.6 키워드 알림 기능 (TDD)
1. [ ] Keyword 도메인 모델 테스트 작성
2. [ ] Keyword 도메인 모델 최소 구현
3. [ ] 키워드 등록 유스케이스 테스트 작성
4. [ ] 키워드 등록 유스케이스 최소 구현
5. [ ] 키워드 조회/삭제 유스케이스 테스트 작성
6. [ ] 키워드 조회/삭제 유스케이스 최소 구현

### 3.9 뉴스 키워드 스케줄링
1. [ ] 키워드 뉴스 검색 스케줄러 구현
2. [ ] 키워드 매칭 뉴스 자동 저장 구현

---

## 뉴스 조회 및 저장 전략

### 뉴스 조회 방식
- **키워드 기반으로만 뉴스 조회** (일반 뉴스 조회 없음)
  1. 사용자가 등록한 키워드로 뉴스 검색
  2. 사용자가 등록한 주식 이름으로 뉴스 검색

### 뉴스 저장 시점 및 전략

#### 1. 주식 관련 뉴스
- **조회**: 주식명으로 뉴스 API 호출 → 결과만 응답
- **저장**: 사용자가 즐겨찾기 클릭 시에만 DB 저장
- **source**: `MANUAL_STOCK`

#### 2. 키워드 관련 뉴스
- **조회**: 스케줄러가 등록된 키워드로 뉴스 API 호출
- **저장**: 매칭된 뉴스 자동 저장
- **알림**: 저장 후 사용자에게 알림 발송 (별도 트랜잭션)
- **source**: `AUTO_KEYWORD`

### 처리 흐름

```
[주식 뉴스 조회]
1. 사용자 → 주식 상세 페이지 접근
2. 시스템 → 주식명으로 뉴스 API 호출
3. 시스템 → 뉴스 목록 반환 (DB 저장 X)
4. 사용자 → 특정 뉴스 즐겨찾기 클릭
5. 시스템 → 해당 뉴스만 DB 저장 (source: MANUAL_STOCK)

[키워드 뉴스 자동 검색]
1. 스케줄러 → 등록된 키워드로 뉴스 API 호출
2. 시스템 → 매칭된 뉴스 DB 저장 (source: AUTO_KEYWORD)
3. 시스템 → 사용자에게 알림 발송 (별도 트랜잭션)
```

---

## 3.1 뉴스 도메인 모델 (TDD)

### 대상 파일
- `news/domain/model/News.java` (테스트 먼저)
- `news/domain/service/NewsDomainService.java` (테스트 먼저)

---

## 3.2 뉴스 API 클라이언트 (인프라 계층)

### 대상 파일
- `news/infrastructure/api/deepsearch/DeepSearchNewsApiClient.java` (인프라 계층이므로 TDD 적용 X)
- `news/infrastructure/api/newsapi/NewsApiOrgClient.java` (인프라 계층이므로 TDD 적용 X)
- `news/infrastructure/api/arirang/ArirangNewsApiClient.java` (인프라 계층이므로 TDD 적용 X)

---

## 3.3 뉴스 API 폴백 전략 (TDD)

### 대상 파일
- `news/application/NewsApiFallbackService.java` (테스트 먼저)

### 폴백 전략
- NewsApiClient 인터페이스 목록을 주입받아 순차 호출
- 첫 번째 API 실패 시 다음 API로 자동 전환
- 모든 API 실패 시 예외 발생

---

## 3.4 뉴스 조회 및 저장 유스케이스 (TDD)

### 대상 파일
- `news/application/NewsQueryService.java` (테스트 먼저)
- `news/application/NewsSaveService.java` (테스트 먼저)
- `news/presentation/NewsController.java` (테스트 먼저)

---

## 3.5 뉴스 API 추상화 구조

### 설계 원칙
- 모든 뉴스 API는 **공통 인터페이스(`NewsApiClient`)**를 구현
- 폴백 서비스는 인터페이스만 의존하여 구현체 교체 가능
- 각 API별 특성은 구현체에서 처리

### 인터페이스 구조
```java
// news/infrastructure/api/NewsApiClient.java (인터페이스)
public interface NewsApiClient {
    List<News> searchNews(String keyword);
    News findNewsById(String newsId);
}
```

### 구현체
- `DeepSearchNewsApiClient.java` - 딥서치 뉴스 API 구현체
- `NewsApiOrgClient.java` - NewsAPI.org 구현체
- `ArirangNewsApiClient.java` - 국제방송교류재단 API 구현체

### 폴백 서비스
- `news/application/NewsApiFallbackService.java`는 `NewsApiClient` 인터페이스 목록을 주입받아 순차 호출
- 첫 번째 API 실패 시 자동으로 다음 API 호출

---

## 3.6 키워드 알림 기능 (TDD)

### 대상 파일
- `keyword/domain/model/Keyword.java` (테스트 먼저)
- `keyword/application/KeywordService.java` (테스트 먼저)
- `keyword/presentation/KeywordController.java` (테스트 먼저)

### 도메인 모델 설계
```java
// keyword/domain/model/Keyword.java
public class Keyword {
    private Long id;
    private String keyword;
    private Long userId;     // User 도메인의 식별자만 참조
    private boolean active;  // 활성화 여부
    private LocalDateTime createdAt;
}
```

---

## 3.7 저장된 뉴스 조회 기능

### 주요 기능
- 사용자별 저장된 뉴스 목록 조회
- source 타입별 필터링 (MANUAL_STOCK, AUTO_KEYWORD)
- 저장된 뉴스 삭제

### 조회 조건
- userId로 필터링
- source 타입별 조회 (선택적)
- 페이징 처리

---

## 3.8 알림 추상화 구조

### 설계 원칙
- **트랜잭션 분리**: 뉴스 즐겨찾기 등록과 알림 발송은 별도 트랜잭션
- 알림 발송 실패가 비즈니스 로직에 영향을 주지 않도록 설계
- 모든 알림은 **공통 인터페이스(`NotificationService`)**를 구현

### 인터페이스 구조
```java
// notification/domain/NotificationService.java (인터페이스)
public interface NotificationService {
    void sendNotification(NotificationRequest request);
}
```

### 구현체
- `EmailNotificationService.java` - 이메일 알림 발송
- `PushNotificationService.java` - 푸시 알림 발송
- `SmsNotificationService.java` - SMS 알림 발송 (선택)

### 트랜잭션 분리 전략
```java
// keyword/application/KeywordNewsScheduler.java
public void processKeywordNews() {
    // 트랜잭션 1: 키워드 매칭 뉴스 저장
    List<News> savedNews = newsService.saveNews(newsList, "AUTO_KEYWORD");

    // 트랜잭션 2: 알림 발송 (비동기)
    notificationService.sendNotification(savedNews);
}
```

---

## 3.9 뉴스 키워드 스케줄링

### 대상 파일
- `keyword/infrastructure/scheduler/KeywordNewsScheduler.java` (인프라 계층이므로 TDD 적용 X)

### 스케줄링 전략
- **주기**: 매 시간 또는 사용자 지정 주기
- **처리 흐름**:
  1. 활성화된 모든 키워드 조회
  2. 각 키워드로 뉴스 API 검색 (NewsApiClient 인터페이스 사용)
  3. 검색된 뉴스를 DB에 저장 (source: "AUTO_KEYWORD")
  4. 사용자에게 알림 발송 (NotificationService 인터페이스 사용, 별도 트랜잭션)
- **배치 처리**: 대량 키워드 처리 시 배치 단위로 처리
- **중복 방지**: 이미 저장된 뉴스는 재저장하지 않음 (URL 기반 중복 체크)

### 스케줄러 구조
```java
// keyword/infrastructure/scheduler/KeywordNewsScheduler.java
@Component
public class KeywordNewsScheduler {

    @Scheduled(cron = "0 0 * * * *")  // 매 시간
    public void searchKeywordNews() {
        // 1. 활성 키워드 조회
        // 2. 뉴스 검색 (NewsApiClient 인터페이스 활용)
        // 3. 뉴스 자동 저장 (source: AUTO_KEYWORD)
        // 4. 알림 발송 (별도 트랜잭션)
    }
}
```

---

## API 제약 사항

### 무료 API 사용량 제한
- **딥서치 뉴스 API**: 일 30,000건 뉴스 수집
- **NewsAPI.org**: localhost만 사용 가능 (배포 불가), 개발용으로만 활용
- **국제방송교류재단**: 공공데이터포털 API

### 폴백 전략
- 여러 API를 순차적으로 시도
- 무료 사용량 초과 시 다음 API로 자동 전환
- Circuit Breaker 패턴 적용 고려

---

## 도메인 간 참조 규칙

### News 도메인 모델
```java
// news/domain/model/News.java
public class News {
    private Long id;
    private String title;
    private String content;
    private String url;
    private Integer relatedStockId;   // ✅ Stock 도메인의 식별자만 참조 (int)
    private Long userId;              // ✅ User 도메인의 식별자만 참조
    private String source;            // "MANUAL_STOCK" | "AUTO_KEYWORD"
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
```

### Application 계층에서 조합
```java
// news/application/NewsQueryService.java
public NewsWithStockResponse findNewsWithStock(Long newsId) {
    News news = newsRepository.findById(newsId);
    // 별도로 주식 정보 조회
    Stock stock = stockQueryService.findById(news.getRelatedStockId());
    return NewsWithStockResponse.of(news, stock);
}
```

### 뉴스 저장 시 source 구분
```java
// 주식 상세 페이지에서 즐겨찾기 클릭
News manualNews = News.builder()
    .source("MANUAL_STOCK")
    .userId(userId)
    .build();

// 스케줄러가 키워드 매칭 뉴스 발견
News autoNews = News.builder()
    .source("AUTO_KEYWORD")
    .userId(userId)
    .build();
```

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **API 키 관리 보안 (환경 변수 사용)**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**수정일**: 2026-01-31