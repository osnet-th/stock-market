# 기업분석리포트 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-07-12-company-analysis-report-gates.md`로 참조한다.

## Stage Decisions
- start: approved (2026-07-12, 기존 자산 탐색 및 brainstorm 논점 제시 승인)
- brainstorm: approved (2026-07-12, "brainstorm 단계로 진행해줘") / 완료 (2026-07-12, Q1~Q7 전체 확정 "오키 그러면 이상태로 진행하자")
- issue: approved (2026-07-12, "진행해" — Issue #81 등록, worktree feat/issue-81-company-analysis-report 생성)
- plan: approved (2026-07-12, "진행해" — Entity 스키마·API·stock 확장 Approval Gate 포함 승인)
- work: approved (2026-07-12, plan 승인과 함께 work 진입 승인) / 완료 (2026-07-12, Phase 1~4 구현·컴파일 검증, docs/works/2026-07-12-company-analysis-report-work.md)
- review: approved (2026-07-12, "진행해") / 완료 (2026-07-12, major 4·minor 7 발견 전부 수정, docs/reviews/2026-07-12-company-analysis-report-review.md)
- validation: approved (2026-07-12, "진행해" — 앱/DB 기동 포함) / 백엔드 실호출 18항목 + 브라우저 10항목(Chrome 확장 직접 검증) 통과 (docs/validations/2026-07-12-company-analysis-report-validation.md)
- commit: pending
- push: pending

## Notes
- 요청: 회사 개요/연혁, 실적 추이(5~10년), 재무제표 요약, 재무지표, 주가지표, 기업가치(청산가치·DCF), 투자판단 7항목 A~E 등급을 등록·조회하는 기업분석리포트 기능.
- Entity/API 신설이 필요한 documented workflow 대상.
- 태형님 승인 범위 추가 (2026-07-13, validation 중): 목록 필터를 종목코드 정확일치 → **종목명 부분일치 전용**으로 변경 ("그냥 종목명으로만 검색 되도록 해줘").
- 태형님 승인 범위 추가 (2026-07-13, validation 중): 작성 화면을 **7단계 위저드**로 개편 + **임시저장(draft)** 도입, 재개 위치는 draft_step으로 마지막 단계 기억(1번안 "1번으로 해줘"). Entity 컬럼 2개 추가 승인 포함.
- 태형님 승인 범위 추가 (2026-07-14, "진행해"): 정성 입력을 **항목별 구조화 입력**(연혁 연도별 행 등)으로 개편 — 노트 TEXT 6컬럼 → `manual` JSONB 1컬럼 교체(Entity 변경 승인), 기존 테스트 데이터 메모 유실 승인.
- 태형님 승인 범위 추가 (2026-07-14): **예상 매출 입력(분기별, 단위 선택)** + **주가지표 계산기(재료 입력→계산식 기반 산출, 예상 매출 기반 예상 PSR/PER)**. 예상 순이익은 선택 입력. 판정 기준값 수정은 보류.
- 태형님 지적 반영 (2026-07-15, commit 단계 중): 주가지표 카드 "?" 툴팁이 `title` 네이티브라 계산식이 안 보이던 문제 → **커스텀 hover/클릭 팝오버**로 교체(배지 28곳, 헬퍼 4종). Chrome 확장 실동작 재검증 통과(validation T1~T6). plan 범위 내 UX 결함 수정으로 재개.
- 태형님 승인 범위 추가 (2026-07-15, "진행해" — 옵션 B): 주가지표 툴팁에 **계산 근거값(실제 대입값) 노출**. 백엔드 `PriceMetrics.breakdowns` 신설(스냅샷 응답 구조 확장 = Approval Gate), `SnapshotFinancialExtractor` 근거 수집, 프론트 동적 조립. 하위호환 유지(옛 스냅샷은 공식만). Chrome 실동작 재검증 통과(validation T7~T12).
- 태형님 요청 반영 (2026-07-15): 재무제표 요약 표를 **BS·IS·CF 3구역으로 분리**(소제목 구분). `statements`의 key 접두사로 프론트 그룹핑, 백엔드 무변경. preview 실동작 확인(validation T13).
- 태형님 승인 범위 추가 (2026-07-15, "A로 진행" — EPS/PER 폴백): 재무제표엔 나오는 당기순이익이 주가지표 EPS에선 결측이던 불일치 해결. 기존 `ValuationMetricService`가 계정명 정확일치 실패로 EPS를 못 구할 때 **companyreport `buildPriceMetrics`에서 timeline 당기순이익(account_id 매칭) ÷ 유통주식수로 폴백**(PER·PER×PBR·근거값 동반). 기존 valuation·종목평가 무변경. 삼성전자 EPS 7,757·PER 36.03 확인(validation T14).
