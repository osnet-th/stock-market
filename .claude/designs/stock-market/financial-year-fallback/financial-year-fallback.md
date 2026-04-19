# 재무 데이터 조회 연도 Fallback 설계

## 작업 리스트

- [x] ChatContextBuilder에 유효 연도 탐색 로직 추가
- [x] ValuationMetricService에 유효 연도 탐색 로직 추가

## 배경

`ChatContextBuilder`와 `ValuationMetricService` 모두 `LocalDate.now().getYear()`(2026)로 DART API를 조회하지만, 사업보고서(`11011`)는 회계연도 종료 후(보통 3월 말) 공시되므로 2026년 데이터는 존재하지 않음. 결과적으로 재무계정, 지표, 가치평가 모두 빈 데이터/N/A 반환.

## 핵심 결정

- **Fallback 방식 채택**: `currentYear` → `currentYear - 1` 순서로 시도, 데이터 있는 연도 사용
- **유효 연도 판정 기준**: 재무계정(ACCOUNT) 조회 결과가 비어있지 않으면 해당 연도를 유효로 판정
- **한 번만 탐색**: 유효 연도를 먼저 결정한 후, 해당 연도를 모든 데이터 조회에 일관 적용
- **최대 1회 추가 API 호출**: currentYear 조회가 비어있을 때만 currentYear-1 시도

## 구현

### ChatContextBuilder 수정
위치: `chatbot/application/ChatContextBuilder.java`

`assembleFacts()`에서 먼저 유효 연도를 결정한 후, 해당 연도를 ACCOUNT/지표 조회에 사용.

[예시 코드](./examples/chat-context-builder-example.md)

### ValuationMetricService 수정
위치: `stock/application/ValuationMetricService.java`

`calculate()`에서 동일한 fallback 로직 적용.

[예시 코드](./examples/valuation-metric-service-example.md)

## 주의사항

- 3개년 지표 조회 범위: 유효 연도가 2025면 `2023, 2024, 2025` / 2026이면 `2024, 2025, 2026`
- fallback 해도 데이터가 없는 경우(신규 상장 등)는 기존대로 "데이터 없음" 표시
