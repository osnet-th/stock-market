# 월급 사용 비율 화면 목업 기반 재설계 Work 기록

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- plan: docs/plans/2026-08-21-001-feat-salary-usage-screen-redesign-plan.md

## 백엔드

### 신규
- `domain/model/SpendingItem` — 하위 항목 (category·name·amount·fixed·sortOrder). 이름 빈 문자열 허용,
  100자 제한, 금액 ≥ 0. `isSameAs`로 세트 NOOP 비교.
- `domain/model/SpendingItemSet` — 월 단위 전체 항목 스냅샷 aggregate. `replaceItems`,
  `hasSameItemsAs`(순서 포함), `itemsOf(category)`.
- `domain/repository/SpendingItemSetRepository` 포트 + `SpendingItemSetEntity`(UNIQUE user+month),
  `SpendingItemEntity`(set_id ID 참조, idx), JPA 리포지토리 2종, `SpendingItemSetMapper`,
  `SpendingItemSetRepositoryImpl`(헤더 upsert 후 항목 전체 교체).
- `application/dto/SaveMonthlyCommand` — 일괄 저장 커맨드 (presentation→application 방향 유지용).
- `application/dto/SpendingItemResponse`, `CategoryAmountResponse`, `PreviousMonthResponse`.
- `presentation/dto/SaveMonthlyRequest` — Bean Validation + `toCommand()` 매핑.
- `PUT /api/salary/monthly/{yearMonth}` (SalaryController.saveMonthly).

### 수정
- `SpendingConfig` — `budget`(nullable ≥ 0) 필드, `updateAmountAndBudget`(메모 보존),
  `isSameAmountAndBudgetAs`(null 예산=0 동일 취급). 레거시 `create`는 budget 파라미터 확장,
  개별 upsert 경로는 상속 budget을 이어받아 보존.
- `SpendingConfigEntity`/`Mapper` — budget 컬럼. `SpendingConfigJpaRepository` 네이티브
  `DISTINCT ON` 쿼리 select 목록에 budget 추가.
- `SalaryService`
  - `getMonthly` — 유효 항목 세트 조회를 합쳐 line별 items/budget 반영,
    `itemsInheritedFromMonth`·`previous`(전월 income/총지출/저축률/카테고리 합) 포함.
  - `getTrend` — 포인트별 `categoryTotals`(8종, 없으면 0) 추가. 합계·저축률 산식 동일.
  - `saveMonthly` — income upsert(기존 NOOP 로직 재사용) + 카테고리 config upsert
    (직접 레코드 수정 / 상속 대비 변경 시에만 생성, 상속 메모 보존, 기록 없고 0이면 skip) +
    항목 세트 upsert(해당 월 세트 교체 / 상속과 다를 때만 생성, 빈→빈 skip). 응답은 갱신된 월별 뷰.
- `MonthlySalaryResponse`·`SpendingLineResponse`·`SalaryTrendResponse` — additive 확장.
- `GlobalExceptionHandler` — `HttpMessageNotReadableException` → 400 (`MALFORMED_REQUEST_BODY`).
  일괄 저장 API가 body에 enum을 받는 첫 사례로, 잘못된 enum 문자열이 500으로 떨어지던 것 보강.

## 프론트

- `partials/salary.html` 전면 재작성 — 목업 구성 그대로:
  헤더(월 전환 ‹›, 상속·미저장 노트, 예산 열 토글, 지난달 복사, + 새 월 기록) /
  요약 카드 3장(실수령액 입력·배분 리본·범례, 저축률 다크 카드(전월 delta·목표 바), 고정비/변동비) /
  사용처 계층 테이블(카테고리 접기·종류 배지·파생 합계 또는 직접 입력·비율·월급 대비 바·
  예산 입력+diff·+ 항목, 항목 행: 이름·고정/변동 토글·금액·카테고리 내 %·바·월급 %·삭제,
  미배분/초과 행, 되돌리기·변경 저장 footer) /
  추이(비율·금액·구성 3모드 SVG + 축·범례) / 지난달 대비 diverging 바(상위 6) / 눈에 띄는 것 3장 /
  새 월 다이얼로그. dc-* 토큰과 IBM Plex Mono(font-mono-num) 사용.
- `js/components/salary.js` 전면 재작성 — 편집 버퍼(income·cats[flat|items|budget|open]) + dirty,
  일괄 저장(PUT), 되돌리기, 지난달 복사(전월 유효값→버퍼), 월 전환 dirty confirm,
  파생 계산(배분/미배분/저축률/고정·변동/리본/예산 diff/전월 대비/인사이트),
  SVG 추이 3모드 직접 렌더(현재 월 포인트는 편집 버퍼 라이브 반영), 재진입 시 미저장 버퍼 보존.
  Chart.js 의존 제거. `salaryMan`은 1만 미만을 원 단위로 표기, 예산 일치 시 `±0`.
- `js/api.js` — `saveSalaryMonthly` 추가.
- `js/app.js` — salary Chart.js destroy 훅 2곳 제거 (SVG 전환으로 불필요).
- `js/components/portfolio.js` — 사라진 salary `renderTrendChart` 언급 주석 정리.

## 실행 중 확정 사항
- ddl-auto=update가 `spending_item_set`·`spending_item` 테이블과 `spending_config.budget`을
  자동 생성함을 로컬 PostgreSQL 16 기동으로 실측 확인.
- 홈 우측 패널(home-side)의 월급 스택 바는 응답 additive 확장으로 무수정 동작 확인.
- 컨테이너 프록시가 브라우저의 CDN 접근을 차단 → 검증은 npm 동일 패키지를 로컬 라우팅해 수행
  (검증 문서 참조). 운영 환경과 무관한 검증 인프라 사정.
