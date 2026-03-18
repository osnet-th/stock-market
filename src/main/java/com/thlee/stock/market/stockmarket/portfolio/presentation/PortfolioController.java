package com.thlee.stock.market.stockmarket.portfolio.presentation;

import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockPurchaseHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.util.List;

/**
 * 포트폴리오 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;

    /**
     * 주식 항목 등록
     */
    @PostMapping("/items/stock")
    public ResponseEntity<PortfolioItemResponse> addStockItem(
            @RequestParam Long userId,
            @RequestBody StockItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addStockItem(
                userId, request.getItemName(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getStockCode(), request.getMarket(),
                request.getExchangeCode(), request.getCountry(),
                request.getQuantity(), request.getPurchasePrice(), request.getDividendYield(),
                request.getPriceCurrency(), request.getInvestedAmountKrw(),
                request.getCashItemId());
        return ResponseEntity.ok(response);
    }

    /**
     * 채권 항목 등록
     */
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

    /**
     * 부동산 항목 등록
     */
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

    /**
     * 펀드 항목 등록
     */
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

    /**
     * 일반 자산 항목 등록 (CRYPTO, GOLD, COMMODITY, CASH, OTHER)
     */
    @PostMapping("/items/general")
    public ResponseEntity<PortfolioItemResponse> addGeneralItem(
            @RequestParam Long userId,
            @RequestBody GeneralItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addGeneralItem(
                userId, request.getAssetType(), request.getItemName(),
                request.getInvestedAmount(), request.getRegion(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 항목 목록 조회
     */
    @GetMapping("/items")
    public ResponseEntity<List<PortfolioItemResponse>> getItems(@RequestParam Long userId) {
        List<PortfolioItemResponse> responses = portfolioService.getItems(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 주식 항목 수정
     */
    @PutMapping("/items/stock/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateStockItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateStockItem(
                userId, itemId,
                request.getItemName(), request.getMemo(),
                request.getSubType(), request.getStockCode(), request.getMarket(),
                request.getExchangeCode(), request.getCountry(),
                request.getQuantity(), request.getPurchasePrice(), request.getDividendYield(),
                request.getPriceCurrency(), request.getInvestedAmountKrw(),
                request.getCashItemId(), request.isDeductOnLink());
        return ResponseEntity.ok(response);
    }

    /**
     * 채권 항목 수정
     */
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

    /**
     * 부동산 항목 수정
     */
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

    /**
     * 펀드 항목 수정
     */
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

    /**
     * 일반 자산 항목 수정
     */
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

    /**
     * 주식 추가 매수 (가중평균 자동 계산)
     */
    @PostMapping("/items/stock/{itemId}/purchase")
    public ResponseEntity<PortfolioItemResponse> addStockPurchase(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockPurchaseRequest request) {
        PortfolioItemResponse response = portfolioService.addStockPurchase(
                userId, itemId, request.getQuantity(), request.getPurchasePrice(),
                request.getInvestedAmountKrw());
        return ResponseEntity.ok(response);
    }

    /**
     * 매수이력 조회
     */
    @GetMapping("/items/stock/{itemId}/purchases")
    public ResponseEntity<List<StockPurchaseHistoryResponse>> getPurchaseHistories(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        List<StockPurchaseHistoryResponse> responses = portfolioService.getPurchaseHistories(userId, itemId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 매수이력 수정
     */
    @PutMapping("/items/stock/{itemId}/purchases/{historyId}")
    public ResponseEntity<PortfolioItemResponse> updatePurchaseHistory(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId,
            @RequestBody StockPurchaseHistoryUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updatePurchaseHistory(
                userId, itemId, historyId,
                request.getQuantity(), request.getPurchasePrice(),
                request.getPurchasedAt(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    /**
     * 매수이력 삭제
     */
    @DeleteMapping("/items/stock/{itemId}/purchases/{historyId}")
    public ResponseEntity<Void> deletePurchaseHistory(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId) {
        portfolioService.deletePurchaseHistory(userId, itemId, historyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 뉴스 수집 토글
     */
    @PatchMapping("/items/{itemId}/news")
    public ResponseEntity<Void> toggleNews(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody PortfolioNewsToggleRequest request) {
        portfolioService.toggleNews(userId, itemId, request.isEnabled());
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오 항목 삭제
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestParam(required = false, defaultValue = "false") boolean restoreCash,
            @RequestParam(required = false) BigDecimal restoreAmount) {
        portfolioService.deleteItem(userId, itemId, restoreCash, restoreAmount);
        return ResponseEntity.noContent().build();
    }

    /**
     * 자산 비중 조회
     */
    @GetMapping("/allocation")
    public ResponseEntity<List<AllocationResponse>> getAllocation(@RequestParam Long userId) {
        List<AllocationResponse> responses = portfolioAllocationService.getAllocation(userId);
        return ResponseEntity.ok(responses);
    }
}