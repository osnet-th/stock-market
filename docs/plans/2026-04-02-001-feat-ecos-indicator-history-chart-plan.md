---
title: "feat: ECOS 경제지표 cycle별 히스토리 차트 시각화"
type: feat
status: completed
date: 2026-04-02
origin: docs/brainstorms/2026-04-02-ecos-indicator-chart-brainstorm.md
deepened: 2026-04-02
---

# feat: ECOS 경제지표 cycle별 히스토리 차트 시각화

## Enhancement Summary

**Deepened on:** 2026-04-02
**Sections enhanced:** 6
**Research agents used:** JPA Query Pattern, Chart.js Multi-Instance, Performance Oracle, Architecture Strategist, Context7 Chart.js Docs

### Key Improvements
1. **DB 쿼리 최적화**: ROW_NUMBER() 윈도우 함수로 DB 레벨 deduplicate + 복합 인덱스 추가
2. **Chart.js 성능**: animation: false, events: [], pointRadius: 0, Map 기반 인스턴스 레지스트리
3. **아키텍처 정합성**: 기존 레이어 패턴(port-adapter, CQS) 완전 준수, presentation DTO로 응답 구조화

### New Considerations Discovered
- Alpine.js reactive proxy가 Chart.js 내부 객체를 감싸면 충돌 발생 → `_chartInstances`를 non-enumerable로 관리
- 동일 cycle에 동일 snapshotDate 중복 가능 → ROW_NUMBER()로 정확히 1행 보장
- 프론트엔드 캐시는 LRU 2-3개 카테고리로 제한하여 메모리 관리

---

## Overview

ECOS 경제지표 페이지에 **표/그래프 뷰 토글**(라디오 버튼)을 추가하고, 그래프 뷰에서는 선택된 카테고리 내 각 지표의 **cycle별 추이를 개별 라인 차트(Chart.js)**로 보여준다.

현재는 `ecos_indicator_latest` 테이블의 현재값+이전값만 테이블로 표시하고 있으나, `ecos_indicator` 테이블에 이미 축적된 히스토리 데이터를 활용하여 시간에 따른 변화 추이를 시각적으로 파악할 수 있게 한다.

## Problem Statement / Motivation

- `ecos_indicator` 테이블에 cycle별 히스토리 데이터가 배치 스케줄러에 의해 꾸준히 축적되고 있으나, 이를 조회하는 API와 시각화 UI가 없음
- 숫자 테이블만으로는 지표의 추세(상승/하락/횡보)를 직관적으로 파악하기 어려움
- Chart.js가 이미 프로젝트에 포함되어 있어 추가 의존성 없이 구현 가능

## Proposed Solution

### 백엔드

카테고리별 히스토리를 일괄 조회하는 API 엔드포인트 1개 추가:

```
GET /api/economics/indicators/history?category={CATEGORY}
```

**응답 구조:**

```json
[
  {
    "keystatName": "한국은행 기준금리",
    "className": "시장금리",
    "unitName": "%",
    "history": [
      {"cycle": "2024.01", "dataValue": "3.50"},
      {"cycle": "2024.02", "dataValue": "3.50"},
      {"cycle": "2024.03", "dataValue": "3.25"}
    ]
  }
]
```

- 동일 cycle에 대해 여러 snapshotDate 레코드가 있을 수 있으므로, **DB 레벨에서 ROW_NUMBER() 윈도우 함수로 deduplicate** (그룹당 정확히 1행 보장)
- 백엔드에서 `ORDER BY cycle ASC` 정렬하여 반환 (동일 지표 내에서는 cycle 형식이 통일되어 있으므로 문자열 정렬로 충분)

### Research Insights — 백엔드 쿼리

**Native SQL + ROW_NUMBER() 패턴 (권장):**

JPQL은 FROM 절 서브쿼리를 지원하지 않으므로, `@Query(nativeQuery = true)`로 구현:

```sql
SELECT t.*
FROM (
    SELECT e.*,
           ROW_NUMBER() OVER (
               PARTITION BY e.class_name, e.keystat_name, e.cycle
               ORDER BY e.snapshot_date DESC
           ) AS rn
    FROM ecos_indicator e
    WHERE e.class_name IN (:classNames)
) t
WHERE t.rn = 1
ORDER BY t.class_name, t.keystat_name, t.cycle
```

**왜 ROW_NUMBER()인가:**
- Self-join + MAX(snapshot_date) 방식은 동일 MAX date에 여러 행이 있으면 중복 반환 가능
- ROW_NUMBER()는 그룹당 정확히 1행을 보장
- MariaDB 10.2+ 지원 (프로젝트 MariaDB 버전 호환)

**필수 복합 인덱스:**

```sql
-- Flyway migration
CREATE INDEX idx_ecos_group_snapshot
    ON ecos_indicator (class_name, keystat_name, cycle, snapshot_date DESC);
```

이 인덱스로 PARTITION BY + ORDER BY를 인덱스 스캔으로 처리. 기존 `idx_ecos_classname_keystat`는 이 인덱스에 포함되므로 유지하되, 향후 정리 가능.

**스케일 전망:**

| 기간 | 예상 행 수 | 인덱스 없음 | 인덱스 있음 |
|------|-----------|------------|------------|
| 1년 | ~36K | ~50ms | ~5ms |
| 3년 | ~110K | ~200ms | ~15ms |
| 5년 | ~180K | ~400ms | ~25ms |

### 프론트엔드

- 라디오 버튼 토글 (표 보기 / 그래프 보기)
- 그래프 뷰 선택 시 히스토리 API 호출 → 지표별 개별 라인 차트를 카드 그리드로 나열
- 카테고리별 히스토리 데이터를 프론트에서 캐시 (LRU 2-3개 카테고리)
- 차트 뷰에서는 스프레드 섹션 숨김 (원본 지표 차트만 표시)

## Technical Considerations

### 아키텍처 레이어별 책임

기존 port-adapter + CQS 패턴을 준수:

| 레이어 | 파일 | 책임 |
|--------|------|------|
| **Domain Repository (port)** | `EcosIndicatorRepository.java` | `findLatestHistoryByClassNames(Set<String>)` 인터페이스 정의. 중복 제거 방법은 인프라의 구현 세부사항 |
| **Infrastructure (adapter)** | `EcosIndicatorJpaRepository.java` | `@Query(nativeQuery=true)` ROW_NUMBER() 쿼리 |
| **Infrastructure (adapter)** | `EcosIndicatorRepositoryImpl.java` | JPA 결과 → `EcosIndicatorMapper`로 도메인 변환 |
| **Application** | `EcosIndicatorService.java` | `getHistoryByCategory()` 메서드 추가. 기존 서비스에 추가 (CQS 읽기 책임 유지) |
| **Presentation** | `EcosIndicatorController.java` | `GET /history` 엔드포인트. `List<EcosIndicator>` → 그룹핑 → `IndicatorHistoryResponse` DTO 변환 |
| **Presentation DTO** | `IndicatorHistoryResponse.java` | record. 프론트엔드 응답 형태 정의 |

**캐싱 결정**: 초기에는 캐싱 없이 구현 (YAGNI). 히스토리 데이터는 배치 주기(24시간)에만 변경되지만, API 호출 빈도를 먼저 관찰한 뒤 필요 시 Caffeine 캐시 추가.

### 데이터 처리

- **중복 cycle 처리**: DB 레벨 ROW_NUMBER()로 처리 (Java에서 하지 않음). 불필요한 데이터 전송과 메모리 할당 방지
- **dataValue 파싱**: String → Number 변환 시 null, 빈 문자열, 콤마 포함 숫자 처리 필요. Chart.js에 `spanGaps: false` 설정으로 null 포인트는 선 끊김 표시
- **히스토리 없는 지표**: history가 2건 미만이면 차트 대신 "데이터 부족" 메시지 표시

### Chart.js 인스턴스 관리

### Research Insights — Chart.js 다중 인스턴스

**Map 기반 레지스트리 패턴:**

```javascript
// ecos.js에서 Chart 인스턴스 관리
ecos: {
  _chartInstances: null, // Alpine reactive proxy 회피를 위해 init에서 설정
  // ...
},

initEcos() {
  // Chart.js 인스턴스를 Alpine reactive scope 밖에서 관리
  Object.defineProperty(this.ecos, '_chartInstances', {
    value: new Map(), writable: true, enumerable: false
  });
}
```

**Alpine.js reactive proxy 회피가 중요한 이유**: Alpine.js가 Chart.js 내부 객체를 Proxy로 감싸면 성능 저하 및 예상치 못한 동작 발생. `Object.defineProperty`로 non-enumerable하게 설정하여 Alpine의 추적에서 제외.

**차트 생성/파괴 패턴:**

```javascript
renderEcosCharts() {
  this.destroyEcosCharts();
  
  this.ecos.historyData.forEach(indicator => {
    if (indicator.history.length < 2) return; // 데이터 부족 스킵
    
    const canvasId = `ecos-chart-${indicator.keystatName}`;
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    
    const chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: indicator.history.map(h => h.cycle),
        datasets: [{
          data: indicator.history.map(h => {
            const v = parseFloat(h.dataValue?.replace(/,/g, ''));
            return isNaN(v) ? null : v;
          }),
          borderColor: '#3B82F6',
          borderWidth: 1.5,
          pointRadius: 0,
          tension: 0.3,
          fill: false,
          spanGaps: false,
        }]
      },
      options: {
        animation: false,          // 20+ 차트에서 필수
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            enabled: true,
            mode: 'index',
            intersect: false,
          },
        },
        scales: {
          x: {
            display: true,
            ticks: { maxRotation: 45, autoSkip: true, maxTicksLimit: 8 }
          },
          y: {
            display: true,
            ticks: { maxTicksLimit: 5 }
          }
        }
      }
    });
    this.ecos._chartInstances.set(canvasId, chart);
  });
},

destroyEcosCharts() {
  if (!this.ecos._chartInstances) return;
  this.ecos._chartInstances.forEach(chart => chart.destroy());
  this.ecos._chartInstances.clear();
}
```

**성능 최적화 요약:**

| 옵션 | 값 | 이유 |
|------|-----|------|
| `animation` | `false` | 20+ 차트에서 CPU 절감, Path2D 캐싱 활성화 |
| `pointRadius` | `0` | 렌더링할 원 없어 성능 향상 |
| `maintainAspectRatio` | `false` | 카드 그리드 컨테이너 높이에 맞춤 |
| `tension` | `0.3` | 시계열 데이터에 적합한 부드러운 곡선 |
| `maxTicksLimit` | X축 8, Y축 5 | 라벨 과다 방지 |

### Race Condition 방지

- 기존 `_requestGeneration` 패턴을 히스토리 API 호출에도 동일하게 적용
- 빠른 카테고리 전환 시 stale 응답 무시

### 반응형 레이아웃

- 차트 카드 그리드: `grid-cols-1 md:grid-cols-2 xl:grid-cols-3`
- 각 차트 카드: 고정 높이 컨테이너 (`h-48` 또는 `h-56`)
- canvas 부모에 명시적 높이 필수 (`responsive: true` + `maintainAspectRatio: false` 조합)

## Acceptance Criteria

- [ ] `GET /api/economics/indicators/history?category={CATEGORY}` 엔드포인트가 지표별 그룹핑된 히스토리 데이터를 반환한다
- [ ] 동일 cycle의 중복 레코드는 DB 레벨 ROW_NUMBER()로 deduplicate된다 (그룹당 정확히 1행)
- [ ] `idx_ecos_group_snapshot` 복합 인덱스가 Flyway 마이그레이션으로 추가된다
- [ ] ECOS 섹션에 라디오 버튼(표 보기/그래프 보기)이 표시된다
- [ ] 그래프 보기 선택 시 카테고리 내 각 지표의 라인 차트가 카드 형태로 표시된다
- [ ] Chart.js 인스턴스가 Map 레지스트리로 관리되고, 뷰/카테고리 전환 시 정리(destroy)된다
- [ ] 뷰 모드 토글 시 히스토리 데이터가 캐시되어 불필요한 재요청이 발생하지 않는다 (LRU 2-3개)
- [ ] 히스토리가 2건 미만인 지표는 "데이터 부족" 메시지를 표시한다
- [ ] dataValue가 null이거나 비숫자인 경우 차트에서 선 끊김(spanGaps: false)으로 처리된다
- [ ] Chart.js `animation: false`로 다수 차트 렌더링 성능 확보

## MVP 작업 리스트

### Phase 1: 백엔드 — 히스토리 조회 API

- [x] 1-1. Entity `@Index`로 `idx_ecos_group_snapshot` 복합 인덱스 추가 (ddl-auto: update로 관리)
- [x] 1-2. `EcosIndicatorJpaRepository`에 `@Query(nativeQuery=true)` ROW_NUMBER() 히스토리 조회 메서드 추가
- [x] 1-3. `EcosIndicatorRepository` (도메인 인터페이스)에 `findLatestHistoryByClassNames(Set<String>)` 추가
- [x] 1-4. `EcosIndicatorRepositoryImpl`에 구현 추가 (JPA → Mapper → 도메인 모델 변환)
- [x] 1-5. 히스토리 응답 DTO record 작성: `IndicatorHistoryResponse(keystatName, className, unitName, List<HistoryPoint>)`, `HistoryPoint(cycle, dataValue)`
- [x] 1-6. `EcosIndicatorService`에 `getHistoryByCategory(EcosIndicatorCategory)` 메서드 추가 (`@Transactional(readOnly=true)`)
- [x] 1-7. `EcosIndicatorController`에 `GET /history` 엔드포인트 추가 (도메인 리스트 → 그룹핑 → DTO 변환)

### Phase 2: 프론트엔드 — 뷰 토글 및 차트 렌더링

- [x] 2-1. `api.js`에 `getEcosIndicatorHistory(category)` API 메서드 추가
- [x] 2-2. `ecos.js`에 뷰 모드 상태 (`ecosViewMode`), 히스토리 데이터 캐시 (LRU Map), Chart.js 인스턴스 레지스트리 (non-enumerable Map) 추가
- [x] 2-3. `ecos.js`에 히스토리 로드 메서드 추가 (LRU 캐싱 + generation 패턴 적용)
- [x] 2-4. `ecos.js`에 차트 렌더링(`renderEcosCharts`) / 정리(`destroyEcosCharts`) 메서드 추가
- [x] 2-5. `index.html` ECOS 섹션에 라디오 버튼 UI 추가 (스프레드 섹션과 테이블 사이)
- [x] 2-6. `index.html`에 차트 뷰 카드 그리드 템플릿 추가 (반응형 grid + 고정 높이 canvas 컨테이너)

## 코드 예시 참조

### 기존 패턴 참조 파일

| 패턴 | 파일 | 라인 |
|------|------|------|
| Controller 엔드포인트 | `EcosIndicatorController.java` | 38-50 |
| Service 캐시 조회 | `EcosIndicatorService.java` | 29-46 |
| JPA Repository | `EcosIndicatorJpaRepository.java` | 5-8 |
| Domain Repository | `EcosIndicatorRepository.java` | 10-21 |
| Repository 구현체 | `EcosIndicatorRepositoryImpl.java` | 13-35 |
| Entity → Domain 매핑 | `EcosIndicatorMapper.java` | 8-35 |
| Category enum (classNames) | `EcosIndicatorCategory.java` | 8-57 |
| Chart.js 생성/파괴 패턴 | `financial.js` | 754-920 |
| Alpine.js 컴포넌트 | `ecos.js` | 1-443 |
| ECOS HTML 섹션 | `index.html` | 406-599 |
| API 클라이언트 | `api.js` | 76-81 |

### 주의사항

- **Alpine.js Proxy 회피**: Chart.js 인스턴스를 `Object.defineProperty`로 non-enumerable 설정. Alpine이 Chart.js 내부 객체를 Proxy로 감싸면 성능 저하 및 오류 발생
- **dataValue 파싱**: `parseFloat(value?.replace(/,/g, ''))` 패턴으로 콤마 처리. `isNaN()` 체크 후 null 반환
- **인덱스 마이그레이션**: Flyway 마이그레이션으로 복합 인덱스 추가. DDL auto-generation에 의존하지 않음

## Sources

- **Origin brainstorm:** [docs/brainstorms/2026-04-02-ecos-indicator-chart-brainstorm.md](docs/brainstorms/2026-04-02-ecos-indicator-chart-brainstorm.md) — 라디오 버튼 토글, 카테고리 일괄 API, 개별 라인 차트, 전체 히스토리 표시 결정
- **ECOS 대시보드 설계:** [docs/plans/2026-03-16-001-feat-ecos-indicator-dashboard-improvement-plan.md](docs/plans/2026-03-16-001-feat-ecos-indicator-dashboard-improvement-plan.md) — generation counter 패턴, 메타데이터 병합 방식
- **포트폴리오 차트 패턴:** [docs/plans/2026-03-15-001-feat-portfolio-donut-chart-section-subtotals-plan.md](docs/plans/2026-03-15-001-feat-portfolio-donut-chart-section-subtotals-plan.md) — Chart.js 인스턴스 관리 패턴
- **반응형 디자인 솔루션:** [docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md](docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md) — matchMedia, dvh, Alpine 반응형 패턴
- **Chart.js 공식 문서:** [Performance](https://www.chartjs.org/docs/latest/general/performance.html), [Responsive](https://www.chartjs.org/docs/latest/configuration/responsive.html), [API destroy()](https://www.chartjs.org/docs/latest/developers/api.html)
