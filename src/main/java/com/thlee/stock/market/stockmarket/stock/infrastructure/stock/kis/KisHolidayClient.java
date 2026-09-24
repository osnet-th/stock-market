package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisHolidayOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 국내휴장일 조회 클라이언트.
 * KisApiClient에 휴장일 전용 파라미터를 조립하여 위임한다.
 *
 * <p>KIS 공식 안내상 원장 서비스와 연동된 API라 1일 1회 호출이 권장된다.
 * 하루 한 번 도는 배치에서만 호출한다.</p>
 */
@Component
@RequiredArgsConstructor
public class KisHolidayClient {

    private static final String HOLIDAY_PATH = "/uapi/domestic-stock/v1/quotations/chk-holiday";
    private static final String HOLIDAY_TR_ID = "CTCA0903R";
    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KisApiClient kisApiClient;

    /**
     * 기준일자부터의 휴장일 정보를 조회한다.
     * 응답은 기준일자 이후 여러 날짜를 함께 담을 수 있다.
     *
     * @param baseDate 기준일자
     */
    public List<KisHolidayOutput> fetchHolidays(LocalDate baseDate) {
        return kisApiClient.get(
            HOLIDAY_PATH,
            HOLIDAY_TR_ID,
            uriBuilder -> uriBuilder
                .queryParam("BASS_DT", baseDate.format(KIS_DATE_FORMAT))
                .queryParam("CTX_AREA_FK", "")
                .queryParam("CTX_AREA_NK", "")
                .build(),
            new ParameterizedTypeReference<>() {},
            "국내 휴장일 조회 [" + baseDate + "]"
        );
    }
}
