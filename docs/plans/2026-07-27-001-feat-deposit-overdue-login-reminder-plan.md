---
title: 로그인 시 미납 납입 리마인더 팝업
type: feat
status: active
date: 2026-07-27
issue: https://github.com/osnet-th/stock-market/issues/100
origin: docs/brainstorms/2026-07-27-deposit-overdue-login-reminder-brainstorm.md
gate: docs/gates/2026-07-27-deposit-overdue-login-reminder-gates.md
---

# 로그인 시 미납 납입 리마인더 팝업 (#100)

## Overview

로그인(대시보드 부트 완료) 시 `depositOverdue` 항목이 있으면 리마인더 팝업을 띄운다. 미납 목록 + 항목별 [납입] 버튼(포트폴리오 이동 + 기존 납입 모달 오픈) + [오늘 하루 보지 않기] 스누즈. 백엔드 무변경, 프런트 3파일 additive.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `static/js/components/portfolio.js` | 리마인더 상태 + `checkDepositReminder` / `openDepositFromReminder` / `closeDepositReminder` 메서드, `submitDeposit` 성공 시 리마인더 목록 갱신 |
| `static/partials/portfolio-deposit-financial.html` | 리마인더 팝업 마크업 (전역 mount 오버레이, 기존 모달 패턴 재사용) |
| `static/js/app.js` | `init()` 부트 완료 후 `checkDepositReminder()` 호출 1곳 |

백엔드·API·Entity·DB 변경 없음. 기존 납입 모달·미납 배지·`isDepositOverdue` 불변.

## Implementation Steps

### 1. portfolio.js — 상태·메서드

- [ ] `portfolio` 상태에 `depositReminder: { show: false, items: [], snoozeChecked: false }` 추가
- [ ] `checkDepositReminder()`:
  - localStorage `depositReminderSnoozeDate` === 오늘(YYYY-MM-DD)이면 skip (localStorage 접근은 try-catch — 실패 시 스누즈 무시하고 진행)
  - `API.getPortfolioItems(this.auth.userId)` 호출 → `depositOverdue === true` 필터
  - 1건 이상이면 `depositReminder.items` 세팅 + `show = true`; 0건/조회 실패 시 조용히 skip (console.error만)
- [ ] `openDepositFromReminder(item)`: 리마인더 닫기(스누즈 저장 없음) → `navigateTo('portfolio')` → `openDepositModal(item)`
- [ ] `closeDepositReminder()`: `snoozeChecked`면 localStorage에 오늘 날짜 저장 후 닫기, 아니면 그냥 닫기 (세션 내 재노출 없음)
- [ ] `submitDeposit()` 성공 시: `depositReminder.items`에서 해당 항목 제거 (남은 목록이 비어도 팝업 재표시는 하지 않음 — 이미 닫힌 상태 유지)

### 2. portfolio-deposit-financial.html — 팝업 마크업

- [ ] 기존 모달 패턴(fixed 오버레이 + x-show + x-cloak) 재사용, `z-index`는 납입 모달보다 낮게 (겹침 시 납입 모달 우선)
- [ ] 헤더: "미납 납입 안내" + 닫기(X)
- [ ] 목록: 항목명, 자산 유형 라벨(펀드/현금성), 월 납입액(format), 납입일("매월 N일"), 항목별 [납입] 버튼
- [ ] 하단: [오늘 하루 보지 않기] 체크박스 + [닫기] 버튼
- [ ] 모바일: 기존 모달 반응형 패턴 따름

### 3. app.js — init 훅

- [ ] `init()`의 SIGNING_USER 분기 통과 후, 초기 페이지 로드와 병렬로 `this.checkDepositReminder()` 호출 (await 하지 않음 — 부트 블로킹 금지, 내부 catch로 실패 무해화)

## Technical Considerations

- **openDepositModal 재사용**: `openDepositModal(item)`은 전역 오버레이라 페이지 무관 동작하지만, 납입 후 목록 상태 일관성을 위해 포트폴리오 페이지로 이동 후 오픈한다 (`refreshDepositItem`이 `portfolio.items` 기준으로 동작).
- **스누즈 키**: `depositReminderSnoozeDate` 단일 키(사용자 구분 없음) — 단일 사용자 앱 전제. 값은 `YYYY-MM-DD`.
- **팝업 노출 시점**: 부트 직후 홈 로드와 병렬 — 홈 API 실패와 무관하게 동작.
- **재노출 정책**: 세션 내 닫으면 그 세션에서는 재노출 없음. 스누즈는 당일 전체. 다음 로그인/새로고침 시 미납 잔존이면 재노출 (brainstorm 확정).
- 기존 코드 리팩토링 없음 — additive만.

## Validation

- `jsc`(JavaScriptCore) `new Function(src)` 파싱으로 JS 문법 확인
- 스크래치패드 목 데이터 하네스(#93 방식)로 브라우저 확인: 미납 있음/없음/스누즈/[납입] 이동/납입 후 목록 제거
- 실서버 실데이터 확인은 태형님 확인 항목으로 validation 문서에 기록

## Risks

- app.js init 수정은 1곳 호출 추가뿐이나 부트 경로라 회귀 주의 (await 없는 fire-and-forget으로 격리)
- localStorage 미지원/차단 환경에서는 스누즈가 동작하지 않고 매번 노출 (수용)

## Out of Scope

- 백엔드 변경 일체, 메일/푸시 알림, 미납 판정 로직·기존 UI 변경
