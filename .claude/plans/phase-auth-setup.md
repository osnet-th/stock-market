# 사용자 인증 패키지 구조 설계

### 작업 리스트(순서대로 작업 필수)
- [완료] Spring Boot 프로젝트 생성
- [완료] 기본 의존성 추가 (Spring Web, JPA, Security, jwt)
- [완료] 데이터베이스 설정 (PostgreSQL 개발용)
- [완료] profiles 구조로 yml 분리
- [완료] Spring Security 작성(profiles 기반으로 dev일때는 모든 API 허용)
- [ ] 사용자 인증 기능 구현
  - [완료] 사용자 인증 도메인 계층 구현
  - [완료] 사용자 인증 유스케이스(서비스) 구현
  - [완료] Kakao Oauth 인증 구조 설정
  - [완료] Kakao Oauth 인증 흐름 구현
  - [완료] JWT Provider 구현
  - [완료] 사용자 UserRepository 구현
  - [완료] OAuthAccountRepository 구현
  - [ ] Google Oauth 인증 구조 설정




## 1. 개요

기획은 이 파일([user-auth-policy.md](../../policies/user-auth-policy.md)) 에 맞춰서 진행했습니다.
이 기획서로 인해 생성할 설계 파일은 .claude/designs/auth-setup/ 폴더 하위에 생성합니다.
사용자 인증 기능을 DDD 계층형 구조에 맞춰 설계합니다.
- JWT 기반 토큰 인증
- OAuth 2.0 소셜 로그인 (카카오, 구글)
- Entity 연관관계 금지 (ID 기반 참조만 사용)

---

## 2. 패키지 구조

```
src/main/java/com/thlee/stock/market/stockmarket/
└── user/
    ├── domain/
    │   ├── model/
    │   │   ├── User.java
    │   │   ├── UserId.java
    │   │   ├── Email.java
    │   │   ├── AuthProvider.java (enum)
    │   │   └── UserRole.java (enum)
    │   ├── repository/
    │   │   └── UserRepository.java (interface)
    │   ├── service/
    │   │   └── JwtTokenProvider.java (interface, 포트)
    │   └── exception/
    │       ├── UserDomainException.java
    │       ├── EmailRequiredException.java
    │       └── InvalidEmailFormatException.java
    │
    ├── application/
    │   ├── AuthService.java
    │   ├── OAuthLoginService.java
    │   └── dto/
    │       ├── LoginRequest.java
    │       ├── LoginResponse.java
    │       ├── TokenRefreshRequest.java
    │       ├── TokenRefreshResponse.java
    │       └── OAuthLoginRequest.java
    │
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── UserEntity.java (JPA Entity)
    │   │   ├── UserJpaRepository.java (JpaRepository)
    │   │   ├── UserRepositoryImpl.java (어댑터)
    │   │   └── mapper/
    │   │       └── UserMapper.java
    │   │
    │   ├── security/
    │   │   └── jwt/
    │   │       ├── JwtTokenProviderImpl.java
    │   │       └── JwtProperties.java
    │   │
    │   └── oauth/
    │       ├── OAuthClient.java (interface)
    │       ├── dto/
    │       │   ├── OAuthUserInfo.java (interface)
    │       │   ├── KakaoUserInfo.java
    │       │   └── GoogleUserInfo.java
    │       ├── kakao/
    │       │   └── KakaoOAuthClient.java
    │       └── google/
    │           └── GoogleOAuthClient.java
    │
    └── presentation/
        ├── AuthController.java
        └── security/
            └── JwtAuthenticationFilter.java
```

---

## 3. 계층별 책임

### 3.1 Domain 계층

#### model 패키지
- **User**: 도메인 모델, 사용자 핵심 속성 및 행위
- **UserId**: Value Object, 사용자 식별자
- **Email**: Value Object, 이메일 검증 로직 포함
- **AuthProvider**: enum (LOCAL, KAKAO, GOOGLE)
- **UserRole**: enum (USER, ADMIN)

#### repository 패키지
- **UserRepository**: 도메인 레포지토리 인터페이스 (포트)
  - ID로 사용자 조회
  - 이메일로 사용자 조회
  - 사용자 저장

#### service 패키지
- **JwtTokenProvider**: JWT 토큰 생성/검증 인터페이스 (포트)
  - Access Token 생성
  - Refresh Token 생성
  - 토큰 검증
  - 토큰 만료 확인

#### exception 패키지
- **UserDomainException**: 사용자 도메인 예외 최상위 클래스 (추상 클래스)
- **EmailRequiredException**: 이메일 필수 입력 위반 예외
- **InvalidEmailFormatException**: 이메일 형식 위반 예외

**주의사항**:
- domain 계층에는 Spring, JPA 의존성 없음
- 외부 라이브러리(JWT) 의존 로직은 인터페이스로만 정의
- 순수 Java 코드로 비즈니스 규칙 구현
- 도메인 예외는 RuntimeException 계열 사용

---

### 3.2 Application 계층

#### AuthService
- **책임**: 사용자 인증 유스케이스 처리
- **주요 기능**:
  - 로그인 처리
  - 토큰 갱신 처리
  - 로그아웃 처리 (리프레시 토큰 무효화)

#### OAuthLoginService
- **책임**: OAuth 소셜 로그인 유스케이스 처리
- **주요 기능**:
  - 카카오 로그인 처리
  - 구글 로그인 처리

#### dto 패키지
- **용도**: Controller ↔ Application 간 데이터 전달 전용
- Request/Response DTO 정의
- **LoginRequest**: 로그인 요청 DTO
- **LoginResponse**: 로그인 응답 DTO (액세스/리프레시 토큰 포함)
- **TokenRefreshRequest**: 토큰 갱신 요청 DTO
- **TokenRefreshResponse**: 토큰 갱신 응답 DTO
- **OAuthLoginRequest**: OAuth 로그인 요청 DTO

**주의사항**:
- 트랜잭션은 application 계층에서만 관리
- domain 객체를 직접 반환하지 않고 DTO로 변환
- Infrastructure DTO(외부 API 응답 등)와 분리

---

### 3.3 Infrastructure 계층

#### persistence 패키지
- **UserEntity**: JPA Entity, DB 테이블 매핑
  - ID 기반 참조만 사용, 연관관계 금지
  - users 테이블과 매핑
- **UserJpaRepository**: Spring Data JPA 레포지토리
- **UserRepositoryImpl**: domain의 UserRepository 구현체
  - UserMapper를 사용하여 Entity ↔ Domain Model 변환
- **UserMapper**: 변환 전용 클래스
  - Entity → Domain Model 변환
  - Domain Model → Entity 변환

#### security/jwt 패키지
- **JwtTokenProviderImpl**: JwtTokenProvider 구현체
  - JWT 라이브러리 사용 (jjwt)
  - 토큰 생성, 검증, 파싱
- **JwtProperties**: JWT 설정 (시크릿 키, 만료 시간)

#### oauth 패키지
- **OAuthClient**: OAuth 클라이언트 인터페이스
  - Access Token으로 사용자 정보 조회
- **dto 패키지**: 외부 API 응답 모델
  - **OAuthUserInfo**: OAuth 사용자 정보 공통 인터페이스
  - **KakaoUserInfo**: 카카오 OAuth 응답 DTO
  - **GoogleUserInfo**: 구글 OAuth 응답 DTO
- **KakaoOAuthClient**: 카카오 OAuth 클라이언트 구현
- **GoogleOAuthClient**: 구글 OAuth 클라이언트 구현

**주의사항**:
- 외부 의존 라이브러리는 infrastructure에서만 사용
- domain 인터페이스(포트)를 구현하는 어댑터 패턴

---

### 3.4 Presentation 계층

#### AuthController
- **책임**: HTTP 요청 처리, 인증 엔드포인트 제공
- **주요 엔드포인트**:
  - 로컬 로그인
  - 토큰 갱신
  - 로그아웃
  - 카카오 로그인
  - 구글 로그인

#### security 패키지
- **JwtAuthenticationFilter**: Spring Security 필터
  - HTTP 요청에서 JWT 토큰 추출
  - 토큰 검증 후 SecurityContext 설정
  - 웹 요청 처리 계층

**주의사항**:
- Controller는 요청 검증, DTO 변환만 담당
- 비즈니스 로직은 application 서비스에 위임
- 요청 검증 어노테이션 사용
- Filter는 웹 계층 책임으로 presentation에 배치

---

## 4. 주요 설계 원칙

### 4.1 의존성 방향
- Presentation → Application → Domain ← Infrastructure
- domain은 다른 계층에 의존하지 않음
- infrastructure는 domain 인터페이스를 구현

### 4.2 Entity 연관관계 금지
- JPA Entity는 연관관계 사용하지 않음
- 필요 시 ID로 참조하고 별도 조회

### 4.3 DTO/Entity 경계
- Controller ↔ Application: DTO 사용
- Application ↔ Domain: Domain Model 사용
- Infrastructure ↔ Domain: Domain Model 변환

### 4.4 외부 의존성 격리
- JWT, OAuth 같은 외부 의존은 infrastructure에만 위치
- domain에는 인터페이스(포트)만 정의

---

## 5. TDD 구현 순서

### 5.1 Domain 계층 (테스트 → 구현)
1. User 도메인 모델 테스트
2. Email Value Object 테스트
3. UserId Value Object 테스트

### 5.2 Infrastructure 계층 (테스트 → 구현)
1. JwtTokenProviderImpl 테스트 (Mock 없이)
2. UserRepositoryImpl 테스트 (Mock JpaRepository)
3. KakaoOAuthClient 테스트 (Mock RestTemplate)
4. GoogleOAuthClient 테스트 (Mock RestTemplate)

### 5.3 Application 계층 (테스트 → 구현)
1. AuthService 테스트 (Mock Repository, JwtTokenProvider)
2. OAuthLoginService 테스트 (Mock Repository, OAuthClient)

### 5.4 Presentation 계층 (테스트 → 구현)
1. AuthController 테스트 (Mock AuthService, OAuthLoginService)

---

## 6. 보안 고려사항

### 6.1 JWT 관리
- Access Token: 짧은 만료 시간 (15분 권장)
- Refresh Token: 긴 만료 시간 (7일 권장)
- Refresh Token은 DB에 저장하여 무효화 가능하게 구현

### 6.2 비밀번호 암호화
- BCryptPasswordEncoder 사용
- 회원가입/로그인 시 암호화된 비밀번호 비교

### 6.3 OAuth 보안
- State 파라미터로 CSRF 방지
- Authorization Code는 1회만 사용 가능

---

## 7. 참고 문서

- [ARCHITECTURE.md](../../../ARCHITECTURE.md)
- [phase-auth-setup.md](../.claude/plans/phase-auth-setup.md)
- [STOCK-MARKET-PROJECT.md](../../../STOCK-MARKET-PROJECT.md)

---


## 1.2 JWT 인증 구현 (TDD)

### 도메인 서비스 배치 주의
- JWT는 보통 외부 라이브러리/키/시간 소스에 의존하므로 **domain에 직접 구현하지 않는 것을 권장**.
- 권장: domain에는 `JwtTokenProvider` 같은 **인터페이스(포트)**만 두고, 구현체는 `infrastructure/security/jwt` 등에 위치.

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **domain 계층은 Mock 없이 순수 테스트**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**수정일**: 2026-01-26
