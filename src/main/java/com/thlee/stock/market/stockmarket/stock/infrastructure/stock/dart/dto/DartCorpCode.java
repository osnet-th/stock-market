package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CORPCODE.xml 파싱 결과
 */
@Getter
@RequiredArgsConstructor
public class DartCorpCode {
    private final String corpCode;    // 고유번호 (8자리)
    private final String corpName;    // 정식명칭
    private final String stockCode;   // 종목코드 (6자리, 비상장은 빈 문자열)
    private final String modifyDate;  // 최종변경일자
}