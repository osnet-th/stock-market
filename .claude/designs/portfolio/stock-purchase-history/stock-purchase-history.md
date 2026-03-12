# 주식 매수 이력 관리 설계

## 작업 리스트

### 백엔드
- [x] StockPurchaseHistory 도메인 모델 생성
- [x] StockPurchaseHistoryEntity JPA Entity 생성
- [x] StockPurchaseHistoryRepository 인터페이스 + 구현체 생성
- [x] PortfolioItem 도메인 모델에 매수이력 기반 재계산 메서드 추가
- [x] PortfolioService에 매수이력 CRUD 메서드 추가 (조회, 수정, 삭제)
- [x] PortfolioService의 addStockPurchase 변경 (이력 저장 연동)
- [x] PortfolioService의 addStockItem 변경 (최초 등록 시 이력 생성)
- [x] StockPurchaseHistoryResponse DTO 생성
- [x] PortfolioController에 매수이력 API 추가 (조회, 수정, 삭제)

### 프론트엔드
- [x] API 클라이언트에 매수이력 API 메서드 추가
- [x] 매수이력 조회/수정/삭제 UI 구현 (추가매수 모달 내 이력 섹션)

## 배경

현재 추가 매수 시 가중평균으로 합산만 하고 개별 매수 이력이 남지 않아, 잘못 입력한 매수를 수정/삭제할 수 없음.

## 핵심 결정

- **별도 테이블**: `stock_purchase_history` 테이블로 매수 건별 이력 관리
- **stock_detail은 파생 데이터**: 매수이력의 합산 결과를 stock_detail의 quantity/avgBuyPrice에 반영 (이력이 원본, stock_detail은 캐시)
- **이력 수정/삭제 시 재계산**: 이력 변경 시 전체 이력을 기반으로 quantity/avgBuyPrice 재계산
- **최초 등록도 이력**: addStockItem 시 첫 번째 매수 이력 자동 생성

## 구현

### StockPurchaseHistory 도메인 모델
위치: `portfolio/domain/model/StockPurchaseHistory.java`

[예시 코드](./examples/domain-model-example.md)

### StockPurchaseHistoryEntity
위치: `portfolio/infrastructure/persistence/StockPurchaseHistoryEntity.java`

- `stock_purchase_history` 테이블
- FK: `portfolio_item_id` → `portfolio_item.id`
- 컬럼: id, portfolio_item_id, quantity, purchase_price, purchased_at, memo, created_at

[예시 코드](./examples/infrastructure-example.md)

### StockPurchaseHistoryRepository
위치: `portfolio/domain/repository/StockPurchaseHistoryRepository.java`

- `findByPortfolioItemId(Long itemId)`: 특정 종목의 매수이력 조회
- `save(StockPurchaseHistory history)`: 저장
- `findById(Long id)`: 단건 조회
- `delete(StockPurchaseHistory history)`: 삭제

### PortfolioItem 도메인 변경
위치: `portfolio/domain/model/PortfolioItem.java`

- `recalculateFromPurchaseHistories(List<StockPurchaseHistory>)`: 이력 기반 재계산
  - 전체 수량 합산, 가중평균단가 계산, investedAmount 갱신

[예시 코드](./examples/domain-model-example.md)

### PortfolioService 변경
위치: `portfolio/application/PortfolioService.java`

- `addStockItem` 변경: 최초 등록 시 매수이력 자동 생성
- `addStockPurchase` 변경: 이력 저장 후 재계산
- `getPurchaseHistories(userId, itemId)`: 매수이력 조회
- `updatePurchaseHistory(userId, itemId, historyId, quantity, purchasePrice)`: 이력 수정 + 재계산
- `deletePurchaseHistory(userId, itemId, historyId)`: 이력 삭제 + 재계산

[예시 코드](./examples/application-service-example.md)

### PortfolioController API 추가
위치: `portfolio/presentation/PortfolioController.java`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/portfolio/items/stock/{itemId}/purchases?userId=` | 매수이력 조회 |
| PUT | `/api/portfolio/items/stock/{itemId}/purchases/{historyId}?userId=` | 매수이력 수정 |
| DELETE | `/api/portfolio/items/stock/{itemId}/purchases/{historyId}?userId=` | 매수이력 삭제 |

[예시 코드](./examples/presentation-example.md)

### 프론트엔드
위치: `static/js/app.js`, `static/js/api.js`, `static/index.html`

- 추가매수 모달에 매수이력 목록 표시
- 각 이력에 수정/삭제 버튼
- 이력 수정 시 인라인 편집
- 이력 삭제 시 confirm 후 삭제 + 재계산된 결과 반영

[예시 코드](./examples/frontend-example.md)

## 주의사항

- 매수이력이 0건이 되면 안 됨 (최소 1건 유지, 마지막 이력 삭제 시 경고)
- 기존 데이터(이력 없는 기존 종목)는 이력 조회 시 빈 배열 반환, 수정 버튼으로 직접 수정 가능 (기존 방식 유지)
- StockPurchaseHistoryEntity는 PortfolioItemEntity와 연관관계 없이 ID 참조만 사용 (CLAUDE.md Entity 규칙 준수)