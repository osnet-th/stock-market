# 로그인 시 미납 납입 리마인더 팝업 Work 기록

**Date:** 2026-07-27
**Issue:** #100
gate: docs/gates/2026-07-27-deposit-overdue-login-reminder-gates.md
**Plan:** docs/plans/2026-07-27-001-feat-deposit-overdue-login-reminder-plan.md

## 변경 파일

- `src/main/resources/static/js/components/portfolio.js`
  - 상태: `depositReminder: { show, items, snoozeChecked }`
  - `checkDepositReminder()`: 스누즈 확인(localStorage `depositReminderSnoozeDate`) → `API.getPortfolioItems` → `depositOverdue === true` 필터 → 1건 이상이면 노출. 실패는 console.error 후 무해화
  - `_depositReminderToday()`: 스누즈용 로컬 날짜 (toISOString 은 UTC 라 KST 오전에 날짜가 어긋나 별도 헬퍼 사용 — 기존 depositForm 의 toISOString 관례는 그대로 둠)
  - `depositReminderMeta(item)`: 유형별 detail(fundDetail/cashDetail)에서 월 납입액·납입일 문자열 조립
  - `openDepositFromReminder(item)`: 팝업 닫기 → 포트폴리오 아니면 `navigateTo('portfolio')` → `openDepositModal(item)`
  - `closeDepositReminder()`: 스누즈 체크 시 오늘 날짜 저장(try-catch) 후 닫기
  - `submitDeposit()` 성공 시 리마인더 목록에서 해당 항목 제거
- `src/main/resources/static/partials/portfolio-deposit-financial.html`
  - 납입 모달(1~140행) 뒤에 리마인더 팝업 추가 — 기존 오버레이 모달 패턴, z-40(납입 모달 z-50 아래), 목록 max-h-72 스크롤, [오늘 하루 보지 않기] 체크 + [닫기]
- `src/main/resources/static/js/app.js`
  - `init()` SIGNING_USER 분기 통과 직후 `this.checkDepositReminder()` fire-and-forget 1줄

## 백엔드·API 변경

없음 (plan 준수 — 기존 `depositOverdue`/`isDepositOverdue` 재사용).

## work 단계 자체 검증 (하네스)

- JS 문법: `node --check` portfolio.js·app.js 통과
- 브라우저 검증: 스크래치패드 목 데이터 하네스(scratchpad/harness100 — 정적 서버 :8100, **실제 portfolio.js·format.js·리마인더 마크업**(partial 에서 추출) + Alpine(npm) + Playwright/사전설치 chromium) — **14/14 PASS**
  - 미납 2건만 표시 (depositOverdue=false·STOCK 제외), 항목명·유형 배지·월납입액·납입일 meta
  - [납입] 클릭 → 팝업 닫힘 + `navigateTo('portfolio')` + `openDepositModal(해당 item)` 호출 순서
  - 스누즈 없이 새로고침 → 재노출 / [오늘 하루 보지 않기]+닫기 → 로컬 날짜 저장 + 당일 재노출 안 함
  - 미납 0건 → 미노출 / 목록 제거 반영 / 콘솔·페이지 에러 없음
- 하네스 경로: scratchpad/harness100 (저장소 외부, 커밋 대상 아님)

## 미검증 항목 (validation 단계 과제)

- 실서버 전체 부트 경로(app.js init 전체 partial mount + 실 API)에서의 동작 — 하네스는 리마인더 관련 조각만 격리 검증. 태형님 실데이터 확인 권장
- 모바일 폭 실기기 확인
- 납입 모달과의 z-index 겹침은 설계상 회피(리마인더 닫은 후 모달 오픈) — 실서버에서 육안 확인
