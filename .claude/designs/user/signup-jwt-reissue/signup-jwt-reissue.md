# 회원가입 완료 후 JWT 재발급

## 작업 리스트

- [x] 백엔드: `SignupCompleteResponse` record 생성
- [x] 백엔드: `OAuthLoginService.completeSignup()` 반환 타입 변경 및 JWT 재발급
- [x] 백엔드: `AuthController.completeSignup()` 응답에 새 토큰 포함
- [x] 프론트엔드: `signup.html`에서 응답의 새 토큰으로 localStorage 갱신

## 배경

회원가입 완료 시 DB의 role은 `USER`로 변경되지만, JWT 토큰에는 여전히 `SIGNING_USER`가 남아있어 서버 측 인가 체크에서 role 불일치 발생.

## 핵심 결정

- `completeSignup()` 응답을 `204 No Content` -> `200 OK + 새 JWT`로 변경
- 기존 `OAuthLoginResponse`를 재사용하지 않고, 회원가입 전용 `SignupCompleteResponse` 생성 (역할이 다름)
- 프론트엔드에서 새 토큰으로 `accessToken` 교체

## 구현

### SignupCompleteResponse

[예시 코드](./examples/signup-complete-response-example.md)

### OAuthLoginService.completeSignup() 수정

[예시 코드](./examples/oauth-login-service-example.md)

### AuthController.completeSignup() 수정

[예시 코드](./examples/auth-controller-example.md)

### signup.html 수정

[예시 코드](./examples/signup-html-example.md)