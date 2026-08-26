---
title: "feat: 주식 매도 실입금액 보정"
type: feat
status: planned
date: 2026-05-25
origin: docs/brainstorms/2026-05-25-stock-sale-net-amount-requirements.md
related-issues: [#55]
scope: portfolio sale backend/frontend
---

# feat: 주식 매도 실입금액 보정

## Summary

주식 매도 이력에 총 체결금액과 실입금액을 분리해 저장하고, CASH 입금·매도 이력 대표 금액·기본 실현 손익을 실입금액 기준으로 맞춘다. 기존 판매 단가 × 수량 기반 매도 흐름은 유지하되, 제세금·수수료·기타 비용 또는 사용자가 확인한 최종 입금액을 반영할 수 있게 한다.

## Problem Frame

원본 요구사항: docs/brainstorms/2026-05-25-stock-sale-net-amount-requirements.md

현재 매도 흐름은 `salePrice × quantity × fxRate`로 계산한 금액을 매도 이력의 KRW 판매금액과 CASH 입금액으로 사용한다. 실제 증권사 기록에서는 수수료, 거래세, 농특세, SEC fee, 환전·해외 제비용 등이 차감되어 예수금 입금액이 달라진다. 이번 plan은 자동 세금 계산기를 만들지 않고, 사용자가 외부 기록에서 확인한 실입금액을 SoT로 저장·수정할 수 있게 하여 실제 계좌 흐름과 포트폴리오 기록의 오차를 줄인다.

## Requirements Trace

- origin R1~R5: 총 체결금액과 실입금액을 분리하고, 실입금액을 정산 기준 SoT로 사용한다.
- origin R6~R10: 등록·상세·수정·월별 합계에서 총 체결금액, 차감액, 실입금액을 함께 다룬다.
- origin R11~R13: 체결 기준 손익과 실입금 기준 손익의 의미를 구분하고, 기본 대표 손익은 실입금액 기준으로 둔다.
- origin R14~R16: 차감액이 없는 기존 흐름과 과거 데이터는 총 체결금액 = 실입금액으로 해석한다. 해외 주식은 기존 환율 흐름과 충돌하지 않게 KRW 실입금액을 정산 기준으로 둔다.

## Scope Boundaries

- 포함: 주식 매도 등록, 매도 이력 수정, 매도 이력 삭제의 CASH 정산 기준을 실입금액으로 변경.
- 포함: 매도 이력 응답과 화면에서 총 체결금액·차감액·실입금액·실입금 기준 손익을 구분.
- 포함: 기존 매도 이력의 backward-compatible fallback.
- 제외: 증권사별 세금·수수료 자동 계산.
- 제외: 외부 증권사 거래내역 자동 연동.
- 제외: FIFO, 양도소득세 신고, 연간 세금 리포트.
- 제외: 주식 외 자산 매각/환매/인출 정산 모델.

## Context & Research

### Relevant Code and Patterns

- `StockSaleHistory`는 현재 `salePrice`, `profit`, `profitRate`, `contributionRate`, `salePriceKrw`, `profitKrw`를 계산·보존한다.
- `PortfolioService.persistStockSale`은 `computeSalePriceKrw` 결과를 CASH 입금 기준으로 사용한다.
- `PortfolioService.updateSaleHistory`는 기존 `salePriceKrw`와 새 `salePriceKrw`의 차액으로 CASH 잔액을 조정한다.
- `PortfolioService.deleteSaleHistory`는 기존 입금액을 CASH에서 되돌리는 흐름을 갖는다.
- `StockSaleRequest`, `StockSaleHistoryUpdateRequest`, `AddStockSaleParam`, `UpdateSaleParam`은 현재 수량·판매 단가·사유·메모 중심이다.
- `portfolio.js`는 미리보기·월별 그룹 합계·상세 모달에서 `salePriceKrw`, `profitKrw || profit`을 대표 금액으로 사용한다.
- `portfolio-sale.html`과 `portfolio.html`은 판매 단가, KRW 환산 판매금액, 손익을 보여주지만 실입금액/차감액 입력과 표시는 없다.
- 기존 테스트는 `StockSaleHistoryTest`, `PortfolioServiceAddStockSaleTest`, `PortfolioServiceUpdateSaleHistoryTest`, `PortfolioControllerSaleTest`에 매도 계산·CASH 정산·요청 매핑 검증이 있다.

### Institutional Constraints

- `ARCHITECTURE.md`: presentation → application → domain ← infrastructure 의존 방향을 유지한다.
- `docs/policies/code-convention.md`: 긴 계산 흐름은 작은 helper로 분리하고, 조건 분기는 guard clause로 유지한다.
- `docs/policies/git-worktree.md`: 구현은 기능 개발이므로 documented workflow + 별도 worktree 사용이 기본값이다.
- `compound-engineering.local.md`: Entity는 ID 기반 참조만 허용하고 Lombok 사용을 기본으로 한다.
- 테스트 작성은 사용자 명시 요청 시에만 진행한다. 다만 이 plan은 구현 검증 대상 파일과 시나리오를 명시해 후속 승인 시 바로 추가/수정할 수 있게 한다.

## Key Technical Decisions

| 결정 | 근거 |
|------|------|
| 기본 대표 금액은 실입금액 | #55의 문제는 총 체결금액이 아니라 실제 예수금 입금액과의 오차다. |
| 총 체결금액은 보조 정보로 계속 노출 | 판매 단가 × 수량 기반 거래 규모와 기존 손익 맥락을 보존해야 한다. |
| 토글 스위치는 도입하지 않음 | 목록·월별 합계가 세전/세후 상태에 따라 바뀌면 해석이 흔들린다. 대표값은 고정하고 보조 금액을 함께 보여준다. |
| 서버 SoT는 `netProceedsKrw` | CASH 입금, 월별 합계, 기본 실현 손익을 같은 기준으로 맞추기 위해 최종 정산금 필드를 단일 기준으로 둔다. |
| 입력은 차감액과 실입금액을 모두 허용하되 실입금액 우선 | 사용자는 수수료 합계를 알 수도 있고 최종 입금액만 알 수도 있다. 둘 다 들어오면 외부 기록에 가장 가까운 최종 실입금액을 우선한다. |
| 해외 주식 실입금액은 KRW 기준 | 현재 CASH 입금이 KRW 환산 금액 기준이고, 사용자가 맞추려는 최종 예수금도 원화 계좌 흐름이다. 원통화 비용 세분화는 후속 범위다. |
| 기존 `salePriceKrw`/`profitKrw`는 총 체결 기준으로 유지 | 기존 API 의미를 갑자기 바꾸지 않고, 실입금 기준 필드를 추가해 호환성을 확보한다. |

## High-Level Design

### Amount Semantics

| 개념 | 의미 | 기본 표시 |
|------|------|----------|
| 판매 단가 | 1주당 매도 가격, 기존 입력값 | 상세/수정 |
| 총 체결금액 | 판매 단가 × 수량 × 환율 | 보조 표시 |
| 차감액 | 제세금·수수료·기타 비용 합계 | 보조 표시 |
| 실입금액 | 총 체결금액 - 차감액 또는 사용자가 직접 입력한 최종 입금액 | 대표 표시 |
| 체결 기준 손익 | 총 체결금액 - 매수 원가 | 상세 보조 |
| 실입금 기준 손익 | 실입금액 - 매수 원가 | 대표 손익 |

### Settlement Resolution

```text
grossProceedsKrw = salePrice * quantity * fxRate

if netProceedsKrw is present:
    netProceedsKrw = user value
    deductionAmountKrw = grossProceedsKrw - netProceedsKrw
else if deductionAmountKrw is present:
    deductionAmountKrw = user value
    netProceedsKrw = grossProceedsKrw - deductionAmountKrw
else:
    deductionAmountKrw = 0
    netProceedsKrw = grossProceedsKrw
```

Validation:
- `deductionAmountKrw`는 0 이상이어야 한다.
- `netProceedsKrw`는 0 이상이어야 한다.
- `netProceedsKrw`는 총 체결금액을 초과할 수 없다.
- 차감액이 총 체결금액보다 크면 저장하지 않는다.

## Implementation Units

### Unit 1. Domain/Persistence 금액 모델 확장

**Requirements:** origin R1~R5, R11~R15

**Files:**
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistory.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryEntity.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryRepositoryImpl.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/StockSaleHistoryResponse.java`
- `src/main/resources/db/migration/stock_sale_net_amount_2026_05_25.sql`

**Plan:**
- `StockSaleHistory`에 총 체결 기준 금액은 기존 필드로 유지하고, 실입금 기준 필드를 추가한다.
- 추가 필드는 최소 `deductionAmountKrw`, `netProceedsKrw`, `netProfitKrw`, `netProfitRate`, `netContributionRate`를 포함한다.
- 기존 row는 `deductionAmountKrw=0`, `netProceedsKrw=salePriceKrw`, `netProfitKrw=profitKrw`로 해석되도록 fallback을 둔다.
- Entity 컬럼은 nullable로 추가하고, domain/response 변환 단계에서 null fallback을 적용한다.
- 수익 재계산은 체결 기준과 실입금 기준을 함께 계산하되, 대표 손익 응답은 실입금 기준 필드가 명확히 노출되도록 한다.

**Test targets if approved:**
- `src/test/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistoryTest.java`

**Test scenarios:**
- 차감액이 없으면 `netProceedsKrw == salePriceKrw`, `netProfitKrw == profitKrw`.
- 차감액 500원이 있으면 실입금액과 실입금 기준 손익이 500원 감소한다.
- 직접 입력한 실입금액이 있으면 차감액은 총 체결금액과의 차이로 계산된다.
- 차감액 또는 실입금액이 음수이면 실패한다.
- 실입금액이 총 체결금액보다 크면 실패한다.

### Unit 2. 매도 등록/수정/삭제 정산 기준 변경

**Requirements:** origin R2~R4, R9~R10, R14~R16

**Files:**
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockSaleRequest.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockSaleHistoryUpdateRequest.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/AddStockSaleParam.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/UpdateSaleParam.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java`
- `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`

**Plan:**
- 등록/수정 요청에 `deductionAmountKrw`, `netProceedsKrw` 선택 필드를 추가한다.
- Controller는 새 request 필드를 application param으로 전달한다.
- `PortfolioService.persistStockSale`은 CASH 입금 기준을 기존 `salePriceKrw`에서 `netProceedsKrw`로 전환한다.
- `PortfolioService.updateSaleHistory`는 기존 실입금액과 새 실입금액의 차액으로 CASH를 조정한다.
- `PortfolioService.deleteSaleHistory`는 삭제 시 실입금액 기준으로 CASH 차감을 되돌린다.
- `unrecordedDeposit=true`인 이력은 기존과 동일하게 CASH 정산을 건너뛴다.
- 외화 매도에서 환율이 없으면 기존처럼 KRW 정산을 보류하되, 사용자가 `netProceedsKrw`를 직접 입력한 경우에는 KRW 정산을 허용할지 구현 중 검토한다. 기본 방향은 `netProceedsKrw`가 있으면 CASH 정산 가능이다.

**Test targets if approved:**
- `src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceAddStockSaleTest.java`
- `src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceUpdateSaleHistoryTest.java`
- `src/test/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioControllerSaleTest.java`

**Test scenarios:**
- KRW 매도 등록 시 총 체결금액 100,000원, 차감액 500원이면 CASH는 99,500원 증가한다.
- KRW 매도 등록 시 `netProceedsKrw=99,480`이 들어오면 차감액 입력값보다 `netProceedsKrw`가 우선한다.
- 매도 이력 수정으로 실입금액이 99,500원에서 99,480원으로 바뀌면 CASH는 20원 차감된다.
- 매도 이력 삭제 시 기존 총 체결금액이 아니라 실입금액만큼 CASH에서 차감된다.
- 기존 요청처럼 새 필드가 없으면 기존 테스트 기대값과 동일하게 동작한다.
- Controller request mapping이 새 필드를 application param에 전달한다.

### Unit 3. Frontend 매도 입력/표시 변경

**Requirements:** origin R6~R12, R14

**Files:**
- `src/main/resources/static/js/components/portfolio.js`
- `src/main/resources/static/partials/portfolio-sale.html`
- `src/main/resources/static/partials/portfolio.html`
- `src/main/resources/static/js/api.js`

**Plan:**
- 매도 등록 form state에 `deductionAmountKrw`, `netProceedsKrw`를 추가한다.
- 미리보기는 총 체결금액, 차감액, 실입금액, 실입금 기준 손익을 함께 계산한다.
- 사용자가 차감액을 입력하면 실입금액을 자동 계산하고, 실입금액을 직접 수정하면 차감액을 역산한다.
- 매도 등록 payload와 매도 이력 수정 payload에 새 금액 필드를 포함한다.
- 매도 이력 목록과 월별 합계의 대표 금액은 `netProceedsKrw`와 `netProfitKrw`를 우선 사용한다.
- 상세 모달에는 대표값으로 실입금액/실입금 기준 손익을 보여주고, 총 체결금액/차감액/체결 기준 손익은 보조로 표시한다.
- 토글 스위치는 만들지 않는다. 화면의 대표 기준이 바뀌면 합계 해석이 흔들리기 때문에 기준은 실입금액으로 고정한다.

**Manual verification scenarios:**
- 차감액 없이 매도 등록하면 기존과 동일한 대표 금액이 표시된다.
- 차감액 입력 시 실입금액과 실입금 기준 손익이 즉시 감소한다.
- 실입금액 직접 수정 시 차감액이 총 체결금액과의 차이로 표시된다.
- 매도 이력 탭 월별 합계가 실입금액 기준으로 계산된다.
- 상세/수정 모달에서 실입금액을 수정한 뒤 목록과 CASH 잔액이 갱신된다.

### Unit 4. Compatibility, Backfill, and Verification

**Requirements:** origin R14~R16

**Files:**
- `src/main/resources/db/migration/stock_sale_net_amount_2026_05_25.sql`
- `scripts/run-harness-checks.sh`

**Plan:**
- migration SQL은 기존 `stock_sale_history` row의 새 컬럼을 총 체결금액 기준으로 backfill하는 형태로 작성한다.
- `ddl-auto=update` 환경에서도 null fallback이 동작해야 하므로, 애플리케이션 로직은 migration 실행 여부에 의존하지 않는다.
- 기존 API 클라이언트가 새 필드를 보내지 않아도 매도 등록/수정이 성공해야 한다.
- 문서 산출물은 documented workflow 검사를 통과하도록 유지한다.

**Verification commands:**
- `git diff --check`
- `./gradlew test --tests "*StockSaleHistoryTest" --tests "*PortfolioServiceAddStockSaleTest" --tests "*PortfolioServiceUpdateSaleHistoryTest" --tests "*PortfolioControllerSaleTest"`
- `./gradlew compileJava`
- `scripts/run-harness-checks.sh local-documented`

## Implementation Checklist

- [x] 새 worktree/branch 생성 여부 확인 (`docs/policies/git-worktree.md` 기준)
- [x] `StockSaleHistory` domain model에 실입금 기준 필드와 계산 로직 추가
- [x] `StockSaleHistoryEntity`와 repository mapper에 새 필드 추가
- [x] `StockSaleHistoryResponse`에 총 체결/차감/실입금/실입금 손익 필드 추가
- [x] 기존 데이터 fallback 및 migration SQL 작성
- [x] `StockSaleRequest`/`StockSaleHistoryUpdateRequest`에 새 입력 필드 추가
- [x] `AddStockSaleParam`/`UpdateSaleParam`에 새 입력 필드 추가
- [x] `PortfolioController` request mapping 보강
- [x] `PortfolioService` add/update/delete의 CASH 정산 기준을 실입금액으로 변경
- [x] frontend sale form state와 preview 계산 보강
- [x] 매도 등록/수정 payload에 새 필드 전달
- [x] 매도 이력 목록/월별 합계/상세 모달 대표값을 실입금 기준으로 변경
- [x] 승인된 경우 관련 테스트 보강
- [x] compile/test/harness 검증

## Risks

- 기존 `profitKrw`를 대표 손익으로 쓰는 화면이 남아 있으면 체결 기준과 실입금 기준이 섞일 수 있다.
- 과거 데이터가 null fallback 없이 노출되면 월별 합계가 0 또는 빈 값으로 보일 수 있다.
- 사용자가 차감액과 실입금액을 모두 수정할 때 어떤 값이 우선인지 UI가 불명확하면 오히려 오차가 커질 수 있다.
- 해외 주식에서 환율 자동 조회 실패와 실입금액 직접 입력이 만나는 경우, CASH 정산 허용 조건을 일관되게 처리해야 한다.

## Approval Required Before Work

이 plan은 아래 approval gate를 포함한다. 사용자 승인 후 구현을 시작한다.

- Entity 수정: `StockSaleHistory` 및 `StockSaleHistoryEntity` 필드 추가
- public API 요청/응답 필드 추가: 매도 등록/수정 request와 매도 이력 response
- 비즈니스 로직 동작 변경: CASH 입금/수정/삭제 정산 기준을 총 체결금액에서 실입금액으로 변경
- 프론트 대표 표시 기준 변경: 매도 이력 목록·상세·월별 합계를 실입금액 중심으로 표시
