# signup.html submitSignup() 수정 예시

```javascript
// 변경 전
async submitSignup() {
    // ...validation...
    this.submitting = true;
    try {
        const userId = parseInt(localStorage.getItem('userId'));
        await API.signup(userId, this.form.name.trim(), this.form.nickname.trim(), this.form.phoneNumber.trim());

        localStorage.setItem('role', 'USER');
        window.location.href = '/';
    } catch (e) {
        this.errorMessage = e.message || '회원가입에 실패했습니다.';
    } finally {
        this.submitting = false;
    }
}

// 변경 후
async submitSignup() {
    // ...validation...
    this.submitting = true;
    try {
        const userId = parseInt(localStorage.getItem('userId'));
        const result = await API.signup(userId, this.form.name.trim(), this.form.nickname.trim(), this.form.phoneNumber.trim());

        // 새 토큰으로 교체
        localStorage.setItem('accessToken', result.accessToken);
        localStorage.setItem('role', 'USER');
        window.location.href = '/';
    } catch (e) {
        this.errorMessage = e.message || '회원가입에 실패했습니다.';
    } finally {
        this.submitting = false;
    }
}
```