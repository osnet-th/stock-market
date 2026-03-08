# 서비스 예시 코드

## PortfolioService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final NewsRepository newsRepository;

    @Transactional
    public PortfolioItem addItem(Long userId, PortfolioItem item) {
        if (portfolioItemRepository.existsByUserIdAndItemNameAndAssetType(
                userId, item.getItemName(), item.getAssetType())) {
            throw new IllegalArgumentException("이미 등록된 포트폴리오 항목입니다.");
        }
        return portfolioItemRepository.save(item);
    }

    // 삭제 시 뉴스 Cascade Delete (KeywordServiceImpl.deleteKeyword과 동일 패턴)
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        PortfolioItem item = findUserItem(userId, itemId);
        newsRepository.deleteByPurposeAndSourceId(NewsPurpose.PORTFOLIO, item.getId());
        portfolioItemRepository.delete(item);
    }

    // userId 검증으로 다른 사용자 접근 차단
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

    public List<AllocationDto> getAllocation(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByUserId(userId);

        BigDecimal totalAmount = items.stream()
                .map(PortfolioItem::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        // AssetType별 합산
        Map<AssetType, BigDecimal> amountByType = items.stream()
                .collect(Collectors.groupingBy(
                        PortfolioItem::getAssetType,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioItem::getInvestedAmount, BigDecimal::add)
                ));

        BigDecimal hundred = new BigDecimal("100");

        return amountByType.entrySet().stream()
                .map(entry -> new AllocationDto(
                        entry.getKey(),
                        entry.getKey().getDescription(),
                        entry.getValue(),
                        entry.getValue().multiply(hundred).divide(totalAmount, 2, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());
    }
}
```

## PortfolioNewsBatchService

```java
@Service
@RequiredArgsConstructor
public class PortfolioNewsBatchService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final NewsSearchService newsSearchService;
    private final NewsSaveService newsSaveService;

    // KeywordNewsBatchServiceImpl과 동일 패턴
    public void collectNews() {
        List<PortfolioItem> items = portfolioItemRepository.findByNewsEnabled(true);

        for (PortfolioItem item : items) {
            List<NewsSearchResult> results = newsSearchService.search(item.getItemName(), item.getRegion());
            List<NewsSaveRequest> requests = results.stream()
                    .map(result -> new NewsSaveRequest(
                            result.getOriginalUrl(), item.getUserId(), result.getTitle(),
                            result.getContent(), result.getPublishedAt(),
                            item.getId(), item.getRegion()
                    ))
                    .collect(Collectors.toList());
            newsSaveService.saveBatch(requests, NewsPurpose.PORTFOLIO);
        }
    }
}
```