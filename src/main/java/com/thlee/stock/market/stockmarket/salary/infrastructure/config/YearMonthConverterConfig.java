package com.thlee.stock.market.stockmarket.salary.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * {@code "yyyy-MM"} 형식 문자열을 {@link YearMonth}로 변환한다.
 *
 * <p>{@code @PathVariable YearMonth yearMonth} 바인딩을 가능하게 하기 위해
 * 전역 {@link Converter} Bean으로 등록한다. salary 도메인의 REST API 경로에서 사용된다.
 */
@Configuration
public class YearMonthConverterConfig {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Bean
    public Converter<String, YearMonth> stringToYearMonthConverter() {
        return source -> YearMonth.parse(source, YEAR_MONTH_FORMAT);
    }
}