# 투자판단 제안 등급 사유 패널 Review 기록

**Date:** 2026-07-26
**Issue:** #93
gate: docs/gates/2026-07-26-grade-reason-panel-gates.md
**방식:** compound-engineering `review_agents` 4종 병렬 리뷰 (code-simplicity-reviewer / security-sentinel / performance-oracle / architecture-strategist — ce:review 플러그인 미설치로 동일 구성 서브에이전트로 대체, 태형님 지시)

## 1. Findings (심각도 순)

### 중간

- **M1. 백엔드 역참조 주석 부재** (architecture) — plan의 "양쪽 상호 참조 주석" 완화책이 프런트(`crGradeCriteria` 주석)만 이행됨. `GradeSuggestionCalculator.java` javadoc·`SnapshotFinancialExtractor.java` 판정부에 프런트 사본 참조 1줄 주석 없음. 단 plan 범위 문구가 "프런트 2파일만 수정"이라 상충 — 백엔드 주석 추가 여부는 게이트 확인 필요.

### 낮음

- **L1. 부채비율 위험 경계 문구 오기** (simplicity·architecture 공통) — `crGradeCriteria.financialHealth.note` "위험 ≥300%". 백엔드 `judgeInverse`는 300 **초과**만 risk(정확히 300은 warn). "위험 >300%"로 수정 필요. 그 외 임계값·밴드 수치는 전수 대조 결과 백엔드와 전부 일치.
- **L2. profitability 문구 정밀도** (simplicity·architecture 공통) — E "영업적자"는 실제로 영업이익률 ≤0(0 포함), A "최근 3개 연도 지속"은 값 쌍 존재 연도 ≥2개 기준. 표시 단순화로 수용 가능하나 문구 보정 여지.
- **L3. `crReasonFormulas` if-체인 약 30줄** (simplicity) — code-convention "10줄 초과 금지" 저촉. `crReasonSourceConfig`처럼 key→빌더 테이블 디스패치로 전환 제안.
- **L4. 쌍둥이 헬퍼 2쌍** (simplicity) — `_crReasonStatementValue`/`_crReasonRatioValue`, `crReasonStatementRows`/`crReasonRatioRows`는 배열명만 다름. 각 1개로 통합 가능.
- **L5. `crReasonBaseYear` 폴백 상이 가능성** (architecture) — "마지막 온기 컬럼" 폴백은 프런트 자체 규칙. 도달 경로가 제한적(valuationInputs 결손 시 비율 3항목 제안은 백엔드에서 생략)이라 실위험 낮음.

### nit

- **N1. `crReasonBasis(key)` 인자 비대칭** (simplicity·architecture 공통) — 형제 헬퍼처럼 `this.crReason.key` 내부 참조로 통일 가능.
- **N2. detail/preview 소스 분기 삼중 중복** (architecture) — `crSuggestion`·`crBreakdownText`·`_crReasonSource`. 기존 코드 불변 원칙상 이번 범위 밖.

### 참고 (견고성·접근성 — 보안 아님)

- basis 원소 비문자열 시 `.match` TypeError 가능(백엔드 계약 의존, 기존 `crSuggestionBasis`도 동일 가정).
- 청산가치 표 `crNum(line.ratio * 100, 0)`은 ratio null이면 "0%" 표시(백엔드는 현재 non-null 보장).
- 패널 focus trap 부재.

### findings 없음

- **security-sentinel**: 명시적 findings 없음 — 표시 전부 x-text, :class 폐쇄형 매핑, 정규식 ReDoS 불가, 노출 데이터는 기존 화면 부분집합.
- **performance-oracle**: 명시적 findings 없음 — Alpine 3 속성 단위 반응성으로 닫힌 패널 재평가 경로 없음, 데이터 규모(항목 5·연도 ~10) 기준 비용 1ms 미만, 리스너 누수 없음, x-transition 요구로 x-show 선택이 타당.

## 2. Open Questions / Assumptions

- M1 백엔드 주석 1줄 추가를 이번 브랜치에 포함할지 (plan 범위 문구 갱신 필요 — 태형님 게이트 확인).
- L1~L4·N1 수정 반영 여부 — 안전장치("발견 즉시 수정 금지") 준수, 태형님 확인 후 반영.
- 툴팁(crHint)과 패널이 같은 z-50 레이어 — 겹침은 validation 화면 확인 항목.

## 반영 내역 (태형님 "전부 수정해줘" 승인, 2026-07-26)

- **M1**: `GradeSuggestionCalculator` javadoc·`SnapshotFinancialExtractor` javadoc에 표시용 사본(crGradeCriteria) 역참조 1줄 추가 — 동작 불변, compileJava 통과.
- **L1**: financialHealth note "위험 ≥300%" → "위험 >300%".
- **L2**: profitability A "…지속(값 존재 연도 2개 이상)", E "영업이익률 ≤0%(영업적자, 0 포함) 또는 …"로 보정.
- **L3**: `crReasonFormulas` if-체인 → `_crReasonFormulaBuilders` 테이블 디스패치(항목별 빌더 ≤8줄).
- **L4**: `_crReasonStatementValue`/`_crReasonRatioValue` → `_crReasonRowValue(kind, key, year)`, `crReasonStatementRows`/`crReasonRatioRows` → `crReasonRows(kind)` 통합 (HTML 호출부 동기 수정).
- **N1**: `crReasonBasis()` 무인자화 — `this.crReason.key` 내부 참조.
- **N2**: 미반영 (기존 코드 리팩토링 금지 원칙 — 범위 밖 유지).
- 재검증: JS 문법(jsc) 통과, 하네스에서 5항목 패널 계산식/사유/원천 표 재확인, 콘솔 에러 없음.

## 3. Change Summary

프런트 2파일 + workflow 문서만 변경(plan 범위 준수, 백엔드 불변). `crReason` 상태는 기존 `crHint` 루트 프로퍼티 패턴, 패널 마크업은 기존 재무 상세 슬라이드 패널 관용구와 일치. 프런트가 참조하는 DTO 필드명·계산식 표기·기준표 수치는 백엔드와 대조 완료(불일치는 L1·L2 문구뿐). 신규 메서드 전부 사용됨(사장 코드 없음), 신규 라이브러리 없음, 기존 헬퍼 재사용 양호. 화살표는 제안 있는 항목에만 렌더(정성 2항목 미노출).
