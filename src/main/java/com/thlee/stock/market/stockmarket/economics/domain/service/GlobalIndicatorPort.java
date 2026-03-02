package com.thlee.stock.market.stockmarket.economics.domain.service;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;

import java.util.List;

public interface GlobalIndicatorPort {
    List<CountryIndicatorSnapshot> fetchByIndicator(GlobalEconomicIndicatorType indicatorType);
}