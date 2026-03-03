# 사용자 프로필 조회 API 설계

## 작업 리스트

- [x] UserProfileResponse DTO 생성
- [x] UserProfileService 생성
- [x] UserProfileController 생성 (`GET /api/users/me`)
- [x] 프론트엔드: `API.getMyProfile()` 메서드 추가 (api.js)
- [x] 프론트엔드: 로그인 후 프로필 조회 및 닉네임 표시 (app.js, index.html)

## 배경

로그인 후 화면 헤더에 사용자 serial ID가 그대로 노출됨. 토큰 기반으로 사용자 프로필을 조회하는 API를 추가하여 닉네임/이름으로 표시.

## 핵심 결정

- **엔드포인트**: `GET /api/users/me` (토큰에서 userId 추출, PathVariable 불필요)
- **userId 추출**: `Authorization` 헤더의 JWT 토큰에서 `JwtTokenProvider.getUserIdFromToken()` 사용
- **표시 우선순위**: nickname > name > "사용자 {id}" (SIGNING_USER는 닉네임/이름이 없을 수 있음)
- **Security**: `/api/users/me`는 인증 필요 (기존 `anyRequest().authenticated()` 적용)

## 구현

### UserProfileResponse

위치: `user/application/dto/UserProfileResponse.java`

[예시 코드](./examples/application-example.md)

### UserProfileService

위치: `user/application/UserProfileService.java`

- JWT 토큰에서 userId 추출
- UserRepository로 사용자 조회
- UserProfileResponse 반환

[예시 코드](./examples/application-example.md)

### UserProfileController

위치: `user/presentation/UserProfileController.java`

- `GET /api/users/me`
- `Authorization` 헤더에서 Bearer 토큰 추출 후 서비스에 전달

[예시 코드](./examples/presentation-example.md)

### 프론트엔드 변경

- `api.js`: `getMyProfile()` 메서드 추가
- `app.js`: `auth` 객체에 `displayName` 추가, `init()`에서 프로필 조회
- `index.html`: `auth.userId` 대신 `auth.displayName` 표시

[예시 코드](./examples/frontend-example.md)

## 주의사항

- SIGNING_USER 상태(가입 미완료)에서는 nickname/name이 null → fallback 표시 필요
- JWT 필터가 아직 TODO 상태(DevSecurityConfig에서 permitAll) → 컨트롤러에서 직접 토큰 파싱