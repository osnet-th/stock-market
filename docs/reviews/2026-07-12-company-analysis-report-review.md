# 기업분석리포트 Review 기록

gate: docs/gates/2026-07-12-company-analysis-report-gates.md
plan: docs/plans/2026-07-12-001-feat-company-analysis-report-plan.md
work: docs/works/2026-07-12-company-analysis-report-work.md

리뷰 방식: 병렬 리뷰 에이전트 2건(백엔드/프론트) + 직접 점검. 정적 분석 기반(런타임 검증은 validation 단계).

## Findings (심각도순) — 전부 수정 완료

### Major

1. **공유 executor 중첩 submit+join 데드락 가능** — `CompanyReportSnapshotService.fetchSideData`의 증자이력 조회(`hasCapitalIncrease`)가 executor 스레드에서 실행되며, 내부의 `DisclosureQueryService.fetchByTypes`가 같은 `financialTimelineExecutor`(풀 8 고정)에 태스크를 제출 후 join. 동시 조립 8건이면 전 스레드가 join 블록 → 타임라인 기능 전체 마비 가능.
   - **수정**: 최대주주 이력과 동일하게 호출 스레드에서 실행 (`fetchCapitalIncreaseSafely`).
2. **자본잠식 기업의 부채비율·ROE 오판정** — `SnapshotFinancialExtractor.percentSeries`가 음수 분모(자본총계 < 0)를 허용해 부채비율이 음수 → `judgeInverse`에서 "good" 판정. 같은 리포트의 `negativeEquity` 시그널과 자기모순.
   - **수정**: 분모 `signum() > 0`일 때만 비율 산출 (해당 연도 값·판정 null 처리, negativeEquity 시그널이 리스크를 표시).
3. **(프론트) 페이지 재진입 시 차트 공백** — 이탈 시 차트 destroy 후 재진입 시 목록만 로드해 상세/미리보기 뷰의 캔버스가 빈 채 유지.
   - **수정**: `companyReportOnEnter()` 추가 — 유지 중인 뷰의 스냅샷으로 차트 재렌더, list 뷰면 목록 갱신.
4. **(프론트) 미리보기 레이스** — preview(최대 60초) 로딩 중 다른 종목 선택 시 요청이 무시되거나 이전 종목 데이터가 표시될 수 있음.
   - **수정**: `_previewGen` 세대 카운터 도입 (검색과 동일 패턴), 마지막 선택만 반영.

### Minor

5. 대량보유 정렬 comparator가 null에서 추이성 위반 가능(TimSort 계약) → `Comparator.nullsLast(reverseOrder())`로 교체.
6. partial 리마운트 시 차트 정리 누락 → `app.js cleanupRegistry`에 company-report 등록.
7. 새로고침 실패 시 기존 차트 소실 → destroy를 성공 응답 후로 이동.
8. "새 리포트" 재진입 시 이전 검색 결과 잔존 → openCreate에서 검색 상태 리셋.
9. 60초 타임아웃 호출의 에러 메시지가 15000ms로 하드코딩(기존 api.js 코드, 이번에 timeoutMs 첫 실사용으로 노출) → 실제 적용 타임아웃 표기로 수정.
10. snapshot 없는 리포트에서 DCF 카드가 빈 카드로 표시 → "계산 불가" 안내 추가. dead code `crHasSnapshot` 제거.
11. 파라미터 입력을 비우면 조용히 기본값 복원 → UI 힌트 문구 추가("비우면 기본값 적용").

### 수정하지 않음 (근거)

- **유동비율 판정에 warn 구간 없음**: 책 기준이 "100% 미만 = 단기 자금조달 위험" 이분법이라 의도된 동작.
- **메서드 10줄 초과 일부** (`buildPriceMetrics`, `riskSignals`, `requireRatios` 등): 다필드 record 생성/검증 나열로, 분리가 가독성을 해치는 code-convention 예외 사유에 해당.
- **ValuationParamsJsonConverter의 FAIL_ON_UNKNOWN_PROPERTIES=true**: 파라미터는 폐쇄형 스키마 유지가 목적(손상 감지). 스냅샷 쪽은 반대로 미지 필드 허용(스키마 진화 대비)으로 이원화 — 의도된 트레이드오프.

## Open Questions / Assumptions

- 금융업 종목은 DART 요약계정 명칭이 달라(영업수익 등) 실적/지표 일부가 비어 나올 수 있음 — validation에서 확인, 필요 시 후속 이슈.
- IS 없이 CIS(포괄손익)만 공시하는 종목은 매출원가율/판관비율이 비어 나올 수 있음 — 결손 표시("—")로 처리됨, 후속 개선 후보.
- `ValuationParams` 행은 항상 애플리케이션 쓰기 경로로만 생성된다고 가정 (DB 직접 조작 미고려).
- 리뷰는 정적 분석 기반 — 실 DART 데이터 통합 동작과 브라우저 렌더는 validation 단계에서 검증.

## Change Summary

신규 `companyreport` 패키지(도메인/스냅샷 조립/청산가치·DCF 계산기/JSONB 저장/REST 7종)와 stock 도메인 DART 확장(기업개황·최대주주·대량보유, 신규 포트), 프론트 "기업 리포트" 탭(목록/작성/상세 3뷰 + Chart.js). 리뷰에서 major 4건(데드락 가능성, 자본잠식 오판정, 재진입 차트 공백, 미리보기 레이스)·minor 7건을 발견해 전부 수정 또는 근거와 함께 수용. 수정 후 `compileJava`·JS 문법 검사 재통과.
