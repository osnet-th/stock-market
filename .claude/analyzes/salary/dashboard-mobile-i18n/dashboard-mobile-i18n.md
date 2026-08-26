---
module: salary
tags: [dashboard, mobile, responsive, i18n, ui]
problem_type: bug
issue: "#37"
---

# 대시보드 월급지출내역 - 모바일 짤림 / 영어 라벨 문제 분석

> 관련 이슈: [osnet-th/stock-market#37](https://github.com/osnet-th/stock-market/issues/37)

## 1. 현재 상태

### 1-1. 영향 범위
- 메인 대시보드의 "월급 사용 비율" 카드 (도넛 차트 + 보조 영역)
- "월급 사용 비율" 탭 페이지 (입력 폼 + 차트 영역)

### 1-2. 관련 파일
- 백엔드 DTO: `src/main/java/com/thlee/stock/market/stockmarket/salary/application/dto/SpendingLineResponse.java`
- 카테고리 enum: `src/main/java/com/thlee/stock/market/stockmarket/salary/domain/model/enums/SpendingCategory.java`
- 대시보드 카드 템플릿: `src/main/resources/static/index.html:269-306`
- 대시보드 차트 렌더: `src/main/resources/static/js/components/dashboardSummary.js:139-193`
- 탭 페이지 템플릿: `src/main/resources/static/index.html:2970-3163`
- 탭 페이지 컴포넌트: `src/main/resources/static/js/components/salary.js`

## 2. 문제점

### 문제 A. 대시보드 카드의 도넛 차트 범례가 영어로 표시됨

- 위치: `dashboardSummary.js:159`
  ```js
  const labels = positives.map(s => s.categoryLabel || s.category || '기타');
  ```
- 백엔드 응답(`SpendingLineResponse`)에는 `categoryLabel` 필드가 **없음**
- `s.category`는 `SpendingCategory` enum 값 (`FOOD`, `HOUSING`, `TRANSPORT`, `EVENTS`, `COMMUNICATION`, `LEISURE`, `SAVINGS_INVESTMENT`, `ETC`) — JSON 직렬화 시 enum 이름이 그대로 영어로 직렬화됨
- 결과: 도넛 차트 범례에 `FOOD`, `HOUSING` 등 영어 키가 표시됨
- 동일한 fallback이 sr-only 테이블(`index.html:295`)에도 사용되어 접근성 영역도 영문 노출

**참고**: 탭 페이지(`salary.js`)는 자체 `SALARY_CATEGORIES` 상수로 한글 라벨을 매핑하고 있어 문제 없음. 대시보드 카드만 별도 컴포넌트라 매핑이 빠져 있음.

### 문제 B. 입력 폼이 모바일에서 짤림

- 위치: `index.html:3063-3101`
- 구조: 라벨(`w-24` 고정) + 입력 필드(`flex-1 min-w-0`)를 `flex` 한 줄에 배치
- 카테고리 입력 행은 라벨(`w-24`) + 금액(`w-28` 고정) + 메모(`flex-1 min-w-0`) 3개 필드를 한 줄에 배치
- 모바일(< 375px)에서:
  - 라벨/금액 고정 너비 합 ≈ 14rem(224px) → 화면 너비 320~375px 기준 메모 입력 영역이 매우 좁아짐
  - 카테고리명이 길거나 메모가 들어가면 시각적으로 짤리거나 줄바꿈으로 레이아웃이 깨짐
- 반응형 수정자(`sm:`, `md:`)가 적용되어 있지 않음

### 문제 C. 월 선택 드롭다운이 모바일에서 과한 너비 차지

- 위치: `index.html:2986`
- `min-w-[140px]` → 320px 모바일에서 약 44% 차지
- 이전/다음 화살표 버튼 + "새 월 기록" 버튼이 함께 가로 배치되어 부담 가중

### 문제 D. 차트 캔버스 높이 고정

- 도넛/바: `h-64` (256px), 라인 차트: `h-80` (320px)
- 모바일에서도 동일한 높이 → 좁은 화면에서 차트가 세로로 길어 보이는 부조화
- (현재 `responsive: true, maintainAspectRatio: false`라 너비는 따라가나 높이는 고정)

## 3. 근본 원인

| 문제 | 근본 원인 |
|------|-----------|
| A | 백엔드 `SpendingLineResponse`에 한글 라벨 필드 없음 + 대시보드 컴포넌트에 자체 한글 매핑 없음 |
| B | 입력 행에 반응형 레이아웃(`sm:flex-row` / 모바일 `flex-col`) 미적용, 라벨/금액 너비 고정 |
| C | 드롭다운 `min-width`가 모바일/데스크톱 무관하게 단일 값 |
| D | 차트 부모 컨테이너 높이가 단일 값으로 고정 |

## 4. 영향도 / 우선순위

| 문제 | 사용자 영향 | 우선순위 |
|------|------------|----------|
| A 영어 라벨 | 대시보드 첫 화면에서 바로 보이는 텍스트 — 가독성 즉시 저하 | High |
| B 입력 폼 짤림 | 모바일 입력 자체 어려움 (이슈 본문이 직접 지목) | High |
| C 드롭다운 너비 | 월 전환 시 불편하나 동작은 가능 | Medium |
| D 차트 높이 | 시각적 비례만 어색함 | Low |

## 5. 해결 방향(요약)

상세는 설계 문서 `/home/user/stock-market/.claude/designs/salary/dashboard-mobile-i18n/dashboard-mobile-i18n.md` 참조.

- A: 백엔드 `SpendingLineResponse`에 `categoryLabel` 필드 추가 (한글) — 도메인 enum에 한글 표시명 도입
- B: 입력 행을 모바일에서 `flex-col`, sm 이상에서 `flex-row`로 전환. 라벨 너비 모바일 자동 / sm 이상 고정
- C: 드롭다운 `min-w` 반응형으로 조정
- D: 차트 컨테이너 높이 반응형(`h-48 sm:h-64` 등)
