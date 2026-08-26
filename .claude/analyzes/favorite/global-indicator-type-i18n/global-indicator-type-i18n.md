---
date: 2026-05-03
module: favorite
problem: 글로벌 관심지표 카드 / 최근 업데이트의 indicatorType 이 영문(`INFLATION RATE` 등)으로 노출
status: 분석 완료
---

# 글로벌 지표 한국어 라벨 미적용

## 증상

- 대시보드 홈 → 관심지표 글로벌 카드: `INFLATION RATE`, `INTEREST RATE` 처럼 enum 식별자가 노출됨.
- 동일 페이지의 "최근 업데이트(글로벌)" 도 동일.
- 사용자는 한국어 라벨(예: "물가상승률") 노출을 원함.

## 현재 동작

1. enum `GlobalEconomicIndicatorType` 에 한국어 `displayName` 필드가 이미 정의돼 있음.
   - 예: `INFLATION_RATE("inflation-rate", "물가상승률")`
2. DTO 직렬화 시 `enum.name()` (영문 식별자)만 노출.
   - `EnrichedFavoriteResponse.GlobalItem.indicatorType` (`EnrichedFavoriteResponse.java:75, 121`)
   - `RecentUpdateResponse.GlobalUpdate.indicatorType` (`RecentUpdateResponse.java:33, 42`)
3. 프론트는 underscore 만 공백으로 치환 → 영어 그대로 노출.
   - `index.html:525` (최근 업데이트)
   - `index.html:562` (GLOBAL GRAPH 카드)
   - `index.html:623` (GLOBAL INDICATOR 카드)

## 적용 범위 (사용자 합의)

- 관심지표 카드 (GRAPH / INDICATOR)
- 홈 대시보드 "최근 업데이트(글로벌)"
- **비-목표**: 글로벌 페이지(`#global` 라우트) 자체

## 후보안

- (안1) DTO 에 `indicatorTypeDisplayName` 한국어 필드 추가 → 프론트는 이 필드 사용. **권장**.
- (안2) DTO `indicatorType` 필드 자체를 한국어 교체. → 영문 enum 식별자가 키로 쓰이는 곳(`globalIndicatorCode`, `removeDashboardFavorite`, `refreshGlobalIndicator` 호출 등)이 깨질 위험.
- (안3) 프론트에서 매핑 테이블 보유. → enum 추가/변경 시 sync 필요. 신뢰성↓.

## 영향 범위

| 위치 | 파일:라인 | 변경 |
|---|---|---|
| EnrichedFavoriteResponse.GlobalItem | `EnrichedFavoriteResponse.java:72-131` | 필드 추가 |
| RecentUpdateResponse.GlobalUpdate | `RecentUpdateResponse.java:31-49` | 필드 추가 |
| 관심지표 GRAPH 카드 표시 | `index.html:562` | x-text 변경 |
| 관심지표 INDICATOR 카드 표시 | `index.html:623` | x-text 변경 |
| 최근 업데이트 표시 | `index.html:525` | x-text 변경 |

## 다음

설계 문서: `.claude/designs/favorite/global-indicator-type-i18n/global-indicator-type-i18n.md`