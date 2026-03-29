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
        displayName: localStorage.getItem('displayName')
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
            localStorage.setItem('displayName', profile.displayName);
            localStorage.setItem('role', profile.role);
        } catch (e) {
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
