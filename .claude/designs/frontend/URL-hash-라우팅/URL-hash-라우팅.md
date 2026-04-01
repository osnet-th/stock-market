# URL hash 기반 페이지 상태 유지

## 배경

현재 SPA 구조에서 `currentPage` 변수가 메모리에만 존재하여 브라우저 새로고침 시 항상 대시보드(`home`)로 돌아간다. URL hash를 활용하여 현재 페이지 상태를 유지한다.

## 현재 구조

- `app.js:5` - `currentPage: 'home'` 하드코딩 초기값
- `app.js:85` - `navigateTo(page)` 호출 시 URL 변경 없음
- `app.js:52` - `init()` 에서 항상 `loadHomeSummary()` 호출

## 핵심 결정

### URL hash 방식 선택 이유
- 서버 요청 없이 클라이언트에서만 처리 가능
- 브라우저 뒤로가기/앞으로가기 지원 (`popstate` 이벤트)
- URL 형태: `/#home`, `/#portfolio`, `/#ecos` 등

### 유효하지 않은 hash 처리
- `menus` 배열의 `key` 값과 비교하여 유효성 검증
- 유효하지 않으면 `home`으로 폴백

## 변경 대상

| 파일 | 변경 내용 |
|------|----------|
| `app.js:5` | 초기값을 URL hash에서 읽도록 변경 |
| `app.js:52-83` | `init()`에서 hash 기반 페이지 로드 + `popstate` 리스너 등록 |
| `app.js:85-123` | `navigateTo()`에서 `location.hash` 업데이트 |

## 작업 리스트

- [x] `app.js` - `currentPage` 초기값을 hash에서 복원하도록 변경
- [x] `app.js` - `navigateTo()`에서 `location.hash` 업데이트
- [x] `app.js` - `init()`에서 hash 기반 초기 페이지 로드 + `popstate` 리스너 등록

## 구현 예시

- [app.js 변경](examples/app-js-example.md)
