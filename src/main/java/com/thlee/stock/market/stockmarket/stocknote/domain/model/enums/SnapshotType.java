package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 가격 스냅샷 시점 종류.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum SnapshotType {
    AT_NOTE,     // 기록 작성 시점
    D_PLUS_7,    // 기록일 + 7 영업일
    D_PLUS_30    // 기록일 + 30 영업일
}