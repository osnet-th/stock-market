package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class DataGoKrStockResponse {

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
        private List<DataGoKrStockItem> item;
    }

    public List<DataGoKrStockItem> getItemList() {
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