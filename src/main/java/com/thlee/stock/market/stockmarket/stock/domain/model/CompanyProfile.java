package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * DART 기업개황
 */
public record CompanyProfile(
        String corpCode,
        String corpName,
        String corpNameEng,
        String stockName,
        String stockCode,
        String ceoName,
        String corpClass,
        String corporateRegistrationNo,
        String businessRegistrationNo,
        String address,
        String homepageUrl,
        String irUrl,
        String phoneNumber,
        String faxNumber,
        String industryCode,
        String establishedDate,
        String accountSettlementMonth
) {}
