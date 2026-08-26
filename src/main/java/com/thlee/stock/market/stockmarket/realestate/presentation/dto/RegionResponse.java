package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.util.List;

public record RegionResponse(List<SidoGroup> sidos) {

    public record SidoGroup(String sidoCode,
                            String sidoName,
                            List<Sigungu> sigungus) {
    }

    public record Sigungu(String regionCode,
                          String sigunguName) {
    }

    public record Emd(String emdCode,
                      String emdName) {
    }
}