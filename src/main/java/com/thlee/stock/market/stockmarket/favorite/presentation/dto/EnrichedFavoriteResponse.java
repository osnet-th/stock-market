package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
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
        boolean hasData
    ) {
        public static GlobalItem from(EnrichedGlobalFavorite enriched) {
            GlobalIndicatorLatest latest = enriched.latest();
            if (latest == null) {
                String[] parts = enriched.favorite().getIndicatorCode().split("::", 2);
                return new GlobalItem(
                    enriched.favorite().getIndicatorCode(),
                    parts.length > 0 ? parts[0] : "",
                    parts.length > 1 ? parts[1] : "",
                    null, null, null, null, false
                );
            }
            return new GlobalItem(
                enriched.favorite().getIndicatorCode(),
                latest.getCountryName(),
                latest.getIndicatorType().name(),
                latest.getDataValue(),
                latest.getPreviousDataValue(),
                latest.getCycle(),
                latest.getUnit(),
                true
            );
        }
    }
}