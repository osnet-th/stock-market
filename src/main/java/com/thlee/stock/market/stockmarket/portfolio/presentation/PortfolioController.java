package com.thlee.stock.market.stockmarket.portfolio.presentation;

import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AddStockSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.DepositHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockPurchaseHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleContextResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.UpdateSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * JWT 인증된 사용자(principal)와 query userId의 일치 검증.
     * dev 환경(permitAll, anonymous principal)에서는 jwtUserId가 null이므로 검증을 건너뛴다.
     * 운영(JWT 강제) 환경에서만 IDOR을 차단한다.
     */
    private void assertUserMatches(Long jwtUserId, Long requestedUserId) {
        if (jwtUserId != null && !jwtUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("요청한 사용자 정보가 인증된 사용자와 일치하지 않습니다.");
        }
    }

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
                request.getSubType(), request.getManagementFee(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 현금성 자산 항목 등록 (예금/적금/CMA)
     */
    @PostMapping("/items/cash")
    public ResponseEntity<PortfolioItemResponse> addCashItem(
            @RequestParam Long userId,
            @RequestBody CashItemAddRequest request) {
        PortfolioItemResponse response = portfolioService.addCashItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getCashType(), request.getInterestRate(),
                request.getStartDate(), request.getMaturityDate(),
                request.getTaxType(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 일반 자산 항목 등록 (CRYPTO, GOLD, COMMODITY, OTHER)
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
                request.getSubType(), request.getManagementFee(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 현금성 자산 항목 수정
     */
    @PutMapping("/items/cash/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateCashItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody CashItemUpdateRequest request) {
        PortfolioItemResponse response = portfolioService.updateCashItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getInterestRate(), request.getStartDate(),
                request.getMaturityDate(), request.getTaxType(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
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

    // ──────────────────────────────────────────────────────────────────
    // 매도 이력 API
    // ──────────────────────────────────────────────────────────────────

    /**
     * 매도 모달 진입 시 자동 입력 컨텍스트 조회
     */
    @GetMapping("/items/stock/{itemId}/sale-context")
    public ResponseEntity<StockSaleContextResponse> getSaleContext(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioService.getSaleContext(userId, itemId));
    }

    /**
     * 주식 매도 등록
     */
    @PostMapping("/items/stock/{itemId}/sale")
    public ResponseEntity<StockSaleHistoryResponse> addStockSale(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody StockSaleRequest request) {
        assertUserMatches(jwtUserId, userId);
        AddStockSaleParam param = new AddStockSaleParam(
                request.getQuantity(),
                request.getSalePrice(),
                request.getSoldAt(),
                request.getReason(),
                request.getMemo(),
                request.getFxRate(),
                request.getDepositCashItemId()
        );
        return ResponseEntity.ok(portfolioService.addStockSale(userId, itemId, param));
    }

    /**
     * 특정 항목의 매도 이력 조회
     */
    @GetMapping("/items/stock/{itemId}/sales")
    public ResponseEntity<List<StockSaleHistoryResponse>> getSaleHistories(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioService.getSaleHistories(userId, itemId));
    }

    /**
     * 매도 이력 사후 수정
     */
    @PutMapping("/items/stock/{itemId}/sales/{historyId}")
    public ResponseEntity<StockSaleHistoryResponse> updateSaleHistory(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId,
            @Valid @RequestBody StockSaleHistoryUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
        UpdateSaleParam param = new UpdateSaleParam(
                request.getQuantity(),
                request.getSalePrice(),
                request.getReason(),
                request.getMemo()
        );
        return ResponseEntity.ok(portfolioService.updateSaleHistory(userId, itemId, historyId, param));
    }

    /**
     * 매도 이력 삭제
     */
    @DeleteMapping("/items/stock/{itemId}/sales/{historyId}")
    public ResponseEntity<Void> deleteSaleHistory(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId) {
        assertUserMatches(jwtUserId, userId);
        portfolioService.deleteSaleHistory(userId, itemId, historyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 전체 매도 이력 조회 (매도일 내림차순)
     */
    @GetMapping("/sales")
    public ResponseEntity<List<StockSaleHistoryResponse>> getAllUserSaleHistories(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioService.getAllUserSaleHistories(userId));
    }

    /**
     * 매도 이력이 있는 PortfolioItem id 집합 (경량 — 보유 카드 disabled 판정 전용).
     */
    @GetMapping("/sales/item-ids")
    public ResponseEntity<List<Long>> getSaleItemIds(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioService.getSaleItemIds(userId));
    }

    // ──────────────────────────────────────────────────────────────────
    // 납입 이력 API
    // ──────────────────────────────────────────────────────────────────

    /**
     * 납입 추가
     */
    @PostMapping("/items/{itemId}/deposits")
    public ResponseEntity<DepositHistoryResponse> addDeposit(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody DepositRequest request) {
        DepositHistoryResponse response = portfolioService.addDeposit(
                userId, itemId, request.getDepositDate(), request.getAmount(),
                request.getUnits(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    /**
     * 납입 이력 조회
     */
    @GetMapping("/items/{itemId}/deposits")
    public ResponseEntity<List<DepositHistoryResponse>> getDepositHistories(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        List<DepositHistoryResponse> responses = portfolioService.getDepositHistories(userId, itemId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 납입 수정
     */
    @PutMapping("/items/{itemId}/deposits/{historyId}")
    public ResponseEntity<DepositHistoryResponse> updateDeposit(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId,
            @Valid @RequestBody DepositRequest request) {
        DepositHistoryResponse response = portfolioService.updateDeposit(
                userId, itemId, historyId,
                request.getDepositDate(), request.getAmount(),
                request.getUnits(), request.getMemo());
        return ResponseEntity.ok(response);
    }

    /**
     * 납입 삭제
     */
    @DeleteMapping("/items/{itemId}/deposits/{historyId}")
    public ResponseEntity<Void> deleteDeposit(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId) {
        portfolioService.deleteDeposit(userId, itemId, historyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 만기 예상 금액 조회
     */
    @GetMapping("/items/{itemId}/expected-maturity")
    public ResponseEntity<BigDecimal> getExpectedMaturityAmount(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        BigDecimal amount = portfolioService.calculateExpectedMaturityAmount(userId, itemId);
        return ResponseEntity.ok(amount);
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