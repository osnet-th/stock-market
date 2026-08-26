package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.CompanyProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CompanyProfileResponse {
    private final String corpName;
    private final String corpNameEng;
    private final String stockName;
    private final String stockCode;
    private final String ceoName;
    private final String address;
    private final String homepageUrl;
    private final String irUrl;
    private final String phoneNumber;
    private final String industryCode;
    private final String establishedDate;
    private final String accountSettlementMonth;

    public static CompanyProfileResponse from(CompanyProfile profile) {
        return new CompanyProfileResponse(
                profile.corpName(),
                profile.corpNameEng(),
                profile.stockName(),
                profile.stockCode(),
                profile.ceoName(),
                profile.address(),
                profile.homepageUrl(),
                profile.irUrl(),
                profile.phoneNumber(),
                profile.industryCode(),
                profile.establishedDate(),
                profile.accountSettlementMonth()
        );
    }
}
