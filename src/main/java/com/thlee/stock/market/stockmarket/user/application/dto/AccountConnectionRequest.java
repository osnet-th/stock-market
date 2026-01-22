package com.thlee.stock.market.stockmarket.user.application.dto;

public record AccountConnectionRequest(
        String nickname,
        String phoneNumber
) {
}