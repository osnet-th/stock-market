# 납입 리마인더 당일 미표시 + 이력 0건 판정 누락 Brainstorm

- Status: Decided (2026-08-10)
- gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md

## 문제 정의

태형님 보고(2026-08-10): "10일 적금을 해놓은게 있는데 납입 알림이 안 떠서. 당일날부터 떠야 하는 거 아냐?"

조사 결과 원인 2건 확인.

### 1. 납입일 당일 팝업 미표시

- #99 확정 스펙의 팝업 대상은 "당일 + 미납"인데, #100 구현이 기존 `depositOverdue`를 그대로 재사용.
- `PortfolioService.isDepositOverdue`는 `!referenceDate.isAfter(depositDueDate)`이면 false — **납입일 다음날부터** 미납 판정.
- 결과: 납입일 당일(예: 8/10, 납입일 10일)에는 리마인더 팝업이 뜨지 않음.

### 2. 납입 이력 0건 항목 판정 누락

- `PortfolioService.getItems`의 depositMap은 `findByPortfolioItemIdIn` 결과를 groupingBy — 이력이 1건 이상인 항목만 키 생성.
- `deposits == null`이면 `isDepositOverdue` 호출 자체를 건너뛰어 `depositOverdue`가 null로 남음.
- 결과: 납입 기록을 한 번도 남기지 않은 예금/적금/펀드는 납입일이 지나도 미납 배지·리마인더에 잡히지 않음.
- 같은 가드에 묶인 만기 예상 금액(`expectedMaturityAmount`)도 이력 0건 항목에서 미계산.

## 선택지와 결정

당일 반영 방식 (태형님 확정, 2026-08-10):

- (A) **팝업만 당일 포함** — 백엔드에 `depositDueToday` 플래그 추가, 팝업 필터를 `depositOverdue || depositDueToday`로 확장. 포트폴리오 목록의 미납 배지는 기존대로 다음날부터 유지(당일은 아직 미납이 아니므로 의미 보존). ← **채택**
- (B) 미납 판정 자체를 당일부터로 변경 — 코드 최소지만 당일에 "미납" 배지가 붙어 의미가 어긋남. 기각.

null 가드 버그는 `getOrDefault(itemId, List.of())`로 이력 0건 항목도 판정 수행 (`isDepositOverdue`는 빈 리스트에서 이미 올바르게 true 반환).

## 범위

- 백엔드: `PortfolioService.getItems` 판정 가드 수정 + `isDepositDueToday` 신설 + `PortfolioItemResponse.depositDueToday` 필드 추가.
- 프런트: 리마인더 필터 확장 + 항목별 미납/오늘 납입일 구분 배지.
- 메일 알림(#99 요구 1)·알림 설정(#99 요구 3)은 본 건 범위 아님 — #99에서 계속 진행.
