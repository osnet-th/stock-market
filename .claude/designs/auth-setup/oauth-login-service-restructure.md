# OAuth Login Service 구조 재설계

**작성일**: 2026-01-24

## 1. 개요

Controller가 Provider별 서비스를 직접 호출하는 구조에서, OAuthLoginService를 단일 진입점으로 하여 Provider별 서비스를 내부에서 호출하는 구조로 변경합니다.

## 2. 현재 구조

### 의존성 흐름
```
AuthController → KakaoOAuthService → OAuthLoginService
```

### 문제점
- Controller가 Provider별 서비스(KakaoOAuthService, GoogleOAuthService 등)를 각각 의존
- Provider 추가 시 Controller 수정 필요
- Provider별 분기 로직이 Controller에 분산

## 3. 변경 후 구조

### 의존성 흐름
```
AuthController → OAuthLoginService → KakaoOAuthService
                                   → GoogleOAuthService (향후)
```

### 데이터 흐름
```
1. AuthController.kakaoCallback(code)
   ↓
2. OAuthLoginService.loginWithKakao(code)
   ↓
3. KakaoOAuthService.loginWithKakao(code)
   → OAuthLoginRequest 반환 (카카오 토큰 발급 + 사용자 정보 조회)
   ↓
4. OAuthLoginService.login(request)
   → OAuthLoginResponse 반환 (DB 조회/저장, JWT 토큰 발급)
```

### 장점
- **단일 진입점**: Controller는 OAuthLoginService만 의존
- **Provider 분기 일원화**: OAuthLoginService에서 Provider별 로직 분기 처리
- **확장성**: 새 Provider 추가 시 OAuthLoginService만 수정
- **일관성**: 모든 OAuth 로그인 로직이 OAuthLoginService를 통과
- **역할 분리**: Provider별 서비스는 외부 API 호출 + DTO 변환만 담당

## 4. 변경점

### 4.1 OAuthLoginService

**추가 메서드**:
- `loginWithKakao(String authorizationCode)`: 카카오 OAuth 로그인 처리
  - `kakaoOAuthService.loginWithKakao(code)` 호출 → `OAuthLoginRequest` 반환받음
  - `this.login(request)` 호출 → `OAuthLoginResponse` 반환
- `loginWithGoogle(String authorizationCode)`: 구글 OAuth 로그인 처리 (향후)

**추가 의존성**:
- `KakaoOAuthService`: 카카오 OAuth 처리 위임
- `GoogleOAuthService`: 구글 OAuth 처리 위임 (향후)

**역할 변경**:
- 기존: OAuth 로그인의 공통 로직만 처리 (`login(OAuthLoginRequest)`)
- 변경 후: Provider별 진입점 메서드 제공 + 공통 로직 처리

### 4.2 KakaoOAuthService

**반환 타입 변경**:
- 기존: `OAuthLoginResponse loginWithKakao(String authorizationCode)`
- 변경 후: `OAuthLoginRequest loginWithKakao(String authorizationCode)`

**역할 변경**:
- 기존: 카카오 토큰 발급 + 사용자 정보 조회 + OAuthLoginService.login() 호출
- 변경 후: 카카오 토큰 발급 + 사용자 정보 조회 + OAuthLoginRequest 생성/반환만 담당

**제거되는 의존성**:
- `OAuthLoginService` 의존성 제거 (순환 참조 방지)

**책임 범위**:
- 카카오 API와의 통신 (토큰 발급, 사용자 정보 조회)
- 카카오 응답 → OAuthLoginRequest DTO 변환
- 비즈니스 로직(DB 조회/저장, JWT 발급)은 OAuthLoginService로 위임

### 4.3 AuthController

**의존성 변경**:
- 기존: `KakaoOAuthService` 의존
- 변경 후: `OAuthLoginService` 의존

**메서드 변경**:
- 기존: `kakaoOAuthService.loginWithKakao(code)`
- 변경 후: `oauthLoginService.loginWithKakao(code)`

## 5. 변경 대상 파일

### 수정 파일

**1. `KakaoOAuthService.java`**
   - 반환 타입 변경: `OAuthLoginResponse` → `OAuthLoginRequest`
   - OAuthLoginService 의존성 제거
   - 생성자에서 OAuthLoginService 파라미터 제거
   - `loginWithKakao()` 메서드 로직 변경:
     - OAuthLoginService.login() 호출 제거
     - OAuthLoginRequest 생성 후 반환만 수행

**2. `OAuthLoginService.java`**
   - KakaoOAuthService 필드 추가
   - 생성자 파라미터 추가
   - `loginWithKakao(String authorizationCode)` 메서드 추가:
     - `kakaoOAuthService.loginWithKakao(code)` 호출
     - `this.login(request)` 호출

**3. `AuthController.java`**
   - 의존성 변경: `KakaoOAuthService` → `OAuthLoginService`
   - 생성자 파라미터 변경
   - 메서드 호출 변경: `kakaoOAuthService` → `oauthLoginService`

## 6. 구현 순서

1. **KakaoOAuthService 수정**
   - 반환 타입 변경: `OAuthLoginResponse` → `OAuthLoginRequest`
   - OAuthLoginService 의존성 제거
   - `loginWithKakao()` 로직 수정: OAuthLoginRequest만 생성/반환

2. **OAuthLoginService 수정**
   - KakaoOAuthService 필드 추가
   - 생성자에 KakaoOAuthService 파라미터 추가
   - `loginWithKakao(String authorizationCode)` 메서드 추가

3. **AuthController 수정**
   - 의존성 변경: `KakaoOAuthService` → `OAuthLoginService`
   - 생성자 파라미터 변경
   - 메서드 호출 변경

4. **테스트 코드 수정**
   - `KakaoOAuthServiceTest.java` 수정 (반환 타입 변경)
   - `OAuthLoginService` 관련 테스트 추가 (필요시)

## 7. 향후 확장

Google OAuth 추가 시:
1. `GoogleOAuthService` 구현
2. OAuthLoginService에 `loginWithGoogle()` 메서드 추가
3. AuthController에 `/oauth/google` 엔드포인트 추가