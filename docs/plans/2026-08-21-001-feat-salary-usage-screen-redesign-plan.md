# 월급 사용 비율 화면 목업 기반 재설계 Plan

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- issue: docs/issues/2026-08-21-salary-usage-screen-redesign-issue.md
- brainstorm: docs/brainstorms/2026-08-21-salary-usage-screen-redesign-brainstorm.md

## 목표

첨부 목업(Claude Design 단일파일 HTML)과 동일한 월급 사용 비율 화면을 구현하고,
목업이 요구하는 데이터 모델(카테고리 하위 항목·예산·일괄 저장·전월 비교·카테고리별 추이)을
백엔드에 추가한다. 기존 Effective Date 상속 모델과 홈 대시보드 소비 계약은 유지한다.

## 설계 요약

- 하위 항목: `spending_item_set`(user_id, effective_from_month, UNIQUE) 월 단위 스냅샷 +
  `spending_item`(set_id, category, name, amount, is_fixed, sort_order).
  유효 세트 = `effective_from_month <= 대상월` 중 최신. 빈 세트 = 명시적 "항목 없음".
- `spending_config.budget` 컬럼 추가(nullable ≥ 0). 항목 보유 카테고리의 `amount`는
  항목 합계를 파생 저장 → 기존 trend/합계/저축률 로직 무수정 정합.
- 일괄 저장 `PUT /api/salary/monthly/{yearMonth}`: income + 카테고리(금액·예산) + 항목 세트를
  한 트랜잭션 upsert. 개별 NOOP 의미론(상속값과 동일하면 레코드 미생성) 유지.
  memo는 신규 레코드 생성 시 상속값을 이어받아 보존.
- 월별 응답 확장(additive): line별 `budget`·`items`, 루트 `itemsInheritedFromMonth`·
  `previous`(전월 income/totalSpending/savingsRatio/categoryTotals).
- trend 포인트 확장(additive): `categoryTotals` (8개 카테고리 유효 금액).
- 프론트: Alpine 편집 버퍼 + dirty 추적, SVG 문자열 렌더(비율/금액/구성), Chart.js 제거.
- DDL: `ddl-auto: update`가 신규 테이블·컬럼 생성. 수동 마이그레이션 불필요.

## 체크리스트

### 백엔드
- [x] SpendingItem 도메인 모델 (category·name·amount·fixed·sortOrder, 검증 포함)
- [x] SpendingItemSet 도메인 모델 (월 스냅샷 aggregate, 동등 비교)
- [x] SpendingConfig budget 필드 + updateAmountAndBudget/isSameAmountAndBudgetAs
- [x] SpendingItemSetRepository 포트 (save / findByUserIdAndEffectiveFromMonth / findEffectiveAsOf)
- [x] SpendingItemSetEntity·SpendingItemEntity + JPA 리포지토리 + Mapper + RepositoryImpl
- [x] SpendingConfigEntity budget 컬럼
- [x] SpendingLineResponse 확장 (budget·items), SpendingItemResponse 신설
- [x] MonthlySalaryResponse 확장 (itemsInheritedFromMonth·previous), PreviousMonthResponse·CategoryAmountResponse 신설
- [x] SalaryTrendResponse.TrendPoint categoryTotals 추가
- [x] SaveMonthlyRequest (+ CategoryPayload/ItemPayload, Bean Validation)
- [x] SalaryService.getMonthly 확장 (items·previous 반영)
- [x] SalaryService.getTrend categoryTotals 반영
- [x] SalaryService.saveMonthly 일괄 upsert (config·item set NOOP 로직 포함)
- [x] SalaryController PUT /api/salary/monthly/{yearMonth}

### 프론트
- [x] api.js saveSalaryMonthly 추가
- [x] salary.js 재작성 — 편집 버퍼·dirty·파생 계산·SVG 추이 3모드·일괄 저장·지난달 복사
- [x] salary.html 재작성 — 목업 레이아웃(헤더 액션·요약 카드 3장·계층 테이블·하단 3섹션·새 월 다이얼로그)
- [x] app.js destroySalaryCharts 참조 제거 (Chart.js 캔버스 소멸)
- [x] 홈 대시보드(home-side) 소비 계약 무파괴 확인 (응답 additive 확장만)

### 마무리
- [x] self review (docs/reviews)
- [x] compileJava + test 검증 (docs/validations)
- [x] commit / push 기록 (docs/commits, docs/pushes)

## 범위 제외
- 커스텀 카테고리 추가/삭제 — enum 8종 유지, 후속 이슈 제안 (issue 문서 참조)
- 저축률 목표(50%)의 서버 설정화 — 프론트 상수로 시작
- 기존 개별 upsert/delete 엔드포인트 제거 — 하위 호환 유지

## 리스크
- spending_config.amount 파생 저장은 일괄 저장 API가 유일한 쓰기 경로일 때 정합 유지.
  레거시 개별 엔드포인트로 직접 쓰면 항목 합계와 불일치 가능 (신규 UI는 미사용, 문서화로 관리).
- 항목 세트는 월 단위 전체 스냅샷 — 한 카테고리만 바뀌어도 세트 전체가 신규 기록된다.
  (저장 UX가 전체 폼 일괄 저장이므로 의미상 자연스럽고, 상속 정합이 단순해진다.)
