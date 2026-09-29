package com.thlee.stock.market.stockmarket.economics.presentation;

import com.thlee.stock.market.stockmarket.economics.application.BondYieldQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.BondYieldResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/economics/bond-yields")
@RequiredArgsConstructor
public class BondYieldController {

    /** 부호 없는 4자리 연도만 받는다. LocalDate.parse는 "-0001-01-01" 같은 부호 있는 연도도 통과시킨다. */
    private static final Pattern DATE_FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    /** 0000년은 폴백 중 음수 연도가 되어 조회 형식(yyyyMMdd)으로 바꿀 수 없다. */
    private static final int MIN_YEAR = 1;

    private final BondYieldQueryService bondYieldQueryService;

    /**
     * 날짜별 채권 기준수익률 조회 (S-RIM 요구수익률 선택용)
     * 날짜를 생략하면 오늘(KST)이고, 요청일 데이터가 없으면 최대 10일 전까지 거슬러 적용일을 찾는다.
     *
     * GET /api/economics/bond-yields?date=2026-09-28
     */
    @GetMapping
    public ResponseEntity<BondYieldResponse> getBondYields(@RequestParam(name = "date", required = false) String date) {
        BondYieldLookup lookup = bondYieldQueryService.lookup(parseDate(date));
        return ResponseEntity.ok(BondYieldResponse.from(lookup, BondYieldQueryService.MAX_FALLBACK_DAYS));
    }

    /** 형식 오류는 IllegalArgumentException으로 바꿔 400으로 응답한다. */
    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String text = date.strip();
        if (!DATE_FORMAT.matcher(text).matches()) {
            throw invalidDate(date);
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw invalidDate(date);
        }
        if (parsed.getYear() < MIN_YEAR) {
            throw invalidDate(date);
        }
        return parsed;
    }

    private static IllegalArgumentException invalidDate(String date) {
        return new IllegalArgumentException("날짜는 YYYY-MM-DD 형식이어야 합니다: " + date);
    }
}
