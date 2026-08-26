# 로그인 시 미납 납입 리마인더 팝업 Review 기록

**Date:** 2026-07-27
**Issue:** #100
gate: docs/gates/2026-07-27-deposit-overdue-login-reminder-gates.md
**대상:** 커밋 0029995 (코드 3파일)
**방식:** 셀프 리뷰 (태형님 지시, 2026-07-27 — 프런트 3파일 additive 소규모)

## Findings

명시적 findings 없음. 아래 경로를 라인 단위로 재검토했고 결함을 찾지 못했다:

- **동작 경로**: `checkDepositReminder`의 스누즈 가드 → `getPortfolioItems` → `depositOverdue === true` 필터(STOCK의 null·false 제외) → 노출. `openDepositFromReminder`는 팝업을 닫은 뒤 `navigateTo('portfolio')`(→ `loadPortfolio()`로 items 적재 — app.js:270-272 확인) 후 `openDepositModal(item)` — 납입 후 `refreshDepositItem`이 id 기준으로 `portfolio.items`에서 재조회하므로 정합
- **부트 안전성**: 호출 지점이 partial mount·로그인 확인·SIGNING_USER 분기 이후, await 없는 fire-and-forget이며 메서드 전체가 try-catch — 부트 블로킹·unhandled rejection 없음. `init()`은 `_mqlCleanup` 가드로 1회 실행이라 중복 노출 없음
- **상태·마크업**: `depositReminder`는 Alpine 반응 프록시 하위라 중첩 변이 반영(하네스에서 실 Alpine으로 검증). x-cloak·오버레이·`@click.outside` 패턴은 같은 파일의 납입 모달과 동일, z-40으로 납입 모달(z-50) 아래. `@click.outside` 닫기도 `closeDepositReminder()`를 경유해 스누즈 체크가 유실되지 않음
- **스누즈 날짜**: 로컬 날짜 헬퍼 사용 — `toISOString`(UTC)이면 KST 오전에 "오늘"이 어긋나는 문제를 회피
- **동시성**: `#portfolio` 해시로 부트 시 `loadPortfolio`와 병렬 호출되나 리마인더는 `portfolio.items`를 건드리지 않아 상태 충돌 없음

남은 리스크 (결함 아님):

- [nit] `checkDepositReminder` 본문이 code-convention의 10줄 기준을 소폭 초과 — 기존 JS 컴포넌트 관행(`submitDeposit` 24줄 등) 범위 내이며 guard clause 단일 책임 흐름이라 유지
- [nit] 리마인더에 Escape 닫기 없음 — 같은 파일의 납입 모달도 동일(일관), 필요 시 후속
- 하네스는 리마인더 조각 격리 검증 — 실서버 전체 부트 경로는 validation 단계 확인 항목

## Open Questions / Assumptions

- 단일 사용자 앱 전제로 스누즈 키(`depositReminderSnoozeDate`)에 사용자 구분 없음 (plan 명시)
- `depositOverdue` 판정 자체(백엔드)는 검증 범위 밖 — 기존 로직 재사용

## Change Summary

로그인 부트 후 미납(`depositOverdue`) 항목이 있으면 리마인더 팝업을 띄우는 프런트 전용 additive 변경 (3파일, +111줄). 목록 + 항목별 [납입](포트폴리오 이동·기존 납입 모달 오픈) + [오늘 하루 보지 않기] 로컬 날짜 스누즈, 납입 완료 시 목록 제거. Playwright 하네스 14/14 PASS로 주요 시나리오 검증 완료, plan 범위와 정확히 일치.
