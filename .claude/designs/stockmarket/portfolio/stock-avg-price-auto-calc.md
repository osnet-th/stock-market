# 주식 평균단가 자동 계산 변경

## 작업 리스트

- [x] StockItemAddRequest 변경 (avgBuyPrice/investedAmount 제거, purchasePrice 추가)
- [x] StockItemUpdateRequest 변경 (avgBuyPrice/investedAmount 제거, purchasePrice 추가)
- [x] StockPurchaseRequest DTO 생성 (추가 매수 전용)
- [x] PortfolioItem.createWithStock() 팩토리 메서드 변경 (investedAmount 자동 계산)
- [x] PortfolioItem.addStockPurchase() 추가 매수 행위 메서드 추가
- [x] PortfolioService.addStockPurchase() 추가 매수 메서드 추가
- [x] PortfolioController 추가 매수 엔드포인트 추가 (POST /api/portfolio/items/stock/{itemId}/purchase)

## 변경 요약

기존: 사용자가 `avgBuyPrice`(평균단가)와 `investedAmount`(투자금액)를 직접 입력
변경: 사용자는 `purchasePrice`(매수가)와 `quantity`(수량)만 입력, `avgBuyPrice`와 `investedAmount`는 시스템이 자동 계산

## 변경 상세

### 1. StockDetail (도메인 모델)

필드 변경 없음. `avgBuyPrice`는 유지하되 자동 계산 값으로만 사용.

**계산 로직**
- 최초 등록: `avgBuyPrice = purchasePrice`
- 추가 매수: `avgBuyPrice = (기존수량 × 기존평균단가 + 추가수량 × 추가매수가) / (기존수량 + 추가수량)`
- `investedAmount = avgBuyPrice × quantity` (항상 자동 계산)

### 2. StockItemAddRequest

```
변경 전: itemName, investedAmount, region, memo, subType, stockCode, market, country, quantity, avgBuyPrice, dividendYield
변경 후: itemName, region, memo, subType, stockCode, market, country, quantity, purchasePrice, dividendYield
```
- `investedAmount` 제거 (자동 계산)
- `avgBuyPrice` → `purchasePrice`

### 3. StockItemUpdateRequest

동일하게 `investedAmount` 제거, `avgBuyPrice` → `purchasePrice`

### 4. StockPurchaseRequest (신규)

```json
{
  "quantity": 50,
  "purchasePrice": 55000
}
```

### 5. PortfolioItem 변경

**팩토리 메서드**
```
변경 전: createWithStock(userId, itemName, investedAmount, region, stockDetail)
변경 후: createWithStock(userId, itemName, region, stockDetail)
         → investedAmount = stockDetail.avgBuyPrice × stockDetail.quantity
```

**행위 메서드 추가**
- `addStockPurchase(int additionalQuantity, BigDecimal purchasePrice)`: 가중평균 계산 후 stockDetail, investedAmount 갱신

### 6. PortfolioController 엔드포인트 추가

| API | 메서드 | 설명 |
|---|---|---|
| `POST /api/portfolio/items/stock/{itemId}/purchase` | addStockPurchase | 주식 추가 매수 |

### 7. PortfolioService 메서드 추가

| 메서드 | 설명 |
|---|---|
| `addStockPurchase(userId, itemId, request)` | 추가 매수 (가중평균 계산, investedAmount 자동 갱신) |

## 주의사항

- BigDecimal 가중평균 계산 시 `RoundingMode.HALF_UP`, 소수점 둘째 자리까지 반올림
- 매수 이력은 저장하지 않음 (현재 누적 상태만 유지)
- StockItemEntity 컬럼 변경 없음 (`avg_buy_price`는 계산된 값을 저장)