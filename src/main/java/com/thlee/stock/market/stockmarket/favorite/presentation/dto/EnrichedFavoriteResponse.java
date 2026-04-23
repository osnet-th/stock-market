package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedEcosFavorite;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedFavorites;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedGlobalFavorite;

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
        boolean hasData
    ) {
        public static EcosItem from(EnrichedEcosFavorite enriched) {
            EcosIndicatorLatest latest = enriched.latest();
            if (latest == null) {
                String[] parts = enriched.favorite().getIndicatorCode().split("::", 2);
                return new EcosItem(
                    enriched.favorite().getIndicatorCode(),
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    null, null, null, false
                );
            }
            return new EcosItem(
                enriched.favorite().getIndicatorCode(),
                latest.getClassName(),
                latest.getKeystatName(),
                latest.getDataValue(),
                latest.getPreviousDataValue(),
                latest.getCycle(),
                true
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
        boolean refreshable
    ) {
        public static GlobalItem from(EnrichedGlobalFavorite enriched) {
            String[] parts = enriched.favorite().getIndicatorCode().split("::", 2);
            String parsedCountry = parts.length > 0 ? parts[0] : "";
            String parsedType = parts.length > 1 ? parts[1] : "";

            if (enriched.isFailed()) {
                return new GlobalItem(
                    enriched.favorite().getIndicatorCode(),
                    parsedCountry, parsedType,
                    null, null, null, null,
                    false, true, enriched.failureReason(), enriched.refreshable()
                );
            }

            CountryIndicatorSnapshot snap = enriched.snapshot();
            if (snap == null) {
                return new GlobalItem(
                    enriched.favorite().getIndicatorCode(),
                    parsedCountry, parsedType,
                    null, null, null, null,
                    false, false, null, true
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
                false, null, true
            );
        }
    }
}