---
title: 글로벌 경제지표 히스토리 조회 UI (표/그래프 토글)
type: feat
status: active
date: 2026-04-15
origin: docs/brainstorms/2026-04-15-global-indicator-history-view-brainstorm.md
---

# 글로벌 경제지표 히스토리 조회 UI (표/그래프 토글)

## Overview

글로벌 경제지표의 히스토리 데이터를 표/그래프로 조회하는 기능.
`global_indicator` 테이블에 히스토리가 이미 적재되고 있지만 (bfdd702), 조회 API와 UI가 없는 상태.
ECOS(국내 경제지표)의 표/그래프 토글 패턴을 미러링하되, 글로벌 특성(국가별 데이터)을 반영한다.

## Key Decisions (see brainstorm: docs/brainstorms/2026-04-15-global-indicator-history-view-brainstorm.md)

1. **뷰 모드**: ECOS와 동일한 2모드 토글 — `table`(기존 최신 스냅샷) / `chart`(히스토리 차트)
2. **API 범위**: 지표타입별 호출 (`GET /{indicatorType}/history`) — 기존 글로벌 API 패턴과 일관성 유지
3. **차트 구성**: 지표타입별 1개 차트, 국가별 라인(색상 구분)
4. **쿼리 패턴**: ECOS의 ROW_NUMBER 중복 제거 패턴 미러링

## Proposed Solution

### Backend

1. `GlobalIndicatorJpaRepository`에 히스토리 조회 native query 추가
2. `GlobalIndicatorRepository` 도메인 인터페이스에 조회 메서드 추가
3. `GlobalIndicatorRepositoryImpl`에 구현 추가
4. 히스토리 전용 Service 메서드 추가
5. `GlobalIndicatorController`에 히스토리 엔드포인트 추가
6. 응답 DTO 생성

### Frontend

1. `globalData`에 히스토리 관련 상태 추가
2. `global.js`에 히스토리 로드/차트 렌더 메서드 추가
3. `index.html`에 뷰 모드 토글 + 차트 뷰 HTML 추가
4. `api.js`에 히스토리 API 호출 메서드 추가

## Technical Considerations

### DB 쿼리 (ROW_NUMBER 패턴 미러링)

ECOS의 `EcosIndicatorJpaRepository:14-28` 패턴을 그대로 차용. 
차이점: `class_name IN (:classNames)` 대신 `indicator_type = :indicatorType`.
PARTITION BY에 `country_name` 추가 (국가별 cycle 중복 제거).

```sql
SELECT t.id, t.country_name, t.indicator_type, t.data_value, t.cycle, t.unit, t.snapshot_date, t.created_at
FROM (
    SELECT e.*,
           ROW_NUMBER() OVER (
               PARTITION BY e.country_name, e.cycle
               ORDER BY e.snapshot_date DESC
           ) AS rn
    FROM global_indicator e
    WHERE e.indicator_type = :indicatorType
) t
WHERE t.rn = 1
ORDER BY t.country_name, t.cycle
```

기존 인덱스 `idx_global_group_snapshot(country_name, indicator_type, cycle, snapshot_date DESC)`가 이 쿼리를 지원.

### 응답 DTO 구조

```java
// 지표타입별 히스토리 응답
public record GlobalIndicatorHistoryResponse(
    String indicatorType,
    String displayName,
    String unit,
    List<CountryHistory> countries
) {}

// 국가별 히스토리
public record CountryHistory(
    String countryName,
    List<HistoryPoint> history  // 기존 HistoryPoint 재사용
) {}
```

### 차트 (Multi-line)

ECOS 차트는 단일 라인이지만, 글로벌 차트는 국가별 멀티 라인.
Chart.js `datasets` 배열에 국가별 dataset 추가. 20색 팔레트 사용.

```javascript
datasets: countries.map((country, idx) => ({
    label: country.countryName,
    data: country.history.map(h => parseFloat(h.dataValue?.replace(/,/g, '')) || null),
    borderColor: COUNTRY_COLORS[idx % COUNTRY_COLORS.length],
    borderWidth: 1.5,
    pointRadius: 0,
    pointHoverRadius: 4,
    tension: 0.3,
    fill: false,
    spanGaps: false
}))
```

### Alpine.js + Chart.js 프록시 충돌 방지

`docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md` 패턴 적용:
- `Object.defineProperty`로 `_chartInstances`를 `enumerable: false`로 설정
- `animation: false`로 멀티차트 성능 확보

### 빈 데이터 처리

- cycle에 데이터 없는 국가: `null` 반환 → Chart.js `spanGaps: false`로 라인 끊김
- 히스토리 데이터 자체가 없는 경우: "히스토리 데이터가 없습니다" 메시지

## Acceptance Criteria

- [ ] `GET /api/economics/global-indicators/{indicatorType}/history` 엔드포인트 동작
- [ ] ROW_NUMBER로 cycle별 최신 snapshot만 반환
- [ ] 글로벌 지표 페이지에 "표 보기 / 그래프 보기" 토글 표시
- [ ] 그래프 모드에서 선택된 지표의 국가별 라인 차트 렌더링
- [ ] 카테고리/지표 전환 시 차트 정상 갱신 (generation counter로 stale 응답 방지)
- [ ] LRU 캐시 적용 (최대 5개 지표타입)

## 작업 리스트

### Phase 1: Backend — 히스토리 조회 API

- [x] **1-1. `GlobalIndicatorJpaRepository`에 native query 추가**
  - `findLatestHistoryByIndicatorType(String indicatorType)` 메서드
  - ROW_NUMBER PARTITION BY `country_name, cycle` ORDER BY `snapshot_date DESC`
  - 파일: `economics/infrastructure/persistence/GlobalIndicatorJpaRepository.java`

- [x] **1-2. `GlobalIndicatorRepository` 도메인 인터페이스에 메서드 추가**
  - `List<GlobalIndicator> findLatestHistoryByIndicatorType(GlobalEconomicIndicatorType indicatorType)`
  - 파일: `economics/domain/repository/GlobalIndicatorRepository.java`

- [x] **1-3. `GlobalIndicatorRepositoryImpl` 구현**
  - JPA Repository 호출 → mapper로 도메인 변환
  - 파일: `economics/infrastructure/persistence/GlobalIndicatorRepositoryImpl.java`

- [x] **1-4. 응답 DTO 생성**
  - `GlobalIndicatorHistoryResponse` record
  - `CountryHistory` record
  - `HistoryPoint` 재사용 (기존 ECOS DTO)
  - 파일: `economics/presentation/dto/GlobalIndicatorHistoryResponse.java`, `CountryHistory.java`

- [x] **1-5. Service 메서드 추가**
  - `GlobalIndicatorQueryService`에 `getHistoryByIndicatorType(indicatorType)` 추가
  - `@Transactional(readOnly = true)`
  - 파일: `economics/application/GlobalIndicatorQueryService.java`

- [x] **1-6. Controller 엔드포인트 추가**
  - `GET /api/economics/global-indicators/{indicatorType}/history`
  - Repository에서 조회 → countryName 기준 그룹핑 → DTO 변환
  - 파일: `economics/presentation/GlobalIndicatorController.java`

### Phase 2: Frontend — 표/그래프 토글 UI

- [x] **2-1. API 메서드 추가**
  - `API.getGlobalIndicatorHistory(indicatorType)`
  - 파일: `static/js/api.js`

- [x] **2-2. `globalData` 상태 확장**
  - `viewMode: 'table'`, `historyData: []`, `historyLoading: false`
  - `_historyGeneration: 0`, `_historyCache: {}`, `_chartInstances: Map`
  - 파일: `static/js/components/global.js`

- [x] **2-3. 히스토리 로드/차트 렌더 메서드 추가**
  - `switchGlobalViewMode(mode)` — ECOS `switchEcosViewMode` 미러링
  - `loadGlobalHistory()` — generation counter + LRU 캐시(5개)
  - `renderGlobalCharts()` — 국가별 멀티라인 Chart.js
  - `destroyGlobalCharts()` / `initGlobalCharts()`
  - 20색 팔레트 상수 `COUNTRY_COLORS`
  - 파일: `static/js/components/global.js`

- [x] **2-4. 지표 전환 시 차트 갱신 연동**
  - `selectGlobalIndicator()` 수정: `viewMode === 'chart'`이면 `loadGlobalHistory()` 호출
  - `selectGlobalCategory()` 수정: 카테고리 변경 시 차트 destroy
  - 파일: `static/js/components/global.js`

- [x] **2-5. HTML 템플릿 — 뷰 모드 토글 + 차트 뷰**
  - 지표 헤더 아래에 "표 보기 / 그래프 보기" 토글 버튼 (ECOS 동일 스타일)
  - 차트 뷰: 1개 차트 카드 (국가별 멀티라인), 범례 표시
  - 차트 로딩 스켈레톤
  - 히스토리 없음 메시지
  - 기존 테이블은 `viewMode === 'table'` 조건 추가
  - 파일: `static/index.html` (lines 668-767)

## Sources & References

### Origin

- **Brainstorm document:** [docs/brainstorms/2026-04-15-global-indicator-history-view-brainstorm.md](docs/brainstorms/2026-04-15-global-indicator-history-view-brainstorm.md)
  - Key decisions: ECOS 패턴 미러링, 지표타입별 조회, 국가별 라인 차트

### Internal References

- ECOS 히스토리 쿼리: `EcosIndicatorJpaRepository.java:14-28`
- ECOS 컨트롤러 그룹핑: `EcosIndicatorController.java:77-106`
- ECOS 프론트엔드 차트: `ecos.js:127-189`
- ECOS HTML 토글: `index.html:535-601`
- 글로벌 엔티티: `GlobalIndicatorEntity.java`
- 글로벌 컨트롤러: `GlobalIndicatorController.java`
- 글로벌 프론트엔드: `global.js`, `index.html:668-767`

### Institutional Learnings

- `docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md` — Alpine+Chart.js 프록시 충돌, ROW_NUMBER 패턴, generation counter
- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md` — 3-table 패턴, cycle 감지
