# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 Validation 기록

**Date:** 2026-08-10
**Issue:** #111
gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md

## 실행한 검증

| 검증 | 명령/방법 | 결과 |
|------|-----------|------|
| 백엔드 컴파일 | `./gradlew compileJava` (worktree) | 통과 |
| 백엔드 테스트 | `./gradlew test` (worktree, 로컬 PostgreSQL 가동 중) | **117/117 PASS** (contextLoads 포함) |
| JS 문법 | 브라우저 하네스에서 portfolio.js 전체 로드 — 콘솔 에러 0 | 통과 (node 미설치로 `node --check` 대체) |
| 브라우저 기능 | 브라우저 목 하네스 (scratchpad/harness111 — 실제 portfolio.js·format.js·리마인더 마크업 추출 + Alpine CDN, launch.json `harness111` 정적 서버) | **전 시나리오 PASS** |

### 하네스 검증 시나리오

- 당일 항목(depositDueToday=true) 팝업 노출 + "오늘 납입일" 배지(emerald) — **핵심 수정 확인**
- 미납 항목(depositOverdue=true) 노출 + "미납" 배지(red), 유형 배지(현금성/펀드)·월납입액·납입일 meta 정상
- 비대상(플래그 없음 CASH·STOCK) 제외 — 2건만 표시
- [납입] 클릭: 팝업 닫힘 → navigateTo('portfolio') → openDepositModal(해당 item) 호출 순서 정확
- [오늘 하루 보지 않기]+닫기 → localStorage `2026-08-10` 저장 → 재확인 시 API 미호출·미노출 / 스누즈 해제 후 재노출(2건) / 대상 0건 미노출
- 스크린샷 확보 (팝업 렌더링 — 당일·미납 배지 구분 표시)

## 미검증 항목 (운영 배포 후 태형님 확인)

1. 실서버 전체 부트 경로(실 API + 카카오 로그인)에서 당일 항목 팝업 노출 — 하네스는 리마인더 조각 격리 검증
2. 실데이터 기준 `depositDueToday` 값 (백엔드 판정은 신규 로직 — 오늘 납입일인 실제 적금으로 확인 가능)
3. 이력 0건 항목의 미납 배지·만기 예상 금액 표시 (실데이터 의존)

## 판단

이 환경에서 가능한 검증 전부 통과. 미검증 항목은 배포 후 확인 성격 — commit/push 승인 대기.
