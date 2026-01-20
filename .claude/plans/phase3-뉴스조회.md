# Phase 3: 뉴스 조회 (Week 5-6)

## 3.1 뉴스 도메인 모델 (TDD)

### 테스트 → 구현 순서

1. [ ] News 도메인 모델 테스트 작성
2. [ ] News 도메인 모델 최소 구현
3. [ ] 뉴스 검색 도메인 서비스 테스트 작성
4. [ ] 뉴스 검색 도메인 서비스 최소 구현

### 대상 파일
- `news/domain/model/News.java` (테스트 먼저)
- `news/domain/service/NewsDomainService.java` (테스트 먼저)

---

## 3.2 뉴스 API 클라이언트 (TDD)

### 테스트 → 구현 순서

1. [ ] 딥서치 뉴스 API 클라이언트 테스트 작성
2. [ ] 딥서치 뉴스 API 클라이언트 최소 구현
3. [ ] NewsAPI.org 클라이언트 테스트 작성
4. [ ] NewsAPI.org 클라이언트 최소 구현
5. [ ] 국제방송교류재단 API 클라이언트 테스트 작성
6. [ ] 국제방송교류재단 API 클라이언트 최소 구현

### 대상 파일
- `news/infrastructure/api/deepsearch/DeepSearchNewsClient.java` (테스트 먼저)
- `news/infrastructure/api/newsapi/NewsApiClient.java` (테스트 먼저)
- `news/infrastructure/api/arirang/ArirangNewsClient.java` (테스트 먼저)

---

## 3.3 뉴스 API 폴백 전략 (TDD)

### 테스트 → 구현 순서

1. [ ] 뉴스 API 폴백 서비스 테스트 작성
2. [ ] 뉴스 API 폴백 서비스 최소 구현

### 대상 파일
- `news/infrastructure/api/fallback/NewsApiFallbackService.java` (테스트 먼저)

---

## 3.4 뉴스 조회 유스케이스 (TDD)

### 테스트 → 구현 순서

1. [ ] 주식별 뉴스 조회 유스케이스 테스트 작성
2. [ ] 주식별 뉴스 조회 유스케이스 최소 구현
3. [ ] 뉴스 검색 유스케이스 테스트 작성
4. [ ] 뉴스 검색 유스케이스 최소 구현

### 대상 파일
- `news/application/NewsQueryService.java` (테스트 먼저)
- `news/presentation/NewsController.java` (테스트 먼저)

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
    private String relatedStockCode;  // ✅ Stock 도메인의 식별자만 참조
}
```

### Application 계층에서 조합
```java
// news/application/NewsQueryService.java
public NewsWithStockResponse findNewsWithStock(Long newsId) {
    News news = newsRepository.findById(newsId);
    // 별도로 주식 정보 조회
    Stock stock = stockQueryService.findByCode(news.getRelatedStockCode());
    return NewsWithStockResponse.of(news, stock);
}
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
**작성자**: Claude Code