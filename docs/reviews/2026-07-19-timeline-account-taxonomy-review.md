# 재무 타임라인 계정 taxonomy 불일치 Review 기록 (ce:review)

work: docs/works/2026-07-19-timeline-account-taxonomy-work.md
gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
issue: https://github.com/osnet-th/stock-market/issues/85
review_agents: code-simplicity-reviewer, security-sentinel, performance-oracle, architecture-strategist

## Findings (심각도 순)

### F1 [MED · 정확성] 당기순이익 총계 매칭이 이름 contains + 무정렬 findFirst
- 근거: `FinancialTimelineAssembler.isTotalNetIncome`/`detailNetIncomeTotal`. `contains("당기순이익")` + `지배/비지배/계속영업/중단영업` 제외 + DART 응답순 `findFirst`.
- 위험: 비표준 라벨 **"주당당기순이익"(EPS, 주당값)** 은 contains에 걸리고 제외어에도 안 걸림. 응답순에서 총계보다 앞서면 결측 연도에 **주당값(수천 원)** 이 backfill → 요약 당기순이익 오표기 + snapshot ROE/ROA/EPS폴백/발생액/실효세율 전파. (실무상 총계가 EPS보다 앞서 오면 무해하나 순서 의존.)
- 완화안: `canonicalAccountId(id) == "ifrs-full_ProfitLoss"`(정확 총계 요소) 우선 매칭 + 무표준코드 연도는 이름 매칭(`!contains("주당")` 추가)으로 폴백.

### F2 [LOW · 정확성] backfill 대상 행 선택 startsWith("당기순이익")
- `normalize(name).startsWith("당기순이익")` findFirst — "당기순이익률"(비율) 앞서면 오선택 가능. 단 요약계정(fnlttSinglAcnt)은 비율 행 미반환 + 소비자 `SnapshotFinancialExtractor.summarySeries`가 **동일 기준** 사용 → 실질 위험 없음(기준 일치는 강점).

### F3 [LOW · 정합성] putIfAbsent null 값 의미
- `putIfAbsent(year, currentTermAmount())`에 null 저장 가능(DART 금액 null). Map은 null을 "부재"로 취급 → 기존 셀 null인 연도는 상세 총계로 덮어써짐. 크래시 아님, "기존 값 보존" 주석과 미묘한 불일치(대체로 결측 보강 의도엔 부합).

### F4 [LOW · 단순성] 네이밍/상수/주석 nit
- `detailNetIncomeTotal`은 Map<연도,값> 반환 → `...ByYear`/`Series`가 정확(지역변수 `total`도 Map). "IS"/"CIS" 리터럴 인라인(파일 내 `CASH_FLOW_DIV` 등과 스타일 불일치). "연결 총계" 주석은 OFS(별도)일 때 부정확.

### F5 [LOW · cosmetic] netRow==null 신규 행 append
- 어느 해에도 당기순이익 요약 행이 없던 종목: 신규 행이 리스트 맨 뒤 → 종목평가/타임라인에서 IS 자연 위치가 아닌 뒤에 표시(값·매칭 정상).

### 보안 (security-sentinel): 명시적 findings 없음
- canonicalAccountId(null)·normalize(null)·substring 경계·isTotalNetIncome null 필드·details() non-null(서비스가 List.of() 보장)·mutable 맵/리스트 전부 안전, 신규 크래시 표면 없음.

### 성능 (performance-oracle): 명시적 findings 없음
- `detailNetIncomeTotal`은 assemble당 1회, ≤10컬럼 × IS/CIS 행. DART 70콜 대비 반올림 오차. N+1/DB 없음, 재계산 없음.

## Open Questions / Assumptions
1. F1 완화 여부(EPS 오매칭 방지) — id 우선 매칭 + `주당` 제외. 태형님 결정.
2. 확인된 blast radius(개선): 구접두사 종목이 이제 BS/CF 앵커에 정상 편입 → 이전 빈 값이던 `borrowings`/`cashAndSecurities`/`valuationInputs`가 채워짐. validation에서 회귀 아닌 개선으로 확인.
3. 정규화 `ifrs_`→`ifrs-full_`는 injective(서로 다른 요소 오병합 불가), raw `ifrs_` 키 소비자 없음(4관점 공통 확인).

## Change Summary
- 심각한 버그·보안·성능·회귀 없음. `putIfAbsent` 기존 값 보존이라 이미 맞던 셀 불변, 표준접두사(현대 등) 종목 주경로 무변화.
- 핵심 개선 후보: **F1(당기순이익 총계 매칭 하드닝, MED)** + 저위험 nit(F4).
