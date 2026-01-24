# OAuth 구조 설계 문서와 기존 코드 차이점 분석

## 1. 개요

카카오 OAuth 구현을 위한 설계 문서와 기존 코드 구조 간의 차이점을 분석하여 통합 방안을 제시합니다.

---

## 2. 주요 차이점

### 2.1 OAuthLoginService 책임 범위

#### 설계 문서 (oauth-kakao-design.md)
```java
public LoginResult loginWithKakao(String code) {
    // 1. 인가 코드로 카카오 토큰 발급
    KakaoTokenResponse token = kakaoOAuthClient.issueToken(code);

    // 2. 액세스 토큰으로 사용자 정보 조회
    KakaoUserResponse user = kakaoOAuthClient.getUserInfo(token.accessToken());

    // 3. 내부 사용자 매핑
    UserAccount account = userAuthService.resolveKakaoUser(user.id(), user.email(), user.nickname());

    // 4. JWT 발급
    String jwt = tokenIssuer.issue(account.id());

    return new LoginResult(jwt, account.id(), account.isNew());
}
```

**책임**: 인가 코드 → 토큰 발급 → 사용자 정보 조회 → 내부 매핑 → JWT 발급 (전체 흐름)

---

#### 기존 코드 (현재 OAuthLoginService.java)
```java
public OAuthLoginResponse login(OAuthLoginRequest request) {
    OAuthProvider provider = request.provider();
    String issuer = request.issuer();
    String subject = request.subject();
    String email = request.email();

    // provider + issuer + subject로 OAuth 계정 조회
    Optional<OAuthAccount> existingAccount = oauthAccountRepository
            .findByProviderAndIssuerAndSubject(provider, issuer, subject);

    if (existingAccount.isPresent()) {
        // 기존 계정 로그인
        ...
    } else {
        // 신규 계정 생성 (SIGNING_USER)
        ...
    }
}
```

**책임**: 이미 외부에서 받은 OAuth 정보(provider, issuer, subject, email)로 내부 사용자 매핑만 처리

---

### 2.2 OAuth 제공자 정보 구조

#### 설계 문서
- 카카오 사용자 ID만 사용
- `resolveKakaoUser(kakaoId, email, nickname)` 형태로 간단하게 매핑

#### 기존 코드
- **OAuthProvider** (enum): KAKAO, GOOGLE
- **issuer**: OAuth 제공자 도메인 (예: https://kauth.kakao.com)
- **subject**: OAuth 제공자의 사용자 고유 ID
- **email**: 사용자 이메일

→ OpenID Connect (OIDC) 표준 형식을 따름

---

### 2.3 사용자 상태 관리

#### 설계 문서
- 단순 신규/기존 사용자 구분
- `isNew` 플래그만 반환

#### 기존 코드
- **UserStatus** enum: SIGNING, ACTIVE, INACTIVE, DELETED
- **SIGNING 상태**: OAuth 인증 완료, 추가 정보 입력 대기
- 가입 완료 처리: `completeSignup()`
- 기존 계정 연결: `connectAccount()`

→ 더 복잡한 사용자 상태 흐름 관리

---

### 2.4 응답 DTO 구조

#### 설계 문서 (LoginResult)
```java
public record LoginResult(
    String accessToken,
    Long userId,
    boolean isNew
) {}
```

#### 기존 코드 (OAuthLoginResponse)
```java
public record OAuthLoginResponse(
    String accessToken,
    String refreshToken,
    Long userId,
    UserRole role
) {}
```

→ refreshToken과 UserRole 포함 여부 차이

---

## 3. 통합 방안

### 방안 1: Application 계층에 KakaoOAuthService 추가 (권장)

#### 구조
```
user/application/
├── OAuthLoginService.java (기존 유지)
├── KakaoOAuthService.java (신규)
└── GoogleOAuthService.java (향후 추가)
```

#### KakaoOAuthService 역할
```java
public OAuthLoginResponse loginWithKakao(String authorizationCode) {
    // 1. 카카오 토큰 발급
    KakaoTokenResponse token = kakaoOAuthClient.issueToken(authorizationCode);

    // 2. 카카오 사용자 정보 조회
    KakaoUserResponse kakaoUser = kakaoOAuthClient.getUserInfo(token.accessToken());

    // 3. OAuthLoginRequest 생성
    OAuthLoginRequest request = new OAuthLoginRequest(
        OAuthProvider.KAKAO,
        "https://kauth.kakao.com",  // issuer
        kakaoUser.id().toString(),   // subject
        kakaoUser.getEmail()
    );

    // 4. 기존 OAuthLoginService.login() 호출
    return oauthLoginService.login(request);
}
```

**장점**:
- 기존 OAuthLoginService 로직 재사용
- 카카오/구글 각각의 특화된 처리 분리
- OIDC 표준 형식 유지

---

### 방안 2: Presentation 계층에서 직접 처리

#### AuthController
```java
@GetMapping("/oauth/kakao")
public ResponseEntity<OAuthLoginResponse> kakaoCallback(@RequestParam("code") String code) {
    // 1. 카카오 토큰 발급
    KakaoTokenResponse token = kakaoOAuthClient.issueToken(code);

    // 2. 카카오 사용자 정보 조회
    KakaoUserResponse kakaoUser = kakaoOAuthClient.getUserInfo(token.accessToken());

    // 3. OAuthLoginRequest 생성
    OAuthLoginRequest request = new OAuthLoginRequest(
        OAuthProvider.KAKAO,
        "https://kauth.kakao.com",
        kakaoUser.id().toString(),
        kakaoUser.getEmail()
    );

    // 4. OAuthLoginService 호출
    OAuthLoginResponse response = oauthLoginService.login(request);

    return ResponseEntity.ok(response);
}
```

**단점**:
- Controller에 비즈니스 로직 포함
- 트랜잭션 경계 불명확
- 테스트 복잡도 증가

---

### 방안 3: OAuthLoginService에 메서드 추가

```java
public OAuthLoginResponse loginWithKakao(String authorizationCode) {
    // KakaoOAuthClient 호출 후 기존 login() 메서드 호출
}

public OAuthLoginResponse loginWithGoogle(String authorizationCode) {
    // GoogleOAuthClient 호출 후 기존 login() 메서드 호출
}
```

**단점**:
- OAuthLoginService가 특정 제공자에 의존
- 단일 책임 원칙 위배
- 새로운 제공자 추가 시 서비스 수정 필요

---

## 4. 권장 방안

### **방안 1 채택 권장**

**이유**:
1. **단일 책임 원칙 준수**
   - OAuthLoginService: 제공자 독립적인 사용자 매핑
   - KakaoOAuthService: 카카오 전용 인증 흐름
   - GoogleOAuthService: 구글 전용 인증 흐름

2. **확장성**
   - 새로운 OAuth 제공자 추가 시 새 서비스만 추가
   - 기존 코드 수정 불필요

3. **테스트 용이성**
   - 각 계층별 Mock 테스트 명확
   - 책임별 단위 테스트 작성 가능

4. **트랜잭션 경계 명확**
   - Application 계층에서 @Transactional 관리

---

## 5. 구현 계획

### 5.1 작업 목록
1. `KakaoOAuthService` 생성 (Application 계층)
2. `AuthController`에 `/oauth/kakao` 엔드포인트 추가
3. TDD로 `KakaoOAuthService` 테스트 작성
4. TDD로 `AuthController` 테스트 작성

### 5.2 설계 문서 업데이트
- `oauth-kakao-design.md` 수정
- Application 계층에 KakaoOAuthService 추가
- 기존 OAuthLoginService와의 관계 명시

---

## 6. issuer 값 결정

### OIDC 표준 형식
- Kakao: `https://kauth.kakao.com`
- Google: `https://accounts.google.com`

### 검증 방법
- 카카오 ID Token 디코딩 시 `iss` 클레임 확인
- OpenID Provider Configuration 엔드포인트 확인

---

**작성일**: 2026-01-24