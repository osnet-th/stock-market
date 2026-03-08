# Service 예시 코드

## PortfolioService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final NewsRepository newsRepository;

    /**
     * 주식 항목 등록
     */
    @Transactional
    public PortfolioItemResponse addStockItem(Long userId, String itemName, BigDecimal investedAmount,
                                               String region, String memo,
                                               String subType, String ticker, String exchange,
                                               Integer quantity, BigDecimal avgBuyPrice, BigDecimal dividendYield) {
        StockDetail detail = new StockDetail(
                subType != null ? StockSubType.valueOf(subType) : null,
                ticker, exchange, quantity, avgBuyPrice, dividendYield
        );
        PortfolioItem item = PortfolioItem.createWithStock(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 채권 항목 등록
     */
    @Transactional
    public PortfolioItemResponse addBondItem(Long userId, String itemName, BigDecimal investedAmount,
                                              String region, String memo,
                                              String subType, LocalDate maturityDate,
                                              BigDecimal couponRate, String creditRating) {
        BondDetail detail = new BondDetail(
                subType != null ? BondSubType.valueOf(subType) : null,
                maturityDate, couponRate, creditRating
        );
        PortfolioItem item = PortfolioItem.createWithBond(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 부동산 항목 등록
     */
    @Transactional
    public PortfolioItemResponse addRealEstateItem(Long userId, String itemName, BigDecimal investedAmount,
                                                    String region, String memo,
                                                    String subType, String address, BigDecimal area) {
        RealEstateDetail detail = new RealEstateDetail(
                subType != null ? RealEstateSubType.valueOf(subType) : null,
                address, area
        );
        PortfolioItem item = PortfolioItem.createWithRealEstate(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 펀드 항목 등록
     */
    @Transactional
    public PortfolioItemResponse addFundItem(Long userId, String itemName, BigDecimal investedAmount,
                                              String region, String memo,
                                              String subType, BigDecimal managementFee) {
        FundDetail detail = new FundDetail(
                subType != null ? FundSubType.valueOf(subType) : null,
                managementFee
        );
        PortfolioItem item = PortfolioItem.createWithFund(
                userId, itemName, investedAmount, Region.valueOf(region), detail);
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 일반 자산 항목 등록 (CRYPTO, GOLD, COMMODITY, CASH, OTHER)
     */
    @Transactional
    public PortfolioItemResponse addGeneralItem(Long userId, String assetType, String itemName,
                                                 BigDecimal investedAmount, String region, String memo) {
        AssetType type = AssetType.valueOf(assetType);
        PortfolioItem item = PortfolioItem.create(userId, itemName, type, investedAmount, Region.valueOf(region));
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 사용자 항목 목록 조회
     */
    public List<PortfolioItemResponse> getItems(Long userId) {
        return portfolioItemRepository.findByUserId(userId).stream()
                .map(PortfolioItemResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 주식 항목 수정
     */
    @Transactional
    public PortfolioItemResponse updateStockItem(Long userId, Long itemId,
                                                  String itemName, BigDecimal investedAmount, String memo,
                                                  String subType, String ticker, String exchange,
                                                  Integer quantity, BigDecimal avgBuyPrice, BigDecimal dividendYield) {
        PortfolioItem item = findUserItem(userId, itemId);
        item.updateItemName(itemName);
        item.updateAmount(investedAmount);
        item.updateMemo(memo);
        StockDetail detail = new StockDetail(
                subType != null ? StockSubType.valueOf(subType) : null,
                ticker, exchange, quantity, avgBuyPrice, dividendYield
        );
        item.updateStockDetail(detail);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 채권 항목 수정
     */
    @Transactional
    public PortfolioItemResponse updateBondItem(Long userId, Long itemId,
                                                 String itemName, BigDecimal investedAmount, String memo,
                                                 String subType, LocalDate maturityDate,
                                                 BigDecimal couponRate, String creditRating) {
        PortfolioItem item = findUserItem(userId, itemId);
        item.updateItemName(itemName);
        item.updateAmount(investedAmount);
        item.updateMemo(memo);
        BondDetail detail = new BondDetail(
                subType != null ? BondSubType.valueOf(subType) : null,
                maturityDate, couponRate, creditRating
        );
        item.updateBondDetail(detail);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 부동산 항목 수정
     */
    @Transactional
    public PortfolioItemResponse updateRealEstateItem(Long userId, Long itemId,
                                                       String itemName, BigDecimal investedAmount, String memo,
                                                       String subType, String address, BigDecimal area) {
        PortfolioItem item = findUserItem(userId, itemId);
        item.updateItemName(itemName);
        item.updateAmount(investedAmount);
        item.updateMemo(memo);
        RealEstateDetail detail = new RealEstateDetail(
                subType != null ? RealEstateSubType.valueOf(subType) : null,
                address, area
        );
        item.updateRealEstateDetail(detail);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 펀드 항목 수정
     */
    @Transactional
    public PortfolioItemResponse updateFundItem(Long userId, Long itemId,
                                                 String itemName, BigDecimal investedAmount, String memo,
                                                 String subType, BigDecimal managementFee) {
        PortfolioItem item = findUserItem(userId, itemId);
        item.updateItemName(itemName);
        item.updateAmount(investedAmount);
        item.updateMemo(memo);
        FundDetail detail = new FundDetail(
                subType != null ? FundSubType.valueOf(subType) : null,
                managementFee
        );
        item.updateFundDetail(detail);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 일반 자산 항목 수정
     */
    @Transactional
    public PortfolioItemResponse updateGeneralItem(Long userId, Long itemId,
                                                    String itemName, BigDecimal investedAmount, String memo) {
        PortfolioItem item = findUserItem(userId, itemId);
        item.updateItemName(itemName);
        item.updateAmount(investedAmount);
        item.updateMemo(memo);
        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 뉴스 수집 토글
     */
    @Transactional
    public void toggleNews(Long userId, Long itemId, boolean enabled) {
        PortfolioItem item = findUserItem(userId, itemId);
        if (enabled) {
            item.enableNews();
        } else {
            item.disableNews();
        }
        portfolioItemRepository.save(item);
    }

    /**
     * 항목 삭제 + 관련 뉴스 Cascade Delete
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        PortfolioItem item = findUserItem(userId, itemId);
        newsRepository.deleteByPurposeAndSourceId(NewsPurpose.PORTFOLIO, item.getId());
        portfolioItemRepository.delete(item);
    }

    private void validateDuplicate(Long userId, PortfolioItem item) {
        if (portfolioItemRepository.existsByUserIdAndItemNameAndAssetType(
                userId, item.getItemName(), item.getAssetType())) {
            throw new IllegalArgumentException("이미 등록된 포트폴리오 항목입니다.");
        }
    }

    private PortfolioItem findUserItem(Long userId, Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오 항목을 찾을 수 없습니다."));
        if (!item.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return item;
    }
}
```

## PortfolioAllocationService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAllocationService {

    private final PortfolioItemRepository portfolioItemRepository;

    /**
     * AssetType별 투자 비중(%) 계산
     */
    public List<AllocationResponse> getAllocation(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByUserId(userId);

        BigDecimal totalAmount = items.stream()
                .map(PortfolioItem::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        Map<AssetType, BigDecimal> amountByType = items.stream()
                .collect(Collectors.groupingBy(
                        PortfolioItem::getAssetType,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioItem::getInvestedAmount, BigDecimal::add)
                ));

        BigDecimal hundred = new BigDecimal("100");

        return amountByType.entrySet().stream()
                .map(entry -> new AllocationResponse(
                        entry.getKey().name(),
                        entry.getKey().getDescription(),
                        entry.getValue(),
                        entry.getValue().multiply(hundred).divide(totalAmount, 2, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());
    }
}
```