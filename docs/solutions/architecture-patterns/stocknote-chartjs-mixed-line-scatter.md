---
title: "Chart.js mixed dataset (line + scatter) — 시계열 위 기록점 오버레이 패턴"
category: architecture-patterns
date: 2026-04-25
tags:
  - chart-js
  - mixed-dataset
  - alpine-js
  - reactive-proxy
  - frontend-performance
  - scatter-overlay
module: stocknote, frontend
symptom: "일봉 라인 차트 위에 사용자 기록점(scatter) 을 오버레이하고, scatter 만 클릭 가능하게 + 클릭 시 raw 메타로 상세 패널을 열어야 함"
root_cause: "Chart.js mixed dataset 의 좌표축 통일, line 의 pointHitRadius 비활성, Alpine.js reactive proxy 와 Chart.js 내부 상태 충돌"
problem_type: pattern
---

# Chart.js mixed dataset (line + scatter) — 시계열 위 기록점 오버레이 패턴

## Problem

stocknote 종목 차트 탭 요구:
- 종목 일봉(line) 위에 사용자 기록점(scatter, UP/DOWN 색상·방향 구분) 오버레이
- 라인 자체는 클릭 비활성, **scatter 점만 클릭** → 우측 상세 드로워 오픈
- scatter 클릭 시 `noteId/verified/summary` 등 raw 메타에 즉시 접근
- Alpine.js reactive proxy 가 Chart 인스턴스를 감싸면 내부 상태 변경 시 오류

## Root Cause

1. **좌표축 통일 부재**: line dataset 은 `labels` index 기반, scatter 는 `{x, y}` 객체 기반 — x축 좌표 호환 처리 필요
2. **line click 간섭**: 기본 `pointHitRadius` 가 0 보다 크면 line 의 데이터 포인트가 클릭 이벤트를 흡수
3. **메타 손실**: scatter dataset 에 raw object 를 직접 넣어야 클릭 이벤트에서 메타 복원 가능 (`element.$context.raw` 대신 `chart.data.datasets[di].data[i]`)
4. **Alpine Proxy 충돌**: Chart.js 는 `chart.update()` 등에서 내부 객체를 직접 변경 — Alpine v3 reactive proxy 가 감싸면 오류 (이미 ecos-timeseries-chart-visualization 에서 학습)

## Solution

### 1. mixed dataset 구성 — labels 기반 line + linear x축 scatter

line dataset 은 `labels` 인덱스로 위치, scatter 는 같은 인덱스를 `x` 로 매핑하면 좌표 통일.

```javascript
// stocknote.js renderStocknoteStockChart()
const labels = data.prices.map(p => p.date);
const priceMap = new Map(labels.map((d, i) => [d, i]));   // noteDate → index 변환표
const lineData = data.prices.map(p => p.close);

const upNotes = (data.notes || [])
    .filter(n => n.direction === 'UP' && n.priceAtNote != null)
    .map(n => ({
        x: priceMap.has(n.noteDate) ? priceMap.get(n.noteDate) : null,
        y: n.priceAtNote,
        noteId: n.noteId,           // ← 클릭 핸들러용 메타
        verified: n.verified,
        summary: n.summary
    }))
    .filter(p => p.x != null);
```

### 2. line 클릭 비활성 — pointRadius/pointHitRadius 모두 0

```javascript
{ type: 'line', label: '종가', data: lineData,
  borderColor: '#3b82f6', borderWidth: 1.5,
  tension: 0,
  pointRadius: 0,        // 점 자체를 그리지 않음
  pointHitRadius: 0 }    // ★ 이게 핵심 — 클릭 이벤트 hit-test 비활성
```

`pointRadius:0` 만 있으면 마우스 hover 시 여전히 hit 발생. `pointHitRadius:0` 까지 있어야 scatter 만 클릭 가능.

### 3. scatter dataset — pointStyle/rotation 으로 UP/DOWN 시각 구분

```javascript
{ type: 'scatter', label: '상승 기록', data: upNotes,
  backgroundColor: '#10b981', pointStyle: 'triangle', pointRadius: 9 },
{ type: 'scatter', label: '하락 기록', data: downNotes,
  backgroundColor: '#ef4444', pointStyle: 'triangle',
  rotation: 180,           // ★ 같은 triangle 을 180도 회전 = 역삼각형
  pointRadius: 9 }
```

### 4. onClick — datasets 배열에서 raw 메타 복원

```javascript
options: {
    onClick: (evt, elements, chart) => {
        if (!elements.length) return;
        const { datasetIndex, index } = elements[0];
        const raw = chart.data.datasets[datasetIndex].data[index];
        if (raw && typeof raw === 'object' && raw.noteId) {
            self.openStocknoteDetail(raw.noteId);
        }
    }
}
```

`element.$context.raw` 도 가능하지만 Chart.js 버전별 안정성 떨어짐. `chart.data.datasets[di].data[i]` 가 가장 견고.

### 5. Alpine Proxy 회피 — 모듈 스코프 Map 보관

```javascript
// 컴포넌트 파일 최상단
const _stocknoteChartRegistry = new Map();   // ★ Alpine 이 추적하지 않음

// 렌더 시
const existing = _stocknoteChartRegistry.get('stock');
if (existing) { try { existing.destroy(); } catch (_) {} _stocknoteChartRegistry.delete('stock'); }
const chart = new Chart(ctx, { ... });
_stocknoteChartRegistry.set('stock', chart);
```

기존 ecos 패턴(`Object.defineProperty(enumerable:false)`) 보다 단순. **컴포넌트 객체 안에 Chart 인스턴스를 넣지 않는다** 가 핵심.

### 6. 성능 옵션 — 대량 일봉 대비

```javascript
options: {
    animation: false,            // 매 렌더 애니메이션 제거
    responsive: true,
    maintainAspectRatio: false,
    scales: {
        x: { type: 'linear', display: false },   // x축 라벨은 tooltip 으로만
        y: { position: 'right' }
    }
}
```

## Adoption

- 구현체: `src/main/resources/static/js/components/stocknote.js` `renderStocknoteStockChart()`
- 백엔드 응답 스키마: `ChartDataResponse.PricePoint(date, close, ...)` + `NotePoint(noteId, noteDate, direction, priceAtNote, verified, summary, ...)`
- 일봉 데이터 누락 시: `data.prices.length === 0` 분기로 차트 미렌더 + UI 안내 문구

## References

- Chart.js mixed chart docs: https://www.chartjs.org/docs/latest/charts/mixed.html
- 사전 학습: [ecos-timeseries-chart-visualization.md](./ecos-timeseries-chart-visualization.md) — Alpine reactive proxy 회피 원조
- 도입 plan: `docs/plans/2026-04-23-001-feat-stock-note-plan.md` Phase 8, 심화 권고 19~22