# 포트폴리오 레이어 경계 수정

## 작업 리스트

- [x] AddRequest DTO에서 `toDomain()` 제거, Service가 도메인 모델 생성하도록 변경
- [x] PortfolioService 반환 타입을 `PortfolioItem` → `PortfolioItemResponse`로 변경
- [x] PortfolioService에서 presentation DTO import 제거 (UpdateRequest → 개별 파라미터)
- [x] PortfolioItemResponse의 `from(PortfolioItem)` → `from()`을 application 계층으로 이동
- [x] DetailResponse DTO들의 domain 모델 의존 제거
- [x] Controller에서 domain 모델 import 완전 제거
- [x] PortfolioAllocationService 반환 타입 정리

## 배경

ARCHITECTURE.md 규칙 위반 3건:
- **presentation 금지**: domain 모델 직접 노출 → Controller가 `PortfolioItem` 직접 사용 중
- **DTO 변환 흐름**: `Request DTO → Application DTO → Domain Model → Entity` → 현재 Request DTO가 직접 Domain Model 생성
- **DTO/Entity 경계**: Domain Model은 JSON 직렬화 금지 → `PortfolioItemResponse.from(PortfolioItem)`이 presentation에서 domain 모델 직접 참조

## 핵심 결정

- **Service가 Response DTO 반환**: Application 계층에서 Domain Model → Response DTO 변환 수행. 별도 Application DTO를 두면 과도하므로, Service가 Response DTO를 직접 생성하여 반환
- **Response DTO를 application 계층으로 이동**: `presentation/dto/` → `application/dto/`로 이동. Controller는 Service가 반환한 DTO를 그대로 전달
- **AddRequest의 toDomain() 제거**: Service가 Request DTO의 개별 필드를 받아 도메인 모델 생성. Request DTO는 순수 데이터 전달 객체로만 사용
- **UpdateRequest도 개별 파라미터로 전달**: Service가 presentation DTO에 의존하지 않도록, Controller에서 필드를 추출하여 Service에 전달

## 구현

### 변경 흐름 (Before → After)

**등록 (Before)**
```
Controller → request.toDomain(userId) → PortfolioItem → Service.addItem(userId, PortfolioItem) → PortfolioItem → PortfolioItemResponse.from(item)
```

**등록 (After)**
```
Controller → Service.addStockItem(userId, request 필드들) → PortfolioItemResponse
```

**수정 (Before)**
```
Controller → Service.updateStockItem(userId, itemId, StockItemUpdateRequest) → PortfolioItem → PortfolioItemResponse.from(item)
```

**수정 (After)**
```
Controller → Service.updateStockItem(userId, itemId, 개별 필드들) → PortfolioItemResponse
```

### Response DTO 이동

| 파일 | Before | After |
|---|---|---|
| PortfolioItemResponse | `presentation/dto/` | `application/dto/` |
| StockDetailResponse | `presentation/dto/` | `application/dto/` |
| BondDetailResponse | `presentation/dto/` | `application/dto/` |
| RealEstateDetailResponse | `presentation/dto/` | `application/dto/` |
| FundDetailResponse | `presentation/dto/` | `application/dto/` |
| AllocationResponse | `presentation/dto/` | `application/dto/` |

### PortfolioService 변경

**등록 메서드**: 타입별 메서드로 분리, 각 메서드가 개별 필드를 받아 도메인 모델 생성 후 Response DTO 반환

| 메서드 | 파라미터 | 반환 |
|---|---|---|
| `addStockItem(userId, itemName, investedAmount, region, memo, subType, ticker, exchange, quantity, avgBuyPrice, dividendYield)` | 개별 필드 | `PortfolioItemResponse` |
| `addBondItem(userId, itemName, investedAmount, region, memo, subType, maturityDate, couponRate, creditRating)` | 개별 필드 | `PortfolioItemResponse` |
| `addRealEstateItem(userId, itemName, investedAmount, region, memo, subType, address, area)` | 개별 필드 | `PortfolioItemResponse` |
| `addFundItem(userId, itemName, investedAmount, region, memo, subType, managementFee)` | 개별 필드 | `PortfolioItemResponse` |
| `addGeneralItem(userId, assetType, itemName, investedAmount, region, memo)` | 개별 필드 | `PortfolioItemResponse` |

**수정 메서드**: 동일하게 개별 필드로 전달

**조회 메서드**:
| 메서드 | 반환 |
|---|---|
| `getItems(userId)` | `List<PortfolioItemResponse>` |

**PortfolioAllocationService**:
| 메서드 | 반환 |
|---|---|
| `getAllocation(userId)` | `List<AllocationResponse>` |

[Service 예시 코드](./examples/service-example.md)

### Controller 변경

- domain 모델 import 완전 제거
- Service 호출 후 반환값을 그대로 전달

[Controller 예시 코드](./examples/controller-example.md)

### AddRequest DTO 변경

- `toDomain()` 메서드 제거
- 순수 데이터 전달 객체로만 사용

## 주의사항

- Response DTO 이동 시 import 경로 변경 (`presentation.dto` → `application.dto`)
- `PortfolioItemResponse.from(PortfolioItem)`의 domain 모델 의존은 application 계층으로 이동하면 정당한 의존 방향 (`application → domain`)
- 기존 `AllocationDto`는 `AllocationResponse`와 역할이 중복되므로, `AllocationResponse`로 통합하여 `application/dto/`에 배치
- Service 메서드의 파라미터가 많아지지만, 이는 각 자산 유형의 고유 필드가 본질적으로 다르기 때문에 불가피. 명확한 타입 안전성 확보
