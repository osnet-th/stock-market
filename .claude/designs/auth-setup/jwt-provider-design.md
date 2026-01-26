# JWT Provider 설계

## 1. 설계 개요

JWT 토큰 생성, 검증, 파싱 기능을 제공하는 컴포넌트 설계

**핵심 목표:**
- Access Token / Refresh Token 생성 및 검증
- 토큰 기반 사용자 인증 지원
- 토큰 만료 처리
- DDD 아키텍처 준수 (domain 인터페이스 + infrastructure 구현체)

---

## 2. 아키텍처 배치

### 2.1 계층별 역할

**domain 계층** (`user/domain/service`)
- `JwtTokenProvider` 인터페이스 (포트) - **이미 구현됨**
- 외부 의존성 없는 순수 인터페이스

**infrastructure 계층** (`infrastructure/security/jwt`)
- `JwtTokenProviderImpl` 구현체
- `JwtProperties` 설정 클래스
- JWT 라이브러리 의존 (jjwt)

**application 계층**
- AuthService에서 JwtTokenProvider 사용
- 로그인/회원가입 시 토큰 발급
- 토큰 갱신 처리

---

## 3. 패키지 구조

```
infrastructure/
└── security/
    └── jwt/
        ├── JwtTokenProviderImpl.java    # JWT 토큰 제공자 구현체
        ├── JwtProperties.java           # JWT 설정 프로퍼티
        └── exception/
            ├── JwtException.java        # JWT 기본 예외
            ├── InvalidTokenException.java      # 유효하지 않은 토큰 예외
            └── ExpiredTokenException.java      # 만료된 토큰 예외
```

---

## 4. 구현 클래스 설계

### 4.1 JwtTokenProviderImpl

**책임:**
- JWT 토큰 생성 (Access Token, Refresh Token)
- JWT 토큰 검증
- JWT 토큰 파싱 (Claims 추출)
- 토큰 만료 여부 확인

**의존성:**
- `JwtProperties` (설정)
- `io.jsonwebtoken` 라이브러리

**주요 메서드:**
- `generateAccessToken(Long userId, UserRole role)` - Access Token 생성
- `generateRefreshToken(Long userId)` - Refresh Token 생성
- `validateToken(String token)` - 토큰 유효성 검증
- `getUserIdFromToken(String token)` - 토큰에서 사용자 ID 추출
- `isTokenExpired(String token)` - 토큰 만료 여부 확인

**내부 헬퍼 메서드:**
- `generateToken(Map<String, Object> claims, Long userId, long expirationMillis)` - 토큰 생성 공통 로직
- `extractAllClaims(String token)` - 토큰에서 모든 Claims 추출
- `extractClaim(String token, Function<Claims, T> claimsResolver)` - 특정 Claim 추출

### 4.2 JwtProperties

**책임:**
- JWT 설정 값 관리
- application.yml의 jwt 설정 바인딩

**설정 항목:**
- `secret` - JWT 서명 키 (환경변수에서 주입)
- `accessTokenExpiration` - Access Token 만료 시간 (밀리초)
- `refreshTokenExpiration` - Refresh Token 만료 시간 (밀리초)

**바인딩:**
- `@ConfigurationProperties(prefix = "jwt")`
- `application.yml`에서 설정 로드

---

## 5. 토큰 구조

### 5.1 Access Token Claims

**표준 Claims:**
- `sub` (subject) - 사용자 ID
- `iat` (issued at) - 발급 시간
- `exp` (expiration) - 만료 시간

**커스텀 Claims:**
- `role` - 사용자 권한 (USER, ADMIN)
- `type` - 토큰 타입 ("ACCESS")

### 5.2 Refresh Token Claims

**표준 Claims:**
- `sub` (subject) - 사용자 ID
- `iat` (issued at) - 발급 시간
- `exp` (expiration) - 만료 시간

**커스텀 Claims:**
- `type` - 토큰 타입 ("REFRESH")

---

## 6. 예외 처리

### 6.1 예외 계층 구조

```
JwtException (추상 클래스)
├── InvalidTokenException      # 서명 오류, 형식 오류 등
└── ExpiredTokenException      # 토큰 만료
```

### 6.2 예외 발생 시나리오

- **InvalidTokenException**
  - 토큰 서명이 유효하지 않음
  - 토큰 형식이 잘못됨
  - 토큰이 null이거나 빈 문자열
  - Claims 파싱 실패

- **ExpiredTokenException**
  - 토큰 만료 시간이 지남

---

## 7. 설정 파일

### 7.1 application.yml

```yaml
jwt:
  secret: ${JWT_SECRET:default-secret-key-for-development-only-change-in-production}
  access-token-expiration: 1800000    # 30분 (밀리초)
  refresh-token-expiration: 604800000  # 7일 (밀리초)
```

### 7.2 환경 변수

**개발 환경:**
- `JWT_SECRET` - 개발용 시크릿 키

**운영 환경:**
- `JWT_SECRET` - 운영용 시크릿 키 (반드시 변경 필요)

---

## 8. TDD 구현 순서

### 8.1 도메인 계층 (이미 완료)
- ✅ JwtTokenProvider 인터페이스

### 8.2 인프라 계층 (TDD 적용)

**Phase 1: JwtProperties 테스트 및 구현**
1. JwtProperties 설정 바인딩 테스트
2. 최소 구현
3. 테스트 통과 확인

**Phase 2: JwtTokenProviderImpl - 토큰 생성 테스트**
1. Access Token 생성 테스트 작성
   - userId, role을 받아 토큰 생성
   - 토큰이 null이 아님
   - 토큰 형식 확인 (JWT 형식)
2. Refresh Token 생성 테스트 작성
   - userId를 받아 토큰 생성
   - 토큰이 null이 아님
3. 최소 구현
4. 테스트 통과 확인

**Phase 3: JwtTokenProviderImpl - 토큰 검증 테스트**
1. 유효한 토큰 검증 테스트
   - 생성한 토큰의 유효성 검증 → true
2. 잘못된 서명 토큰 검증 테스트
   - 다른 키로 서명된 토큰 → false
3. 만료된 토큰 검증 테스트
   - 만료된 토큰 → false
4. 최소 구현
5. 테스트 통과 확인

**Phase 4: JwtTokenProviderImpl - 토큰 파싱 테스트**
1. 토큰에서 사용자 ID 추출 테스트
   - userId로 생성한 토큰에서 userId 추출
2. 토큰 만료 여부 확인 테스트
   - 유효한 토큰 → false
   - 만료된 토큰 → true
3. 최소 구현
4. 테스트 통과 확인

**Phase 5: 예외 처리 테스트**
1. InvalidTokenException 발생 테스트
   - null 토큰
   - 빈 문자열 토큰
   - 잘못된 형식 토큰
2. ExpiredTokenException 발생 테스트
   - 만료된 토큰으로 Claims 추출 시도
3. 최소 구현
4. 테스트 통과 확인

**Phase 6: 통합 시나리오 테스트**
1. Access Token 생성 → 검증 → 사용자 ID 추출
2. Refresh Token 생성 → 검증
3. 만료 시나리오 (짧은 만료 시간 설정)

---

## 9. 의존성

### 9.1 build.gradle 추가 필요

```gradle
// JWT
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
```

---

## 10. 제약 사항

### 10.1 아키텍처 제약
- domain 계층에는 인터페이스만 존재
- infrastructure 계층에 구현체 배치
- JWT 라이브러리 의존성은 infrastructure 계층에만 존재

### 10.2 TDD 제약
- 테스트 먼저 작성 필수
- 테스트 실패 → 최소 구현 → 테스트 성공 순서 준수
- Mock 사용 최소화 (설정 값 주입 시에만 사용)

### 10.3 보안 제약
- Secret Key는 환경 변수로 관리
- 운영 환경에서는 반드시 강력한 Secret Key 사용
- 토큰 만료 시간은 보안 정책에 맞게 설정

### 10.4 예외 처리 제약
- 토큰 검증 실패 시 적절한 예외 발생
- 예외 메시지에 민감한 정보 포함 금지
- 로그에 토큰 전체 내용 출력 금지

---

## 11. 구현 후 통합 지점

### 11.1 AuthService 통합
- 로그인 성공 시 토큰 발급
- 회원가입 완료 시 토큰 발급
- 토큰 갱신 처리

### 11.2 Spring Security 통합
- JWT 인증 필터 구현
- 토큰 검증 및 SecurityContext 설정
- 인증 실패 핸들러 구현

---

**작성일**: 2026-01-25