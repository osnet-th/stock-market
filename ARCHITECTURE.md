# ARCHITECTURE.md

이 파일은 Claude Code (claude.ai/code)가 **주식 & 뉴스 통합 플랫폼(stock-market)** 프로젝트를 작업할 때 패키지 구조, 계층 규칙, 의존성 방향, 수정 범위를 정확히 이해하도록 돕는 **아키텍처 기준 문서**입니다.

---

## 1. 전체 아키텍처 개요

- 본 프로젝트는 **Layered Architecture 기반 + DDD 개념을 적용한 구조**입니다.
- 패키지는 **도메인별로 구성**되며, 각 도메인 내에서 계층별로 패키지가 분리됩니다.
- Hexagonal / Clean Architecture는 사용하지 않습니다.

기본 철학:

- 구조 안정성 최우선
- 기존 패키지 구조 및 의존성 규칙 유지
- 아키텍처 변경은 명시적 요청 없이는 금지

---

## 2. 계층 구조 (DDD 스타일)

```
presentation (API Layer)
    ↓
application (Use Case Layer)
    ↓
domain (Business Logic Layer)
    ↓
infrastructure (Infrastructure Layer)
```

---

## 3. 도메인 모델 식별

### 주요 도메인

1. **User (사용자)**
   - 회원 정보
   - OAuth 인증 정보

2. **Stock (주식)**
   - 주식 코드, 종목명
   - 현재가, 등락률 등 시세 정보

3. **News (뉴스)**
   - 제목, 본문, 출처, 링크
   - 관련 주식 코드

4. **Post (게시글)**
   - 주식 관련 커멘트/게시글
   - 작성자, 내용, 작성일

5. **Favorite (관심/즐겨찾기)**
   - 사용자별 관심 주식
   - 사용자별 즐겨찾기 뉴스

---

## 4. 패키지 구조 (도메인별 분리)

```
src/main/java/com/stockmarket/
├── stock/                      # 주식 도메인
│   ├── presentation/
│   │   ├── StockController.java
│   │   └── dto/
│   │       ├── StockRequest.java
│   │       └── StockResponse.java
│   ├── application/
│   │   ├── StockQueryService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── model/
│   │   │   └── Stock.java
│   │   ├── repository/
│   │   │   └── StockRepository.java (interface)
│   │   └── service/
│   │       └── StockDomainService.java
│   └── infrastructure/
│       ├── entity/
│       │   └── StockEntity.java
│       ├── repository/
│       │   └── StockRepositoryImpl.java
│       └── api/
│           ├── alphavantage/
│           ├── finnhub/
│           ├── fss/
│           └── fallback/
│
├── news/                       # 뉴스 도메인
│   ├── presentation/
│   │   ├── NewsController.java
│   │   └── dto/
│   ├── application/
│   │   ├── NewsQueryService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── model/
│   │   │   └── News.java    # relatedStockCode 필드로 Stock 참조
│   │   ├── repository/
│   │   │   └── NewsRepository.java (interface)
│   │   └── service/
│   │       └── NewsDomainService.java
│   └── infrastructure/
│       ├── entity/
│       │   └── NewsEntity.java  # relatedStockCode 필드 (ID 기반 참조)
│       ├── repository/
│       │   └── NewsRepositoryImpl.java
│       └── api/
│           ├── deepsearch/
│           ├── newsapi/
│           ├── arirang/
│           └── fallback/
│
├── user/                       # 사용자/인증 도메인
│   ├── presentation/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   └── dto/
│   ├── application/
│   │   ├── AuthService.java
│   │   ├── OAuthLoginService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   └── OAuthInfo.java
│   │   ├── repository/
│   │   │   └── UserRepository.java (interface)
│   │   └── service/
│   │       └── JwtTokenService.java
│   └── infrastructure/
│       ├── entity/
│       │   └── UserEntity.java
│       ├── repository/
│       │   └── UserRepositoryImpl.java
│       └── oauth/
│           ├── kakao/
│           │   └── KakaoOAuthClient.java
│           └── google/
│               └── GoogleOAuthClient.java
│
├── post/                       # 게시글 도메인
│   ├── presentation/
│   │   ├── PostController.java
│   │   └── dto/
│   ├── application/
│   │   ├── PostCommandService.java
│   │   ├── PostQueryService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── model/
│   │   │   └── Post.java    # userId, stockCode 필드로 참조
│   │   ├── repository/
│   │   │   └── PostRepository.java (interface)
│   │   └── service/
│   │       └── PostDomainService.java
│   └── infrastructure/
│       ├── entity/
│       │   └── PostEntity.java  # userId, stockCode 필드 (ID 기반 참조)
│       └── repository/
│           └── PostRepositoryImpl.java
│
├── favorite/                   # 관심/즐겨찾기 도메인
│   ├── presentation/
│   │   ├── FavoriteController.java
│   │   └── dto/
│   ├── application/
│   │   ├── FavoriteCommandService.java
│   │   ├── FavoriteQueryService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Favorite.java
│   │   │   └── FavoriteType.java (enum: STOCK, NEWS)
│   │   ├── repository/
│   │   │   └── FavoriteRepository.java (interface)
│   │   └── service/
│   │       └── FavoriteDomainService.java
│   └── infrastructure/
│       ├── entity/
│       │   └── FavoriteEntity.java  # userId, targetType, targetId
│       └── repository/
│           └── FavoriteRepositoryImpl.java
│
└── common/                     # 공통 모듈
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── JpaConfig.java
    │   └── WebConfig.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── BusinessException.java
    │   └── ErrorCode.java
    ├── util/
    │   └── DateTimeUtils.java
    └── dto/
        ├── ApiResponse.java
        └── PageResponse.java
```

---

## 5. 레이어별 책임 정의 (Layer Responsibilities)

### presentation

- 역할: HTTP 요청/응답 처리, Controller
- 허용:
    - Spring MVC 의존
    - DTO 변환
    - 인증/인가 검증
- 금지:
    - 비즈니스 로직 작성 금지
    - domain 모델 직접 노출 금지
    - 트랜잭션 제어 금지

### application

- 역할: 유스케이스 구현, 트랜잭션 경계
- 허용:
    - domain 서비스 호출
    - 여러 domain 조합
    - @Transactional 사용
- 금지:
    - 직접적인 외부 API 호출 금지 (infrastructure 계층 사용)
    - domain 규칙 중복 작성 금지

### domain

- 역할: 핵심 비즈니스 규칙, 순수 도메인 로직
- 허용:
    - Java 표준 라이브러리
    - 도메인 내부 객체 참조
- 금지 (중요):
    - Spring 의존 금지
    - JPA / MyBatis / RestTemplate 의존 금지
    - 외부 시스템 접근 금지

**domain.service 작성 원칙:**
- **비즈니스 규칙/검증/도메인 로직만 담당**
- 여러 도메인 모델에 걸친 복잡한 비즈니스 로직 처리
- 순수 Java로 작성, 인프라 의존성 없음

예시:
```java
public class OrderDomainService {
    public void validateOrder(Order order) {
        // 비즈니스 규칙: 최소 주문 금액
        if (order.getTotalAmount() < 1000) {
            throw new InvalidOrderException("최소 주문 금액 미달");
        }
        // 비즈니스 규칙: 주문 상품 검증
        if (order.getItems().isEmpty()) {
            throw new InvalidOrderException("주문 상품이 없습니다.");
        }
    }
}
```

**domain.repository 인터페이스:**
- domain 계층에는 리포지토리 인터페이스만 정의
- 실제 구현은 infrastructure.repository에 위치
- 인프라에 독립적인 도메인 저장소 추상화

### infrastructure

- 역할: DB, 외부 API, 인프라 연동 구현
- 허용:
    - JPA / MyBatis 구현
    - RestTemplate / WebClient 사용
    - 외부 API 클라이언트 구현
- 규칙:
    - entity는 infrastructure 계층 전용
    - domain.model 직접 노출 금지

**infrastructure.repository 구현:**
- domain.repository 인터페이스를 구현
- JPA/MyBatis를 사용한 실제 저장소 로직
- entity ↔ domain.model 변환 담당

예시:
```java
// domain/repository/UserRepository.java (인터페이스)
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
}

// infrastructure/repository/UserRepositoryImpl.java (구현)
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
}
```

---

## 6. 주요 설계 원칙

### 1. Entity 연관관계 금지 (ID 기반 참조만 사용)
- JPA Entity는 ID 기반 참조만 사용
- 예: Post Entity는 User 객체가 아닌 userId(Long)만 보유
- 예: News Entity는 Stock 객체가 아닌 relatedStockCode(String)만 보유

```java
// ❌ 금지: Entity 연관관계
@Entity
public class PostEntity {
    @ManyToOne
    private UserEntity user;
}

// ✅ 허용: ID 기반 참조
@Entity
public class PostEntity {
    private Long userId;
}
```

### 2. 도메인 간 참조 규칙
- **도메인 모델(domain layer)**: 다른 도메인의 식별자(ID/Code)만 참조 가능
  ```java
  // news/domain/model/News.java
  public class News {
      private String relatedStockCode;  // ✅ Stock 도메인의 식별자만 참조
  }

  // post/domain/model/Post.java
  public class Post {
      private Long userId;          // ✅ User 도메인의 ID만 참조
      private String stockCode;     // ✅ Stock 도메인의 식별자만 참조
  }
  ```

- **Application 계층에서 조합**: 여러 도메인 정보가 필요할 경우 application 계층에서 조합
  ```java
  // news/application/NewsQueryService.java
  public NewsWithStockResponse findNewsWithStock(Long newsId) {
      News news = newsRepository.findById(newsId);
      // 별도로 주식 정보 조회
      Stock stock = stockQueryService.findByCode(news.getRelatedStockCode());
      return NewsWithStockResponse.of(news, stock);
  }
  ```

### 3. N+1 문제 해결 방안
- **배치 조회 패턴 사용**:
  ```java
  // 여러 뉴스와 관련 주식 정보를 효율적으로 조회
  public List<NewsWithStockResponse> findNewsListWithStock(List<Long> newsIds) {
      List<News> newsList = newsRepository.findAllById(newsIds);

      // 주식 코드 추출
      List<String> stockCodes = newsList.stream()
          .map(News::getRelatedStockCode)
          .distinct()
          .collect(Collectors.toList());

      // 배치로 한 번에 조회
      Map<String, Stock> stockMap = stockQueryService.findByCodesAsMap(stockCodes);

      // 조합
      return newsList.stream()
          .map(news -> NewsWithStockResponse.of(news, stockMap.get(news.getRelatedStockCode())))
          .collect(Collectors.toList());
  }
  ```

- **캐싱 활용**: 자주 조회되는 데이터는 Caffeine Cache 활용
- **페이징 처리**: 대량 데이터는 반드시 페이징 적용

### 4. Domain 계층 순수성
- domain 계층은 Spring, JPA 의존성 없음
- 순수 Java로 비즈니스 로직 구현
- 외부 의존성 없이 테스트 가능

### 5. DTO 변환 규칙
- Controller: Request DTO → Application DTO
- Application: Application DTO → Domain Model
- Infrastructure: Domain Model → Entity

### 6. 트랜잭션 경계
- @Transactional은 application 계층에서만 사용

### 7. API 폴백 전략
- 여러 API를 순차적으로 시도
- 무료 사용량 초과 시 다음 API로 자동 전환
- Circuit Breaker 패턴 적용 고려

---

## 7. DTO / Entity / Domain 경계 규칙

- `infrastructure/entity`는 절대 API 응답으로 직접 노출 금지
- `domain/model`은 직접 JSON 직렬화 금지
- API 응답은 반드시 `application/dto` 또는 `presentation` 전용 DTO 사용

### 저장(생성/수정) 시 변환 흐름:

```
Controller Request
   → application.dto
       → domain.model
           → infrastructure.entity (저장)
```

---

## 8. 트랜잭션 경계 규칙

- @Transactional 사용 위치:
    - application 계층에서만 허용

금지:

- controller에 @Transactional 금지
- domain/service에 @Transactional 금지
- infrastructure/repository에서 트랜잭션 제어 금지

---

## 9. 예외 처리 규칙

- domain.exception
    - 비즈니스 규칙 위반 표현
- application
    - domain exception을 유스케이스 실패로 해석
- presentation
    - @ControllerAdvice에서 HTTP Status 매핑

규칙:
- 외부 예외(DB/HTTP)는 application에서 domain exception으로 감싸기 가능
- RuntimeException 무분별한 사용 금지

---

## 10. 수정 시 필수 준수 사항

- 패키지 구조 변경 금지
- 레이어 간 의존성 규칙 위반 금지
- domain 계층 순수성 유지
- API 호환성 유지
- 기존 public 클래스 / 메서드 시그니처 변경 금지 (요청 없을 시)

---

## 11. 최종 가이드

이 구조의 목표:

- 안정성 유지
- 유지보수 용이성 확보
- 도메인별 모듈화된 구조 유지

아키텍처 변경이 필요한 경우:

- 반드시 명시적 요청 또는 설계 검토 후 진행
- 임의 구조 변경 금지

---

**작성일**: 2026-01-19
**작성자**: Claude Code