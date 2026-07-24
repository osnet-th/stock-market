# 투자판단 등급 자동 제안 Review 기록

**Date:** 2026-07-24
**Gate:** docs/gates/2026-07-24-grade-suggestion-gates.md
**방식:** 셀프 리뷰 (경계값 시나리오 추적 + diff 전수 확인 + 컨벤션 체크리스트)

## Findings (심각도 순)

명시적 findings 없음. 아래는 확인한 리스크와 수용한 예외다.

- (low, 수용) `GradeSuggestionCalculator.calculate()` 11줄 — 5항목 조립 메서드. code-convention 예외 정책(조립/mapper 성격) 적용, 기존 `SnapshotFinancialExtractor.buildBreakdowns` 선례와 동일 결.
- (low, 수용) `band()` for+if 중첩 2단계 — "불가피한 경우 2단계 허용" 범위 내.
- (info) DCF 보수가치가 음수(순현금 대폭 마이너스)면 비율이 null이라 A/B 밴드가 생성되지 않음 — 낙관/PER 밴드로만 판정. 의도된 보수 동작.
- (info) 성장성 제안은 기준연도 성장률 쌍이 없으면 생략(전년 0/결손 기저효과 케이스) — brainstorm의 "오판보다 생략" 원칙.

## 경계값 시나리오 확인 (통과)

- 청산가치 비율 0.9→A, PBR만 0.5→B(자산 A는 청산가치 근거일 때만), 둘 다 없음→생략.
- DCF 보수 1.2→B, 보수 초과+낙관 1.2→D, 보수·낙관 초과+PER 12→C(유리한 쪽), FCF≤0+PER 없음→E, FCF 데이터 없음+PER 없음→생략.
- 건전성 risk 2개→E, 채무초과 시그널→E(판정 무관), 전부 good+순현금>0+시그널 청정→A, 판정 일부 결손→C(보수 처리).
- 수익성 영업이익률 risk→E, ROE 기준연도 음수→E, 전부 good+ROE≥15·마진≥10 2~3개년→A.
- 성장성 둘 다 음수 2년 연속→E, 단년 음수→D, 둘 다 ≥10 + 4개년 중 3개년 충족→A.
- 프론트: preview 실패/스냅샷 없음 시 제안 UI 전체 숨김, 정성 2항목 폼 값 불변, draft 재개 시 제안 유지.

## Open Questions / Assumptions

- 임계값(1.0/1.5/2.5/4.0배, PER 8/15/25, ROE 15 등)은 2026-07-24 대화에서 승인한 기준표 고정값 — 조정은 후속(파라미터화) 범위.
- 제안 등급과 확정 등급의 차이 이력은 저장하지 않는다는 가정 유지 (brainstorm 범위 밖 명시).

## Change Summary

백엔드: 신규 계산기 1 + DTO 필드 additive 확장 + ReadService 연결 (3파일). 프론트: 위저드 7단계 제안 표시/적용 + 상세 뷰 참고 표시 (2파일). Entity·DB·스냅샷 스키마·저장 로직 불변.
