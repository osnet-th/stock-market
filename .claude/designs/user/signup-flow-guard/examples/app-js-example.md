# app.js 수정 예시

## init() 수정

```javascript
// 변경 전
async init() {
    this.handleOAuthCallback();

    if (!this.checkLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    await this.loadMyProfile();
    await this.loadHomeSummary();
},

// 변경 후
async init() {
    this.handleOAuthCallback();

    if (!this.checkLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    await this.loadMyProfile();

    // SIGNING_USER면 회원가입 페이지로 리다이렉트
    if (this.auth.role === 'SIGNING_USER') {
        window.location.href = '/signup.html';
        return;
    }

    await this.loadHomeSummary();
},
```

## loadMyProfile() 수정

```javascript
// 변경 전
async loadMyProfile() {
    try {
        const profile = await API.getMyProfile();
        this.auth.displayName = profile.displayName;
        localStorage.setItem('displayName', profile.displayName);
    } catch (e) {
        console.error('프로필 로드 실패:', e);
    }
},

// 변경 후
async loadMyProfile() {
    try {
        const profile = await API.getMyProfile();
        this.auth.displayName = profile.displayName;
        this.auth.role = profile.role;
        localStorage.setItem('displayName', profile.displayName);
        localStorage.setItem('role', profile.role);
    } catch (e) {
        console.error('프로필 로드 실패:', e);
    }
},
```

## 변경 요약

- `loadMyProfile()`에서 서버 응답의 `role`을 localStorage와 auth 객체에 동기화
- `init()`에서 프로필 로드 후 role이 `SIGNING_USER`이면 `signup.html`로 리다이렉트
- role 체크를 서버 응답 기준으로 함 (URL 파라미터보다 신뢰성 높음)