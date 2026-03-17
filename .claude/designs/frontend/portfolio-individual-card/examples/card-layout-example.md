# 종목별 개별 카드 레이아웃 예시

## 변경 전 (index.html:686~688)

```html
<!-- 섹션 내용 -->
<div x-show="portfolio.expandedSections[alloc.assetType]" class="border-t border-gray-100">
    <template x-for="item in getItemsByType(alloc.assetType)" :key="item.id">
        <div>
            <div @click="..."
                class="px-5 py-3 border-b border-gray-50 last:border-b-0 flex items-center justify-between transition"
                ...>
```

## 변경 후

```html
<!-- 섹션 내용 -->
<div x-show="portfolio.expandedSections[alloc.assetType]" class="p-3 space-y-2 border-t border-gray-100">
    <template x-for="item in getItemsByType(alloc.assetType)" :key="item.id">
        <div class="bg-white rounded-lg border border-gray-200 overflow-hidden shadow-sm">
            <div @click="..."
                class="px-4 py-3 flex items-center justify-between transition"
                ...>
```

### 핵심 변경사항
1. 섹션 컨테이너: `border-t border-gray-100` → `p-3 space-y-2 border-t border-gray-100` (패딩과 카드 간격 추가)
2. 종목 래퍼 `<div>`: `<div>` → `<div class="bg-white rounded-lg border border-gray-200 overflow-hidden shadow-sm">` (개별 카드 스타일)
3. 종목 내용 `<div>`: `px-5 py-3 border-b border-gray-50 last:border-b-0` → `px-4 py-3` (행 구분선 제거)