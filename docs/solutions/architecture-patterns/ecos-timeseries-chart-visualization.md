---
title: "ECOS 경제지표 시계열 차트 시각화 패턴"
category: architecture-patterns
date: 2026-04-02
tags:
  - chart-js
  - alpine-js
  - window-functions
  - row-number
  - time-series
  - frontend-performance
  - reactive-proxy
module: economics, frontend
symptom: "경제지표 히스토리 데이터를 cycle별 추이 차트로 시각화해야 하나, 중복 데이터 처리와 다수 차트 렌더링 성능 문제"
root_cause: "히스토리 조회 API 부재, 동일 cycle 중복 snapshotDate 데이터 존재, Alpine.js reactive proxy와 Chart.js 내부 객체 충돌"
---

# ECOS 경제지표 시계열 차트 시각화 패턴

## Problem

`ecos_indicator` 테이블에 배치 스케줄러가 매일 축적하는 히스토리 데이터가 있으나, 이를 조회하는 API와 시각화 UI가 없었음. 추가로:
- 동일 (className, keystatName, cycle) 조합에 대해 여러 snapshotDate의 레코드가 존재하여 중복 처리 필요
- 카테고리당 10-20개 지표의 개별 차트를 동시 렌더링해야 하는 성능 과제
- Alpine.js의 reactive proxy가 Chart.js 내부 객체를 감싸면 오류 발생

## Root Cause

1. **데이터 중복**: 배치가 같은 cycle에 대해 다른 snapshotDate로 여러 번 저장 가능
2. **API 부재**: `EcosIndicatorRepository`에 조회 메서드가 `saveAll`과 `existsAny`만 존재
3. **프레임워크 충돌**: Alpine.js v3은 모든 enumerable 프로퍼티를 Proxy로 감싸는데, Chart.js는 내부 상태를 직접 변경하므로 Proxy와 충돌

## Solution

### 1. DB 쿼리 — ROW_NUMBER() 윈도우 함수로 중복 제거

GROUP BY + MAX(snapshot_date) 대신 ROW_NUMBER()를 사용하면 동일 MAX date에 여러 행이 있어도 **정확히 1행**을 보장:

```java
// EcosIndicatorJpaRepository.java
@Query(value = """
    SELECT t.id, t.class_name, t.keystat_name, t.data_value,
           t.cycle, t.unit_name, t.snapshot_date, t.created_at
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
    """, nativeQuery = true)
List<EcosIndicatorEntity> findLatestHistoryByClassNames(
    @Param("classNames") Set<String> classNames);
```

**필수 복합 인덱스** (Entity `@Index`로 관리, `ddl-auto: update`):

```java
@Index(name = "idx_ecos_group_snapshot",
       columnList = "class_name, keystat_name, cycle, snapshot_date DESC")
```

### 2. Chart.js 인스턴스 — Alpine.js Proxy 회피

`Object.defineProperty`로 `enumerable: false` 설정하여 Alpine.js의 reactive tracking에서 제외:

```javascript
initEcosCharts() {
    Object.defineProperty(this.ecos, '_chartInstances', {
        value: new Map(), writable: true, enumerable: false
    });
}
```

### 3. 다수 차트 성능 — animation: false

20+ Chart.js 인스턴스 동시 렌더링 시 `animation: false`로 CPU 절감 + Path2D 캐싱 활성화:

```javascript
options: {
    animation: false,
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
        x: { ticks: { maxTicksLimit: 8, maxRotation: 45 } },
        y: { ticks: { maxTicksLimit: 5 } }
    }
}
```

### 4. Race Condition — Generation Counter

빠른 카테고리 전환 시 stale 응답 무시:

```javascript
async loadEcosHistory() {
    const thisGeneration = ++this.ecos._historyGeneration;
    // ... API 호출 ...
    if (thisGeneration !== this.ecos._historyGeneration) return; // stale 응답 버림
}
```

### 5. LRU 캐시 — 프론트엔드 메모리 관리

최대 3개 카테고리만 캐시하여 메모리 사용량 제한:

```javascript
const cacheKeys = Object.keys(this.ecos._historyCache);
if (cacheKeys.length >= 3) {
    delete this.ecos._historyCache[cacheKeys[0]]; // 가장 오래된 항목 제거
}
this.ecos._historyCache[this.ecos.selectedCategory] = result;
```

## Prevention & Best Practices

**Window Function 유지보수**
- 데이터 패턴 변경 시 쿼리 플랜 확인 필요. 복합 인덱스가 PARTITION BY + ORDER BY와 일치하는지 주기적 점검
- 인덱스 생성 목적과 날짜를 주석으로 기록

**Alpine.js + Chart.js 통합**
- `Object.defineProperty` 사용 이유를 코드 주석으로 명시 (향후 개발자가 Alpine 디렉티브로 변경 시도 방지)
- 뷰 전환/카테고리 변경 시 반드시 `destroy()` → `clear()` → 새로 생성 순서 준수

**캐시 모니터링**
- LRU 3개가 충분한지 사용 패턴 관찰. hit rate 60% 미만이면 용량 증가 고려
- 배치 실행(07:00) 후 캐시 무효화는 현재 미구현 — 사용자가 새로고침하면 갱신됨

**성능 기준선**
- 인덱스 있을 때: 36K행 ~5ms, 110K행 ~15ms, 180K행 ~25ms
- 인덱스 없을 때: 36K행 ~50ms → 180K행 ~400ms (5년 후 주의)

## Related Documentation

- [ECOS 히스토리 차트 계획서](../../plans/2026-04-02-001-feat-ecos-indicator-history-chart-plan.md)
- [ECOS 대시보드 개선 계획서](../../plans/2026-03-16-001-feat-ecos-indicator-dashboard-improvement-plan.md) — generation counter 패턴 원본
- [포트폴리오 도넛 차트 계획서](../../plans/2026-03-15-001-feat-portfolio-donut-chart-section-subtotals-plan.md) — Chart.js 인스턴스 관리 패턴
- [반응형 디자인 솔루션](../ui-bugs/responsive-design-tailwind-alpine.md) — Alpine.js 반응형 패턴