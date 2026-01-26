# 카카오 OAuth 로그인 설계

## 1. 개요

### 목적
- 카카오 OAuth 2.0을 이용한 소셜 로그인 기능 구현
- DDD 계층형 구조에 맞춘 책임 분리
- 인가 코드 → 토큰 발급 → 사용자 정보 조회 → 내부 로그인 처리 흐름 구현

### 기획 문서
- [kakao-oauth-api-guide.md](../docs/kakao-oauth-api-guide.md)
- [auth-package-structure.md](./auth-package-structure.md)

### 기본 정보
- REST API KEY: `9e30f4bba41efa94c5256b8f981c2da9`
- Client Secret Key: `OPXDUa5oZsvKzWdk1hCeBc2ju9Gc8OiG`
- Redirect URL: `http://localhost:8080/oauth/kakao`

---

## 2. 전체 흐름

```
1. 사용자 → 카카오 인가 페이지 이동 (클라이언트 처리)
2. 카카오 → Redirect URL로 인가 코드 전달
3. 백엔드 → 인가 코드로 카카오 토큰 발급 요청
4. 카카오 → 액세스 토큰/리프레시 토큰 응답
5. 백엔드 → 액세스 토큰으로 사용자 정보 조회
6. 카카오 → 사용자 정보 응답 (id, email, nickname)
7. 백엔드 → 내부 사용자 매핑 (신규 가입 또는 기존 로그인)
8. 백엔드 → 내부 JWT 발급
9. 백엔드 → 클라이언트에 JWT 응답
```

---

## 3. 계층별 설계

### 3.1 Domain 계층

#### 책임
- OAuth 로그인은 사용자 인증 흐름의 일부이므로, domain에는 별도 OAuth 모델을 두지 않음
- 기존 User 도메인 모델에 OAuth 제공자 정보 포함
- OAuth 사용자 식별은 application 계층에서 처리

#### 관련 도메인 모델
- `User`: OAuth 제공자(KAKAO) 정보 포함
- `AuthProvider` enum: KAKAO, GOOGLE, LOCAL

---

### 3.2 Application 계층

#### OAuthLoginService
- **위치**: `user/application/OAuthLoginService.java`
- **책임**: 카카오 OAuth 로그인 유스케이스 처리
- **주요 메서드**:
  ```java
  LoginResponse loginWithKakao(String authorizationCode)
  ```

#### 처리 흐름
1. 인가 코드로 카카오 토큰 발급 (Infrastructure 위임)
2. 액세스 토큰으로 카카오 사용자 정보 조회 (Infrastructure 위임)
3. 카카오 사용자 ID 기반 내부 사용자 매핑
   - 기존 회원: 로그인 처리
   - 신규 회원: 가입 처리
4. 내부 JWT 발급 (Domain 포트 호출)
5. LoginResponse 반환

#### DTO
- **OAuthLoginRequest**: 인가 코드 전달
  ```java
  public record OAuthLoginRequest(String code) {}
  ```

- **LoginResponse**: JWT 및 사용자 정보 반환
  ```java
  public record LoginResponse(
      String accessToken,
      String refreshToken,
      Long userId,
      boolean isNewUser
  ) {}
  ```

---

### 3.3 Infrastructure 계층

#### oauth/kakao 패키지 구조
```
infrastructure/oauth/kakao/
├── KakaoOAuthClient.java (카카오 API 호출)
├── KakaoOAuthProperties.java (설정 바인딩)
└── dto/
    ├── KakaoTokenResponse.java
    └── KakaoUserResponse.java
```

#### KakaoOAuthClient
- **책임**: 카카오 REST API 호출 전담
- **주요 메서드**:
  ```java
  KakaoTokenResponse issueToken(String authorizationCode)
  KakaoUserResponse getUserInfo(String accessToken)
  ```

#### 구현 상세

**1. 토큰 발급**
- URL: `https://kauth.kakao.com/oauth/token`
- Method: POST
- Content-Type: `application/x-www-form-urlencoded`
- Body 파라미터:
  - `grant_type`: authorization_code
  - `client_id`: REST API KEY
  - `client_secret`: Client Secret Key
  - `redirect_uri`: Redirect URL
  - `code`: 인가 코드

**2. 사용자 정보 조회**
- URL: `https://kapi.kakao.com/v2/user/me`
- Method: POST
- Authorization: `Bearer {access_token}`

#### KakaoOAuthProperties
```java
@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String tokenUri,
    String userInfoUri
) {}
```

#### DTO 구조

**KakaoTokenResponse**
```java
public record KakaoTokenResponse(
    String accessToken,
    String tokenType,
    String refreshToken,
    Long expiresIn,
    Long refreshTokenExpiresIn
) {}
```

**KakaoUserResponse**
```java
public record KakaoUserResponse(
    Long id,
    String email,
    String nickname
) {}
```

---

### 3.4 Presentation 계층

#### AuthController
- **경로**: `user/presentation/AuthController.java`
- **엔드포인트**:
  ```java
  @GetMapping("/oauth/kakao")
  public ResponseEntity<LoginResponse> kakaoCallback(
      @RequestParam("code") String code
  )
  ```

#### 책임
- HTTP 요청 수신 (인가 코드)
- OAuthLoginService 호출
- LoginResponse 반환

---

## 4. 설정 구조

### application.yml
```yaml
kakao:
  oauth:
    client-id: 9e30f4bba41efa94c5256b8f981c2da9
    client-secret: OPXDUa5oZsvKzWdk1hCeBc2ju9Gc8OiG
    redirect-uri: http://localhost:8080/oauth/kakao
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
```

---

## 5. 예외 처리 전략

### 5.1 예외 시나리오

#### 인가 코드 관련
- 인가 코드 만료
- 인가 코드 중복 사용
- 잘못된 인가 코드

**처리 방안**: 카카오 API 에러 응답 기록, 사용자에게 재인증 안내

#### 토큰 발급 실패
- 네트워크 오류
- 카카오 서버 오류
- 잘못된 설정 (client_id, secret 불일치)

**처리 방안**: 에러 로그 기록, 사용자에게 일시적 오류 안내

#### 사용자 정보 조회 실패
- 액세스 토큰 만료
- 권한 부족 (동의 항목 미동의)

**처리 방안**: 토큰 재발급 또는 재인증 유도

### 5.2 예외 클래스 구조

```
infrastructure/oauth/kakao/exception/
├── KakaoOAuthException.java (추상 클래스)
├── KakaoTokenIssueFailed.java
└── KakaoUserInfoFetchFailed.java
```

---

## 6. 보안 고려사항

### 6.1 설정 관리
- Client Secret은 환경 변수 또는 보안 저장소에서 관리 (운영 환경)
- Redirect URL은 카카오 개발자 콘솔 설정과 정확히 일치

### 6.2 로그 관리
- 인가 코드, 액세스 토큰, 리프레시 토큰을 로그에 남기지 않음
- 사용자 ID 정도만 마스킹하여 로그 기록

### 6.3 타임아웃 설정
- 카카오 API 호출 시 타임아웃 설정 (연결 3초, 읽기 5초)
- 재시도 정책 고려 (일시적 네트워크 오류 대비)

---

## 7. TDD 구현 순서

### 7.1 Infrastructure 계층
1. **KakaoOAuthClient 단위 테스트**
   - Mock RestClient로 토큰 발급 테스트
   - Mock RestClient로 사용자 정보 조회 테스트
   - 예외 상황 테스트 (네트워크 오류, 4xx/5xx 응답)

### 7.2 Application 계층
2. **OAuthLoginService 단위 테스트**
   - Mock KakaoOAuthClient로 신규 회원 가입 흐름 테스트
   - Mock KakaoOAuthClient로 기존 회원 로그인 흐름 테스트
   - Mock UserRepository, TokenIssuer
   - 예외 전파 테스트

### 7.3 Presentation 계층
3. **AuthController 단위 테스트**
   - Mock OAuthLoginService
   - 정상 응답 테스트
   - 인가 코드 누락 시 400 응답 테스트

---

## 8. 구현 제약사항

- JPA Entity는 연관관계를 사용하지 않음 (ID 기반 참조만 허용)
- domain 계층에는 Spring, JPA 의존성 없음
- 외부 API 호출은 infrastructure에서만 처리
- 테스트 코드 없이 구현 코드를 먼저 작성하지 않음 (TDD 필수)

---

## 9. 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [auth-package-structure.md](./auth-package-structure.md)
- [kakao-oauth-api-guide.md](../docs/kakao-oauth-api-guide.md)

---

**작성일**: 2026-01-24