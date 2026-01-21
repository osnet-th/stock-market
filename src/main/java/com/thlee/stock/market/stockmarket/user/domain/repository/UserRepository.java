package com.thlee.stock.market.stockmarket.user.domain.repository;

import com.thlee.stock.market.stockmarket.user.domain.model.Nickname;
import com.thlee.stock.market.stockmarket.user.domain.model.PhoneNumber;
import com.thlee.stock.market.stockmarket.user.domain.model.User;

import java.util.Optional;

/**
 * User Repository 인터페이스
 */
public interface UserRepository {
    /**
     * 사용자 저장
     */
    User save(User user);

    /**
     * ID로 사용자 조회
     */
    Optional<User> findById(Long id);

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(Nickname nickname);

    /**
     * 닉네임과 전화번호로 사용자 조회
     */
    Optional<User> findByNicknameAndPhoneNumber(Nickname nickname, PhoneNumber phoneNumber);
}