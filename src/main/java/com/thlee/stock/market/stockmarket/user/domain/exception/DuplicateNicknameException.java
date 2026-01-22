package com.thlee.stock.market.stockmarket.user.domain.exception;

/**
 * 닉네임 중복 예외 (도메인 규칙: 닉네임은 시스템 전체에서 unique)
 */
public class DuplicateNicknameException extends UserDomainException {

    public DuplicateNicknameException(String nickname) {
        super("이미 사용 중인 닉네임입니다: " + nickname);
    }
}