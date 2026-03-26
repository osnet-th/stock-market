# Brainstorm: 챗봇 설계 리뷰 — 재무 데이터 제공 방식 재설계

**Date**: 2026-03-25
**Status**: Decided

## What We're Building

포트폴리오 화면에서 두 가지 모드의 AI 챗봇:
1. **포트폴리오 비중 분석 모드**: 현재 자산 배분이 적절한지 질문/응답
2. **종목 재무 분석 모드**: 특정 종목의 재무지표를 기반으로 매수/매도/보유 의견 질문/응답

## Why This Approach

기존 설계의 **재무 데이터 전달 방식**에 문제가 있었음:
- 현재 설계: 클라이언트가 PER/PBR/EPS/ROE를 직접 request body에 담아 전송
- 문제: 서버에 이미 DART 재무지표 조회 API(`StockFinancialController`)가 존재
- 사용자가 수동으로 값을 입력하는 것은 UX 관점에서 부자연스러움

**변경**: 서버가 기존 재무 API를 활용해 자동으로 재무 데이터를 조회하여 Gemini 컨텍스트에 포함

## Key Decisions

| 항목 | 기존 설계 | 변경 후 |
|------|-----------|---------|
| 재무 데이터 전달 | 클라이언트가 body에 직접 포함 | **서버가 기존 DART API로 자동 조회** |
| 종목 범위 | 보유 종목만 (암묵적) | **보유 + 미보유 종목 모두 가능** |
| 조회 지표 범위 | PER, PBR, EPS, ROE만 | **수익성(M210000) + 안정성(M220000) 전체** |
| 종목코드 전달 | 메시지에 포함 (암묵적) | **UI 드롭다운/검색으로 선택, request에 stockCode 포함** |
| 챗 모드 | 단일 통합 챗 | **모드 분리 (포트폴리오 분석 / 종목 재무 분석)** |
| 재무 데이터 호출 방식 | N/A (클라이언트 전달) | **서비스 레이어 DI 직접 호출** (HTTP 자기 호출 아님) |
| 보유 종목 재무 분석 시 | 재무지표만 | **포트폴리오 정보(매수가, 수량, 투자금액)도 포함** |
| 조회 기간 | 미정 | **최신 사업보고서(연간) 기준** |

## Design Impact on Current Document

### 변경이 필요한 항목

1. **`ChatRequest` DTO 변경**
   - `financialData` 필드 제거
   - `stockCode` (String, nullable) 추가 — 재무 분석 모드 시 사용
   - `chatMode` (enum: PORTFOLIO / FINANCIAL) 추가

2. **`FinancialData` DTO 제거**
   - 클라이언트 전달 방식이 아니므로 불필요

3. **`ChatContextBuilder` 변경**
   - 모드별 분기 로직 추가
   - PORTFOLIO 모드: 기존과 동일 (PortfolioAllocationService + PortfolioService)
   - FINANCIAL 모드:
     - 기존 재무 서비스 레이어를 **DI로 직접 호출** (HTTP 자기 호출 아님)
     - 수익성(M210000) + 안정성(M220000) 지표 조회 → 시스템 프롬프트에 포함
     - **보유 종목인 경우**: 포트폴리오 정보(매수 평균가, 수량, 투자금액)도 컨텍스트에 포함
     - 조회 기간: 최신 사업보고서(연간) 기준

4. **`ChatController` 변경**
   - 엔드포인트는 동일 (`POST /api/chat`)
   - request body에 chatMode, stockCode 반영

### 유지되는 항목

- LlmPort 인터페이스, GeminiAdapter, GeminiProperties/Config — 변경 없음
- SSE 스트리밍 방식 — 변경 없음
- 패키지 구조 — 변경 없음 (FinancialData DTO만 제거)

## Open Questions

_없음 — 모든 핵심 결정 완료_