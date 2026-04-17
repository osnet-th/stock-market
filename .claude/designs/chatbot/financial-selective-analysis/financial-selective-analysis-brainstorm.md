# 재무 분석 챗봇 - 지표 선택형 분석 (Brainstorm)

작성일: 2026-04-17
상태: 브레인스토밍 완료, 설계 문서 작성 대기

## 무엇을 만드는가

재무 분석 챗봇 화면에서 종목 선택 후, **분석에 포함할 재무 데이터 항목을 사용자가 직접 체크**해서 **정해진 분석 작업 버튼**으로 LLM 요청을 보낼 수 있게 한다. 현재는 FINANCIAL 모드에서 수익성·안정성 2가지 지표만 하드코딩으로 주입되고 있어, 사용자가 원하는 관점의 분석(저평가 판단 등)을 정확히 얻기 어렵다.

## 왜 이 방향인가

- 백엔드에 재무계정/재무지표/전체 재무제표 조회 메서드는 이미 있음 → 프론트 선택 + 백엔드 컨텍스트 빌더 확장만으로 구현 가능
- 자유 입력 방식보다 **버튼 기반**이 사용자 의도와 프롬프트 품질을 안정적으로 맞춤
- 토큰 비용 통제: 체크된 항목만 주입하면 프롬프트 크기 예측 가능

## 핵심 결정

| 항목 | 결정 |
|---|---|
| 선택 단위 | **2단계 구조** — 카테고리 기본 체크 + 펼치기로 세부 항목 선택 |
| 시계열 기간 | **고정 3개년** (최근 3년 연간 데이터 자동 주입) |
| 요청 트리거 | **분석 작업 버튼 방식** (자유 입력 없음) |
| 분석 작업 | **4개** — 저평가/고평가 판단 · 실적 추세 요약 · 리스크 요인 진단 · 투자 적정성 의견 |
| 비교 기준 | **자기 자신 3개년 추세 1순위 + LLM 일반지식(업종 평균)** — 프롬프트에 추정치 고지 의무 명시 |
| UI 위치 | **종목 선택 직후 인라인 접이식 패널** (채팅창 상단) |

## 카테고리 구성 (초안)

백엔드 DTO(`FinancialAccountResponse`, `FinancialIndexResponse`, `FullFinancialStatementResponse`)와 DART 지표 분류(M210000 수익성, M220000 안정성 등) 기준으로 맞춰야 함.

- 재무계정 (매출액, 영업이익, 당기순이익, 총자산, 부채총계 등)
- 수익성 지표 (ROE, ROA, 영업이익률 등)
- 안정성 지표 (부채비율, 유동비율 등)
- 성장성 지표 (매출성장률, 영업이익성장률 등) — 실제 제공 여부 확인 필요
- 가치평가 지표 (PER, PBR, EPS, BPS 등) — 실제 제공 여부 확인 필요

> ⚠ 실제 어떤 카테고리/세부 항목이 제공 가능한지는 `FinancialOptionsResponse`·DART API 문서 기반으로 설계 단계에서 확정.

## UI 와이어프레임 (요약)

```
┌─ 종목선택: 삼성전자 ▼ ─────────────────────┐
├─ [선택된 지표로 분석] ▼ (접이식)            │
│  ☑ 재무계정    [▶ 세부]                     │
│  ☑ 수익성 지표 [▶ 세부]                     │
│  ☐ 안정성 지표 [▶ 세부]                     │
│  ☑ 가치평가 지표 [▶ 세부]                   │
│                                              │
│  [저평가/고평가] [추세요약] [리스크] [적정성]│
└──────────────────────────────────────────────┘
  메시지 카드 영역 …
  [입력창]
```

## 백엔드 영향 범위 (예상)

- `ChatContextBuilder.buildFinancialContext()` — 선택된 카테고리/항목 배열을 받아 동적으로 컨텍스트 조립
- `ChatRequest` — `selectedCategories`, `selectedAccounts`, `selectedIndicators`, `analysisTask` 필드 추가
- `StockFinancialService` — 3개년 연간 데이터를 한 번에 반환하는 조회 경로 확인/보강
- 프롬프트 템플릿 — 4개 분석 작업별 프롬프트를 백엔드에 고정 보관 (프론트는 `analysisTask` 식별자만 전송)

## 해결된 질문

1. **기본 체크 상태**: **전부 해제**. 사용자가 하나 이상 체크해야 분석 버튼 활성화.
2. **선택 상태 지속성**: **종목 변경 시 초기화**. 같은 종목 재선택이어도 초기화.
3. **백엔드 API**: **기존 `/api/chat` 확장**. `ChatRequest`에 `analysisTask`, `selectedCategories`, `selectedAccounts`, `selectedIndicators` 추가. 기존 SSE 스트림 재사용.
4. **프롬프트 위치**: **전용 클래스 `FinancialAnalysisPromptTemplate` 분리** (`chatbot/application/prompt/`). 4개 작업별 `render(task, context)` 제공. `ChatContextBuilder`는 조립 책임만.
5. **카테고리만 체크되고 세부 미체크 시**: **카테고리 체크 = 해당 카테고리 전체 세부 항목 주입**. 세부 체크는 "부분 제외" 용도.
6. **적용 범위**: **기존 FINANCIAL 모드를 신규 모드(VALUATION 가칭)로 교체**. '일반'·'지표 해설' 모드는 그대로 둠.

## 확정된 백엔드 API 모양

```
POST /api/chat?userId=X
{
  chatMode: "VALUATION",                   // (기존 FINANCIAL 교체)
  stockCode: "005930",
  analysisTask: "UNDERVALUATION",          // | TREND_SUMMARY | RISK_DIAGNOSIS | INVESTMENT_OPINION
  selectedCategories: ["PROFITABILITY", "VALUATION_METRIC"],
  selectedAccounts:   ["REVENUE", "OP_INCOME"],    // 세부 체크가 있는 경우만
  selectedIndicators: ["ROE", "PER"],              // 세부 체크가 있는 경우만
  messages: [...]
}
```

- 세부 배열이 비어있고 해당 카테고리가 `selectedCategories`에 있으면 → 백엔드가 전체 세부 조회.
- `analysisTask`에 따라 `FinancialAnalysisPromptTemplate`이 다른 시스템 프롬프트 생성.

## 구성 요소 영향 (확정)

| 레이어 | 변경 |
|---|---|
| 프론트 `chat.js` | 모드 라벨 교체, 체크박스 패널 렌더, 종목 변경 시 상태 초기화, 분석 버튼 4개 |
| 프론트 `api.js` | `chat()` 바디에 새 필드 직렬화 |
| `ChatRequest` (백엔드 DTO) | 필드 4개 추가 |
| `ChatContextBuilder` | VALUATION 모드 분기 추가, 선택 기반 컨텍스트 조립 |
| `FinancialAnalysisPromptTemplate` (신규) | 4개 분석 작업 프롬프트 관리 |
| `StockFinancialService` | 3개년 연간 데이터 조회 확인/필요 시 보강 |

## 다음 단계

- `/ce:plan` 로 넘어가 정식 설계 문서 작성 (`.claude/designs/chatbot/financial-selective-analysis/financial-selective-analysis.md`)
- 설계 단계 사전 과제: `FinancialOptionsResponse`와 DART 제공 카테고리 실체 파악