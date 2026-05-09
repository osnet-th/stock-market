package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 관심지표 일괄 순서 갱신 요청.
 *
 * <p>컨테이너 스코프 (sourceType × displayMode) 내 보이는 항목들의 새 순서를 원하는 순으로 담는다.
 * 서버는 R7 슬롯 보존 알고리즘에 따라 (user_id, sourceType) 그룹 전체를 dense 0..N-1로 재할당한다.
 *
 * <p><b>중복 코드 dedup 정책:</b> 같은 코드가 두 번 이상 들어오면 첫 occurrence만 채택, 이후는 silent skip.
 * 400 거부 대신 last-writer-wins 정책과 일관된 방어적 수용을 한다.
 *
 * <p><b>cross-container 코드:</b> 페이로드 안에 다른 sourceType이나 다른 displayMode 항목 코드가 섞이면
 * R7(a)에 따라 서버가 silent ignore한다(서버 컨테이너에 없는 코드로 취급).
 */
public record FavoriteOrderRequest(
    @NotNull FavoriteIndicatorSourceType sourceType,
    @NotNull FavoriteDisplayMode displayMode,
    // MAX_CODES=200: 사용자 단위 컨테이너 카드 상한 추정치(DoS 방지용 페이로드 캡)
    // 각 String 최대 310자: Entity indicator_code 컬럼 length와 일치
    @NotEmpty
    @Size(max = 200, message = "indicatorCodes는 최대 200개까지 허용합니다")
    List<@NotBlank @Size(max = 310, message = "각 indicatorCode는 310자 이하여야 합니다") String> indicatorCodes
) {
}