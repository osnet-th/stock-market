# 바 차트 구현 예시

## index.html - 바 차트 캔버스 (결과 영역 내, 요약 카드 위에 배치)

```html
<!-- 바 차트 (재무계정 탭에서만 표시) -->
<template x-if="portfolio.financialResult.length > 0 && portfolio.selectedFinancialMenu === 'accounts'">
    <div class="mb-4 bg-white rounded-lg border border-gray-200 p-4">
        <h4 class="text-xs font-semibold text-gray-500 mb-3">주요 재무 추세 (당기 / 전기 / 전전기)</h4>
        <div style="height: 250px;">
            <canvas id="financialBarChart"></canvas>
        </div>
    </div>
</template>
```

## app.js - 바 차트 렌더링 메서드

```javascript
// portfolio 상태에 추가
// financialChartInstance: null,

renderFinancialBarChart() {
    var canvas = document.getElementById('financialBarChart');
    if (!canvas) return;

    // 기존 차트 삭제
    if (this.portfolio.financialChartInstance) {
        this.portfolio.financialChartInstance.destroy();
        this.portfolio.financialChartInstance = null;
    }

    var result = this.portfolio.financialResult;
    var config = this.financialSummaryConfig.accounts;
    if (!result || !config) return;

    // 주요 계정 데이터 추출
    var labels = [];
    var currentData = [];
    var previousData = [];
    var beforePreviousData = [];

    for (var i = 0; i < config.length; i++) {
        var cfg = config[i];
        for (var j = 0; j < result.length; j++) {
            var row = result[j];
            var name = row.accountName || '';
            if (name.indexOf(cfg.match) !== -1) {
                labels.push(cfg.label);
                currentData.push(this.parseAmount(row.currentTermAmount));
                previousData.push(this.parseAmount(row.previousTermAmount));
                beforePreviousData.push(this.parseAmount(row.beforePreviousTermAmount));
                break;
            }
        }
    }

    if (labels.length === 0) return;

    var self = this;
    this.portfolio.financialChartInstance = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: '당기',
                    data: currentData,
                    backgroundColor: '#3B82F6',
                    borderRadius: 4
                },
                {
                    label: '전기',
                    data: previousData,
                    backgroundColor: '#93C5FD',
                    borderRadius: 4
                },
                {
                    label: '전전기',
                    data: beforePreviousData,
                    backgroundColor: '#DBEAFE',
                    borderRadius: 4
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'rect',
                        font: { size: 11 }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return context.dataset.label + ': ' + Format.compactNumber(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return Format.compactNumber(value);
                        },
                        font: { size: 11 }
                    },
                    grid: { color: '#F3F4F6' }
                },
                x: {
                    ticks: { font: { size: 11 } },
                    grid: { display: false }
                }
            }
        }
    });
},

// 금액 문자열 → 숫자 변환 헬퍼
parseAmount(value) {
    if (!value) return 0;
    var num = parseFloat(String(value).replace(/,/g, ''));
    return isNaN(num) ? 0 : num;
}
```

## app.js - loadSelectedFinancial에서 차트 렌더링 호출

```javascript
// loadSelectedFinancial() 의 finally 블록 이후에 차트 렌더링 추가
// accounts 메뉴일 때만 바 차트 렌더링

async loadSelectedFinancial() {
    // ... 기존 코드 ...

    this.portfolio.financialResult = result || [];

    // 바 차트 렌더링 (accounts 탭일 때만)
    if (menu === 'accounts' && this.portfolio.financialResult.length > 0) {
        var self = this;
        this.$nextTick(function() {
            self.renderFinancialBarChart();
        });
    }

    // ... 기존 코드 ...
}
```

### 색상 선택 이유

- 당기(진한 파랑 `#3B82F6`): 가장 중요한 최신 데이터 → 진한 색으로 강조
- 전기(중간 파랑 `#93C5FD`): 비교 데이터 → 중간 톤
- 전전기(연한 파랑 `#DBEAFE`): 참고 데이터 → 연한 톤
- 동일 색상 계열로 시간 흐름의 연속성 표현