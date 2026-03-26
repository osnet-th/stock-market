# app.js chat 상태 및 메서드 예시

```javascript
// dashboard() 함수 반환 객체에 추가

// === 상태 (기존 상태 객체에 추가) ===
chat: {
    isOpen: false,
    chatMode: 'PORTFOLIO',
    stockCode: null,
    stockName: null,
    stockSearchQuery: '',
    stockSearchResults: [],
    messages: [],
    inputText: '',
    isLoading: false
},

// === 메서드 (기존 메서드들 아래에 추가) ===

toggleChat() {
    this.chat.isOpen = !this.chat.isOpen;
},

setChatMode(mode) {
    this.chat.chatMode = mode;
    this.chat.stockCode = null;
    this.chat.stockName = null;
    this.chat.stockSearchQuery = '';
    this.chat.stockSearchResults = [];
},

async searchChatStocks() {
    if (this.chat.stockSearchQuery.length < 1) {
        this.chat.stockSearchResults = [];
        return;
    }
    try {
        const results = await API.searchStocks(this.chat.stockSearchQuery);
        this.chat.stockSearchResults = results || [];
    } catch (e) {
        this.chat.stockSearchResults = [];
    }
},

selectChatStock(stock) {
    this.chat.stockCode = stock.stockCode;
    this.chat.stockName = stock.stockName;
    this.chat.stockSearchQuery = '';
    this.chat.stockSearchResults = [];
},

clearChatStock() {
    this.chat.stockCode = null;
    this.chat.stockName = null;
},

async sendChatMessage() {
    const text = this.chat.inputText.trim();
    if (!text || this.chat.isLoading) return;

    if (this.chat.chatMode === 'FINANCIAL' && !this.chat.stockCode) {
        return;
    }

    this.chat.messages.push({ role: 'user', content: text });
    this.chat.inputText = '';
    this.chat.isLoading = true;

    this.chat.messages.push({ role: 'assistant', content: '' });
    const assistantIdx = this.chat.messages.length - 1;

    this.$nextTick(() => this.scrollChatToBottom());

    await API.streamChat(
        this.auth.userId,
        text,
        this.chat.chatMode,
        this.chat.stockCode,
        (chunk) => {
            this.chat.messages[assistantIdx].content += chunk;
            this.$nextTick(() => this.scrollChatToBottom());
        },
        () => {
            this.chat.isLoading = false;
        },
        (error) => {
            this.chat.messages[assistantIdx].content += '\n\n(오류가 발생했습니다. 다시 시도해주세요.)';
            this.chat.isLoading = false;
        }
    );
},

scrollChatToBottom() {
    const el = document.getElementById('chatMessages');
    if (el) el.scrollTop = el.scrollHeight;
}
```