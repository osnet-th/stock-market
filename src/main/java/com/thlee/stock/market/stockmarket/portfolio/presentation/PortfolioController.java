package com.thlee.stock.market.stockmarket.portfolio.presentation;

import com.thlee.stock.market.stockmarket.portfolio.application.AllocationStatusService;
import com.thlee.stock.market.stockmarket.portfolio.application.AllocationTargetService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioSummaryService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AddStockSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationStatusResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationTargetResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.DepositHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioSnapshotResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioSummaryResponse;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;
    private final AllocationTargetService allocationTargetService;
    private final AllocationStatusService allocationStatusService;
    private final PortfolioSummaryService portfolioSummaryService;

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
     * 포트폴리오 상단 요약 — 누적 수익률·보유일수·CAGR·환차손익 (#110)
     */
    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioSummaryService.getSummary(userId));
    }

    /**
     * 오늘자 자산 스냅샷 저장 (같은 날 재저장은 덮어쓰기) (#110)
     */
    @PostMapping("/snapshots")
    public ResponseEntity<PortfolioSnapshotResponse> saveSnapshot(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioSummaryService.saveTodaySnapshot(userId));
    }

    /**
     * 최근 N개월 자산 스냅샷 (날짜 오름차순) (#110)
     */
    @GetMapping("/snapshots")
    public ResponseEntity<List<PortfolioSnapshotResponse>> getSnapshots(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "12") int months) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(portfolioSummaryService.getSnapshots(userId, months));
    }

    /**
     * 주식 항목 등록
     */
    @PostMapping("/items/stock")
    public ResponseEntity<PortfolioItemResponse> addStockItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody StockItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody BondItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody RealEstateItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody FundItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.addFundItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getManagementFee(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 연금 항목 등록 (IRP / 연금저축 / DC / DB)
     */
    @PostMapping("/items/pension")
    public ResponseEntity<PortfolioItemResponse> addPensionItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody PensionItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.addPensionItem(
                userId, request.getItemName(), request.getInvestedAmount(),
                request.getRegion(), request.getMemo(),
                request.getSubType(), request.getProvider(), request.getEvaluatedAmount(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 현금성 자산 항목 등록 (예금/적금/CMA)
     */
    @PostMapping("/items/cash")
    public ResponseEntity<PortfolioItemResponse> addCashItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody CashItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody GeneralItemAddRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.addGeneralItem(
                userId, request.getAssetType(), request.getItemName(),
                request.getInvestedAmount(), request.getRegion(), request.getMemo(),
                request.getQuantityGrams());
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 항목 목록 조회
     */
    @GetMapping("/items")
    public ResponseEntity<List<PortfolioItemResponse>> getItems(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        List<PortfolioItemResponse> responses = portfolioService.getItems(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 주식 항목 수정
     */
    @PutMapping("/items/stock/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateStockItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody BondItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody RealEstateItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody FundItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.updateFundItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getManagementFee(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 연금 항목 수정
     */
    @PutMapping("/items/pension/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updatePensionItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody PensionItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.updatePensionItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getSubType(), request.getProvider(), request.getEvaluatedAmount(),
                request.getMonthlyDepositAmount(), request.getDepositDay());
        return ResponseEntity.ok(response);
    }

    /**
     * 현금성 자산 항목 수정
     */
    @PutMapping("/items/cash/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateCashItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody CashItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody GeneralItemUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.updateGeneralItem(
                userId, itemId,
                request.getItemName(), request.getInvestedAmount(), request.getMemo(),
                request.getQuantityGrams());
        return ResponseEntity.ok(response);
    }

    /**
     * 주식 추가 매수 (가중평균 자동 계산)
     */
    @PostMapping("/items/stock/{itemId}/purchase")
    public ResponseEntity<PortfolioItemResponse> addStockPurchase(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody StockPurchaseRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        assertUserMatches(jwtUserId, userId);
        List<StockPurchaseHistoryResponse> responses = portfolioService.getPurchaseHistories(userId, itemId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 매수이력 수정
     */
    @PutMapping("/items/stock/{itemId}/purchases/{historyId}")
    public ResponseEntity<PortfolioItemResponse> updatePurchaseHistory(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId,
            @RequestBody StockPurchaseHistoryUpdateRequest request) {
        assertUserMatches(jwtUserId, userId);
        PortfolioItemResponse response = portfolioService.updatePurchaseHistory(
                userId, itemId, historyId,
                request.getQuantity(), request.getPurchasePrice(),
                request.getPurchasedAt(), request.getMemo(), request.getFxRate());
        return ResponseEntity.ok(response);
    }

    /**
     * 매수이력 삭제
     */
    @DeleteMapping("/items/stock/{itemId}/purchases/{historyId}")
    public ResponseEntity<Void> deletePurchaseHistory(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId) {
        assertUserMatches(jwtUserId, userId);
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
                request.getDeductionAmountKrw(),
                request.getNetProceedsKrw(),
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
                request.getDeductionAmountKrw(),
                request.getNetProceedsKrw(),
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody DepositRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        assertUserMatches(jwtUserId, userId);
        List<DepositHistoryResponse> responses = portfolioService.getDepositHistories(userId, itemId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 납입 수정
     */
    @PutMapping("/items/{itemId}/deposits/{historyId}")
    public ResponseEntity<DepositHistoryResponse> updateDeposit(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId,
            @Valid @RequestBody DepositRequest request) {
        assertUserMatches(jwtUserId, userId);
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @PathVariable Long historyId) {
        assertUserMatches(jwtUserId, userId);
        portfolioService.deleteDeposit(userId, itemId, historyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 만기 예상 금액 조회
     */
    @GetMapping("/items/{itemId}/expected-maturity")
    public ResponseEntity<BigDecimal> getExpectedMaturityAmount(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        assertUserMatches(jwtUserId, userId);
        BigDecimal amount = portfolioService.calculateExpectedMaturityAmount(userId, itemId);
        return ResponseEntity.ok(amount);
    }

    /**
     * 뉴스 수집 토글
     */
    @PatchMapping("/items/{itemId}/news")
    public ResponseEntity<Void> toggleNews(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody PortfolioNewsToggleRequest request) {
        assertUserMatches(jwtUserId, userId);
        portfolioService.toggleNews(userId, itemId, request.isEnabled());
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오 항목 삭제
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestParam(required = false, defaultValue = "false") boolean restoreCash,
            @RequestParam(required = false) BigDecimal restoreAmount) {
        assertUserMatches(jwtUserId, userId);
        portfolioService.deleteItem(userId, itemId, restoreCash, restoreAmount);
        return ResponseEntity.noContent().build();
    }

    /**
     * 자산 비중 조회
     */
    @GetMapping("/allocation")
    public ResponseEntity<List<AllocationResponse>> getAllocation(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        List<AllocationResponse> responses = portfolioAllocationService.getAllocation(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 목표 자산 배분 현황 조회 (평가액 기준 현재/목표/편차)
     */
    @GetMapping("/allocation/status")
    public ResponseEntity<AllocationStatusResponse> getAllocationStatus(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return ResponseEntity.ok(allocationStatusService.getStatus(userId));
    }

    /**
     * 목표 자산 배분 설정 조회 (미설정 시 204)
     */
    @GetMapping("/allocation/target")
    public ResponseEntity<AllocationTargetResponse> getAllocationTarget(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId) {
        assertUserMatches(jwtUserId, userId);
        return allocationTargetService.getTarget(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * 목표 자산 배분 설정 저장 (업서트)
     */
    @PutMapping("/allocation/target")
    public ResponseEntity<AllocationTargetResponse> saveAllocationTarget(
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody AllocationTargetSaveRequest request) {
        assertUserMatches(jwtUserId, userId);
        AllocationTargetResponse response = allocationTargetService.saveTarget(
                userId,
                request.getSafeRatio(),
                request.getInvestRatio(),
                request.getBandPctPoint(),
                toAssetRatioMap(request.getInvestAssets()));
        return ResponseEntity.ok(response);
    }

    private Map<AssetType, BigDecimal> toAssetRatioMap(List<AllocationTargetSaveRequest.AssetRatio> assets) {
        Map<AssetType, BigDecimal> ratios = new LinkedHashMap<>();
        if (assets == null) {
            return ratios;
        }
        for (AllocationTargetSaveRequest.AssetRatio asset : assets) {
            AssetType type;
            try {
                type = AssetType.valueOf(asset.getAssetType());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("알 수 없는 자산 유형입니다: " + asset.getAssetType());
            }
            if (ratios.put(type, asset.getTargetRatio()) != null) {
                throw new IllegalArgumentException("자산 유형이 중복되었습니다: " + type.getDescription());
            }
        }
        return ratios;
    }
}
