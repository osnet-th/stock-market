# 재무 타임라인 계정 taxonomy 불일치 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md`로 참조한다.

## Stage Decisions
- start: approved (2026-07-19, 현대차 값 누락 원인 확인 요청 → 원천 DART 진단)
- brainstorm: approved (2026-07-19, "방금 나온 두가지 이슈 한번에 처리하고싶어") / 완료 (2026-07-19, 원인 실측 확정 + 수정 범위 "근본 수정" 선택)
- issue: approved (2026-07-19, 근본 수정 선택 = 등록 승인 — Issue #85 등록, worktree 생성)
- plan: approved (2026-07-19, "진행해" — 결정1 공유 canonical 헬퍼·결정2 결측 보강(a) 포함, 공유 타임라인 Approval Gate 승인)
- work: approved (2026-07-19, plan 승인과 함께 work 진입 — Phase 1~2 구현) / 완료 (2026-07-19, 조립기만 수정·compileJava exit 0, docs/works/2026-07-19-timeline-account-taxonomy-work.md)
- review: approved (2026-07-19, "ce:review 로") / 완료 (2026-07-19, 4관점 — MED 1·LOW 4, 보안/성능 findings 없음, docs/reviews/2026-07-19-timeline-account-taxonomy-review.md)
- review 반영: 태형님 결정 (2026-07-19) — F1 하드닝(ifrs-full_ProfitLoss id 우선 + 주당 제외) + F4 nit. work 복귀 반영·재컴파일 exit 0 (work 문서 "리뷰 반영")
- validation: approved (2026-07-19, "진행해" — wt-85 재기동) / 완료 (2026-07-19, 현대차 netIncome·operatingCf·fcf 2017~2026 전부 채워짐·DART 값 일치, 삼성 회귀 없음(기존 값 불변)+2017·2018 영업CF 개선, docs/validations/2026-07-19-timeline-account-taxonomy-validation.md). priceMetrics EPS 경고는 ValuationMetricService 선행 별개 항목(범위 밖)
- 범위 확장: 태형님 지적 (2026-07-19 "영업이익 없는데? 매출액도 같이") — validation 중 영업이익 2018·2019 결측 발견. 보강을 매출액·영업이익·당기순이익 3종으로 일반화(work 범위 확장). 재기동 재검증: 현대차 영업이익 2018=2.42조·2019=3.61조 채워짐(DART 일치), 삼성 불변. compileJava exit 0.
- 번들 추가: 태형님 요청 (2026-07-19) — "조 2자리 버림" 통일. ① 공용 Format.truncTo2 신설 ② 종목평가 토글(stock-eval.js) ③ 기업리포트 crAmt(company-report.js, Format.compactNumber trunc 옵션). compactNumber 기본 동작 무변경(차트/재무 축 불변). 검증: 현대차 자본금 1.48조(이전 1.5조), 앱 재기동 새 JS 서빙 확인. 성격 달라 커밋 분리 예정.
- commit: pending
- push: pending

## Notes
- 원인: (A) 영업CF 2017·2018 — 세부계정 id 접두사(ifrs_ vs ifrs-full_) 별도 행 분리. (B) 당기순이익 2018~2022 — 주요계정(fnlttSinglAcnt) 결측(값은 전체재무제표 IS "연결당기순이익"에 존재).
- 영업CF 음수(2020~2025)는 DART 실제 값 → 범위 밖.
- Approval Gate 예정: 공유 타임라인(FinancialTimelineAssembler) 비즈니스 로직 변경 — 종목 평가·재무 타임라인 영향. plan 승인으로 확정.
- 관련 코드: FinancialTimelineAssembler(detailRowKey·toSummaryRows·mergeRows), SnapshotFinancialExtractor(summarySeries·detailSeries).
- 선행 무관 작업: #84(feat/issue-84, commit 게이트 대기).
