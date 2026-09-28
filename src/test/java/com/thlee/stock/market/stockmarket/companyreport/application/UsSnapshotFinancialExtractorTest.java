package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RatioRow;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyFacts;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsFinancialConcept;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsSnapshotFinancialExtractor — 미국 리포트 ROE 기준")
class UsSnapshotFinancialExtractorTest {

    private final UsSnapshotFinancialExtractor extractor = new UsSnapshotFinancialExtractor();

    @Test
    @DisplayName("E1 지배주주 전용 순이익·자본 시리즈가 모두 있으면 지배주주 기준으로 ROE를 계산한다")
    void roeOnOwnersBasis() {
        Map<UsFinancialConcept, Map<String, BigDecimal>> annual = totals();
        annual.put(UsFinancialConcept.NET_INCOME_TO_PARENT, Map.of("2025", new BigDecimal("100")));
        annual.put(UsFinancialConcept.EQUITY_OF_PARENT, Map.of("2025", new BigDecimal("1000")));
        UsCompanyFacts facts = new UsCompanyFacts(List.of("2025"), annual);

        assertThat(roe(facts)).containsEntry("2025", "10.00");
        assertThat(extractor.roeBasis(facts)).isEqualTo(ReportSnapshot.ROE_BASIS_OWNERS);
    }

    @Test
    @DisplayName("E2 지배주주 전용 자본 시리즈가 없으면 기준을 섞지 않고 기존 계산·전체 기준으로 둔다")
    void roeFallsBackToTotal() {
        Map<UsFinancialConcept, Map<String, BigDecimal>> annual = totals();
        annual.put(UsFinancialConcept.NET_INCOME_TO_PARENT, Map.of("2025", new BigDecimal("100")));
        UsCompanyFacts facts = new UsCompanyFacts(List.of("2025"), annual);

        assertThat(roe(facts)).containsEntry("2025", "10.91");
        assertThat(extractor.roeBasis(facts)).isEqualTo(ReportSnapshot.ROE_BASIS_TOTAL);
    }

    private Map<UsFinancialConcept, Map<String, BigDecimal>> totals() {
        Map<UsFinancialConcept, Map<String, BigDecimal>> annual = new EnumMap<>(UsFinancialConcept.class);
        annual.put(UsFinancialConcept.NET_INCOME, Map.of("2025", new BigDecimal("120")));
        annual.put(UsFinancialConcept.EQUITY, Map.of("2025", new BigDecimal("1100")));
        return annual;
    }

    private Map<String, String> roe(UsCompanyFacts facts) {
        return extractor.ratioRows(facts, "2025").stream()
                .filter(r -> "roe".equals(r.key())).findFirst()
                .map(RatioRow::values).orElse(Map.of());
    }
}
