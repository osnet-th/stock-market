# Overseas Stock Price Integration - Brainstorm

**Date:** 2026-03-15
**Status:** Decided

## What We're Building

포트폴리오에 등록된 해외 주식의 현재가를 KIS API를 통해 조회하여 평가금액/수익률을 계산하고, 포트폴리오 총 평가금액에 원화 환산하여 합산하는 기능.

### 현재 상태

- 해외 주식 등록은 가능 (market, exchangeCode, country 필드 저장)
- KIS API 해외 현재가 조회 인프라 존재 (`KisStockPriceAdapter`, `KisOverseasPriceOutput`)
- 하지만 프론트엔드 `loadStockPrices()`에서 해외 주식 현재가를 조회하지 않음
- 해외 주식은 `investedAmount`만 표시되고, 평가금액/수익률 미계산

### 변경 범위

1. **백엔드**: 기존 `/api/stocks/prices` API 확장 (해외 주식 현재가 포함) + 환율 조회 API 연동
2. **프론트엔드**: 해외 주식 현재가/평가금액/수익률 표시, 총 합산 시 원화 환산

### 변경하지 않는 것

- 해외 주식 등록/수정 UI (이미 동작)
- 해외 주식 재무정보 조회 (이번 범위 아님)
- 국내 주식 기존 로직

## Why This Approach

- **KIS API 재활용**: 이미 토큰/인증 인프라가 구축되어 있어 해외 현재가 + 환율 조회 모두 동일 인프라 활용 가능
- **기존 API 확장**: 별도 API를 만들지 않고 기존 `/api/stocks/prices`를 확장하여 국내/해외 통합 조회
- **현지 통화 개별 표시 + 합산 시 원화 환산**: 개별 종목은 실제 매매 통화로 직관적이고, 포트폴리오 합산은 원화로 통일

## Key Decisions

| 결정 사항 | 선택 | 이유 |
|----------|------|------|
| 해외 주식 현재가 소스 | KIS API (기존 인프라) | 토큰/캐시 인프라 재활용 |
| API 구조 | 기존 `/api/stocks/prices` 확장 | 국내/해외 통합 조회, 별도 API 불필요 |
| 개별 종목 가격 표시 | 현지 통화 그대로 (USD, JPY 등) | 매매 통화와 일치하여 직관적 |
| 통화 표시 형식 | 금액 + 통화코드 (195.00 USD) | 통화코드 후위 표기 |
| 포트폴리오 합산 | 원화 환산 후 합산 | 총 평가금액을 원화로 통일 |
| 환율 데이터 소스 | KIS API 환율 조회 | 기존 인프라 활용, 추가 의존성 없음 |

## Technical Notes

### 기존 인프라 활용 가능한 부분

- `KisStockPriceAdapter`: 해외 현재가 조회 로직 이미 구현 (TR_ID: HHDFS00000300)
- `KisOverseasPriceOutput`: 해외 주식 응답 DTO 존재
- `KisStockPriceMapper`: 해외 응답 → 도메인 매핑 존재
- `StockPriceResponse`: 응답 DTO에 marketType, exchangeCode 필드 포함
- Caffeine 캐시: 해외 주식 가격도 동일 패턴으로 캐싱 가능

### 추가 구현 필요한 부분

- KIS API 환율 조회 클라이언트 (통화별 환율)
- 환율 캐싱 (Caffeine, TTL 설정)
- `StockPriceResponse`에 통화코드(currency) 필드 추가
- 프론트엔드: `loadStockPrices()` → 해외 주식도 포함하여 현재가 조회
- 프론트엔드: 해외 주식 개별 표시 시 `195.00 USD` 형식
- 프론트엔드: 포트폴리오 합산 시 환율 적용하여 원화 변환

### 고려 사항

- 해외 주식은 시가/고가/저가 필드가 없음 (KIS API 해외 응답 제한)
- 환율 갱신 주기: Caffeine 캐시 TTL로 관리 (예: 1시간)
- 해외 시장 개장 시간 고려 (미국 23:30~06:00 KST 등) - 폐장 시 마지막 종가 표시