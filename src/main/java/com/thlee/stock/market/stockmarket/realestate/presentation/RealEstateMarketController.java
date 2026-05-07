package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.application.RealEstateMarketQueryService;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.ComparisonResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.PeriodOption;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SourceMetaResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SummaryResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.TabResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 부동산 시장 데이터 API. 모든 endpoint는 로그인 사용자 전용 (Spring Security 기본 인증 정책).
 * <p>
 * 응답에 판단/추천/등급 라벨 필드는 일체 노출되지 않는다.
 */
@RestController
@RequestMapping("/api/realestate/market")
@RequiredArgsConstructor
public class RealEstateMarketController {

    private final RealEstateMarketQueryService queryService;

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> summary(@RequestParam("region_code") String regionCode) {
        return ResponseEntity.ok(queryService.getSummary(regionCode));
    }

    @GetMapping("/tabs/{category}")
    public ResponseEntity<TabResponse> tab(
            @PathVariable("category") RealEstateMarketCategory category,
            @RequestParam("region_code") String regionCode,
            @RequestParam(value = "period", defaultValue = "ONE_MONTH") PeriodOption period) {
        return ResponseEntity.ok(queryService.getTab(regionCode, category, period));
    }

    @GetMapping("/comparison")
    public ResponseEntity<ComparisonResponse> comparison(
            @RequestParam("region_codes") List<String> regionCodes,
            @RequestParam("category") RealEstateMarketCategory category,
            @RequestParam(value = "period", defaultValue = "ONE_MONTH") PeriodOption period) {
        return ResponseEntity.ok(queryService.getComparison(regionCodes, category, period));
    }

    @GetMapping("/sources")
    public ResponseEntity<SourceMetaResponse> sources() {
        return ResponseEntity.ok(queryService.getSourceMetadata());
    }
}