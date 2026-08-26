package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionLevel;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.HistoryPoint;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.IndicatorCard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * 백엔드 집계 (이슈 #35 정신 정합).
 * <p>
 * read 시점에 history → IndicatorCard 변환. 변화율은 history 마지막 두 시점 비교.
 * 판단/등급/추천 라벨 절대 사용 금지.
 */
@Component
public class RealEstateMarketAggregator {

    private static final String JEONSE_DEPOSIT_AVG = "JEONSE_DEPOSIT_AVG";
    private static final String APT_TRADE_AVG_PRICE = "APT_TRADE_AVG_PRICE";
    private static final String JEONSE_RATIO_ESTIMATE = "JEONSE_RATIO_ESTIMATE";

    /**
     * 단일 indicator의 history → IndicatorCard.
     * <p>
     * history는 snapshotDate 오름차순으로 들어온다고 가정. 마지막 항목이 latest.
     */
    public IndicatorCard toCard(RealEstateMarketMetadata metadata,
                                 List<RealEstateMarketIndicator> history) {
        if (history.isEmpty() || metadata == null) {
            return null;
        }
        RealEstateMarketIndicator latest = history.get(history.size() - 1);
        BigDecimal previousValue = previousValue(history);
        BigDecimal changeRate = computeChangeRate(latest.getValue(), previousValue);

        return new IndicatorCard(
                metadata.getIndicatorCode(),
                metadata.getDisplayName(),
                metadata.getDescription(),
                metadata.getUnit(),
                metadata.getSource().name(),
                latest.getSourceUrl(),
                latest.getValue(),
                previousValue,
                changeRate,
                latest.getReferenceText(),
                history.size() >= 2 ? history.get(history.size() - 2).getReferenceText() : null,
                latest.getSnapshotDate() == null ? null : latest.getSnapshotDate().toString(),
                isEstimated(metadata.getIndicatorCode()),
                deriveRegionLevel(latest.getRegionCode()),
                history.stream().map(this::toPoint).toList()
        );
    }

    /**
     * regionCode 길이 기반 derive — SoT는 length(2=SIDO, 5=SIGUNGU). null/invalid는 null 반환.
     */
    private RegionLevel deriveRegionLevel(String regionCode) {
        if (regionCode == null) return null;
        try {
            return RegionLevel.fromCode(regionCode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 매매 평균 + 전세 평균을 같은 region/period에서 받아 전세가율 추정.
     * estimated=true 명시.
     */
    public IndicatorCard estimateJeonseRatio(RealEstateMarketMetadata jeonseRatioMetadata,
                                             RealEstateMarketIndicator saleAvg,
                                             RealEstateMarketIndicator jeonseAvg) {
        if (jeonseRatioMetadata == null || saleAvg == null || jeonseAvg == null) {
            return null;
        }
        if (saleAvg.getValue() == null || jeonseAvg.getValue() == null
                || saleAvg.getValue().signum() <= 0) {
            return null;
        }
        BigDecimal ratio = jeonseAvg.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(saleAvg.getValue(), 2, RoundingMode.HALF_UP);

        return new IndicatorCard(
                JEONSE_RATIO_ESTIMATE,
                jeonseRatioMetadata.getDisplayName(),
                jeonseRatioMetadata.getDescription(),
                jeonseRatioMetadata.getUnit(),
                jeonseRatioMetadata.getSource().name(),
                jeonseAvg.getSourceUrl(),
                ratio,
                null,
                null,
                jeonseAvg.getReferenceText(),
                null,
                jeonseAvg.getSnapshotDate() == null ? null : jeonseAvg.getSnapshotDate().toString(),
                true,
                deriveRegionLevel(jeonseAvg.getRegionCode()),
                List.of()
        );
    }

    private boolean isEstimated(String indicatorCode) {
        return JEONSE_RATIO_ESTIMATE.equals(indicatorCode);
    }

    private BigDecimal previousValue(List<RealEstateMarketIndicator> history) {
        if (history.size() < 2) return null;
        return history.get(history.size() - 2).getValue();
    }

    private BigDecimal computeChangeRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private HistoryPoint toPoint(RealEstateMarketIndicator indicator) {
        return new HistoryPoint(
                indicator.getReferenceText(),
                indicator.getValue(),
                indicator.getSnapshotDate() == null ? null : indicator.getSnapshotDate().toString()
        );
    }

    /**
     * 같은 region에서 매매 평균과 전세 평균을 indicatorCode 기준으로 짝짓는 헬퍼.
     */
    public RealEstateMarketIndicator latestByCode(List<RealEstateMarketIndicator> indicators,
                                                   String indicatorCode) {
        return indicators.stream()
                .filter(i -> indicatorCode.equals(i.getIndicatorCode()))
                .max(Comparator.comparing(RealEstateMarketIndicator::getSnapshotDate))
                .orElse(null);
    }

    public String saleAvgCode() {
        return APT_TRADE_AVG_PRICE;
    }

    public String jeonseDepositCode() {
        return JEONSE_DEPOSIT_AVG;
    }
}