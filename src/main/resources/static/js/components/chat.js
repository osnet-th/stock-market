/**
 * ChatComponent - 챗봇 SSE 스트리밍, 드래그, 마크다운 렌더링
 * 소유 프로퍼티: chat
 */
const ChatComponent = {
    chat: {
        isOpen: false,
        expanded: false,
        chatMode: 'PORTFOLIO',
        stockCode: null,
        stockName: null,
        stockSearchQuery: '',
        stockSearchResults: [],
        indicatorCategory: null,
        messages: [],
        inputText: '',
        isLoading: false,
        dragging: false,
        dragPos: { x: null, y: null },
        dragOffset: { x: 0, y: 0 },
        _abortController: null,
        analysisTaskLabels: {
            UNDERVALUATION: '저평가/고평가 판단',
            TREND_SUMMARY: '실적 추세 요약',
            RISK_DIAGNOSIS: '리스크 요인 진단',
            INVESTMENT_OPINION: '투자 적정성 의견'
        }
    },

    toggleChat() {
        this.chat.isOpen = !this.chat.isOpen;
        if (this.chat.isOpen) {
            // 현재 페이지에 맞는 모드로 초기화
            const modeMap = { ecos: 'ECONOMIC', portfolio: 'PORTFOLIO' };
            const targetMode = modeMap[this.currentPage] || 'PORTFOLIO';

            if (this.chat._abortController) {
                this.chat._abortController.abort();
            }
            this.chat.chatMode = targetMode;
            this.chat.messages = [];
            this.chat.inputText = '';
            this.chat.isLoading = false;
            this.chat.stockCode = null;
            this.chat.stockName = null;
            this.chat.stockSearchQuery = '';
            this.chat.stockSearchResults = [];
            this.chat.indicatorCategory = targetMode === 'ECONOMIC' ? this.ecos?.selectedCategory : null;
            this.chat.dragPos = { x: null, y: null };
        } else if (this.chat._cleanupDrag) {
            this.chat._cleanupDrag();
        }
    },

    chatPanelClasses() {
        const base = 'bg-white shadow-xl border border-gray-200 flex flex-col transition-all duration-200';
        if (this.isMobile) {
            return `fixed inset-0 w-full h-dvh rounded-none z-[60] ${base}`;
        }
        const size = this.chat.expanded ? 'w-[600px] h-[700px]' : 'w-96 h-[500px]';
        const position = this.chat.dragPos.x !== null ? 'fixed' : 'fixed bottom-24 right-6';
        return `${size} ${position} rounded-xl z-50 ${base}`;
    },

    setChatMode(mode) {
        this.chat.chatMode = mode;
        this.chat.stockCode = null;
        this.chat.stockName = null;
        this.chat.stockSearchQuery = '';
        this.chat.stockSearchResults = [];
        this.chat.indicatorCategory = null;
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

    canRequestAnalysis() {
        return this.chat.chatMode === 'FINANCIAL'
            && !!this.chat.stockCode
            && !this.chat.isLoading;
    },

    requestAnalysis(task) {
        if (!this.canRequestAnalysis()) return;
        this.sendChatMessage({ message: '', analysisTask: task });
    },

    collectChatHistory() {
        const MAX_HISTORY_MESSAGES = 20; // 10턴 = 20개 메시지
        const history = this.chat.messages
            .filter(m => m.content && m.content.trim())
            .map(m => ({ role: m.role === 'assistant' ? 'model' : m.role, content: m.content }));
        // 마지막 빈 assistant 메시지 제외 (아직 스트리밍 중인 경우)
        if (history.length > MAX_HISTORY_MESSAGES) {
            return history.slice(history.length - MAX_HISTORY_MESSAGES);
        }
        return history;
    },

    async sendChatMessage(options = {}) {
        if (this.chat.isLoading) return;

        const analysisTask = options.analysisTask || null;
        const explicitMessage = Object.prototype.hasOwnProperty.call(options, 'message');
        const rawText = explicitMessage ? (options.message || '') : this.chat.inputText.trim();

        if (this.chat.chatMode === 'FINANCIAL') {
            if (!this.chat.stockCode) return;
            if (!analysisTask) return;
        } else {
            if (!rawText) return;
            if (this.chat.chatMode === 'ECONOMIC' && !this.chat.indicatorCategory) return;
        }

        if (this.chat._abortController) {
            this.chat._abortController.abort();
        }
        const abortController = new AbortController();
        this.chat._abortController = abortController;

        const history = this.collectChatHistory();

        const displayText = analysisTask
            ? `[분석 요청] ${this.chat.analysisTaskLabels[analysisTask] || analysisTask}`
            : rawText;
        this.chat.messages.push({ role: 'user', content: displayText });

        if (!explicitMessage) {
            this.chat.inputText = '';
        }
        this.chat.isLoading = true;

        this.chat.messages.push({ role: 'assistant', content: '' });
        const assistantIdx = this.chat.messages.length - 1;

        this.$nextTick(() => this.scrollChatToBottom());

        await API.streamChat(
            this.auth.userId,
            rawText,
            this.chat.chatMode,
            this.chat.stockCode,
            this.chat.indicatorCategory,
            analysisTask,
            history,
            (chunk) => {
                if (abortController.signal.aborted) return;
                this.chat.messages[assistantIdx].content += chunk;
                this.$nextTick(() => this.scrollChatToBottom());
            },
            () => {
                if (abortController.signal.aborted) return;
                this.chat.isLoading = false;
            },
            (error) => {
                if (abortController.signal.aborted) return;
                this.chat.messages[assistantIdx].content += '\n\n(오류가 발생했습니다. 다시 시도해주세요.)';
                this.chat.isLoading = false;
            },
            abortController.signal
        );
    },

    scrollChatToBottom() {
        const el = document.getElementById('chatMessages');
        if (el) el.scrollTop = el.scrollHeight;
    },

    renderMarkdown(text) {
        if (!text) return '';
        if (typeof marked !== 'undefined') {
            return marked.parse(text, { breaks: true });
        }
        return text.replace(/\n/g, '<br>');
    },

    startChatDrag(e) {
        if (e.target.closest('button')) return;
        const panel = e.currentTarget.closest('[x-show]');
        const rect = panel.getBoundingClientRect();
        this.chat.dragging = true;
        this.chat.dragOffset = { x: e.clientX - rect.left, y: e.clientY - rect.top };
        if (this.chat.dragPos.x === null) {
            this.chat.dragPos = { x: rect.left, y: rect.top };
        }

        const onMove = (ev) => {
            if (!this.chat.dragging) return;
            this.chat.dragPos = {
                x: ev.clientX - this.chat.dragOffset.x,
                y: ev.clientY - this.chat.dragOffset.y
            };
        };
        const cleanup = () => {
            this.chat.dragging = false;
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            this.chat._cleanupDrag = null;
        };
        const onUp = () => cleanup();
        this.chat._cleanupDrag = cleanup;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    }
};
