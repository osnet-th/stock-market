package com.thlee.stock.market.stockmarket.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User JPA Repository
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임과 전화번호로 사용자 조회
     */
    Optional<UserEntity> findByNicknameAndPhoneNumber(String nickname, String phoneNumber);
}