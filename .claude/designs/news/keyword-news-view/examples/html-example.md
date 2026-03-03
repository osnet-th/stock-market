# index.html 키워드 뉴스 페이지 예시

```html
<!-- ========== NEWS ========== -->
<div x-show="currentPage === 'news'" x-cloak>
    <h2 class="text-xl font-bold text-gray-800 mb-6">키워드 뉴스</h2>

    <!-- 로그인 필요 안내 -->
    <template x-if="!checkLoggedIn()">
        <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-sm text-yellow-700">
            키워드 뉴스를 보려면 로그인이 필요합니다.
            <a href="/login.html" class="underline font-medium">로그인하기</a>
        </div>
    </template>

    <template x-if="checkLoggedIn()">
        <div>
            <!-- 키워드 칩 목록 -->
            <div class="flex gap-2 mb-6 overflow-x-auto pb-1 flex-wrap">
                <template x-if="news.keywords.length === 0">
                    <p class="text-sm text-gray-400">활성 키워드가 없습니다. 키워드 관리에서 등록해주세요.</p>
                </template>
                <template x-for="kw in news.keywords" :key="kw.id">
                    <button @click="selectNewsKeyword(kw.keyword)"
                        :class="news.selectedKeyword === kw.keyword
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                        class="px-3 py-1.5 rounded-lg text-xs font-medium transition whitespace-nowrap">
                        <span x-text="kw.keyword"></span>
                        <span :class="kw.region === 'DOMESTIC' ? 'text-blue-200' : 'text-purple-200'"
                            class="ml-1 text-xs"
                            x-text="kw.region === 'DOMESTIC' ? '(국내)' : '(해외)'"></span>
                    </button>
                </template>
            </div>

            <!-- 로딩 -->
            <template x-if="news.loading">
                <div class="flex justify-center py-12">
                    <div class="spinner"></div>
                </div>
            </template>

            <!-- 뉴스 목록 -->
            <template x-if="!news.loading && news.selectedKeyword">
                <div>
                    <!-- 결과 요약 -->
                    <p class="text-sm text-gray-500 mb-4">
                        '<span x-text="news.selectedKeyword" class="font-medium text-gray-700"></span>'
                        검색 결과 <span x-text="news.totalElements" class="font-medium"></span>건
                    </p>

                    <!-- 뉴스 카드 -->
                    <template x-if="news.list.length === 0">
                        <div class="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 text-sm">
                            수집된 뉴스가 없습니다.
                        </div>
                    </template>

                    <div class="space-y-3">
                        <template x-for="article in news.list" :key="article.id">
                            <a :href="article.originalUrl" target="_blank" rel="noopener noreferrer"
                                class="block bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition cursor-pointer">
                                <h3 class="text-sm font-semibold text-gray-800 mb-1 line-clamp-2" x-text="article.title"></h3>
                                <p class="text-xs text-gray-500 line-clamp-2 mb-2" x-text="article.content"></p>
                                <div class="flex items-center gap-3 text-xs text-gray-400">
                                    <span x-text="article.publishedAt ? new Date(article.publishedAt).toLocaleDateString('ko-KR') : '-'"></span>
                                    <span class="text-blue-500 hover:underline">원문 보기</span>
                                </div>
                            </a>
                        </template>
                    </div>

                    <!-- 페이지 네비게이션 -->
                    <template x-if="news.totalPages > 1">
                        <div class="flex justify-center items-center gap-2 mt-6">
                            <button @click="loadNews(news.page - 1)"
                                :disabled="news.page === 0"
                                :class="news.page === 0 ? 'text-gray-300 cursor-not-allowed' : 'text-gray-600 hover:text-blue-600'"
                                class="px-3 py-1.5 text-sm font-medium transition">
                                이전
                            </button>
                            <template x-for="p in news.totalPages" :key="p">
                                <button @click="loadNews(p - 1)"
                                    x-show="Math.abs((p - 1) - news.page) < 3 || p === 1 || p === news.totalPages"
                                    :class="(p - 1) === news.page
                                        ? 'bg-blue-600 text-white'
                                        : 'text-gray-600 hover:bg-gray-100'"
                                    class="w-8 h-8 rounded text-sm font-medium transition"
                                    x-text="p">
                                </button>
                            </template>
                            <button @click="loadNews(news.page + 1)"
                                :disabled="news.page >= news.totalPages - 1"
                                :class="news.page >= news.totalPages - 1 ? 'text-gray-300 cursor-not-allowed' : 'text-gray-600 hover:text-blue-600'"
                                class="px-3 py-1.5 text-sm font-medium transition">
                                다음
                            </button>
                        </div>
                    </template>
                </div>
            </template>
        </div>
    </template>
</div>
```