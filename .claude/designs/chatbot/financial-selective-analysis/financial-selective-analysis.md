# 재무 분석 챗봇 - 자동 주입형 분석 버튼

작성일: 2026-04-17
상태: 설계 (승인 대기)
원본: `.claude/designs/chatbot/financial-selective-analysis/financial-selective-analysis-brainstorm.md`

## 목표

재무 분석 챗봇(FINANCIAL 모드)에서 종목 선택 후, **분석 버튼만 클릭**하면 백엔드가 해당 작업에 필요한 재무 지표를 자동으로 조회해 LLM에 주입하고 응답을 받는다. 사용자가 카테고리나 세부 항목을 직접 고를 필요는 없다. 현재는 수익성·안정성 2개 지표만 하드코딩으로 주입돼 의도한 관점(저평가 판단 등)의 답변을 얻기 어렵다.

## 설계 변경 이력

브레인스토밍 → 1차 설계에서는 사용자가 체크박스로 카테고리·세부 항목을 선택하는 방식이었으나, **2026-04-17 태형님 지시로 자동 주입 방식으로 전환**. 체크박스 UI와 선택 상태 전달 필드는 모두 제거된다.

## 확정된 UX 흐름

1. 챗봇에서 '종목 분석' 모드 선택 (라벨 변경 '종목 재무' → '종목 분석')
2. 종목 검색 → 선택
3. 종목 선택 직후 **분석 버튼 4개** 노출 (종목 선택 전에는 비활성)
4. 버튼 하나 클릭 → 백엔드가 해당 작업의 카테고리 세트 자동 조회 → SSE 스트리밍 응답
5. 종목 변경해도 버튼 배치는 동일. FINANCIAL 모드에서는 자유 입력창 없음
6. 다른 모드(PORTFOLIO/ECONOMIC)는 기존 동작 유지

## 분석 작업 ↔ 카테고리 매핑 (백엔드 고정)

`AnalysisTask` enum 안에 매핑을 직접 보관한다. 새 작업 추가 시 한 곳만 수정.

| AnalysisTask | 자동 주입 카테고리 |
|---|---|
| `UNDERVALUATION` (저평가/고평가 판단) | 재무계정 + 수익성 + **가치평가** |
| `TREND_SUMMARY` (실적 추세 요약) | 재무계정 + 수익성 + **성장성** |
| `RISK_DIAGNOSIS` (리스크 요인 진단) | 재무계정 + **안정성** + **활동성** |
| `INVESTMENT_OPINION` (투자 적정성 의견) | 전체 6개 카테고리 |

## 카테고리별 데이터 소스

| 카테고리 (식별자) | 데이터 소스 | 시계열 |
|---|---|---|
| 재무계정 (`ACCOUNT`) | `StockFinancialService.getFinancialAccounts` | 3개년 (1회 호출로 제공) |
| 수익성 지표 (`PROFITABILITY` / M210000) | `getFinancialIndices` | 3개년 (3회 호출 병렬) |
| 안정성 지표 (`STABILITY` / M220000) | 〃 | 3개년 (3회 호출 병렬) |
| 성장성 지표 (`GROWTH` / M230000) | 〃 | 3개년 (3회 호출 병렬) |
| 활동성 지표 (`ACTIVITY` / M240000) | 〃 | 3개년 (3회 호출 병렬) |
| 가치평가 지표 (`VALUATION`) | 신규 `ValuationMetricService` | **최근 1년만** (역사 주가 API 부재) |

`getFinancialIndices`는 Caffeine 캐싱 적용 (키 `(stockCode, year, indexClassCode)`, TTL 1일). 병렬 호출 조합은 `CompletableFuture.allOf`.

## API 변경

### 요청 (`/api/chat`)

```
POST /api/chat?userId=X
{
  "message": "",                             // 빈 문자열 (버튼 전용)
  "chatMode": "FINANCIAL",
  "stockCode": "005930",
  "indicatorCategory": null,
  "analysisTask": "UNDERVALUATION",          // 신규 필드, FINANCIAL 모드에서 필수
  "messages": [...]
}
```

- 추가 필드는 `analysisTask` **하나뿐**. 카테고리/세부 항목 전달 필드는 만들지 않는다.
- `analysisTask`가 null이고 `chatMode == FINANCIAL` 이면 방어 안내 문구 반환 (프론트에서 이미 차단).
- 응답 포맷·SSE 구조 변경 없음 (`Flux<String>` 그대로).

## 구성 요소 영향

| 레이어 | 파일 | 변경 |
|---|---|---|
| presentation | `chatbot/presentation/ChatController.java` | `ChatMessageRequest` record에 `analysisTask` 추가, `ChatRequest` 생성 로직 업데이트 |
| application DTO | `chatbot/application/dto/ChatRequest.java` | `analysisTask` 필드 1개 추가 |
| application DTO | `chatbot/application/dto/AnalysisTask.java` (신규) | enum 4값 + 각 값의 카테고리 매핑 |
| application DTO | `chatbot/application/dto/FinancialCategory.java` (신규) | enum 6값 (백엔드 내부 매핑용) |
| application | `chatbot/application/ChatContextBuilder.java` | FINANCIAL 분기 재작성 — `analysisTask.categories()`로 자동 조립 |
| application | `chatbot/application/prompt/FinancialAnalysisPromptTemplate.java` (신규) | 4개 분석 작업별 시스템 프롬프트 생성 |
| application | `stock/application/ValuationMetricService.java` (신규) | 최근 1년 EPS/BPS/PER/PBR 계산 |
| application DTO | `stock/application/dto/ValuationMetricResponse.java` (신규) | 단일 연도 결과 DTO |
| application | `stock/application/StockFinancialService.java` | `getFinancialIndices` Caffeine 캐싱 적용, 키 `(stockCode, year, indexClassCode)`, TTL 1일 |
| 프론트 | `static/js/api.js` | `streamChat` 바디에 `analysisTask` 직렬화 |
| 프론트 | `static/js/components/chat.js` | 분석 버튼 트리거 함수 추가 (상태에 체크박스 관련 필드 없음) |
| 프론트 | `static/resources/static/index.html` | FINANCIAL 영역에 분석 버튼 4개 추가, 모드 라벨 변경, FINANCIAL 모드 입력창 숨김 |

**이전 설계에서 제거된 항목 (구현하지 않음)**

- `selectedCategories`, `selectedAccounts`, `selectedIndicators` 필드
- `GET /api/stocks/financial/categories/items` 엔드포인트
- `FinancialCategoryItemService`, `FinancialCategoryItemsResponse`
- 프론트 카탈로그 조회, 체크박스 UI, 펼치기 UI

## 예시 코드

- [API·DTO 변경](./examples/chat-api-example.md)
- [ChatContextBuilder·PromptTemplate (자동 매핑)](./examples/financial-analysis-prompt-template-example.md)
- [ValuationMetricService (최근 1년)](./examples/valuation-metric-service-example.md)
- [프론트 UI 버튼](./examples/chat-ui-panel-example.md)

## 데이터 조립 규칙 (`ChatContextBuilder` FINANCIAL)

1. `analysisTask`가 null이면 방어 문구 반환.
2. `analysisTask.categories()`를 순회하며 해당 카테고리만 조회·조립.
3. 재무계정은 1회 호출로 3개년 제공. `statementDiv` 그룹(IS/BS)으로 서브 섹션 분리.
4. 수익성·안정성·성장성·활동성 지표는 3개년 병렬 호출(`CompletableFuture.allOf`). 실패 연도는 "데이터 없음" 표기 후 계속 진행.
5. 가치평가는 `ValuationMetricService.calculate(stockCode)`로 최근 1년 EPS/BPS/PER/PBR. 실패 시 "계산 불가: 사유".
6. 모든 프롬프트 끝에 "업종 평균이 필요하면 일반지식으로 추정하되 추정치임을 명시" 문구 삽입.

## `ValuationMetricService` 책임

- 입력: `stockCode`
- 출력: `ValuationMetricResponse { String termName, BigDecimal eps, BigDecimal bps, BigDecimal per, BigDecimal pbr, String referencePriceDate, BigDecimal referencePrice, List<String> warnings }`
- 계산 로직
  - `getFinancialAccounts(stockCode, currentYear, "11011")` → 당기순이익·자본총계 최근 연도(currentTerm) 추출
  - `getStockQuantities(stockCode, currentYear, "11011")` → 유통주식수
  - `EPS = 당기순이익 / 유통주식수`, `BPS = 자본총계 / 유통주식수`
  - `StockPriceService.getPrice(stockCode, MarketType.DOMESTIC, ExchangeCode.KRX)` 현재 주가
  - `PER = 주가 / EPS`, `PBR = 주가 / BPS`
- 실패 내성: 일부 항목이 비면 해당 값만 null + warnings에 사유 추가. 나머지 값은 프롬프트에 포함.
- 아키텍처: application 계층, 외부 API 직접 호출 없음, 기존 서비스만 조합.

## 프론트 UI 구조

- FINANCIAL 영역 종목 선택 배지 바로 아래 **분석 버튼 4개** (2×2 그리드)
  - `[저평가/고평가 판단]` `[실적 추세 요약]` `[리스크 요인 진단]` `[투자 적정성 의견]`
- 종목이 선택된 상태에서만 버튼 활성화
- 버튼 클릭 → `sendChatMessage({message:'', analysisTask:<ID>})` → SSE 요청
- FINANCIAL 모드에서는 자유 입력창 숨김. 다른 모드는 입력창 유지
- 체크박스·세부 항목·카테고리 펼침·전체 선택 토글 모두 없음

## 작업 리스트

**구현 순서: 백엔드 DTO·enum → 서비스 → 프롬프트 → 컨텍스트빌더 → 엔드포인트 → 프론트.**

### 백엔드 — enum·DTO
- [x] `chatbot/application/dto/FinancialCategory.java` enum 신설 (`ACCOUNT/PROFITABILITY/STABILITY/GROWTH/ACTIVITY/VALUATION`)
- [x] `chatbot/application/dto/AnalysisTask.java` enum 신설 — 각 값에 `List<FinancialCategory>` 매핑 보유
- [x] `chatbot/application/dto/ChatRequest.java` `analysisTask` 필드 추가
- [x] `stock/application/dto/ValuationMetricResponse.java` DTO 신설

### 백엔드 — 서비스·프롬프트
- [x] `stock/application/ValuationMetricService.java` 신설 — 최근 1년 EPS/BPS/PER/PBR 계산
- [x] `stock/application/StockFinancialService.java` `getFinancialIndices`에 Caffeine 캐싱 (`financial-indices` 캐시, 키 `(stockCode, year, indexClassCode)`, TTL 1일)
- [x] `chatbot/application/prompt/FinancialAnalysisPromptTemplate.java` 신설 — 4개 작업별 시스템 프롬프트
- [x] `chatbot/application/ChatContextBuilder.java` `buildFinancialContext` 재작성 — `analysisTask.categories()` 기반 자동 조립, 병렬 3개년 조회, 템플릿 주입

### 백엔드 — 엔드포인트
- [x] `chatbot/presentation/ChatController.java` `ChatMessageRequest` record에 `analysisTask` 추가 및 `ChatRequest` 생성 로직 업데이트

### 프론트
- [x] `static/js/api.js` `streamChat` 바디에 `analysisTask` 직렬화
- [x] `static/js/components/chat.js` 분석 버튼 트리거 함수(`requestAnalysis(task)`) 추가, FINANCIAL 모드 상태 단순화
- [x] `static/resources/static/index.html` FINANCIAL 영역에 분석 버튼 4개 추가, 모드 라벨 변경, FINANCIAL 모드 입력창 숨김

### 정리
- [x] 변경된 함수·DTO의 테스트 가능성 확인 (의존성 주입·단일 책임 유지)

## 테스트 방침 (구현 단계에서 요청 시)

- `AnalysisTask.categories()` — enum 값별 매핑 검증.
- `ValuationMetricService` — 재무계정/주식수/주가 포트를 Mock으로 대체. EPS/BPS/PER/PBR 계산식의 결과값 검증.
- `ChatContextBuilder#buildFinancialContext` — 각 `AnalysisTask`에 대해 기대 섹션이 포함된 문자열이 만들어지는지 검증.
- `FinancialAnalysisPromptTemplate` — 4개 작업별 출력 문자열 스냅샷 검증.
- 메서드 호출 여부 테스트는 작성하지 않음 (CLAUDE.md 규칙).

## 해결된 결정

1. **지표 선택**: 사용자가 선택하지 않음. 버튼 클릭만으로 백엔드가 자동 주입.
2. **작업별 카테고리 매핑**: 상기 표대로 백엔드 고정. `AnalysisTask` enum이 매핑 보유.
3. **3개년 지표 조회**: `CompletableFuture.allOf` 병렬 + `getFinancialIndices` Caffeine 캐싱(TTL 1일).
4. **FINANCIAL 자유 입력창**: 제거. 분석 버튼 전용.
5. **가치평가 지표**: 최근 1년만 계산 (역사 주가 API 부재).
6. **비교 기준**: 자기 자신 3개년 추세 + LLM 일반지식 추정치 (추정치임을 명시하도록 프롬프트 지시).

## 사후 참고

- 기존 `indicatorCategory` 필드는 ECONOMIC 모드 전용으로 유지. 재사용하지 않음.
- 프로젝트 로깅 관행은 `@Slf4j` (기존 chatbot 코드와 통일).
- 한국주식(DART) 한정. 미국주식(SEC) 지원은 후속 과제.
