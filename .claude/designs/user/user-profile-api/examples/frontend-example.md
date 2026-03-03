# 프론트엔드 구현 예시

## api.js - getMyProfile 메서드 추가

```javascript
// Users
getMyProfile() {
    return this.request('GET', '/api/users/me');
},
```

## app.js - auth 객체 및 init 변경

```javascript
// auth 객체에 displayName 추가
auth: {
    token: localStorage.getItem('accessToken'),
    userId: localStorage.getItem('userId'),
    role: localStorage.getItem('role'),
    displayName: localStorage.getItem('displayName')
},

// init()에서 프로필 조회 추가
async init() {
    this.handleOAuthCallback();

    if (!this.checkLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    await this.loadMyProfile();
    await this.loadHomeSummary();
},

// 프로필 조회 메서드
async loadMyProfile() {
    try {
        const profile = await API.getMyProfile();
        this.auth.displayName = profile.displayName;
        localStorage.setItem('displayName', profile.displayName);
    } catch (e) {
        console.error('프로필 로드 실패:', e);
    }
},

// logout()에 displayName 제거 추가
logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('role');
    localStorage.removeItem('displayName');
    this.auth.token = null;
    this.auth.userId = null;
    this.auth.role = null;
    this.auth.displayName = null;
    window.location.href = '/login.html';
},
```

## index.html - 헤더 표시 변경

```html
<span class="text-sm text-gray-600">
    <span x-text="auth.displayName || ('ID: ' + auth.userId)" class="font-medium"></span>
</span>
```