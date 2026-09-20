package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/** 금액과 비율을 지수 표기 없는 문자열로 전달해 브라우저 정밀도를 보존한다. */
public class SrimDecimalSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(value.toPlainString());
    }
}
