# 템플릿 레이아웃 예시

## index.html — STOCK 섹션 내부 구조

기존의 단일 `x-for` 루프를 STOCK 타입일 때 국내/해외 두 블록으로 분기:

```html
<!-- 섹션 내용 -->
<div x-show="portfolio.expandedSections[alloc.assetType]" class="p-3 space-y-2 border-t border-gray-100">

    <!-- STOCK이 아닌 경우: 기존 그대로 -->
    <template x-if="alloc.assetType !== 'STOCK'">
        <template x-for="item in getItemsByType(alloc.assetType)" :key="item.id">
            <!-- 기존 카드 그대로 -->
        </template>
    </template>

    <!-- STOCK인 경우: 국내/해외 서브그룹 -->
    <template x-if="alloc.assetType === 'STOCK'">
        <div class="space-y-2">
            <!-- 국내 주식 -->
            <template x-if="getDomesticStocks().length > 0">
                <div class="space-y-2">
                    <div class="text-xs font-medium text-gray-500 px-1">🇰🇷 국내 주식</div>
                    <template x-for="item in getDomesticStocks()" :key="item.id">
                        <!-- 기존 카드 그대로 -->
                    </template>
                </div>
            </template>

            <!-- 해외 주식 -->
            <template x-if="getOverseasStocks().length > 0">
                <div class="space-y-2">
                    <div class="text-xs font-medium text-gray-500 px-1">🌐 해외 주식</div>
                    <template x-for="item in getOverseasStocks()" :key="item.id">
                        <!-- 기존 카드 그대로 -->
                    </template>
                </div>
            </template>
        </div>
    </template>
</div>
```

### 서브 헤더 스타일
- `text-xs font-medium text-gray-500 px-1` — 작고 눈에 띄지 않는 라벨
- 해당 그룹에 항목이 없으면 `x-if`로 숨김