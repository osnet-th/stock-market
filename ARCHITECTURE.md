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

### 기본 패키지 구조 패턴

각 도메인은 다음과 같은 구조를 따릅니다:

```
src/main/java/com/thlee/stock/market/stockmarket/
└── {domain}/
    ├── domain/
    │   ├── model/           # 도메인 모델, Value Object
    │   ├── repository/      # 도메인 레포지토리 인터페이스 (포트)
    │   ├── service/         # 도메인 서비스 (순수 비즈니스 로직)
    │   └── exception/       # 도메인 예외
    │
    ├── application/
    │   ├── {UseCase}Service.java  # 유스케이스 구현
    │   └── dto/            # Application DTO
    │
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── {Domain}Entity.java      # JPA Entity
    │   │   ├── {Domain}JpaRepository.java
    │   │   └── {Domain}RepositoryImpl.java  # 어댑터
    │   │
    │   ├── client/         # 외부 API 클라이언트 (필요시)
    │   └── [기타 인프라 구현]
    │
    └── presentation/
        └── {Domain}Controller.java
```

### 도메인별 패키지 예시

#### User 도메인
```
user/
├── domain/
│   ├── model/
│   │   ├── User.java
│   │   ├── UserId.java
│   │   ├── Email.java
│   │   ├── AuthProvider.java
│   │   └── UserRole.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       └── JwtTokenProvider.java (interface)
│
├── application/
│   ├── AuthService.java
│   ├── OAuthLoginService.java
│   └── dto/
│
├── infrastructure/
│   ├── persistence/
│   │   ├── UserEntity.java
│   │   ├── UserJpaRepository.java
│   │   └── UserRepositoryImpl.java
│   ├── security/
│   │   └── jwt/
│   │       ├── JwtTokenProviderImpl.java
│   │       └── JwtAuthenticationFilter.java
│   └── oauth/
│       ├── kakao/
│       └── google/
│
└── presentation/
    └── AuthController.java
```

#### Stock 도메인
```
stock/
├── domain/
│   ├── model/
│   │   ├── Stock.java
│   │   ├── StockCode.java
│   │   └── StockPrice.java
│   ├── repository/
│   │   └── StockRepository.java
│   └── service/
│       └── StockPriceCalculator.java
│
├── application/
│   ├── StockQueryService.java
│   └── dto/
│
├── infrastructure/
│   ├── persistence/
│   ├── client/
│   │   ├── StockApiClient.java (interface)
│   │   ├── polygon/
│   │   ├── alphavantage/
│   │   └── finnhub/
│   └── cache/
│
└── presentation/
    └── StockController.java
```

#### News 도메인
```
news/
├── domain/
│   ├── model/
│   │   ├── News.java
│   │   ├── NewsId.java
│   │   └── NewsSource.java
│   ├── repository/
│   │   └── NewsRepository.java
│   └── service/
│
├── application/
│   ├── NewsQueryService.java
│   ├── NewsSearchService.java
│   └── dto/
│
├── infrastructure/
│   ├── persistence/
│   └── client/
│       ├── NewsApiClient.java (interface)
│       ├── newsapi/
│       └── finnhub/
│
└── presentation/
    └── NewsController.java
```

#### Post 도메인 (커뮤니티)
```
post/
├── domain/
│   ├── model/
│   │   ├── Post.java
│   │   ├── PostId.java
│   │   └── Comment.java
│   ├── repository/
│   │   └── PostRepository.java
│   └── service/
│
├── application/
│   ├── PostService.java
│   ├── CommentService.java
│   └── dto/
│
├── infrastructure/
│   └── persistence/
│
└── presentation/
    └── PostController.java
```

#### Favorite 도메인
```
favorite/
├── domain/
│   ├── model/
│   │   ├── Favorite.java
│   │   └── FavoriteType.java
│   ├── repository/
│   │   └── FavoriteRepository.java
│   └── service/
│
├── application/
│   ├── FavoriteService.java
│   └── dto/
│
├── infrastructure/
│   └── persistence/
│
└── presentation/
    └── FavoriteController.java
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

---

## 6. 주요 설계 원칙

### 1. Entity 연관관계 금지 (ID 기반 참조만 사용)
- JPA Entity는 ID 기반 참조만 사용
- 예: Post Entity는 User 객체가 아닌 userId(Long)만 보유
- 예: News Entity는 Stock 객체가 아닌 relatedStockCode(String)만 보유

### 2. 도메인 간 참조 규칙

#### 도메인 모델(domain layer)
- 다른 도메인의 식별자(ID/Code)만 참조 가능
- 예시:
  - News 도메인 모델은 Stock 도메인의 식별자(relatedStockCode)만 보유
  - Post 도메인 모델은 User 도메인의 ID(userId)와 Stock 도메인의 식별자(stockCode)만 보유

#### Application 계층에서 조합
- 여러 도메인 정보가 필요할 경우 application 계층에서 조합
- 예시:
  - NewsQueryService에서 뉴스 조회 시
  - 먼저 News를 조회하고
  - News의 relatedStockCode로 Stock 정보를 별도 조회하여
  - 두 정보를 조합한 Response DTO 반환

### 3. N+1 문제 해결 방안
- **배치 조회 패턴 사용**:
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

### 8. 외부 의존성 격리 (포트-어댑터 패턴)

외부 라이브러리(JWT, OAuth, 외부 API 등)에 대한 의존성은 domain 계층에서 격리합니다.

#### 기본 원칙
- **domain 계층**: 인터페이스(포트)만 정의
- **infrastructure 계층**: 실제 구현체(어댑터) 배치

#### 적용 대상
- JWT 토큰 처리: domain에 JwtTokenProvider 인터페이스, infrastructure에 구현체
- OAuth 클라이언트: domain/infrastructure에 OAuthClient 인터페이스, infrastructure 하위에 구현체
- 외부 API 클라이언트: infrastructure에 인터페이스와 구현체 모두 배치

#### Infrastructure 하위 구조

```
infrastructure/
├── persistence/          # 데이터베이스 연동
│   ├── {Domain}Entity.java
│   ├── {Domain}JpaRepository.java
│   └── {Domain}RepositoryImpl.java
│
├── security/            # 보안 관련 구현
│   ├── jwt/
│   │   ├── JwtTokenProviderImpl.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtProperties.java
│   └── oauth/
│       ├── OAuthClient.java (interface)
│       ├── kakao/
│       └── google/
│
├── client/              # 외부 API 클라이언트
│   ├── {Service}ApiClient.java (interface)
│   ├── {Provider1}/
│   ├── {Provider2}/
│   └── fallback/
│
└── cache/               # 캐싱 구현
    └── CacheConfig.java
```

#### 장점
1. **테스트 용이성**: Mock 객체로 쉽게 대체 가능
2. **변경 유연성**: 외부 라이브러리 변경 시 어댑터만 수정
3. **도메인 순수성 유지**: 비즈니스 로직이 인프라에 의존하지 않음
4. **멀티 구현 지원**: 같은 포트에 여러 어댑터 구현 가능 (폴백 전략)

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