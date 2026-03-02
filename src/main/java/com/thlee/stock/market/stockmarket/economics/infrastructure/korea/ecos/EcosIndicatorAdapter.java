package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.service.EcosIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.dto.EcosKeyStatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcosIndicatorAdapter implements EcosIndicatorPort {

    private final EcosApiClient ecosApiClient;

    @Override
    public EcosKeyStatResult fetchKeyStatistics() {
        EcosKeyStatResponse response = ecosApiClient.fetchKeyStatistics();

        if (response.getKeyStatisticList() == null || response.getKeyStatisticList().getRow() == null) {
            return new EcosKeyStatResult(0, Collections.emptyList());
        }

        List<KeyStatIndicator> indicators = response.getKeyStatisticList().getRow().stream()
            .map(row -> new KeyStatIndicator(
                row.getClassName(),
                row.getKeystatName(),
                row.getDataValue(),
                row.getCycle(),
                row.getUnitName()))
            .toList();

        return new EcosKeyStatResult(
            response.getKeyStatisticList().getListTotalCount(),
            indicators);
    }
}