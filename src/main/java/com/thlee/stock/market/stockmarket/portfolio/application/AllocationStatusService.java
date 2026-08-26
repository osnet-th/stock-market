package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationStatusResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationStatusResponse.AssetStatus;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationStatusResponse.BucketStatus;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.AllocationTarget;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.AssetClassification;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AllocationBucket;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.AllocationTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 목표 자산 배분 현황 계산 서비스
 * - 평가액 기준 (ACTIVE만, CRYPTO 제외)
 * - 상위 안전/투자 버킷: 전체 평가액 대비 비율
 * - 투자 자산군: 투자자산 평가액 대비 비율
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationStatusService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PortfolioEvaluationService portfolioEvaluationService;
    private final AllocationTargetRepository allocationTargetRepository;

    public AllocationStatusResponse getStatus(Long userId) {
        Optional<AllocationTarget> target = allocationTargetRepository.findByUserId(userId);
        PortfolioEvaluation evaluation = portfolioEvaluationService
                .evaluatePortfolios(List.of(userId)).get(userId);
        List<ItemEvaluation> items = evaluation != null ? evaluation.getItems() : List.of();

        int excludedCryptoCount = 0;
        Map<AssetType, BigDecimal> amountByType = new EnumMap<>(AssetType.class);
        for (ItemEvaluation item : items) {
            AssetType type = AssetType.valueOf(item.getAssetType());
            if (AssetClassification.isExcluded(type)) {
                excludedCryptoCount++;
                continue;
            }
            amountByType.merge(type, item.getEvaluatedAmount(), BigDecimal::add);
        }

        BigDecimal safeAmount = BigDecimal.ZERO;
        BigDecimal investAmount = BigDecimal.ZERO;
        for (Map.Entry<AssetType, BigDecimal> entry : amountByType.entrySet()) {
            if (AssetClassification.isSafe(entry.getKey())) {
                safeAmount = safeAmount.add(entry.getValue());
            } else {
                investAmount = investAmount.add(entry.getValue());
            }
        }
        BigDecimal total = safeAmount.add(investAmount);

        boolean configured = target.isPresent();
        BigDecimal band = target.map(AllocationTarget::getBandPctPoint).orElse(null);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new AllocationStatusResponse(
                    configured, BigDecimal.ZERO, band, excludedCryptoCount, List.of(), List.of());
        }

        List<BucketStatus> buckets = List.of(
                buildBucket(AllocationBucket.SAFE, safeAmount, total,
                        target.map(AllocationTarget::getSafeRatio).orElse(null), band),
                buildBucket(AllocationBucket.INVEST, investAmount, total,
                        target.map(AllocationTarget::getInvestRatio).orElse(null), band)
        );

        Map<AssetType, BigDecimal> assetTargets = target
                .map(AllocationTarget::getInvestAssetRatios)
                .orElse(Map.of());
        List<AssetStatus> investAssets = buildInvestAssets(amountByType, investAmount, assetTargets, band);

        return new AllocationStatusResponse(
                configured, total, band, excludedCryptoCount, buckets, investAssets);
    }

    private BucketStatus buildBucket(AllocationBucket bucket, BigDecimal amount, BigDecimal total,
                                     BigDecimal targetRatio, BigDecimal band) {
        BigDecimal currentRatio = ratioOf(amount, total);
        if (targetRatio == null) {
            return new BucketStatus(bucket.name(), bucket.getDescription(),
                    amount, currentRatio, null, null, null, null, null);
        }
        BigDecimal targetAmount = amountOf(total, targetRatio);
        BigDecimal deviationPctPoint = currentRatio.subtract(targetRatio);
        return new BucketStatus(bucket.name(), bucket.getDescription(),
                amount, currentRatio, targetRatio, targetAmount,
                amount.subtract(targetAmount), deviationPctPoint,
                isBandExceeded(deviationPctPoint, band));
    }

    private List<AssetStatus> buildInvestAssets(Map<AssetType, BigDecimal> amountByType,
                                                BigDecimal investAmount,
                                                Map<AssetType, BigDecimal> assetTargets,
                                                BigDecimal band) {
        List<AssetStatus> result = new ArrayList<>();
        for (AssetType type : AssetClassification.investTypes()) {
            BigDecimal amount = amountByType.getOrDefault(type, BigDecimal.ZERO);
            BigDecimal targetRatio = assetTargets.get(type);
            boolean hasHolding = amount.compareTo(BigDecimal.ZERO) > 0;
            if (!hasHolding && targetRatio == null) {
                continue;
            }
            BigDecimal currentRatio = ratioOf(amount, investAmount);
            if (targetRatio == null) {
                result.add(new AssetStatus(type.name(), type.getDescription(),
                        amount, currentRatio, null, null, null, null, null));
                continue;
            }
            BigDecimal targetAmount = amountOf(investAmount, targetRatio);
            BigDecimal deviationPctPoint = currentRatio.subtract(targetRatio);
            result.add(new AssetStatus(type.name(), type.getDescription(),
                    amount, currentRatio, targetRatio, targetAmount,
                    amount.subtract(targetAmount), deviationPctPoint,
                    isBandExceeded(deviationPctPoint, band)));
        }
        return result;
    }

    private BigDecimal ratioOf(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal amountOf(BigDecimal total, BigDecimal ratio) {
        return total.multiply(ratio).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private Boolean isBandExceeded(BigDecimal deviationPctPoint, BigDecimal band) {
        if (band == null) {
            return null;
        }
        return deviationPctPoint.abs().compareTo(band) > 0;
    }
}
