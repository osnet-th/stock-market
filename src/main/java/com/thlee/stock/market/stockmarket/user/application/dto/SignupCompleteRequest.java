package com.thlee.stock.market.stockmarket.user.application.dto;

public record SignupCompleteRequest(
        Long userId,
        String name,
        String nickname,
        String phoneNumber
) {
}