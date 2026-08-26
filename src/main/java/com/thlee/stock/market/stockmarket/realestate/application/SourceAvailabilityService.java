package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SourceAvailabilityResponse;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.SourceAvailabilityResponse.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 부동산 외부 출처 가용성 판단 (application 계층).
 * <p>
 * 현재 판단 기준은 {@code api-key} 설정 여부만. 향후 어댑터별 최근 호출 성공 여부 등을
 * 메트릭/캐시에서 추가 조회해 보강 가능.
 * <p>
 * presentation 계층(Controller)은 본 서비스에 위임만 수행하며 비즈니스 로직을 포함하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SourceAvailabilityService {

    /**
     * 일반화된 사유 — 내부 구성 상태(api-key 유무 등) 노출 회피.
     * 보안: 본 endpoint가 향후 permitAll 정책으로 변경되더라도 내부 상태가 leak되지 않도록 일반어로 통일.
     */
    private static final String REASON_UNAVAILABLE = "unavailable";

    private final RealEstateMarketProperties properties;

    public SourceAvailabilityResponse currentAvailability() {
        return new SourceAvailabilityResponse(
                evaluate(properties.getReb()),
                evaluate(properties.getMolit()),
                evaluate(properties.getKosis()),
                evaluate(properties.getGgDataDream())
        );
    }

    private Source evaluate(RealEstateMarketProperties.SourceProps props) {
        if (props == null) {
            return new Source(false, REASON_UNAVAILABLE);
        }
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            return new Source(false, REASON_UNAVAILABLE);
        }
        return new Source(true, null);
    }
}
