/**
 * AuthComponent - 인증, OAuth 콜백, 프로필 관리
 *
 * 소유 프로퍼티: auth
 * 메서드: checkLoggedIn, handleOAuthCallback, loadMyProfile, logout
 */
const AuthComponent = {
    auth: {
        token: localStorage.getItem('accessToken'),
        userId: localStorage.getItem('userId'),
        role: localStorage.getItem('role'),
        displayName: localStorage.getItem('displayName'),
        isAdmin: false,
        // loadMyProfile 실패 시 true — UI 측에서 운영자 카드 silent 미노출 대신 안내 표시 가능.
        profileLoadFailed: false
    },

    checkLoggedIn() {
        return !!this.auth.token && !!this.auth.userId;
    },

    handleOAuthCallback() {
        const params = new URLSearchParams(window.location.search);
        const token = params.get('token');
        const userId = params.get('userId');
        const role = params.get('role');

        if (token && userId) {
            localStorage.setItem('accessToken', token);
            localStorage.setItem('userId', userId);
            if (role) localStorage.setItem('role', role);
            this.auth.token = token;
            this.auth.userId = userId;
            this.auth.role = role;
            window.history.replaceState({}, '', '/');
        }
    },

    async loadMyProfile() {
        try {
            const profile = await API.getMyProfile();
            this.auth.displayName = profile.displayName;
            this.auth.role = profile.role;
            this.auth.notificationEnabled = profile.notificationEnabled || false;
            this.auth.isAdmin = profile.admin === true;
            this.auth.profileLoadFailed = false;
            localStorage.setItem('displayName', profile.displayName);
            localStorage.setItem('role', profile.role);
        } catch (e) {
            // 실패 시 auth.isAdmin 은 false 로 유지되며 profileLoadFailed=true 로 UI 가 안내를 표시할 수 있게 함.
            this.auth.profileLoadFailed = true;
            console.warn('dashboard:auth:profile-load-failed (admin 분기 미초기화 가능)');
            console.error('프로필 로드 실패:', e);
        }
    },

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
    }
};
