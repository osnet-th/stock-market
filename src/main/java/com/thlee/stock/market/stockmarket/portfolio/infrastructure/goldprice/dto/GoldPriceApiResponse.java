package com.thlee.stock.market.stockmarket.portfolio.infrastructure.goldprice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 금융위원회_일반상품시세정보 getGoldPriceInfo 응답
 */
@Getter
@Setter
public class GoldPriceApiResponse {

    private Response response;

    @Getter
    @Setter
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    public static class Body {
        private int numOfRows;
        private int pageNo;
        private int totalCount;
        private Items items;
    }

    @Getter
    @Setter
    public static class Items {
        private List<GoldPriceItem> item;
    }

    @Getter
    @Setter
    public static class GoldPriceItem {
        private String basDt;   // 기준일자 (yyyyMMdd)
        private String itmsNm;  // 종목명 (예: 금 99.99_1Kg)
        private String clpr;    // 종가 (1g당 KRW)
    }

    public List<GoldPriceItem> getItemList() {
        if (response == null || response.getBody() == null
                || response.getBody().getItems() == null
                || response.getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getBody().getItems().getItem();
    }

    public boolean isSuccess() {
        return response != null
                && response.getHeader() != null
                && "00".equals(response.getHeader().getResultCode());
    }
}
