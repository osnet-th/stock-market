# 종목평가 추정 손익 표 가독성 개선 Work 기록

gate: docs/gates/2026-07-12-stock-eval-estimate-income-table-gates.md

## 변경 파일 (프론트 전용, 백엔드 0)

### src/main/resources/static/js/components/stock-eval.js
"추정 손익"(output2) 전용 표시 헬퍼 6종 추가. 기존 함수 무변경.
- `isEstimateIncomeSection(sec)`: title==='추정 손익' && rows 짝수(>=2)일 때만 전용 표. 아니면 generic 폴백.
- `estimateIncomePeriods(sec)`: columns[1..] → `{label, est}`. "2026.12E" → {label:'2026', est:true}.
- `estimateIncomeGroups(sec)`: 6행을 (금액행+증감률행) 쌍으로 3그룹화. cell = `{amt, chg, dir, est}`.
- `fmtChangePct(v)`: ▲/▼ + 절대값% (÷10은 백엔드가 이미 처리, 여기선 표시만).
- `changeClass(dir)`: 증가 text-red-500 / 감소 text-blue-500 / 그 외 text-gray-400 (format.js 관례).
- `estimateUnitLabel()`: amountUnit → '조원'|'억원'.

### src/main/resources/static/partials/stock-eval.html
추정실적 `x-for` 섹션 루프 내부에서 `isEstimateIncomeSection(sec)`로 분기:
- true → 전용 표: 헤더(단위 라벨 + 기간, E 음영·배지), 바디 3행(지표 라벨 + 기간별 금액 상단/증감률 하단 색).
- false → 기존 generic 표(기본정보·추정 투자지표) 그대로.

## 설계 포인트
- 백엔드 output2 6행 데이터·엔드포인트·DTO 무변경. 프론트에서만 재구성.
- 증감률 ÷10은 기존대로 백엔드(`scaleBy10`)가 수행. 프론트는 값을 그대로 표시.
- 단위(억/조)는 기존 공유 `amountUnit` 토글을 그대로 사용(억 기본 유지). 전역 기본 미변경으로 재무 탭 무영향.

## 결정 변경
- 단위: brainstorm '조 기본' → work 중 `amountUnit`이 재무 탭과 공유 전역 상태임을 확인, '조 기본'은 재무 탭 시작값도 바꿔 범위 밖 → 태형님 재승인으로 **억 기본 유지**.
