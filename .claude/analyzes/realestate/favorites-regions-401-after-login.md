# 로그인 직후 `/api/realestate/favorites/regions` 401 → 재로그인 리다이렉트 원인 분석

## 증상

- 로그인 직후 `GET /api/realestate/favorites/regions` 호출 시 401 발생
- `api.js`의 401 핸들러가 토큰을 지우고 `/login.html`로 리다이렉트 → 재로그인 화면

## 근본 원인: 부트 순서 race condition (x-init이 토큰 저장보다 먼저 실행)

`app.js init()`의 실행 순서가 문제다.

```
app.js:125  await PartialLoader.mountAllPartials(...)   // ← 여기서 realestate x-init 실행
app.js:151  this.handleOAuthCallback();                 // ← 여기서 localStorage 토큰 저장
```

### 흐름

1. OAuth 로그인 성공 → 서버가 `/?token=...&userId=...&role=...`로 리다이렉트
   - `AuthController.java:39-41`
2. `init()`에서 `mountAllPartials()`가 `realestate` partial을 mount하고 `Alpine.initTree(host)` 호출
   - `partial-loader.js:72-73`
3. `realestate.html:3`의 `x-init="initRealEstate()"`가 **즉시 동기 실행**
   - `x-show`가 false여도 `x-init`은 mount 시점에 실행됨 (Alpine 특성)
4. `initRealEstate()` → `loadRealEstateFavorites()` → `API.getRealEstateFavoriteRegions()`
   - `realestate.js:290-296`, `realestate.js:241-249`
5. **이 시점에 `handleOAuthCallback()`(app.js:151)이 아직 실행되지 않아** `localStorage.accessToken`이 없음
   - `auth.js:22-36`
6. `api.js getHeaders()`가 Authorization 헤더를 붙이지 못함
   - `api.js:5-9`
7. Spring Security `anyRequest().authenticated()` → 401
   - `ProdSecurityConfig.java:59-88`, `RealEstateFavoriteRegionController.java:63-71`
8. `api.js:36-41` 401 핸들러 → 토큰 삭제 + `/login.html` 리다이렉트

### 왜 "처음 로그인 직후"에만 발생하나

- 토큰이 URL 쿼리 파라미터로만 전달되고 `localStorage`에는 아직 없는 상태에서 x-init이 먼저 돈다.
- 이미 토큰이 `localStorage`에 있는 재방문 시에는 x-init 시점에 토큰이 존재해 정상 동작.

## 핵심 근거 (파일:라인)

| 위치 | 내용 |
|------|------|
| `app.js:125` | `mountAllPartials` await (partial x-init 실행 트리거) |
| `app.js:151` | `handleOAuthCallback()` — 토큰 저장 (mount 이후) |
| `partial-loader.js:72-73` | `Alpine.initTree(host)` → x-init 실행 |
| `realestate.html:3` | `x-init="initRealEstate()"` |
| `realestate.js:290-296` | `initRealEstate()` → `loadRealEstateFavorites()` |
| `api.js:5-9` | `getHeaders()` — 토큰 없으면 Authorization 미포함 |
| `api.js:36-41` | 401 시 토큰 삭제 + `/login.html` 리다이렉트 |
| `RealEstateFavoriteRegionController.java:63-71` | 인증 없으면 401 |

## 참고: 다른 realestate API는 왜 멀쩡한가

- `GET /api/realestate/regions`는 `currentUserId()` 호출이 없어 인증 불필요 (`RealEstateRegionController.java:21-24`)
- favorites/regions만 user 식별이 필요해 인증 필수 → race condition에 노출

## 권장 수정 방향 (택1, 미적용 상태)

1. **순서 교정**: `handleOAuthCallback()`를 `mountAllPartials()` **이전**으로 이동
   → mount 시점에 토큰이 이미 저장되어 있음. 가장 근본적.
2. **x-init 가드**: `realestate.html`의 `x-init`을 `x-init="checkLoggedIn() && initRealEstate()"`로 변경
3. **lazy init**: x-init에서 인증 필요 API 직접 호출을 제거하고, `navigateTo('realestate')` 진입 시점에 로드하도록 이전 (다른 페이지 패턴과 일치)

> 1번이 다른 partial의 동일 잠재 버그까지 함께 해소하므로 우선 권장.