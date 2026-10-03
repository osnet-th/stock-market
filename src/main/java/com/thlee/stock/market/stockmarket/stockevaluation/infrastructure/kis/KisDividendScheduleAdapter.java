package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.DividendSchedule;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.service.DividendSchedulePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KIS 예탁원 배당일정 조회를 통한 현금배당 일정 어댑터.
 * DividendSchedulePort 구현체.
 *
 * <p>결과는 종목·기간 단위로 12시간 캐시한다. 배당 일정은 자주 바뀌지 않는데, 캐시가 없으면 포트폴리오
 * 요약을 열 때마다 보유 종목 수만큼 외부 호출이 나간다. 조회 실패는 캐시하지 않아 다음 요청에서 다시 조회한다.</p>
 *
 * <p>응답 문자열 형식은 문서로 확인되지 않아 행 단위로 관대하게 읽는다. 해석할 수 없는 지급일은 지급일 없음으로 두고,
 * 현금배당금이 없거나 0 이하이거나 해석할 수 없는 행(주식배당 등)은 뺀다. 이런 행은 조회 실패로 보지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class KisDividendScheduleAdapter implements DividendSchedulePort {

    private static final String PAYMENT_DATE = "divi_pay_dt";
    private static final String CASH_PER_SHARE = "per_sto_divi_amt";
    /** KIS 날짜 형식 (yyyyMMdd) */
    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int KIS_DATE_LENGTH = 8;
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final long CACHE_MAX_SIZE = 500;

    private final KisKsdScheduleClient ksdScheduleClient;

    private final Cache<CacheKey, List<DividendSchedule>> cache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    /**
     * 로더가 예외를 던지면 캐시에 아무것도 남지 않고 예외가 그대로 호출자에게 간다.
     */
    @Override
    public List<DividendSchedule> findCashDividends(String stockCode, LocalDate from, LocalDate to) {
        return cache.get(new CacheKey(stockCode, from, to), this::fetch);
    }

    private List<DividendSchedule> fetch(CacheKey key) {
        List<Map<String, String>> rows = ksdScheduleClient.fetch(KsdScheduleType.DIVIDEND, key.stockCode(),
                key.from().format(KIS_DATE_FORMAT), key.to().format(KIS_DATE_FORMAT));
        if (rows == null) {
            return List.of();
        }

        List<DividendSchedule> schedules = new ArrayList<>();
        for (Map<String, String> row : rows) {
            if (row == null) {
                continue;
            }
            BigDecimal cashPerShare = parseAmount(row.get(CASH_PER_SHARE));
            if (cashPerShare != null && cashPerShare.signum() > 0) {
                schedules.add(new DividendSchedule(parseDate(row.get(PAYMENT_DATE)), cashPerShare));
            }
        }
        return List.copyOf(schedules);
    }

    /** 콤마·공백을 지우고 숫자로 읽는다. 비었거나 숫자가 아니면 null */
    private BigDecimal parseAmount(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[,\\s]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 숫자만 남겨 8자리면 yyyyMMdd 로 읽는다. 그 외에는 지급일 없음(null) */
    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != KIS_DATE_LENGTH) {
            return null;
        }
        try {
            return LocalDate.parse(digits, KIS_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record CacheKey(String stockCode, LocalDate from, LocalDate to) {
    }
}
