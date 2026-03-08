# Controller & DTO 예시 코드

## PortfolioController (자산 유형별 등록 엔드포인트)

```java
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;

    // 주식 등록
    @PostMapping("/items/stock")
    public ResponseEntity<PortfolioItemResponse> addStockItem(
            @RequestParam Long userId,
            @RequestBody StockItemAddRequest request) {
        PortfolioItem item = request.toDomain(userId);
        PortfolioItem saved = portfolioService.addItem(userId, item);
        return ResponseEntity.ok(PortfolioItemResponse.from(saved));
    }

    // 채권 등록
    @PostMapping("/items/bond")
    public ResponseEntity<PortfolioItemResponse> addBondItem(
            @RequestParam Long userId,
            @RequestBody BondItemAddRequest request) {
        PortfolioItem item = request.toDomain(userId);
        PortfolioItem saved = portfolioService.addItem(userId, item);
        return ResponseEntity.ok(PortfolioItemResponse.from(saved));
    }

    // 부동산 등록
    @PostMapping("/items/real-estate")
    public ResponseEntity<PortfolioItemResponse> addRealEstateItem(
            @RequestParam Long userId,
            @RequestBody RealEstateItemAddRequest request) {
        PortfolioItem item = request.toDomain(userId);
        PortfolioItem saved = portfolioService.addItem(userId, item);
        return ResponseEntity.ok(PortfolioItemResponse.from(saved));
    }

    // 펀드 등록
    @PostMapping("/items/fund")
    public ResponseEntity<PortfolioItemResponse> addFundItem(
            @RequestParam Long userId,
            @RequestBody FundItemAddRequest request) {
        PortfolioItem item = request.toDomain(userId);
        PortfolioItem saved = portfolioService.addItem(userId, item);
        return ResponseEntity.ok(PortfolioItemResponse.from(saved));
    }

    // 일반 자산 등록 (CRYPTO, GOLD, COMMODITY, CASH, OTHER)
    @PostMapping("/items/general")
    public ResponseEntity<PortfolioItemResponse> addGeneralItem(
            @RequestParam Long userId,
            @RequestBody GeneralItemAddRequest request) {
        PortfolioItem item = request.toDomain(userId);
        PortfolioItem saved = portfolioService.addItem(userId, item);
        return ResponseEntity.ok(PortfolioItemResponse.from(saved));
    }

    // 주식 수정
    @PutMapping("/items/stock/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateStockItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockItemUpdateRequest request) {
        PortfolioItem updated = portfolioService.updateStockItem(userId, itemId, request);
        return ResponseEntity.ok(PortfolioItemResponse.from(updated));
    }

    // 채권 수정
    @PutMapping("/items/bond/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateBondItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody BondItemUpdateRequest request) {
        PortfolioItem updated = portfolioService.updateBondItem(userId, itemId, request);
        return ResponseEntity.ok(PortfolioItemResponse.from(updated));
    }

    // 부동산 수정 — real-estate, fund도 동일 패턴
    // 일반 자산 수정
    @PutMapping("/items/general/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateGeneralItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody GeneralItemUpdateRequest request) {
        PortfolioItem updated = portfolioService.updateGeneralItem(userId, itemId, request);
        return ResponseEntity.ok(PortfolioItemResponse.from(updated));
    }

    // 조회/삭제는 공통
    @GetMapping("/items")
    public ResponseEntity<List<PortfolioItemResponse>> getItems(@RequestParam Long userId) { ... }

    @PatchMapping("/items/{itemId}/news")
    public ResponseEntity<Void> toggleNews(...) { ... }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(...) { ... }

    @GetMapping("/allocation")
    public ResponseEntity<List<AllocationResponse>> getAllocation(@RequestParam Long userId) { ... }
}
```

## 타입별 등록 Request DTO

```java
// 주식 등록 — 공통 필드 + 주식 고유 필드가 플랫하게 존재
@Getter
public class StockItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;       // ETF, INDIVIDUAL
    private String ticker;        // 005930
    private String exchange;      // KRX, NYSE
    private Integer quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal dividendYield;

    public PortfolioItem toDomain(Long userId) {
        StockDetail detail = new StockDetail(
                subType != null ? StockSubType.valueOf(subType) : null,
                ticker, exchange, quantity, avgBuyPrice, dividendYield
        );
        PortfolioItem item = PortfolioItem.createWithStock(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) { item.updateMemo(memo); }
        return item;
    }
}

// 채권 등록
@Getter
public class BondItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;       // GOVERNMENT, CORPORATE
    private LocalDate maturityDate;
    private BigDecimal couponRate;
    private String creditRating;

    public PortfolioItem toDomain(Long userId) {
        BondDetail detail = new BondDetail(
                subType != null ? BondSubType.valueOf(subType) : null,
                maturityDate, couponRate, creditRating
        );
        PortfolioItem item = PortfolioItem.createWithBond(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) { item.updateMemo(memo); }
        return item;
    }
}

// 부동산 등록
@Getter
public class RealEstateItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;       // APARTMENT, OFFICETEL, LAND, COMMERCIAL
    private String address;
    private BigDecimal area;

    public PortfolioItem toDomain(Long userId) {
        RealEstateDetail detail = new RealEstateDetail(
                subType != null ? RealEstateSubType.valueOf(subType) : null,
                address, area
        );
        PortfolioItem item = PortfolioItem.createWithRealEstate(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) { item.updateMemo(memo); }
        return item;
    }
}

// 펀드 등록
@Getter
public class FundItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;       // EQUITY_FUND, BOND_FUND, MIXED_FUND
    private BigDecimal managementFee;

    public PortfolioItem toDomain(Long userId) {
        FundDetail detail = new FundDetail(
                subType != null ? FundSubType.valueOf(subType) : null,
                managementFee
        );
        PortfolioItem item = PortfolioItem.createWithFund(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) { item.updateMemo(memo); }
        return item;
    }
}

// 일반 자산 등록 (CRYPTO, GOLD, COMMODITY, CASH, OTHER)
@Getter
public class GeneralItemAddRequest {
    private String assetType;     // CRYPTO, GOLD, COMMODITY, CASH, OTHER
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;

    public PortfolioItem toDomain(Long userId) {
        AssetType type = AssetType.valueOf(assetType);
        PortfolioItem item = PortfolioItem.create(
                userId, itemName, type, investedAmount, Region.valueOf(region));
        if (memo != null) { item.updateMemo(memo); }
        return item;
    }
}
```

## 타입별 수정 Request DTO

```java
// 주식 수정 — AddRequest와 동일 구조
@Getter
public class StockItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private String ticker;
    private String exchange;
    private Integer quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal dividendYield;
}

// 채권 수정
@Getter
public class BondItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private LocalDate maturityDate;
    private BigDecimal couponRate;
    private String creditRating;
}

// 부동산 수정
@Getter
public class RealEstateItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private String address;
    private BigDecimal area;
}

// 펀드 수정
@Getter
public class FundItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private BigDecimal managementFee;
}

// 일반 자산 수정 (CRYPTO, GOLD, COMMODITY, CASH, OTHER)
@Getter
public class GeneralItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
}
```

## PortfolioItemResponse (조회 — 기존과 동일)

```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioItemResponse {
    private final Long id;
    private final String assetType;
    private final String itemName;
    private final BigDecimal investedAmount;
    private final boolean newsEnabled;
    private final String region;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final StockDetailResponse stockDetail;
    private final BondDetailResponse bondDetail;
    private final RealEstateDetailResponse realEstateDetail;
    private final FundDetailResponse fundDetail;

    public static PortfolioItemResponse from(PortfolioItem item) {
        return new PortfolioItemResponse(
                item.getId(), item.getAssetType().name(), item.getItemName(),
                item.getInvestedAmount(), item.isNewsEnabled(), item.getRegion().name(),
                item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                item.getStockDetail() != null ? StockDetailResponse.from(item.getStockDetail()) : null,
                item.getBondDetail() != null ? BondDetailResponse.from(item.getBondDetail()) : null,
                item.getRealEstateDetail() != null ? RealEstateDetailResponse.from(item.getRealEstateDetail()) : null,
                item.getFundDetail() != null ? FundDetailResponse.from(item.getFundDetail()) : null
        );
    }
}
```

## AllocationResponse

```java
@Getter
@RequiredArgsConstructor
public class AllocationResponse {
    private final String assetType;
    private final String assetTypeName;
    private final BigDecimal totalAmount;
    private final BigDecimal percentage;

    public static AllocationResponse from(AllocationDto dto) {
        return new AllocationResponse(
                dto.getAssetType().name(),
                dto.getAssetTypeName(),
                dto.getTotalAmount(),
                dto.getPercentage()
        );
    }
}
```