# SIGNING_USER 회원가입 플로우 가드 설계

## 작업 리스트

- [x] 프론트엔드: signup.html 회원가입 페이지 생성
- [x] 프론트엔드: app.js `init()`에서 role 체크 후 SIGNING_USER → signup.html 리다이렉트
- [x] 프론트엔드: signup.html에서 회원가입 완료 API 호출 및 대시보드 이동

## 배경

OAuth 로그인 후 신규 사용자는 `SIGNING_USER` role로 생성되지만, 프론트엔드에서 role 체크 없이 바로 대시보드에 진입하는 문제. `SIGNING_USER`는 회원가입 폼을 통해 정보를 입력해야 `USER`로 전환되어야 한다.

## 현재 문제

- `app.js:init()` → `handleOAuthCallback()` → `checkLoggedIn()`에서 `token + userId`만 확인
- role이 localStorage에 저장만 되고 **분기 처리 없음**
- `SIGNING_USER`도 대시보드에 그대로 진입
- 회원가입 페이지(`signup.html`)가 존재하지 않음

## 핵심 결정

- **별도 signup.html 페이지 생성**: 대시보드 내 모달이 아닌, 독립된 페이지로 구성 (가입 전 대시보드 접근 자체를 차단)
- **role 체크 시점**: `init()`에서 `loadMyProfile()` 응답의 role을 확인 (localStorage의 role이 아닌 서버 응답 기준)
- **리다이렉트 플로우**: `SIGNING_USER` → `signup.html`, 가입 완료 → `/` (대시보드)
- **백엔드 변경 없음**: 기존 `POST /signup` API와 `GET /api/users/me` API를 그대로 사용

## 구현

### signup.html

위치: `src/main/resources/static/signup.html`

- 이름, 닉네임, 전화번호 입력 폼
- 기존 `login.html`과 동일한 디자인 톤 (Tailwind CSS)
- `API.signup()` 호출 → 성공 시 localStorage의 role을 `USER`로 갱신 → `/`로 이동
- 로그인하지 않은 상태(token 없음)에서 접근 시 `/login.html`로 리다이렉트
- 이미 `USER` role인 경우 `/`로 리다이렉트

[예시 코드](./examples/signup-html-example.md)

### app.js 수정

위치: `src/main/resources/static/js/app.js`

- `init()` 수정: `loadMyProfile()` 후 role이 `SIGNING_USER`이면 `signup.html`로 리다이렉트
- `loadMyProfile()`에서 role도 localStorage에 갱신

[예시 코드](./examples/app-js-example.md)

## 주의사항

- `loadMyProfile()` 응답의 role을 기준으로 분기 (리다이렉트 URL의 role 파라미터는 초기 진입 시에만 사용, 이후 서버 응답이 최신)
- signup.html에서 token이 없으면 login.html로 보내야 함 (직접 URL 접근 방어)
- 회원가입 완료 후 localStorage의 role을 `USER`로 갱신해야 다음 페이지 로드 시 다시 signup으로 가지 않음