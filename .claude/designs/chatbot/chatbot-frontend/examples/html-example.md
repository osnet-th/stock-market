# index.html 챗 버블 + 패널 HTML 예시

포트폴리오 섹션 (`x-show="currentPage === 'portfolio'"`) 내부 끝에 추가.

```html
<!-- 챗봇 버블 버튼 -->
<button @click="toggleChat()"
        x-show="currentPage === 'portfolio'"
        class="fixed bottom-6 right-6 w-14 h-14 bg-indigo-600 hover:bg-indigo-700 text-white rounded-full shadow-lg flex items-center justify-center z-40 transition-all duration-200">
    <svg x-show="!chat.isOpen" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
    </svg>
    <svg x-show="chat.isOpen" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
    </svg>
</button>

<!-- 챗 패널 -->
<div x-show="chat.isOpen && currentPage === 'portfolio'"
     x-transition:enter="transition ease-out duration-200"
     x-transition:enter-start="opacity-0 translate-y-4"
     x-transition:enter-end="opacity-100 translate-y-0"
     x-transition:leave="transition ease-in duration-150"
     x-transition:leave-start="opacity-100 translate-y-0"
     x-transition:leave-end="opacity-0 translate-y-4"
     x-cloak
     class="fixed bottom-24 right-6 w-96 h-[500px] bg-white rounded-xl shadow-xl border border-gray-200 flex flex-col z-50">

    <!-- 헤더 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <span class="font-semibold text-gray-800 text-sm">AI 어시스턴트</span>
        <button @click="toggleChat()" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
        </button>
    </div>

    <!-- 모드 토글 -->
    <div class="flex gap-2 px-4 py-2 border-b border-gray-100">
        <button @click="setChatMode('PORTFOLIO')"
                :class="chat.chatMode === 'PORTFOLIO'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                class="flex-1 text-xs font-medium py-1.5 rounded-lg transition-colors">
            포트폴리오 분석
        </button>
        <button @click="setChatMode('FINANCIAL')"
                :class="chat.chatMode === 'FINANCIAL'
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                class="flex-1 text-xs font-medium py-1.5 rounded-lg transition-colors">
            재무 분석
        </button>
    </div>

    <!-- 종목 선택 (FINANCIAL 모드만) -->
    <div x-show="chat.chatMode === 'FINANCIAL'" class="px-4 py-2 border-b border-gray-100">
        <template x-if="!chat.stockCode">
            <div class="relative">
                <input type="text"
                       x-model="chat.stockSearchQuery"
                       @input.debounce.300ms="searchChatStocks()"
                       placeholder="종목명 검색..."
                       class="w-full text-xs border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-1 focus:ring-indigo-500">
                <!-- 검색 결과 드롭다운 -->
                <div x-show="chat.stockSearchResults.length > 0"
                     @click.outside="chat.stockSearchResults = []"
                     class="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto z-10">
                    <template x-for="stock in chat.stockSearchResults" :key="stock.stockCode">
                        <button @click="selectChatStock(stock)"
                                class="w-full text-left px-3 py-2 text-xs hover:bg-gray-50 border-b border-gray-50 last:border-0">
                            <span x-text="stock.stockName" class="font-medium"></span>
                            <span x-text="stock.stockCode" class="text-gray-400 ml-1"></span>
                        </button>
                    </template>
                </div>
            </div>
        </template>
        <template x-if="chat.stockCode">
            <div class="flex items-center justify-between bg-indigo-50 rounded-lg px-3 py-1.5">
                <span class="text-xs font-medium text-indigo-700" x-text="chat.stockName + ' (' + chat.stockCode + ')'"></span>
                <button @click="clearChatStock()" class="text-indigo-400 hover:text-indigo-600">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
        </template>
    </div>

    <!-- 메시지 영역 -->
    <div id="chatMessages" class="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        <!-- 빈 상태 -->
        <template x-if="chat.messages.length === 0">
            <div class="flex flex-col items-center justify-center h-full text-gray-400">
                <svg class="w-10 h-10 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                          d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                </svg>
                <p class="text-xs" x-text="chat.chatMode === 'PORTFOLIO' ? '포트폴리오에 대해 질문해보세요' : '종목을 선택하고 재무 분석을 요청하세요'"></p>
            </div>
        </template>
        <!-- 메시지 -->
        <template x-for="(msg, idx) in chat.messages" :key="idx">
            <div :class="msg.role === 'user' ? 'flex justify-end' : 'flex justify-start'">
                <div :class="msg.role === 'user'
                         ? 'bg-indigo-600 text-white rounded-xl rounded-tr-sm'
                         : 'bg-gray-100 text-gray-800 rounded-xl rounded-tl-sm'"
                     class="max-w-[85%] px-3 py-2 text-xs leading-relaxed whitespace-pre-wrap"
                     x-text="msg.content">
                </div>
            </div>
        </template>
        <!-- 로딩 인디케이터 -->
        <div x-show="chat.isLoading && chat.messages.length > 0 && chat.messages[chat.messages.length-1].content === ''"
             class="flex justify-start">
            <div class="bg-gray-100 rounded-xl rounded-tl-sm px-3 py-2">
                <div class="flex gap-1">
                    <div class="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0ms"></div>
                    <div class="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 150ms"></div>
                    <div class="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 300ms"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- 입력 영역 -->
    <div class="px-4 py-3 border-t border-gray-100">
        <div class="flex gap-2">
            <input type="text"
                   x-model="chat.inputText"
                   @keydown.enter="sendChatMessage()"
                   :disabled="chat.isLoading"
                   :placeholder="chat.chatMode === 'FINANCIAL' && !chat.stockCode ? '먼저 종목을 선택해주세요' : '메시지를 입력하세요...'"
                   class="flex-1 text-xs border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-indigo-500 disabled:bg-gray-50 disabled:text-gray-400">
            <button @click="sendChatMessage()"
                    :disabled="chat.isLoading || !chat.inputText.trim() || (chat.chatMode === 'FINANCIAL' && !chat.stockCode)"
                    class="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white rounded-lg px-3 py-2 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
                </svg>
            </button>
        </div>
    </div>
</div>
```