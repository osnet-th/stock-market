package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 가격 스냅샷 상태 머신.
 *
 * <p>전이: {@code PENDING → SUCCESS | FAILED}, {@code FAILED (retry_count++) → ...}.
 * retry_count >= 3 인 FAILED 는 종결(재시도 종료)된 것으로 해석한다.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum SnapshotStatus {
    PENDING,   // 아직 가격 조회가 완료되지 않음 (재시도 대상)
    SUCCESS,   // 가격 조회/저장 완료
    FAILED     // 가격 조회 실패 (retry_count 로 재시도 소진 여부 판단)
}