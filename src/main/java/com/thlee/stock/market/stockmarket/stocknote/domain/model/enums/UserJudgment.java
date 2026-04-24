package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 기록 작성 시점의 투자자 본인 예측(판단).
 * 사후 검증 결과와는 분리된다 (그쪽은 {@link JudgmentResult}).
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum UserJudgment {
    MORE_UPSIDE,         // 추가 상승 가능
    NEUTRAL,             // 중립
    OVERHEATED,          // 단기 과열
    CATALYST_EXHAUSTED   // 재료 소멸 우려
}