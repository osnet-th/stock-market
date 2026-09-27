package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 키워드 수정 요청 (#115).
 *
 * <p>{@code Keyword} 는 여러 사용자가 공유하는 불변 리소스라 <b>UPDATE 가 아니라 재구독</b>으로 처리한다.
 * 새 {@code (keyword, region)} 을 구독하고 기존 구독을 해제한다.
 */
@Getter
@Setter
public class UpdateKeywordRequest {

    @NotBlank(message = "키워드는 필수입니다.")
    @Size(max = 100, message = "키워드는 100자 이내여야 합니다.")
    private String keyword;

    @NotNull(message = "수집 범위는 필수입니다.")
    private Region region;

    /** 수정 모달의 활성 토글 값. 재구독 후 이 상태로 맞춘다. */
    private boolean active = true;
}
