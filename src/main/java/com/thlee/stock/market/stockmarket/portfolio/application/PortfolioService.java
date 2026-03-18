package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.application.NewsCleanupService;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockPurchaseHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.*;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.BondSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.RealEstateSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.CashStockLinkRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 항목 CRUD 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final StockPurchaseHistoryRepository purchaseHistoryRepository;
    private final CashStockLinkRepository cashStockLinkRepository;
    private final NewsCleanupService newsCleanupService;

    /**
     * 주식 항목 등록 (investedAmount = quantity × purchasePrice 자동 계산)
     */
    @Transactional
    public PortfolioItemResponse addStockItem(Long userId, String itemName,
                                               String region, String memo,
                                               String subType, String stockCode, String market,
                                               String exchangeCode, String country,
                                               Integer quantity, BigDecimal purchasePrice, BigDecimal dividendYield,
                                               String priceCurrency, BigDecimal investedAmountKrw,
                                               Long cashItemId) {
        StockDetail detail = new StockDetail(
                subType != null ? StockSubType.valueOf(subType) : null,
                stockCode, market, exchangeCode, country, quantity, purchasePrice, dividendYield,
                priceCurrency != null ? PriceCurrency.valueOf(priceCurrency) : PriceCurrency.KRW,
                investedAmountKrw
        );
        PortfolioItem item = PortfolioItem.createWithStock(
                userId, itemName, Region.valueOf(region), detail);
        if (memo != null) {
            item.updateMemo(memo);
        }
        validateDuplicate(userId, item);

        // CASH 연결 시 잔액 검증을 먼저 수행
        PortfolioItem cashItem = null;
        BigDecimal deductionAmount = null;
        if (cashItemId != null) {
            cashItem = findAndValidateCashItem(userId, cashItemId);
            deductionAmount = calculateDeductionAmount(detail.getPriceCurrency(), quantity, purchasePrice, investedAmountKrw);
            cashItem.deductAmount(deductionAmount);
        }

        PortfolioItem saved = portfolioItemRepository.save(item);

        // 최초 매수이력 생성
        StockPurchaseHistory history = StockPurchaseHistory.create(
                saved.getId(), quantity, purchasePrice, LocalDate.now(), null);
        purchaseHistoryRepository.save(history);

        // CASH 연결 저장
        if (cashItem != null) {
            CashStockLink link = CashStockLink.create(cashItemId, saved.getId());
            cashStockLinkRepository.save(link);
            portfolioItemRepository.save(cashItem);
        }

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
     * 사용자 항목 목록 조회 (linkedCashItemId 배치 조회 포함)
     */
    public List<PortfolioItemResponse> getItems(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByUserId(userId);

        // STOCK 항목의 linkedCashItemId 배치 조회
        List<Long> stockItemIds = items.stream()
                .filter(i -> i.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getId)
                .collect(Collectors.toList());

        Map<Long, Long> linkMap = Map.of();
        if (!stockItemIds.isEmpty()) {
            linkMap = cashStockLinkRepository.findByStockItemIdIn(stockItemIds).stream()
                    .collect(Collectors.toMap(CashStockLink::getStockItemId, CashStockLink::getCashItemId));
        }

        Map<Long, Long> finalLinkMap = linkMap;
        return items.stream()
                .map(item -> PortfolioItemResponse.from(item, finalLinkMap.get(item.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 주식 항목 수정 (investedAmount = quantity × purchasePrice 자동 계산)
     */
    @Transactional
    public PortfolioItemResponse updateStockItem(Long userId, Long itemId,
                                                  String itemName, String memo,
                                                  String subType, String stockCode, String market,
                                                  String exchangeCode, String country,
                                                  Integer quantity, BigDecimal purchasePrice, BigDecimal dividendYield,
                                                  String priceCurrency, BigDecimal investedAmountKrw,
                                                  Long cashItemId, boolean deductOnLink) {
        PortfolioItem item = findUserItem(userId, itemId);

        // 수정 전 금액 스냅샷
        BigDecimal oldInvestedAmount = item.getInvestedAmount();
        BigDecimal oldInvestedAmountKrw = item.getStockDetail() != null ? item.getStockDetail().getInvestedAmountKrw() : null;
        PriceCurrency oldPriceCurrency = item.getStockDetail() != null ? item.getStockDetail().getPriceCurrency() : PriceCurrency.KRW;

        item.updateItemName(itemName);
        item.updateMemo(memo);
        StockDetail detail = new StockDetail(
                subType != null ? StockSubType.valueOf(subType) : null,
                stockCode, market, exchangeCode, country, quantity, purchasePrice, dividendYield,
                priceCurrency != null ? PriceCurrency.valueOf(priceCurrency) : PriceCurrency.KRW,
                investedAmountKrw
        );
        item.updateStockDetail(detail);

        // CASH 연결 변경/해제 처리
        handleCashLinkChange(userId, itemId, cashItemId, item, oldInvestedAmount, oldInvestedAmountKrw, oldPriceCurrency, deductOnLink);

        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 주식 추가 매수 - 이력 저장 후 재계산
     */
    @Transactional
    public PortfolioItemResponse addStockPurchase(Long userId, Long itemId,
                                                   Integer quantity, BigDecimal purchasePrice,
                                                   BigDecimal investedAmountKrw) {
        PortfolioItem item = findUserItem(userId, itemId);

        // 기존 매수이력이 없으면 현재 보유분으로 초기 이력 자동 생성
        List<StockPurchaseHistory> existingHistories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
        if (existingHistories.isEmpty() && item.getStockDetail() != null) {
            StockPurchaseHistory initialHistory = StockPurchaseHistory.create(
                    itemId, item.getStockDetail().getQuantity(),
                    item.getStockDetail().getAvgBuyPrice(), LocalDate.now(), "기존 보유분");
            purchaseHistoryRepository.save(initialHistory);
        }

        // 연결된 CASH가 있으면 차감
        deductCashBalance(itemId, item, quantity, purchasePrice, investedAmountKrw);

        // 매수이력 저장
        StockPurchaseHistory history = StockPurchaseHistory.create(
                itemId, quantity, purchasePrice, LocalDate.now(), null);
        purchaseHistoryRepository.save(history);

        // 전체 이력 기반 재계산
        List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
        item.recalculateFromPurchaseHistories(histories);

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
     * 매수이력 조회
     */
    public List<StockPurchaseHistoryResponse> getPurchaseHistories(Long userId, Long itemId) {
        findUserItem(userId, itemId); // 권한 검증
        return purchaseHistoryRepository.findByPortfolioItemId(itemId).stream()
                .map(StockPurchaseHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 매수이력 수정 + 재계산 + CASH 차액 조정
     */
    @Transactional
    public PortfolioItemResponse updatePurchaseHistory(Long userId, Long itemId, Long historyId,
                                                        Integer quantity, BigDecimal purchasePrice,
                                                        LocalDate purchasedAt, String memo) {
        PortfolioItem item = findUserItem(userId, itemId);

        StockPurchaseHistory history = purchaseHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("매수 이력을 찾을 수 없습니다."));
        if (!history.getPortfolioItemId().equals(itemId)) {
            throw new IllegalArgumentException("해당 항목의 매수 이력이 아닙니다.");
        }

        // 재계산 전 금액 스냅샷
        BigDecimal oldAmount = item.getInvestedAmount();

        history.update(quantity, purchasePrice, purchasedAt, memo);
        purchaseHistoryRepository.save(history);

        // 전체 이력 기반 재계산
        List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
        item.recalculateFromPurchaseHistories(histories);

        // CASH 차액 조정
        adjustCashForAmountChange(itemId, oldAmount, item.getInvestedAmount());

        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 매수이력 삭제 + 재계산 + CASH 차액 복원
     */
    @Transactional
    public PortfolioItemResponse deletePurchaseHistory(Long userId, Long itemId, Long historyId) {
        PortfolioItem item = findUserItem(userId, itemId);

        StockPurchaseHistory history = purchaseHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("매수 이력을 찾을 수 없습니다."));
        if (!history.getPortfolioItemId().equals(itemId)) {
            throw new IllegalArgumentException("해당 항목의 매수 이력이 아닙니다.");
        }

        // 최소 1건 검증
        List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
        if (histories.size() <= 1) {
            throw new IllegalArgumentException("매수 이력은 최소 1건 이상이어야 합니다.");
        }

        // 재계산 전 금액 스냅샷
        BigDecimal oldAmount = item.getInvestedAmount();

        purchaseHistoryRepository.delete(history);

        // 삭제 후 남은 이력으로 재계산
        histories.removeIf(h -> h.getId().equals(historyId));
        item.recalculateFromPurchaseHistories(histories);

        // CASH 차액 복원
        adjustCashForAmountChange(itemId, oldAmount, item.getInvestedAmount());

        PortfolioItem saved = portfolioItemRepository.save(item);
        return PortfolioItemResponse.from(saved);
    }

    /**
     * 항목 삭제 + 매수이력 삭제 + 관련 뉴스 출처 매핑 삭제 + orphan 뉴스 정리
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId, boolean restoreCash, BigDecimal restoreAmount) {
        PortfolioItem item = findUserItem(userId, itemId);

        if (item.getAssetType() == AssetType.CASH) {
            validateCashDeletion(itemId);
        }

        if (item.getAssetType() == AssetType.STOCK) {
            handleStockDeletion(item, restoreCash, restoreAmount);
        }

        purchaseHistoryRepository.deleteByPortfolioItemId(itemId);
        newsCleanupService.deleteSourceAndCleanOrphans(NewsPurpose.PORTFOLIO, item.getId());
        portfolioItemRepository.delete(item);
    }

    // ──────────────────────────────────────────────────────────────────
    // CASH 연동 private 헬퍼 메서드
    // ──────────────────────────────────────────────────────────────────

    /**
     * cashItemId 3단계 검증: 존재 + 소유권 + CASH 타입
     */
    private PortfolioItem findAndValidateCashItem(Long userId, Long cashItemId) {
        PortfolioItem cashItem = portfolioItemRepository.findById(cashItemId)
                .orElseThrow(() -> new IllegalArgumentException("원화 항목을 찾을 수 없습니다."));
        if (!cashItem.getUserId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        if (cashItem.getAssetType() != AssetType.CASH) {
            throw new IllegalArgumentException("원화(CASH) 타입이 아닙니다.");
        }
        return cashItem;
    }

    /**
     * 차감 금액 계산: KRW 주식은 quantity × purchasePrice, 외화 주식은 investedAmountKrw
     */
    private BigDecimal calculateDeductionAmount(PriceCurrency currency, Integer quantity,
                                                 BigDecimal purchasePrice, BigDecimal investedAmountKrw) {
        if (currency != null && currency != PriceCurrency.KRW) {
            if (investedAmountKrw == null || investedAmountKrw.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("외화 주식의 원화 환산 금액(investedAmountKrw)은 필수입니다.");
            }
            return investedAmountKrw;
        }
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 추가 매수 시 연결된 CASH에서 차감
     */
    private void deductCashBalance(Long stockItemId, PortfolioItem stockItem,
                                    Integer quantity, BigDecimal purchasePrice,
                                    BigDecimal investedAmountKrw) {
        cashStockLinkRepository.findByStockItemId(stockItemId).ifPresent(link -> {
            PortfolioItem cashItem = portfolioItemRepository.findById(link.getCashItemId())
                    .orElseThrow(() -> new IllegalArgumentException("연결된 원화 항목을 찾을 수 없습니다."));
            PriceCurrency currency = stockItem.getStockDetail() != null
                    ? stockItem.getStockDetail().getPriceCurrency() : PriceCurrency.KRW;
            BigDecimal deductionAmount = calculateDeductionAmount(currency, quantity, purchasePrice, investedAmountKrw);
            cashItem.deductAmount(deductionAmount);
            portfolioItemRepository.save(cashItem);
        });
    }

    /**
     * investedAmount 변동분만큼 CASH 차감/복원
     */
    private void adjustCashForAmountChange(Long stockItemId, BigDecimal oldAmount, BigDecimal newAmount) {
        int comparison = newAmount.compareTo(oldAmount);
        if (comparison == 0) {
            return;
        }

        cashStockLinkRepository.findByStockItemId(stockItemId).ifPresent(link -> {
            PortfolioItem cashItem = portfolioItemRepository.findById(link.getCashItemId())
                    .orElseThrow(() -> new IllegalArgumentException("연결된 원화 항목을 찾을 수 없습니다."));
            BigDecimal diff = newAmount.subtract(oldAmount);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                cashItem.deductAmount(diff);
            } else {
                cashItem.restoreAmount(diff.negate());
            }
            portfolioItemRepository.save(cashItem);
        });
    }

    /**
     * 주식 수정 시 CASH 연결 변경/해제 처리
     */
    private void handleCashLinkChange(Long userId, Long stockItemId, Long newCashItemId,
                                       PortfolioItem stockItem,
                                       BigDecimal oldInvestedAmount, BigDecimal oldInvestedAmountKrw,
                                       PriceCurrency oldPriceCurrency, boolean deductOnLink) {
        CashStockLink existingLink = cashStockLinkRepository.findByStockItemId(stockItemId).orElse(null);
        Long existingCashItemId = existingLink != null ? existingLink.getCashItemId() : null;

        boolean hasExistingLink = existingCashItemId != null;
        boolean hasNewLink = newCashItemId != null;

        if (hasExistingLink && hasNewLink && existingCashItemId.equals(newCashItemId)) {
            // 동일 CASH 유지 → 차액 조정
            BigDecimal oldKrwAmount = getKrwAmount(oldPriceCurrency, oldInvestedAmount, oldInvestedAmountKrw);
            BigDecimal newKrwAmount = getKrwAmount(
                    stockItem.getStockDetail().getPriceCurrency(),
                    stockItem.getInvestedAmount(),
                    stockItem.getStockDetail().getInvestedAmountKrw());
            adjustCashDiff(existingCashItemId, oldKrwAmount, newKrwAmount);
        } else if (hasExistingLink && hasNewLink && !existingCashItemId.equals(newCashItemId)) {
            // 교체 불허 (v1)
            throw new IllegalArgumentException("원화 연결을 변경하려면 먼저 기존 연결을 해제한 후 새로 연결해 주세요.");
        } else if (hasExistingLink && !hasNewLink) {
            // 해제: 기존 차감 복원 없음, 링크만 삭제
            cashStockLinkRepository.deleteByStockItemId(stockItemId);
        } else if (!hasExistingLink && hasNewLink) {
            // 신규 연결: deductOnLink=true이면 현재 investedAmount 차감, false이면 연결만
            PortfolioItem cashItem = findAndValidateCashItem(userId, newCashItemId);
            if (deductOnLink) {
                BigDecimal deductionAmount = getKrwAmount(
                        stockItem.getStockDetail().getPriceCurrency(),
                        stockItem.getInvestedAmount(),
                        stockItem.getStockDetail().getInvestedAmountKrw());
                cashItem.deductAmount(deductionAmount);
                portfolioItemRepository.save(cashItem);
            }
            CashStockLink link = CashStockLink.create(newCashItemId, stockItemId);
            cashStockLinkRepository.save(link);
        }
        // 둘 다 null이면 아무것도 안 함
    }

    /**
     * KRW 기준 금액 반환 (KRW 주식은 investedAmount, 외화는 investedAmountKrw)
     */
    private BigDecimal getKrwAmount(PriceCurrency currency, BigDecimal investedAmount, BigDecimal investedAmountKrw) {
        if (currency != null && currency != PriceCurrency.KRW && investedAmountKrw != null) {
            return investedAmountKrw;
        }
        return investedAmount;
    }

    /**
     * CASH 차액 조정 (oldKrw vs newKrw)
     */
    private void adjustCashDiff(Long cashItemId, BigDecimal oldKrwAmount, BigDecimal newKrwAmount) {
        int comparison = newKrwAmount.compareTo(oldKrwAmount);
        if (comparison == 0) {
            return;
        }
        PortfolioItem cashItem = portfolioItemRepository.findById(cashItemId)
                .orElseThrow(() -> new IllegalArgumentException("연결된 원화 항목을 찾을 수 없습니다."));
        BigDecimal diff = newKrwAmount.subtract(oldKrwAmount);
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            cashItem.deductAmount(diff);
        } else {
            cashItem.restoreAmount(diff.negate());
        }
        portfolioItemRepository.save(cashItem);
    }

    /**
     * CASH 삭제 방어: 연결된 주식이 있으면 삭제 차단
     */
    private void validateCashDeletion(Long cashItemId) {
        if (cashStockLinkRepository.existsByCashItemId(cashItemId)) {
            throw new IllegalArgumentException("연결된 주식이 있어 삭제할 수 없습니다. 주식의 원화 연결을 먼저 해제해 주세요.");
        }
    }

    /**
     * 주식 삭제 시 선택적 CASH 복원 + cash_stock_link 삭제
     */
    private void handleStockDeletion(PortfolioItem stockItem, boolean restoreCash, BigDecimal restoreAmount) {
        CashStockLink link = cashStockLinkRepository.findByStockItemId(stockItem.getId()).orElse(null);

        if (link != null) {
            if (restoreCash) {
                if (restoreAmount == null || restoreAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("복원 금액은 0보다 커야 합니다.");
                }
                PortfolioItem cashItem = portfolioItemRepository.findById(link.getCashItemId())
                        .orElseThrow(() -> new IllegalArgumentException("연결된 원화 항목을 찾을 수 없습니다."));
                cashItem.restoreAmount(restoreAmount);
                portfolioItemRepository.save(cashItem);
            }
            cashStockLinkRepository.deleteByStockItemId(stockItem.getId());
        }
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