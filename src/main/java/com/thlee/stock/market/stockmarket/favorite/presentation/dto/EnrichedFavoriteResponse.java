package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedEcosFavorite;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedFavorites;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedGlobalFavorite;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.HistoryPoint;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;

import java.time.LocalDate;
import java.util.List;

public record EnrichedFavoriteResponse(
    List<EcosItem> ecos,
    List<GlobalItem> global
) {

    public static EnrichedFavoriteResponse from(EnrichedFavorites enriched) {
        List<EcosItem> ecosItems = enriched.ecos().stream()
            .map(EcosItem::from)
            .toList();

        List<GlobalItem> globalItems = enriched.global().stream()
            .map(GlobalItem::from)
            .toList();

        return new EnrichedFavoriteResponse(ecosItems, globalItems);
    }

    public record EcosItem(
        String indicatorCode,
        String className,
        String keystatName,
        String dataValue,
        String previousDataValue,
        String cycle,
        boolean hasData,
        String displayMode,
        List<EnrichedHistoryPoint> history
    ) {
        public static EcosItem from(EnrichedEcosFavorite enriched) {
            FavoriteDisplayMode mode = enriched.favorite().getDisplayMode();
            String displayMode = (mode != null ? mode : FavoriteDisplayMode.INDICATOR).name();
            List<EnrichedHistoryPoint> history = toHistoryPoints(enriched.history());

            EcosIndicatorLatest latest = enriched.latest();
            if (latest == null) {
                String[] parts = enriched.favorite().getIndicatorCode().split("::", 2);
                return new EcosItem(
                    enriched.favorite().getIndicatorCode(),
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    null, null, null, false,
                    displayMode, history
                );
            }
            return new EcosItem(
                enriched.favorite().getIndicatorCode(),
                latest.getClassName(),
                latest.getKeystatName(),
                latest.getDataValue(),
                latest.getPreviousDataValue(),
                latest.getCycle(),
                true,
                displayMode, history
            );
        }
    }

    public record GlobalItem(
        String indicatorCode,
        String countryName,
        String indicatorType,
        String dataValue,
        String previousDataValue,
        String cycle,
        String unit,
        boolean hasData,
        boolean failed,
        String failureReason,
        boolean refreshable,
        String displayMode,
        List<EnrichedHistoryPoint> history
    ) {
        public static GlobalItem from(EnrichedGlobalFavorite enriched) {
            String[] parts = enriched.favorite().getIndicatorCode().split("::", 2);
            String parsedCountry = parts.length > 0 ? parts[0] : "";
            String parsedType = parts.length > 1 ? parts[1] : "";
            FavoriteDisplayMode mode = enriched.favorite().getDisplayMode();
            String displayMode = (mode != null ? mode : FavoriteDisplayMode.INDICATOR).name();
            List<EnrichedHistoryPoint> history = toHistoryPoints(enriched.history());

            if (enriched.isFailed()) {
                return new GlobalItem(
                    enriched.favorite().getIndicatorCode(),
                    parsedCountry, parsedType,
                    null, null, null, null,
                    false, true, enriched.failureReason(), enriched.refreshable(),
                    displayMode, history
                );
            }

            CountryIndicatorSnapshot snap = enriched.snapshot();
            if (snap == null) {
                return new GlobalItem(
                    enriched.favorite().getIndicatorCode(),
                    parsedCountry, parsedType,
                    null, null, null, null,
                    false, false, null, true,
                    displayMode, history
                );
            }

            IndicatorValue last = snap.getLastValue();
            IndicatorValue prev = snap.getPreviousValue();
            return new GlobalItem(
                enriched.favorite().getIndicatorCode(),
                snap.getCountryName(),
                snap.getIndicatorType().name(),
                last != null ? last.getRawText() : null,
                prev != null ? prev.getRawText() : null,
                snap.getReferenceText(),
                last != null ? last.getUnit() : null,
                last != null,
                false, null, true,
                displayMode, history
            );
        }
    }

    public record EnrichedHistoryPoint(LocalDate snapshotDate, String dataValue) {}

    private static List<EnrichedHistoryPoint> toHistoryPoints(List<HistoryPoint> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        return points.stream()
            .map(p -> new EnrichedHistoryPoint(p.snapshotDate(), p.dataValue()))
            .toList();
    }
}
