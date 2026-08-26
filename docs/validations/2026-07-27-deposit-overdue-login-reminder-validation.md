# 로그인 시 미납 납입 리마인더 팝업 Validation 기록

**Date:** 2026-07-27
**Issue:** #100
gate: docs/gates/2026-07-27-deposit-overdue-login-reminder-gates.md

## 실행한 검증

| 검증 | 명령/방법 | 결과 |
|------|-----------|------|
| JS 문법 | `node --check` (portfolio.js, app.js) | 통과 |
| 브라우저 기능 | Playwright + 사전설치 chromium, 목 데이터 하네스 (scratchpad/harness100 — 실제 portfolio.js·format.js·리마인더 마크업 추출 + Alpine) | **14/14 PASS** |
| 셀프 리뷰 | 커밋 0029995 라인 단위 재검토 | 명시적 findings 없음 (docs/reviews 참조) |

### 하네스 검증 시나리오 (14건)

- 미납 2건만 필터 표시 (depositOverdue=false·STOCK 제외), 항목명·유형 배지, 펀드/현금성 각각의 월납입액·납입일 meta
- [납입] 클릭: 팝업 닫힘 → `navigateTo('portfolio')` → `openDepositModal(해당 item)` 호출 순서 정확
- 스누즈 없이 새로고침 → 재노출 / [오늘 하루 보지 않기]+닫기 → 로컬 날짜 저장 → 당일 재노출 안 함
- 미납 0건 → 미노출 / 목록 제거 반영 / 콘솔·페이지 에러 없음

## 미검증 항목 (운영 배포 후 태형님 확인)

1. 실서버 전체 부트 경로(전체 partial mount + 실 API + 카카오 로그인)에서 팝업 노출 — 하네스는 리마인더 조각 격리 검증
2. 실데이터 기준 미납 판정 표시 (기존 `depositOverdue` 로직 재사용이므로 포트폴리오 "⚠ 미납" 배지와 동일하게 나오는지)
3. 모바일 폭 실기기 확인

## 판단

이 환경에서 가능한 검증 전부 통과. 미검증 항목은 배포 후 확인 성격 — commit/push 진행 가능.
