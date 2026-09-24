package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.service.MarketCalendarPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisHolidayOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 국내휴장일조회를 통한 개장일 판별 어댑터.
 * MarketCalendarPort 구현체.
 *
 * <p>응답이 비었거나 대상 일자가 없으면 false 대신 예외를 던진다. 그 상황은 휴장이 아니라
 * 판별 실패이며, false 로 돌려주면 호출자가 휴장일로 오인한다.</p>
 */
@Component
@RequiredArgsConstructor
public class KisMarketCalendarAdapter implements MarketCalendarPort {

    /** 개장일 여부(opnd_yn) 가 이 값이면 장이 열린 것으로 본다. */
    private static final String OPEN_DAY = "Y";
    /** 개장일 여부(opnd_yn) 가 이 값이면 휴장으로 본다. 둘 중 어느 것도 아니면 판별 실패다. */
    private static final String CLOSED_DAY = "N";
    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KisHolidayClient holidayClient;

    @Override
    public boolean isOpen(LocalDate date) {
        List<KisHolidayOutput> holidays = holidayClient.fetchHolidays(date);
        if (holidays == null || holidays.isEmpty()) {
            throw new KisApiException("국내 휴장일 조회 응답이 비어 있습니다: " + date);
        }

        String targetDate = date.format(KIS_DATE_FORMAT);
        return holidays.stream()
                .filter(holiday -> targetDate.equals(holiday.getBaseDate()))
                .findFirst()
                .map(holiday -> resolveOpenDay(holiday, date))
                .orElseThrow(() -> new KisApiException(
                        "국내 휴장일 조회 응답에 대상 일자가 없습니다: " + date));
    }

    /**
     * 개장일 여부 값을 해석한다. {@code Y}/{@code N} 외의 값은 휴장이 아니라 판별 실패로 본다.
     *
     * <p>알 수 없는 값을 휴장으로 접으면 그날 저장이 조용히 생략되고, 호출자의 조회 실패 대비 경로도
     * 타지 않는다. 지나간 날짜의 시세는 되살릴 수 없어 복구가 불가능하다.</p>
     *
     * <p>예외 메시지에 원본 값을 넣지 않는다. 외부에서 온 문자열을 로그로 흘리지 않기 위함이다.</p>
     */
    private boolean resolveOpenDay(KisHolidayOutput holiday, LocalDate date) {
        String openDayYn = holiday.getOpenDayYn();
        if (OPEN_DAY.equals(openDayYn)) {
            return true;
        }
        if (CLOSED_DAY.equals(openDayYn)) {
            return false;
        }
        throw new KisApiException("국내 휴장일 조회 응답의 개장일 여부를 해석할 수 없습니다: " + date);
    }
}
