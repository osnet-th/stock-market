# OAuth 구조 변경에 따른 테스트 코드 분석

**작성일**: 2026-01-24

## 1. 개요

KakaoOAuthService와 OAuthLoginService의 의존성 방향 변경에 따라 테스트 코드가 어떻게 변경되는지 분석합니다.

## 2. KakaoOAuthServiceTest 변경사항

### 2.1 현재 테스트 구조

```java
@BeforeEach
void setUp() {
    kakaoOAuthClient = mock(KakaoOAuthClient.class);
    oauthLoginService = mock(OAuthLoginService.class);  // Mock 의존
    kakaoOAuthService = new KakaoOAuthService(kakaoOAuthClient, oauthLoginService);
}
```

**Mock 대상**:
- `KakaoOAuthClient`: 외부 API 호출 (카카오)
- `OAuthLoginService`: 비즈니스 로직 처리

**검증 대상**:
- 반환 타입: `OAuthLoginResponse`
- 반환 값: JWT 토큰, userId, role 등

### 2.2 변경 후 테스트 구조

```java
@BeforeEach
void setUp() {
    kakaoOAuthClient = mock(KakaoOAuthClient.class);
    // oauthLoginService Mock 제거
    kakaoOAuthService = new KakaoOAuthService(kakaoOAuthClient);
}
```

**Mock 대상**:
- `KakaoOAuthClient`: 외부 API 호출만 Mock

**검증 대상**:
- 반환 타입: `OAuthLoginRequest`
- 반환 값: provider, issuer, subject, email

### 2.3 테스트 케이스별 변경

#### 테스트 1: 로그인 성공 케이스

**현재**:
```java
@Test
void loginWithKakao_success() {
    // Given
    given(kakaoOAuthClient.issueToken(authorizationCode)).willReturn(tokenResponse);
    given(kakaoOAuthClient.getUserInfo(accessToken)).willReturn(userResponse);
    given(oauthLoginService.login(any(OAuthLoginRequest.class))).willReturn(expectedResponse);

    // When
    OAuthLoginResponse response = kakaoOAuthService.loginWithKakao(authorizationCode);

    // Then
    assertThat(response.accessToken()).isEqualTo("jwt-access-token");
    assertThat(response.userId()).isEqualTo(1L);
}
```

**변경 후**:
```java
@Test
void loginWithKakao_success() {
    // Given
    given(kakaoOAuthClient.issueToken(authorizationCode)).willReturn(tokenResponse);
    given(kakaoOAuthClient.getUserInfo(accessToken)).willReturn(userResponse);
    // oauthLoginService.login() Mock 제거

    // When
    OAuthLoginRequest request = kakaoOAuthService.loginWithKakao(authorizationCode);

    // Then
    assertThat(request.provider()).isEqualTo(OAuthProvider.KAKAO);
    assertThat(request.issuer()).isEqualTo("https://kauth.kakao.com");
    assertThat(request.subject()).isEqualTo("123456789");
    assertThat(request.email()).isEqualTo("test@kakao.com");
}
```

**주요 변경점**:
- OAuthLoginService Mock 제거
- 반환 타입: `OAuthLoginResponse` → `OAuthLoginRequest`
- 검증 대상: JWT 토큰/userId → provider/issuer/subject/email
- 테스트 범위 축소: 외부 API 호출 + DTO 변환만 검증

#### 테스트 2: 토큰 발급 실패

**변경 없음**:
- 카카오 API 호출 실패 시나리오는 동일하게 유지
- 예외 전파 검증은 그대로 유지

#### 테스트 3: 사용자 정보 조회 실패

**변경 없음**:
- 카카오 API 호출 실패 시나리오는 동일하게 유지
- 예외 전파 검증은 그대로 유지

### 2.4 변경 요약

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| Mock 객체 수 | 2개 (KakaoOAuthClient, OAuthLoginService) | 1개 (KakaoOAuthClient) |
| 반환 타입 | OAuthLoginResponse | OAuthLoginRequest |
| 검증 필드 | accessToken, refreshToken, userId, role | provider, issuer, subject, email |
| 테스트 범위 | 카카오 API + 비즈니스 로직 전체 | 카카오 API + DTO 변환만 |
| 실패 케이스 | 변경 없음 | 변경 없음 |

## 3. OAuthLoginServiceTest 변경사항

### 3.1 현재 테스트 구조

```java
@BeforeEach
void setUp() {
    oauthLoginService = new OAuthLoginService(
        userRepository,
        oauthAccountRepository,
        oauthConnectionService,
        jwtTokenProvider
    );
}
```

**기존 테스트**:
- `login(OAuthLoginRequest)`: 기존 계정 로그인, 신규 계정 생성
- `completeSignup()`: 가입 완료
- `connectAccount()`: 기존 계정 연결

### 3.2 추가 필요 테스트

**새로운 메서드**: `loginWithKakao(String authorizationCode)`

```java
@Mock
private KakaoOAuthService kakaoOAuthService;  // 추가

@BeforeEach
void setUp() {
    oauthLoginService = new OAuthLoginService(
        userRepository,
        oauthAccountRepository,
        oauthConnectionService,
        jwtTokenProvider,
        kakaoOAuthService  // 추가
    );
}
```

### 3.3 추가 테스트 케이스

#### 테스트 1: loginWithKakao 성공

```java
@Test
@DisplayName("카카오 인가 코드로 로그인 성공")
void loginWithKakao_success() {
    // Given
    String authorizationCode = "test-code";
    OAuthLoginRequest request = new OAuthLoginRequest(
        OAuthProvider.KAKAO,
        "https://kauth.kakao.com",
        "12345",
        "test@kakao.com"
    );

    given(kakaoOAuthService.loginWithKakao(authorizationCode)).willReturn(request);
    given(oauthAccountRepository.findByProviderAndIssuerAndSubject(...)).willReturn(Optional.empty());
    // ... 나머지 Mock 설정

    // When
    OAuthLoginResponse response = oauthLoginService.loginWithKakao(authorizationCode);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.accessToken()).isEqualTo("access-token");
}
```

**검증 내용**:
1. KakaoOAuthService.loginWithKakao() 호출 확인
2. 반환된 OAuthLoginRequest로 내부 login() 호출 확인
3. 최종 OAuthLoginResponse 검증

#### 테스트 2: loginWithGoogle 성공 (향후)

```java
@Test
@DisplayName("구글 인가 코드로 로그인 성공")
void loginWithGoogle_success() {
    // 유사한 구조로 Google OAuth 테스트
}
```

### 3.4 기존 테스트 영향

**변경 없음**:
- `login(OAuthLoginRequest)` 테스트는 그대로 유지
- `completeSignup()` 테스트는 그대로 유지
- `connectAccount()` 테스트는 그대로 유지

**이유**:
- 기존 메서드는 수정 없이 유지
- 새 메서드만 추가되므로 기존 테스트에 영향 없음

## 4. 테스트 범위 변화

### 4.1 KakaoOAuthService 테스트 범위

**변경 전**:
```
[KakaoOAuthServiceTest]
  → 카카오 API 호출
  → DTO 변환
  → OAuthLoginService.login() 호출  (Mock)
  → JWT 토큰 발급 결과 검증        (Mock)
```

**변경 후**:
```
[KakaoOAuthServiceTest]
  → 카카오 API 호출
  → DTO 변환
  → OAuthLoginRequest 생성/반환 검증
```

**장점**:
- 단일 책임 원칙 준수: 외부 API 호출 + DTO 변환만 검증
- Mock 의존성 감소: OAuthLoginService Mock 제거
- 테스트 단순화: 검증 대상 축소

### 4.2 OAuthLoginService 테스트 범위

**추가**:
```
[OAuthLoginServiceTest - loginWithKakao]
  → KakaoOAuthService.loginWithKakao() 호출  (Mock)
  → OAuthLoginRequest 수신
  → login() 내부 호출
  → OAuthLoginResponse 반환 검증
```

**장점**:
- Provider별 진입점 테스트 추가
- 전체 플로우 통합 검증
- 기존 login() 테스트와 분리

## 5. 수정 대상 파일 목록

### 필수 수정
1. **KakaoOAuthServiceTest.java**
   - OAuthLoginService Mock 제거
   - 생성자 파라미터 수정
   - 반환 타입 검증 변경: OAuthLoginResponse → OAuthLoginRequest
   - 검증 필드 변경

2. **OAuthLoginServiceTest.java**
   - KakaoOAuthService Mock 추가
   - 생성자에 KakaoOAuthService 파라미터 추가
   - `loginWithKakao()` 테스트 케이스 추가

### 선택 수정
- 기존 테스트 케이스들은 변경 없음

## 6. 테스트 실행 순서

1. **KakaoOAuthServiceTest 수정**
   - Mock 제거 및 반환 타입 변경
   - 테스트 실행하여 통과 확인

2. **KakaoOAuthService 구현 변경**
   - 반환 타입 변경
   - OAuthLoginService 의존성 제거

3. **OAuthLoginServiceTest 수정**
   - KakaoOAuthService Mock 추가
   - loginWithKakao() 테스트 추가
   - 테스트 실행하여 통과 확인

4. **OAuthLoginService 구현 변경**
   - KakaoOAuthService 의존성 추가
   - loginWithKakao() 메서드 추가

5. **전체 테스트 실행**
   - 모든 테스트 통과 확인

## 7. 변경 후 테스트 이점

1. **명확한 책임 분리**
   - KakaoOAuthService: 카카오 API 통신만 테스트
   - OAuthLoginService: 비즈니스 로직만 테스트

2. **Mock 의존성 감소**
   - 각 서비스가 외부 경계에서만 Mock 사용
   - 순환 의존 제거로 테스트 복잡도 감소

3. **유지보수성 향상**
   - Provider별 서비스는 외부 API만 검증
   - OAuthLoginService는 통합 플로우 검증
   - 각 레이어의 책임이 명확해짐

4. **확장성**
   - Google OAuth 추가 시 동일한 패턴 적용 가능
   - 테스트 구조 일관성 유지