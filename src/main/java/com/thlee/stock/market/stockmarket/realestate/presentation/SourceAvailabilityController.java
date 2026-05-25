package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.application.SourceAvailabilityService;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SourceAvailabilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부동산 외부 출처 가용성 조회. application 계층 {@link SourceAvailabilityService}에 위임.
 */
@RestController
@RequestMapping("/api/realestate/sources")
@RequiredArgsConstructor
public class SourceAvailabilityController {

    private final SourceAvailabilityService service;

    @GetMapping("/availability")
    public SourceAvailabilityResponse availability() {
        return service.currentAvailability();
    }
}
