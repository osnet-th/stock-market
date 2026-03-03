# Frontend 예시 코드

## api.js

```javascript
// News Collect
collectNewsByKeyword(keyword, userId, region) {
    return this.request('POST', '/api/news/collect', { keyword, userId, region });
},
```

## app.js 상태 추가

```javascript
news: {
    selectedKeyword: null,
    collectingKeywordId: null,  // 수집 중인 키워드 ID (중복 클릭 방지)
    list: [],
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0,
    loading: false
},
```

## app.js 메서드 추가

```javascript
async collectNews(kw) {
    if (this.news.collectingKeywordId) return;
    this.news.collectingKeywordId = kw.id;
    try {
        var result = await API.collectNewsByKeyword(kw.keyword, this.auth.userId, kw.region);
        var msg = '수집 완료: ' + result.successCount + '건 저장';
        if (result.ignoredCount > 0) msg += ', ' + result.ignoredCount + '건 중복';
        alert(msg);

        // 수집한 키워드가 현재 선택된 키워드면 뉴스 목록 갱신
        if (this.news.selectedKeyword === kw.keyword) {
            await this.loadNews(0);
        }
    } catch (e) {
        console.error('뉴스 수집 실패:', e);
        alert('뉴스 수집에 실패했습니다.');
    } finally {
        this.news.collectingKeywordId = null;
    }
},
```

## index.html 키워드 항목 버튼 영역

```html
<div class="flex gap-2">
    <!-- 수집 버튼 (활성 키워드만) -->
    <template x-if="kw.active">
        <button @click.stop="collectNews(kw)"
            :disabled="news.collectingKeywordId === kw.id"
            :class="news.collectingKeywordId === kw.id
                ? 'text-gray-400 cursor-not-allowed'
                : 'text-blue-600 hover:text-blue-800'"
            class="text-sm font-medium transition"
            x-text="news.collectingKeywordId === kw.id ? '수집 중...' : '수집'">
        </button>
    </template>
    <!-- 기존 활성화/비활성화, 삭제 버튼 유지 -->
    <button @click.stop="toggleKeyword(kw)" ...>...</button>
    <button @click.stop="removeKeyword(kw.id)" ...>삭제</button>
</div>
```