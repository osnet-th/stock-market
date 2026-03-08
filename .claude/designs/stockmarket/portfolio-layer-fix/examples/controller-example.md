# Controller 예시 코드

## PortfolioController

```java
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;

    // --- 등록 ---

    @PostMapping("/items/stock")
    public ResponseEntity<PortfolioItemResponse> addStockItem(
            @RequestParam Long userId,
            @RequestBody StockItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addStockItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getTicker(), request.getExchange(),
                request.getQuantity(), request.getAvgBuyPrice(), request.getDividendYield());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/bond")
    public ResponseEntity<PortfolioItemResponse> addBondItem(
            @RequestParam Long userId,
            @RequestBody BondItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addBondItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getMaturityDate(),
                request.getCouponRate(), request.getCreditRating());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/real-estate")
    public ResponseEntity<PortfolioItemResponse> addRealEstateItem(
            @RequestParam Long userId,
            @RequestBody RealEstateItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addRealEstateItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getAddress(), request.getArea());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/fund")
    public ResponseEntity<PortfolioItemResponse> addFundItem(
            @RequestParam Long userId,
            @RequestBody FundItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addFundItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getManagementFee());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/general")
    public ResponseEntity<PortfolioItemResponse> addGeneralItem(
            @RequestParam Long userId,
            @RequestBody GeneralItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addGeneralItem(
                userId, request.getAssetType(), request.getItemName(),
                request.getInvestedAmount(), request.getRegion(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    // --- 수정 ---

    @PutMapping("/items/stock/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateStockItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateStockItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getTicker(), request.getExchange(),
                request.getQuantity(), request.getAvgBuyPrice(), request.getDividendYield());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/bond/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateBondItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody BondItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateBondItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getMaturityDate(),
                request.getCouponRate(), request.getCreditRating());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/real-estate/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateRealEstateItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody RealEstateItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateRealEstateItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getAddress(), request.getArea());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/fund/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateFundItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody FundItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateFundItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getManagementFee());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/general/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateGeneralItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody GeneralItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateGeneralItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    // --- 조회/삭제 (공통) ---

    @GetMapping("/items")
    public ResponseEntity<List<PortfolioItemResponse>> getItems(@RequestParam Long userId) {
        List<PortfolioItemResponse> responses = portfolioService.getItems(userId);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/items/{itemId}/news")
    public ResponseEntity<Void> toggleNews(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody PortfolioNewsToggleRequest request) {
        portfolioService.toggleNews(userId, itemId, request.isEnabled());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        portfolioService.deleteItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/allocation")
    public ResponseEntity<List<AllocationResponse>> getAllocation(@RequestParam Long userId) {
        List<AllocationResponse> responses = portfolioAllocationService.getAllocation(userId);
        return ResponseEntity.ok(responses);
    }
}
```

## 핵심 변경점

- Controller에서 `PortfolioItem` (domain 모델) import 완전 제거
- Controller는 Request DTO에서 필드를 추출하여 Service에 개별 파라미터로 전달
- Service가 반환한 Response DTO를 그대로 전달 (변환 로직 없음)
- `PortfolioItemResponse`, `AllocationResponse`는 `application/dto/` 패키지에서 import