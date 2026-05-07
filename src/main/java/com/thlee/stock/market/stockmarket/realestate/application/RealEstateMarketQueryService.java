package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketIndicatorRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketLatestRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketMetadataRepository;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.ComparisonResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.IndicatorCard;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.PeriodOption;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SourceMetaResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SummaryResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.TabResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 부동산 시장 데이터 read 유스케이스.
 * <p>
 * 모든 응답은 referenceText/snapshotDate/source/sourceUrl을 항목마다 포함하며,
 * 데이터 누락 시 빈 카드 + lastUpdatedAt=null + nextScheduledAt 노출.
 * 판단/추천/등급 라벨은 응답 어디에도 노출되지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealEstateMarketQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime BATCH_TIME = LocalTime.of(6, 0);
    private static final int HISTORY_LIMIT = 24;

    private static final List<String> HEADLINE_CODES = List.of(
            "APT_TRADE_VOLUME", "APT_TRADE_AVG_PRICE",
            "JEONSE_DEPOSIT_AVG", "MONTHLY_RENT_RATIO",
            "UNSOLD_HOUSEHOLDS", "AVG_FIRST_PRIORITY_RATIO");

    private final RealEstateMarketIndicatorRepository indicatorRepository;
    private final RealEstateMarketLatestRepository latestRepository;
    private final RealEstateMarketMetadataRepository metadataRepository;
    private final RealEstateRegionService regionService;
    private final RealEstateMarketAggregator aggregator;

    public SummaryResponse getSummary(String regionCode) {
        Map<MetaKey, RealEstateMarketMetadata> metaMap = loadMetadataMap();
        List<IndicatorCard> cards = new ArrayList<>();

        for (RealEstateMarketCategory category : RealEstateMarketCategory.values()) {
            for (Map.Entry<MetaKey, RealEstateMarketMetadata> entry : metaMap.entrySet()) {
                MetaKey key = entry.getKey();
                if (key.category() != category) continue;
                if (!HEADLINE_CODES.contains(key.indicatorCode())) continue;

                List<RealEstateMarketIndicator> history = indicatorRepository.findHistory(
                        regionCode, key.category(), key.source(), key.indicatorCode(), HISTORY_LIMIT);
                IndicatorCard card = aggregator.toCard(entry.getValue(), history);
                if (card != null) cards.add(card);
            }
        }

        LocalDateTime lastUpdated = latestUpdatedAt(regionCode);
        return new SummaryResponse(
                regionCode,
                cards,
                lastUpdated == null ? null : lastUpdated.toString(),
                nextScheduledAt(),
                cards.stream().map(IndicatorCard::source).distinct().toList());
    }

    public TabResponse getTab(String regionCode,
                              RealEstateMarketCategory category,
                              PeriodOption period) {
        Map<MetaKey, RealEstateMarketMetadata> metaMap = loadMetadataMap();
        List<RealEstateMarketIndicator> latestPerSource =
                indicatorRepository.findLatestByRegionAndCategory(regionCode, category);
        List<IndicatorCard> cards = new ArrayList<>();

        for (Map.Entry<MetaKey, RealEstateMarketMetadata> entry : metaMap.entrySet()) {
            MetaKey key = entry.getKey();
            if (key.category() != category) continue;
            List<RealEstateMarketIndicator> history = indicatorRepository.findHistory(
                    regionCode, key.category(), key.source(), key.indicatorCode(), HISTORY_LIMIT);
            IndicatorCard card = aggregator.toCard(entry.getValue(), history);
            if (card != null) cards.add(card);
        }

        if (category == RealEstateMarketCategory.RENT) {
            attachJeonseRatioEstimate(regionCode, metaMap, cards);
        }

        LocalDateTime latest = latestPerSource.stream()
                .map(RealEstateMarketIndicator::getSnapshotDate)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new TabResponse(
                regionCode,
                category.name(),
                period.name(),
                cards,
                latest == null ? null : latest.toString(),
                nextScheduledAt(),
                cards.stream().map(IndicatorCard::source).distinct().toList());
    }

    public ComparisonResponse getComparison(List<String> regionCodes,
                                             RealEstateMarketCategory category,
                                             PeriodOption period) {
        Map<MetaKey, RealEstateMarketMetadata> metaMap = loadMetadataMap();
        List<ComparisonResponse.ComparisonRow> rows = new ArrayList<>();

        for (String code : regionCodes) {
            String label = regionService.regionLabel(code);
            for (Map.Entry<MetaKey, RealEstateMarketMetadata> entry : metaMap.entrySet()) {
                MetaKey key = entry.getKey();
                if (key.category() != category) continue;
                List<RealEstateMarketIndicator> history = indicatorRepository.findHistory(
                        code, key.category(), key.source(), key.indicatorCode(), 2);
                IndicatorCard card = aggregator.toCard(entry.getValue(), history);
                if (card == null) continue;
                rows.add(new ComparisonResponse.ComparisonRow(
                        code, label,
                        card.indicatorCode(), card.displayName(),
                        card.unit(), card.value(), card.changeRate(),
                        card.referenceText(), card.source()));
            }
        }
        return new ComparisonResponse(category.name(), period.name(), rows, nowKstString());
    }

    public SourceMetaResponse getSourceMetadata() {
        List<RealEstateMarketLatest> all = latestRepository.findAll();
        Map<RealEstateMarketSource, List<RealEstateMarketLatest>> grouped = all.stream()
                .collect(Collectors.groupingBy(RealEstateMarketLatest::getSource));

        String nextScheduled = nextScheduledAt();
        List<SourceMetaResponse.SourceEntry> entries = Stream.of(RealEstateMarketSource.values())
                .map(source -> {
                    List<RealEstateMarketLatest> rows = grouped.getOrDefault(source, List.of());
                    LocalDateTime lastSuccess = rows.stream()
                            .map(RealEstateMarketLatest::getUpdatedAt)
                            .filter(d -> d != null)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    return new SourceMetaResponse.SourceEntry(
                            source.name(),
                            source.getLabel(),
                            lastSuccess == null ? null : lastSuccess.toString(),
                            nextScheduled,
                            rows.size());
                })
                .toList();
        return new SourceMetaResponse(entries);
    }

    private void attachJeonseRatioEstimate(String regionCode,
                                            Map<MetaKey, RealEstateMarketMetadata> metaMap,
                                            List<IndicatorCard> cards) {
        RealEstateMarketMetadata jeonseRatioMeta = metaMap.values().stream()
                .filter(m -> "JEONSE_RATIO_ESTIMATE".equals(m.getIndicatorCode()))
                .findFirst().orElse(null);
        if (jeonseRatioMeta == null) return;

        RealEstateMarketIndicator saleAvg = latestRepository
                .findByKey(regionCode, RealEstateMarketCategory.TRADE,
                        RealEstateMarketSource.MOLIT, aggregator.saleAvgCode())
                .map(this::toIndicator)
                .orElse(null);
        RealEstateMarketIndicator jeonseAvg = latestRepository
                .findByKey(regionCode, RealEstateMarketCategory.RENT,
                        RealEstateMarketSource.MOLIT, aggregator.jeonseDepositCode())
                .map(this::toIndicator)
                .orElse(null);
        IndicatorCard ratio = aggregator.estimateJeonseRatio(jeonseRatioMeta, saleAvg, jeonseAvg);
        if (ratio != null) {
            cards.add(ratio);
        }
    }

    private RealEstateMarketIndicator toIndicator(RealEstateMarketLatest latest) {
        return RealEstateMarketIndicator.builder()
                .regionCode(latest.getRegionCode())
                .category(latest.getCategory())
                .source(latest.getSource())
                .indicatorCode(latest.getIndicatorCode())
                .referenceText(latest.getReferenceText())
                .value(latest.getValue())
                .payload(latest.getPayload())
                .snapshotDate(latest.getUpdatedAt())
                .build();
    }

    private Map<MetaKey, RealEstateMarketMetadata> loadMetadataMap() {
        Map<MetaKey, RealEstateMarketMetadata> map = new HashMap<>();
        for (RealEstateMarketMetadata meta : metadataRepository.findAll()) {
            map.put(new MetaKey(meta.getCategory(), meta.getSource(), meta.getIndicatorCode()), meta);
        }
        return map;
    }

    private LocalDateTime latestUpdatedAt(String regionCode) {
        return Optional.ofNullable(latestRepository.findAll()).orElse(List.of()).stream()
                .filter(l -> regionCode.equals(l.getRegionCode()))
                .map(RealEstateMarketLatest::getUpdatedAt)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String nextScheduledAt() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime next = now.with(BATCH_TIME);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.toLocalDateTime().toString();
    }

    private String nowKstString() {
        return ZonedDateTime.now(KST).toLocalDateTime().toString();
    }

    /** 메타 조회 키. */
    private record MetaKey(RealEstateMarketCategory category,
                            RealEstateMarketSource source,
                            String indicatorCode) {
    }
}