package com.thlee.stock.market.stockmarket.news.application.dto;

import java.util.List;

/**
 * 외부 뉴스 API 조회 결과 (#115).
 *
 * <p>이전에는 조회 실패 시에도 빈 리스트를 돌려줘서 <b>"수집 실패"와 "새 기사 없음"이 구분되지 않았다</b>.
 * 수집 이력의 연속 실패 판정이 이 구분에 달려 있어 결과에 성공 여부를 함께 싣는다.
 *
 * @param results      조회된 기사. 실패면 빈 리스트
 * @param succeeded    포트 중 하나라도 예외 없이 응답했으면 true. 빈 결과라도 성공이다
 * @param errorMessage 실패 시 마지막 포트의 오류 메시지
 */
public record NewsSearchOutcome(
        List<NewsResultDto> results,
        boolean succeeded,
        String errorMessage
) {

    public static NewsSearchOutcome success(List<NewsResultDto> results) {
        return new NewsSearchOutcome(results, true, null);
    }

    public static NewsSearchOutcome failure(String errorMessage) {
        return new NewsSearchOutcome(List.of(), false, errorMessage);
    }
}
