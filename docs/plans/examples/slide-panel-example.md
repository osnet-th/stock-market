# 슬라이드 패널 HTML 구조 예시

## index.html - 슬라이드 패널 (포트폴리오 섹션 바깥, body 직전)

```html
<!-- 우측 슬라이드 패널 오버레이 -->
<div x-show="portfolio.selectedStockItem"
     x-transition:enter="transition ease-out duration-300"
     x-transition:enter-start="opacity-0"
     x-transition:enter-end="opacity-100"
     x-transition:leave="transition ease-in duration-200"
     x-transition:leave-start="opacity-100"
     x-transition:leave-end="opacity-0"
     class="fixed inset-0 bg-black bg-opacity-30 z-40"
     @click="closeStockDetail()">
</div>

<!-- 우측 슬라이드 패널 본체 -->
<div x-show="portfolio.selectedStockItem"
     x-transition:enter="transition ease-out duration-300"
     x-transition:enter-start="translate-x-full"
     x-transition:enter-end="translate-x-0"
     x-transition:leave="transition ease-in duration-200"
     x-transition:leave-start="translate-x-0"
     x-transition:leave-end="translate-x-full"
     class="fixed top-0 right-0 h-full w-[65%] bg-white shadow-2xl z-50 overflow-y-auto"
     @click.stop>

    <!-- 패널 헤더 -->
    <div class="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 z-10">
        <div class="flex justify-between items-center">
            <h3 class="text-lg font-bold text-gray-800">
                <span x-text="portfolio.selectedStockItem?.itemName"></span>
                <span class="text-sm text-gray-500 ml-1"
                      x-text="'(' + (portfolio.selectedStockItem?.stockDetail?.stockCode || '') + ')'"></span>
            </h3>
            <button @click="closeStockDetail()"
                    class="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>

        <!-- 보유 정보 -->
        <div class="flex gap-4 text-sm text-gray-600 mt-2">
            <span>수량: <span class="font-semibold" x-text="Format.number(portfolio.selectedStockItem?.stockDetail?.quantity)"></span>주</span>
            <span>평단가: <span class="font-semibold" x-text="'&#8361;' + Format.number(portfolio.selectedStockItem?.stockDetail?.avgBuyPrice)"></span></span>
            <span>투자금: <span class="font-semibold" x-text="'&#8361;' + Format.number(portfolio.selectedStockItem?.investedAmount)"></span></span>
        </div>
    </div>

    <!-- 패널 콘텐츠 (기존 재무 상세 패널의 메뉴/필터/결과 영역 이동) -->
    <div class="px-6 py-4">
        <!-- 여기에 기존 메뉴 버튼, 필터 드롭다운, 결과 영역을 이동 -->
    </div>
</div>
```

## app.js - Alpine.js 상태 (변경 없음, 기존 활용)

```javascript
// portfolio.selectedStockItem 을 그대로 슬라이드 패널의 open/close 상태로 사용
// null이면 닫힘, 값이 있으면 열림
// 추가 상태 불필요
```