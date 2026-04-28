package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

/**
 * 사건 링크 입력. {@code displayOrder}는 application 서비스가 리스트 인덱스로 부여한다.
 */
public record NewsEventLinkCommand(
        String title,
        String url
) { }