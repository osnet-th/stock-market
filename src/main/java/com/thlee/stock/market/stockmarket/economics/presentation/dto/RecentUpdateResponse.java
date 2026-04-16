package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;

import java.util.List;

public record RecentUpdateResponse(
    List<EcosUpdate> ecos,
    List<GlobalUpdate> global
) {

    public record EcosUpdate(
        String className,
        String keystatName,
        String dataValue,
        String previousDataValue,
        String cycle
    ) {
        public static EcosUpdate from(EcosIndicatorLatest latest) {
            return new EcosUpdate(
                latest.getClassName(),
                latest.getKeystatName(),
                latest.getDataValue(),
                latest.getPreviousDataValue(),
                latest.getCycle()
            );
        }
    }

    public record GlobalUpdate(
        String countryName,
        String indicatorType,
        String dataValue,
        String previousDataValue,
        String cycle,
        String unit
    ) {
        public static GlobalUpdate from(GlobalIndicatorLatest latest) {
            return new GlobalUpdate(
                latest.getCountryName(),
                latest.getIndicatorType().name(),
                latest.getDataValue(),
                latest.getPreviousDataValue(),
                latest.getCycle(),
                latest.getUnit()
            );
        }
    }
}