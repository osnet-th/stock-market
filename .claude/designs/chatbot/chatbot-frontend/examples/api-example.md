# api.js SSE 스트리밍 예시

```javascript
// api.js 맨 하단, 기존 메서드들 아래에 추가

async streamChat(userId, message, chatMode, stockCode, onChunk, onDone, onError) {
    try {
        const response = await fetch(`${this.baseUrl}/api/chat?userId=${userId}`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ message, chatMode, stockCode })
        });

        if (response.status === 401) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userId');
            window.location.href = '/login.html';
            return;
        }

        if (!response.ok) {
            const errorText = await response.text();
            onError(new Error(`API Error ${response.status}: ${errorText}`));
            return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop();

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const text = line.substring(5);
                    if (text.trim()) {
                        onChunk(text);
                    }
                }
            }
        }

        onDone();
    } catch (error) {
        onError(error);
    }
}
```