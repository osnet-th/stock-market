# api.js 추가 예시

```javascript
// News
getNewsByKeyword(keyword, page = 0, size = 20) {
    return this.request('GET', `/api/news?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`);
},
```