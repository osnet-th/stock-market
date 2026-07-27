# 로그인 시 미납 납입 리마인더 팝업 - Brainstorm

**Date:** 2026-07-27
**Status:** Decided (태형님 요청 + 노출 빈도/동작 선택 확정, 2026-07-27)
gate: docs/gates/2026-07-27-deposit-overdue-login-reminder-gates.md

## 배경

- 태형님 요청: "로그인시 미처리된 납입 내역 있으면 재 팝업으로 해줄래"
- 기존 구조 (이미 있는 것):
  - 펀드/현금성 항목의 월 납입 설정: `monthlyDepositAmount` / `depositDay` (FundDetail·CashDetail)
  - 미납 판정: `PortfolioService.isDepositOverdue` — 당월 납입일 경과 + 당월 납입 기록 없음 (`PortfolioService.java:1113`)
  - 포트폴리오 목록 API(`GET /api/portfolio/items`)가 항목별 `depositOverdue` 반환, 포트폴리오 페이지에서 "⚠ 미납" 배지 표시
  - 납입 처리 모달: `portfolio-deposit-financial.html` (`showDepositModal`, 전역 mount 오버레이) + `openDepositModal(item)`
- 문제: 미납 여부를 포트폴리오 페이지에 들어가야만 인지할 수 있음 → 로그인 시점에 리마인더 필요

## What We're Building

로그인(대시보드 부트 완료) 시 포트폴리오 항목을 조회해 `depositOverdue` 항목이 있으면 리마인더 팝업을 띄운다.

- 팝업 내용: 미납 항목 목록 — 항목명, 자산 유형, 월 납입액, 납입일(매월 N일)
- 항목별 **[납입]** 버튼 → 포트폴리오 페이지로 이동 + 해당 항목의 기존 납입 모달 오픈
- **[오늘 하루 보지 않기]** 체크 후 닫으면 당일 재노출 안 함 (localStorage, 날짜 기반)
- 닫기(X/오버레이): 이번 세션에서만 닫힘 — 다음 로그인/새로고침 시 미납이 남아 있으면 다시 노출

## 확정된 결정 (태형님 선택, 2026-07-27)

| 항목 | 결정 |
|------|------|
| 노출 빈도 | 기본 매 로그인/새로고침 노출 + "오늘 하루 보지 않기" 체크 옵션 (localStorage `depositReminderSnoozeDate` = YYYY-MM-DD) |
| 팝업 동작 | 미납 목록 + 항목별 [납입] 버튼 (포트폴리오 이동 + 기존 납입 모달 오픈) |
| 백엔드 | **변경 없음** — 기존 `getPortfolioItems` 응답의 `depositOverdue` 재사용. API·Entity·DB 불변 |
| 미납 판정 | 기존 `isDepositOverdue` 로직 그대로 (변경 없음) |

## 구현 방향

- `app.js init()`: 부트 완료 후(프로필 로드·SIGNING_USER 분기 통과 뒤) `checkDepositReminder()` 호출 — 홈 로드와 병렬, 실패 시 조용히 스킵
- `portfolio.js`(PortfolioComponent): 리마인더 상태(`depositReminder: { show, items }`) + `checkDepositReminder()` / `openDepositFromReminder(item)` / `closeDepositReminder(snooze)` 메서드
- 팝업 마크업: `portfolio-deposit-financial.html`에 추가 (납입 도메인 + 전역 mount라 어느 페이지에서든 표시 가능)
- 납입 완료(`submitDeposit` 성공) 시 리마인더 목록에서 해당 항목 제거, 목록이 비면 리마인더 자동 종료

## 검토한 대안 (채택 안 함)

- **하루 1회 고정 노출** — "재 팝업" 요청 취지(미납이 남아 있는 동안 계속 상기)와 어긋남. 스누즈 옵션으로 사용자가 선택하게 함
- **납입 모달 즉시 오픈** — 미납이 여러 개일 때 순차 진행 UX가 복잡. 목록형이 전체 현황 파악에 유리
- **전용 백엔드 API(`/deposits/overdue`) 신설** — 기존 목록 API로 충분 (YAGNI, 승인 게이트 대상 회피)

## Edge Cases

- 미납 항목 0개 → 팝업 미노출 (조회 실패 포함 — 콘솔 로그만)
- 스누즈 당일 재로그인 → 미노출, 다음날 미납 지속 시 재노출
- [납입] 클릭 → 포트폴리오 페이지 이동 후 납입 모달 오픈 — 리마인더 팝업은 닫음 (모달 겹침 방지)
- 납입 처리 후 미납 항목이 남아 있으면 리마인더 재표시하지 않음 (이번 세션에서는 이미 인지) — 다음 로그인 시 재노출
- SIGNING_USER(가입 중)·비로그인 → 체크 자체를 하지 않음 (init 분기 이후에만 호출)
- localStorage 접근 불가 환경 → 스누즈 없이 매번 노출 (try-catch)

## 범위 밖 (하지 않음)

- 백엔드 API·Entity·DB·미납 판정 로직 변경
- 메일/푸시 알림 (기존 장마감 알림과 별개)
- 포트폴리오 페이지 기존 "⚠ 미납" 배지·납입 모달 변경
