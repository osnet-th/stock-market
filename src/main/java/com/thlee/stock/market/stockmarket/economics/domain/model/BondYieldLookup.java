package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 요청일 기준수익률 조회 결과. 폴백하면 실제 적용 기준일이 요청일보다 앞선다.
 * 한도 초과(NOT_FOUND)면 스냅샷이 없다.
 */
public record BondYieldLookup(LocalDate requestedDate, Status status, String source, BondYieldSnapshot snapshot) {

    public enum Status {
        /** 요청일 데이터를 적용 */
        FOUND,
        /** 요청일 데이터가 없어 이전 날짜 데이터를 적용 */
        FALLBACK,
        /** 폴백 한도 안에 데이터가 없음 */
        NOT_FOUND
    }

    public BondYieldLookup {
        Objects.requireNonNull(requestedDate, "requestedDate");
        Objects.requireNonNull(status, "status");
        if ((status == Status.NOT_FOUND) != (snapshot == null)) {
            throw new IllegalArgumentException("스냅샷은 NOT_FOUND일 때만 없다: status=" + status);
        }
    }

    /** 데이터가 있는 스냅샷을 적용한다. 기준일이 요청일보다 앞서면 폴백이다. */
    public static BondYieldLookup applied(LocalDate requestedDate, String source, BondYieldSnapshot snapshot) {
        if (snapshot.isEmpty() || snapshot.baseDate().isAfter(requestedDate)) {
            throw new IllegalArgumentException("요청일 이전의 데이터가 있는 스냅샷만 적용한다: baseDate=" + snapshot.baseDate());
        }
        Status status = snapshot.baseDate().equals(requestedDate) ? Status.FOUND : Status.FALLBACK;
        return new BondYieldLookup(requestedDate, status, source, snapshot);
    }

    public static BondYieldLookup notFound(LocalDate requestedDate, String source) {
        return new BondYieldLookup(requestedDate, Status.NOT_FOUND, source, null);
    }

    /** 실제 적용 기준일. 한도 초과면 null */
    public LocalDate baseDate() {
        return snapshot == null ? null : snapshot.baseDate();
    }

    public List<BondYield> yields() {
        return snapshot == null ? List.of() : snapshot.yields();
    }
}
