package com.thlee.stock.market.stockmarket.user.application.dto;

public record SignupCompleteRequest(
        String name,
        String nickname,
        String phoneNumber
) {
}