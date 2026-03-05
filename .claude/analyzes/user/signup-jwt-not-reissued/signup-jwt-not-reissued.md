# 회원가입 완료 후 JWT 토큰 미갱신 문제

## 현재 상태

- `POST /signup` (`AuthController:52`) -> `OAuthLoginService.completeSignup()` (:91)
- DB에서 User role을 `SIGNING_USER` -> `USER`로 변경
- 응답: `204 No Content` (새 JWT 미발급)

## 문제점

JWT Access Token에 `role: SIGNING_USER`가 클레임으로 포함되어 있음 (`JwtTokenProviderImpl:35`).
회원가입 완료 후에도 **기존 토큰을 계속 사용**하므로, 서버 측에서 토큰의 role은 여전히 `SIGNING_USER`.

프론트엔드에서 `localStorage.role = 'USER'`로 갱신하지만 이는 화면 분기용일 뿐, **서버 인가 체크에서 role 불일치** 발생.

## 근본 원인

`completeSignup()`이 role 변경 후 새 JWT를 발급하지 않음.

## 영향 범위

- role 기반 서버 측 인가가 있는 모든 API에서 `SIGNING_USER`로 인식
- 토큰 만료 전까지 role 불일치 지속

## 해결 방향

`completeSignup()` 응답에 새 JWT(role=USER)를 포함하여 반환. 프론트엔드에서 새 토큰으로 교체.