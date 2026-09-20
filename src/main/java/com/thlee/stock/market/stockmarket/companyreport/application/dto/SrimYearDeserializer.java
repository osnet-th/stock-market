package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** Jackson의 소수→정수 자동 절삭을 허용하지 않는다. */
public class SrimYearDeserializer extends JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) return parser.getIntValue();
        return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }
}
