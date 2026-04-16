package com.thlee.stock.market.stockmarket.salary.application.dto;

import lombok.Getter;

import java.time.YearMonth;

/**
 * upsert 결과. NOOP은 상속값과 동일 값을 저장 시도한 경우 — 레코드를 만들지 않는다.
 */
@Getter
public class UpsertResultResponse {

    public enum UpsertStatus { CREATED, UPDATED, NOOP }

    private final UpsertStatus status;

    /** NOOP일 때만 non-null. 상속 출처 월. */
    private final YearMonth inheritedFromMonth;

    private final String message;

    private UpsertResultResponse(UpsertStatus status, YearMonth inheritedFromMonth, String message) {
        this.status = status;
        this.inheritedFromMonth = inheritedFromMonth;
        this.message = message;
    }

    public static UpsertResultResponse created() {
        return new UpsertResultResponse(UpsertStatus.CREATED, null,
                "새 변경 레코드를 생성했습니다.");
    }

    public static UpsertResultResponse updated() {
        return new UpsertResultResponse(UpsertStatus.UPDATED, null,
                "기존 변경 레코드를 수정했습니다.");
    }

    public static UpsertResultResponse noop(YearMonth inheritedFromMonth) {
        return new UpsertResultResponse(UpsertStatus.NOOP, inheritedFromMonth,
                "상속값과 동일하여 레코드를 생성하지 않았습니다.");
    }
}