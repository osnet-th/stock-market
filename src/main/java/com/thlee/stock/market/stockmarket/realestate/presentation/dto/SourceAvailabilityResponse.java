package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

/**
 * 부동산 외부 출처별 가용성 응답.
 * <p>
 * FE는 본 응답으로 카드 섹션 상태(정상/준비 중)를 분기한다. 특히 광역(REB) 카드는
 * {@link Source#available()}가 false면 빈 카드 그리드 대신 "광역 통계 준비 중" 안내 패널로 대체.
 * <p>
 * 가용성 판단 기준 (application 계층 {@code SourceAvailabilityService}에서 결정):
 * <ul>
 *   <li>{@code api-key} 설정 여부</li>
 *   <li>(향후) 최근 호출 성공 여부</li>
 * </ul>
 */
public record SourceAvailabilityResponse(
        Source reb,
        Source molit,
        Source kosis,
        Source gg
) {
    /**
     * 단일 출처 상태.
     *
     * @param available 호출 가능 여부 (api-key 등 사전 조건 만족 시 true)
     * @param reason    사유 코드 (null/빈 문자열 = 정상). 예: {@code api-key-missing}, {@code recent-call-failed}
     */
    public record Source(boolean available, String reason) {
    }
}